import React, { useState, useEffect } from 'react';
import {
  CalendarClock,
  Plus,
  Play,
  Pause,
  Clock,
  Trash2,
  CheckCircle2,
  Calendar,
  AlertCircle,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { recurringApi } from '../api/recurring';
import { accountsApi } from '../api/accounts';
import { transactionsApi } from '../api/transactions';
import { Account, Category, RecurringTransaction, UpcomingReminder } from '../types';
import { formatINR, formatDate } from '../utils/formatters';
import { useToast } from '../contexts/ToastContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const RecurringPaymentsPage: React.FC = () => {
  const { showToast } = useToast();
  const [isLoading, setIsLoading] = useState(true);
  const [recurringList, setRecurringList] = useState<RecurringTransaction[]>([]);
  const [upcoming, setUpcoming] = useState<UpcomingReminder[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [createData, setCreateData] = useState({
    title: '',
    amount: '',
    transactionType: 'EXPENSE',
    frequency: 'MONTHLY',
    accountId: '',
    categoryId: '',
    startDate: new Date().toISOString().split('T')[0],
    description: '',
  });

  const fetchData = async () => {
    setIsLoading(true);
    try {
      const [recRes, upRes, accsRes, catsRes] = await Promise.all([
        recurringApi.getRecurringTransactions(),
        recurringApi.getUpcomingReminders(14),
        accountsApi.getAccounts(),
        transactionsApi.getCategories('EXPENSE'),
      ]);
      setRecurringList(recRes);
      setUpcoming(upRes);
      setAccounts(accsRes);
      setCategories(catsRes);
      if (accsRes.length > 0 && !createData.accountId) {
        setCreateData((prev) => ({ ...prev, accountId: String(accsRes[0].id) }));
      }
      if (catsRes.length > 0 && !createData.categoryId) {
        setCreateData((prev) => ({ ...prev, categoryId: String(catsRes[0].id) }));
      }
    } catch {
      showToast('Error loading recurring schedules', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleTogglePause = async (item: RecurringTransaction) => {
    try {
      if (item.isActive) {
        await recurringApi.pauseRecurring(item.id);
        showToast(`Paused "${item.title}"`, 'info');
      } else {
        await recurringApi.resumeRecurring(item.id);
        showToast(`Resumed "${item.title}"`, 'success');
      }
      fetchData();
    } catch {
      showToast('Failed to update status', 'error');
    }
  };

  const handleExecuteNow = async (id: number, title: string) => {
    try {
      await recurringApi.executeRecurring(id);
      showToast(`Triggered execution for "${title}"!`, 'success');
      fetchData();
    } catch {
      showToast('Execution failed', 'error');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await recurringApi.deleteRecurring(id);
      showToast('Recurring payment deleted', 'success');
      fetchData();
    } catch {
      showToast('Failed to delete schedule', 'error');
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createData.title || !createData.amount || !createData.accountId || !createData.categoryId) {
      showToast('Please fill all required schedule fields', 'error');
      return;
    }

    try {
      await recurringApi.createRecurring({
        title: createData.title,
        amount: createData.amount,
        transactionType: createData.transactionType,
        frequency: createData.frequency,
        accountId: Number(createData.accountId),
        categoryId: Number(createData.categoryId),
        startDate: createData.startDate,
        description: createData.description || undefined,
      });
      showToast('Recurring payment schedule created!', 'success');
      setIsCreateOpen(false);
      fetchData();
    } catch {
      showToast('Failed to create recurring rule', 'error');
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Checking automated recurring rules and bill dates..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Recurring Payments & Subscriptions</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Automate monthly SIPs, broadband bills, apartment rent, and streaming subscriptions with auto-post triggers.
          </p>
        </div>

        <Button variant="emerald" size="sm" onClick={() => setIsCreateOpen(true)} className="text-xs">
          <Plus className="w-4 h-4 mr-1.5" />
          Add Recurring Schedule
        </Button>
      </div>

      {/* Upcoming Reminders (Next 14 Days) */}
      <Card>
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Clock className="w-4 h-4 text-blue-600" />
            <CardTitle>Upcoming Payment Schedule (Next 14 Days)</CardTitle>
          </div>
          <Badge variant="blue" size="sm">
            {upcoming.length} Pending
          </Badge>
        </CardHeader>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {upcoming.map((u) => (
            <div
              key={u.id}
              className="p-3.5 rounded-xl border border-slate-200/80 bg-slate-50/60 flex flex-col justify-between space-y-3"
            >
              <div>
                <div className="flex items-start justify-between">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    {u.categoryName}
                  </span>
                  <span
                    className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                      u.daysRemaining <= 3 ? 'bg-rose-100 text-rose-700' : 'bg-blue-100 text-blue-700'
                    }`}
                  >
                    in {u.daysRemaining} days
                  </span>
                </div>
                <h4 className="text-sm font-bold text-slate-900 mt-1 truncate">{u.title}</h4>
                <p className="text-xs font-bold text-slate-800 mt-1">{formatINR(u.amount)}</p>
              </div>

              <div className="pt-2 border-t border-slate-200/60 flex items-center justify-between text-xs text-slate-500">
                <span>{u.accountName}</span>
                <button
                  onClick={() => handleExecuteNow(u.id, u.title)}
                  className="text-[11px] font-bold text-emerald-600 hover:text-emerald-700"
                >
                  Pay Now
                </button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Active Schedules Table */}
      <Card className="p-0 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <CardTitle>All Active Recurring Schedules</CardTitle>
          <span className="text-xs text-slate-500">{recurringList.length} Rules Active</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider text-[10px] font-semibold border-b border-slate-100">
              <tr>
                <th className="py-3 px-4">Title & Description</th>
                <th className="py-3 px-4">Frequency</th>
                <th className="py-3 px-4">Account</th>
                <th className="py-3 px-4">Next Due Date</th>
                <th className="py-3 px-4 text-right">Amount</th>
                <th className="py-3 px-4 text-center">Status</th>
                <th className="py-3 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {recurringList.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="py-3.5 px-4">
                    <p className="font-semibold text-slate-900">{item.title}</p>
                    {item.description && <p className="text-[11px] text-slate-400">{item.description}</p>}
                  </td>
                  <td className="py-3.5 px-4">
                    <Badge variant="slate" size="sm">
                      {item.frequency}
                    </Badge>
                  </td>
                  <td className="py-3.5 px-4 text-slate-600 font-medium">{item.accountName}</td>
                  <td className="py-3.5 px-4 text-slate-600 font-medium">
                    {formatDate(item.nextExecutionDate)}
                  </td>
                  <td className="py-3.5 px-4 text-right font-bold text-slate-900">{formatINR(item.amount)}</td>
                  <td className="py-3.5 px-4 text-center">
                    <Badge variant={item.isActive ? 'emerald' : 'slate'} size="sm">
                      {item.isActive ? 'Active' : 'Paused'}
                    </Badge>
                  </td>
                  <td className="py-3.5 px-4 text-center">
                    <div className="flex items-center justify-center space-x-1.5">
                      <button
                        onClick={() => handleTogglePause(item)}
                        title={item.isActive ? 'Pause rule' : 'Resume rule'}
                        className="p-1 rounded text-slate-500 hover:text-slate-900 hover:bg-slate-100"
                      >
                        {item.isActive ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
                      </button>
                      <button
                        onClick={() => handleExecuteNow(item.id, item.title)}
                        title="Execute Transaction Now"
                        className="p-1 rounded text-slate-500 hover:text-emerald-600 hover:bg-slate-100"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleDelete(item.id)}
                        title="Delete Schedule"
                        className="p-1 rounded text-slate-400 hover:text-rose-600 hover:bg-slate-100"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Add Recurring Rule Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="New Recurring Schedule" maxWidth="md">
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Schedule Title"
            required
            placeholder="e.g. Netflix 4K, Nifty 50 SIP, Flat Rent"
            value={createData.title}
            onChange={(e) => setCreateData({ ...createData, title: e.target.value })}
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Amount"
              type="number"
              step="0.01"
              prefixText="₹"
              required
              placeholder="999.00"
              value={createData.amount}
              onChange={(e) => setCreateData({ ...createData, amount: e.target.value })}
            />

            <Select
              label="Frequency"
              options={[
                { value: 'MONTHLY', label: 'Monthly' },
                { value: 'WEEKLY', label: 'Weekly' },
                { value: 'DAILY', label: 'Daily' },
                { value: 'QUARTERLY', label: 'Quarterly' },
                { value: 'YEARLY', label: 'Yearly' },
              ]}
              value={createData.frequency}
              onChange={(e) => setCreateData({ ...createData, frequency: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Select
              label="Payment Account"
              options={accounts.map((a) => ({ value: a.id, label: a.accountName }))}
              value={createData.accountId}
              onChange={(e) => setCreateData({ ...createData, accountId: e.target.value })}
            />

            <Select
              label="Category"
              options={categories.map((c) => ({ value: c.id, label: c.name }))}
              value={createData.categoryId}
              onChange={(e) => setCreateData({ ...createData, categoryId: e.target.value })}
            />
          </div>

          <Input
            label="Start Date"
            type="date"
            required
            value={createData.startDate}
            onChange={(e) => setCreateData({ ...createData, startDate: e.target.value })}
          />

          <Input
            label="Description (Optional)"
            placeholder="Details or notes about this recurring payment"
            value={createData.description}
            onChange={(e) => setCreateData({ ...createData, description: e.target.value })}
          />

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="emerald">
              Save Recurring Rule
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
