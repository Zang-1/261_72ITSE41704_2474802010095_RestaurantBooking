import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { FormError } from '../components/FormField.jsx';
import ReservationForm from '../components/ReservationForm.jsx';
import { RESERVATION_STATUS_LABELS, timeOf, todayIso } from '../format.js';

/** UC-03 Create reservation and UC-04 Modify or cancel a reservation. */
export default function ReservationsPage() {
  const [date, setDate] = useState(todayIso());
  const [reservations, setReservations] = useState([]);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(null); // null = closed, {} = new, reservation = edit
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    setError(null);
    api.get(`/reservations?date=${date}`).then(setReservations).catch(setError);
  }, [date]);

  useEffect(load, [load]);

  async function handleCancel(reservation) {
    if (!window.confirm(`Cancel reservation ${reservation.code} for ${reservation.guestName}?`)) return;
    try {
      await api.post(`/reservations/${reservation.id}/cancel`);
      setMessage(`Reservation ${reservation.code} was cancelled.`);
      load();
    } catch (err) {
      setError(err);
    }
  }

  function handleSaved(saved, isNew) {
    setEditing(null);
    setMessage(isNew
      ? `Reservation ${saved.code} confirmed for ${saved.guestName}, table ${saved.tableNumber}.`
      : `Reservation ${saved.code} was updated.`);
    setDate(saved.startTime.slice(0, 10));
    load();
  }

  return (
    <>
      <div className="page-head">
        <h1>Reservations</h1>
        <button type="button" className="primary" onClick={() => { setMessage(''); setEditing({}); }}>
          New reservation
        </button>
      </div>

      {editing && (
        <ReservationForm
          reservation={editing.id ? editing : null}
          onSaved={handleSaved}
          onClose={() => setEditing(null)}
        />
      )}

      {message && <p className="notice">{message}</p>}
      <FormError error={error} />

      <label className="inline-field">
        Date <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </label>

      {reservations.length === 0 ? (
        <p className="muted">No reservations on this date.</p>
      ) : (
        <table className="data">
          <thead>
            <tr>
              <th>Time</th><th>Code</th><th>Table</th><th>Guest</th><th>Phone</th>
              <th>Party</th><th>Note</th><th>Status</th><th />
            </tr>
          </thead>
          <tbody>
            {reservations.map((r) => (
              <tr key={r.id} className={r.status === 'CANCELLED' ? 'dimmed' : ''}>
                <td>{timeOf(r.startTime)}</td>
                <td><code>{r.code}</code></td>
                <td>{r.tableNumber}</td>
                <td>{r.guestName}</td>
                <td>{r.phone}</td>
                <td>{r.partySize}</td>
                <td>{r.note}</td>
                <td>{RESERVATION_STATUS_LABELS[r.status]}</td>
                <td className="actions">
                  {r.status === 'CONFIRMED' && (
                    <>
                      <button type="button" onClick={() => { setMessage(''); setEditing(r); }}>Edit</button>
                      <button type="button" className="danger" onClick={() => handleCancel(r)}>Cancel</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
