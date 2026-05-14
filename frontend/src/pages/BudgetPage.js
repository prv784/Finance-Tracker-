import React, { useState, useEffect, useCallback } from 'react';
import { budgetAPI, categoryAPI } from '../api';
import Modal from '../components/common/Modal';
import toast from 'react-hot-toast';

const fmt = (n) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0 }).format(n || 0);
const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];

function BudgetForm({ initial, categories, onSave, onClose }) {
  const now = new Date();
  const [form, setForm] = useState({
    name: '', amount: '', month: now.getMonth() + 1, year: now.getFullYear(),
    categoryId: '', alertThreshold: 80,
    ...initial,
    amount: initial?.amount || ''
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onSave({ ...form, amount: parseFloat(form.amount), categoryId: form.categoryId ? +form.categoryId : null, alertThreshold: +form.alertThreshold });
      onClose();
    } catch {}
    finally { setLoading(false); }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="label">Budget Name *</label>
        <input type="text" className="input-field" placeholder="e.g. Monthly Groceries"
          value={form.name} onChange={e => setForm({...form, name: e.target.value})} required />
      </div>
      <div>
        <label className="label">Budget Amount (USD) *</label>
        <input type="number" step="0.01" min="0.01" className="input-field" placeholder="0.00"
          value={form.amount} onChange={e => setForm({...form, amount: e.target.value})} required />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Month *</label>
          <select className="input-field" value={form.month} onChange={e => setForm({...form, month: +e.target.value})}>
            {MONTHS.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Year *</label>
          <select className="input-field" value={form.year} onChange={e => setForm({...form, year: +e.target.value})}>
            {[2023, 2024, 2025, 2026].map(y => <option key={y} value={y}>{y}</option>)}
          </select>
        </div>
      </div>
      <div>
        <label className="label">Category (optional)</label>
        <select className="input-field" value={form.categoryId} onChange={e => setForm({...form, categoryId: e.target.value})}>
          <option value="">All Expenses (Overall Budget)</option>
          {categories.filter(c => c.type !== 'INCOME').map(c => (
            <option key={c.id} value={c.id}>{c.icon} {c.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="label">Alert Threshold: <span className="text-primary-600 font-semibold">{form.alertThreshold}%</span></label>
        <input type="range" min="50" max="100" step="5" className="w-full accent-primary-600"
          value={form.alertThreshold} onChange={e => setForm({...form, alertThreshold: +e.target.value})} />
        <div className="flex justify-between text-xs text-gray-400 mt-1">
          <span>50%</span><span>Send alert at {form.alertThreshold}% usage</span><span>100%</span>
        </div>
      </div>
      <div className="flex gap-3 pt-2">
        <button type="button" onClick={onClose} className="btn-secondary flex-1">Cancel</button>
        <button type="submit" className="btn-primary flex-1" disabled={loading}>
          {loading ? 'Saving...' : initial?.id ? 'Update Budget' : 'Create Budget'}
        </button>
      </div>
    </form>
  );
}

function BudgetCard({ budget, onEdit, onDelete }) {
  const pct = Math.min(budget.percentageUsed, 100);
  const barColor = pct >= 90 ? '#ef4444' : pct >= 75 ? '#f59e0b' : '#10b981';
  const status = pct >= 100 ? 'exceeded' : pct >= budget.alertThreshold ? 'warning' : 'ok';
  const statusBadge = { exceeded: 'badge-red', warning: 'badge-yellow', ok: 'badge-green' };
  const statusLabel = { exceeded: '🚨 Exceeded', warning: '⚠️ Alert', ok: '✅ On Track' };

  return (
    <div className="card-hover">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl gradient-primary flex items-center justify-center text-lg">
            {budget.category?.icon || '🎯'}
          </div>
          <div>
            <h3 className="font-semibold text-gray-900">{budget.name}</h3>
            <p className="text-xs text-gray-400">{MONTHS[budget.month - 1]} {budget.year}</p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <span className={`badge ${statusBadge[status]}`}>{statusLabel[status]}</span>
          <button onClick={() => onEdit(budget)} className="btn-icon text-blue-500 hover:bg-blue-50 ml-1">✏️</button>
          <button onClick={() => onDelete(budget.id)} className="btn-icon text-red-500 hover:bg-red-50">🗑️</button>
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex justify-between text-sm">
          <span className="text-gray-500">Spent</span>
          <span className="font-semibold text-gray-800">{fmt(budget.spent)}</span>
        </div>
        <div className="progress-bar">
          <div className="progress-fill transition-all duration-700" style={{ width: `${pct}%`, background: barColor }}></div>
        </div>
        <div className="flex justify-between text-sm">
          <span style={{ color: barColor }} className="font-medium">{pct.toFixed(1)}% used</span>
          <span className="text-gray-500">Budget: {fmt(budget.amount)}</span>
        </div>
        <div className="flex justify-between text-sm pt-1 border-t border-gray-50">
          <span className="text-gray-500">Remaining</span>
          <span className={`font-semibold ${budget.remaining < 0 ? 'text-red-500' : 'text-green-500'}`}>
            {fmt(Math.abs(budget.remaining))} {budget.remaining < 0 ? 'over' : 'left'}
          </span>
        </div>
        {budget.category && (
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Category</span>
            <span className="badge badge-blue">{budget.category.icon} {budget.category.name}</span>
          </div>
        )}
      </div>
    </div>
  );
}

export default function BudgetPage() {
  const [budgets, setBudgets] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [deleteId, setDeleteId] = useState(null);
  const now = new Date();
  const [filterMonth, setFilterMonth] = useState(now.getMonth() + 1);
  const [filterYear, setFilterYear] = useState(now.getFullYear());

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [bRes, cRes] = await Promise.all([
        budgetAPI.getAll({ month: filterMonth, year: filterYear }),
        categoryAPI.getAll()
      ]);
      setBudgets(bRes.data.data || []);
      setCategories(cRes.data.data || []);
    } catch {}
    finally { setLoading(false); }
  }, [filterMonth, filterYear]);

  useEffect(() => { load(); }, [load]);

  const handleSave = async (payload) => {
    if (editItem?.id) { await budgetAPI.update(editItem.id, payload); toast.success('Budget updated'); }
    else { await budgetAPI.create(payload); toast.success('Budget created'); }
    load();
  };

  const handleDelete = async () => {
    await budgetAPI.delete(deleteId);
    toast.success('Budget deleted');
    setDeleteId(null);
    load();
  };

  const totalBudget = budgets.reduce((s, b) => s + parseFloat(b.amount || 0), 0);
  const totalSpent = budgets.reduce((s, b) => s + parseFloat(b.spent || 0), 0);
  const overBudget = budgets.filter(b => b.percentageUsed >= 100).length;

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="page-title">Budgets 🎯</h1>
          <p className="text-gray-500 mt-1">{budgets.length} budgets · {overBudget > 0 && <span className="text-red-500 font-medium">{overBudget} exceeded</span>}</p>
        </div>
        <button onClick={() => { setEditItem(null); setModalOpen(true); }} className="btn-primary flex items-center gap-2">
          <span>+</span> Create Budget
        </button>
      </div>

      {/* Summary */}
      {budgets.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {[
            { label: 'Total Budget', value: fmt(totalBudget), icon: '🎯', color: 'text-primary-600' },
            { label: 'Total Spent', value: fmt(totalSpent), icon: '💸', color: 'text-red-500' },
            { label: 'Remaining', value: fmt(totalBudget - totalSpent), icon: '💰', color: 'text-green-500' },
          ].map(s => (
            <div key={s.label} className="card text-center">
              <div className="text-3xl mb-1">{s.icon}</div>
              <p className="text-sm text-gray-500">{s.label}</p>
              <p className={`text-xl font-bold mt-1 ${s.color}`}>{s.value}</p>
            </div>
          ))}
        </div>
      )}

      {/* Month/Year filter */}
      <div className="card">
        <div className="flex flex-wrap items-center gap-4">
          <div>
            <label className="label">Month</label>
            <select className="input-field py-2 w-36" value={filterMonth} onChange={e => setFilterMonth(+e.target.value)}>
              {MONTHS.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Year</label>
            <select className="input-field py-2 w-28" value={filterYear} onChange={e => setFilterYear(+e.target.value)}>
              {[2023, 2024, 2025, 2026].map(y => <option key={y} value={y}>{y}</option>)}
            </select>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-48">
          <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
        </div>
      ) : budgets.length === 0 ? (
        <div className="card flex flex-col items-center justify-center h-48 text-gray-400">
          <span className="text-4xl mb-3">🎯</span>
          <p className="font-medium">No budgets for this period</p>
          <p className="text-sm mt-1">Create a budget to start tracking spending</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {budgets.map(budget => (
            <BudgetCard key={budget.id} budget={budget}
              onEdit={(b) => { setEditItem(b); setModalOpen(true); }}
              onDelete={setDeleteId} />
          ))}
        </div>
      )}

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editItem ? 'Edit Budget' : 'Create Budget'}>
        <BudgetForm initial={editItem} categories={categories} onSave={handleSave} onClose={() => setModalOpen(false)} />
      </Modal>

      <Modal isOpen={!!deleteId} onClose={() => setDeleteId(null)} title="Confirm Delete" size="sm">
        <div className="text-center">
          <div className="text-5xl mb-4">⚠️</div>
          <p className="text-gray-700 mb-6">Are you sure you want to delete this budget?</p>
          <div className="flex gap-3">
            <button onClick={() => setDeleteId(null)} className="btn-secondary flex-1">Cancel</button>
            <button onClick={handleDelete} className="btn-danger flex-1">Delete</button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
