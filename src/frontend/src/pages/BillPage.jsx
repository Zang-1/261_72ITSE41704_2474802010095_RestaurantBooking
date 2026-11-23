import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import FormField, { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Badge, Empty, Notice, PageHeader } from '../components/ui.jsx';
import { PAYMENT_METHOD_LABELS, SPLIT_LABELS, formatDateTime, formatPrice } from '../format.js';

/**
 * Bill and payment screen of one table: UC-12 Generate bill, UC-13 Split bill, UC-14 Record payment
 * and close bill (FR-21 to FR-26). A single payment takes 4 steps from the floor overview (NFR-05):
 * Bill → Generate bill → choose the method (the amount is filled in) → Record payment.
 */
export default function BillPage() {
  const t = useT();
  const tableId = Number(useParams().tableId);
  const [table, setTable] = useState(null);
  const [order, setOrder] = useState(undefined);
  const [bill, setBill] = useState(undefined);  // undefined: loading, null: no unpaid bill
  const [shift, setShift] = useState(undefined);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    api.get('/tables').then((all) => setTable(all.find((x) => x.id === tableId) ?? null)).catch(setError);
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

  const generate = () => run(() => api.post('/bills', { orderId: order.id }), t('Bill generated. The order is locked.'));
  const cancel = () => run(() => api.post(`/bills/${bill.id}/cancel`),
    t('Bill cancelled. The order is open again, so the guests can order more.'));
  const splitEven = (shares) => run(() => api.post(`/bills/${bill.id}/split/even`, { shares }),
    (b) => t('Split into {n} shares.', { n: b.shares.length }));
  const splitByItem = (shares) => run(() => api.post(`/bills/${bill.id}/split/by-item`, { shares }),
    (b) => t('Split by item into {n} shares.', { n: b.shares.length }));
  const removeSplit = () => run(() => api.del(`/bills/${bill.id}/split`), t('The bill is no longer split.'));
  const pay = (body) => run(() => api.post(`/bills/${bill.id}/payments`, body), (b) => {
    const last = b.payments[b.payments.length - 1];
    const change = last.change > 0 ? ` ${t('Give back {amount} change.', { amount: formatPrice(last.change) })}` : '';
    return (b.status === 'CLOSED'
      ? t('Paid in full. The bill is closed and table {n} needs cleaning.', { n: b.tableNumber })
      : t('Payment recorded. {amount} left to pay.', { amount: formatPrice(b.remaining) })) + change;
  });

  const open = bill?.status === 'OPEN';
  const noPayments = open && bill.payments.length === 0;

  return (
    <>
      <PageHeader back={{ to: '/floor', label: t('Floor overview') }}
        title={table ? t('Bill, table {n}', { n: table.number }) : t('Bill')}
        subtitle={t('Generate the bill when every dish is ready, split it if the guests ask, then record payments.')}>
        {bill && <Badge tone={bill.status}>{bill.status === 'OPEN' ? t('Unpaid') : t('Closed')}</Badge>}
        {order && <Link className="btn" to={`/order/${tableId}`}><Icon name="receipt" size={16} />{t('Order')}</Link>}
      </PageHeader>
      <Notice>{message}</Notice>
      <FormError error={error} />
      {shift === null && open && (
        <p className="alert warning"><Icon name="alert" size={16} />{t('No shift is open. Ask the Manager to open a shift before taking payments (BR-13).')}</p>
      )}

      {bill === null && (
        <div className="card">
          {order ? (
            <>
              <div className="due"><span>{t('Order total so far ({n} dishes)', { n: order.lines.length })}</span><strong>{formatPrice(order.total)}</strong></div>
              <button type="button" className="btn btn-primary" onClick={generate} disabled={!shift}>
                <Icon name="receipt" size={16} />{t('Generate bill')}
              </button>
              <p className="muted" style={{ marginTop: '0.75rem' }}>
                {shift === null ? t('No shift is open. Ask the Manager to open a shift first.') : t('The bill can be generated when every dish is sent and ready in the kitchen (BR-09).')}
              </p>
            </>
          ) : (
            order === null && <Empty icon="receipt" title={t('This table has no unpaid order')} />
          )}
        </div>
      )}

      {bill && (
        <div className="split">
          <section className="card">
            <BillSummary bill={bill} />
            {open && noPayments && (
              <button type="button" className="btn btn-danger" onClick={cancel}>{t('Cancel bill to order more')}</button>
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
  const t = useT();
  const byItem = bill.splitType === 'BY_ITEM';
  return (
    <>
      <div className="card-head"><h2>{t('Bill')}</h2></div>
      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr>
              <th>{t('Dish')}</th><th className="num">{t('Qty')}</th><th className="num">{t('Unit price')}</th><th className="num">{t('Amount')}</th>
              {byItem && <th className="num">{t('Share')}</th>}
            </tr>
          </thead>
          <tbody>
            {bill.lines.map((line) => (
              <tr key={line.id}>
                <td>{t(line.itemName)}</td>
                <td className="num">{line.quantity}</td>
                <td className="num">{formatPrice(line.unitPrice)}</td>
                <td className="num">{formatPrice(line.lineTotal)}</td>
                {byItem && <td className="num">{line.shareNumber}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <dl className="totals">
        <dt>{t('Total')}</dt><dd>{formatPrice(bill.total)}</dd>
        <dt>{t('Paid')}</dt><dd>{formatPrice(bill.paid)}</dd>
        <dt>{t('Remaining')}</dt><dd className="grand">{formatPrice(bill.remaining)}</dd>
      </dl>

      {bill.shares.length > 0 && (
        <>
          <h3>{t(SPLIT_LABELS[bill.splitType])}</h3>
          <ul className="line-list">
            {bill.shares.map((share) => (
              <li key={share.id} className={share.remaining === 0 ? 'is-paid' : ''}>
                <span>{t('Share {n}', { n: share.number })}</span>
                <span>{formatPrice(share.amount)}</span>
                <span className="muted">{share.remaining === 0 ? t('Paid') : t('{amount} left', { amount: formatPrice(share.remaining) })}</span>
              </li>
            ))}
          </ul>
        </>
      )}

      {bill.payments.length > 0 && (
        <>
          <h3>{t('Payments')}</h3>
          <ul className="line-list">
            {bill.payments.map((p) => (
              <li key={p.id}>
                <span>{formatDateTime(p.paidAt).slice(11)} · {t(PAYMENT_METHOD_LABELS[p.method])}
                  {p.shareNumber ? ` · ${t('Share {n}', { n: p.shareNumber })}` : ''}</span>
                <span>{formatPrice(p.amount)}{p.change > 0 && <span className="muted"> ({t('change {amount}', { amount: formatPrice(p.change) })})</span>}</span>
              </li>
            ))}
          </ul>
        </>
      )}
      {bill.closedAt && <p className="muted">{t('Closed {time}. A closed bill cannot be changed (BR-12).', { time: formatDateTime(bill.closedAt) })}</p>}
    </>
  );
}

/** FR-25, BR-11: card, transfer and e-wallet up to the amount due; cash may be more and the change is shown. */
function PaymentForm({ bill, disabled, onPay }) {
  const t = useT();
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
      <div className="card-head"><h2>{t('Payment')}</h2></div>
      {bill.shares.length > 0 && (
        <div className="choice-grid" style={{ marginBottom: '1rem' }}>
          {unpaidShares.map((s) => (
            <label key={s.id} className={`choice${s.id === shareId ? ' selected' : ''}`}>
              <input type="radio" name="share" checked={s.id === shareId} onChange={() => chooseShare(s.id)} />
              <span className="choice-text"><strong>{t('Share {n}', { n: s.number })}</strong><span>{formatPrice(s.remaining)}</span></span>
            </label>
          ))}
        </div>
      )}
      <div className="method-grid" role="radiogroup" aria-label={t('Payment method')}>
        {Object.entries(PAYMENT_METHOD_LABELS).map(([value, label]) => (
          <button key={value} type="button" role="radio" aria-checked={method === value} className="btn"
            onClick={() => { setMethod(value); if (value !== 'CASH' && handed > due) setAmount(String(due)); }}>
            {t(label)}
          </button>
        ))}
      </div>
      <FormField label={method === 'CASH' ? t('Cash received (VND)') : t('Amount (VND)')}
        error={tooMuch ? t('Cannot be more than {amount} (BR-11)', { amount: formatPrice(due) }) : undefined}>
        <input type="number" min="1" value={amount} onChange={(e) => setAmount(e.target.value)} />
      </FormField>
      <div className="due">
        <span>{t('Due')}</span><strong>{formatPrice(due)}</strong>
      </div>
      {method === 'CASH' && change > 0 && (
        <div className="due"><span>{t('Change')}</span><strong>{formatPrice(change)}</strong></div>
      )}
      <button type="submit" className="btn btn-primary btn-block" disabled={disabled || handed <= 0 || tooMuch}>
        <Icon name="wallet" size={16} />{t('Record payment')}
      </button>
    </form>
  );
}

/** UC-13: split evenly (BR-10) or by item (FR-24). Only before the first payment. */
function SplitCard({ bill, onEven, onByItem, onRemove }) {
  const t = useT();
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
      <div className="card-head">
        <h2>{t('Split bill')}</h2>
        {bill.splitType !== 'NONE' && <button type="button" className="btn btn-ghost btn-sm" onClick={onRemove}>{t('Remove split')}</button>}
      </div>
      <div className="segmented" role="group" aria-label={t('Split')} style={{ marginBottom: '1rem' }}>
        <button type="button" className="btn btn-sm" aria-pressed={mode === 'even'} onClick={() => setMode('even')}>{t('Evenly')}</button>
        <button type="button" className="btn btn-sm" aria-pressed={mode === 'item'} onClick={() => setMode('item')}>{t('By item')}</button>
      </div>
      {mode === 'even' ? (
        <div className="form-row">
          <FormField label={t('Number of shares (2-20)')}>
            <input type="number" min="2" max="20" value={shares} onChange={(e) => setShares(e.target.value)} />
          </FormField>
          <button type="button" className="btn" onClick={() => onEven(Number(shares))}>{t('Split evenly')}</button>
        </div>
      ) : (
        <>
          <FormField label={t('Number of shares')}>
            <select value={itemShares} onChange={(e) => setItemShares(e.target.value)}>
              {[...Array(Math.min(19, Math.max(1, bill.lines.length - 1))).keys()].map((i) => (
                <option key={i} value={i + 2}>{i + 2}</option>
              ))}
            </select>
          </FormField>
          <ul className="line-list">
            {bill.lines.map((line) => (
              <li key={line.id}>
                <span>{line.quantity} × {t(line.itemName)} · {formatPrice(line.lineTotal)}</span>
                <select aria-label={t('Share for {name}', { name: line.itemName })} value={assigned[line.id]}
                  onChange={(e) => setAssigned({ ...assigned, [line.id]: e.target.value })}>
                  <option value="">{t('Choose')}</option>
                  {[...Array(count).keys()].map((i) => <option key={i} value={i + 1}>{t('Share {n}', { n: i + 1 })}</option>)}
                </select>
              </li>
            ))}
          </ul>
          {(unassigned.length > 0 || emptyShares.length > 0) && (
            <p className="muted" style={{ marginTop: '0.75rem' }}>{t('Every dish needs a share and every share needs a dish.')}</p>
          )}
          <button type="button" className="btn" style={{ marginTop: '0.75rem' }} onClick={confirmByItem}
            disabled={unassigned.length > 0 || emptyShares.length > 0}>
            {t('Split by item')}
          </button>
        </>
      )}
    </section>
  );
}
