import { Brand } from "./Brand.jsx";

export function AuthLayout({ title, children, footer }) {
  return (
    <main className="auth">
      <Brand animate tagline />
      <section className="panel">
        <h1>{title}</h1>
        {children}
      </section>
      <p className="switch">{footer}</p>
    </main>
  );
}
