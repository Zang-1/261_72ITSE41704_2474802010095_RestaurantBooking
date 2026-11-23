import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import { useDialog } from '../components/Dialog.jsx';
import { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import ReservationForm from '../components/ReservationForm.jsx';
import { Badge, Empty, Notice, PageHeader } from '../components/ui.jsx';
import { RESERVATION_STATUS_LABELS, timeOf, todayIso } from '../format.js';

/** UC-03 Create reservation and UC-04 Modify, cancel or check in a reservation. */
export default function ReservationsPage() {
  const t = useT();
  const ask = useDialog();
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
    const yes = await ask({
      title: t('Cancel reservation {code}?', { code: reservation.code }),
      message: t('{name}, {n} guests at {time}. The table is released.', { name: reservation.guestName, n: reservation.partySize, time: timeOf(reservation.startTime) }),
      confirmLabel: t('Cancel reservation'),
      danger: true,
    });
    if (!yes) return;
    setMessage('');
    try {
      await api.post(`/reservations/${reservation.id}/cancel`);
      setMessage(t('Reservation {code} was cancelled.', { code: reservation.code }));
      load();
    } catch (err) {
      setError(err);
    }
  }

  async function handleCheckIn(reservation) {
    setMessage('');
    try {
      const saved = await api.post(`/reservations/${reservation.id}/check-in`);
      setMessage(t('{name} checked in at table {n}.', { name: saved.guestName, n: saved.tableNumber }));
      load();
    } catch (err) {
      setError(err);
    }
  }

  function handleSaved(saved, isNew) {
    setEditing(null);
    setMessage(isNew
      ? t('Reservation {code} confirmed for {name}, table {n}.', { code: saved.code, name: saved.guestName, n: saved.tableNumber })
      : t('Reservation {code} was updated.', { code: saved.code }));
    setDate(saved.startTime.slice(0, 10));
    load();
  }

  const active = reservations.filter((r) => r.status === 'CONFIRMED');
  const guests = active.reduce((sum, r) => sum + r.partySize, 0);

  return (
    <>
      <PageHeader title={t('Reservations')}
        subtitle={t('Book a table in three steps. Guests can check in from 30 minutes before their time.')}>
        <button type="button" className="btn btn-primary" onClick={() => { setMessage(''); setEditing({}); }}>
          <Icon name="plus" size={16} />{t('New reservation')}
        </button>
      </PageHeader>

      {editing && (
        <ReservationForm
          reservation={editing.id ? editing : null}
          onSaved={handleSaved}
          onClose={() => setEditing(null)}
        />
      )}

      <Notice>{message}</Notice>
      <FormError error={error} />

      <div className="toolbar">
        <label className="inline-field">
          <Icon name="calendar" size={16} />{t('Date')}
          <input className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <span className="muted">{t('{n} confirmed, {g} guests expected', { n: active.length, g: guests })}</span>
      </div>

      {reservations.length === 0 ? (
        <Empty icon="calendar" title={t('No reservations on this date')}>{t('Pick another date or add a new reservation.')}</Empty>
      ) : (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>{t('Time')}</th><th>{t('Guest')}</th><th>{t('Table')}</th><th className="num">{t('Party')}</th>
                <th>{t('Note')}</th><th>{t('Status')}</th><th><span className="sr-only">{t('Actions')}</span></th>
              </tr>
            </thead>
            <tbody>
              {reservations.map((r) => (
                <tr key={r.id} className={r.status === 'CONFIRMED' || r.status === 'CHECKED_IN' ? '' : 'dimmed'}>
                  <td><strong>{timeOf(r.startTime)}</strong><span className="cell-sub"><code>{r.code}</code></span></td>
                  <td>{r.guestName}<span className="cell-sub">{r.phone}</span></td>
                  <td>{t('Table {n}', { n: r.tableNumber })}</td>
                  <td className="num">{r.partySize}</td>
                  <td>{r.note}</td>
                  <td><Badge tone={r.status}>{t(RESERVATION_STATUS_LABELS[r.status])}</Badge></td>
                  <td className="actions">
                    {r.status === 'CONFIRMED' && (
                      <div>
                        <button type="button" className="btn btn-sm btn-primary" onClick={() => handleCheckIn(r)}>{t('Check in')}</button>
                        <button type="button" className="btn btn-sm" onClick={() => { setMessage(''); setEditing(r); }}>
                          <Icon name="edit" size={14} />{t('Edit')}
                        </button>
                        <button type="button" className="btn btn-sm btn-danger" onClick={() => handleCancel(r)}>{t('Cancel')}</button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
