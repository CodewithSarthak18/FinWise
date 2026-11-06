import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { useToast } from '../contexts/ToastContext';
import { Lock, Mail, ArrowRight, ShieldCheck } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login({ email, password });
      showToast('Welcome back to FinWise!', 'success');
      navigate('/dashboard');
    } catch (err: any) {
      let serverMsg = err?.response?.data?.message;
      if (!serverMsg) {
        if (err?.code === 'ECONNABORTED' || err?.message?.includes('timeout')) {
          serverMsg = 'Connection timed out. The server might be waking up—please try again in a few moments.';
        } else {
          serverMsg = 'Invalid email or password. Please try again.';
        }
      }
      setError(serverMsg);
      showToast(serverMsg, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFillDemo = () => {
    setEmail('rahul.verma@example.com');
    setPassword('DevPassword#2026');
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 selection:bg-blue-100">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Link to="/" className="inline-flex items-center space-x-2.5">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-emerald-500 flex items-center justify-center text-white font-bold text-xl shadow-md">
            F
          </div>
          <span className="text-2xl font-black text-slate-900 tracking-tight">FinWise</span>
        </Link>
        <h2 className="mt-6 text-2xl font-bold tracking-tight text-slate-900">
          Sign in to your financial dashboard
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          Or{' '}
          <Link to="/register" className="font-semibold text-blue-600 hover:text-blue-500">
            create a new account with ₹ INR support
          </Link>
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4">
        <div className="bg-white py-8 px-6 sm:px-10 rounded-2xl shadow-sm border border-slate-200/80">
          {error && (
            <div className="mb-4 p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-700 font-medium">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Input
                label="Work or Personal Email"
                type="email"
                required
                placeholder="rahul.verma@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-600">
                  Password
                </label>
                <span className="text-xs text-blue-600 hover:underline cursor-pointer">
                  Forgot password?
                </span>
              </div>
              <Input
                type="password"
                required
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <Button type="submit" variant="primary" size="lg" className="w-full mt-2" isLoading={isLoading}>
              Sign In <ArrowRight className="w-4 h-4 ml-2" />
            </Button>
          </form>

          {/* Demo Credentials Quick Fill */}
          <div className="mt-6 pt-6 border-t border-slate-100">
            <div className="flex items-center justify-between text-xs text-slate-500 mb-2">
              <span className="font-medium text-slate-700">Need instant testing credentials?</span>
              <button
                type="button"
                onClick={handleFillDemo}
                className="text-xs font-semibold text-emerald-600 hover:text-emerald-700 underline"
              >
                Autofill Seed Data
              </button>
            </div>
            <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/80 text-[11px] font-mono text-slate-600 space-y-0.5">
              <div className="flex items-center">
                <Mail className="w-3 h-3 mr-1.5 text-slate-400" />
                <span>rahul.verma@example.com</span>
              </div>
              <div className="flex items-center">
                <Lock className="w-3 h-3 mr-1.5 text-slate-400" />
                <span>DevPassword#2026</span>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-6 text-center flex items-center justify-center space-x-1.5 text-xs text-slate-500">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          <span>Stateless JWT Authentication with 256-Bit Cryptography</span>
        </div>
      </div>
    </div>
  );
};
