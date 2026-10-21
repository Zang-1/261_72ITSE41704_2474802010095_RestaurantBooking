import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';

const ROLE_LABELS = { WAITER_HOST: 'Waiter/Host', KITCHEN_STAFF: 'Kitchen Staff', MANAGER: 'Manager' };

/** Top bar with the links the current role may use. */
export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  const role = user?.role;
  const frontOfHouse = role === 'WAITER_HOST' || role === 'MANAGER';

  return (
    <>
      <header className="topbar">
        <span className="brand">Restaurant Booking</span>
        {user && (
          <nav>
            {frontOfHouse && <NavLink to="/floor">Floor</NavLink>}
            {frontOfHouse && <NavLink to="/reservations">Reservations</NavLink>}
            {frontOfHouse && <NavLink to="/waitlist">Waiting list</NavLink>}
            {role === 'MANAGER' && <NavLink to="/tables">Tables</NavLink>}
            {(role === 'KITCHEN_STAFF' || role === 'MANAGER') && <NavLink to="/kitchen">Kitchen</NavLink>}
          </nav>
        )}
        {user && (
          <span className="user">
            {user.username} · {ROLE_LABELS[user.role]}
            <button type="button" className="link" onClick={handleLogout}>Log out</button>
          </span>
        )}
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}
