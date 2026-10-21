import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import FormField, { FormError } from '../components/FormField.jsx';
import { STATUS_LABELS } from '../format.js';

const EMPTY_TABLE = { id: null, number: '', capacity: '4', sectionId: '' };

/** UC-02 Manage tables and sections (Manager only). */
export default function TablesPage() {
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
    const body = {
      number: tableForm.number === '' ? null : Number(tableForm.number),
      capacity: tableForm.capacity === '' ? null : Number(tableForm.capacity),
      sectionId: tableForm.sectionId === '' ? null : Number(tableForm.sectionId),
    };
    try {
      const saved = tableForm.id
        ? await api.put(`/tables/${tableForm.id}`, body)
        : await api.post('/tables', body);
      setMessage(`Table ${saved.number} saved.`);
      setTableForm(EMPTY_TABLE);
      load();
    } catch (err) {
      setTableError(err);
    }
  }

  async function toggleActive(table) {
    setTableError(null);
    try {
      await api.post(`/tables/${table.id}/${table.active ? 'deactivate' : 'activate'}`);
      setMessage(`Table ${table.number} ${table.active ? 'deactivated' : 'activated'}.`);
      load();
    } catch (err) {
      setTableError(err);
    }
  }

  async function addSection(event) {
    event.preventDefault();
    setSectionError(null);
    try {
      const saved = await api.post('/sections', { name: sectionName });
      setMessage(`Section ${saved.name} added.`);
      setSectionName('');
      load();
    } catch (err) {
      setSectionError(err);
    }
  }

  async function saveSettings(event) {
    event.preventDefault();
    setSettingsError(null);
    try {
      await api.put('/settings/reservation', {
        diningMinutes: settings.diningMinutes === '' ? null : Number(settings.diningMinutes),
        turnMinutes: settings.turnMinutes === '' ? null : Number(settings.turnMinutes),
      });
      setMessage('Reservation settings saved.');
    } catch (err) {
      setSettingsError(err);
    }
  }

  const tableFields = tableError?.fieldErrors || {};
  const updateTable = (field) => (event) => setTableForm((f) => ({ ...f, [field]: event.target.value }));

  return (
    <>
      <h1>Tables and sections</h1>
      {message && <p className="notice">{message}</p>}

      <form className="card" onSubmit={saveTable} noValidate>
        <h2>{tableForm.id ? `Edit table ${tableForm.number}` : 'Add a table'}</h2>
        <div className="row">
          <FormField label="Table number" error={tableFields.number}>
            <input type="number" min="1" value={tableForm.number} onChange={updateTable('number')} />
          </FormField>
          <FormField label="Seats (1-20)" error={tableFields.capacity}>
            <input type="number" min="1" max="20" value={tableForm.capacity} onChange={updateTable('capacity')} />
          </FormField>
          <FormField label="Section" error={tableFields.sectionId}>
            <select value={tableForm.sectionId} onChange={updateTable('sectionId')}>
              <option value="">Choose…</option>
              {sections.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </FormField>
          <button type="submit" className="primary">{tableForm.id ? 'Save' : 'Add table'}</button>
          {tableForm.id && <button type="button" onClick={() => setTableForm(EMPTY_TABLE)}>Cancel</button>}
        </div>
        <FormError error={tableError && !Object.keys(tableFields).length ? tableError : null} />
      </form>

      <table className="data">
        <thead>
          <tr><th>Table</th><th>Seats</th><th>Section</th><th>Status</th><th>In use</th><th /></tr>
        </thead>
        <tbody>
          {tables.map((t) => (
            <tr key={t.id} className={t.active ? '' : 'dimmed'}>
              <td>{t.number}</td>
              <td>{t.capacity}</td>
              <td>{t.sectionName}</td>
              <td><span className={`badge status-${t.status}`}>{STATUS_LABELS[t.status]}</span></td>
              <td>{t.active ? 'Yes' : 'No'}</td>
              <td className="actions">
                <button type="button" onClick={() => setTableForm({
                  id: t.id, number: String(t.number), capacity: String(t.capacity), sectionId: String(t.sectionId),
                })}>Edit</button>
                <button type="button" className={t.active ? 'danger' : ''} onClick={() => toggleActive(t)}>
                  {t.active ? 'Deactivate' : 'Activate'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="two-columns">
        <form className="card" onSubmit={addSection} noValidate>
          <h2>Sections</h2>
          <p className="muted">{sections.map((s) => s.name).join(', ')}</p>
          <div className="row">
            <FormField label="New section" error={sectionError?.fieldErrors?.name}>
              <input value={sectionName} onChange={(e) => setSectionName(e.target.value)} maxLength={50} />
            </FormField>
            <button type="submit">Add section</button>
          </div>
        </form>

        {settings && (
          <form className="card" onSubmit={saveSettings} noValidate>
            <h2>Reservation timing</h2>
            <p className="muted">Used by the no-double-booking rule (BR-01).</p>
            <div className="row">
              <FormField label="Dining duration (min)" error={settingsError?.fieldErrors?.diningMinutes}>
                <input type="number" value={settings.diningMinutes}
                       onChange={(e) => setSettings((s) => ({ ...s, diningMinutes: e.target.value }))} />
              </FormField>
              <FormField label="Turn-time (min)" error={settingsError?.fieldErrors?.turnMinutes}>
                <input type="number" value={settings.turnMinutes}
                       onChange={(e) => setSettings((s) => ({ ...s, turnMinutes: e.target.value }))} />
              </FormField>
              <button type="submit">Save</button>
            </div>
          </form>
        )}
      </div>
    </>
  );
}
