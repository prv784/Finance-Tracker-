import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Wallet,
  Mail,
  Lock,
  Eye,
  EyeOff,
  Loader2,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [loading, setLoading] = useState(false);
  const [showPass, setShowPass] = useState(false);
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState("");

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setForm((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (loading) return;

    setLoading(true);
    setError("");

    try {
      await login(form.email, form.password);
      navigate("/dashboard");
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          "Invalid email or password."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-gradient-to-br from-slate-50 via-white to-indigo-50">

      {/* Left Side */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-indigo-600 via-blue-600 to-cyan-500 text-white p-14 flex-col justify-center relative overflow-hidden">

        <div className="absolute inset-0 opacity-10">
          {[...Array(6)].map((_, i) => (
            <div
              key={i}
              className="absolute rounded-full border border-white"
              style={{
                width: `${150 + i * 100}px`,
                height: `${150 + i * 100}px`,
                left: "50%",
                top: "50%",
                transform: "translate(-50%,-50%)",
              }}
            />
          ))}
        </div>

        <div className="relative z-10">

          <div className="w-20 h-20 rounded-3xl bg-white/20 flex items-center justify-center mb-8 backdrop-blur-md">
            <Wallet size={42} />
          </div>

          <h1 className="text-5xl font-bold mb-5">
            Finance AI
          </h1>

          <p className="text-xl text-white/80 mb-10 max-w-md">
            Smarter budgeting, AI insights, expense tracking and
            financial planning — all in one place.
          </p>

          <div className="grid grid-cols-2 gap-5">

            {[
              "AI Expense Analysis",
              "Budget Planner",
              "Savings Prediction",
              "Monthly Reports",
            ].map((item) => (
              <div
                key={item}
                className="rounded-2xl bg-white/10 backdrop-blur-lg p-5"
              >
                ✓ {item}
              </div>
            ))}

          </div>
        </div>
      </div>

      {/* Right Side */}

      <div className="flex-1 flex items-center justify-center p-8">

        <div className="w-full max-w-md">

          <div className="rounded-3xl bg-white shadow-2xl border border-gray-100 p-8">

            <div className="text-center mb-8">

              <div className="w-16 h-16 mx-auto rounded-2xl bg-indigo-600 text-white flex items-center justify-center mb-4">
                <Wallet size={30} />
              </div>

              <h2 className="text-3xl font-bold">
                Welcome Back 👋
              </h2>

              <p className="text-gray-500 mt-2">
                Login to continue managing your finances.
              </p>

            </div>

            {error && (
              <div className="mb-5 rounded-lg bg-red-50 border border-red-200 p-3 text-red-600">
                {error}
              </div>
            )}

            <form
              onSubmit={handleSubmit}
              className="space-y-5"
            >
              {/* Email */}

              <div>

                <label className="label">
                  Email Address
                </label>

                <div className="relative">

                  <Mail
                    size={18}
                    className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400"
                  />

                  <input
                    name="email"
                    type="email"
                    autoComplete="email"
                    value={form.email}
                    onChange={handleChange}
                    placeholder="you@example.com"
                    className="input-field pl-11"
                    disabled={loading}
                    required
                  />

                </div>
              </div>

              {/* Password */}

              <div>

                <label className="label">
                  Password
                </label>

                <div className="relative">

                  <Lock
                    size={18}
                    className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400"
                  />

                  <input
                    name="password"
                    type={
                      showPass
                        ? "text"
                        : "password"
                    }
                    autoComplete="current-password"
                    value={form.password}
                    onChange={handleChange}
                    className="input-field pl-11 pr-12"
                    placeholder="••••••••"
                    disabled={loading}
                    required
                  />

                  <button
                    type="button"
                    onClick={() =>
                      setShowPass(!showPass)
                    }
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400"
                  >
                    {showPass ? (
                      <EyeOff size={18} />
                    ) : (
                      <Eye size={18} />
                    )}
                  </button>

                </div>
              </div>

              <div className="flex items-center justify-between">

                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={remember}
                    onChange={(e) =>
                      setRemember(e.target.checked)
                    }
                  />
                  Remember me
                </label>

                <Link
                  to="/forgot-password"
                  className="text-indigo-600 font-medium"
                >
                  Forgot Password?
                </Link>

              </div>

              <button
                disabled={loading}
                className="w-full rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white py-3 font-semibold transition"
              >
                {loading ? (
                  <span className="flex justify-center items-center gap-2">
                    <Loader2
                      size={18}
                      className="animate-spin"
                    />
                    Signing In...
                  </span>
                ) : (
                  "Sign In"
                )}
              </button>

            </form>

            <p className="text-center text-sm text-gray-500 mt-8">
              Don't have an account?{" "}
              <Link
                to="/register"
                className="font-semibold text-indigo-600"
              >
                Create Account
              </Link>
            </p>

          </div>

        </div>

      </div>

    </div>
  );
}
