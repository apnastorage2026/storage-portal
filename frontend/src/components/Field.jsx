import { useId } from "react";

export function Field({ label, error, ...props }) {
  const id = useId();
  return (
    <label className="field" htmlFor={id}>
      <span>{label}</span>
      <input id={id} aria-invalid={error ? true : undefined} {...props} />
      {error ? <small>{error}</small> : null}
    </label>
  );
}
