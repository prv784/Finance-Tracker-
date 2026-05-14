import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authAPI } from '../api';
import toast from 'react-hot-toast';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await authAPI.forgotPassword(email);
      setSent(true);
      toast.success('Reset link sent to your email');
    } catch {}
    finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-500/10 to-secondary-500/10 p-6">
      <div className="card w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-16 h-16 gradient-primary rounded-2xl flex items-center justify-center text-3xl mx-auto mb-4 shadow-lg">🔒</div>
          <h2 className="text-2xl font-bold text-gray-900">Forgot Password?</h2>
          <p className="text-gray-500 mt-2">Enter your email and we'll send you a reset link</p>
        </div>

        {sent ? (
          <div className="text-center">
            <div className="text-5xl mb-4">📬</div>
            <p className="text-gray-700 font-medium mb-2">Check your inbox!</p>
            <p className="text-gray-500 text-sm mb-6">We sent a password reset link to <strong>{email}</strong></p>
            <Link to="/login" className="btn-primary inline-block">Back to Login</Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="label">Email Address</label>
              <input type="email" className="input-field" placeholder="you@example.com"
                value={email} onChange={e => setEmail(e.target.value)} required />
            </div>
            <button type="submit" className="btn-primary w-full" disabled={loading}>
              {loading ? <span className="flex items-center justify-center gap-2">
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>Sending...
              </span> : 'Send Reset Link'}
            </button>
            <p className="text-center text-sm text-gray-500">
              Remember your password? <Link to="/login" className="text-primary-600 font-semibold">Sign in</Link>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}
