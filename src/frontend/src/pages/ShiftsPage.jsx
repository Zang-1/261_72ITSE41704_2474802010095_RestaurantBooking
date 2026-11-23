import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import FormField, { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Badge, Empty, Notice, PageHeader } from '../components/ui.jsx';
import { PAYMENT_METHOD_LABELS, SPLIT_LABELS, formatDateTime, formatPrice } from '../format.js';

/** Manager: UC-11 Open and close shift (FR-27, FR-29) and UC-15 Shift report (FR-28). */
export default function ShiftsPage() {
  const t = useT();
  const [shifts, setShifts] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [report, setReport] = useState(null);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    api.get('/shifts').then((all) => {
      setShifts(all);
      setSelectedId((current) => current ?? all[0]?.id ?? null);
    }).catch(setError);
  }, []);

  useEffect(load, [load]);

  useEffect(() => {
    if (selectedId) api.get(`/shifts/${selectedId}/report`).then(setReport).catch(setError);
    else setReport(null);
  }, [selectedId, shifts]);

  const current = shifts.find((s) => s.open);

  async function act(action, done) {
    setError(null);
    setMessage('');
    try {
      const shift = await action();
      setMessage(done);
      setSelectedId(shift.id);
      load();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <>
      <PageHeader title={t('Shifts')}
        subtitle={t('Payments belong to the shift that is open. Close the shift at the end of service to see its report.')}>
        {!current && (
          <button type="button" className="btn btn-primary" onClick={() => act(() => api.post('/shifts'), t('Shift opened.'))}>
            <Icon name="plus" size={16} />{t('Open shift')}
          </button>
        )}
      </PageHeader>
      <Notice>{message}</Notice>
      <FormError error={error} />

      {current ? (
        <section className="card">
          <CloseShiftForm key={current.id} shift={current}
            onClose={(countedCash) => act(() => api.post(`/shifts/${current.id}/close`, { countedCash }),
              t('Shift closed. Its report is shown below.'))} />
        </section>
      ) : (
        <p className="alert warning"><Icon name="alert" size={16} />{t('No shift is open. Waiters cannot take payments until a shift is opened (BR-13).')}</p>
      )}

      <div className="split-narrow">
        <section className="card">
          <div className="card-head"><h2>{t('All shifts')}</h2></div>
          {shifts.length === 0 && <p className="muted">{t('No shifts yet.')}</p>}
          <ul className="shift-list">
            {shifts.map((s) => (
              <li key={s.id}>
                <button type="button" className={s.id === selectedId ? 'selected' : ''} onClick={() => setSelectedId(s.id)}>
                  <strong>{formatDateTime(s.openedAt)}</strong>
                  <span>{s.open ? t('Open now') : t('to {time}', { time: formatDateTime(s.closedAt).slice(11) })}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
        {report ? <ShiftReport report={report} /> : <Empty icon="clock" title={t('No report yet')}>{t('Open a shift and record payments to see its report.')}</Empty>}
      </div>
    </>
  );
}

/** FR-29: the counted cash is optional; leave it empty to close without counting the till. */
function CloseShiftForm({ shift, onClose }) {
  const t = useT();
  const [counted, setCounted] = useState('');

  function submit(event) {
    event.preventDefault();
    onClose(counted === '' ? null : Number(counted));
  }

  return (
    <form onSubmit={submit}>
      <div className="card-head">
        <div>
          <h2>{t('Current shift')}</h2>
          <p className="muted">{t('Open since {time}, opened by {name}.', { time: formatDateTime(shift.openedAt), name: shift.openedBy })}</p>
        </div>
        <Badge tone="OPEN">{t('Open now')}</Badge>
      </div>
      <div className="form-row">
        <FormField label={t('Cash counted in the till (VND, optional)')}>
          <input type="number" min="0" value={counted} onChange={(e) => setCounted(e.target.value)} />
        </FormField>
        <button type="submit" className="btn btn-danger">{t('Close shift')}</button>
      </div>
      <p className="muted">{t('A shift cannot close while a bill is unpaid (BR-13).')}</p>
    </form>
  );
}

/** FR-28: closed bills, revenue, revenue per method, split bills; FR-29: expected and counted cash. */
function ShiftReport({ report }) {
  const t = useT();
  const { shift } = report;
  return (
    <section className="card">
      <div className="card-head">
        <h2>{t('Report')}: {formatDateTime(shift.openedAt)}{shift.closedAt ? ` – ${formatDateTime(shift.closedAt).slice(11)}` : ''}</h2>
        {report.provisional && <Badge tone="OPEN">{t('Provisional, shift still open')}</Badge>}
      </div>
      <div className="stats">
        <Stat label={t('Revenue')} value={formatPrice(report.revenue)} accent />
        <Stat label={t('Closed bills')} value={report.closedBills} />
        <Stat label={t('Split bills')} value={report.splitBills} />
        <Stat label={t('Payments')} value={report.payments} />
      </div>

      <h3>{t('Revenue by payment method')}</h3>
      <div className="table-wrap">
        <table className="data">
          <tbody>
            {Object.entries(PAYMENT_METHOD_LABELS).map(([method, label]) => {
              const amount = report.revenueByMethod[method] ?? 0;
              const share = report.revenue ? Math.round(amount * 100 / report.revenue) : 0;
              return (
                <tr key={method}>
                  <td>{t(label)}</td>
                  <td style={{ width: '40%' }}><div className="bar"><span style={{ width: `${share}%` }} /></div></td>
                  <td className="num">{formatPrice(amount)}</td>
                  <td className="num muted">{share}%</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <h3>{t('Cash check')}</h3>
      <dl className="totals">
        <dt>{t('Expected cash')}</dt><dd>{formatPrice(report.expectedCash)}</dd>
        <dt>{t('Counted cash')}</dt><dd>{report.countedCash == null ? t('Not counted') : formatPrice(report.countedCash)}</dd>
        {report.cashDifference != null && (
          <><dt>{t('Difference')}</dt>
            <dd className={report.cashDifference < 0 ? 'negative' : ''}>
              {report.cashDifference > 0 ? '+' : ''}{formatPrice(report.cashDifference)}
            </dd></>
        )}
      </dl>

      <h3>{t('Closed bills')}</h3>
      {report.bills.length === 0 ? <p className="muted">{t('No bills were closed in this shift.')}</p> : (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>{t('Closed')}</th><th>{t('Table')}</th><th>{t('Split')}</th><th className="num">{t('Total')}</th></tr></thead>
            <tbody>
              {report.bills.map((b) => (
                <tr key={b.id}>
                  <td>{formatDateTime(b.closedAt).slice(11)}</td>
                  <td>{t('Table {n}', { n: b.tableNumber })}</td>
                  <td>{b.splitType === 'NONE' ? '' : t(SPLIT_LABELS[b.splitType])}</td>
                  <td className="num">{formatPrice(b.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function Stat({ label, value, accent }) {
  return (
    <div className={`stat${accent ? ' tone-line-accent' : ''}`}>
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
    </div>
  );
}
