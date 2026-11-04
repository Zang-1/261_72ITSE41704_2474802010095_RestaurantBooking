import { useCallback, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { FormError } from '../components/FormField.jsx';
import { COURSE_LABELS, ITEM_STATUS_LABELS, formatPrice } from '../format.js';
import usePolling from '../usePolling.js';

/** The menu is reloaded every 3 seconds, so a sold-out dish disappears within 5 seconds (NFR-03). */
const POLL_MS = 3000;

/** UC-08 Create order and send to kitchen (FR-11 to FR-14, FR-18), opened from an Occupied table. */
export default function OrderPage() {
  const tableId = Number(useParams().tableId);
  const [table, setTable] = useState(null);
  const [order, setOrder] = useState(undefined);  // undefined: loading, null: the table has no order yet
  const [menu, setMenu] = useState([]);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    api.get('/tables').then((tables) => setTable(tables.find((t) => t.id === tableId) ?? null)).catch(setError);
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
    line.kitchenStatus ? `"${line.menuItemName}" cancelled.` : '');
  const send = () => run(() => api.post(`/orders/${order.id}/send`), 'Sent to the kitchen.');

  const unsent = order ? order.lines.filter((line) => !line.kitchenStatus).length : 0;

  return (
    <>
      <p><Link to="/floor">Floor overview</Link></p>
      <div className="page-head">
        <h1>{table ? `Table ${table.number}` : 'Order'}</h1>
        {order && <span className="order-total">{formatPrice(order.total)}</span>}
      </div>
      {message && <p className="notice">{message}</p>}
      <FormError error={error} />

      {order === null && (
        <div className="card">
          <p>This table has no order yet.</p>
          <button type="button" className="primary" onClick={startOrder}
            disabled={table && table.status !== 'OCCUPIED'}>Start order</button>
          {table && table.status !== 'OCCUPIED' && (
            <p className="muted">Seat the guests first; an order needs an occupied table.</p>
          )}
        </div>
      )}

      {order && (
        <div className="order-layout">
          <section className="card">
            <h2>Menu</h2>
            {groupByCategory(menu).map(([category, items]) => (
              <div key={category}>
                <h3>{category}</h3>
                <div className="menu-grid">
                  {items.map((item) => (
                    <button key={item.id} type="button" className="menu-pick" onClick={() => addDish(item)}>
                      <span>{item.name}</span>
                      <span className="muted">{COURSE_LABELS[item.course]}, {formatPrice(item.price)}</span>
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </section>

          <section className="card">
            <div className="page-head">
              <h2>Order</h2>
              <button type="button" className="primary" onClick={send} disabled={unsent === 0}>
                Send to kitchen{unsent > 0 ? ` (${unsent})` : ''}
              </button>
            </div>
            {order.lines.length === 0 && <p className="muted">Pick dishes from the menu to add them.</p>}
            <ul className="order-lines">
              {order.lines.map((line) => (
                // the key resets the inputs when the server's values change
                <OrderLineRow key={`${line.id}-${line.quantity}-${line.note}`} line={line} onChange={changeLine} onRemove={removeLine} />
              ))}
            </ul>
          </section>
        </div>
      )}
    </>
  );
}

/** An unsent line can be edited; a sent line shows its kitchen status and can only be cancelled while New (BR-08). */
function OrderLineRow({ line, onChange, onRemove }) {
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
        <strong>{line.menuItemName}</strong>
        <span className="muted">{COURSE_LABELS[line.course]}, {formatPrice(line.lineTotal)}</span>
      </div>
      {sent ? (
        <div className="order-line-sent">
          <span>{line.quantity} ×{line.note ? `, ${line.note}` : ''}</span>
          <span className={`badge item-${line.kitchenStatus}`}>{ITEM_STATUS_LABELS[line.kitchenStatus]}</span>
          {line.kitchenStatus === 'NEW' && (
            <button type="button" className="danger" onClick={() => onRemove(line)}>Cancel</button>
          )}
        </div>
      ) : (
        <div className="order-line-edit">
          <input type="number" min="1" max="50" aria-label={`Quantity of ${line.menuItemName}`}
            value={quantity} onChange={(e) => setQuantity(e.target.value)} onBlur={saveQuantity}
            onKeyDown={(e) => e.key === 'Enter' && saveQuantity()} />
          <input type="text" maxLength={200} placeholder="Note, e.g. no onion" aria-label={`Note for ${line.menuItemName}`}
            value={note} onChange={(e) => setNote(e.target.value)} onBlur={saveNote}
            onKeyDown={(e) => e.key === 'Enter' && saveNote()} />
          <button type="button" className="danger" onClick={() => onRemove(line)}>Remove</button>
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
