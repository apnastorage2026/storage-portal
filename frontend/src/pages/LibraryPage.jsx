import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client.js";
import { useAuth } from "../auth.jsx";
import { Brand } from "../components/Brand.jsx";
import { Button } from "../components/Button.jsx";

export function LibraryPage() {
  const navigate = useNavigate();
  const { session, signOut } = useAuth();
  const [images, setImages] = useState([]);
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const [loading, setLoading] = useState(true);
  const [menuOpen, setMenuOpen] = useState(false);
  const [viewer, setViewer] = useState(null);
  const fileRef = useRef(null);
  const menuRef = useRef(null);

  useEffect(() => {
    let active = true;
    Promise.all([
      api("/api/images/", { token: session.token }),
      api("/api/account/", { token: session.token }),
    ])
      .then(([rows, account]) => {
        if (!active) {
          return;
        }
        setImages(rows);
        setProfile(account);
      })
      .catch((failure) => {
        if (!active) {
          return;
        }
        if (failure.status === 401) {
          signOut();
          navigate("/signin");
          return;
        }
        setError(failure.message || "Could not load photos.");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [session.token, signOut, navigate]);

  useEffect(() => {
    function close(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  async function uploadFiles(list) {
    if (list.length === 0) {
      return;
    }
    setPending(true);
    setError("");
    try {
      for (const file of list) {
        const form = new FormData();
        form.append("image", file);
        const saved = await api("/api/upload-image/", { method: "POST", form, token: session.token });
        setImages((current) => [saved, ...current]);
      }
    } catch (failure) {
      if (failure.status === 401) {
        signOut();
        navigate("/signin");
        return;
      }
      setError(failure.fields?.image || failure.message || "Could not upload the image.");
    } finally {
      setPending(false);
    }
  }

  const name = profile?.name || session.username;
  const initial = name.trim().charAt(0).toUpperCase() || "A";
  const groups = groupByDay(images);

  return (
    <main className="library">
      <header className="topbar">
        <Brand />
        <div className="toolbar" ref={menuRef}>
          <button type="button" className="upload-pill" disabled={pending} onClick={() => fileRef.current?.click()}>
            {pending ? "Uploading…" : "Upload"}
          </button>
          <input
            ref={fileRef}
            hidden
            type="file"
            multiple
            accept="image/jpeg,image/png,image/gif,image/webp"
            onChange={(event) => {
              const list = [...(event.target.files ?? [])];
              event.target.value = "";
              uploadFiles(list);
            }}
          />
          <button
            type="button"
            className="avatar"
            aria-expanded={menuOpen}
            aria-label="Open profile"
            onClick={() => setMenuOpen((open) => !open)}
          >
            {initial}
          </button>
          {menuOpen ? (
            <section className="profile">
              <div className="profile-head">
                <span className="avatar avatar-lg">{initial}</span>
                <div>
                  <strong>{name}</strong>
                  <p>@{profile?.username || session.username}</p>
                </div>
              </div>
              <dl>
                <div>
                  <dt>Email</dt>
                  <dd>{profile?.email ? profile.email : "Not added"}</dd>
                </div>
                <div>
                  <dt>Mobile</dt>
                  <dd>{profile?.mob || "—"}</dd>
                </div>
                <div>
                  <dt>Joined</dt>
                  <dd>{profile ? formatDay(profile.date_joined) : "…"}</dd>
                </div>
              </dl>
              <button type="button" className="signout" onClick={signOut}>Sign out</button>
            </section>
          ) : null}
        </div>
      </header>

      {error ? <p className="form-error">{error}</p> : null}
      {loading ? <p className="muted">Loading photos…</p> : null}
      {!loading && images.length === 0 ? (
        <section className="empty">
          <h1>Photos</h1>
          <p>Your pictures will live here, grouped by the day you added them.</p>
          <Button type="button" disabled={pending} onClick={() => fileRef.current?.click()}>Upload photos</Button>
        </section>
      ) : null}

      {groups.map((group) => (
        <section key={group.label}>
          <h2 className="day">{group.label}</h2>
          <ul className="mosaic">
            {group.images.map((image) => (
              <li key={image.id}>
                <button type="button" onClick={() => setViewer(image)}>
                  <img src={image.image} alt="" />
                </button>
              </li>
            ))}
          </ul>
        </section>
      ))}

      {viewer ? (
        <div className="viewer" role="dialog" aria-modal="true" onClick={() => setViewer(null)}>
          <button type="button" className="viewer-close" onClick={() => setViewer(null)}>Close</button>
          <img src={viewer.image} alt="" onClick={(event) => event.stopPropagation()} />
          <time dateTime={viewer.uploaded_at}>{formatWhen(viewer.uploaded_at)}</time>
        </div>
      ) : null}
    </main>
  );
}

function groupByDay(images) {
  const groups = [];
  for (const image of images) {
    const label = dayLabel(image.uploaded_at);
    const last = groups.at(-1);
    if (last && last.label === label) {
      last.images.push(image);
    } else {
      groups.push({ label, images: [image] });
    }
  }
  return groups;
}

function dayLabel(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Photos";
  }
  const today = startOfDay(new Date());
  const day = startOfDay(date);
  const diff = Math.round((today - day) / 86400000);
  if (diff === 0) {
    return "Today";
  }
  if (diff === 1) {
    return "Yesterday";
  }
  return date.toLocaleDateString(undefined, { day: "numeric", month: "long", year: "numeric" });
}

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

function formatDay(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString(undefined, { day: "numeric", month: "long", year: "numeric" });
}

function formatWhen(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
