import { useEffect, useState } from 'react';
import { api } from '../api.js';
import FormField, { FormError } from './FormField.jsx';
import { timeOf, todayIso } from '../format.js';

/**
 * Three steps (NFR-05): 1. search by date, time and party size; 2. pick a table; 3. guest details and Confirm.
 * Used for both a new reservation and editing an existing one.
 */
export default function ReservationForm({ reservation, onSaved, onClose }) {
  const isNew = !reservation;
  const [form, setForm] = useState(() => ({
    date: reservation ? reservation.startTime.slice(0, 10) : todayIso(),
    startTime: reservation ? timeOf(reservation.startTime) : '19:00',
    partySize: reservation ? String(reservation.partySize) : '2',
    tableId: reservation ? reservation.tableId : null,
    guestName: reservation?.guestName || '',
    phone: reservation?.phone || '',
    note: reservation?.note || '',
  }));
  const [tables, setTables] = useState(null);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const fieldErrors = error?.fieldErrors || {};
  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  async function search() {
    setError(null);
    try {
      const params = new URLSearchParams({ date: form.date, startTime: form.startTime, partySize: form.partySize });
      const found = await api.get(`/reservations/available-tables?${params}`);
      setTables(found);
      if (!found.some((t) => t.id === form.tableId)) {
        setForm((f) => ({ ...f, tableId: found[0]?.id ?? null }));
      }
    } catch (err) {
      setTables(null);
      setError(err);
    }
  }

  // When editing, show the tables that fit straight away
  useEffect(() => {
    if (!isNew) search();
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    const body = {
      tableId: form.tableId,
      guestName: form.guestName,
      phone: form.phone,
      partySize: Number(form.partySize),
      date: form.date,
      startTime: form.startTime,
      note: form.note,
    };
    try {
      const saved = isNew
        ? await api.post('/reservations', body)
        : await api.put(`/reservations/${reservation.id}`, body);
      onSaved(saved, isNew);
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card" onSubmit={handleSubmit} noValidate>
      <div className="page-head">
        <h2>{isNew ? 'New reservation' : `Edit reservation ${reservation.code}`}</h2>
        <button type="button" className="link" onClick={onClose}>Close</button>
      </div>

      <h3>1. When and how many</h3>
      <div className="row">
        <FormField label="Date" error={fieldErrors.date}>
          <input type="date" value={form.date} onChange={update('date')} />
        </FormField>
        <FormField label="Start time" error={fieldErrors.startTime}>
          <input type="time" value={form.startTime} onChange={update('startTime')} />
        </FormField>
        <FormField label="Party size" error={fieldErrors.partySize}>
          <input type="number" min="1" max="20" value={form.partySize} onChange={update('partySize')} />
        </FormField>
        <button type="button" onClick={search}>Find tables</button>
      </div>

      {tables && (
        <>
          <h3>2. Table</h3>
          {tables.length === 0 ? (
            <p className="muted">No table available for this time. Try another time or use the waiting list.</p>
          ) : (
            <div className="choice-list">
              {tables.map((t) => (
                <label key={t.id} className={`choice${form.tableId === t.id ? ' selected' : ''}`}>
                  <input type="radio" name="table" checked={form.tableId === t.id}
                         onChange={() => setForm((f) => ({ ...f, tableId: t.id }))} />
                  Table {t.number} · {t.capacity} seats · {t.sectionName}
                </label>
              ))}
            </div>
          )}
          {fieldErrors.tableId && <small className="error-text">{fieldErrors.tableId}</small>}

          <h3>3. Guest</h3>
          <div className="row">
            <FormField label="Guest name" error={fieldErrors.guestName}>
              <input value={form.guestName} onChange={update('guestName')} maxLength={100} />
            </FormField>
            <FormField label="Phone" error={fieldErrors.phone}>
              <input value={form.phone} onChange={update('phone')} placeholder="0901234567" maxLength={15} />
            </FormField>
          </div>
          <FormField label="Note (optional)" error={fieldErrors.note}>
            <input value={form.note} onChange={update('note')} maxLength={200} />
          </FormField>
        </>
      )}

      <FormError error={error} />
      {tables && tables.length > 0 && (
        <button type="submit" className="primary" disabled={saving || !form.tableId}>
          {saving ? 'Saving…' : isNew ? 'Confirm reservation' : 'Save changes'}
        </button>
      )}
    </form>
  );
}
