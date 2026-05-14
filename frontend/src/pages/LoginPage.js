import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [showPass, setShowPass] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(form.email, form.password);
      navigate('/dashboard');
    } catch {}
    finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen flex">
      {/* Left panel */}
      <div className="hidden lg:flex lg:w-1/2 gradient-primary flex-col justify-center items-center p-12 relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="absolute rounded-full border border-white"
              style={{ width: `${(i + 1) * 120}px`, height: `${(i + 1) * 120}px`,
                top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }} />
          ))}
        </div>
        <div className="relative z-10 text-center text-white">
          <div className="text-7xl mb-6">💰</div>
          <h1 className="text-4xl font-bold mb-4">Finance AI</h1>
          <p className="text-xl text-white/80 mb-8">Smart money management powered by AI</p>
          <div className="grid grid-cols-2 gap-4 text-left">
            {['📊 Track Expenses', '🤖 AI Insights', '🎯 Set Budgets', '📈 Grow Savings'].map(item => (
              <div key={item} className="bg-white/10 rounded-xl p-4 backdrop-blur-sm">
                <p className="text-white font-medium">{item}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right panel - Form */}
      <div className="flex-1 flex items-center justify-center p-8 bg-gray-50">
        <div className="w-full max-w-md">
          <div className="lg:hidden text-center mb-8">
            <div className="w-14 h-14 gradient-primary rounded-2xl flex items-center justify-center text-2xl mx-auto mb-3 shadow-lg">💰</div>
            <h1 className="text-2xl font-bold text-gradient">Finance AI</h1>
          </div>

          <div className="card">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Welcome back</h2>
            <p className="text-gray-500 mb-8">Sign in to your account</p>

            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="label">Email</label>
                <input type="email" className="input-field" placeholder="you@example.com"
                  value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} required />
              </div>
              <div>
                <label className="label">Password</label>
                <div className="relative">
                  <input type={showPass ? 'text' : 'password'} className="input-field pr-12"
                    placeholder="••••••••" value={form.password}
                    onChange={e => setForm({ ...form, password: e.target.value })} required />
                  <button type="button" onClick={() => setShowPass(!showPass)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                    {showPass ? '🙈' : '👁️'}
                  </button>
                </div>
              </div>
              <div className="flex justify-end">
                <Link to="/forgot-password" className="text-sm text-primary-600 hover:text-primary-700 font-medium">
                  Forgot password?
                </Link>
              </div>
              <button type="submit" className="btn-primary w-full" disabled={loading}>
                {loading ? <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                  Signing in...
                </span> : 'Sign In'}
              </button>
            </form>

            <p className="text-center text-sm text-gray-500 mt-6">
              Don't have an account?{' '}
              <Link to="/register" className="text-primary-600 font-semibold hover:text-primary-700">Sign up</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
