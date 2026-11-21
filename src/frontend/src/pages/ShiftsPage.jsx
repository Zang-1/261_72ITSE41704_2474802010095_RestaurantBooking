import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import FormField, { FormError } from '../components/FormField.jsx';
import { PAYMENT_METHOD_LABELS, SPLIT_LABELS, formatDateTime, formatPrice } from '../format.js';

/** Manager: UC-11 Open and close shift (FR-27, FR-29) and UC-15 Shift report (FR-28). */
export default function ShiftsPage() {
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
      <h1>Shifts</h1>
      {message && <p className="notice">{message}</p>}
      <FormError error={error} />

      <section className="card">
        {current ? (
          <CloseShiftForm key={current.id} shift={current}
            onClose={(countedCash) => act(() => api.post(`/shifts/${current.id}/close`, { countedCash }),
              'Shift closed. Its report is shown below.')} />
        ) : (
          <div className="page-head">
            <p>No shift is open. Waiters cannot take payments until a shift is opened (BR-13).</p>
            <button type="button" className="primary" onClick={() => act(() => api.post('/shifts'), 'Shift opened.')}>
              Open shift
            </button>
          </div>
        )}
      </section>

      <div className="shift-layout">
        <section className="card">
          <h2>All shifts</h2>
          {shifts.length === 0 && <p className="muted">No shifts yet.</p>}
          <ul className="shift-list">
            {shifts.map((s) => (
              <li key={s.id}>
                <button type="button" className={s.id === selectedId ? 'selected' : ''} onClick={() => setSelectedId(s.id)}>
                  <strong>{formatDateTime(s.openedAt)}</strong>
                  <span className="muted">{s.open ? 'Open now' : `to ${formatDateTime(s.closedAt).slice(11)}`}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
        {report && <ShiftReport report={report} />}
      </div>
    </>
  );
}

/** FR-29: the counted cash is optional; leave it empty to close without counting the till. */
function CloseShiftForm({ shift, onClose }) {
  const [counted, setCounted] = useState('');

  function submit(event) {
    event.preventDefault();
    onClose(counted === '' ? null : Number(counted));
  }

  return (
    <form onSubmit={submit}>
      <p>Shift open since <strong>{formatDateTime(shift.openedAt)}</strong>, opened by {shift.openedBy}.</p>
      <div className="row">
        <FormField label="Cash counted in the till (VND, optional)">
          <input type="number" min="0" value={counted} onChange={(e) => setCounted(e.target.value)} />
        </FormField>
        <button type="submit" className="danger">Close shift</button>
      </div>
      <p className="muted">A shift cannot close while a bill is unpaid (BR-13).</p>
    </form>
  );
}

/** FR-28: closed bills, revenue, revenue per method, split bills; FR-29: expected and counted cash. */
function ShiftReport({ report }) {
  const { shift } = report;
  return (
    <section className="card">
      <div className="page-head">
        <h2>Report: {formatDateTime(shift.openedAt)}{shift.closedAt ? ` – ${formatDateTime(shift.closedAt).slice(11)}` : ''}</h2>
        {report.provisional && <span className="badge bill-OPEN">Provisional, shift still open</span>}
      </div>
      <div className="stat-row">
        <Stat label="Revenue" value={formatPrice(report.revenue)} />
        <Stat label="Closed bills" value={report.closedBills} />
        <Stat label="Split bills" value={report.splitBills} />
        <Stat label="Payments" value={report.payments} />
      </div>

      <h3>Revenue by payment method</h3>
      <table className="data">
        <tbody>
          {Object.entries(PAYMENT_METHOD_LABELS).map(([method, label]) => (
            <tr key={method}>
              <td>{label}</td>
              <td className="num">{formatPrice(report.revenueByMethod[method] ?? 0)}</td>
              <td className="num muted">{report.revenue ? Math.round((report.revenueByMethod[method] ?? 0) * 100 / report.revenue) : 0}%</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3>Cash check</h3>
      <dl className="bill-totals">
        <dt>Expected cash</dt><dd>{formatPrice(report.expectedCash)}</dd>
        <dt>Counted cash</dt><dd>{report.countedCash == null ? 'Not counted' : formatPrice(report.countedCash)}</dd>
        {report.cashDifference != null && (
          <><dt>Difference</dt>
            <dd className={report.cashDifference < 0 ? 'negative' : ''}>
              {report.cashDifference > 0 ? '+' : ''}{formatPrice(report.cashDifference)}
            </dd></>
        )}
      </dl>

      <h3>Closed bills</h3>
      {report.bills.length === 0 ? <p className="muted">No bills were closed in this shift.</p> : (
        <table className="data">
          <thead><tr><th>Closed</th><th>Table</th><th>Split</th><th className="num">Total</th></tr></thead>
          <tbody>
            {report.bills.map((b) => (
              <tr key={b.id}>
                <td>{formatDateTime(b.closedAt).slice(11)}</td>
                <td>{b.tableNumber}</td>
                <td>{b.splitType === 'NONE' ? '' : SPLIT_LABELS[b.splitType]}</td>
                <td className="num">{formatPrice(b.total)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function Stat({ label, value }) {
  return (
    <div className="stat">
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
    </div>
  );
}
