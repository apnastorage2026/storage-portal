export function Button({ children, ...props }) {
  return (
    <button className="button" type="submit" {...props}>
      {children}
    </button>
  );
}
