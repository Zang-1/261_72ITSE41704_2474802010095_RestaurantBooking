export const STATUS_LABELS = {
  AVAILABLE: 'Available',
  RESERVED: 'Reserved',
  OCCUPIED: 'Occupied',
  NEEDS_CLEANING: 'Needs cleaning',
};

export const RESERVATION_STATUS_LABELS = {
  CONFIRMED: 'Confirmed',
  CHECKED_IN: 'Checked in',
  CANCELLED: 'Cancelled',
  NO_SHOW: 'No-show',
};

/** Today's date as YYYY-MM-DD in the browser's time zone. */
export function todayIso() {
  return new Date().toLocaleDateString('en-CA');
}

/** "2026-10-24T19:00:00" -> "19:00" */
export function timeOf(dateTime) {
  return dateTime ? dateTime.slice(11, 16) : '';
}

/** "2026-10-24T19:00:00" -> "24/10/2026 19:00" */
export function formatDateTime(dateTime) {
  if (!dateTime) return '';
  const [date, time] = dateTime.split('T');
  const [year, month, day] = date.split('-');
  return `${day}/${month}/${year} ${time.slice(0, 5)}`;
}

/** [[sectionName, tables[]], ...] keeping the order of the tables. */
export function groupBySection(tables) {
  const groups = new Map();
  for (const table of tables) {
    if (!groups.has(table.sectionName)) groups.set(table.sectionName, []);
    groups.get(table.sectionName).push(table);
  }
  return [...groups.entries()];
}
