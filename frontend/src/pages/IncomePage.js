import React, { useState, useEffect, useCallback } from 'react';
import { incomeAPI } from '../api';
import Modal from '../components/common/Modal';
import toast from 'react-hot-toast';

const fmt = (n) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n || 0);
const SOURCES = ['SALARY','FREELANCE','BUSINESS','INVESTMENT','RENTAL','GIFT','BONUS','OTHER'];
const SOURCE_ICONS = { SALARY:'💼', FREELANCE:'💻', BUSINESS:'🏢', INVESTMENT:'📈', RENTAL:'🏠', GIFT:'🎁', BONUS:'🎉', OTHER:'💵' };

function IncomeForm({ initial, onSave, onClose }) {
  const [form, setForm] = useState({
    title: '', description: '', amount: '', date: new Date().toISOString().split('T')[0],
    source: 'SALARY', notes: '', isRecurring: false, recurrenceType: '',
    ...initial
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onSave({ ...form, amount: parseFloat(form.amount) });
      onClose();
    } catch {}
    finally { setLoading(false); }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="label">Title *</label>
        <input type="text" className="input-field" placeholder="e.g. Monthly Salary"
          value={form.title} onChange={e => setForm({...form, title: e.target.value})} required />
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
      <div>
        <label className="label">Source</label>
        <select className="input-field" value={form.source} onChange={e => setForm({...form, source: e.target.value})}>
          {SOURCES.map(s => <option key={s} value={s}>{SOURCE_ICONS[s]} {s}</option>)}
        </select>
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
        <label htmlFor="recurring" className="text-sm text-gray-700">Recurring income</label>
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
          {loading ? 'Saving...' : initial?.id ? 'Update Income' : 'Add Income'}
        </button>
      </div>
    </form>
  );
}

export default function IncomePage() {
  const [incomes, setIncomes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [deleteId, setDeleteId] = useState(null);
  const [filters, setFilters] = useState({ startDate: '', endDate: '' });
  const [search, setSearch] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = {};
      if (filters.startDate) params.startDate = filters.startDate;
      if (filters.endDate) params.endDate = filters.endDate;
      const res = await incomeAPI.getAll(params);
      setIncomes(res.data.data || []);
    } catch {}
    finally { setLoading(false); }
  }, [filters]);

  useEffect(() => { load(); }, [load]);

  const handleSave = async (payload) => {
    if (editItem?.id) {
      await incomeAPI.update(editItem.id, payload);
      toast.success('Income updated');
    } else {
      await incomeAPI.create(payload);
      toast.success('Income added');
    }
    load();
  };

  const handleDelete = async () => {
    await incomeAPI.delete(deleteId);
    toast.success('Income deleted');
    setDeleteId(null);
    load();
  };

  const filtered = incomes.filter(i => i.title.toLowerCase().includes(search.toLowerCase()) || i.source.toLowerCase().includes(search.toLowerCase()));
  const total = filtered.reduce((s, i) => s + parseFloat(i.amount || 0), 0);

  const sourceBreakdown = SOURCES.map(src => ({
    source: src,
    icon: SOURCE_ICONS[src],
    amount: filtered.filter(i => i.source === src).reduce((s, i) => s + parseFloat(i.amount || 0), 0),
    count: filtered.filter(i => i.source === src).length
  })).filter(s => s.count > 0);

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="page-title">Income 💰</h1>
          <p className="text-gray-500 mt-1">{filtered.length} entries · Total: <span className="font-semibold text-green-500">{fmt(total)}</span></p>
        </div>
        <button onClick={() => { setEditItem(null); setModalOpen(true); }} className="btn-primary flex items-center gap-2">
          <span>+</span> Add Income
        </button>
      </div>

      {/* Source Cards */}
      {sourceBreakdown.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {sourceBreakdown.map(src => (
            <div key={src.source} className="card-hover text-center p-4">
              <div className="text-3xl mb-2">{src.icon}</div>
              <p className="text-xs text-gray-500 uppercase tracking-wide font-medium">{src.source}</p>
              <p className="text-lg font-bold text-gray-900 mt-1">{fmt(src.amount)}</p>
              <p className="text-xs text-gray-400">{src.count} entry</p>
            </div>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="card">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="label">Search</label>
            <input type="text" className="input-field" placeholder="Search income..."
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
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 text-gray-400">
            <span className="text-4xl mb-3">💰</span>
            <p className="font-medium">No income records found</p>
            <p className="text-sm mt-1">Add your first income entry to get started</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-100">
                <tr>
                  <th className="table-header text-left">Title</th>
                  <th className="table-header text-left hidden sm:table-cell">Source</th>
                  <th className="table-header text-left hidden md:table-cell">Date</th>
                  <th className="table-header text-right">Amount</th>
                  <th className="table-header text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(inc => (
                  <tr key={inc.id} className="table-row">
                    <td className="table-cell">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg gradient-income flex items-center justify-center text-sm">
                          {SOURCE_ICONS[inc.source] || '💰'}
                        </div>
                        <div>
                          <p className="font-medium text-gray-800">{inc.title}</p>
                          {inc.isRecurring && <span className="badge badge-blue">🔄 {inc.recurrenceType}</span>}
                        </div>
                      </div>
                    </td>
                    <td className="table-cell hidden sm:table-cell">
                      <span className="badge badge-green">{inc.source}</span>
                    </td>
                    <td className="table-cell hidden md:table-cell text-gray-500">{inc.date}</td>
                    <td className="table-cell text-right font-semibold text-green-500">+{fmt(inc.amount)}</td>
                    <td className="table-cell text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => { setEditItem(inc); setModalOpen(true); }} className="btn-icon text-blue-500 hover:bg-blue-50">✏️</button>
                        <button onClick={() => setDeleteId(inc.id)} className="btn-icon text-red-500 hover:bg-red-50">🗑️</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editItem ? 'Edit Income' : 'Add Income'}>
        <IncomeForm initial={editItem} onSave={handleSave} onClose={() => setModalOpen(false)} />
      </Modal>

      <Modal isOpen={!!deleteId} onClose={() => setDeleteId(null)} title="Confirm Delete" size="sm">
        <div className="text-center">
          <div className="text-5xl mb-4">⚠️</div>
          <p className="text-gray-700 mb-6">Are you sure you want to delete this income record?</p>
          <div className="flex gap-3">
            <button onClick={() => setDeleteId(null)} className="btn-secondary flex-1">Cancel</button>
            <button onClick={handleDelete} className="btn-danger flex-1">Delete</button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
