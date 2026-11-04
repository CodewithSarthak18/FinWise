import React, { useState, useEffect } from 'react';
import {
  Wallet,
  Plus,
  CreditCard,
  Building2,
  TrendingUp,
  Banknote,
  ArrowRightLeft,
  Trash2,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { accountsApi } from '../api/accounts';
import { Account } from '../types';
import { formatINR } from '../utils/formatters';
import { useToast } from '../contexts/ToastContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const AccountsPage: React.FC = () => {
  const { showToast } = useToast();
  const [isLoading, setIsLoading] = useState(true);
  const [accounts, setAccounts] = useState<Account[]>([]);

  // Create Modal
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [createData, setCreateData] = useState({
    accountName: '',
    accountType: 'SAVINGS',
    institutionName: '',
    accountNumberMask: '',
    initialBalance: '',
    creditLimit: '0.00',
  });

  const fetchAccounts = async () => {
    setIsLoading(true);
    try {
      const data = await accountsApi.getAccounts();
      setAccounts(data);
    } catch {
      showToast('Error loading accounts', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, []);

  const getAccountIcon = (type: string) => {
    switch (type) {
      case 'CREDIT_CARD':
        return CreditCard;
      case 'INVESTMENT':
        return TrendingUp;
      case 'WALLET_UPI':
      case 'CASH':
        return Banknote;
      default:
        return Building2;
    }
  };

  const handleCreateAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createData.accountName || !createData.initialBalance) {
      showToast('Please specify account name and opening balance', 'error');
      return;
    }

    try {
      await accountsApi.createAccount({
        accountName: createData.accountName,
        accountType: createData.accountType,
        institutionName: createData.institutionName || undefined,
        accountNumberMask: createData.accountNumberMask || undefined,
        initialBalance: createData.initialBalance,
        creditLimit: createData.creditLimit,
      });
      showToast('New account added successfully!', 'success');
      setIsCreateOpen(false);
      setCreateData({
        accountName: '',
        accountType: 'SAVINGS',
        institutionName: '',
        accountNumberMask: '',
        initialBalance: '',
        creditLimit: '0.00',
      });
      fetchAccounts();
    } catch {
      showToast('Failed to create account', 'error');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await accountsApi.deleteAccount(id);
      showToast('Account archived successfully', 'success');
      fetchAccounts();
    } catch {
      showToast('Failed to archive account', 'error');
    }
  };

  const totalBalance = accounts.reduce((sum, a) => sum + parseFloat(a.currentBalance), 0);

  if (isLoading) {
    return <LoadingSpinner message="Aggregating multi-institution account balances..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Financial Accounts & Wallets</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Manage multi-bank savings accounts, credit cards, demat portfolios, and digital UPI wallets.
          </p>
        </div>

        <Button variant="emerald" size="sm" onClick={() => setIsCreateOpen(true)} className="text-xs">
          <Plus className="w-4 h-4 mr-1.5" />
          Add Account
        </Button>
      </div>

      {/* Aggregate Balance Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-navy-950 via-navy-900 to-slate-900 text-white shadow-md flex flex-col sm:flex-row sm:items-center justify-between gap-4 border border-navy-800">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 block">
            Consolidated Liquid Net Worth
          </span>
          <h2 className="text-3xl font-extrabold tracking-tight mt-1 text-white">
            {formatINR(totalBalance)}
          </h2>
          <p className="text-xs text-slate-400 mt-1">Across {accounts.length} active financial accounts</p>
        </div>

        <div className="flex items-center space-x-3">
          <Badge variant="emerald" size="md">
            All Balances Reconciled
          </Badge>
        </div>
      </div>

      {/* Account Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {accounts.map((acc) => {
          const Icon = getAccountIcon(acc.accountType);
          const isCredit = acc.accountType === 'CREDIT_CARD';

          return (
            <Card key={acc.id} hoverEffect className="flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-start justify-between">
                  <div className="p-2.5 rounded-xl bg-slate-100 text-slate-700">
                    <Icon className="w-5 h-5" />
                  </div>
                  <Badge variant={acc.status === 'ACTIVE' ? 'emerald' : 'slate'} size="sm">
                    {acc.accountType}
                  </Badge>
                </div>

                <h3 className="text-base font-bold text-slate-900 mt-3">{acc.accountName}</h3>
                <p className="text-xs text-slate-500">
                  {acc.institutionName || 'Self Managed'} • {acc.accountNumberMask || '••••'}
                </p>

                <div className="mt-4 pt-3 border-t border-slate-100">
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">
                    {isCredit ? 'Outstanding Due' : 'Available Balance'}
                  </span>
                  <span
                    className={`text-2xl font-black tracking-tight ${
                      isCredit ? 'text-rose-600' : 'text-slate-900'
                    }`}
                  >
                    {formatINR(acc.currentBalance)}
                  </span>
                  {isCredit && parseFloat(acc.creditLimit || '0') > 0 && (
                    <p className="text-[11px] text-slate-400 mt-0.5">
                      Credit Limit: {formatINR(acc.creditLimit)}
                    </p>
                  )}
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                <span className="text-[11px] text-slate-400 font-mono">Currency: INR (₹)</span>
                <button
                  onClick={() => handleDelete(acc.id)}
                  title="Archive account"
                  className="p-1 rounded text-slate-400 hover:text-rose-600 hover:bg-slate-100 transition-colors"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Add Account Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Link New Account" maxWidth="md">
        <form onSubmit={handleCreateAccount} className="space-y-4">
          <Input
            label="Account Display Name"
            required
            placeholder="e.g. HDFC Salary, Zerodha Demat"
            value={createData.accountName}
            onChange={(e) => setCreateData({ ...createData, accountName: e.target.value })}
          />

          <div className="grid grid-cols-2 gap-3">
            <Select
              label="Account Type"
              options={[
                { value: 'SAVINGS', label: 'Savings Bank' },
                { value: 'CURRENT', label: 'Current Account' },
                { value: 'CREDIT_CARD', label: 'Credit Card' },
                { value: 'INVESTMENT', label: 'Demat / Stocks / Mutual Funds' },
                { value: 'WALLET_UPI', label: 'Digital Wallet / UPI' },
                { value: 'CASH', label: 'Physical Cash' },
              ]}
              value={createData.accountType}
              onChange={(e) => setCreateData({ ...createData, accountType: e.target.value })}
            />

            <Input
              label="Bank / Institution Name"
              placeholder="e.g. HDFC, ICICI, Zerodha"
              value={createData.institutionName}
              onChange={(e) => setCreateData({ ...createData, institutionName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Opening Balance"
              type="number"
              step="0.01"
              prefixText="₹"
              required
              placeholder="50000.00"
              value={createData.initialBalance}
              onChange={(e) => setCreateData({ ...createData, initialBalance: e.target.value })}
            />

            <Input
              label="Account Number (Masked)"
              placeholder="XXXX-XXXX-1234"
              value={createData.accountNumberMask}
              onChange={(e) => setCreateData({ ...createData, accountNumberMask: e.target.value })}
            />
          </div>

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="emerald">
              Create Account
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
