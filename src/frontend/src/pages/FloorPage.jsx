import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import { useDialog } from '../components/Dialog.jsx';
import { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Badge, Notice, PageHeader } from '../components/ui.jsx';
import { STATUS_LABELS, groupBySection } from '../format.js';

/** Floor overview: every active table with its section, capacity and status (FR-10), plus the BR-05 actions. */
export default function FloorPage() {
  const t = useT();
  const ask = useDialog();
  const [tables, setTables] = useState([]);
  const [section, setSection] = useState('');
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

  async function seatWalkIn(table) {
    const answer = await ask({
      title: t('Seat a walk-in at table {n}', { n: table.number }),
      message: t('This table seats up to {n} guests.', { n: table.capacity }),
      input: { label: t('Party size'), type: 'number', min: 1, max: table.capacity, value: '2' },
      confirmLabel: t('Seat party'),
    });
    if (answer === null) return;
    act(`/tables/${table.id}/seat-walk-in`, { partySize: Number(answer) },
      () => t('Walk-in party seated at table {n}.', { n: table.number }));
  }

  const count = (status) => tables.filter((table) => table.status === status).length;
  const groups = groupBySection(tables);
  const shown = section ? groups.filter(([name]) => name === section) : groups;

  return (
    <>
      <PageHeader title={t('Floor overview')}
        subtitle={t('Every table and what it needs next. Seat walk-ins, open orders and turn tables around from here.')}>
        <Link className="btn" to="/waitlist"><Icon name="queue" size={16} />{t('Waiting list')}</Link>
        <Link className="btn btn-primary" to="/reservations"><Icon name="plus" size={16} />{t('New reservation')}</Link>
      </PageHeader>

      <div className="stats">
        {Object.keys(STATUS_LABELS).map((status) => (
          <div key={status} className={`stat tone-line-${status}`}>
            <span className="stat-label">{t(STATUS_LABELS[status])}</span>
            <span className="stat-value">{count(status)}</span>
          </div>
        ))}
      </div>

      <Notice>{message}</Notice>
      <FormError error={error} />

      <div className="toolbar">
        <div className="segmented" role="group" aria-label={t('Section')}>
          <button type="button" className="btn btn-sm" aria-pressed={section === ''} onClick={() => setSection('')}>
            {t('All sections')}
          </button>
          {groups.map(([name]) => (
            <button key={name} type="button" className="btn btn-sm" aria-pressed={section === name} onClick={() => setSection(name)}>
              {name}
            </button>
          ))}
        </div>
        <span className="muted">{t('{n} tables', { n: tables.length })}</span>
      </div>

      {shown.map(([name, sectionTables]) => (
        <section key={name}>
          <div className="section-title">
            <h2>{name}</h2>
            <span className="muted">{t('{free} of {all} available', { free: sectionTables.filter((x) => x.status === 'AVAILABLE').length, all: sectionTables.length })}</span>
          </div>
          <div className="table-grid">
            {sectionTables.map((table) => (
              <article key={table.id} className={`table-tile status-${table.status}`}>
                <div className="tile-top">
                  <div>
                    <div className="tile-number">{table.number}</div>
                    <span className="tile-seats"><Icon name="users" size={14} />{t('{n} seats', { n: table.capacity })}</span>
                  </div>
                  <Badge tone={table.status}>{t(STATUS_LABELS[table.status])}</Badge>
                </div>
                <div className="tile-actions">
                  {table.status === 'AVAILABLE' && (
                    <>
                      <button type="button" className="btn btn-sm" onClick={() => seatWalkIn(table)}>{t('Seat walk-in')}</button>
                      <button type="button" className="btn btn-sm btn-ghost" onClick={() => act(`/waitlist/seat-next?tableId=${table.id}`, undefined,
                        (p) => t('{name} ({n}) seated at table {table}.', { name: p.partyName, n: p.partySize, table: p.tableNumber }))}>
                        {t('Seat next waiting')}
                      </button>
                    </>
                  )}
                  {table.status === 'RESERVED' && (
                    <Link className="btn btn-sm" to="/reservations">{t('See booking')}</Link>
                  )}
                  {table.status === 'OCCUPIED' && (
                    <>
                      <Link className="btn btn-sm btn-primary" to={`/order/${table.id}`}>{t('Order')}</Link>
                      <Link className="btn btn-sm" to={`/bill/${table.id}`}>{t('Bill')}</Link>
                      <button type="button" className="btn btn-sm btn-ghost" onClick={() => act(`/tables/${table.id}/needs-cleaning`, undefined,
                        () => t('Table {n} needs cleaning.', { n: table.number }))}>{t('Guests left')}</button>
                    </>
                  )}
                  {table.status === 'NEEDS_CLEANING' && (
                    <button type="button" className="btn btn-sm btn-primary" onClick={() => act(`/tables/${table.id}/clean`, undefined,
                      () => t('Table {n} is clean and available.', { n: table.number }))}>
                      <Icon name="check" size={16} />{t('Mark clean')}
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </section>
      ))}
    </>
  );
}
