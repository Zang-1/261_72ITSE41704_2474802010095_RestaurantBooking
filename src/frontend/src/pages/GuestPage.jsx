import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { homePath, useAuth } from '../auth.jsx';
import { useT } from '../prefs.jsx';
import FormField, { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Prefs } from '../components/Layout.jsx';
import { formatDateTime, formatPrice, todayIso } from '../format.js';

/**
 * Public website for guests: no login. A booking made here goes through the same rules as the host's
 * (BR-01, BR-02) and appears on the staff Reservations and Floor screens straight away.
 */
export default function GuestPage() {
  const t = useT();
  const { user } = useAuth();

  return (
    <div className="guest">
      <header className="g-header">
        <div className="g-wrap g-header-row">
          <a className="brand g-brand" href="#top">
            <span className="brand-mark">EO</span>
            <span className="brand-name"><strong>Ember &amp; Oak</strong><span>{t('Steak & Pasta')}</span></span>
          </a>
          <nav className="g-nav" aria-label={t('Sections of the page')}>
            <a href="#book">{t('Book a table')}</a>
            <a href="#spaces">{t('Our rooms')}</a>
            <a href="#menu">{t('Menu')}</a>
          </nav>
          <div className="g-header-actions">
            <Prefs className="g-prefs" buttonClass="btn btn-ghost btn-sm" />
            <Link className="btn btn-sm" to={user ? homePath(user.role) : '/login'}>
              {user ? t('Staff app') : t('Staff login')}
            </Link>
          </div>
        </div>
      </header>

      <section className="g-hero" id="top">
        <img className="g-hero-img" src="/img/interior.jpg" alt={t('The dining room of Ember & Oak in the evening')} />
        <div className="g-wrap g-hero-body">
          <p className="g-eyebrow">{t('Steak & pasta · Ho Chi Minh City')}</p>
          <h1>{t('Steak, pasta and a table kept for you')}</h1>
          <p className="g-lead">{t('Book online in a minute. Choose a time, tell us how many you are, and we keep the right table ready.')}</p>
          <div className="g-hero-actions">
            <a className="btn btn-primary g-btn-lg" href="#book"><Icon name="calendar" size={18} />{t('Book a table')}</a>
            <a className="btn g-btn-lg g-btn-light" href="#menu">{t('See the menu')}</a>
          </div>
          <ul className="g-facts">
            <li><strong>11:00 – 21:00</strong><span>{t('online bookings, every day')}</span></li>
            <li><strong>{t('45 min')}</strong><span>{t('we hold your table if you run late')}</span></li>
            <li><strong>{t('4 rooms')}</strong><span>{t('from a table for two to the cellar for twelve')}</span></li>
          </ul>
        </div>
      </section>

      <BookingSection />
      <SpacesSection />
      <MenuSection />

      <footer className="g-footer">
        <div className="g-wrap g-footer-row">
          <div className="brand g-brand">
            <span className="brand-mark">EO</span>
            <span className="brand-name"><strong>Ember &amp; Oak</strong><span>{t('Steak & Pasta')}</span></span>
          </div>
          <p className="muted">{t('Bookings 11:00 – 21:00 every day. Ho Chi Minh City.')}</p>
          <p className="muted g-small">{t('Student project for 72ITSE41704, Van Lang University. Photos are AI-generated illustrations.')}</p>
        </div>
      </footer>
    </div>
  );
}

