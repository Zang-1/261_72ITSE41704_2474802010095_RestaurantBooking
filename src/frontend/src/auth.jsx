import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api, setUnauthorizedHandler } from './api.js';

const AuthContext = createContext(null);

/** Keeps the logged-in user ({ username, role }) for the whole app (UC-01). */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null));
    api.get('/auth/me')
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setChecking(false));
  }, []);

  const login = useCallback(async (username, password) => {
    const loggedIn = await api.post('/auth/login', { username, password });
    setUser(loggedIn);
    return loggedIn;
  }, []);

  const logout = useCallback(async () => {
    await api.post('/auth/logout').catch(() => {});
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, checking, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}

/** The first screen of each role: floor overview, or kitchen display for Kitchen Staff. */
export function homePath(role) {
  return role === 'KITCHEN_STAFF' ? '/kitchen' : '/floor';
}

/** Shows the page only to the given roles. The server checks the role again on every request. */
export function RequireRole({ roles, children }) {
  const { user, checking } = useAuth();
  if (checking) return <p className="muted page">Loading…</p>;
  if (!user) return <Navigate to="/login" replace />;
  if (!roles.includes(user.role)) return <Navigate to={homePath(user.role)} replace />;
  return children;
}
