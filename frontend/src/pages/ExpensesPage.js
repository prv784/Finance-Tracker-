import React, { useState, useEffect, useCallback } from 'react';
import { expenseAPI, categoryAPI, aiAPI } from '../api';
import Modal from '../components/common/Modal';
import toast from 'react-hot-toast';

const fmt = (n) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n || 0);

const PAYMENT_METHODS = ['Cash', 'Credit Card', 'Debit Card', 'UPI', 'Bank Transfer', 'Other'];

function ExpenseForm({ initial, categories, onSave, onClose }) {
  const [form, setForm] = useState({
    title: '', description: '', amount: '', date: new Date().toISOString().split('T')[0],
    categoryId: '', paymentMethod: 'Cash', notes: '', isRecurring: false, recurrenceType: '',
    ...initial
  });
  const [loading, setLoading] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);

  const handleAiCategorize = async () => {
    if (!form.title) return;
    setAiLoading(true);
    try {
      const res = await aiAPI.categorize({ title: form.title, description: form.description });
      const suggestedName = res.data.data;
      const match = categories.find(c => c.name.toLowerCase() === suggestedName.toLowerCase());
      if (match) {
        setForm(f => ({ ...f, categoryId: match.id.toString() }));
        toast.success(`AI suggested: ${match.name}`);
      } else {
        toast(`AI suggests: ${suggestedName}`, { icon: '🤖' });
      }
    } catch {}
    finally { setAiLoading(false); }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload = { ...form, amount: parseFloat(form.amount), categoryId: form.categoryId ? +form.categoryId : null };
      await onSave(payload);
      onClose();
    } catch {}
    finally { setLoading(false); }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="label">Title *</label>
        <div className="flex gap-2">
          <input type="text" className="input-field" placeholder="e.g. Lunch at cafe"
            value={form.title} onChange={e => setForm({...form, title: e.target.value})} required />
          <button type="button" onClick={handleAiCategorize} disabled={aiLoading || !form.title}
            title="AI Auto-Categorize"
            className="px-3 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl hover:opacity-90 disabled:opacity-50 transition-all text-sm whitespace-nowrap">
            {aiLoading ? '⏳' : '🤖'}
          </button>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Amount (USD) *</label>
          <input type="number" step="0.01" min="0.01" className="input-field" placeholder="0.00"
            value={form.amount} onChange={e => setForm({...form, amount: e.target.value})} required />
        </div>
        <div>
          <label className="label">Date *</label>
          <input type="date" className="input-field"
            value={form.date} onChange={e => setForm({...form, date: e.target.value})} required />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Category</label>
          <select className="input-field" value={form.categoryId} onChange={e => setForm({...form, categoryId: e.target.value})}>
            <option value="">Select category</option>
            {categories.filter(c => c.type !== 'INCOME').map(c => (
              <option key={c.id} value={c.id}>{c.icon} {c.name}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="label">Payment Method</label>
          <select className="input-field" value={form.paymentMethod} onChange={e => setForm({...form, paymentMethod: e.target.value})}>
            {PAYMENT_METHODS.map(m => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>
      </div>
      <div>
        <label className="label">Description</label>
        <textarea className="input-field resize-none" rows={2} placeholder="Optional notes..."
          value={form.description} onChange={e => setForm({...form, description: e.target.value})} />
      </div>
      <div className="flex items-center gap-3">
        <input type="checkbox" id="recurring" checked={form.isRecurring}
          onChange={e => setForm({...form, isRecurring: e.target.checked})}
          className="w-4 h-4 text-primary-600 rounded" />
        <label htmlFor="recurring" className="text-sm text-gray-700">Recurring expense</label>
        {form.isRecurring && (
          <select className="input-field py-1.5 ml-2" value={form.recurrenceType}
            onChange={e => setForm({...form, recurrenceType: e.target.value})}>
            <option value="">Frequency</option>
            {['DAILY','WEEKLY','MONTHLY','YEARLY'].map(r => <option key={r} value={r}>{r}</option>)}
          </select>
        )}
      </div>
      <div className="flex gap-3 pt-2">
        <button type="button" onClick={onClose} className="btn-secondary flex-1">Cancel</button>
        <button type="submit" className="btn-primary flex-1" disabled={loading}>
          {loading ? 'Saving...' : initial?.id ? 'Update Expense' : 'Add Expense'}
        </button>
      </div>
    </form>
  );
}

export default function ExpensesPage() {
  const [expenses, setExpenses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [deleteId, setDeleteId] = useState(null);
  const [filters, setFilters] = useState({ startDate: '', endDate: '', categoryId: '' });
  const [search, setSearch] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = {};
      if (filters.startDate) params.startDate = filters.startDate;
      if (filters.endDate) params.endDate = filters.endDate;
      if (filters.categoryId) params.categoryId = filters.categoryId;
      const [eRes, cRes] = await Promise.all([expenseAPI.getAll(params), categoryAPI.getAll()]);
      setExpenses(eRes.data.data || []);
      setCategories(cRes.data.data || []);
    } catch {}
    finally { setLoading(false); }
  }, [filters]);

  useEffect(() => { load(); }, [load]);

  const handleSave = async (payload) => {
    if (editItem?.id) {
      await expenseAPI.update(editItem.id, payload);
      toast.success('Expense updated');
    } else {
      await expenseAPI.create(payload);
      toast.success('Expense added');
    }
    load();
  };

  const handleDelete = async () => {
    await expenseAPI.delete(deleteId);
    toast.success('Expense deleted');
    setDeleteId(null);
    load();
  };

  const openAdd = () => { setEditItem(null); setModalOpen(true); };
  const openEdit = (item) => { setEditItem(item); setModalOpen(true); };

  const filtered = expenses.filter(e =>
    e.title.toLowerCase().includes(search.toLowerCase()) ||
    e.category?.name?.toLowerCase().includes(search.toLowerCase())
  );

  const totalFiltered = filtered.reduce((s, e) => s + parseFloat(e.amount || 0), 0);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="page-title">Expenses 💸</h1>
          <p className="text-gray-500 mt-1">{filtered.length} transactions · Total: <span className="font-semibold text-red-500">{fmt(totalFiltered)}</span></p>
        </div>
        <button onClick={openAdd} className="btn-primary flex items-center gap-2">
          <span>+</span> Add Expense
        </button>
      </div>

      {/* Filters */}
      <div className="card">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label className="label">Search</label>
            <input type="text" className="input-field" placeholder="Search expenses..."
              value={search} onChange={e => setSearch(e.target.value)} />
          </div>
          <div>
            <label className="label">From Date</label>
            <input type="date" className="input-field"
              value={filters.startDate} onChange={e => setFilters({...filters, startDate: e.target.value})} />
          </div>
          <div>
            <label className="label">To Date</label>
            <input type="date" className="input-field"
              value={filters.endDate} onChange={e => setFilters({...filters, endDate: e.target.value})} />
          </div>
          <div>
            <label className="label">Category</label>
            <select className="input-field" value={filters.categoryId}
              onChange={e => setFilters({...filters, categoryId: e.target.value})}>
              <option value="">All Categories</option>
              {categories.filter(c => c.type !== 'INCOME').map(c => (
                <option key={c.id} value={c.id}>{c.icon} {c.name}</option>
              ))}
            </select>
          </div>
        </div>
        <button onClick={() => setFilters({ startDate: '', endDate: '', categoryId: '' })}
          className="text-sm text-primary-600 hover:text-primary-700 mt-3 font-medium">
          Clear Filters
        </button>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 text-gray-400">
            <span className="text-4xl mb-3">💸</span>
            <p className="font-medium">No expenses found</p>
            <p className="text-sm mt-1">Add your first expense to get started</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-100">
                <tr>
                  <th className="table-header text-left">Title</th>
                  <th className="table-header text-left hidden sm:table-cell">Category</th>
                  <th className="table-header text-left hidden md:table-cell">Date</th>
                  <th className="table-header text-left hidden lg:table-cell">Payment</th>
                  <th className="table-header text-right">Amount</th>
                  <th className="table-header text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(exp => (
                  <tr key={exp.id} className="table-row">
                    <td className="table-cell">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg gradient-expense flex items-center justify-center text-xs">
                          {exp.category?.icon || '💸'}
                        </div>
                        <div>
                          <p className="font-medium text-gray-800">{exp.title}</p>
                          {exp.isRecurring && <span className="badge badge-blue">🔄 {exp.recurrenceType}</span>}
                        </div>
                      </div>
                    </td>
                    <td className="table-cell hidden sm:table-cell">
                      <span className="badge badge-yellow">{exp.category?.name || 'Uncategorized'}</span>
                    </td>
                    <td className="table-cell hidden md:table-cell text-gray-500">{exp.date}</td>
                    <td className="table-cell hidden lg:table-cell text-gray-500">{exp.paymentMethod}</td>
                    <td className="table-cell text-right font-semibold text-red-500">{fmt(exp.amount)}</td>
                    <td className="table-cell text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit({ ...exp, categoryId: exp.category?.id, date: exp.date })}
                          className="btn-icon text-blue-500 hover:bg-blue-50">✏️</button>
                        <button onClick={() => setDeleteId(exp.id)}
                          className="btn-icon text-red-500 hover:bg-red-50">🗑️</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add/Edit Modal */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)}
        title={editItem ? 'Edit Expense' : 'Add New Expense'}>
        <ExpenseForm initial={editItem} categories={categories}
          onSave={handleSave} onClose={() => setModalOpen(false)} />
      </Modal>

      {/* Delete Confirm Modal */}
      <Modal isOpen={!!deleteId} onClose={() => setDeleteId(null)} title="Confirm Delete" size="sm">
        <div className="text-center">
          <div className="text-5xl mb-4">⚠️</div>
          <p className="text-gray-700 mb-6">Are you sure you want to delete this expense? This action cannot be undone.</p>
          <div className="flex gap-3">
            <button onClick={() => setDeleteId(null)} className="btn-secondary flex-1">Cancel</button>
            <button onClick={handleDelete} className="btn-danger flex-1">Delete</button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
