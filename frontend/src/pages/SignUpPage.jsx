import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, applyApiError } from "../api/client.js";
import { useAuth } from "../auth.jsx";
import { AuthLayout } from "../components/AuthLayout.jsx";
import { Button } from "../components/Button.jsx";
import { Field } from "../components/Field.jsx";
import { PasswordField } from "../components/PasswordField.jsx";

const EMPTY = { name: "", username: "", email: "", mob: "", password: "" };

export function SignUpPage() {
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
      const session = await api("/api/signup/", {
        method: "POST",
        json: {
          name: values.name.trim(),
          username: values.username.trim(),
          email: values.email.trim(),
          mob: values.mob.trim(),
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
      title="Create account"
      footer={<>Already have an account? <Link to="/signin">Sign in</Link></>}
    >
      <form onSubmit={submit}>
        {formError ? <p className="form-error">{formError}</p> : null}
        <Field label="Name" name="name" autoComplete="name" value={values.name} error={errors.name} onChange={change} />
        <Field label="Username" name="username" autoComplete="username" value={values.username} error={errors.username} onChange={change} />
        <Field label="Email" name="email" type="email" autoComplete="email" value={values.email} error={errors.email} onChange={change} />
        <Field label="Mobile" name="mob" autoComplete="tel" value={values.mob} error={errors.mob} onChange={change} />
        <PasswordField label="Password" name="password" autoComplete="new-password" value={values.password} error={errors.password} onChange={change} />
        <Button disabled={pending}>{pending ? "Creating account…" : "Create account"}</Button>
      </form>
    </AuthLayout>
  );
}
