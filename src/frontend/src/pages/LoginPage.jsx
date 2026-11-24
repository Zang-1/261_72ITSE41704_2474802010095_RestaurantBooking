import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { homePath, useAuth } from '../auth.jsx';
import { useT } from '../prefs.jsx';
import FormField, { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Prefs } from '../components/Layout.jsx';

/** UC-01 Log in. */
export default function LoginPage() {
  const { user, login } = useAuth();
  const t = useT();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={homePath(user.role)} replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const loggedIn = await login(username, password);
      navigate(homePath(loggedIn.role));
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  }

  const fieldErrors = error?.fieldErrors || {};

  return (
    <div className="login">
      <section className="login-brand">
        <div className="brand">
          <Link to="/" className="brand-mark" aria-label={t('Back to the guest website')}>EO</Link>
          <span className="brand-name"><strong>Ember &amp; Oak</strong><span>{t('Steak & Pasta · Staff')}</span></span>
        </div>
        <div>
          <h1>{t('One screen for the whole evening service')}</h1>
          <p>{t('Bookings, walk-ins, orders, the kitchen line and the bill share one live view of every table.')}</p>
        </div>
        <ul className="login-points">
          <Point icon="calendar" title={t('No double bookings')} text={t('Each table is locked while it is being booked.')} />
          <Point icon="chef" title={t('Live kitchen display')} text={t('New orders reach the kitchen within 3 seconds.')} />
          <Point icon="wallet" title={t('Bills that add up')} text={t('Split evenly or by dish, pay by cash, card or e-wallet.')} />
        </ul>
      </section>

      <section className="login-panel">
        <Prefs className="login-prefs" buttonClass="btn btn-ghost btn-sm" />
        <form className="login-form" onSubmit={handleSubmit} noValidate>
          <h2>{t('Staff login')}</h2>
          <p className="muted">{t('Sign in with the account your manager gave you.')}</p>
          <FormError error={error && !Object.keys(fieldErrors).length ? error : null} />
          <FormField label={t('Username')} error={fieldErrors.username}>
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" autoFocus />
          </FormField>
          <FormField label={t('Password')} error={fieldErrors.password}>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password" />
          </FormField>
          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? t('Logging in…') : t('Log in')}
          </button>
          {/* shortcut: demo accounts shown for the course demo; remove before real use */}
          <p className="demo-accounts">
            {t('Demo accounts')}:<br />
            manager / Manager@2026 ({t('Manager')})<br />
            waiter / Waiter@2026 ({t('Waiter/Host')})<br />
            kitchen / Kitchen@2026 ({t('Kitchen Staff')})
          </p>
          <p className="muted" style={{ marginTop: '1.25rem', textAlign: 'center' }}>
            <Link to="/">{t('Guest? Book a table on our website')}</Link>
          </p>
        </form>
      </section>
    </div>
  );
}

function Point({ icon, title, text }) {
  return (
    <li>
      <span className="point-icon"><Icon name={icon} /></span>
      <div><strong>{title}</strong><span>{text}</span></div>
    </li>
  );
}
