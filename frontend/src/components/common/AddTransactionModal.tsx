import React, { useState, useEffect } from 'react';
import { AlertCircle } from 'lucide-react';
import { Modal } from './Modal';
import { Input } from './Input';
import { Select } from './Select';
import { Button } from './Button';
import { transactionsApi } from '../../api/transactions';
import { accountsApi } from '../../api/accounts';
import { Account, Category } from '../../types';
import { useToast } from '../../contexts/ToastContext';

interface AddTransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const AddTransactionModal: React.FC<AddTransactionModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const { showToast } = useToast();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isFetching, setIsFetching] = useState(false);

  // Quick account creation form state
  const [showAccountForm, setShowAccountForm] = useState(false);
  const [isSavingAccount, setIsSavingAccount] = useState(false);
  const [quickAccount, setQuickAccount] = useState({
    accountName: '',
    accountType: 'SAVINGS',
    initialBalance: '0.00',
  });

  const [formData, setFormData] = useState({
    transactionType: 'EXPENSE',
    accountId: '',
    categoryId: '',
    amount: '',
    transactionDate: new Date().toISOString().split('T')[0],
    merchantName: '',
    description: '',
    paymentMode: 'UPI',
    referenceNumber: '',
  });

  const fetchDependencies = async () => {
    setIsFetching(true);
    try {
      const [accs, cats] = await Promise.all([
        accountsApi.getAccounts(),
        transactionsApi.getCategories(),
      ]);
      const safeAccs = Array.isArray(accs) ? accs : [];
      const safeCats = Array.isArray(cats) ? cats : [];

      setAccounts(safeAccs);
      setCategories(safeCats);

      setFormData((prev) => {
        const currentType = prev.transactionType || 'EXPENSE';
        const filteredCats = safeCats.filter((c) => c.type === currentType);
        const defaultCat = filteredCats[0] || safeCats[0];
        const activeAcc = safeAccs.find((a) => String(a.id) === prev.accountId) || safeAccs[0];

        return {
          ...prev,
          accountId: activeAcc ? String(activeAcc.id) : '',
          categoryId: prev.categoryId && safeCats.some((c) => String(c.id) === prev.categoryId)
            ? prev.categoryId
            : defaultCat ? String(defaultCat.id) : '',
        };
      });
    } catch (err) {
      console.error('Failed to load accounts and categories', err);
    } finally {
      setIsFetching(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchDependencies();
    } else {
      setShowAccountForm(false);
    }
  }, [isOpen]);

  const handleTypeToggle = (type: 'EXPENSE' | 'INCOME') => {
    const filteredCats = categories.filter((c) => c.type === type);
    const firstCat = filteredCats[0];
    setFormData((prev) => ({
      ...prev,
      transactionType: type,
      categoryId: firstCat ? String(firstCat.id) : prev.categoryId,
      accountId: prev.accountId || (accounts[0] ? String(accounts[0].id) : ''),
    }));
  };

  const handleQuickCreateAccount = async () => {
    if (!quickAccount.accountName.trim()) {
      showToast('Please enter an account name', 'error');
      return;
    }

    setIsSavingAccount(true);
    try {
      const newAcc = await accountsApi.createAccount({
        accountName: quickAccount.accountName.trim(),
        accountType: quickAccount.accountType,
        initialBalance: quickAccount.initialBalance || '0.00',
      });

      setAccounts((prev) => [...prev, newAcc]);
      setFormData((prev) => ({ ...prev, accountId: String(newAcc.id) }));
      setShowAccountForm(false);
      setQuickAccount({
        accountName: '',
        accountType: 'SAVINGS',
        initialBalance: '0.00',
      });
      showToast(`Account "${newAcc.accountName}" created and selected!`, 'success');
    } catch {
      showToast('Failed to create account. Please check inputs.', 'error');
    } finally {
      setIsSavingAccount(false);
    }
  };

  const filteredCategories = categories.filter((c) => c.type === formData.transactionType);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.accountId) {
      showToast('Please select or create an account for this transaction', 'error');
      return;
    }
    if (!formData.categoryId) {
      showToast('Please select a category for this transaction', 'error');
      return;
    }
    if (!formData.amount || parseFloat(formData.amount) <= 0) {
      showToast('Please enter a valid transaction amount', 'error');
      return;
    }

    setIsLoading(true);
    try {
      await transactionsApi.createTransaction({
        accountId: Number(formData.accountId),
        categoryId: Number(formData.categoryId),
        transactionType: formData.transactionType,
        amount: formData.amount,
        transactionDate: `${formData.transactionDate}T12:00:00Z`,
        merchantName: formData.merchantName || undefined,
        description: formData.description || undefined,
        paymentMode: formData.paymentMode,
        referenceNumber: formData.referenceNumber || undefined,
      });

      showToast(
        `${formData.transactionType === 'INCOME' ? 'Income' : 'Expense'} recorded successfully!`,
        'success'
      );
      onSuccess?.();
      onClose();
    } catch {
      showToast('Failed to record transaction', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={formData.transactionType === 'INCOME' ? 'Record New Income' : 'Record New Expense'}
      maxWidth="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Type Toggle */}
        <div className="flex rounded-lg bg-slate-100 p-1">
          <button
            type="button"
            onClick={() => handleTypeToggle('EXPENSE')}
            className={`flex-1 py-1.5 text-xs font-semibold rounded-md transition-all ${
              formData.transactionType === 'EXPENSE'
                ? 'bg-rose-600 text-white shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Expense
          </button>
          <button
            type="button"
            onClick={() => handleTypeToggle('INCOME')}
            className={`flex-1 py-1.5 text-xs font-semibold rounded-md transition-all ${
              formData.transactionType === 'INCOME'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Income
          </button>
        </div>

        {/* Empty Account Banner */}
        {accounts.length === 0 && !isFetching && (
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-900 space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-1.5">
                <AlertCircle className="w-4 h-4 text-amber-600 flex-shrink-0" />
                <span className="font-bold">No Account Found</span>
              </div>
              {!showAccountForm && (
                <button
                  type="button"
                  onClick={() => setShowAccountForm(true)}
                  className="px-2 py-1 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white rounded-md transition-colors"
                >
                  + Add Account
                </button>
              )}
            </div>
            <p className="text-amber-700">
              You need an account (Bank, Cash, or UPI Wallet) to credit income or debit expenses.
            </p>
          </div>
        )}

        {/* Quick Add Account Form */}
        {showAccountForm && (
          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-800">
                Quick Link New Account
              </span>
              <button
                type="button"
                onClick={() => setShowAccountForm(false)}
                className="text-xs text-slate-500 hover:text-slate-700 font-medium"
              >
                Cancel
              </button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              <Input
                label="Account Name"
                placeholder="e.g. HDFC Salary, Cash"
                value={quickAccount.accountName}
                onChange={(e) => setQuickAccount({ ...quickAccount, accountName: e.target.value })}
                required
              />
              <Select
                label="Type"
                options={[
                  { value: 'SAVINGS', label: 'Savings Account' },
                  { value: 'CURRENT', label: 'Current Account' },
                  { value: 'WALLET_UPI', label: 'Wallet / UPI' },
                  { value: 'CASH', label: 'Cash in Hand' },
                  { value: 'CREDIT_CARD', label: 'Credit Card' },
                  { value: 'INVESTMENT', label: 'Investment' },
                ]}
                value={quickAccount.accountType}
                onChange={(e) => setQuickAccount({ ...quickAccount, accountType: e.target.value })}
              />
              <Input
                label="Opening Balance"
                placeholder="0.00"
                prefixText="₹"
                type="number"
                step="0.01"
                value={quickAccount.initialBalance}
                onChange={(e) => setQuickAccount({ ...quickAccount, initialBalance: e.target.value })}
              />
            </div>
            <div className="flex justify-end pt-1">
              <Button
                type="button"
                size="sm"
                variant="emerald"
                isLoading={isSavingAccount}
                onClick={handleQuickCreateAccount}
              >
                Create & Link Account
              </Button>
            </div>
          </div>
        )}

        {/* Amount */}
        <Input
          label="Amount"
          type="number"
          step="0.01"
          placeholder="0.00"
          prefixText="₹"
          required
          value={formData.amount}
          onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
        />

        {/* Account and Category Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Account
              </label>
              {!showAccountForm && (
                <button
                  type="button"
                  onClick={() => setShowAccountForm(true)}
                  className="text-[11px] font-semibold text-emerald-600 hover:text-emerald-700"
                >
                  + Add Account
                </button>
              )}
            </div>
            <Select
              options={accounts.map((a) => ({
                value: a.id,
                label: `${a.accountName} (${a.accountType})`,
              }))}
              placeholder={accounts.length === 0 ? 'No accounts - add one first' : 'Select an account'}
              value={formData.accountId}
              onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
              required
            />
          </div>

          <div>
            <div className="mb-1">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600">
                Category
              </label>
            </div>
            <Select
              options={filteredCategories.map((c) => ({
                value: c.id,
                label: c.name,
              }))}
              placeholder="Select a category"
              value={formData.categoryId}
              onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
              required
            />
          </div>
        </div>

        {/* Merchant & Date */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label={formData.transactionType === 'INCOME' ? 'Source / Payer' : 'Merchant / Payee'}
            placeholder={
              formData.transactionType === 'INCOME'
                ? 'e.g. Employer, Client, Tenant'
                : 'e.g. Swiggy, Netflix, Amazon'
            }
            value={formData.merchantName}
            onChange={(e) => setFormData({ ...formData, merchantName: e.target.value })}
          />

          <Input
            label="Transaction Date"
            type="date"
            required
            value={formData.transactionDate}
            onChange={(e) => setFormData({ ...formData, transactionDate: e.target.value })}
          />
        </div>

        {/* Payment Mode */}
        <Select
          label="Payment Method"
          options={[
            { value: 'UPI', label: 'UPI (GPay, PhonePe, Paytm)' },
            { value: 'NET_BANKING', label: 'Net Banking' },
            { value: 'CREDIT_CARD', label: 'Credit Card' },
            { value: 'DEBIT_CARD', label: 'Debit Card' },
            { value: 'NEFT_RTGS_IMPS', label: 'NEFT / RTGS / IMPS' },
            { value: 'CASH', label: 'Cash' },
            { value: 'OTHER', label: 'Other' },
          ]}
          value={formData.paymentMode}
          onChange={(e) => setFormData({ ...formData, paymentMode: e.target.value })}
        />

        {/* Description */}
        <Input
          label="Description / Note (Optional)"
          placeholder="What was this transaction for?"
          value={formData.description}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
        />

        {/* Submit Buttons */}
        <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
          <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="emerald"
            isLoading={isLoading}
            disabled={accounts.length === 0}
          >
            Save Transaction
          </Button>
        </div>
      </form>
    </Modal>
  );
};