function BookingSection() {
  const t = useT();
  const [date, setDate] = useState(todayIso());
  const [partySize, setPartySize] = useState('2');
  const [slots, setSlots] = useState(null);
  const [time, setTime] = useState('');
  const [form, setForm] = useState({ guestName: '', phone: '', note: '' });
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [booked, setBooked] = useState(null);

  const loadSlots = useCallback(() => {
    const size = Number(partySize);
    if (!date || !(size >= 1 && size <= 12)) {
      setSlots([]);
      return;
    }
    setSlots(null);
    api.get(`/public/slots?date=${date}&partySize=${size}`)
      .then((list) => {
        setSlots(list);
        setTime((current) => (list.some((s) => s.time.slice(0, 5) === current && s.available) ? current : ''));
      })
      .catch((err) => { setSlots([]); setError(err); });
  }, [date, partySize]);

  useEffect(loadSlots, [loadSlots]);

  const fieldErrors = error?.fieldErrors || {};
  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const result = await api.post('/public/reservations', { ...form, partySize: Number(partySize), date, startTime: time || null });
      setBooked(result);
    } catch (err) {
      setError(err);
      if (err.status === 409) loadSlots();  // the time was just taken: show the times that are still free
    } finally {
      setSaving(false);
    }
  }

  function bookAgain() {
    setBooked(null);
    setForm({ guestName: '', phone: '', note: '' });
    setTime('');
    loadSlots();
  }

  return (
    <section className="g-section" id="book">
      <div className="g-wrap g-book">
        <div className="g-book-intro">
          <p className="g-eyebrow">{t('Book a table')}</p>
          <h2>{t('Choose a time, we choose the table')}</h2>
          <p className="muted">{t('Only times with a free table for your party can be chosen. Your booking reaches our hosts the moment you confirm it.')}</p>
          <ul className="g-steps">
            <li><Icon name="clock" /><div><strong>{t('Arrive up to 30 minutes early')}</strong><span>{t('Your table is ready from half an hour before your time.')}</span></div></li>
            <li><Icon name="calendar" /><div><strong>{t('Held for 45 minutes')}</strong><span>{t('Running late? We keep the table for 45 minutes after your time.')}</span></div></li>
            <li><Icon name="users" /><div><strong>{t('Parties up to 12')}</strong><span>{t('For a larger group, please book two tables at the door.')}</span></div></li>
          </ul>
        </div>

        <div className="card g-book-card">
          {booked ? (
            <div className="g-confirm" role="status">
              <span className="g-confirm-icon"><Icon name="check" size={28} /></span>
              <h3>{t('Your table is booked')}</h3>
              <p>{t('{name}, we look forward to seeing you.', { name: booked.guestName })}</p>
              <dl className="totals">
                <dt>{t('Booking code')}</dt><dd><code>{booked.code}</code></dd>
                <dt>{t('Date and time')}</dt><dd>{formatDateTime(booked.startTime)}</dd>
                <dt>{t('Guests')}</dt><dd>{booked.partySize}</dd>
              </dl>
              <p className="muted">{t('Show the booking code at the door. To change or cancel, call the restaurant and give the code.')}</p>
              <button type="button" className="btn" onClick={bookAgain}>{t('Make another booking')}</button>
            </div>
          ) : (
            <form onSubmit={submit} noValidate>
              <FormField label={t('Your name')} error={fieldErrors.guestName}>
                <input value={form.guestName} onChange={update('guestName')} maxLength={100} autoComplete="name" />
              </FormField>
              <FormField label={t('Phone')} error={fieldErrors.phone}>
                <input value={form.phone} onChange={update('phone')} placeholder="0901234567" maxLength={15} inputMode="tel" autoComplete="tel" />
              </FormField>
              <div className="form-row">
                <FormField label={t('Date')} error={fieldErrors.date}>
                  <input type="date" min={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} />
                </FormField>
                <FormField label={t('Time')} error={fieldErrors.startTime}>
                  <select value={time} onChange={(e) => setTime(e.target.value)} disabled={!slots || slots.length === 0}>
                    <option value="">
                      {slots === null ? t('Loading…') : slots.length === 0 ? t('No times left this day') : t('Choose a time')}
                    </option>
                    {(slots || []).map((slot) => {
                      const value = slot.time.slice(0, 5);
                      return (
                        <option key={value} value={value} disabled={!slot.available}>
                          {slot.available ? value : `${value} · ${t('full')}`}
                        </option>
                      );
                    })}
                  </select>
                </FormField>
              </div>
              <FormField label={t('Number of guests (including you)')} error={fieldErrors.partySize}>
                <input type="number" min="1" max="12" value={partySize} onChange={(e) => setPartySize(e.target.value)} />
              </FormField>
              <FormField label={t('Note (optional)')} error={fieldErrors.note}>
                <textarea rows="3" value={form.note} onChange={update('note')} maxLength={150} placeholder={t('e.g. birthday, window seat')} />
              </FormField>

              <FormError error={error && !Object.keys(fieldErrors).length ? error : null} />
              <button type="submit" className="btn btn-primary btn-block g-btn-lg" disabled={saving}>
                {saving ? t('Booking…') : t('Book a table')}
              </button>
            </form>
          )}
        </div>
      </div>
    </section>
  );
}

