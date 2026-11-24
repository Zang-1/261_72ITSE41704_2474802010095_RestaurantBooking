import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import { useDialog } from '../components/Dialog.jsx';
import { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Empty, PageHeader } from '../components/ui.jsx';
import { COURSE_LABELS, ITEM_STATUS_LABELS, minutesSince, timeOf } from '../format.js';
import usePolling from '../usePolling.js';

/** Polling every 2 seconds puts a sent order on the display within 3 seconds (NFR-02). */
const POLL_MS = 2000;
/** A ticket waiting longer than this is highlighted. */
const LATE_MINUTES = 15;

/** UC-09 Process kitchen ticket (FR-15): tickets oldest first, dishes in course order (BR-06). */
export default function KitchenPage() {
  const t = useT();
  const ask = useDialog();
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

  async function sendBack(item) {
    const reason = await ask({
      title: t('Send back "{dish}"?', { dish: item.menuItemName }),
      message: t('The dish goes back to In Progress so the kitchen can cook it again.'),
      input: { label: t('Reason'), value: '' },
      confirmLabel: t('Send back'),
    });
    if (reason === null) return;
    move(item, 'send-back', { reason });
  }

  const dishes = tickets.flatMap((ticket) => ticket.items);
  const waiting = dishes.filter((item) => item.status === 'NEW').length;
  const cooking = dishes.filter((item) => item.status === 'IN_PROGRESS').length;

  return (
    <>
      <PageHeader title={t('Kitchen display')}
        subtitle={t('Oldest ticket first, dishes in course order. The screen refreshes by itself every 2 seconds.')} />

      <div className="stats">
        <div className="stat tone-line-accent"><span className="stat-label">{t('Open tickets')}</span><span className="stat-value">{tickets.length}</span></div>
        <div className="stat tone-line-RESERVED"><span className="stat-label">{t('New dishes')}</span><span className="stat-value">{waiting}</span></div>
        <div className="stat tone-line-OCCUPIED"><span className="stat-label">{t('Cooking')}</span><span className="stat-value">{cooking}</span></div>
      </div>

      <FormError error={error} />
      {tickets.length === 0 && <Empty icon="chef" title={t('No dishes waiting')}>{t('New tickets appear here by themselves.')}</Empty>}
      <div className="ticket-rail">
        {tickets.map((ticket) => (
          <Ticket key={ticket.id} ticket={ticket} now={now} onMove={move} onSendBack={sendBack} />
        ))}
      </div>
      {finished.length > 0 && (
        <details className="card panel">
          <summary>{t('Finished tickets ({n})', { n: finished.length })}</summary>
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
  const t = useT();
  const waited = minutesSince(ticket.sentAt, now);
  const late = waited >= LATE_MINUTES && ticket.items.some((item) => item.status === 'NEW' || item.status === 'IN_PROGRESS');
  return (
    <article className={`ticket${late ? ' late' : ''}`}>
      <header className="ticket-head">
        <span className="ticket-table">{t('Table {n}', { n: ticket.tableNumber })}</span>
        <span className="ticket-time">
          <Icon name="clock" size={14} />
          {timeOf(ticket.sentAt)} · {waited === 0 ? t('just now') : t('{n} min ago', { n: waited })}
        </span>
      </header>
      {groupByCourse(ticket.items).map(([course, items]) => (
        <section key={course} className="ticket-course">
          <h3>{t(COURSE_LABELS[course])}</h3>
          <ul>
            {items.map((item) => (
              <li key={item.id} className={`ticket-item item-${item.status}`}>
                <div className="ticket-dish">
                  <strong>{item.quantity} × {t(item.menuItemName)}</strong>
                  {item.note && <span className="ticket-note">{item.note}</span>}
                </div>
                <div className="ticket-actions">
                  {item.status === 'NEW' && (
                    <button type="button" className="btn btn-sm btn-primary" onClick={() => onMove(item, 'start')}>{t('Start')}</button>
                  )}
                  {item.status === 'IN_PROGRESS' && (
                    <button type="button" className="btn btn-sm btn-primary" onClick={() => onMove(item, 'ready')}>
                      <Icon name="check" size={14} />{t('Ready')}
                    </button>
                  )}
                  {item.status === 'READY' && (
                    <button type="button" className="btn btn-sm btn-ghost" onClick={() => onSendBack(item)}>{t('Sent back')}</button>
                  )}
                  {item.status !== 'NEW' && <span className="muted">{t(item.status === 'IN_PROGRESS' ? 'Cooking' : ITEM_STATUS_LABELS[item.status])}</span>}
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
  const t = useT();
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
    <details className="card panel">
      <summary>{t('Sold-out dishes ({n})', { n: soldOut })}</summary>
      <FormError error={error} />
      <ul className="stock-list">
        {items.map((item) => (
          <li key={item.id} className={item.inStock ? '' : 'is-out'}>
            <span>{t(item.name)}</span>
            <button type="button" className={`btn btn-sm ${item.inStock ? 'btn-danger' : 'btn-primary'}`} onClick={() => toggle(item)}>
              {item.inStock ? t('Mark sold out') : t('Back in stock')}
            </button>
          </li>
        ))}
      </ul>
    </details>
  );
}
