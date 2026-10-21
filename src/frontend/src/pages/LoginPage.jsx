import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { homePath, useAuth } from '../auth.jsx';
import FormField, { FormError } from '../components/FormField.jsx';

/** UC-01 Log in. */
export default function LoginPage() {
  const { user, login } = useAuth();
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
    <div className="login-wrap">
      <form className="card login-card" onSubmit={handleSubmit} noValidate>
        <h1>Restaurant Booking</h1>
        <p className="muted">Staff login</p>
        <FormError error={error && !Object.keys(fieldErrors).length ? error : null} />
        <FormField label="Username" error={fieldErrors.username}>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" autoFocus />
        </FormField>
        <FormField label="Password" error={fieldErrors.password}>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 autoComplete="current-password" />
        </FormField>
        <button type="submit" className="primary" disabled={submitting}>
          {submitting ? 'Logging in…' : 'Log in'}
        </button>
      </form>
    </div>
  );
}
