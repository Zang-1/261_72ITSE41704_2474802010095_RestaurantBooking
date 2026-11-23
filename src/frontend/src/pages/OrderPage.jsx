import { useCallback, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Badge, Empty, Notice, PageHeader } from '../components/ui.jsx';
import { COURSE_LABELS, ITEM_STATUS_LABELS, formatPrice } from '../format.js';
import usePolling from '../usePolling.js';

/** The menu is reloaded every 3 seconds, so a sold-out dish disappears within 5 seconds (NFR-03). */
const POLL_MS = 3000;

/** UC-08 Create order and send to kitchen (FR-11 to FR-14, FR-18, FR-19), opened from an Occupied table. */
export default function OrderPage() {
  const t = useT();
  const tableId = Number(useParams().tableId);
  const navigate = useNavigate();
  const [tables, setTables] = useState([]);
  const [table, setTable] = useState(null);
  const [order, setOrder] = useState(undefined);  // undefined: loading, null: the table has no order yet
  const [menu, setMenu] = useState([]);
  const [category, setCategory] = useState('');
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    api.get('/tables').then((all) => {
      setTables(all);
      setTable(all.find((x) => x.id === tableId) ?? null);
    }).catch(setError);
    api.get(`/orders?tableId=${tableId}`).then((orders) => setOrder(orders[0] ?? null)).catch(setError);
    api.get('/menu/items?availableOnly=true').then(setMenu).catch(setError);
  }, [tableId]);

  usePolling(load, POLL_MS);

  async function run(action, done) {
    setError(null);
    setMessage('');
    try {
      const result = await action();
      setOrder(result);
      if (done) setMessage(done);
    } catch (err) {
      setError(err);
      load();
    }
  }

  const startOrder = () => run(() => api.post('/orders', { tableId }));
  const addDish = (item) => run(() => api.post(`/orders/${order.id}/lines`, { menuItemId: item.id, quantity: 1 }));
  const changeLine = (line, changes) => run(() => api.put(`/orders/${order.id}/lines/${line.id}`,
    { quantity: line.quantity, note: line.note, ...changes }));
  const removeLine = (line) => run(() => api.del(`/orders/${order.id}/lines/${line.id}`),
    line.kitchenStatus ? t('"{name}" cancelled.', { name: line.menuItemName }) : '');
  const send = () => run(() => api.post(`/orders/${order.id}/send`), t('Sent to the kitchen.'));
  const sendNextCourse = () => run(() => api.post(`/orders/${order.id}/send`, { lineIds: nextLines.map((line) => line.id) }),
    t('{course} dishes sent to the kitchen.', { course: t(COURSE_LABELS[nextCourse]) }));

  async function move(targetId, partySize) {
    setError(null);
    setMessage('');
    try {
      const moved = await api.post(`/orders/${order.id}/move`, { tableId: targetId, partySize });
      navigate(`/order/${moved.tableId}`);
      setMessage(t('Order moved from table {from} to table {to}. Table {from} needs cleaning.', { from: table.number, to: moved.tableNumber }));
    } catch (err) {
      setError(err);
    }
  }

  const unsentLines = order ? order.lines.filter((line) => !line.kitchenStatus) : [];
  const unsent = unsentLines.length;
  // COURSE_LABELS is in meal order, so this is the earliest course still unsent (BR-06)
  const nextCourse = Object.keys(COURSE_LABELS).find((course) => unsentLines.some((line) => line.course === course));
  const nextLines = unsentLines.filter((line) => line.course === nextCourse);
  const groups = groupByCategory(menu);
  const shownGroups = category ? groups.filter(([name]) => name === category) : groups;

  return (
    <>
      <PageHeader back={{ to: '/floor', label: t('Floor overview') }}
        title={table ? t('Table {n}', { n: table.number }) : t('Order')}
        subtitle={table ? `${table.sectionName} · ${t('{n} seats', { n: table.capacity })}` : null}>
        {order && (
          <>
            <span className="order-total">{formatPrice(order.total)}</span>
            <Link className="btn" to={`/bill/${tableId}`}><Icon name="receipt" size={16} />{order.status === 'LOCKED' ? t('Open bill') : t('Bill')}</Link>
          </>
        )}
      </PageHeader>
      <Notice>{message}</Notice>
      <FormError error={error} />

      {order === null && (
        <div className="card">
          <Empty icon="receipt" title={t('This table has no order yet')}>
            {table && table.status !== 'OCCUPIED' ? t('Seat the guests first; an order needs an occupied table.') : t('Start an order, then pick dishes from the menu.')}
          </Empty>
          <button type="button" className="btn btn-primary" onClick={startOrder}
            disabled={table && table.status !== 'OCCUPIED'}><Icon name="plus" size={16} />{t('Start order')}</button>
        </div>
      )}

      {order && (
        <div className="split">
          <section className="card">
            <div className="card-head">
              <h2>{t('Menu')}</h2>
              <div className="segmented" role="group" aria-label={t('Category')}>
                <button type="button" className="btn btn-sm" aria-pressed={category === ''} onClick={() => setCategory('')}>{t('All')}</button>
                {groups.map(([name]) => (
                  <button key={name} type="button" className="btn btn-sm" aria-pressed={category === name} onClick={() => setCategory(name)}>{t(name)}</button>
                ))}
              </div>
            </div>
            {shownGroups.map(([name, items]) => (
              <div key={name}>
                <h3>{t(name)}</h3>
                <div className="menu-grid">
                  {items.map((item) => (
                    <button key={item.id} type="button" className="menu-pick" onClick={() => addDish(item)}>
                      <strong>{t(item.name)}</strong>
                      <span>{t(COURSE_LABELS[item.course])} · {formatPrice(item.price)}</span>
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </section>

          <section className="card">
            <div className="card-head">
              <h2>{t('Order')}</h2>
              <Badge tone={order.status}>{order.status === 'LOCKED' ? t('Bill generated') : t('Open')}</Badge>
            </div>
            {order.lines.length === 0 && <p className="muted">{t('Pick dishes from the menu to add them.')}</p>}
            <ul className="order-lines">
              {order.lines.map((line) => (
                // the key resets the inputs when the server's values change
                <OrderLineRow key={`${line.id}-${line.quantity}-${line.note}`} line={line} onChange={changeLine} onRemove={removeLine} />
              ))}
            </ul>
            <div className="page-actions" style={{ marginTop: '1rem' }}>
              {nextLines.length < unsent && (
                <button type="button" className="btn" onClick={sendNextCourse}>
                  {t('Send {course} only ({n})', { course: t(COURSE_LABELS[nextCourse]), n: nextLines.length })}
                </button>
              )}
              <button type="button" className="btn btn-primary" onClick={send} disabled={unsent === 0}>
                <Icon name="send" size={16} />{unsent > 0 ? t('Send to kitchen ({n})', { n: unsent }) : t('Send to kitchen')}
              </button>
            </div>
            <MoveOrderForm key={tableId} tables={tables.filter((x) => x.status === 'AVAILABLE' && x.active)} onMove={move} />
          </section>
        </div>
      )}
    </>
  );
}

/** FR-19: the guests move to a free table; the party size is checked against its seats (BR-02). */
function MoveOrderForm({ tables, onMove }) {
  const t = useT();
  const [targetId, setTargetId] = useState('');
  const [partySize, setPartySize] = useState('2');

  function submit(event) {
    event.preventDefault();
    if (targetId) onMove(Number(targetId), Number(partySize));
  }

  return (
    <form className="move-order" onSubmit={submit}>
      <h3>{t('Move to another table')}</h3>
      <div className="order-line-edit">
        <select className="input" aria-label={t('New table')} value={targetId} onChange={(e) => setTargetId(e.target.value)}>
          <option value="">{t('Choose a free table')}</option>
          {tables.map((x) => (
            <option key={x.id} value={x.id}>{t('Table {n}', { n: x.number })}, {t('{n} seats', { n: x.capacity })}, {x.sectionName}</option>
          ))}
        </select>
        <input className="input" type="number" min="1" max="20" aria-label={t('Party size')} value={partySize}
          onChange={(e) => setPartySize(e.target.value)} />
        <button type="submit" className="btn" disabled={!targetId}><Icon name="move" size={16} />{t('Move order')}</button>
      </div>
    </form>
  );
}

/** An unsent line can be edited; a sent line shows its kitchen status and can only be cancelled while New (BR-08). */
function OrderLineRow({ line, onChange, onRemove }) {
  const t = useT();
  const [quantity, setQuantity] = useState(String(line.quantity));
  const [note, setNote] = useState(line.note ?? '');
  const sent = Boolean(line.kitchenStatus);

  function saveQuantity() {
    if (quantity !== String(line.quantity)) onChange(line, { quantity: Number(quantity) });
  }

  function saveNote() {
    if (note !== (line.note ?? '')) onChange(line, { note });
  }

  return (
    <li className={`order-line${line.kitchenStatus === 'CANCELLED' ? ' is-cancelled' : ''}`}>
      <div className="order-line-main">
        <strong>{t(line.menuItemName)}</strong>
        <span className="muted">{t(COURSE_LABELS[line.course])} · {formatPrice(line.lineTotal)}</span>
      </div>
      {sent ? (
        <div className="order-line-sent">
          <span>{line.quantity} ×{line.note ? `, ${line.note}` : ''}</span>
          <Badge tone={line.kitchenStatus}>{t(ITEM_STATUS_LABELS[line.kitchenStatus])}</Badge>
          {line.kitchenStatus === 'NEW' && (
            <button type="button" className="btn btn-sm btn-danger" onClick={() => onRemove(line)}>{t('Cancel')}</button>
          )}
        </div>
      ) : (
        <div className="order-line-edit">
          <input className="input" type="number" min="1" max="50" aria-label={t('Quantity of {name}', { name: line.menuItemName })}
            value={quantity} onChange={(e) => setQuantity(e.target.value)} onBlur={saveQuantity}
            onKeyDown={(e) => e.key === 'Enter' && saveQuantity()} />
          <input className="input" type="text" maxLength={200} placeholder={t('Note, e.g. no onion')} aria-label={t('Note for {name}', { name: line.menuItemName })}
            value={note} onChange={(e) => setNote(e.target.value)} onBlur={saveNote}
            onKeyDown={(e) => e.key === 'Enter' && saveNote()} />
          <button type="button" className="btn btn-sm btn-danger" onClick={() => onRemove(line)}>{t('Remove')}</button>
        </div>
      )}
    </li>
  );
}

/** [[categoryName, items[]], ...] in the order the server lists them. */
function groupByCategory(items) {
  const groups = new Map();
  for (const item of items) {
    if (!groups.has(item.categoryName)) groups.set(item.categoryName, []);
    groups.get(item.categoryName).push(item);
  }
  return [...groups.entries()];
}
