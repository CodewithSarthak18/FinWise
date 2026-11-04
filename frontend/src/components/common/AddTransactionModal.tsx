import React, { useState, useEffect } from 'react';
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

  useEffect(() => {
    if (isOpen) {
      Promise.all([accountsApi.getAccounts(), transactionsApi.getCategories()]).then(
        ([accs, cats]) => {
          setAccounts(accs);
          setCategories(cats);
          if (accs.length > 0 && !formData.accountId) {
            setFormData((prev) => ({ ...prev, accountId: String(accs[0].id) }));
          }
          if (cats.length > 0 && !formData.categoryId) {
            const defaultCat = cats.find((c) => c.type === 'EXPENSE') || cats[0];
            setFormData((prev) => ({ ...prev, categoryId: String(defaultCat.id) }));
          }
        }
      );
    }
  }, [isOpen]);

  const filteredCategories = categories.filter((c) => c.type === formData.transactionType);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.accountId || !formData.categoryId || !formData.amount) {
      showToast('Please fill in required fields', 'error');
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
      showToast('Transaction recorded successfully!', 'success');
      onSuccess?.();
      onClose();
    } catch {
      showToast('Failed to record transaction', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Record New Transaction" maxWidth="md">
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Type Toggle */}
        <div className="flex rounded-lg bg-slate-100 p-1">
          <button
            type="button"
            onClick={() => {
              setFormData((prev) => ({ ...prev, transactionType: 'EXPENSE' }));
              const firstExp = categories.find((c) => c.type === 'EXPENSE');
              if (firstExp) setFormData((prev) => ({ ...prev, categoryId: String(firstExp.id) }));
            }}
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
            onClick={() => {
              setFormData((prev) => ({ ...prev, transactionType: 'INCOME' }));
              const firstInc = categories.find((c) => c.type === 'INCOME');
              if (firstInc) setFormData((prev) => ({ ...prev, categoryId: String(firstInc.id) }));
            }}
            className={`flex-1 py-1.5 text-xs font-semibold rounded-md transition-all ${
              formData.transactionType === 'INCOME'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Income
          </button>
        </div>

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
          <Select
            label="Account"
            options={accounts.map((a) => ({
              value: a.id,
              label: `${a.accountName} (${a.accountType})`,
            }))}
            value={formData.accountId}
            onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
          />

          <Select
            label="Category"
            options={filteredCategories.map((c) => ({
              value: c.id,
              label: c.name,
            }))}
            value={formData.categoryId}
            onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
          />
        </div>

        {/* Merchant & Date */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="Merchant / Payee"
            placeholder="e.g. Swiggy, Netflix, Salary"
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
          <Button type="submit" variant="emerald" isLoading={isLoading}>
            Save Transaction
          </Button>
        </div>
      </form>
    </Modal>
  );
};
