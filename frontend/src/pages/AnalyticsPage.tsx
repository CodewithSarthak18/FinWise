import React, { useState, useEffect } from 'react';
import {
  BarChart3,
  TrendingUp,
  TrendingDown,
  PieChart,
  ShieldCheck,
  Sparkles,
  Calendar,
  Layers,
  ArrowUpRight,
  ArrowDownRight,
  Info,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { StatCard } from '../components/common/StatCard';
import { Badge } from '../components/common/Badge';
import { IncomeExpenseBarChart } from '../components/charts/IncomeExpenseBarChart';
import { CategoryPieChart } from '../components/charts/CategoryPieChart';
import { CashFlowTrendChart } from '../components/charts/CashFlowTrendChart';
import { HealthScoreGauge } from '../components/charts/HealthScoreGauge';
import { analyticsApi } from '../api/analytics';
import {
  CategoryExpense,
  FinancialHealthScore,
  IncomeVsExpense,
  MonthlySpendingTrend,
  NeedsWantsBreakdown,
  SmartInsight,
} from '../types';
import { formatINR, formatPercent } from '../utils/formatters';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const AnalyticsPage: React.FC = () => {
  const [selectedMonth, setSelectedMonth] = useState('2026-11');
  const [isLoading, setIsLoading] = useState(true);

  // Analytics states
  const [income, setIncome] = useState('133500.00');
  const [expense, setExpense] = useState('48200.00');
  const [netSavings, setNetSavings] = useState('85300.00');
  const [savingsRate, setSavingsRate] = useState(63.9);

  const [spendingTrends, setSpendingTrends] = useState<MonthlySpendingTrend[]>([]);
  const [categoryExpenses, setCategoryExpenses] = useState<CategoryExpense[]>([]);
  const [incomeVsExpense, setIncomeVsExpense] = useState<IncomeVsExpense[]>([]);
  const [needsWants, setNeedsWants] = useState<NeedsWantsBreakdown | null>(null);
  const [healthScore, setHealthScore] = useState<FinancialHealthScore | null>(null);
  const [insights, setInsights] = useState<SmartInsight[]>([]);

  useEffect(() => {
    const fetchAnalytics = async () => {
      setIsLoading(true);
      try {
        const [
          incRes,
          expRes,
          savRes,
          rateRes,
          trendsRes,
          catRes,
          incVsExpRes,
          needsWantsRes,
          healthRes,
          insightsRes,
        ] = await Promise.all([
          analyticsApi.getTotalIncome(selectedMonth),
          analyticsApi.getTotalExpenses(selectedMonth),
          analyticsApi.getNetSavings(selectedMonth),
          analyticsApi.getSavingsRate(selectedMonth),
          analyticsApi.getSpendingTrends(6),
          analyticsApi.getCategoryExpenses(selectedMonth),
          analyticsApi.getIncomeVsExpense(6),
          analyticsApi.getNeedsVsWants(selectedMonth),
          analyticsApi.getFinancialHealthScore(selectedMonth),
          analyticsApi.getSmartInsights(selectedMonth),
        ]);

        setIncome(incRes?.totalIncome ?? '0.00');
        setExpense(expRes?.totalExpenses ?? '0.00');
        setNetSavings(savRes?.netSavings ?? '0.00');
        setSavingsRate(rateRes?.savingsRate ?? 0);
        setSpendingTrends(Array.isArray(trendsRes) ? trendsRes : []);
        setCategoryExpenses(Array.isArray(catRes) ? catRes : []);
        setIncomeVsExpense(Array.isArray(incVsExpRes) ? incVsExpRes : []);
        setNeedsWants(needsWantsRes);
        setHealthScore(healthRes);
        const insList = Array.isArray(insightsRes?.insights) ? insightsRes.insights : Array.isArray(insightsRes) ? insightsRes : [];
        setInsights(insList);
      } catch (err) {
        console.error('Error fetching analytics:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAnalytics();
  }, [selectedMonth]);

  if (isLoading) {
    return <LoadingSpinner message="Calculating mathematical financial telemetry..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header & Period Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Financial Intelligence & Analytics</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Deep-dive multi-period telemetry, 50/30/20 breakdown, and educational financial health assessment.
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <Calendar className="w-4 h-4 text-slate-400" />
          <select
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white py-1.5 px-3 text-xs font-semibold text-slate-700 shadow-sm focus:border-navy-900 focus:outline-none"
          >
            <option value="2026-11">November 2026 (Current)</option>
            <option value="2026-10">October 2026</option>
            <option value="2026-09">September 2026</option>
            <option value="2026-08">August 2026</option>
          </select>
        </div>
      </div>

      {/* Row 1: Core Analytics KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Period Income"
          value={formatINR(income)}
          trend={{ value: '+13.1%', isPositive: true, label: 'MoM Growth' }}
          icon={ArrowUpRight}
          variant="income"
        />
        <StatCard
          title="Total Period Expenses"
          value={formatINR(expense)}
          trend={{ value: '-2.4%', isPositive: true, label: 'Spending Reduced' }}
          icon={ArrowDownRight}
          variant="expense"
        />
        <StatCard
          title="Net Savings"
          value={formatINR(netSavings)}
          subtitle="Net liquid cash retained"
          icon={TrendingUp}
          variant="savings"
        />
        <StatCard
          title="Net Savings Rate"
          value={`${savingsRate.toFixed(1)}%`}
          subtitle="Target threshold: > 40.0%"
          icon={ShieldCheck}
          variant="health"
        />
      </div>

      {/* Row 2: Charts (Cash Flow Bar + Category Donut) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2">
          <CardHeader>
            <div>
              <CardTitle>Income vs Expenses (6-Month Horizon)</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Dual-column historical earnings & expenditure</p>
            </div>
          </CardHeader>
          <IncomeExpenseBarChart data={incomeVsExpense} />
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Category Spend Breakdown</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Top expenditure buckets for {selectedMonth}</p>
            </div>
          </CardHeader>
          <CategoryPieChart data={categoryExpenses} />
        </Card>
      </div>

      {/* Row 3: 50/30/20 Rule Breakdown & Health Score Gauge */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 50/30/20 Rule Card */}
        <Card>
          <CardHeader>
            <div className="flex items-center space-x-2">
              <Layers className="w-5 h-5 text-blue-600" />
              <div>
                <CardTitle>50 / 30 / 20 Budget Rule Breakdown</CardTitle>
                <p className="text-xs text-slate-500 mt-0.5">Needs (50%) • Wants (30%) • Net Savings (20%)</p>
              </div>
            </div>
            {needsWants?.adheresTo503020Rule && (
              <Badge variant="emerald" size="sm">
                Adhering To Rule
              </Badge>
            )}
          </CardHeader>

          {needsWants && (
            <div className="space-y-4">
              {/* Needs */}
              <div className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-700">Needs (Rent, Groceries, Utilities, Transit)</span>
                  <span className="font-bold text-slate-900">
                    {formatINR(needsWants.needsAmount)} ({needsWants.needsPercentage.toFixed(1)}% of total)
                  </span>
                </div>
                <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-blue-600 rounded-full"
                    style={{ width: `${Math.min(100, needsWants.needsPercentage)}%` }}
                  />
                </div>
                <span className="text-[10px] text-slate-400">Target benchmark: $\le 50\%$</span>
              </div>

              {/* Wants */}
              <div className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-700">Wants (Dining, Shopping, Streaming, Leisure)</span>
                  <span className="font-bold text-slate-900">
                    {formatINR(needsWants.wantsAmount)} ({needsWants.wantsPercentage.toFixed(1)}% of total)
                  </span>
                </div>
                <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-purple-500 rounded-full"
                    style={{ width: `${Math.min(100, needsWants.wantsPercentage)}%` }}
                  />
                </div>
                <span className="text-[10px] text-slate-400">Target benchmark: $\le 30\%$</span>
              </div>

              {/* Savings */}
              <div className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-700">Investments & Net Savings</span>
                  <span className="font-bold text-emerald-600">
                    {formatINR(needsWants.savingsAmount)} ({needsWants.savingsPercentage.toFixed(1)}% of income)
                  </span>
                </div>
                <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-emerald-500 rounded-full"
                    style={{ width: `${Math.min(100, needsWants.savingsPercentage)}%` }}
                  />
                </div>
                <span className="text-[10px] text-slate-400">Target benchmark: $\ge 20\%$</span>
              </div>
            </div>
          )}
        </Card>

        {/* Financial Health Score Gauge */}
        <Card>
          <CardHeader>
            <CardTitle>Financial Health Index (Formula-Based)</CardTitle>
          </CardHeader>
          {healthScore ? (
            <HealthScoreGauge scoreData={healthScore} />
          ) : (
            <div className="py-8 text-center text-xs text-slate-500">Calculating score...</div>
          )}
        </Card>
      </div>

      {/* Row 4: Monthly Spending Trend (Area Chart) & Smart Insights */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2">
          <CardHeader>
            <div>
              <CardTitle>Monthly Expenditure Trajectory</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Smooth spend progression curves across past quarters</p>
            </div>
          </CardHeader>
          <CashFlowTrendChart data={spendingTrends} />
        </Card>

        {/* Smart Rule-Based Insights */}
        <Card>
          <CardHeader>
            <div className="flex items-center space-x-2">
              <Sparkles className="w-4 h-4 text-emerald-600" />
              <CardTitle>Smart Rule-Based Insights</CardTitle>
            </div>
          </CardHeader>

          <div className="space-y-3">
            {insights.map((ins, idx) => (
              <div
                key={idx}
                className="p-3 rounded-xl bg-slate-50 border border-slate-200/80 space-y-1 hover:bg-slate-100/70 transition-colors"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-900">{ins.title}</span>
                  <Badge
                    variant={ins.type === 'POSITIVE' ? 'emerald' : ins.type === 'WARNING' ? 'amber' : 'blue'}
                    size="sm"
                  >
                    {ins.category || ins.type}
                  </Badge>
                </div>
                <p className="text-xs text-slate-600">{ins.message}</p>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
};
