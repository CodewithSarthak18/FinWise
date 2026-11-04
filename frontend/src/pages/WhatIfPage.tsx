import React, { useState, useEffect } from 'react';
import {
  Calculator,
  TrendingUp,
  PiggyBank,
  Sparkles,
  ShieldCheck,
  Percent,
  Sliders,
  ArrowRight,
  Info,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Badge } from '../components/common/Badge';
import { whatIfApi } from '../api/whatif';
import { WhatIfSimulationResult } from '../types';
import { formatINR } from '../utils/formatters';
import { useToast } from '../contexts/ToastContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const WhatIfPage: React.FC = () => {
  const { showToast } = useToast();
  const [categories, setCategories] = useState<Array<{ categoryId: number; categoryName: string; averageMonthlySpending: string }>>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number>(3); // Default Dining Out
  const [reductionPercentage, setReductionPercentage] = useState<number>(15);
  const [simulation, setSimulation] = useState<WhatIfSimulationResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    whatIfApi.getSpendingCategories().then((cats) => {
      setCategories(cats);
      if (cats.length > 0) {
        setSelectedCategoryId(cats[0].categoryId);
      }
    });
  }, []);

  const runSimulation = async (catId: number, pct: number) => {
    setIsLoading(true);
    try {
      const res = await whatIfApi.simulate(catId, pct);
      setSimulation(res);
    } catch {
      showToast('Simulation calculation failed', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (selectedCategoryId) {
      runSimulation(selectedCategoryId, reductionPercentage);
    }
  }, [selectedCategoryId, reductionPercentage]);

  const presetPercentages = [5, 10, 15, 20, 25];

  return (
    <div className="space-y-6 max-w-5xl">
      {/* Header */}
      <div>
        <div className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-teal-50 text-teal-700 border border-teal-200 mb-1.5">
          <Calculator className="w-3.5 h-3.5 mr-1" />
          Predictive Wealth Accumulation Engine
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">What-If Savings Calculator</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          Simulate lifestyle expense trims across spending buckets and preview the compound monthly and annual savings.
        </p>
      </div>

      {/* Safety Notice */}
      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600 flex items-start space-x-2.5">
        <ShieldCheck className="w-4 h-4 text-emerald-600 mt-0.5 flex-shrink-0" />
        <div>
          <span className="font-bold text-slate-800">Mathematical Simulation Guarantee:</span>
          <span>
            {' '}Simulations use historical transaction data without altering real account balances, budgets, or ledger records.
          </span>
        </div>
      </div>

      {/* Interactive Controls Card */}
      <Card>
        <CardHeader>
          <CardTitle>Select Spending Category & Reduction Target</CardTitle>
        </CardHeader>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
          {/* Category Selector */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-2">
              Expense Category to Optimize
            </label>
            <select
              value={selectedCategoryId}
              onChange={(e) => setSelectedCategoryId(Number(e.target.value))}
              className="block w-full rounded-xl border border-slate-300 bg-white py-2.5 px-3 text-slate-900 text-sm font-semibold focus:border-navy-900 focus:outline-none"
            >
              {categories.map((c) => (
                <option key={c.categoryId} value={c.categoryId}>
                  {c.categoryName} (Avg: {formatINR(c.averageMonthlySpending)}/mo)
                </option>
              ))}
            </select>
          </div>

          {/* Preset Buttons */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-2">
              Quick Preset Reduction
            </label>
            <div className="flex flex-wrap gap-2">
              {presetPercentages.map((p) => (
                <button
                  key={p}
                  onClick={() => setReductionPercentage(p)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                    reductionPercentage === p
                      ? 'bg-navy-900 text-white shadow-sm'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {p}% Cut
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Custom Percentage Slider */}
        <div className="mt-6 pt-6 border-t border-slate-100 space-y-2">
          <div className="flex items-center justify-between text-xs font-semibold">
            <span className="text-slate-700">Custom Cut Percentage</span>
            <span className="text-base font-black text-blue-600">{reductionPercentage}% Reduction</span>
          </div>
          <input
            type="range"
            min="1"
            max="50"
            step="1"
            value={reductionPercentage}
            onChange={(e) => setReductionPercentage(Number(e.target.value))}
            className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
          />
          <div className="flex justify-between text-[10px] text-slate-400 font-semibold">
            <span>1% (Minor tweak)</span>
            <span>25% (Moderate discipline)</span>
            <span>50% (Aggressive frugality)</span>
          </div>
        </div>
      </Card>

      {/* Simulation Results Display */}
      {isLoading ? (
        <LoadingSpinner message="Calculating compound annual savings projection..." />
      ) : simulation ? (
        <div className="space-y-6">
          {/* Main 4 Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-5 rounded-xl bg-white border border-slate-200 shadow-sm">
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Current Spending</span>
              <h3 className="text-xl font-bold text-slate-900 mt-1">{formatINR(simulation.currentMonthlySpending)}</h3>
              <p className="text-[11px] text-slate-400 mt-1">Average monthly baseline</p>
            </div>

            <div className="p-5 rounded-xl bg-white border border-slate-200 shadow-sm">
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Reduced Spending</span>
              <h3 className="text-xl font-bold text-slate-700 mt-1">{formatINR(simulation.reducedMonthlySpending)}</h3>
              <p className="text-[11px] text-emerald-600 font-semibold mt-1">Target monthly spend</p>
            </div>

            <div className="p-5 rounded-xl bg-emerald-50 border border-emerald-200 shadow-sm">
              <span className="text-[10px] uppercase font-bold text-emerald-700 block">Monthly Savings</span>
              <h3 className="text-xl font-black text-emerald-800 mt-1">+{formatINR(simulation.monthlySavings)}</h3>
              <p className="text-[11px] text-emerald-700 mt-1">Extra cash saved per month</p>
            </div>

            <div className="p-5 rounded-xl bg-gradient-to-tr from-navy-950 to-blue-900 text-white shadow-md border border-navy-800">
              <span className="text-[10px] uppercase font-bold text-blue-300 block">Projected Yearly Savings</span>
              <h3 className="text-2xl font-black text-white mt-1">+{formatINR(simulation.estimatedYearlySavings)}</h3>
              <p className="text-[11px] text-emerald-400 font-semibold mt-1">12-Month compounding gain</p>
            </div>
          </div>

          {/* Preset Comparison Cards Grid */}
          <Card>
            <CardHeader>
              <CardTitle>Benchmark Scenarios Comparison (Annualized)</CardTitle>
            </CardHeader>

            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
              {simulation.presetScenarios.map((scen) => {
                const isSelected = parseFloat(scen.percentage) === reductionPercentage;
                return (
                  <div
                    key={scen.percentage}
                    onClick={() => setReductionPercentage(parseFloat(scen.percentage))}
                    className={`p-3.5 rounded-xl border text-center cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-blue-50 border-blue-600 ring-2 ring-blue-600/20'
                        : 'bg-slate-50 border-slate-200/80 hover:bg-slate-100/70'
                    }`}
                  >
                    <span className="text-xs font-bold text-slate-700">{parseFloat(scen.percentage)}% Reduction</span>
                    <h4 className="text-base font-black text-slate-900 mt-1">
                      {formatINR(scen.monthlySavings)}
                    </h4>
                    <span className="text-[10px] font-semibold text-slate-400 block">per month</span>
                    <div className="mt-2 pt-2 border-t border-slate-200/60">
                      <span className="text-xs font-bold text-emerald-600">{formatINR(scen.estimatedYearlySavings)}</span>
                      <span className="text-[9px] text-slate-400 block">yearly</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </Card>
        </div>
      ) : null}
    </div>
  );
};
