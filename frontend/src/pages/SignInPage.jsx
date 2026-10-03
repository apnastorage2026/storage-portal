import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, applyApiError } from "../api/client.js";
import { useAuth } from "../auth.jsx";
import { AuthLayout } from "../components/AuthLayout.jsx";
import { Button } from "../components/Button.jsx";
import { Field } from "../components/Field.jsx";
import { PasswordField } from "../components/PasswordField.jsx";

const EMPTY = { username: "", password: "" };

export function SignInPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState("");
  const [pending, setPending] = useState(false);

  function change(event) {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setPending(true);
    setErrors({});
    setFormError("");
    try {
      const session = await api("/api/signin/", {
        method: "POST",
        json: {
          username: values.username.trim(),
          password: values.password.trim(),
        },
      });
      signIn(session);
      navigate("/");
    } catch (error) {
      applyApiError(error, setErrors, setFormError);
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthLayout
      title="Sign in"
      footer={<>New here? <Link to="/signup">Create an account</Link></>}
    >
      <form onSubmit={submit}>
        {formError ? <p className="form-error">{formError}</p> : null}
        <Field label="Username" name="username" autoComplete="username" value={values.username} error={errors.username} onChange={change} />
        <PasswordField label="Password" name="password" autoComplete="current-password" value={values.password} error={errors.password} onChange={change} />
        <Button disabled={pending}>{pending ? "Signing in…" : "Sign in"}</Button>
      </form>
    </AuthLayout>
  );
}
