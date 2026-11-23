import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import { useDialog } from '../components/Dialog.jsx';
import FormField, { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Empty, Notice, PageHeader } from '../components/ui.jsx';
import { minutesSince, timeOf } from '../format.js';

const EMPTY = { partyName: '', phone: '', partySize: '2' };

/** UC-05 Manage walk-in waiting list; parties are seated from the floor overview (FR-08, BR-04). */
export default function WaitlistPage() {
  const t = useT();
  const ask = useDialog();
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
    setMessage('');
    try {
      const added = await api.post('/waitlist', { ...form, partySize: Number(form.partySize) });
      setMessage(t('{name} added at position {n}.', { name: added.partyName, n: added.position }));
      setForm(EMPTY);
      load();
    } catch (err) {
      setError(err);
    }
  }

  async function handleRemove(entry) {
    const yes = await ask({
      title: t('Remove {name} from the waiting list?', { name: entry.partyName }),
      message: t('The parties behind them move up one place.'),
      confirmLabel: t('Remove'),
      danger: true,
    });
    if (!yes) return;
    setMessage('');
    try {
      await api.del(`/waitlist/${entry.id}`);
      setMessage(t('{name} was removed.', { name: entry.partyName }));
      load();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <>
      <PageHeader title={t('Waiting list')}
        subtitle={t('Walk-in parties in arrival order. When a table frees up, "Seat next waiting" on the floor seats the earliest party that fits.')}>
        <Link className="btn" to="/floor"><Icon name="floor" size={16} />{t('Floor overview')}</Link>
      </PageHeader>

      <form className="card" onSubmit={handleAdd} noValidate>
        <div className="card-head"><h2>{t('Add a walk-in party')}</h2></div>
        <div className="form-row">
          <FormField label={t('Name')} error={fieldErrors.partyName}>
            <input value={form.partyName} onChange={update('partyName')} maxLength={100} />
          </FormField>
          <FormField label={t('Phone')} error={fieldErrors.phone}>
            <input value={form.phone} onChange={update('phone')} placeholder="0901234567" maxLength={15} />
          </FormField>
          <FormField label={t('Party size')} error={fieldErrors.partySize}>
            <input type="number" min="1" max="20" value={form.partySize} onChange={update('partySize')} />
          </FormField>
          <button type="submit" className="btn btn-primary"><Icon name="plus" size={16} />{t('Add to waiting list')}</button>
        </div>
      </form>

      <Notice>{message}</Notice>
      <FormError error={error && !Object.keys(fieldErrors).length ? error : null} />

      {entries.length === 0 ? (
        <Empty icon="queue" title={t('Nobody is waiting')}>{t('Walk-in parties you add appear here in arrival order.')}</Empty>
      ) : (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th className="num">#</th><th>{t('Name')}</th><th className="num">{t('Party')}</th><th>{t('Phone')}</th>
                <th>{t('Arrived')}</th><th><span className="sr-only">{t('Actions')}</span></th>
              </tr>
            </thead>
            <tbody>
              {entries.map((e) => (
                <tr key={e.id}>
                  <td className="num"><strong>{e.position}</strong></td>
                  <td>{e.partyName}</td>
                  <td className="num">{e.partySize}</td>
                  <td>{e.phone}</td>
                  <td>{timeOf(e.arrivedAt)}<span className="cell-sub">{t('waiting {n} min', { n: minutesSince(e.arrivedAt) })}</span></td>
                  <td className="actions">
                    <div><button type="button" className="btn btn-sm btn-danger" onClick={() => handleRemove(e)}>{t('Remove')}</button></div>
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
