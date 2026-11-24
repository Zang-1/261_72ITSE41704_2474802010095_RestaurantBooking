import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { useT } from '../prefs.jsx';
import FormField, { FormError } from '../components/FormField.jsx';
import Icon from '../components/Icon.jsx';
import { Badge, Notice, PageHeader } from '../components/ui.jsx';
import { COURSE_LABELS, formatPrice } from '../format.js';

const EMPTY_ITEM = { id: null, name: '', description: '', price: '', course: 'MAIN', categoryId: '' };

/** UC-07 Manage menu (Manager only): items, categories, and the in-stock switch. */
export default function MenuPage() {
  const t = useT();
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [itemForm, setItemForm] = useState(EMPTY_ITEM);
  const [itemError, setItemError] = useState(null);
  const [categoryName, setCategoryName] = useState('');
  const [categoryError, setCategoryError] = useState(null);
  const [message, setMessage] = useState('');
  const [query, setQuery] = useState('');

  const load = useCallback(() => {
    api.get('/menu/items?includeInactive=true').then(setItems).catch(setItemError);
    api.get('/menu/categories').then(setCategories).catch(setCategoryError);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function saveItem(event) {
    event.preventDefault();
    setItemError(null);
    setMessage('');
    const body = {
      name: itemForm.name,
      description: itemForm.description,
      price: itemForm.price === '' ? null : Number(itemForm.price),
      course: itemForm.course,
      categoryId: itemForm.categoryId === '' ? null : Number(itemForm.categoryId),
    };
    try {
      const saved = itemForm.id
        ? await api.put(`/menu/items/${itemForm.id}`, body)
        : await api.post('/menu/items', body);
      setMessage(t('"{name}" saved.', { name: saved.name }));
      setItemForm(EMPTY_ITEM);
      load();
    } catch (err) {
      setItemError(err);
    }
  }

  async function runAction(item, action, doneText) {
    setItemError(null);
    setMessage('');
    try {
      await api.post(`/menu/items/${item.id}/${action}`);
      setMessage(t(doneText, { name: item.name }));
      load();
    } catch (err) {
      setItemError(err);
    }
  }

  async function addCategory(event) {
    event.preventDefault();
    setCategoryError(null);
    setMessage('');
    try {
      const saved = await api.post('/menu/categories', { name: categoryName });
      setMessage(t('Category {name} added.', { name: saved.name }));
      setCategoryName('');
      load();
    } catch (err) {
      setCategoryError(err);
    }
  }

  const fields = itemError?.fieldErrors || {};
  const update = (field) => (event) => setItemForm((f) => ({ ...f, [field]: event.target.value }));
  const shown = items.filter((item) => item.name.toLowerCase().includes(query.trim().toLowerCase()));

  return (
    <>
      <PageHeader title={t('Menu')}
        subtitle={t('Dishes, prices and courses. A sold-out dish disappears from every order screen within 5 seconds.')} />

      <div className="stats">
        <div className="stat tone-line-accent"><span className="stat-label">{t('Dishes on the menu')}</span><span className="stat-value">{items.filter((i) => i.active).length}</span></div>
        <div className="stat tone-line-NEEDS_CLEANING"><span className="stat-label">{t('Sold out')}</span><span className="stat-value">{items.filter((i) => i.active && !i.inStock).length}</span></div>
        <div className="stat"><span className="stat-label">{t('Categories')}</span><span className="stat-value">{categories.length}</span></div>
      </div>

      <Notice>{message}</Notice>

      <form className="card" onSubmit={saveItem} noValidate>
        <div className="card-head">
          <h2>{itemForm.id ? t('Edit "{name}"', { name: itemForm.name }) : t('Add a menu item')}</h2>
          {itemForm.id && <button type="button" className="btn btn-ghost btn-sm" onClick={() => setItemForm(EMPTY_ITEM)}>{t('Cancel')}</button>}
        </div>
        <div className="form-row">
          <FormField label={t('Name')} error={fields.name}>
            <input value={itemForm.name} onChange={update('name')} maxLength={100} />
          </FormField>
          <FormField label={t('Price (VND)')} error={fields.price}>
            <input type="number" min="1" value={itemForm.price} onChange={update('price')} />
          </FormField>
          <FormField label={t('Category')} error={fields.categoryId}>
            <select value={itemForm.categoryId} onChange={update('categoryId')}>
              <option value="">{t('Choose…')}</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </FormField>
          <FormField label={t('Course')} error={fields.course}>
            <select value={itemForm.course} onChange={update('course')}>
              {Object.entries(COURSE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{t(label)}</option>
              ))}
            </select>
          </FormField>
        </div>
        <div className="form-row">
          <FormField label={t('Description (optional)')} error={fields.description}>
            <input value={itemForm.description} onChange={update('description')} maxLength={300} />
          </FormField>
          <button type="submit" className="btn btn-primary">
            <Icon name={itemForm.id ? 'check' : 'plus'} size={16} />{itemForm.id ? t('Save') : t('Add item')}
          </button>
        </div>
        <FormError error={itemError && !Object.keys(fields).length ? itemError : null} />
      </form>

      <div className="toolbar">
        <label className="inline-field">
          <Icon name="search" size={16} />
          <span className="sr-only">{t('Search dishes')}</span>
          <input className="input" type="search" placeholder={t('Search dishes')} value={query} onChange={(e) => setQuery(e.target.value)} />
        </label>
        <span className="muted">{t('{n} dishes', { n: shown.length })}</span>
      </div>

      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr>
              <th>{t('Dish')}</th><th>{t('Category')}</th><th>{t('Course')}</th><th className="num">{t('Price')}</th>
              <th>{t('Status')}</th><th><span className="sr-only">{t('Actions')}</span></th>
            </tr>
          </thead>
          <tbody>
            {shown.map((item) => (
              <tr key={item.id} className={item.active ? '' : 'dimmed'}>
                <td><strong>{t(item.name)}</strong>{item.description && <span className="cell-sub">{t(item.description)}</span>}</td>
                <td>{t(item.categoryName)}</td>
                <td>{t(COURSE_LABELS[item.course])}</td>
                <td className="num">{formatPrice(item.price)}</td>
                <td>
                  {!item.active ? <Badge tone="INACTIVE">{t('Off the menu')}</Badge>
                    : item.inStock ? <Badge tone="AVAILABLE">{t('In stock')}</Badge>
                      : <Badge tone="OPEN">{t('Sold out')}</Badge>}
                </td>
                <td className="actions">
                  <div>
                    <button type="button" className="btn btn-sm" onClick={() => setItemForm({
                      id: item.id, name: item.name, description: item.description || '',
                      price: String(item.price), course: item.course, categoryId: String(item.categoryId),
                    })}><Icon name="edit" size={14} />{t('Edit')}</button>
                    <button type="button" className="btn btn-sm" onClick={() => (item.inStock
                      ? runAction(item, 'out-of-stock', '"{name}" marked out of stock.')
                      : runAction(item, 'restock', '"{name}" is back in stock.'))}>
                      {item.inStock ? t('Sold out') : t('Restock')}
                    </button>
                    <button type="button" className={`btn btn-sm${item.active ? ' btn-danger' : ''}`}
                      onClick={() => (item.active
                        ? runAction(item, 'deactivate', '"{name}" removed from the menu.')
                        : runAction(item, 'activate', '"{name}" added back to the menu.'))}>
                      {item.active ? t('Remove') : t('Add back')}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form className="card" onSubmit={addCategory} noValidate>
        <div className="card-head">
          <div><h2>{t('Categories')}</h2><p className="muted">{categories.map((c) => c.name).join(' · ')}</p></div>
        </div>
        <div className="form-row">
          <FormField label={t('New category')} error={categoryError?.fieldErrors?.name}>
            <input value={categoryName} onChange={(e) => setCategoryName(e.target.value)} maxLength={50} />
          </FormField>
          <button type="submit" className="btn"><Icon name="plus" size={16} />{t('Add category')}</button>
        </div>
        <FormError error={categoryError && !categoryError.fieldErrors?.name ? categoryError : null} />
      </form>
    </>
  );
}
