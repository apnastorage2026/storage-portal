import { createContext, useContext, useMemo, useState } from "react";

const KEY = "apna.session";
const AuthContext = createContext(null);

function readSession() {
  const raw = localStorage.getItem(KEY);
  if (!raw) {
    return null;
  }
  try {
    const session = JSON.parse(raw);
    if (session?.token && session?.username) {
      return session;
    }
  } catch {
    localStorage.removeItem(KEY);
  }
  return null;
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession);

  const value = useMemo(() => ({
    session,
    signIn(next) {
      localStorage.setItem(KEY, JSON.stringify(next));
      setSession(next);
    },
    signOut() {
      localStorage.removeItem(KEY);
      setSession(null);
    },
  }), [session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