function SpacesSection() {
  const t = useT();
  const rooms = [
    { img: '/img/interior.jpg', name: 'Hearth Hall', text: t('The main dining room, with tables for two to six.') },
    { img: '/img/vip.jpg', name: 'Cellar Room', text: t('Private tables for groups of eight to twelve.') },
    { img: '/img/rooftop.jpg', name: 'Sky Terrace', text: t('Rooftop tables for two to six, best after sunset.') },
  ];
  return (
    <section className="g-section g-alt" id="spaces">
      <div className="g-wrap">
        <p className="g-eyebrow">{t('Our rooms')}</p>
        <h2>{t('Four rooms, one kitchen')}</h2>
        <p className="muted g-measure">{t('Duo Lounge, the fourth room, is kept for tables of two. Tell us in the note if you would like a particular room.')}</p>
        <div className="g-rooms">
          {rooms.map((room) => (
            <article key={room.name} className="g-room">
              <div className="g-room-img"><img src={room.img} alt={room.name} loading="lazy" /></div>
              <h3>{room.name}</h3>
              <p className="muted">{room.text}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function MenuSection() {
  const t = useT();
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    api.get('/public/menu').then(setItems).catch(() => setItems([]));
  }, []);

  // one block per category, in meal order; a category the Manager adds later goes last
  const categories = [...new Set(items.map((item) => item.categoryName))]
    .sort((a, b) => mealOrder(a) - mealOrder(b));
  const dishesOf = (category) => items.filter((item) => item.categoryName === category);

  return (
    <section className="g-menu-lux" id="menu">
      <div className="g-menu-banner">
        <img src="/img/ribeye.jpg" alt={t('A ribeye steak on the grill')} loading="lazy" />
        <div className="g-wrap g-menu-head">
          <p className="g-eyebrow">{t('Menu')}</p>
          <h2>{t("Tonight's menu")}</h2>
          <p>{t('Dishes sold out in the kitchen disappear from this list.')}</p>
        </div>
      </div>
      {/* Preview: one card per category with two dishes; the full menu opens below on request */}
      <div className="g-wrap g-menu-preview">
        {categories.map((category) => {
          const dishes = dishesOf(category);
          return (
            <article key={category} className="g-menu-card">
              <h3>{t(category)}</h3>
              <p className="g-menu-count">{t('{n} dishes', { n: dishes.length })} · {t('from {price}', { price: formatPrice(Math.min(...dishes.map((d) => d.price))) })}</p>
              <ul>
                {dishes.slice(0, 2).map((item) => <li key={item.id}>{t(item.name)}</li>)}
              </ul>
            </article>
          );
        })}
      </div>
      <div className="g-wrap g-menu-toggle">
        <button type="button" className="btn g-btn-lg g-btn-light" aria-expanded={open} aria-controls="full-menu"
          onClick={() => setOpen((v) => !v)}>
          {open ? t('Close the full menu') : t('See the full menu')}
        </button>
      </div>

      <div id="full-menu" className="g-wrap g-menu-book" hidden={!open}>
        {categories.map((category) => (
          <div key={category} className="g-menu-group">
            <h3><span>{t(category)}</span></h3>
            <ul>
              {dishesOf(category).map((item) => (
                <li key={item.id}>
                  <div className="g-dish-line">
                    <span className="g-dish-name">{t(item.name)}</span>
                    <span className="g-leader" aria-hidden="true" />
                    <span className="g-dish-price">{formatPrice(item.price)}</span>
                  </div>
                  {item.description && <p className="g-dish-desc">{t(item.description)}</p>}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="g-wrap g-menu-cta">
        <a className="btn btn-primary g-btn-lg" href="#book"><Icon name="calendar" size={18} />{t('Book a table')}</a>
      </div>
    </section>
  );
}

const MEAL_ORDER = ['Appetizers', 'Soups', 'Main dishes', 'Desserts', 'Drinks'];

function mealOrder(category) {
  const index = MEAL_ORDER.indexOf(category);
  return index === -1 ? MEAL_ORDER.length : index;
}
