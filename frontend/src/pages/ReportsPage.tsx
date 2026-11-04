import React, { useState, useEffect } from 'react';
import {
  FileText,
  FileSpreadsheet,
  Calendar,
  TrendingUp,
  TrendingDown,
  PieChart,
  Target,
  ShieldAlert,
  Eye,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Badge } from '../components/common/Badge';
import { useToast } from '../contexts/ToastContext';
import { reportsApi, ReportType, ReportQueryParams } from '../api/reports';
import { transactionsApi } from '../api/transactions';
import { Category } from '../types';

interface ReportTabConfig {
  id: ReportType;
  title: string;
  description: string;
  icon: React.ElementType;
}

const REPORT_TABS: ReportTabConfig[] = [
  {
    id: 'MONTHLY',
    title: 'Monthly Report',
    description: 'Complete breakdown of monthly cash flow, category expenses, and active budget adherence.',
    icon: Calendar,
  },
  {
    id: 'ANNUAL',
    title: 'Annual Report',
    description: 'Year-at-a-glance financial summary, 12-month trends, and annual savings averages.',
    icon: TrendingUp,
  },
  {
    id: 'CATEGORY_WISE',
    title: 'Category-Wise Report',
    description: 'Detailed expense & income analysis across categories with average ticket size and share.',
    icon: PieChart,
  },
  {
    id: 'INCOME_EXPENSE',
    title: 'Income & Expense',
    description: 'Cash flow analysis, net savings, and 50/30/20 Needs vs Wants vs Savings breakdown.',
    icon: TrendingDown,
  },
  {
    id: 'SAVINGS',
    title: 'Savings & Goals',
    description: 'Progress across all long-term savings goals, target milestones, and contribution logs.',
    icon: Target,
  },
  {
    id: 'BUDGET_PERFORMANCE',
    title: 'Budget Performance',
    description: 'Planned vs actual expenditures, category variances, utilization %, and overspending alerts.',
    icon: ShieldAlert,
  },
];

