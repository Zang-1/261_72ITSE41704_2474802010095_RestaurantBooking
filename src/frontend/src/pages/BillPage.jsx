import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import FormField, { FormError } from '../components/FormField.jsx';
import { PAYMENT_METHOD_LABELS, SPLIT_LABELS, formatDateTime, formatPrice } from '../format.js';

/**
 * Bill and payment screen of one table: UC-12 Generate bill, UC-13 Split bill, UC-14 Record payment
 * and close bill (FR-21 to FR-26). A single payment takes 4 steps from the floor overview (NFR-05):
 * Bill → Generate bill → choose the method (the amount is filled in) → Record payment.
 */
export default function BillPage() {
  const tableId = Number(useParams().tableId);
  const [table, setTable] = useState(null);
  const [order, setOrder] = useState(undefined);
  const [bill, setBill] = useState(undefined);  // undefined: loading, null: no unpaid bill
  const [shift, setShift] = useState(undefined);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    api.get('/tables').then((all) => setTable(all.find((t) => t.id === tableId) ?? null)).catch(setError);
    api.get(`/orders?tableId=${tableId}`).then((orders) => setOrder(orders[0] ?? null)).catch(setError);
    api.get('/shifts/current').then(setShift).catch(setError);
    api.get(`/bills?tableId=${tableId}`)
      .then((bills) => setBill((current) => (current?.status === 'CLOSED' ? current : bills[0] ?? null)))
      .catch(setError);
  }, [tableId]);

  useEffect(load, [load]);

  async function run(action, done) {
    setError(null);
    setMessage('');
    try {
      const result = await action();
      setBill(result.status === 'CANCELLED' ? null : result);
      if (done) setMessage(typeof done === 'function' ? done(result) : done);
      if (result.status !== 'OPEN') load();
    } catch (err) {
      setError(err);
    }
  }

  const generate = () => run(() => api.post('/bills', { orderId: order.id }), 'Bill generated. The order is locked.');
  const cancel = () => run(() => api.post(`/bills/${bill.id}/cancel`),
    'Bill cancelled. The order is open again, so the guests can order more.');
  const splitEven = (shares) => run(() => api.post(`/bills/${bill.id}/split/even`, { shares }),
    (b) => `Split into ${b.shares.length} shares.`);
  const splitByItem = (shares) => run(() => api.post(`/bills/${bill.id}/split/by-item`, { shares }),
    (b) => `Split by item into ${b.shares.length} shares.`);
  const removeSplit = () => run(() => api.del(`/bills/${bill.id}/split`), 'The bill is no longer split.');
  const pay = (body) => run(() => api.post(`/bills/${bill.id}/payments`, body), (b) => {
    const last = b.payments[b.payments.length - 1];
    const change = last.change > 0 ? ` Give back ${formatPrice(last.change)} change.` : '';
    return b.status === 'CLOSED'
      ? `Paid in full. The bill is closed and table ${b.tableNumber} needs cleaning.${change}`
      : `Payment recorded. ${formatPrice(b.remaining)} left to pay.${change}`;
  });

  const open = bill?.status === 'OPEN';
  const noPayments = open && bill.payments.length === 0;

  return (
    <>
      <p><Link to="/floor">Floor overview</Link>{order && <> · <Link to={`/order/${tableId}`}>Order</Link></>}</p>
      <div className="page-head">
        <h1>{table ? `Bill, table ${table.number}` : 'Bill'}</h1>
        {bill && <span className={`badge bill-${bill.status}`}>{bill.status === 'OPEN' ? 'Unpaid' : 'Closed'}</span>}
      </div>
      {message && <p className="notice">{message}</p>}
      <FormError error={error} />
      {shift === null && open && (
        <p className="alert">No shift is open. Ask the Manager to open a shift before taking payments (BR-13).</p>
      )}

      {bill === null && (
        <div className="card">
          {order ? (
            <>
              <p>Order total so far: <strong>{formatPrice(order.total)}</strong>, {order.lines.length} dishes.</p>
              <button type="button" className="primary" onClick={generate} disabled={!shift}>Generate bill</button>
              {shift === null && <p className="muted">No shift is open. Ask the Manager to open a shift first.</p>}
              <p className="muted">The bill can be generated when every dish is sent and ready in the kitchen (BR-09).</p>
            </>
          ) : (
            order === null && <p>This table has no unpaid order.</p>
          )}
        </div>
      )}

      {bill && (
        <div className="bill-layout">
          <section className="card">
            <BillSummary bill={bill} />
            {open && noPayments && (
              <button type="button" className="danger" onClick={cancel}>Cancel bill to order more</button>
            )}
          </section>

          {open && (
            <div>
              <PaymentForm key={`${bill.id}-${bill.payments.length}-${bill.splitType}`} bill={bill} disabled={!shift} onPay={pay} />
              {noPayments && (
                <SplitCard key={`${bill.id}-${bill.splitType}-${bill.shares.length}`} bill={bill}
                  onEven={splitEven} onByItem={splitByItem} onRemove={removeSplit} />
              )}
            </div>
          )}
        </div>
      )}
    </>
  );
}

