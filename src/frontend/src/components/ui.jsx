import { Link } from 'react-router-dom';
import Icon from './Icon.jsx';

/** Title, one line of help and the page's main actions. */
export function PageHeader({ title, subtitle, back, children }) {
  return (
    <>
      {back && <Link className="back-link" to={back.to}><Icon name="arrowLeft" size={16} />{back.label}</Link>}
      <header className="page-header">
        <div>
          <h1>{title}</h1>
          {subtitle && <p>{subtitle}</p>}
        </div>
        {children && <div className="page-actions">{children}</div>}
      </header>
    </>
  );
}

/** Green confirmation after an action. */
export function Notice({ children }) {
  if (!children) return null;
  return <p className="notice" role="status"><Icon name="check" size={16} />{children}</p>;
}

export function Empty({ icon = 'sparkle', title, children }) {
  return (
    <div className="empty">
      <Icon name={icon} size={30} />
      <strong>{title}</strong>
      {children && <span>{children}</span>}
    </div>
  );
}

/** Coloured status pill; tone is a status code such as AVAILABLE or IN_PROGRESS. */
export function Badge({ tone, children }) {
  return <span className={`badge tone-${tone}`}>{children}</span>;
}
