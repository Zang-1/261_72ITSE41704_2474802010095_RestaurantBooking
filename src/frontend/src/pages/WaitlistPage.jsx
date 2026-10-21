import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import FormField, { FormError } from '../components/FormField.jsx';
import { formatDateTime } from '../format.js';

const EMPTY = { partyName: '', phone: '', partySize: '2' };

/** UC-05 Manage walk-in waiting list. Seating a party is added in Week 7 (FR-08, BR-04). */
export default function WaitlistPage() {
  const [entries, setEntries] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    api.get('/waitlist').then(setEntries).catch(setError);
  }, []);

  useEffect(load, [load]);

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));
  const fieldErrors = error?.fieldErrors || {};

  async function handleAdd(event) {
    event.preventDefault();
    setError(null);
    try {
      const added = await api.post('/waitlist', { ...form, partySize: Number(form.partySize) });
      setMessage(`${added.partyName} added at position ${added.position}.`);
      setForm(EMPTY);
      load();
    } catch (err) {
      setError(err);
    }
  }

  async function handleRemove(entry) {
    if (!window.confirm(`Remove ${entry.partyName} from the waiting list?`)) return;
    try {
      await api.del(`/waitlist/${entry.id}`);
      setMessage(`${entry.partyName} was removed.`);
      load();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <>
      <h1>Waiting list</h1>
      <form className="card" onSubmit={handleAdd} noValidate>
        <h2>Add a walk-in party</h2>
        <div className="row">
          <FormField label="Name" error={fieldErrors.partyName}>
            <input value={form.partyName} onChange={update('partyName')} maxLength={100} />
          </FormField>
          <FormField label="Phone" error={fieldErrors.phone}>
            <input value={form.phone} onChange={update('phone')} placeholder="0901234567" maxLength={15} />
          </FormField>
          <FormField label="Party size" error={fieldErrors.partySize}>
            <input type="number" min="1" max="20" value={form.partySize} onChange={update('partySize')} />
          </FormField>
          <button type="submit" className="primary">Add to waiting list</button>
        </div>
      </form>

      {message && <p className="notice">{message}</p>}
      <FormError error={error && !Object.keys(fieldErrors).length ? error : null} />

      {entries.length === 0 ? (
        <p className="muted">Nobody is waiting.</p>
      ) : (
        <table className="data">
          <thead>
            <tr><th>#</th><th>Name</th><th>Party</th><th>Phone</th><th>Arrived</th><th /></tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.id}>
                <td>{e.position}</td>
                <td>{e.partyName}</td>
                <td>{e.partySize}</td>
                <td>{e.phone}</td>
                <td>{formatDateTime(e.arrivedAt)}</td>
                <td className="actions">
                  <button type="button" className="danger" onClick={() => handleRemove(e)}>Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
