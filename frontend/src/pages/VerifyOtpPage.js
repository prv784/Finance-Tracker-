import React, { useState, useRef, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authAPI } from '../api';
import toast from 'react-hot-toast';

export default function VerifyOtpPage() {
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [countdown, setCountdown] = useState(60);
  const inputRefs = useRef([]);
  const { verifyOtp } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const email = location.state?.email || '';

  useEffect(() => {
    if (!email) navigate('/register');
    inputRefs.current[0]?.focus();
  }, [email, navigate]);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(c => c - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  const handleChange = (i, val) => {
    if (!/^\d*$/.test(val)) return;
    const newOtp = [...otp];
    newOtp[i] = val.slice(-1);
    setOtp(newOtp);
    if (val && i < 5) inputRefs.current[i + 1]?.focus();
    if (newOtp.every(d => d) && newOtp.join('').length === 6) {
      handleSubmit(newOtp.join(''));
    }
  };

  const handleKeyDown = (i, e) => {
    if (e.key === 'Backspace' && !otp[i] && i > 0) inputRefs.current[i - 1]?.focus();
  };

  const handlePaste = (e) => {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted.length === 6) {
      setOtp(pasted.split(''));
      handleSubmit(pasted);
    }
  };

  const handleSubmit = async (code) => {
    const otpCode = code || otp.join('');
    if (otpCode.length !== 6) return;
    setLoading(true);
    try {
      await verifyOtp(email, otpCode);
      navigate('/dashboard');
    } catch {}
    finally { setLoading(false); }
  };

  const handleResend = async () => {
    setResending(true);
    try {
      await authAPI.resendOtp(email);
      toast.success('OTP resent to your email');
      setCountdown(60);
      setOtp(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } catch {}
    finally { setResending(false); }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-500/10 to-secondary-500/10 p-6">
      <div className="card w-full max-w-md text-center">
        <div className="w-16 h-16 gradient-primary rounded-2xl flex items-center justify-center text-3xl mx-auto mb-6 shadow-lg">📧</div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Check your email</h2>
        <p className="text-gray-500 mb-2">We sent a 6-digit code to</p>
        <p className="font-semibold text-primary-600 mb-8">{email}</p>

        <div className="flex gap-3 justify-center mb-8" onPaste={handlePaste}>
          {otp.map((digit, i) => (
            <input key={i} ref={el => inputRefs.current[i] = el}
              type="text" inputMode="numeric" maxLength={1}
              value={digit} onChange={e => handleChange(i, e.target.value)}
              onKeyDown={e => handleKeyDown(i, e)}
              className={`w-12 h-14 text-center text-xl font-bold border-2 rounded-xl outline-none transition-all duration-200
                ${digit ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 bg-white text-gray-900'}
                focus:border-primary-500 focus:ring-2 focus:ring-primary-200`} />
          ))}
        </div>

        <button onClick={() => handleSubmit()} className="btn-primary w-full mb-4" disabled={loading || otp.join('').length < 6}>
          {loading ? <span className="flex items-center justify-center gap-2">
            <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>Verifying...
          </span> : 'Verify Account'}
        </button>

        <div className="text-sm text-gray-500">
          {countdown > 0 ? (
            <span>Resend code in <span className="font-semibold text-primary-600">{countdown}s</span></span>
          ) : (
            <button onClick={handleResend} disabled={resending} className="text-primary-600 font-semibold hover:text-primary-700 disabled:opacity-50">
              {resending ? 'Resending...' : 'Resend Code'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
