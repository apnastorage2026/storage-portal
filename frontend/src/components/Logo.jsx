export function Logo({ size, animate = false }) {
  return (
    <img
      className={animate ? "logo logo-float" : "logo"}
      src="/logo-shield.png"
      alt=""
      style={size ? { height: size, width: "auto" } : undefined}
    />
  );
}
