import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);
  const [showPass, setShowPass] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirmPassword) {
      alert('Passwords do not match'); return;
    }
    setLoading(true);
    try {
      await register({ firstName: form.firstName, lastName: form.lastName, email: form.email, password: form.password });
      navigate('/verify-otp', { state: { email: form.email } });
    } catch {}
    finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen flex">
      <div className="hidden lg:flex lg:w-1/2 gradient-primary flex-col justify-center items-center p-12 relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="absolute rounded-full border border-white"
              style={{ width: `${(i+1)*120}px`, height: `${(i+1)*120}px`, top:'50%', left:'50%', transform:'translate(-50%,-50%)' }} />
          ))}
        </div>
        <div className="relative z-10 text-center text-white">
          <div className="text-7xl mb-6">🚀</div>
          <h1 className="text-4xl font-bold mb-4">Get Started Free</h1>
          <p className="text-xl text-white/80 mb-8">Join thousands managing finances smarter</p>
          <div className="space-y-3 text-left">
            {['✅ Free forever plan', '✅ AI-powered insights', '✅ Bank-level security', '✅ Real-time analytics'].map(item => (
              <p key={item} className="text-white/90 text-lg">{item}</p>
            ))}
          </div>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center p-8 bg-gray-50">
        <div className="w-full max-w-md">
          <div className="lg:hidden text-center mb-8">
            <div className="w-14 h-14 gradient-primary rounded-2xl flex items-center justify-center text-2xl mx-auto mb-3 shadow-lg">💰</div>
            <h1 className="text-2xl font-bold text-gradient">Finance AI</h1>
          </div>

          <div className="card">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Create account</h2>
            <p className="text-gray-500 mb-6">Start your financial journey today</p>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">First Name</label>
                  <input type="text" className="input-field" placeholder="John"
                    value={form.firstName} onChange={e => setForm({...form, firstName: e.target.value})} required />
                </div>
                <div>
                  <label className="label">Last Name</label>
                  <input type="text" className="input-field" placeholder="Doe"
                    value={form.lastName} onChange={e => setForm({...form, lastName: e.target.value})} required />
                </div>
              </div>
              <div>
                <label className="label">Email</label>
                <input type="email" className="input-field" placeholder="you@example.com"
                  value={form.email} onChange={e => setForm({...form, email: e.target.value})} required />
              </div>
              <div>
                <label className="label">Password</label>
                <div className="relative">
                  <input type={showPass ? 'text' : 'password'} className="input-field pr-12"
                    placeholder="Min 8 characters" value={form.password}
                    onChange={e => setForm({...form, password: e.target.value})} required minLength={8} />
                  <button type="button" onClick={() => setShowPass(!showPass)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                    {showPass ? '🙈' : '👁️'}
                  </button>
                </div>
              </div>
              <div>
                <label className="label">Confirm Password</label>
                <input type="password" className="input-field" placeholder="Repeat password"
                  value={form.confirmPassword} onChange={e => setForm({...form, confirmPassword: e.target.value})} required />
              </div>
              <button type="submit" className="btn-primary w-full mt-2" disabled={loading}>
                {loading ? <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>Creating account...
                </span> : 'Create Account'}
              </button>
            </form>

            <p className="text-center text-sm text-gray-500 mt-6">
              Already have an account?{' '}
              <Link to="/login" className="text-primary-600 font-semibold hover:text-primary-700">Sign in</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
