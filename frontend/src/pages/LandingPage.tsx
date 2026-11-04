import React from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldCheck,
  TrendingUp,
  PieChart,
  Sparkles,
  ArrowRight,
  Calculator,
  Lock,
  ChevronRight,
  Layers,
  Calendar,
} from 'lucide-react';
import { Button } from '../components/common/Button';

export const LandingPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-900 text-white selection:bg-blue-500 selection:text-white">
      {/* Top Navbar */}
      <header className="sticky top-0 z-50 backdrop-blur-md bg-slate-900/80 border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-emerald-500 flex items-center justify-center text-white font-black text-xl shadow-lg shadow-blue-500/20">
              F
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight text-white">FinWise</span>
              <span className="block text-[10px] font-semibold text-emerald-400 tracking-wider uppercase">
                Enterprise Wealth
              </span>
            </div>
          </div>

          <nav className="hidden md:flex items-center space-x-8 text-sm font-medium text-slate-300">
            <a href="#features" className="hover:text-white transition-colors">Features</a>
            <a href="#intelligence" className="hover:text-white transition-colors">Intelligence</a>
            <a href="#security" className="hover:text-white transition-colors">Security</a>
          </nav>

          <div className="flex items-center space-x-3">
            <Link to="/login">
              <Button variant="ghost" className="text-slate-200 hover:text-white hover:bg-slate-800 text-sm">
                Sign In
              </Button>
            </Link>
            <Link to="/register">
              <Button variant="emerald" className="text-sm shadow-md shadow-emerald-600/30">
                Get Started <ArrowRight className="w-4 h-4 ml-1.5" />
              </Button>
            </Link>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative pt-20 pb-28 overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_80%_80%_at_50%_-20%,rgba(59,130,246,0.25),rgba(255,255,255,0))]"></div>
        <div className="relative max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="inline-flex items-center px-3.5 py-1.5 rounded-full bg-slate-800 border border-slate-700 text-xs font-semibold text-emerald-400 mb-8 shadow-inner">
            <Sparkles className="w-4 h-4 mr-2 text-emerald-400 animate-pulse" />
            FinWise 2.0 with AI Subscription Detector is Live
          </div>

          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight leading-tight text-white max-w-4xl mx-auto">
            Take Total Command of Your{' '}
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-emerald-400 to-teal-300">
              Personal Wealth & Cash Flow
            </span>
          </h1>

          <p className="mt-6 text-lg sm:text-xl text-slate-300 max-w-2xl mx-auto leading-relaxed">
            The modern financial operating system for high-performing professionals. Track multi-account balances,
            simulate savings scenarios, and automate bill tracking with precision.
          </p>

          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link to="/register" className="w-full sm:w-auto">
              <Button size="lg" variant="emerald" className="w-full sm:w-auto text-base px-8 py-3.5 shadow-xl shadow-emerald-500/20">
                Start Free Trial <ChevronRight className="w-5 h-5 ml-1" />
              </Button>
            </Link>
            <Link to="/login" className="w-full sm:w-auto">
              <Button size="lg" variant="outline" className="w-full sm:w-auto text-base px-8 py-3.5 bg-slate-800/80 border-slate-700 text-white hover:bg-slate-700">
                Explore Demo Account
              </Button>
            </Link>
          </div>

          {/* Trust Badges */}
          <div className="mt-14 pt-8 border-t border-slate-800/80 flex flex-wrap items-center justify-center gap-8 text-xs font-medium text-slate-400">
            <div className="flex items-center space-x-2">
              <Lock className="w-4 h-4 text-emerald-400" />
              <span>256-Bit Bank Level Encryption</span>
            </div>
            <div className="flex items-center space-x-2">
              <ShieldCheck className="w-4 h-4 text-blue-400" />
              <span>Native Indian Rupee (INR) Precision</span>
            </div>
            <div className="flex items-center space-x-2">
              <Sparkles className="w-4 h-4 text-purple-400" />
              <span>Double-Entry Transfer Verification</span>
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section id="features" className="py-24 bg-slate-950 border-t border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <span className="text-xs font-bold uppercase tracking-widest text-emerald-400">Engineered For Clarity</span>
            <h2 className="text-3xl font-bold text-white mt-2">Comprehensive Wealth Management Architecture</h2>
            <p className="text-sm text-slate-400 mt-3">
              Everything required to budget, track, and amplify your savings without spreadsheet chaos.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all shadow-lg group">
              <div className="w-12 h-12 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <TrendingUp className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Real-Time Financial Health Score</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Receive an educational 0–100 score analyzing savings rate, budget adherence, and spending volatility to
                coach better financial habits.
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all shadow-lg group">
              <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <PieChart className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Category Burn-Rate Tracking</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Smart threshold alerts at 50%, 80%, 100%, and 120% budget utilization to prevent unexpected overspending before month-end.
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all shadow-lg group">
              <div className="w-12 h-12 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <Sparkles className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">AI Subscription Detector</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Automatically scans recurring merchant debits and flags recurring subscriptions, estimating annual cost and upcoming due dates.
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all shadow-lg group">
              <div className="w-12 h-12 rounded-xl bg-teal-500/10 border border-teal-500/20 text-teal-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <Calculator className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">What-If Savings Simulator</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Simulate cutting 5%, 10%, 15%, or 20% from discretionary categories and immediately view projected annual wealth accumulation.
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all shadow-lg group">
              <div className="w-12 h-12 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <Calendar className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Visual Financial Calendar</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                View bills, SIP investments, salaries, and daily cash outflows overlaid onto an interactive calendar so no due date surprises you.
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all shadow-lg group">
              <div className="w-12 h-12 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <Layers className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">50/30/20 Rule Intelligence</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Automated classification of spending into Needs, Wants, and Net Savings to ensure your financial blueprint remains resilient.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 bg-slate-950 border-t border-slate-900 text-xs text-slate-500 text-center">
        <div className="max-w-7xl mx-auto px-4">
          <p>© 2026 FinWise Technologies. All rights reserved. Built with Java 21 & React 18.</p>
        </div>
      </footer>
    </div>
  );
};
