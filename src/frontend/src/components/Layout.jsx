import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { homePath, useAuth } from '../auth.jsx';
import { usePrefs } from '../prefs.jsx';
import Icon from './Icon.jsx';

export const ROLE_LABELS = { WAITER_HOST: 'Waiter/Host', KITCHEN_STAFF: 'Kitchen Staff', MANAGER: 'Manager' };

/** Sidebar with the screens the current role may use, the theme and language switches, and the page. */
export default function Layout() {
  const { user, logout } = useAuth();
  const { t } = usePrefs();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => setMenuOpen(false), [location.pathname]);

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  const role = user?.role;
  const frontOfHouse = role === 'WAITER_HOST' || role === 'MANAGER';
  const manager = role === 'MANAGER';
  const kitchen = role === 'KITCHEN_STAFF' || manager;

  return (
    <div className={`app${menuOpen ? ' menu-open' : ''}`}>
      <a className="sr-only" href="#main">{t('Skip to content')}</a>
      <aside className="sidebar" aria-label={t('Main navigation')}>
        <Link className="brand" to={user ? homePath(user.role) : '/login'}>
          <span className="brand-mark">EO</span>
          <span className="brand-name"><strong>Ember &amp; Oak</strong><span>{t('Steak & Pasta · Staff')}</span></span>
        </Link>

        {user && (
          <nav className="nav">
            {frontOfHouse && (
              <>
                <span className="nav-group">{t('Front of house')}</span>
                <NavItem to="/floor" icon="floor" label={t('Floor')} />
                <NavItem to="/reservations" icon="calendar" label={t('Reservations')} />
                <NavItem to="/waitlist" icon="queue" label={t('Waiting list')} />
              </>
            )}
            {kitchen && (
              <>
                <span className="nav-group">{t('Kitchen')}</span>
                <NavItem to="/kitchen" icon="chef" label={t('Kitchen display')} />
              </>
            )}
            {manager && (
              <>
                <span className="nav-group">{t('Management')}</span>
                <NavItem to="/tables" icon="table" label={t('Tables')} />
                <NavItem to="/menu" icon="menu" label={t('Menu')} />
                <NavItem to="/shifts" icon="clock" label={t('Shifts')} />
              </>
            )}
          </nav>
        )}

        <div className="sidebar-foot">
          <Prefs />
          {user && (
            <div className="user-card">
              <span className="avatar">{user.username.slice(0, 2)}</span>
              <span className="who"><strong>{user.username}</strong><span>{t(ROLE_LABELS[user.role])}</span></span>
              <button type="button" className="icon-btn" onClick={handleLogout} title={t('Log out')} aria-label={t('Log out')}>
                <Icon name="logout" />
              </button>
            </div>
          )}
        </div>
      </aside>
      {menuOpen && <button type="button" className="scrim" aria-label={t('Close menu')} onClick={() => setMenuOpen(false)} />}

      <div className="main">
        <div className="mobile-bar">
          <button type="button" className="icon-btn" aria-label={t('Open menu')} aria-expanded={menuOpen}
            onClick={() => setMenuOpen(true)}>
            <Icon name="hamburger" />
          </button>
          <strong>Ember &amp; Oak</strong>
        </div>
        <main id="main" className="page">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function NavItem({ to, icon, label }) {
  return (
    <NavLink to={to} className="nav-link">
      <Icon name={icon} />
      {label}
    </NavLink>
  );
}

/** Theme and language switches; also used on the login screen. */
export function Prefs({ className = 'prefs', buttonClass = 'pref-btn' }) {
  const { t, theme, lang, toggleTheme, toggleLang } = usePrefs();
  return (
    <div className={className}>
      <button type="button" className={buttonClass} onClick={toggleTheme}
        aria-label={theme === 'dark' ? t('Switch to light theme') : t('Switch to dark theme')}>
        <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={16} />
        <span className="pref-label">{theme === 'dark' ? t('Light') : t('Dark')}</span>
      </button>
      <button type="button" className={buttonClass} onClick={toggleLang}
        aria-label={lang === 'vi' ? 'Switch to English' : 'Chuyển sang tiếng Việt'}>
        <Icon name="globe" size={16} />
        <span className="pref-label">{lang === 'vi' ? 'English' : 'Tiếng Việt'}</span>
      </button>
    </div>
  );
}
