import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import FormField, { FormError } from '../components/FormField.jsx';
import { COURSE_LABELS, formatPrice } from '../format.js';

const EMPTY_ITEM = { id: null, name: '', description: '', price: '', course: 'MAIN', categoryId: '' };

/** UC-08 Manage menu (Manager only): items, categories, and the in-stock switch. */
export default function MenuPage() {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [itemForm, setItemForm] = useState(EMPTY_ITEM);
  const [itemError, setItemError] = useState(null);
  const [categoryName, setCategoryName] = useState('');
  const [categoryError, setCategoryError] = useState(null);
  const [message, setMessage] = useState('');

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
      setMessage(`"${saved.name}" saved.`);
      setItemForm(EMPTY_ITEM);
      load();
    } catch (err) {
      setItemError(err);
    }
  }

  async function runAction(item, action, doneText) {
    setItemError(null);
    try {
      await api.post(`/menu/items/${item.id}/${action}`);
      setMessage(`"${item.name}" ${doneText}.`);
      load();
    } catch (err) {
      setItemError(err);
    }
  }

  async function addCategory(event) {
    event.preventDefault();
    setCategoryError(null);
    try {
      const saved = await api.post('/menu/categories', { name: categoryName });
      setMessage(`Category ${saved.name} added.`);
      setCategoryName('');
      load();
    } catch (err) {
      setCategoryError(err);
    }
  }

  const fields = itemError?.fieldErrors || {};
  const update = (field) => (event) => setItemForm((f) => ({ ...f, [field]: event.target.value }));

  return (
    <>
      <h1>Menu</h1>
      {message && <p className="notice">{message}</p>}

      <form className="card" onSubmit={saveItem} noValidate>
        <h2>{itemForm.id ? `Edit "${itemForm.name}"` : 'Add a menu item'}</h2>
        <div className="row">
          <FormField label="Name" error={fields.name}>
            <input value={itemForm.name} onChange={update('name')} maxLength={100} />
          </FormField>
          <FormField label="Price (VND)" error={fields.price}>
            <input type="number" min="1" value={itemForm.price} onChange={update('price')} />
          </FormField>
          <FormField label="Category" error={fields.categoryId}>
            <select value={itemForm.categoryId} onChange={update('categoryId')}>
              <option value="">Choose…</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </FormField>
          <FormField label="Course" error={fields.course}>
            <select value={itemForm.course} onChange={update('course')}>
              {Object.entries(COURSE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </FormField>
        </div>
        <div className="row">
          <FormField label="Description (optional)" error={fields.description}>
            <input value={itemForm.description} onChange={update('description')} maxLength={300} />
          </FormField>
          <button type="submit" className="primary">{itemForm.id ? 'Save' : 'Add item'}</button>
          {itemForm.id && <button type="button" onClick={() => setItemForm(EMPTY_ITEM)}>Cancel</button>}
        </div>
        <FormError error={itemError && !Object.keys(fields).length ? itemError : null} />
      </form>

      <table className="data">
        <thead>
          <tr>
            <th>Name</th><th>Category</th><th>Course</th><th>Price</th><th>Stock</th><th>On menu</th><th />
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id} className={item.active ? '' : 'dimmed'}>
              <td>{item.name}{item.description && <div className="muted">{item.description}</div>}</td>
              <td>{item.categoryName}</td>
              <td>{COURSE_LABELS[item.course]}</td>
              <td>{formatPrice(item.price)}</td>
              <td>{item.inStock ? 'In stock' : 'Out of stock'}</td>
              <td>{item.active ? 'Yes' : 'No'}</td>
              <td className="actions">
                <button type="button" onClick={() => setItemForm({
                  id: item.id, name: item.name, description: item.description || '',
                  price: String(item.price), course: item.course, categoryId: String(item.categoryId),
                })}>Edit</button>
                <button type="button" onClick={() => (item.inStock
                  ? runAction(item, 'out-of-stock', 'marked out of stock')
                  : runAction(item, 'restock', 'is back in stock'))}>
                  {item.inStock ? 'Out of stock' : 'Restock'}
                </button>
                <button type="button" className={item.active ? 'danger' : ''}
                        onClick={() => (item.active
                          ? runAction(item, 'deactivate', 'removed from the menu')
                          : runAction(item, 'activate', 'added back to the menu'))}>
                  {item.active ? 'Remove' : 'Add back'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <form className="card" onSubmit={addCategory} noValidate>
        <h2>Categories</h2>
        <p className="muted">{categories.map((c) => c.name).join(', ')}</p>
        <div className="row">
          <FormField label="New category" error={categoryError?.fieldErrors?.name}>
            <input value={categoryName} onChange={(e) => setCategoryName(e.target.value)} maxLength={50} />
          </FormField>
          <button type="submit">Add category</button>
        </div>
      </form>
    </>
  );
}
