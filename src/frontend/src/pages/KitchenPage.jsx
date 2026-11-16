import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { FormError } from '../components/FormField.jsx';
import { COURSE_LABELS, ITEM_STATUS_LABELS, minutesSince, timeOf } from '../format.js';
import usePolling from '../usePolling.js';

/** Polling every 2 seconds puts a sent order on the display within 3 seconds (NFR-02). */
const POLL_MS = 2000;
/** A ticket waiting longer than this is highlighted. */
const LATE_MINUTES = 15;

/** UC-09 Process kitchen ticket (FR-15): tickets oldest first, dishes in course order (BR-06). */
export default function KitchenPage() {
  const [tickets, setTickets] = useState([]);
  const [finished, setFinished] = useState([]);
  const [error, setError] = useState(null);
  const [now, setNow] = useState(() => new Date());

  const load = useCallback(() => {
    api.get('/kitchen/tickets/finished').then(setFinished).catch(setError);
    return api.get('/kitchen/tickets')
      .then((data) => {
        setTickets(data);
        setNow(new Date());
      })
      .catch(setError);
  }, []);

  usePolling(load, POLL_MS);

  async function move(item, action, body) {
    setError(null);
    try {
      await api.post(`/kitchen/items/${item.id}/${action}`, body);
      load();
    } catch (err) {
      setError(err);
      load();
    }
  }

  function sendBack(item) {
    const reason = window.prompt(`Why was "${item.menuItemName}" sent back?`);
    if (reason === null) return;
    move(item, 'send-back', { reason });
  }

  return (
    <>
      <div className="page-head">
        <h1>Kitchen display</h1>
        <span className="muted">{tickets.length} open {tickets.length === 1 ? 'ticket' : 'tickets'}</span>
      </div>
      <FormError error={error} />
      {tickets.length === 0 && <p className="muted">No dishes waiting. New tickets appear here by themselves.</p>}
      <div className="ticket-rail">
        {tickets.map((ticket) => (
          <Ticket key={ticket.id} ticket={ticket} now={now} onMove={move} onSendBack={sendBack} />
        ))}
      </div>
      {finished.length > 0 && (
        <details className="card sold-out">
          <summary>Finished tickets ({finished.length})</summary>
          <div className="ticket-rail">
            {finished.map((ticket) => (
              <Ticket key={ticket.id} ticket={ticket} now={now} onMove={move} onSendBack={sendBack} />
            ))}
          </div>
        </details>
      )}
      <SoldOutPanel />
    </>
  );
}

function Ticket({ ticket, now, onMove, onSendBack }) {
  const waited = minutesSince(ticket.sentAt, now);
  const late = waited >= LATE_MINUTES && ticket.items.some((item) => item.status === 'NEW' || item.status === 'IN_PROGRESS');
  return (
    <article className={`ticket${late ? ' late' : ''}`}>
      <header className="ticket-head">
        <span className="ticket-table">Table {ticket.tableNumber}</span>
        <span className="ticket-time">
          Sent {timeOf(ticket.sentAt)}, {waited === 0 ? 'just now' : `${waited} min ago`}
        </span>
      </header>
      {groupByCourse(ticket.items).map(([course, items]) => (
        <section key={course} className="ticket-course">
          <h3>{COURSE_LABELS[course]}</h3>
          <ul>
            {items.map((item) => (
              <li key={item.id} className={`ticket-item item-${item.status}`}>
                <div className="ticket-dish">
                  <strong>{item.quantity} × {item.menuItemName}</strong>
                  {item.note && <span className="ticket-note">{item.note}</span>}
                </div>
                <div className="ticket-actions">
                  {item.status === 'NEW' && (
                    <button type="button" className="primary" onClick={() => onMove(item, 'start')}>Start</button>
                  )}
                  {item.status === 'IN_PROGRESS' && (
                    <button type="button" className="primary" onClick={() => onMove(item, 'ready')}>Ready</button>
                  )}
                  {item.status === 'READY' && (
                    <>
                      <span className="item-status">{ITEM_STATUS_LABELS.READY}</span>
                      <button type="button" className="link" onClick={() => onSendBack(item)}>Sent back</button>
                    </>
                  )}
                  {item.status === 'IN_PROGRESS' && <span className="item-status">Cooking</span>}
                  {item.status === 'CANCELLED' && <span className="item-status">{ITEM_STATUS_LABELS.CANCELLED}</span>}
                </div>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </article>
  );
}

/** [[course, items[]], ...]; the server already sends the items in course order. */
function groupByCourse(items) {
  const groups = new Map();
  for (const item of items) {
    if (!groups.has(item.course)) groups.set(item.course, []);
    groups.get(item.course).push(item);
  }
  return [...groups.entries()];
}

/** UC-10: the kitchen marks a dish sold out or back in stock; order screens update within 5 seconds (NFR-03). */
function SoldOutPanel() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState(null);

  const load = useCallback(() => {
    api.get('/menu/items').then(setItems).catch(setError);
  }, []);

  useEffect(load, [load]);

  async function toggle(item) {
    setError(null);
    try {
      await api.post(`/menu/items/${item.id}/${item.inStock ? 'out-of-stock' : 'restock'}`);
      load();
    } catch (err) {
      setError(err);
    }
  }

  const soldOut = items.filter((item) => !item.inStock).length;

  return (
    <details className="card sold-out">
      <summary>Sold-out dishes ({soldOut})</summary>
      <FormError error={error} />
      <ul className="sold-out-list">
        {items.map((item) => (
          <li key={item.id} className={item.inStock ? '' : 'is-out'}>
            <span>{item.name}</span>
            <button type="button" className={item.inStock ? 'danger' : 'primary'} onClick={() => toggle(item)}>
              {item.inStock ? 'Mark sold out' : 'Back in stock'}
            </button>
          </li>
        ))}
      </ul>
    </details>
  );
}
