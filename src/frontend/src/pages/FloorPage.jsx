import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { FormError } from '../components/FormField.jsx';
import { STATUS_LABELS, groupBySection } from '../format.js';

/** Floor overview: every active table with its section, capacity and status (FR-10), plus the BR-05 actions. */
export default function FloorPage() {
  const [tables, setTables] = useState([]);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    api.get('/tables').then(setTables).catch(setError);
  }, []);

  useEffect(load, [load]);

  async function act(path, body, done) {
    setError(null);
    setMessage('');
    try {
      const result = await api.post(path, body);
      setMessage(done(result));
      load();
    } catch (err) {
      setError(err);
    }
  }

  function seatWalkIn(table) {
    const answer = window.prompt(`Party size for table ${table.number} (max ${table.capacity})?`, '2');
    if (answer === null) return;
    act(`/tables/${table.id}/seat-walk-in`, { partySize: Number(answer) },
      () => `Walk-in party seated at table ${table.number}.`);
  }

  return (
    <>
      <h1>Floor overview</h1>
      <div className="legend">
        {Object.entries(STATUS_LABELS).map(([status, label]) => (
          <span key={status} className={`badge status-${status}`}>{label}</span>
        ))}
      </div>
      {message && <p className="notice">{message}</p>}
      <FormError error={error} />
      {groupBySection(tables).map(([section, sectionTables]) => (
        <section key={section}>
          <h2>{section}</h2>
          <div className="table-grid">
            {sectionTables.map((table) => (
              <div key={table.id} className={`table-tile status-${table.status}`}>
                <strong>Table {table.number}</strong>
                <span>{table.capacity} seats</span>
                <span className="tile-status">{STATUS_LABELS[table.status]}</span>
                {table.status === 'AVAILABLE' && (
                  <>
                    <button type="button" onClick={() => seatWalkIn(table)}>Seat walk-in</button>
                    <button type="button" onClick={() => act(`/waitlist/seat-next?tableId=${table.id}`, undefined,
                      (p) => `${p.partyName} (${p.partySize}) seated at table ${p.tableNumber}.`)}>
                      Seat next waiting
                    </button>
                  </>
                )}
                {table.status === 'OCCUPIED' && (
                  <button type="button" onClick={() => act(`/tables/${table.id}/needs-cleaning`, undefined,
                    () => `Table ${table.number} needs cleaning.`)}>Guests left</button>
                )}
                {table.status === 'NEEDS_CLEANING' && (
                  <button type="button" className="primary" onClick={() => act(`/tables/${table.id}/clean`, undefined,
                    () => `Table ${table.number} is clean and available.`)}>Mark clean</button>
                )}
              </div>
            ))}
          </div>
        </section>
      ))}
    </>
  );
}
