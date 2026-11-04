import React, { useState, useEffect } from 'react';
import {
  PieChart,
  Plus,
  AlertTriangle,
  CheckCircle2,
  AlertOctagon,
  Clock,
  Trash2,
  Edit2,
  Layers,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { budgetsApi } from '../api/budgets';
import { transactionsApi } from '../api/transactions';
import { Budget, Category } from '../types';
import { formatINR } from '../utils/formatters';
import { useToast } from '../contexts/ToastContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const BudgetsPage: React.FC = () => {
  const { showToast } = useToast();
  const [isLoading, setIsLoading] = useState(true);
  const [currentBudget, setCurrentBudget] = useState<Budget | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);

  // Create Modal
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newBudgetName, setNewBudgetName] = useState('December 2026 Monthly Budget');
  const [newTotalAmount, setNewTotalAmount] = useState('65000');
  const [newAllocations, setNewAllocations] = useState<Array<{ categoryId: number; amount: string }>>([
    { categoryId: 1, amount: '22000' },
    { categoryId: 2, amount: '12000' },
    { categoryId: 3, amount: '8000' },
    { categoryId: 4, amount: '6000' },
  ]);

  const fetchBudget = async () => {
    setIsLoading(true);
    try {
      const [bgt, cats] = await Promise.all([
        budgetsApi.getCurrentBudget(),
        transactionsApi.getCategories('EXPENSE'),
      ]);
      setCurrentBudget(bgt);
      setCategories(cats);
    } catch {
      showToast('Error loading budget data', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchBudget();
  }, []);

  const getAlertBadge = (status: string) => {
    switch (status) {
      case 'OVERSPENT_120':
        return (
          <Badge variant="rose" size="sm" className="font-bold flex items-center">
            <AlertOctagon className="w-3 h-3 mr-1" /> Overspent (&gt;120%)
          </Badge>
        );
      case 'EXCEEDED_100':
        return (
          <Badge variant="rose" size="sm" className="font-bold flex items-center">
            <AlertTriangle className="w-3 h-3 mr-1" /> Budget Exceeded (100%)
          </Badge>
        );
      case 'WARNING_80':
        return (
          <Badge variant="amber" size="sm" className="font-bold flex items-center">
            <AlertTriangle className="w-3 h-3 mr-1" /> Caution (80% Reached)
          </Badge>
        );
      default:
        return (
          <Badge variant="emerald" size="sm" className="font-bold flex items-center">
            <CheckCircle2 className="w-3 h-3 mr-1" /> On Track (Normal)
          </Badge>
        );
    }
  };

  const handleCreateBudget = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await budgetsApi.createBudget({
        name: newBudgetName,
        periodType: 'MONTHLY',
        startDate: '2026-12-01',
        endDate: '2026-12-31',
        totalBudgetAmount: newTotalAmount,
        notificationThreshold: 80,
        categories: newAllocations.map((a) => ({
          categoryId: a.categoryId,
          allocatedAmount: a.amount,
        })),
      });
      showToast('New budget created successfully', 'success');
      setIsCreateOpen(false);
      fetchBudget();
    } catch {
      showToast('Failed to create budget', 'error');
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Calculating real-time budget utilization..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Budget Management</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Monitor real-time expenditure against allocated monthly caps with automated threshold warnings.
          </p>
        </div>

        <Button variant="emerald" size="sm" onClick={() => setIsCreateOpen(true)} className="text-xs">
          <Plus className="w-4 h-4 mr-1.5" />
          Create New Budget
        </Button>
      </div>

      {currentBudget ? (
        <>
          {/* Active Budget Executive Summary */}
          <Card className="border-l-4 border-l-blue-600">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-100">
              <div>
                <div className="flex items-center space-x-2">
                  <h2 className="text-lg font-bold text-slate-900">{currentBudget.name}</h2>
                  {getAlertBadge(currentBudget.overallAlertStatus)}
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  Period: {currentBudget.startDate} to {currentBudget.endDate} (Monthly)
                </p>
              </div>

              <div className="flex items-center space-x-6 text-sm">
                <div>
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Total Budget</span>
                  <span className="text-base font-bold text-slate-900">{formatINR(currentBudget.totalBudgetAmount)}</span>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Actual Spent</span>
                  <span className="text-base font-bold text-rose-600">{formatINR(currentBudget.totalSpent)}</span>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Remaining</span>
                  <span className="text-base font-bold text-emerald-600">{formatINR(currentBudget.remainingBudget)}</span>
                </div>
              </div>
            </div>

            {/* Overall Progress Meter */}
            <div className="mt-4 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-slate-700">Monthly Burn Rate Progress</span>
                <span className="font-bold text-slate-900">{currentBudget.utilizationPercentage.toFixed(1)}% Used</span>
              </div>
              <div className="w-full h-3.5 bg-slate-100 rounded-full overflow-hidden p-0.5">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    currentBudget.utilizationPercentage > 100
                      ? 'bg-rose-600'
                      : currentBudget.utilizationPercentage > 80
                      ? 'bg-amber-500'
                      : 'bg-emerald-500'
                  }`}
                  style={{ width: `${Math.min(100, currentBudget.utilizationPercentage)}%` }}
                />
              </div>
              <div className="flex items-center justify-between text-[11px] text-slate-400">
                <span>0%</span>
                <span>50% (Normal)</span>
                <span>80% (Warning Threshold)</span>
                <span>100% (Cap)</span>
              </div>
            </div>
          </Card>

          {/* Category-Wise Budget Allocations Grid */}
          <div>
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500 mb-3">
              Category-Wise Spending Discipline
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {currentBudget.categories.map((cat) => {
                const isOverspent = cat.utilizationPercentage >= 100;
                return (
                  <Card key={cat.categoryId} hoverEffect className="space-y-3">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center space-x-2">
                        <div
                          className="w-3 h-3 rounded-full flex-shrink-0"
                          style={{ backgroundColor: cat.colorCode || '#3B82F6' }}
                        />
                        <h4 className="text-sm font-bold text-slate-900 truncate">{cat.categoryName}</h4>
                      </div>
                      {getAlertBadge(cat.alertStatus)}
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs py-2 border-y border-slate-100">
                      <div>
                        <span className="text-[10px] text-slate-400 uppercase font-bold block">Allocated Cap</span>
                        <span className="font-bold text-slate-800">{formatINR(cat.allocatedAmount)}</span>
                      </div>
                      <div className="text-right">
                        <span className="text-[10px] text-slate-400 uppercase font-bold block">Actual Spent</span>
                        <span className={`font-bold ${isOverspent ? 'text-rose-600' : 'text-slate-800'}`}>
                          {formatINR(cat.spentAmount)}
                        </span>
                      </div>
                    </div>

                    {/* Progress Bar */}
                    <div className="space-y-1">
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-500">Utilization</span>
                        <span className="font-bold text-slate-800">{cat.utilizationPercentage.toFixed(1)}%</span>
                      </div>
                      <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all duration-300 ${
                            cat.utilizationPercentage >= 100
                              ? 'bg-rose-500'
                              : cat.utilizationPercentage >= 80
                              ? 'bg-amber-500'
                              : 'bg-emerald-500'
                          }`}
                          style={{ width: `${Math.min(100, cat.utilizationPercentage)}%` }}
                        />
                      </div>
                      <p className="text-[10px] text-slate-400 text-right mt-1">
                        Remaining: <span className="font-semibold text-slate-700">{formatINR(cat.remainingAmount)}</span>
                      </p>
                    </div>
                  </Card>
                );
              })}
            </div>
          </div>
        </>
      ) : (
        <Card className="p-12 text-center">
          <PieChart className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">No Active Budget Plan Found</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1 mb-4">
            Setting up a monthly budget ensures your actual expenses do not eat into your net savings goals.
          </p>
          <Button variant="emerald" onClick={() => setIsCreateOpen(true)}>
            Create First Monthly Budget
          </Button>
        </Card>
      )}

      {/* Create Monthly Budget Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create Monthly Budget" maxWidth="lg">
        <form onSubmit={handleCreateBudget} className="space-y-4">
          <Input
            label="Budget Name"
            required
            value={newBudgetName}
            onChange={(e) => setNewBudgetName(e.target.value)}
          />

          <Input
            label="Overall Total Budget Cap"
            type="number"
            step="100"
            prefixText="₹"
            required
            value={newTotalAmount}
            onChange={(e) => setNewTotalAmount(e.target.value)}
          />

          <div className="space-y-2 pt-2 border-t border-slate-100">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-600 block">
              Category Splits (Top Allocations)
            </span>
            <div className="grid grid-cols-2 gap-3 max-h-56 overflow-y-auto pr-1">
              {categories.slice(0, 6).map((cat, idx) => {
                const currentVal = newAllocations.find((a) => a.categoryId === cat.id)?.amount || '5000';
                return (
                  <div key={cat.id} className="p-2.5 rounded-lg border border-slate-200 bg-slate-50/50 space-y-1">
                    <span className="text-xs font-semibold text-slate-700 block truncate">{cat.name}</span>
                    <Input
                      type="number"
                      prefixText="₹"
                      value={currentVal}
                      onChange={(e) => {
                        const val = e.target.value;
                        setNewAllocations((prev) => {
                          const existing = prev.filter((x) => x.categoryId !== cat.id);
                          return [...existing, { categoryId: cat.id, amount: val }];
                        });
                      }}
                    />
                  </div>
                );
              })}
            </div>
          </div>

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="emerald">
              Activate Budget
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
