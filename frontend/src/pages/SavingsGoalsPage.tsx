import React, { useState, useEffect } from 'react';
import {
  Target,
  Plus,
  TrendingUp,
  Clock,
  CheckCircle2,
  Calendar,
  PiggyBank,
  History,
  Trash2,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { goalsApi } from '../api/goals';
import { accountsApi } from '../api/accounts';
import { Account, GoalContribution, SavingsGoal } from '../types';
import { formatINR, formatDate } from '../utils/formatters';
import { useToast } from '../contexts/ToastContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const SavingsGoalsPage: React.FC = () => {
  const { showToast } = useToast();
  const [isLoading, setIsLoading] = useState(true);
  const [goals, setGoals] = useState<SavingsGoal[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isContributeOpen, setIsContributeOpen] = useState(false);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [selectedGoal, setSelectedGoal] = useState<SavingsGoal | null>(null);
  const [contributions, setContributions] = useState<GoalContribution[]>([]);

  // Form states
  const [createData, setCreateData] = useState({
    title: '',
    description: '',
    targetAmount: '',
    targetDate: '2027-06-30',
    priority: 'HIGH',
  });

  const [contributeData, setContributeData] = useState({
    accountId: '',
    amount: '',
    note: '',
  });

  const fetchGoals = async () => {
    setIsLoading(true);
    try {
      const [goalsRes, accsRes] = await Promise.all([goalsApi.getGoals(), accountsApi.getAccounts()]);
      const safeGoals = Array.isArray(goalsRes) ? goalsRes : [];
      const safeAccs = Array.isArray(accsRes) ? accsRes : [];
      setGoals(safeGoals);
      setAccounts(safeAccs);
      if (safeAccs.length > 0) {
        setContributeData((prev) => ({ ...prev, accountId: String(safeAccs[0].id) }));
      }
    } catch {
      showToast('Error loading savings goals', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchGoals();
  }, []);

  const handleCreateGoal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createData.title || !createData.targetAmount || !createData.targetDate) {
      showToast('Please fill all required goal fields', 'error');
      return;
    }

    try {
      await goalsApi.createGoal({
        title: createData.title,
        description: createData.description || undefined,
        targetAmount: createData.targetAmount,
        targetDate: createData.targetDate,
        priority: createData.priority,
      });
      showToast('Savings goal created successfully!', 'success');
      setIsCreateOpen(false);
      setCreateData({ title: '', description: '', targetAmount: '', targetDate: '2027-06-30', priority: 'HIGH' });
      fetchGoals();
    } catch {
      showToast('Failed to create goal', 'error');
    }
  };

  const handleContribute = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedGoal || !contributeData.amount || !contributeData.accountId) {
      showToast('Please specify source account and contribution amount', 'error');
      return;
    }

    try {
      await goalsApi.contributeToGoal(selectedGoal.id, {
        accountId: Number(contributeData.accountId),
        amount: contributeData.amount,
        note: contributeData.note || undefined,
      });
      showToast(`Added ${formatINR(contributeData.amount)} to "${selectedGoal.title}"!`, 'success');
      setIsContributeOpen(false);
      setContributeData((prev) => ({ ...prev, amount: '', note: '' }));
      fetchGoals();
    } catch {
      showToast('Contribution failed', 'error');
    }
  };

  const handleViewHistory = async (goal: SavingsGoal) => {
    setSelectedGoal(goal);
    try {
      const hist = await goalsApi.getGoalContributions(goal.id);
      setContributions(hist);
      setIsHistoryOpen(true);
    } catch {
      showToast('Failed to load contributions history', 'error');
    }
  };

  const handleDeleteGoal = async (id: number) => {
    try {
      await goalsApi.deleteGoal(id);
      showToast('Savings goal removed', 'success');
      fetchGoals();
    } catch {
      showToast('Failed to delete goal', 'error');
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Calculating savings target milestones..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Savings Goals & Targets</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Set long-term milestone targets, allocate funds from liquid accounts, and visualize completion timelines.
          </p>
        </div>

        <Button variant="emerald" size="sm" onClick={() => setIsCreateOpen(true)} className="text-xs">
          <Plus className="w-4 h-4 mr-1.5" />
          Create New Goal
        </Button>
      </div>

      {/* Goals Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {goals.map((goal) => {
          const isCompleted = goal.completionPercentage >= 100;
          return (
            <Card key={goal.id} hoverEffect className="flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-start justify-between">
                  <div className="p-2.5 rounded-xl bg-blue-50 text-blue-600 border border-blue-100 mb-2">
                    <Target className="w-5 h-5" />
                  </div>
                  <Badge
                    variant={isCompleted ? 'emerald' : goal.priority === 'HIGH' ? 'rose' : 'blue'}
                    size="sm"
                  >
                    {isCompleted ? 'Completed' : `${goal.priority} Priority`}
                  </Badge>
                </div>

                <h3 className="text-base font-bold text-slate-900 tracking-tight">{goal.title}</h3>
                {goal.description && (
                  <p className="text-xs text-slate-500 mt-1 line-clamp-2">{goal.description}</p>
                )}

                {/* Progress Details */}
                <div className="mt-4 pt-3 border-t border-slate-100 space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-500">Saved so far</span>
                    <span className="font-bold text-slate-900">{formatINR(goal.currentAmount)}</span>
                  </div>
                  <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden p-0.5">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${
                        isCompleted ? 'bg-emerald-500' : 'bg-blue-600'
                      }`}
                      style={{ width: `${Math.min(100, goal.completionPercentage)}%` }}
                    />
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-slate-500">
                    <span>{goal.completionPercentage.toFixed(1)}% complete</span>
                    <span>Target: {formatINR(goal.targetAmount)}</span>
                  </div>
                </div>

                {/* Deadline & Remaining */}
                <div className="mt-4 p-2.5 rounded-lg bg-slate-50 border border-slate-200/60 flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-1.5 text-slate-500">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    <span>Target: {formatDate(goal.targetDate)}</span>
                  </div>
                  <span className="font-semibold text-slate-700">{goal.daysRemaining} days left</span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                <Button
                  size="sm"
                  variant="emerald"
                  onClick={() => {
                    setSelectedGoal(goal);
                    setIsContributeOpen(true);
                  }}
                  className="flex-1 text-xs"
                >
                  <PiggyBank className="w-3.5 h-3.5 mr-1" />
                  Contribute
                </Button>

                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => handleViewHistory(goal)}
                  className="p-2 text-slate-500"
                  title="View History"
                >
                  <History className="w-3.5 h-3.5" />
                </Button>

                <button
                  onClick={() => handleDeleteGoal(goal.id)}
                  className="p-2 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-slate-100 transition-colors"
                  title="Delete Goal"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Contribute Modal */}
      <Modal
        isOpen={isContributeOpen}
        onClose={() => setIsContributeOpen(false)}
        title={selectedGoal ? `Contribute to "${selectedGoal.title}"` : 'Contribute Funds'}
        maxWidth="md"
      >
        <form onSubmit={handleContribute} className="space-y-4">
          <Select
            label="Source Account"
            options={accounts.map((a) => ({
              value: a.id,
              label: `${a.accountName} (Balance: ${formatINR(a.currentBalance)})`,
            }))}
            value={contributeData.accountId}
            onChange={(e) => setContributeData({ ...contributeData, accountId: e.target.value })}
          />

          <Input
            label="Contribution Amount"
            type="number"
            step="0.01"
            prefixText="₹"
            required
            placeholder="10000.00"
            value={contributeData.amount}
            onChange={(e) => setContributeData({ ...contributeData, amount: e.target.value })}
            helperText={
              selectedGoal
                ? `Remaining target to complete goal: ${formatINR(selectedGoal.remainingAmount)}`
                : undefined
            }
          />

          <Input
            label="Contribution Note (Optional)"
            placeholder="e.g. Monthly SIP transfer, Bonus allocation"
            value={contributeData.note}
            onChange={(e) => setContributeData({ ...contributeData, note: e.target.value })}
          />

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={() => setIsContributeOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="emerald">
              Confirm Contribution
            </Button>
          </div>
        </form>
      </Modal>

      {/* History Modal */}
      <Modal
        isOpen={isHistoryOpen}
        onClose={() => setIsHistoryOpen(false)}
        title={selectedGoal ? `Contributions: ${selectedGoal.title}` : 'Contribution History'}
        maxWidth="md"
      >
        <div className="space-y-3">
          {contributions.length === 0 ? (
            <p className="text-xs text-slate-500 py-6 text-center">No contributions recorded yet.</p>
          ) : (
            contributions.map((c) => (
              <div key={c.id} className="p-3 rounded-lg border border-slate-100 bg-slate-50 flex items-center justify-between text-xs">
                <div>
                  <p className="font-semibold text-slate-800">{c.note || 'Contribution'}</p>
                  <p className="text-[11px] text-slate-400">
                    From {c.accountName} • {formatDate(c.contributionDate)}
                  </p>
                </div>
                <span className="font-bold text-emerald-600 text-sm">+{formatINR(c.amount)}</span>
              </div>
            ))
          )}
        </div>
      </Modal>

      {/* Create Goal Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create New Savings Goal" maxWidth="md">
        <form onSubmit={handleCreateGoal} className="space-y-4">
          <Input
            label="Goal Title"
            required
            placeholder="e.g. Emergency Fund, House Down Payment"
            value={createData.title}
            onChange={(e) => setCreateData({ ...createData, title: e.target.value })}
          />

          <Input
            label="Description (Optional)"
            placeholder="Why are you saving for this?"
            value={createData.description}
            onChange={(e) => setCreateData({ ...createData, description: e.target.value })}
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Target Amount"
              type="number"
              step="1000"
              prefixText="₹"
              required
              placeholder="300000"
              value={createData.targetAmount}
              onChange={(e) => setCreateData({ ...createData, targetAmount: e.target.value })}
            />

            <Input
              label="Target Date"
              type="date"
              required
              value={createData.targetDate}
              onChange={(e) => setCreateData({ ...createData, targetDate: e.target.value })}
            />
          </div>

          <Select
            label="Priority"
            options={[
              { value: 'HIGH', label: 'High Priority' },
              { value: 'MEDIUM', label: 'Medium Priority' },
              { value: 'LOW', label: 'Low Priority' },
            ]}
            value={createData.priority}
            onChange={(e) => setCreateData({ ...createData, priority: e.target.value })}
          />

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="emerald">
              Set Target
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