/** FR-22: lines, total, paid and remaining; the shares and the payments when there are any. */
function BillSummary({ bill }) {
  const byItem = bill.splitType === 'BY_ITEM';
  return (
    <>
      <table className="data bill-lines">
        <thead>
          <tr>
            <th>Dish</th><th className="num">Qty</th><th className="num">Unit price</th><th className="num">Amount</th>
            {byItem && <th className="num">Share</th>}
          </tr>
        </thead>
        <tbody>
          {bill.lines.map((line) => (
            <tr key={line.id}>
              <td>{line.itemName}</td>
              <td className="num">{line.quantity}</td>
              <td className="num">{formatPrice(line.unitPrice)}</td>
              <td className="num">{formatPrice(line.lineTotal)}</td>
              {byItem && <td className="num">{line.shareNumber}</td>}
            </tr>
          ))}
        </tbody>
      </table>
      <dl className="bill-totals">
        <dt>Total</dt><dd>{formatPrice(bill.total)}</dd>
        <dt>Paid</dt><dd>{formatPrice(bill.paid)}</dd>
        <dt>Remaining</dt><dd className="bill-remaining">{formatPrice(bill.remaining)}</dd>
      </dl>

      {bill.shares.length > 0 && (
        <>
          <h3>{SPLIT_LABELS[bill.splitType]}</h3>
          <ul className="share-list">
            {bill.shares.map((share) => (
              <li key={share.id} className={share.remaining === 0 ? 'is-paid' : ''}>
                <span>Share {share.number}</span>
                <span>{formatPrice(share.amount)}</span>
                <span className="muted">{share.remaining === 0 ? 'Paid' : `${formatPrice(share.remaining)} left`}</span>
              </li>
            ))}
          </ul>
        </>
      )}

      {bill.payments.length > 0 && (
        <>
          <h3>Payments</h3>
          <ul className="payment-list">
            {bill.payments.map((p) => (
              <li key={p.id}>
                <span>{formatDateTime(p.paidAt).slice(11)} {PAYMENT_METHOD_LABELS[p.method]}
                  {p.shareNumber ? `, share ${p.shareNumber}` : ''}</span>
                <span>{formatPrice(p.amount)}{p.change > 0 && <span className="muted"> (change {formatPrice(p.change)})</span>}</span>
              </li>
            ))}
          </ul>
        </>
      )}
      {bill.closedAt && <p className="muted">Closed {formatDateTime(bill.closedAt)}. A closed bill cannot be changed (BR-12).</p>}
    </>
  );
}

/** FR-25, BR-11: card, transfer and e-wallet up to the amount due; cash may be more and the change is shown. */
function PaymentForm({ bill, disabled, onPay }) {
  const unpaidShares = bill.shares.filter((s) => s.remaining > 0);
  const [shareId, setShareId] = useState(unpaidShares[0]?.id ?? null);
  const share = bill.shares.find((s) => s.id === shareId);
  const due = share ? share.remaining : bill.remaining;
  const [method, setMethod] = useState('CASH');
  const [amount, setAmount] = useState(String(due));

  const handed = Number(amount) || 0;
  const change = method === 'CASH' ? Math.max(0, handed - due) : 0;
  const tooMuch = method !== 'CASH' && handed > due;

  function chooseShare(id) {
    setShareId(id);
    setAmount(String(bill.shares.find((s) => s.id === id).remaining));
  }

  function submit(event) {
    event.preventDefault();
    onPay({ method, amount: handed, ...(share ? { shareId: share.id } : {}) });
  }

  return (
    <form className="card" onSubmit={submit}>
      <h2>Payment</h2>
      {bill.shares.length > 0 && (
        <div className="choice-list share-pick">
          {unpaidShares.map((s) => (
            <label key={s.id} className={`choice${s.id === shareId ? ' selected' : ''}`}>
              <input type="radio" name="share" checked={s.id === shareId} onChange={() => chooseShare(s.id)} />
              Share {s.number}: {formatPrice(s.remaining)}
            </label>
          ))}
        </div>
      )}
      <div className="method-pick" role="radiogroup" aria-label="Payment method">
        {Object.entries(PAYMENT_METHOD_LABELS).map(([value, label]) => (
          <button key={value} type="button" role="radio" aria-checked={method === value}
            className={method === value ? 'primary' : ''}
            onClick={() => { setMethod(value); if (value !== 'CASH' && handed > due) setAmount(String(due)); }}>
            {label}
          </button>
        ))}
      </div>
      <FormField label={method === 'CASH' ? 'Cash received (VND)' : 'Amount (VND)'} 
        error={tooMuch ? `Cannot be more than ${formatPrice(due)} (BR-11)` : undefined}>
        <input type="number" min="1" value={amount} onChange={(e) => setAmount(e.target.value)} />
      </FormField>
      <p className="pay-due">
        Due: <strong>{formatPrice(due)}</strong>
        {method === 'CASH' && change > 0 && <> · Change: <strong>{formatPrice(change)}</strong></>}
      </p>
      <button type="submit" className="primary" disabled={disabled || handed <= 0 || tooMuch}>Record payment</button>
    </form>
  );
}

