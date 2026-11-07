import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Wallet,
  ArrowUpRight,
  ArrowDownRight,
  PiggyBank,
  ArrowRight,
  Clock,
  Sparkles,
  AlertTriangle,
  Receipt,
  Calendar,
} from 'lucide-react';
import { StatCard } from '../components/common/StatCard';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { EmptyState } from '../components/common/EmptyState';
import { IncomeExpenseBarChart } from '../components/charts/IncomeExpenseBarChart';
import { CategoryPieChart } from '../components/charts/CategoryPieChart';
import { HealthScoreGauge } from '../components/charts/HealthScoreGauge';
import { analyticsApi } from '../api/analytics';
import { transactionsApi } from '../api/transactions';
import { budgetsApi } from '../api/budgets';
import { recurringApi } from '../api/recurring';
import {
  Budget,
  CategoryExpense,
  FinancialHealthScore,
  IncomeVsExpense,
  SmartInsight,
  Transaction,
  UpcomingReminder,
} from '../types';
import { formatINR, formatDate } from '../utils/formatters';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const DashboardPage: React.FC = () => {
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [summary, setSummary] = useState<{
    netWorth: string;
    currentMonthIncome: string;
    currentMonthExpense: string;
    netSavings: string;
    savingsRatePercentage: number;
  }>({
    netWorth: '0.00',
    currentMonthIncome: '0.00',
    currentMonthExpense: '0.00',
    netSavings: '0.00',
    savingsRatePercentage: 0,
  });

  const [incomeVsExpense, setIncomeVsExpense] = useState<IncomeVsExpense[]>([]);
  const [categoryExpenses, setCategoryExpenses] = useState<CategoryExpense[]>([]);
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
  const [currentBudget, setCurrentBudget] = useState<Budget | null>(null);
  const [upcomingReminders, setUpcomingReminders] = useState<UpcomingReminder[]>([]);
  const [healthScore, setHealthScore] = useState<FinancialHealthScore | null>(null);
  const [insights, setInsights] = useState<SmartInsight[]>([]);

  useEffect(() => {
    const fetchDashboardData = async () => {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const results = await Promise.allSettled([
          analyticsApi.getDashboardSummary(),
          analyticsApi.getIncomeVsExpense(6),
          analyticsApi.getCategoryExpenses(),
          transactionsApi.getTransactions({ size: 5 }),
          budgetsApi.getCurrentBudget(),
          recurringApi.getUpcomingReminders(14),
          analyticsApi.getFinancialHealthScore(),
          analyticsApi.getSmartInsights(),
        ]);

        // 1. Dashboard summary
        if (results[0].status === 'fulfilled') {
          const sumRes = results[0].value as any;
          const inc = parseFloat((sumRes.monthlyIncome || sumRes.currentMonthIncome || '0').toString());
          const exp = parseFloat((sumRes.monthlyExpense || sumRes.currentMonthExpense || '0').toString());
          const savings = inc - exp;

          setSummary({
            netWorth: (sumRes.netWorth || '0.00').toString(),
            currentMonthIncome: inc.toFixed(2),
            currentMonthExpense: exp.toFixed(2),
            netSavings: (sumRes.monthlySavings != null ? sumRes.monthlySavings : savings).toString(),
            savingsRatePercentage: Number(sumRes.savingsRate || sumRes.savingsRatePercentage || 0),
          });
        }

        // 2. Income vs Expense
        if (results[1].status === 'fulfilled') {
          setIncomeVsExpense(Array.isArray(results[1].value) ? results[1].value : []);
        }

        // 3. Category Expenses
        if (results[2].status === 'fulfilled') {
          setCategoryExpenses(Array.isArray(results[2].value) ? results[2].value : []);
        }

        // 4. Transactions
        if (results[3].status === 'fulfilled') {
          const txVal = results[3].value as any;
          setRecentTransactions(Array.isArray(txVal?.content) ? txVal.content : Array.isArray(txVal) ? txVal : []);
        }

        // 5. Current Budget
        if (results[4].status === 'fulfilled') {
          setCurrentBudget(results[4].value);
        }

        // 6. Upcoming Reminders
        if (results[5].status === 'fulfilled') {
          setUpcomingReminders(Array.isArray(results[5].value) ? results[5].value : []);
        }

        // 7. Health Score
        if (results[6].status === 'fulfilled') {
          setHealthScore(results[6].value);
        }

        // 8. Smart Insights
        if (results[7].status === 'fulfilled') {
          const insVal = results[7].value as any;
          setInsights(Array.isArray(insVal?.insights) ? insVal.insights : Array.isArray(insVal) ? insVal : []);
        }
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setErrorMessage('Failed to connect to the backend server. Please verify your connection.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (isLoading) {
    return <LoadingSpinner message="Aggregating user financial data from server..." />;
  }

  return (
    <div className="space-y-6">
      {/* Error alert if backend completely down */}
      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-medium flex items-center space-x-2">
          <AlertTriangle className="w-4 h-4 text-rose-600 flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Smart Insight Banner */}
      {insights.length > 0 && (
        <div className="p-4 rounded-xl bg-gradient-to-r from-blue-50 via-indigo-50 to-emerald-50 border border-blue-200/80 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-blue-600 text-white shadow-sm flex-shrink-0">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-blue-700">
                FinWise AI Recommendation
              </span>
              <p className="text-xs sm:text-sm font-semibold text-slate-800">
                {insights[0].message}
              </p>
            </div>
          </div>
          <Link to="/analytics" className="text-xs font-semibold text-blue-700 hover:text-blue-900 flex items-center flex-shrink-0">
            View all insights <ArrowRight className="w-3.5 h-3.5 ml-1" />
          </Link>
        </div>
      )}

      {/* Top Row: Executive Financial KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* 1. Total Net Worth / Balance */}
        <StatCard
          title="Total Net Worth"
          value={formatINR(summary.netWorth)}
          subtitle="Consolidated account balances"
          icon={Wallet}
          variant="default"
        />

        {/* 2. Monthly Income */}
        <StatCard
          title="Monthly Income"
          value={formatINR(summary.currentMonthIncome)}
          trend={{ value: 'Real Data', isPositive: true, label: 'Current month' }}
          icon={ArrowUpRight}
          variant="income"
        />

        {/* 3. Monthly Expenses */}
        <StatCard
          title="Monthly Expenses"
          value={formatINR(summary.currentMonthExpense)}
          trend={{ value: 'Real Data', isPositive: false, label: 'Current month' }}
          icon={ArrowDownRight}
          variant="expense"
        />

        {/* 4. Net Savings & Rate */}
        <StatCard
          title="Net Savings"
          value={formatINR(summary.netSavings)}
          subtitle={`${Number(summary.savingsRatePercentage || 0).toFixed(1)}% savings rate`}
          icon={PiggyBank}
          variant="savings"
        />
      </div>

      {/* Row 2: Charts (Income vs Expense & Spending Category) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Income vs Expense Chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <div>
              <CardTitle>Cash Flow: Inflow vs Outflow</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Historical 6-month comparison of earnings and spend</p>
            </div>
            <Link to="/analytics" className="text-xs font-semibold text-blue-600 hover:text-blue-800 flex items-center">
              Deep Dive <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </Link>
          </CardHeader>
          {incomeVsExpense.length > 0 ? (
            <IncomeExpenseBarChart data={incomeVsExpense} />
          ) : (
            <EmptyState
              title="No Cash Flow Data Available"
              description="Record income and expense transactions to view cash flow charts."
            />
          )}
        </Card>

        {/* Spending Category Donut Chart */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Category Expenses</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Distribution for Current Month</p>
            </div>
          </CardHeader>
          {categoryExpenses.length > 0 ? (
            <CategoryPieChart data={categoryExpenses} />
          ) : (
            <EmptyState
              title="No Category Expenses"
              description="No expense transactions have been recorded for this month."
            />
          )}
        </Card>
      </div>

      {/* Row 3: Financial Health Score & Budget Progress */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Financial Health Score */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Financial Health Index</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Automated assessment based on 3 financial pillars</p>
            </div>
          </CardHeader>
          {healthScore ? (
            <HealthScoreGauge scoreData={healthScore} />
          ) : (
            <EmptyState
              title="Health Score Pending"
              description="Score will compute automatically after logging transaction records."
            />
          )}
        </Card>

        {/* Budget Progress Card */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Monthly Budget Progress</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">
                {currentBudget ? currentBudget.name : 'Active Monthly Budget'}
              </p>
            </div>
            <Link to="/budgets">
              <Button size="sm" variant="outline" className="text-xs">
                Manage Budgets
              </Button>
            </Link>
          </CardHeader>

          {currentBudget ? (
            <div className="space-y-5">
              {/* Overall Burn Rate Bar */}
              <div>
                <div className="flex items-center justify-between text-xs mb-1.5">
                  <span className="font-semibold text-slate-700">Overall Budget Burn Rate</span>
                  <span className="font-bold text-slate-900">
                    {formatINR(currentBudget.totalSpent)} / {formatINR(currentBudget.totalBudgetAmount)} (
                    {Number(currentBudget.utilizationPercentage || 0).toFixed(1)}%)
                  </span>
                </div>
                <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      Number(currentBudget.utilizationPercentage || 0) > 100
                        ? 'bg-rose-600'
                        : Number(currentBudget.utilizationPercentage || 0) > 80
                        ? 'bg-amber-500'
                        : 'bg-emerald-500'
                    }`}
                    style={{ width: `${Math.min(100, Math.max(0, Number(currentBudget.utilizationPercentage || 0)))}%` }}
                  />
                </div>
                <div className="flex items-center justify-between text-[11px] text-slate-500 mt-1">
                  <span>Remaining: {formatINR(currentBudget.remainingBudget)}</span>
                  <Badge
                    variant={
                      currentBudget.overallAlertStatus === 'NORMAL'
                        ? 'emerald'
                        : currentBudget.overallAlertStatus === 'WARNING_80'
                        ? 'amber'
                        : 'rose'
                    }
                    size="sm"
                  >
                    {currentBudget.overallAlertStatus === 'NORMAL' ? 'On Track' : 'Alert Triggered'}
                  </Badge>
                </div>
              </div>

              {/* Top Category Split */}
              <div className="space-y-3 pt-2 border-t border-slate-100">
                <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 block">
                  Top Category Allocations
                </span>
                {Array.isArray(currentBudget.categories) && currentBudget.categories.slice(0, 3).map((cat) => (
                  <div key={cat.categoryId} className="space-y-1">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-medium text-slate-700">{cat.categoryName}</span>
                      <span className="font-semibold text-slate-900">
                        {formatINR(cat.spentAmount)} / {formatINR(cat.allocatedAmount)} ({Number(cat.utilizationPercentage || 0).toFixed(0)}%)
                      </span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          Number(cat.utilizationPercentage || 0) > 100
                            ? 'bg-rose-500'
                            : Number(cat.utilizationPercentage || 0) > 80
                            ? 'bg-amber-500'
                            : 'bg-blue-500'
                        }`}
                        style={{ width: `${Math.min(100, Math.max(0, Number(cat.utilizationPercentage || 0)))}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <EmptyState
              title="No Active Budget Defined"
              description="Define a monthly spending budget to track burn rates and receive alerts."
              actionText="Create Budget"
              onAction={() => window.location.href = '/budgets'}
            />
          )}
        </Card>
      </div>

      {/* Row 4: Recent Transactions & Upcoming Bills */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Transactions Table */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <div>
              <CardTitle>Recent Transactions</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Latest account ledger entries</p>
            </div>
            <Link to="/transactions" className="text-xs font-semibold text-blue-600 hover:text-blue-800 flex items-center">
              View All <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </Link>
          </CardHeader>

          {recentTransactions.length === 0 ? (
            <EmptyState
              icon={Receipt}
              title="No Transactions Recorded"
              description="Your account ledger is currently empty. Record your first income or expense."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider text-[10px] font-semibold border-b border-slate-100">
                  <tr>
                    <th className="py-2.5 px-3">Date</th>
                    <th className="py-2.5 px-3">Transaction</th>
                    <th className="py-2.5 px-3">Category</th>
                    <th className="py-2.5 px-3">Payment</th>
                    <th className="py-2.5 px-3 text-right">Amount</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {recentTransactions.map((tx) => {
                    const isIncome = tx.transactionType === 'INCOME';
                    return (
                      <tr key={tx.id} className="hover:bg-slate-50/80 transition-colors">
                        <td className="py-3 px-3 text-slate-500 whitespace-nowrap">
                          {formatDate(tx.transactionDate)}
                        </td>
                        <td className="py-3 px-3">
                          <p className="font-semibold text-slate-900">{tx.merchantName || tx.description || 'Transaction'}</p>
                          <p className="text-[11px] text-slate-400">{tx.accountName}</p>
                        </td>
                        <td className="py-3 px-3">
                          <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 font-medium text-[11px]">
                            {tx.categoryName}
                          </span>
                        </td>
                        <td className="py-3 px-3 text-slate-500">{tx.paymentMode}</td>
                        <td
                          className={`py-3 px-3 text-right font-bold whitespace-nowrap ${
                            isIncome ? 'text-emerald-600' : 'text-slate-900'
                          }`}
                        >
                          {isIncome ? '+' : '-'} {formatINR(tx.amount)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        {/* Upcoming Bills & Scheduled Reminders */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Upcoming Bills & SIPs</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Due in the next 14 days</p>
            </div>
            <Link to="/recurring" className="text-xs font-semibold text-blue-600 hover:text-blue-800 flex items-center">
              Calendar <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </Link>
          </CardHeader>

          {upcomingReminders.length === 0 ? (
            <EmptyState
              icon={Calendar}
              title="No Bills Due"
              description="No recurring expenses or SIP debits are due in the next 14 days."
            />
          ) : (
            <div className="space-y-3">
              {upcomingReminders.map((reminder) => (
                <div
                  key={reminder.id}
                  className="p-3 rounded-xl bg-slate-50 border border-slate-200/70 flex items-center justify-between hover:bg-slate-100/70 transition-colors"
                >
                  <div className="min-w-0 pr-2">
                    <p className="text-xs font-bold text-slate-800 truncate">{reminder.title}</p>
                    <div className="flex items-center space-x-1.5 text-[11px] text-slate-500 mt-0.5">
                      <Clock className="w-3 h-3 text-slate-400" />
                      <span>Due {formatDate(reminder.nextExecutionDate)}</span>
                      <span>•</span>
                      <span className="text-slate-600 font-medium">{reminder.accountName}</span>
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="text-xs font-bold text-slate-900">{formatINR(reminder.amount)}</p>
                    <span
                      className={`inline-block px-1.5 py-0.5 rounded text-[10px] font-bold mt-0.5 ${
                        Number(reminder.daysRemaining ?? (reminder as any).daysUntilDue ?? 0) <= 3
                          ? 'bg-rose-100 text-rose-700'
                          : 'bg-blue-100 text-blue-700'
                      }`}
                    >
                      in {Number(reminder.daysRemaining ?? (reminder as any).daysUntilDue ?? 0)} days
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};
