import { Logo } from "./Logo.jsx";

export function Brand({ size, animate = false, tagline = false }) {
  return (
    <div className={tagline ? "brand brand-stacked" : "brand"}>
      <Logo size={size} animate={animate} />
      <div>
        <p className="wordmark">apna<span>storage</span></p>
        {tagline && (
          <>
            <p className="tagline">Every Pixel Has a Home.</p>

          </>
        )}
      </div>
    </div>
  );
}
