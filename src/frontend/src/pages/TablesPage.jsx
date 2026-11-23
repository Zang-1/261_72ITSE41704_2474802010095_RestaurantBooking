import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import FormField, { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Badge, Notice, PageHeader } from '../components/ui.jsx';
import { STATUS_LABELS } from '../format.js';

const EMPTY_TABLE = { id: null, number: '', capacity: '4', sectionId: '' };

/** UC-02 Manage tables and sections (Manager only). */
export default function TablesPage() {
  const t = useT();
  const [tables, setTables] = useState([]);
  const [sections, setSections] = useState([]);
  const [tableForm, setTableForm] = useState(EMPTY_TABLE);
  const [tableError, setTableError] = useState(null);
  const [sectionName, setSectionName] = useState('');
  const [sectionError, setSectionError] = useState(null);
  const [settings, setSettings] = useState(null);
  const [settingsError, setSettingsError] = useState(null);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    api.get('/tables?includeInactive=true').then(setTables).catch(setTableError);
    api.get('/sections').then(setSections).catch(setSectionError);
  }, []);

  useEffect(() => {
    load();
    api.get('/settings/reservation')
      .then((s) => setSettings({ diningMinutes: String(s.diningMinutes), turnMinutes: String(s.turnMinutes) }))
      .catch(setSettingsError);
  }, [load]);

  async function saveTable(event) {
    event.preventDefault();
    setTableError(null);
    setMessage('');
    const body = {
      number: tableForm.number === '' ? null : Number(tableForm.number),
      capacity: tableForm.capacity === '' ? null : Number(tableForm.capacity),
      sectionId: tableForm.sectionId === '' ? null : Number(tableForm.sectionId),
    };
    try {
      const saved = tableForm.id
        ? await api.put(`/tables/${tableForm.id}`, body)
        : await api.post('/tables', body);
      setMessage(t('Table {n} saved.', { n: saved.number }));
      setTableForm(EMPTY_TABLE);
      load();
    } catch (err) {
      setTableError(err);
    }
  }

  async function toggleActive(table) {
    setTableError(null);
    setMessage('');
    try {
      await api.post(`/tables/${table.id}/${table.active ? 'deactivate' : 'activate'}`);
      setMessage(table.active ? t('Table {n} deactivated.', { n: table.number }) : t('Table {n} activated.', { n: table.number }));
      load();
    } catch (err) {
      setTableError(err);
    }
  }

  async function addSection(event) {
    event.preventDefault();
    setSectionError(null);
    setMessage('');
    try {
      const saved = await api.post('/sections', { name: sectionName });
      setMessage(t('Section {name} added.', { name: saved.name }));
      setSectionName('');
      load();
    } catch (err) {
      setSectionError(err);
    }
  }

  async function saveSettings(event) {
    event.preventDefault();
    setSettingsError(null);
    setMessage('');
    try {
      await api.put('/settings/reservation', {
        diningMinutes: settings.diningMinutes === '' ? null : Number(settings.diningMinutes),
        turnMinutes: settings.turnMinutes === '' ? null : Number(settings.turnMinutes),
      });
      setMessage(t('Reservation settings saved.'));
    } catch (err) {
      setSettingsError(err);
    }
  }

  const tableFields = tableError?.fieldErrors || {};
  const updateTable = (field) => (event) => setTableForm((f) => ({ ...f, [field]: event.target.value }));
  const activeCount = tables.filter((table) => table.active).length;
  const seats = tables.filter((table) => table.active).reduce((sum, table) => sum + table.capacity, 0);

  return (
    <>
      <PageHeader title={t('Tables and sections')}
        subtitle={t('The floor plan used by bookings and walk-ins. A table with future bookings cannot be deactivated.')} />

      <div className="stats">
        <div className="stat tone-line-accent"><span className="stat-label">{t('Tables in use')}</span><span className="stat-value">{activeCount}</span></div>
        <div className="stat"><span className="stat-label">{t('Seats')}</span><span className="stat-value">{seats}</span></div>
        <div className="stat"><span className="stat-label">{t('Sections')}</span><span className="stat-value">{sections.length}</span></div>
      </div>

      <Notice>{message}</Notice>

      <form className="card" onSubmit={saveTable} noValidate>
        <div className="card-head">
          <h2>{tableForm.id ? t('Edit table {n}', { n: tableForm.number }) : t('Add a table')}</h2>
          {tableForm.id && <button type="button" className="btn btn-ghost btn-sm" onClick={() => setTableForm(EMPTY_TABLE)}>{t('Cancel')}</button>}
        </div>
        <div className="form-row">
          <FormField label={t('Table number')} error={tableFields.number}>
            <input type="number" min="1" value={tableForm.number} onChange={updateTable('number')} />
          </FormField>
          <FormField label={t('Seats (1-20)')} error={tableFields.capacity}>
            <input type="number" min="1" max="20" value={tableForm.capacity} onChange={updateTable('capacity')} />
          </FormField>
          <FormField label={t('Section')} error={tableFields.sectionId}>
            <select value={tableForm.sectionId} onChange={updateTable('sectionId')}>
              <option value="">{t('Choose…')}</option>
              {sections.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </FormField>
          <button type="submit" className="btn btn-primary">
            <Icon name={tableForm.id ? 'check' : 'plus'} size={16} />{tableForm.id ? t('Save') : t('Add table')}
          </button>
        </div>
        <FormError error={tableError && !Object.keys(tableFields).length ? tableError : null} />
      </form>

      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr>
              <th>{t('Table')}</th><th className="num">{t('Seats')}</th><th>{t('Section')}</th><th>{t('Status')}</th>
              <th>{t('In use')}</th><th><span className="sr-only">{t('Actions')}</span></th>
            </tr>
          </thead>
          <tbody>
            {tables.map((table) => (
              <tr key={table.id} className={table.active ? '' : 'dimmed'}>
                <td><strong>{t('Table {n}', { n: table.number })}</strong></td>
                <td className="num">{table.capacity}</td>
                <td>{table.sectionName}</td>
                <td><Badge tone={table.status}>{t(STATUS_LABELS[table.status])}</Badge></td>
                <td>{table.active ? t('Yes') : <Badge tone="INACTIVE">{t('No')}</Badge>}</td>
                <td className="actions">
                  <div>
                    <button type="button" className="btn btn-sm" onClick={() => setTableForm({
                      id: table.id, number: String(table.number), capacity: String(table.capacity), sectionId: String(table.sectionId),
                    })}><Icon name="edit" size={14} />{t('Edit')}</button>
                    <button type="button" className={`btn btn-sm${table.active ? ' btn-danger' : ''}`} onClick={() => toggleActive(table)}>
                      {table.active ? t('Deactivate') : t('Activate')}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="grid-2">
        <form className="card" onSubmit={addSection} noValidate>
          <div className="card-head">
            <div><h2>{t('Sections')}</h2><p className="muted">{sections.map((s) => s.name).join(' · ')}</p></div>
          </div>
          <div className="form-row">
            <FormField label={t('New section')} error={sectionError?.fieldErrors?.name}>
              <input value={sectionName} onChange={(e) => setSectionName(e.target.value)} maxLength={50} />
            </FormField>
            <button type="submit" className="btn"><Icon name="plus" size={16} />{t('Add section')}</button>
          </div>
          <FormError error={sectionError && !sectionError.fieldErrors?.name ? sectionError : null} />
        </form>

        {settings && (
          <form className="card" onSubmit={saveSettings} noValidate>
            <div className="card-head">
              <div><h2>{t('Reservation timing')}</h2><p className="muted">{t('Used by the no-double-booking rule (BR-01).')}</p></div>
            </div>
            <div className="form-row">
              <FormField label={t('Dining duration (min)')} error={settingsError?.fieldErrors?.diningMinutes}>
                <input type="number" value={settings.diningMinutes}
                  onChange={(e) => setSettings((s) => ({ ...s, diningMinutes: e.target.value }))} />
              </FormField>
              <FormField label={t('Turn-time (min)')} error={settingsError?.fieldErrors?.turnMinutes}>
                <input type="number" value={settings.turnMinutes}
                  onChange={(e) => setSettings((s) => ({ ...s, turnMinutes: e.target.value }))} />
              </FormField>
              <button type="submit" className="btn">{t('Save')}</button>
            </div>
          </form>
        )}
      </div>
    </>
  );
}
