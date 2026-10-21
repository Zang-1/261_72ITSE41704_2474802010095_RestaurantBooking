import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { FormError } from '../components/FormField.jsx';
import { STATUS_LABELS, groupBySection } from '../format.js';

/** Floor overview: every active table with its section, capacity and status (FR-10). */
export default function FloorPage() {
  const [tables, setTables] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.get('/tables').then(setTables).catch(setError);
  }, []);

  return (
    <>
      <h1>Floor overview</h1>
      <div className="legend">
        {Object.entries(STATUS_LABELS).map(([status, label]) => (
          <span key={status} className={`badge status-${status}`}>{label}</span>
        ))}
      </div>
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
              </div>
            ))}
          </div>
        </section>
      ))}
    </>
  );
}