export const ReportsPage: React.FC = () => {
  const { showToast } = useToast();

  const [activeTab, setActiveTab] = useState<ReportType>('MONTHLY');
  const [year, setYear] = useState<number>(new Date().getFullYear());
  const [month, setMonth] = useState<number>(new Date().getMonth() + 1);
  const [startDate, setStartDate] = useState<string>(
    new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0]
  );
  const [endDate, setEndDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [categoryId, setCategoryId] = useState<string>('');
  const [categories, setCategories] = useState<Category[]>([]);

  const [downloadingFormat, setDownloadingFormat] = useState<'pdf' | 'csv' | null>(null);
  const [loadingPreview, setLoadingPreview] = useState<boolean>(false);
  const [previewData, setPreviewData] = useState<any>(null);

  useEffect(() => {
    loadCategories();
  }, []);

  useEffect(() => {
    setPreviewData(null);
  }, [activeTab]);

  const loadCategories = async () => {
    try {
      const data = await transactionsApi.getCategories();
      setCategories(data);
    } catch (err) {
      console.error('Failed to load categories', err);
    }
  };

  const getParams = (): ReportQueryParams => {
    return {
      year,
      month,
      startDate: activeTab !== 'MONTHLY' && activeTab !== 'ANNUAL' ? startDate : undefined,
      endDate: activeTab !== 'MONTHLY' && activeTab !== 'ANNUAL' ? endDate : undefined,
      categoryId: categoryId ? parseInt(categoryId) : undefined,
    };
  };

  const handleDownload = async (format: 'pdf' | 'csv') => {
    if (activeTab !== 'MONTHLY' && activeTab !== 'ANNUAL') {
      if (!startDate || !endDate) {
        showToast('Please specify both start date and end date', 'error');
        return;
      }
      if (new Date(startDate) > new Date(endDate)) {
        showToast('Start date cannot be after end date', 'error');
        return;
      }
    }

    try {
      setDownloadingFormat(format);
      await reportsApi.downloadReport(activeTab, format, getParams());
      showToast(`${REPORT_TABS.find((t) => t.id === activeTab)?.title} (${format.toUpperCase()}) downloaded!`, 'success');
    } catch (err: any) {
      const message = err.response?.data?.message || `Failed to download ${format.toUpperCase()} report.`;
      showToast(message, 'error');
    } finally {
      setDownloadingFormat(null);
    }
  };

  const handlePreview = async () => {
    if (activeTab !== 'MONTHLY' && activeTab !== 'ANNUAL') {
      if (!startDate || !endDate) {
        showToast('Please specify both start date and end date', 'error');
        return;
      }
      if (new Date(startDate) > new Date(endDate)) {
        showToast('Start date cannot be after end date', 'error');
        return;
      }
    }

    try {
      setLoadingPreview(true);
      const data = await reportsApi.getPreviewData(activeTab, getParams());
      setPreviewData(data);
      showToast('Report preview generated successfully', 'info');
    } catch (err: any) {
      const message = err.response?.data?.message || 'Failed to load report preview.';
      showToast(message, 'error');
    } finally {
      setLoadingPreview(false);
    }
  };

  const monthOptions = [
    { value: 1, label: 'January' },
    { value: 2, label: 'February' },
    { value: 3, label: 'March' },
    { value: 4, label: 'April' },
    { value: 5, label: 'May' },
    { value: 6, label: 'June' },
    { value: 7, label: 'July' },
    { value: 8, label: 'August' },
    { value: 9, label: 'September' },
    { value: 10, label: 'October' },
    { value: 11, label: 'November' },
    { value: 12, label: 'December' },
  ];

  const yearOptions = [
    { value: 2024, label: '2024' },
    { value: 2025, label: '2025' },
    { value: 2026, label: '2026' },
    { value: 2027, label: '2027' },
  ];

  const categoryOptions = [
    { value: '', label: 'All Categories' },
    ...categories.map((c) => ({
      value: c.id,
      label: `${c.name} (${c.type})`,
    })),
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Financial Reports & Exports</h1>
          <p className="text-sm text-slate-500">
            Export audit-ready financial statements in PDF and CSV formatted in Indian Rupees (INR).
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        {REPORT_TABS.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex flex-col items-center justify-center rounded-xl p-4 text-center transition-all ${
                isActive
                  ? 'border-2 border-emerald-600 bg-emerald-50/50 shadow-sm'
                  : 'border border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
              }`}
            >
              <div
                className={`mb-2 rounded-lg p-2 ${
                  isActive ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-600'
                }`}
              >
                <Icon className="h-5 w-5" />
              </div>
              <span className={`text-xs font-semibold ${isActive ? 'text-emerald-900' : 'text-slate-800'}`}>
                {tab.title}
              </span>
            </button>
          );
        })}
      </div>

      {/* Configuration Card */}
      <Card className="p-6">
        <div className="flex flex-col gap-4 border-b border-slate-100 pb-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">
              {REPORT_TABS.find((t) => t.id === activeTab)?.title} Configuration
            </h2>
            <p className="text-xs text-slate-500">
              {REPORT_TABS.find((t) => t.id === activeTab)?.description}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="outline"
              onClick={handlePreview}
              isLoading={loadingPreview}
              disabled={downloadingFormat !== null}
            >
              <Eye className="mr-2 h-4 w-4 text-slate-600" />
              Preview Data
            </Button>
            <Button
              variant="outline"
              onClick={() => handleDownload('csv')}
              isLoading={downloadingFormat === 'csv'}
              disabled={loadingPreview}
              className="border-emerald-200 text-emerald-700 hover:bg-emerald-50"
            >
              <FileSpreadsheet className="mr-2 h-4 w-4 text-emerald-600" />
              Export CSV
            </Button>
            <Button
              onClick={() => handleDownload('pdf')}
              isLoading={downloadingFormat === 'pdf'}
              disabled={loadingPreview}
              className="bg-slate-900 text-white hover:bg-slate-800"
            >
              <FileText className="mr-2 h-4 w-4" />
              Export PDF
            </Button>
          </div>
        </div>

        {/* Filter Controls */}
        <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {activeTab === 'MONTHLY' && (
            <>
              <div>
                <Select
                  label="Month"
                  value={month}
                  onChange={(e) => setMonth(parseInt(e.target.value))}
                  options={monthOptions}
                />
              </div>
              <div>
                <Select
                  label="Year"
                  value={year}
                  onChange={(e) => setYear(parseInt(e.target.value))}
                  options={yearOptions}
                />
              </div>
            </>
          )}

          {activeTab === 'ANNUAL' && (
            <div>
              <Select
                label="Financial Year"
                value={year}
                onChange={(e) => setYear(parseInt(e.target.value))}
                options={yearOptions}
              />
            </div>
          )}

          {activeTab !== 'MONTHLY' && activeTab !== 'ANNUAL' && (
            <>
              <div>
                <label className="mb-1 block text-xs font-semibold uppercase text-slate-600">Start Date</label>
                <Input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  max={endDate}
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold uppercase text-slate-600">End Date</label>
                <Input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  min={startDate}
                />
              </div>
              {activeTab === 'CATEGORY_WISE' && (
                <div>
                  <Select
                    label="Category Filter"
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                    options={categoryOptions}
                  />
                </div>
              )}
            </>
          )}
        </div>
      </Card>

      {/* Preview Section */}
      {previewData && (
        <Card className="space-y-6 p-6">
          <div className="flex flex-col gap-2 border-b border-slate-100 pb-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <Badge variant="emerald">Live Preview</Badge>
                <h3 className="text-lg font-bold text-slate-900">{previewData.metadata?.title}</h3>
              </div>
              <p className="text-xs text-slate-500">
                Generated: {previewData.metadata?.generatedAt} • Period: {previewData.metadata?.dateRangeText} • Currency: {previewData.metadata?.currencyCode}
              </p>
            </div>
            <div className="text-xs font-medium text-slate-500">
              Account: {previewData.metadata?.userName} ({previewData.metadata?.userEmail})
            </div>
          </div>

          {/* Quick Metrics */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {previewData.formattedTotalIncome && (
              <div className="rounded-lg bg-emerald-50/60 p-4 border border-emerald-100">
                <p className="text-xs font-semibold uppercase text-emerald-800">Total Income</p>
                <p className="mt-1 text-xl font-bold text-emerald-700">{previewData.formattedTotalIncome}</p>
              </div>
            )}
            {previewData.formattedTotalExpenses && (
              <div className="rounded-lg bg-rose-50/60 p-4 border border-rose-100">
                <p className="text-xs font-semibold uppercase text-rose-800">Total Expenses</p>
                <p className="mt-1 text-xl font-bold text-rose-700">{previewData.formattedTotalExpenses}</p>
              </div>
            )}
            {previewData.formattedNetSavings && (
              <div className="rounded-lg bg-slate-50 p-4 border border-slate-200">
                <p className="text-xs font-semibold uppercase text-slate-600">Net Savings</p>
                <p className="mt-1 text-xl font-bold text-slate-900">{previewData.formattedNetSavings}</p>
              </div>
            )}
            {previewData.formattedSavingsRate && (
              <div className="rounded-lg bg-slate-50 p-4 border border-slate-200">
                <p className="text-xs font-semibold uppercase text-slate-600">Savings Rate</p>
                <p className="mt-1 text-xl font-bold text-emerald-600">{previewData.formattedSavingsRate}</p>
              </div>
            )}
          </div>

          {/* Budget Adherence if available */}
          {previewData.hasBudget && (
            <div className="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold uppercase text-slate-700">
                  Budget: {previewData.budgetName}
                </span>
                <Badge variant={previewData.budgetStatus === 'NORMAL' ? 'emerald' : 'amber'}>
                  {previewData.budgetStatus} ({previewData.formattedBudgetUtilization})
                </Badge>
              </div>
              <div className="grid grid-cols-3 gap-2 text-xs">
                <div>
                  <span className="text-slate-500">Allocated:</span>{' '}
                  <span className="font-semibold text-slate-800">{previewData.formattedBudgetAmount}</span>
                </div>
                <div>
                  <span className="text-slate-500">Spent:</span>{' '}
                  <span className="font-semibold text-rose-600">{previewData.formattedBudgetSpent}</span>
                </div>
                <div>
                  <span className="text-slate-500">Remaining:</span>{' '}
                  <span className="font-semibold text-emerald-600">{previewData.formattedBudgetRemaining}</span>
                </div>
              </div>
            </div>
          )}

          {/* Category Breakdown Table Preview */}
          {previewData.categoryBreakdown && previewData.categoryBreakdown.length > 0 && (
            <div>
              <h4 className="mb-3 text-sm font-bold text-slate-900">Category Breakdown</h4>
              <div className="overflow-x-auto rounded-lg border border-slate-200">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-100 text-slate-700">
                    <tr>
                      <th className="p-3 font-semibold">Category</th>
                      <th className="p-3 font-semibold text-right">Amount</th>
                      <th className="p-3 font-semibold text-right">% of Expenses</th>
                      <th className="p-3 font-semibold text-right">Count</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {previewData.categoryBreakdown.map((cat: any, i: number) => (
                      <tr key={i} className="hover:bg-slate-50">
                        <td className="p-3 font-medium text-slate-800">{cat.categoryName}</td>
                        <td className="p-3 text-right font-semibold text-slate-900">{cat.formattedAmount}</td>
                        <td className="p-3 text-right text-slate-600">{cat.formattedPercentage}</td>
                        <td className="p-3 text-right text-slate-600">{cat.transactionCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </Card>
      )}
    </div>
  );
};
