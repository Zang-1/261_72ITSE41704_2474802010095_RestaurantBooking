import { useT } from '../prefs.jsx';
import Icon from './Icon.jsx';

/** A labelled input with the server's validation message shown under it (NFR-11). */
export default function FormField({ label, error, children }) {
  const t = useT();
  return (
    <label className={`field${error ? ' has-error' : ''}`}>
      <span>{label}</span>
      {children}
      {error && <small className="error-text">{t(error)}</small>}
    </label>
  );
}

/** Message for errors that do not belong to a single field. */
export function FormError({ error }) {
  const t = useT();
  if (!error) return null;
  return <p className="alert" role="alert"><Icon name="alert" size={16} />{t(error.message)}</p>;
}
