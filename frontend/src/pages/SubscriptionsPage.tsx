import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Sparkles,
  CheckCircle2,
  Clock,
  Calendar,
  AlertCircle,
  TrendingDown,
  Info,
  Layers,
  ArrowRight,
  ShieldCheck,
  RefreshCw,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { Select } from '../components/common/Select';
import { EmptyState } from '../components/common/EmptyState';
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
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [scanMonths, setScanMonths] = useState(6);
  const [suggestions, setSuggestions] = useState<DetectedSubscription[]>([]);
  const [totalAnnual, setTotalAnnual] = useState('0.00');
  const [totalMonthly, setTotalMonthly] = useState('0.00');
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  // Confirmation modal
  const [selectedCandidate, setSelectedCandidate] = useState<DetectedSubscription | null>(null);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [confirmData, setConfirmData] = useState({
    accountId: '',
    categoryId: '',
    nextExecutionDate: new Date().toISOString().split('T')[0],
  });

  const fetchSubscriptions = async (months: number = scanMonths, silent: boolean = false) => {
    if (!silent) setIsLoading(true);
    else setIsRefreshing(true);

    try {
      const [subRes, accsRes, catsRes] = await Promise.all([
        subscriptionsApi.getDetectedSubscriptions(months),
        accountsApi.getAccounts(),
        transactionsApi.getCategories('EXPENSE'),
      ]);

      const safeSuggestions = Array.isArray(subRes?.suggestions) ? subRes.suggestions : [];
      setSuggestions(safeSuggestions);
      setTotalAnnual(subRes?.totalEstimatedAnnualCost || '0.00');
      setTotalMonthly(subRes?.totalEstimatedMonthlyCost || '0.00');

      const safeAccs = Array.isArray(accsRes) ? accsRes : [];
      const safeCats = Array.isArray(catsRes) ? catsRes : [];
      setAccounts(safeAccs);
      setCategories(safeCats);

      if (safeAccs.length > 0 && !confirmData.accountId) {
        setConfirmData((prev) => ({ ...prev, accountId: String(safeAccs[0].id) }));
      }
      if (safeCats.length > 0 && !confirmData.categoryId) {
        setConfirmData((prev) => ({ ...prev, categoryId: String(safeCats[0].id) }));
      }
    } catch (err) {
      console.error('Failed to detect subscriptions', err);
      showToast('Error analyzing recurring subscriptions', 'error');
      setSuggestions([]);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchSubscriptions(scanMonths);
  }, [scanMonths]);

  const handleOpenConfirm = (sub: DetectedSubscription) => {
    setSelectedCandidate(sub);
    const matchedAccount = accounts.find((a) => a.id === sub.accountId) || accounts[0];
    const matchedCategory = categories.find((c) => c.id === sub.categoryId) || categories[0];
    const nextDate = sub.suggestedNextPaymentDate || new Date().toISOString().split('T')[0];

    setConfirmData({
      accountId: matchedAccount ? String(matchedAccount.id) : accounts[0] ? String(accounts[0].id) : '',
      categoryId: matchedCategory ? String(matchedCategory.id) : categories[0] ? String(categories[0].id) : '',
      nextExecutionDate: nextDate,
    });
    setIsConfirmOpen(true);
  };

  const handleConfirm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCandidate) return;

    if (!confirmData.accountId) {
      showToast('Please select a payment account for this subscription', 'error');
      return;
    }
    if (!confirmData.categoryId) {
      showToast('Please select a category for this subscription', 'error');
      return;
    }

    setIsConfirming(true);
    try {
      await subscriptionsApi.confirmSubscription({
        candidateId: selectedCandidate.candidateId,
        subscriptionName: selectedCandidate.subscriptionName,
        amount: selectedCandidate.averagePayment,
        frequency: selectedCandidate.frequency,
        accountId: Number(confirmData.accountId),
        categoryId: Number(confirmData.categoryId),
        startDate: confirmData.nextExecutionDate,
        nextExecutionDate: confirmData.nextExecutionDate,
      });

      showToast(`Confirmed "${selectedCandidate.subscriptionName}" as an active recurring subscription!`, 'success');
      setIsConfirmOpen(false);
      fetchSubscriptions(scanMonths, true);
    } catch {
      showToast('Failed to confirm subscription', 'error');
    } finally {
      setIsConfirming(false);
    }
  };

  const handleDismiss = async (candidateId: string, name: string) => {
    try {
      await subscriptionsApi.dismissSubscription(candidateId);
      showToast(`Dismissed suggestion for "${name}"`, 'info');
      fetchSubscriptions(scanMonths, true);
    } catch {
      showToast('Failed to dismiss suggestion', 'error');
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Scanning transaction records & clustering recurring debit patterns..." />;
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
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">AI Subscription Detector</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Uncovers hidden recurring charges, streaming subscriptions, and auto-debits across your accounts.
          </p>
        </div>

        {/* Scan Window Filter & Refresh */}
        <div className="flex items-center space-x-2">
          <div className="flex rounded-lg bg-slate-100 p-1 text-xs font-semibold">
            {[3, 6, 12].map((m) => (
              <button
                key={m}
                onClick={() => setScanMonths(m)}
                className={`px-3 py-1 rounded-md transition-all ${
                  scanMonths === m
                    ? 'bg-white text-navy-900 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {m}M
              </button>
            ))}
          </div>
          <Button
            size="sm"
            variant="outline"
            onClick={() => fetchSubscriptions(scanMonths, true)}
            isLoading={isRefreshing}
            className="text-xs"
          >
            <RefreshCw className="w-3.5 h-3.5 mr-1 text-slate-500" />
            Rescan
          </Button>
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
          <p className="text-xs text-slate-500 mt-1">
            Across {suggestions.length} detected recurring service{suggestions.length === 1 ? '' : 's'} (approx. {formatINR(totalMonthly)}/month)
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <Badge variant={suggestions.length > 0 ? 'purple' : 'slate'} size="md">
            {suggestions.length} Potential Subscriptions Identified
          </Badge>
          <Link
            to="/recurring"
            className="text-xs font-semibold text-blue-600 hover:text-blue-800 inline-flex items-center"
          >
            View Active Rules <ArrowRight className="w-3.5 h-3.5 ml-1" />
          </Link>
        </div>
      </div>

      {/* Candidate Cards Grid or Empty State */}
      {suggestions.length === 0 ? (
        <div className="space-y-6">
          <EmptyState
            icon={Sparkles}
            title="No Subscriptions Detected Yet"
            description="The AI detector analyzes your debit transactions across Swiggy, Netflix, Spotify, gym memberships, and utility bills. When 2 or more debits with consistent amounts and intervals are logged, they will appear here as actionable subscription suggestions."
            actionText="View Transaction Ledger"
            onAction={() => (window.location.href = '/transactions')}
          />

          {/* Educational Explainer */}
          <Card>
            <CardHeader>
              <div className="flex items-center space-x-2">
                <Layers className="w-5 h-5 text-purple-600" />
                <CardTitle>How the AI Subscription Detector Works</CardTitle>
              </div>
            </CardHeader>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-1.5">
                <div className="font-bold text-slate-800 flex items-center space-x-1.5">
                  <span className="w-5 h-5 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-[11px]">1</span>
                  <span>Pattern Recognition</span>
                </div>
                <p className="text-slate-500">
                  Scans all past debit entries, clustering identical merchant tokens and description keywords over 3–12 month windows.
                </p>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-1.5">
                <div className="font-bold text-slate-800 flex items-center space-x-1.5">
                  <span className="w-5 h-5 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-[11px]">2</span>
                  <span>Cadence & Variance Engine</span>
                </div>
                <p className="text-slate-500">
                  Calculates average debit amounts and standard deviation of day intervals (e.g. 28–31 days for monthly) with up to 95% confidence.
                </p>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-1.5">
                <div className="font-bold text-slate-800 flex items-center space-x-1.5">
                  <span className="w-5 h-5 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-[11px]">3</span>
                  <span>One-Click Activation</span>
                </div>
                <p className="text-slate-500">
                  Review and confirm suggestions with custom account allocations. Confirmed items automatically sync with your recurring calendar and upcoming bill alerts.
                </p>
              </div>
            </div>
          </Card>
        </div>
      ) : (
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
                  Frequency: <span className="font-semibold text-slate-700">{sub.frequencyLabel || sub.frequency}</span> ({sub.occurrences} debit{sub.occurrences === 1 ? '' : 's'})
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

                {sub.lastPaymentDate && (
                  <div className="mt-3 p-2 rounded-lg bg-slate-50 border border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                    <span>Last Debit:</span>
                    <span className="font-semibold text-slate-700">{formatDate(sub.lastPaymentDate)}</span>
                  </div>
                )}

                {sub.suggestedNextPaymentDate && (
                  <div className="mt-1.5 p-2 rounded-lg bg-purple-50/50 border border-purple-100 flex items-center justify-between text-[11px] text-purple-700">
                    <span className="flex items-center">
                      <Calendar className="w-3 h-3 mr-1" />
                      Suggested Next:
                    </span>
                    <span className="font-bold">{formatDate(sub.suggestedNextPaymentDate)}</span>
                  </div>
                )}
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
      )}

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
            options={accounts.map((a) => ({
              value: a.id,
              label: `${a.accountName} (${a.accountType})`,
            }))}
            placeholder={accounts.length === 0 ? 'No accounts available' : 'Select payment account'}
            value={confirmData.accountId}
            onChange={(e) => setConfirmData({ ...confirmData, accountId: e.target.value })}
            required
          />

          <Select
            label="Category"
            options={categories.map((c) => ({ value: c.id, label: c.name }))}
            placeholder={categories.length === 0 ? 'No categories available' : 'Select category'}
            value={confirmData.categoryId}
            onChange={(e) => setConfirmData({ ...confirmData, categoryId: e.target.value })}
            required
          />

          <Input
            label="Next Scheduled Execution Date"
            type="date"
            required
            value={confirmData.nextExecutionDate}
            onChange={(e) => setConfirmData({ ...confirmData, nextExecutionDate: e.target.value })}
          />

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={() => setIsConfirmOpen(false)} disabled={isConfirming}>
              Cancel
            </Button>
            <Button type="submit" variant="emerald" isLoading={isConfirming}>
              Activate Subscription Rule
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