/** UC-13: split evenly (BR-10) or by item (FR-24). Only before the first payment. */
function SplitCard({ bill, onEven, onByItem, onRemove }) {
  const [mode, setMode] = useState(bill.splitType === 'BY_ITEM' ? 'item' : 'even');
  const [shares, setShares] = useState(String(bill.splitType === 'EVEN' ? bill.shares.length : 2));
  const [itemShares, setItemShares] = useState(String(bill.splitType === 'BY_ITEM' ? bill.shares.length : 2));
  const [assigned, setAssigned] = useState(() => Object.fromEntries(bill.lines.map((l) => [l.id, String(l.shareNumber ?? '')])));

  const count = Number(itemShares);
  const unassigned = bill.lines.filter((l) => !assigned[l.id]);
  const emptyShares = [...Array(count).keys()].map((i) => String(i + 1)).filter((n) => !Object.values(assigned).includes(n));

  function confirmByItem() {
    const groups = [...Array(count).keys()].map((i) => bill.lines.filter((l) => assigned[l.id] === String(i + 1)).map((l) => l.id));
    onByItem(groups);
  }

  return (
    <section className="card">
      <div className="page-head">
        <h2>Split bill</h2>
        {bill.splitType !== 'NONE' && <button type="button" className="link" onClick={onRemove}>Remove split</button>}
      </div>
      <div className="method-pick">
        <button type="button" className={mode === 'even' ? 'primary' : ''} onClick={() => setMode('even')}>Evenly</button>
        <button type="button" className={mode === 'item' ? 'primary' : ''} onClick={() => setMode('item')}>By item</button>
      </div>
      {mode === 'even' ? (
        <div className="row">
          <FormField label="Number of shares (2–20)">
            <input type="number" min="2" max="20" value={shares} onChange={(e) => setShares(e.target.value)} />
          </FormField>
          <button type="button" onClick={() => onEven(Number(shares))}>Split evenly</button>
        </div>
      ) : (
        <>
          <FormField label="Number of shares">
            <select value={itemShares} onChange={(e) => setItemShares(e.target.value)}>
              {[...Array(Math.min(19, Math.max(1, bill.lines.length - 1))).keys()].map((i) => (
                <option key={i} value={i + 2}>{i + 2}</option>
              ))}
            </select>
          </FormField>
          <ul className="assign-list">
            {bill.lines.map((line) => (
              <li key={line.id}>
                <span>{line.quantity} × {line.itemName}, {formatPrice(line.lineTotal)}</span>
                <select aria-label={`Share for ${line.itemName}`} value={assigned[line.id]}
                  onChange={(e) => setAssigned({ ...assigned, [line.id]: e.target.value })}>
                  <option value="">Choose</option>
                  {[...Array(count).keys()].map((i) => <option key={i} value={i + 1}>Share {i + 1}</option>)}
                </select>
              </li>
            ))}
          </ul>
          {(unassigned.length > 0 || emptyShares.length > 0) && (
            <p className="muted">Every dish needs a share and every share needs a dish.</p>
          )}
          <button type="button" onClick={confirmByItem} disabled={unassigned.length > 0 || emptyShares.length > 0}>
            Split by item
          </button>
        </>
      )}
    </section>
  );
}
