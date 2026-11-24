import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import FormField, { FormError } from './FormField.jsx';
import Icon from './Icon.jsx';
import { timeOf, todayIso } from '../format.js';

/**
 * Three steps (NFR-05): 1. search by date, time and party size; 2. pick a table; 3. guest details and Confirm.
 * Used for both a new reservation and editing an existing one.
 */
export default function ReservationForm({ reservation, onSaved, onClose }) {
  const t = useT();
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
      if (!found.some((table) => table.id === form.tableId)) {
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
      <div className="card-head">
        <h2>{isNew ? t('New reservation') : t('Edit reservation {code}', { code: reservation.code })}</h2>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}><Icon name="x" size={16} />{t('Close')}</button>
      </div>

      <div className="steps">
        <div className="step">
          <span className="step-no">1</span>
          <div className="step-body">
            <h3>{t('When and how many')}</h3>
            <div className="form-row">
              <FormField label={t('Date')} error={fieldErrors.date}>
                <input type="date" value={form.date} onChange={update('date')} />
              </FormField>
              <FormField label={t('Start time')} error={fieldErrors.startTime}>
                <input type="time" value={form.startTime} onChange={update('startTime')} />
              </FormField>
              <FormField label={t('Party size')} error={fieldErrors.partySize}>
                <input type="number" min="1" max="20" value={form.partySize} onChange={update('partySize')} />
              </FormField>
              <button type="button" className="btn" onClick={search}><Icon name="search" size={16} />{t('Find tables')}</button>
            </div>
          </div>
        </div>

        {tables && (
          <>
            <div className="step">
              <span className="step-no">2</span>
              <div className="step-body">
                <h3>{t('Table')}</h3>
                {tables.length === 0 ? (
                  <p className="muted">{t('No table is free at this time. Try another time or add the party to the waiting list.')}</p>
                ) : (
                  <div className="choice-grid">
                    {tables.map((table) => (
                      <label key={table.id} className={`choice${form.tableId === table.id ? ' selected' : ''}`}>
                        <input type="radio" name="table" checked={form.tableId === table.id}
                          onChange={() => setForm((f) => ({ ...f, tableId: table.id }))} />
                        <span className="choice-text">
                          <strong>{t('Table {n}', { n: table.number })}</strong>
                          <span>{t('{n} seats', { n: table.capacity })} · {table.sectionName}</span>
                        </span>
                      </label>
                    ))}
                  </div>
                )}
                {fieldErrors.tableId && <small className="error-text">{t(fieldErrors.tableId)}</small>}
              </div>
            </div>

            <div className="step">
              <span className="step-no">3</span>
              <div className="step-body">
                <h3>{t('Guest')}</h3>
                <div className="form-row">
                  <FormField label={t('Guest name')} error={fieldErrors.guestName}>
                    <input value={form.guestName} onChange={update('guestName')} maxLength={100} />
                  </FormField>
                  <FormField label={t('Phone')} error={fieldErrors.phone}>
                    <input value={form.phone} onChange={update('phone')} placeholder="0901234567" maxLength={15} />
                  </FormField>
                </div>
                <FormField label={t('Note (optional)')} error={fieldErrors.note}>
                  <input value={form.note} onChange={update('note')} maxLength={200} placeholder={t('e.g. birthday, window seat')} />
                </FormField>
              </div>
            </div>
          </>
        )}
      </div>

      <FormError error={error} />
      {tables && tables.length > 0 && (
        <button type="submit" className="btn btn-primary" disabled={saving || !form.tableId}>
          <Icon name="check" size={16} />
          {saving ? t('Saving…') : isNew ? t('Confirm reservation') : t('Save changes')}
        </button>
      )}
    </form>
  );
}
