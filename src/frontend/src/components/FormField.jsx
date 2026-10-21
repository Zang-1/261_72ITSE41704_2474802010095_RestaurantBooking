/** A labelled input with the server's validation message shown under it (NFR-11). */
export default function FormField({ label, error, children }) {
  return (
    <label className={`field${error ? ' has-error' : ''}`}>
      <span>{label}</span>
      {children}
      {error && <small className="error-text">{error}</small>}
    </label>
  );
}

/** Message for errors that do not belong to a single field. */
export function FormError({ error }) {
  if (!error) return null;
  return <p className="alert">{error.message}</p>;
}
