import React, { useState, useEffect, useCallback } from 'react';
import { dashboardAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import { format } from 'date-fns';

const fmt = (n) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0 }).format(n || 0);
const pct = (n) => `${(n || 0).toFixed(1)}%`;

const STAT_CARDS = (data) => [
  { label: 'Total Income', value: fmt(data?.totalIncome), icon: '💰', gradient: 'gradient-income' },
  { label: 'Total Expenses', value: fmt(data?.totalExpenses), icon: '💸', gradient: 'gradient-expense' },
  { label: 'Net Savings', value: fmt(data?.totalSavings), icon: '🏦', gradient: 'gradient-savings' },
  { label: 'Savings Rate', value: pct(data?.savingsRate), icon: '📈', gradient: 'gradient-ai', change: 'This month' },
];

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-white rounded-xl shadow-card-hover border border-gray-100 p-3 text-sm">
      <p className="font-semibold text-gray-800 mb-2">{label}</p>
      {payload.map((p, i) => (
        <p key={i} style={{ color: p.color }} className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full inline-block" style={{ background: p.color }}></span>
          {p.name}: {fmt(p.value)}
        </p>
      ))}
    </div>
  );
};

const CustomPieLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
  if (percent < 0.05) return null;
  const RADIAN = Math.PI / 180;
  const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
  const x = cx + radius * Math.cos(-midAngle * RADIAN);
  const y = cy + radius * Math.sin(-midAngle * RADIAN);
  return (
    <text x={x} y={y} fill="white" textAnchor="middle" dominantBaseline="central" fontSize={11} fontWeight="600">
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
};

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await dashboardAPI.get({ month: selectedMonth, year: selectedYear });
      setData(res.data.data);
    } catch {}
    finally { setLoading(false); }
  }, [selectedMonth, selectedYear]);

  useEffect(() => { load(); }, [load]);

  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const years = [2022, 2023, 2024, 2025];

  if (loading) return (
    <div className="flex items-center justify-center h-96">
      <div className="text-center">
        <div className="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
        <p className="text-gray-500">Loading dashboard...</p>
      </div>
    </div>
  );

  const COLORS = ['#667eea','#764ba2','#f5576c','#f093fb','#4facfe','#43e97b','#fa709a','#fee140','#30cfd0','#667eea'];

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="page-title">Dashboard 👋</h1>
          <p className="text-gray-500 mt-1">Hello {user?.firstName}, here's your financial overview</p>
        </div>
        <div className="flex items-center gap-3">
          <select value={selectedMonth} onChange={e => setSelectedMonth(+e.target.value)}
            className="input-field py-2 w-32">
            {months.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
          </select>
          <select value={selectedYear} onChange={e => setSelectedYear(+e.target.value)}
            className="input-field py-2 w-28">
            {years.map(y => <option key={y} value={y}>{y}</option>)}
          </select>
        </div>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {STAT_CARDS(data).map((card) => (
          <div key={card.label} className="card-hover group">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-gray-500 font-medium">{card.label}</p>
                <p className="text-2xl font-bold text-gray-900 mt-1">{card.value}</p>
                <span className="badge badge-green mt-2">{card.change}</span>
              </div>
              <div className={`w-12 h-12 rounded-2xl ${card.gradient} flex items-center justify-center text-xl shadow-md group-hover:scale-110 transition-transform`}>
                {card.icon}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Monthly Trend - Area Chart */}
        <div className="card xl:col-span-2">
          <h3 className="section-title">Monthly Financial Trend</h3>
          <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={data?.monthlyData || []} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
              <defs>
                <linearGradient id="incomeGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.2}/>
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="expenseGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ef4444" stopOpacity={0.2}/>
                  <stop offset="95%" stopColor="#ef4444" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="savingsGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#667eea" stopOpacity={0.2}/>
                  <stop offset="95%" stopColor="#667eea" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="month" tick={{ fill: '#9ca3af', fontSize: 12 }} />
              <YAxis tick={{ fill: '#9ca3af', fontSize: 12 }} tickFormatter={v => `$${(v/1000).toFixed(0)}k`} />
              <Tooltip content={<CustomTooltip />} />
              <Legend />
              <Area type="monotone" dataKey="income" stroke="#10b981" strokeWidth={2} fill="url(#incomeGrad)" name="Income" />
              <Area type="monotone" dataKey="expenses" stroke="#ef4444" strokeWidth={2} fill="url(#expenseGrad)" name="Expenses" />
              <Area type="monotone" dataKey="savings" stroke="#667eea" strokeWidth={2} fill="url(#savingsGrad)" name="Savings" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Category Pie Chart */}
        <div className="card">
          <h3 className="section-title">Expenses by Category</h3>
          {data?.expenseByCategory?.length > 0 ? (
            <>
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie data={data.expenseByCategory} cx="50%" cy="50%"
                    innerRadius={50} outerRadius={85}
                    dataKey="amount" nameKey="category"
                    labelLine={false} label={<CustomPieLabel />}>
                    {data.expenseByCategory.map((entry, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v) => fmt(v)} />
                </PieChart>
              </ResponsiveContainer>
              <div className="space-y-2 mt-2 max-h-36 overflow-y-auto scrollbar-hide">
                {data.expenseByCategory.slice(0, 6).map((cat, i) => (
                  <div key={i} className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <div className="w-3 h-3 rounded-full flex-shrink-0" style={{ background: COLORS[i % COLORS.length] }}></div>
                      <span className="text-gray-600 truncate max-w-[120px]">{cat.category}</span>
                    </div>
                    <div className="text-right">
                      <span className="font-semibold text-gray-800">{fmt(cat.amount)}</span>
                      <span className="text-gray-400 ml-1 text-xs">{cat.percentage?.toFixed(0)}%</span>
                    </div>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="flex flex-col items-center justify-center h-48 text-gray-400">
              <span className="text-4xl mb-2">📊</span>
              <p className="text-sm">No expense data yet</p>
            </div>
          )}
        </div>
      </div>

      {/* Charts Row 2 */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        {/* Monthly Bar Chart */}
        <div className="card">
          <h3 className="section-title">Income vs Expenses (Bar)</h3>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={data?.monthlyData || []} margin={{ top: 5, right: 10, left: 0, bottom: 5 }} barGap={2}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="month" tick={{ fill: '#9ca3af', fontSize: 12 }} />
              <YAxis tick={{ fill: '#9ca3af', fontSize: 12 }} tickFormatter={v => `$${(v/1000).toFixed(0)}k`} />
              <Tooltip content={<CustomTooltip />} />
              <Legend />
              <Bar dataKey="income" fill="#10b981" name="Income" radius={[4, 4, 0, 0]} />
              <Bar dataKey="expenses" fill="#ef4444" name="Expenses" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Budget Overview */}
        <div className="card">
          <h3 className="section-title">Budget Status</h3>
          {data?.activeBudgets?.length > 0 ? (
            <div className="space-y-4 max-h-64 overflow-y-auto scrollbar-hide">
              {data.activeBudgets.map(budget => {
                const pctUsed = Math.min(budget.percentageUsed, 100);
                const color = pctUsed >= 90 ? '#ef4444' : pctUsed >= 75 ? '#f59e0b' : '#10b981';
                return (
                  <div key={budget.id}>
                    <div className="flex justify-between text-sm mb-1.5">
                      <span className="font-medium text-gray-700">{budget.category?.icon} {budget.name}</span>
                      <span className="text-gray-500">{fmt(budget.spent)} / {fmt(budget.amount)}</span>
                    </div>
                    <div className="progress-bar">
                      <div className="progress-fill" style={{ width: `${pctUsed}%`, background: color }}></div>
                    </div>
                    <div className="flex justify-between text-xs mt-1">
                      <span style={{ color }}>{pctUsed.toFixed(1)}% used</span>
                      <span className="text-gray-400">{fmt(budget.remaining)} left</span>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-48 text-gray-400">
              <span className="text-4xl mb-2">🎯</span>
              <p className="text-sm">No budgets set for this month</p>
            </div>
          )}
        </div>
      </div>

      {/* Recent Transactions */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <div className="card">
          <h3 className="section-title">Recent Expenses</h3>
          {data?.recentExpenses?.length > 0 ? (
            <div className="space-y-3">
              {data.recentExpenses.slice(0, 6).map(exp => (
                <div key={exp.id} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl gradient-expense flex items-center justify-center text-sm">
                      {exp.category?.icon || '💸'}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-800">{exp.title}</p>
                      <p className="text-xs text-gray-400">{exp.category?.name} • {exp.date}</p>
                    </div>
                  </div>
                  <span className="font-semibold text-red-500">-{fmt(exp.amount)}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-36 text-gray-400">
              <span className="text-3xl mb-2">💸</span><p className="text-sm">No expenses yet</p>
            </div>
          )}
        </div>

        <div className="card">
          <h3 className="section-title">Recent Income</h3>
          {data?.recentIncomes?.length > 0 ? (
            <div className="space-y-3">
              {data.recentIncomes.slice(0, 6).map(inc => (
                <div key={inc.id} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl gradient-income flex items-center justify-center text-sm">💰</div>
                    <div>
                      <p className="text-sm font-medium text-gray-800">{inc.title}</p>
                      <p className="text-xs text-gray-400">{inc.source} • {inc.date}</p>
                    </div>
                  </div>
                  <span className="font-semibold text-green-500">+{fmt(inc.amount)}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-36 text-gray-400">
              <span className="text-3xl mb-2">💰</span><p className="text-sm">No income yet</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
