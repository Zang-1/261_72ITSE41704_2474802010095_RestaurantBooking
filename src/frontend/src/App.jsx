import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import { RequireRole, homePath, useAuth } from './auth.jsx';
import LoginPage from './pages/LoginPage.jsx';
import FloorPage from './pages/FloorPage.jsx';
import ReservationsPage from './pages/ReservationsPage.jsx';
import WaitlistPage from './pages/WaitlistPage.jsx';
import TablesPage from './pages/TablesPage.jsx';
import KitchenPage from './pages/KitchenPage.jsx';

const FRONT_OF_HOUSE = ['WAITER_HOST', 'MANAGER'];

export default function App() {
  const { user, checking } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<Layout />}>
        <Route path="/floor" element={<RequireRole roles={FRONT_OF_HOUSE}><FloorPage /></RequireRole>} />
        <Route path="/reservations" element={<RequireRole roles={FRONT_OF_HOUSE}><ReservationsPage /></RequireRole>} />
        <Route path="/waitlist" element={<RequireRole roles={FRONT_OF_HOUSE}><WaitlistPage /></RequireRole>} />
        <Route path="/tables" element={<RequireRole roles={['MANAGER']}><TablesPage /></RequireRole>} />
        <Route path="/kitchen" element={<RequireRole roles={['KITCHEN_STAFF', 'MANAGER']}><KitchenPage /></RequireRole>} />
      </Route>
      <Route
        path="*"
        element={checking ? null : <Navigate to={user ? homePath(user.role) : '/login'} replace />}
      />
    </Routes>
  );
}
