import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  CheckCircle2,
  XCircle,
  Clock,
  Calendar,
  AlertCircle,
  TrendingDown,
  Info,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { Select } from '../components/common/Select';
import { subscriptionsApi } from '../api/subscriptions';
import { accountsApi } from '../api/accounts';
import { transactionsApi } from '../api/transactions';
import { Account, Category, DetectedSubscription } from '../types';
import { formatINR, formatDate } from '../utils/formatters';
import { useToast } from '../contexts/ToastContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const SubscriptionsPage: React.FC = () => {
  const { showToast } = useToast();
  const [isLoading, setIsLoading] = useState(true);
  const [suggestions, setSuggestions] = useState<DetectedSubscription[]>([]);
  const [totalAnnual, setTotalAnnual] = useState('0.00');
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  // Confirmation modal
  const [selectedCandidate, setSelectedCandidate] = useState<DetectedSubscription | null>(null);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [confirmData, setConfirmData] = useState({
    accountId: '',
    categoryId: '',
    nextExecutionDate: new Date().toISOString().split('T')[0],
  });

  const fetchSubscriptions = async () => {
    setIsLoading(true);
    try {
      const [subRes, accsRes, catsRes] = await Promise.all([
        subscriptionsApi.getDetectedSubscriptions(6),
        accountsApi.getAccounts(),
        transactionsApi.getCategories('EXPENSE'),
      ]);
      setSuggestions(subRes.suggestions);
      setTotalAnnual(subRes.totalEstimatedAnnualCost);
      setAccounts(accsRes);
      setCategories(catsRes);
      if (accsRes.length > 0) setConfirmData((prev) => ({ ...prev, accountId: String(accsRes[0].id) }));
      if (catsRes.length > 0) setConfirmData((prev) => ({ ...prev, categoryId: String(catsRes[0].id) }));
    } catch {
      showToast('Error detecting subscriptions', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscriptions();
  }, []);

  const handleOpenConfirm = (sub: DetectedSubscription) => {
    setSelectedCandidate(sub);
    setIsConfirmOpen(true);
  };

  const handleConfirm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCandidate || !confirmData.accountId || !confirmData.categoryId) return;

    try {
      await subscriptionsApi.confirmSubscription({
        candidateId: selectedCandidate.candidateId,
        subscriptionName: selectedCandidate.subscriptionName,
        amount: selectedCandidate.averagePayment,
        frequency: selectedCandidate.frequency,
        accountId: Number(confirmData.accountId),
        categoryId: Number(confirmData.categoryId),
        nextExecutionDate: confirmData.nextExecutionDate,
      });
      showToast(`Confirmed "${selectedCandidate.subscriptionName}" as an active recurring payment!`, 'success');
      setIsConfirmOpen(false);
      fetchSubscriptions();
    } catch {
      showToast('Failed to confirm subscription', 'error');
    }
  };

  const handleDismiss = async (candidateId: string, name: string) => {
    try {
      await subscriptionsApi.dismissSubscription(candidateId);
      showToast(`Dismissed suggestion for "${name}"`, 'info');
      fetchSubscriptions();
    } catch {
      showToast('Failed to dismiss suggestion', 'error');
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Scanning expense patterns & clustering recurring intervals..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-purple-50 text-purple-700 border border-purple-200 mb-1.5">
            <Sparkles className="w-3.5 h-3.5 mr-1" />
            Machine Learning Recurring Pattern Detection
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Subscription Detector</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Analyzes transaction descriptions, payment variances, and date intervals to uncover hidden recurring costs.
          </p>
        </div>
      </div>

      {/* Suggestion Notice */}
      <div className="p-3.5 rounded-xl bg-blue-50/70 border border-blue-200 text-xs text-blue-800 flex items-start space-x-2.5">
        <Info className="w-4 h-4 text-blue-600 mt-0.5 flex-shrink-0" />
        <div>
          <span className="font-bold">Detected Subscriptions are Suggestions:</span>
          <span>
            {' '}FinWise highlights recurring charges as suggestions until confirmed by you. Confirming a subscription
            integrates it into your scheduled Recurring Payments engine.
          </span>
        </div>
      </div>

      {/* Aggregate Annual Impact Banner */}
      <div className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-[10px] uppercase font-bold text-slate-400 block">Total Estimated Annual Cost</span>
          <h2 className="text-2xl sm:text-3xl font-black text-slate-900 mt-0.5">{formatINR(totalAnnual)} / year</h2>
          <p className="text-xs text-slate-500 mt-1">Across {suggestions.length} detected recurring services</p>
        </div>
        <Badge variant="purple" size="md">
          {suggestions.length} Potential Subscriptions Identified
        </Badge>
      </div>

      {/* Candidate Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {suggestions.map((sub) => (
          <Card key={sub.candidateId} hoverEffect className="flex flex-col justify-between space-y-4">
            <div>
              <div className="flex items-start justify-between">
                <Badge variant="purple" size="sm">
                  {sub.confidenceScore}% Confidence
                </Badge>
                <Badge variant="slate" size="sm">
                  {sub.status}
                </Badge>
              </div>

              <h3 className="text-base font-bold text-slate-900 mt-3">{sub.subscriptionName}</h3>
              <p className="text-xs text-slate-500">
                Frequency: <span className="font-semibold text-slate-700">{sub.frequency}</span> ({sub.occurrences} debits)
              </p>

              <div className="mt-4 pt-3 border-t border-slate-100 grid grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="text-[10px] text-slate-400 uppercase font-bold block">Avg Payment</span>
                  <span className="font-bold text-slate-900">{formatINR(sub.averagePayment)}</span>
                </div>
                <div className="text-right">
                  <span className="text-[10px] text-slate-400 uppercase font-bold block">Annual Cost</span>
                  <span className="font-bold text-rose-600">{formatINR(sub.estimatedAnnualCost)}</span>
                </div>
              </div>

              <div className="mt-3 p-2 rounded-lg bg-slate-50 border border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                <span>Last Debit:</span>
                <span className="font-semibold text-slate-700">{formatDate(sub.lastPaymentDate)}</span>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
              <Button
                size="sm"
                variant="emerald"
                onClick={() => handleOpenConfirm(sub)}
                className="flex-1 text-xs"
              >
                <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                Confirm
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleDismiss(sub.candidateId, sub.subscriptionName)}
                className="text-xs text-slate-500 hover:text-slate-700"
              >
                Dismiss
              </Button>
            </div>
          </Card>
        ))}
      </div>

      {/* Confirmation Modal */}
      <Modal
        isOpen={isConfirmOpen}
        onClose={() => setIsConfirmOpen(false)}
        title={selectedCandidate ? `Confirm Subscription: ${selectedCandidate.subscriptionName}` : 'Confirm Subscription'}
        maxWidth="md"
      >
        <form onSubmit={handleConfirm} className="space-y-4">
          <p className="text-xs text-slate-600">
            Confirming will create an automated recurring rule for{' '}
            <span className="font-bold">{formatINR(selectedCandidate?.averagePayment)}</span> with{' '}
            <span className="font-bold">{selectedCandidate?.frequency}</span> auto-post tracking.
          </p>

          <Select
            label="Payment Account"
            options={accounts.map((a) => ({ value: a.id, label: a.accountName }))}
            value={confirmData.accountId}
            onChange={(e) => setConfirmData({ ...confirmData, accountId: e.target.value })}
          />

          <Select
            label="Category"
            options={categories.map((c) => ({ value: c.id, label: c.name }))}
            value={confirmData.categoryId}
            onChange={(e) => setConfirmData({ ...confirmData, categoryId: e.target.value })}
          />

          <Input
            label="Next Scheduled Execution Date"
            type="date"
            required
            value={confirmData.nextExecutionDate}
            onChange={(e) => setConfirmData({ ...confirmData, nextExecutionDate: e.target.value })}
          />

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={() => setIsConfirmOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="emerald">
              Activate Subscription Rule
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
