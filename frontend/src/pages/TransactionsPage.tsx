import React, { useState, useEffect } from 'react';
import {
  Search,
  Filter,
  Plus,
  ArrowUpDown,
  Download,
  Trash2,
  Edit2,
  ChevronLeft,
  ChevronRight,
  Receipt,
  RotateCcw,
  X,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { transactionsApi } from '../api/transactions';
import { accountsApi } from '../api/accounts';
import { Account, Category, Transaction } from '../types';
import { formatINR, formatDate } from '../utils/formatters';
import { useToast } from '../contexts/ToastContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const TransactionsPage: React.FC = () => {
  const { showToast } = useToast();
  const [isLoading, setIsLoading] = useState(true);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);

  // Pagination & Filtering state
  const [search, setSearch] = useState('');
  const [selectedAccount, setSelectedAccount] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedType, setSelectedType] = useState('');
  const [sortField, setSortField] = useState('transactionDate');
  const [sortOrder, setSortOrder] = useState<'desc' | 'asc'>('desc');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modals
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isRefundModalOpen, setIsRefundModalOpen] = useState(false);
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReason, setRefundReason] = useState('');
  const [isRefunding, setIsRefunding] = useState(false);

  // Edit form state
  const [editFormData, setEditFormData] = useState({
    accountId: '',
    categoryId: '',
    transactionType: 'EXPENSE',
    amount: '',
    transactionDate: '',
    merchantName: '',
    description: '',
    paymentMode: 'UPI',
  });

  const fetchTransactions = async () => {
    setIsLoading(true);
    try {
      const response = await transactionsApi.getTransactions({
        search: search || undefined,
        accountId: selectedAccount ? Number(selectedAccount) : undefined,
        categoryId: selectedCategory ? Number(selectedCategory) : undefined,
        transactionType: selectedType || undefined,
        page,
        size: 15,
        sort: `${sortField},${sortOrder}`,
      });
      setTransactions(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch {
      showToast('Error loading transactions', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    Promise.all([transactionsApi.getCategories(), accountsApi.getAccounts()]).then(
      ([cats, accs]) => {
        setCategories(cats);
        setAccounts(accs);
      }
    );
  }, []);

  useEffect(() => {
    fetchTransactions();
  }, [search, selectedAccount, selectedCategory, selectedType, sortField, sortOrder, page]);

  const handleOpenEdit = (tx: Transaction) => {
    setSelectedTx(tx);
    setEditFormData({
      accountId: String(tx.accountId),
      categoryId: String(tx.categoryId),
      transactionType: tx.transactionType,
      amount: tx.amount,
      transactionDate: tx.transactionDate.split('T')[0],
      merchantName: tx.merchantName || '',
      description: tx.description || '',
      paymentMode: tx.paymentMode,
    });
    setIsEditModalOpen(true);
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTx) return;

    try {
      await transactionsApi.updateTransaction(selectedTx.id, {
        accountId: Number(editFormData.accountId),
        categoryId: Number(editFormData.categoryId),
        transactionType: editFormData.transactionType,
        amount: editFormData.amount,
        transactionDate: `${editFormData.transactionDate}T12:00:00Z`,
        merchantName: editFormData.merchantName,
        description: editFormData.description,
        paymentMode: editFormData.paymentMode,
      });
      showToast('Transaction updated successfully', 'success');
      setIsEditModalOpen(false);
      fetchTransactions();
    } catch {
      showToast('Failed to update transaction', 'error');
    }
  };

  const handleDelete = async () => {
    if (!selectedTx) return;
    try {
      await transactionsApi.deleteTransaction(selectedTx.id);
      showToast('Transaction deleted successfully', 'success');
      setIsDeleteModalOpen(false);
      fetchTransactions();
    } catch {
      showToast('Failed to delete transaction', 'error');
    }
  };

  const handleRefund = async () => {
    if (!selectedTx) return;
    try {
      setIsRefunding(true);
      await transactionsApi.refundTransaction(selectedTx.id, {
        amount: refundAmount ? refundAmount : undefined,
        reason: refundReason ? refundReason : undefined,
      });
      showToast('Refund processed successfully', 'success');
      setIsRefundModalOpen(false);
      setSelectedTx(null);
      fetchTransactions();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to process refund';
      showToast(msg, 'error');
    } finally {
      setIsRefunding(false);
    }
  };

  // Client-side CSV Export
  const handleExportCSV = () => {
    if (transactions.length === 0) {
      showToast('No transactions to export', 'info');
      return;
    }

    const headers = ['ID', 'Date', 'Type', 'Merchant/Description', 'Category', 'Account', 'Payment Mode', 'Amount', 'Currency'];
    const rows = transactions.map((t) => [
      t.id,
      formatDate(t.transactionDate),
      t.transactionType,
      `"${(t.merchantName || t.description || '').replace(/"/g, '""')}"`,
      `"${t.categoryName}"`,
      `"${t.accountName}"`,
      t.paymentMode,
      t.amount,
      'INR',
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `FinWise_Transactions_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('Transactions exported to CSV successfully', 'success');
  };

  return (
    <div className="space-y-6">
      {/* Header and Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Transaction Ledger</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Search, filter, and audit all inflows, expenses, and account transfers.
          </p>
        </div>

        <div className="flex items-center space-x-2.5">
          <Button variant="outline" size="sm" onClick={handleExportCSV} className="text-xs">
            <Download className="w-3.5 h-3.5 mr-1 text-slate-500" />
            Export CSV
          </Button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {/* Search */}
          <div className="lg:col-span-2 relative">
            <Input
              placeholder="Search merchant, description..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              prefixText=""
              className="pl-9"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
          </div>

          {/* Account Filter */}
          <Select
            options={[
              { value: '', label: 'All Accounts' },
              ...accounts.map((a) => ({ value: a.id, label: a.accountName })),
            ]}
            value={selectedAccount}
            onChange={(e) => {
              setSelectedAccount(e.target.value);
              setPage(0);
            }}
          />

          {/* Category Filter */}
          <Select
            options={[
              { value: '', label: 'All Categories' },
              ...categories.map((c) => ({ value: c.id, label: c.name })),
            ]}
            value={selectedCategory}
            onChange={(e) => {
              setSelectedCategory(e.target.value);
              setPage(0);
            }}
          />

          {/* Type Filter */}
          <Select
            options={[
              { value: '', label: 'All Types' },
              { value: 'EXPENSE', label: 'Expenses Only' },
              { value: 'INCOME', label: 'Income Only' },
            ]}
            value={selectedType}
            onChange={(e) => {
              setSelectedType(e.target.value);
              setPage(0);
            }}
          />
        </div>
      </Card>

      {/* Transactions Table */}
      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider text-[10px] font-semibold border-b border-slate-100">
              <tr>
                <th
                  className="py-3 px-4 cursor-pointer hover:text-slate-800"
                  onClick={() => {
                    setSortField('transactionDate');
                    setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                  }}
                >
                  <div className="flex items-center space-x-1">
                    <span>Date</span>
                    <ArrowUpDown className="w-3 h-3 text-slate-400" />
                  </div>
                </th>
                <th className="py-3 px-4">Merchant / Description</th>
                <th className="py-3 px-4">Category</th>
                <th className="py-3 px-4">Account</th>
                <th className="py-3 px-4">Method</th>
                <th
                  className="py-3 px-4 text-right cursor-pointer hover:text-slate-800"
                  onClick={() => {
                    setSortField('amount');
                    setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
                  }}
                >
                  <div className="flex items-center justify-end space-x-1">
                    <span>Amount</span>
                    <ArrowUpDown className="w-3 h-3 text-slate-400" />
                  </div>
                </th>
                <th className="py-3 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12">
                    <LoadingSpinner message="Filtering ledger entries..." />
                  </td>
                </tr>
              ) : transactions.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-500">
                    No transactions match the specified search or filter criteria.
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => {
                  const isCredit = tx.transactionType === 'INCOME' || tx.transactionType === 'REFUND' || tx.transactionType === 'TRANSFER_CREDIT';
                  const isRefund = tx.transactionType === 'REFUND';
                  return (
                    <tr key={tx.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 px-4 text-slate-500 whitespace-nowrap">
                        {formatDate(tx.transactionDate)}
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1.5">
                          <p className="font-semibold text-slate-900">{tx.merchantName || tx.description || 'Transaction'}</p>
                          {isRefund && (
                            <Badge variant="blue" size="sm">
                              Refund
                            </Badge>
                          )}
                        </div>
                        {tx.referenceNumber && (
                          <p className="text-[10px] text-slate-400 font-mono">{tx.referenceNumber}</p>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 font-medium text-[11px]">
                          {tx.categoryName}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-slate-600 font-medium">{tx.accountName}</td>
                      <td className="py-3 px-4 text-slate-500">{tx.paymentMode}</td>
                      <td
                        className={`py-3 px-4 text-right font-bold whitespace-nowrap ${
                          isCredit ? 'text-emerald-600' : 'text-slate-900'
                        }`}
                      >
                        {isCredit ? '+' : '-'} {formatINR(tx.amount)}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <div className="flex items-center justify-center space-x-1">
                          {tx.transactionType === 'EXPENSE' && (
                            <button
                              onClick={() => {
                                setSelectedTx(tx);
                                setRefundAmount(tx.amount);
                                setRefundReason('');
                                setIsRefundModalOpen(true);
                              }}
                              title="Process Refund"
                              className="p-1 rounded text-slate-400 hover:text-emerald-600 hover:bg-slate-100"
                            >
                              <RotateCcw className="w-3.5 h-3.5" />
                            </button>
                          )}
                          <button
                            onClick={() => handleOpenEdit(tx)}
                            title="Edit"
                            className="p-1 rounded text-slate-400 hover:text-blue-600 hover:bg-slate-100"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => {
                              setSelectedTx(tx);
                              setIsDeleteModalOpen(true);
                            }}
                            title="Delete"
                            className="p-1 rounded text-slate-400 hover:text-rose-600 hover:bg-slate-100"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="flex items-center justify-between px-4 py-3 bg-slate-50 border-t border-slate-100 text-xs text-slate-600">
          <span>
            Showing <span className="font-semibold">{transactions.length}</span> of{' '}
            <span className="font-semibold">{totalElements}</span> records
          </span>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="p-1 rounded border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-50 text-slate-600"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-2 font-medium">
              Page {page + 1} of {Math.max(1, totalPages)}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="p-1 rounded border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-50 text-slate-600"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </Card>

      {/* Edit Transaction Modal */}
      <Modal isOpen={isEditModalOpen} onClose={() => setIsEditModalOpen(false)} title="Edit Transaction" maxWidth="md">
        <form onSubmit={handleSaveEdit} className="space-y-4">
          <Input
            label="Amount"
            type="number"
            step="0.01"
            required
            prefixText="₹"
            value={editFormData.amount}
            onChange={(e) => setEditFormData({ ...editFormData, amount: e.target.value })}
          />

          <div className="grid grid-cols-2 gap-3">
            <Select
              label="Account"
              options={accounts.map((a) => ({ value: a.id, label: a.accountName }))}
              value={editFormData.accountId}
              onChange={(e) => setEditFormData({ ...editFormData, accountId: e.target.value })}
            />
            <Select
              label="Category"
              options={categories.map((c) => ({ value: c.id, label: c.name }))}
              value={editFormData.categoryId}
              onChange={(e) => setEditFormData({ ...editFormData, categoryId: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Merchant / Payee"
              value={editFormData.merchantName}
              onChange={(e) => setEditFormData({ ...editFormData, merchantName: e.target.value })}
            />
            <Input
              label="Date"
              type="date"
              required
              value={editFormData.transactionDate}
              onChange={(e) => setEditFormData({ ...editFormData, transactionDate: e.target.value })}
            />
          </div>

          <Input
            label="Description"
            value={editFormData.description}
            onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
          />

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={() => setIsEditModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary">
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal isOpen={isDeleteModalOpen} onClose={() => setIsDeleteModalOpen(false)} title="Confirm Transaction Deletion" maxWidth="sm">
        <div className="space-y-4">
          <p className="text-xs text-slate-600">
            Are you sure you want to permanently delete this transaction? The corresponding account balance will be
            automatically recalculated.
          </p>
          {selectedTx && (
            <div className="p-3 bg-slate-50 rounded-lg text-xs space-y-1">
              <p className="font-semibold text-slate-800">{selectedTx.merchantName || selectedTx.description}</p>
              <p className="text-slate-500 font-bold">{formatINR(selectedTx.amount)}</p>
            </div>
          )}
          <div className="flex items-center justify-end space-x-3 pt-2">
            <Button variant="outline" size="sm" onClick={() => setIsDeleteModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="danger" size="sm" onClick={handleDelete}>
              Confirm Delete
            </Button>
          </div>
        </div>
      </Modal>

      {/* Refund Modal */}
      <Modal isOpen={isRefundModalOpen} onClose={() => setIsRefundModalOpen(false)} title="Record Transaction Refund" maxWidth="sm">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleRefund();
          }}
          className="space-y-4"
        >
          <p className="text-xs text-slate-600">
            Process a full or partial refund for this expense. The refund amount will be credited back to your account and recorded in your transaction history.
          </p>
          {selectedTx && (
            <div className="p-3 bg-slate-50 rounded-lg text-xs space-y-1 border border-slate-200">
              <p className="font-semibold text-slate-800">{selectedTx.merchantName || selectedTx.description}</p>
              <p className="text-slate-500">Original Expense: <span className="font-bold text-slate-900">{formatINR(selectedTx.amount)}</span></p>
            </div>
          )}

          <Input
            label="Refund Amount (INR)"
            type="number"
            step="0.01"
            value={refundAmount}
            onChange={(e) => setRefundAmount(e.target.value)}
            required
          />

          <Input
            label="Refund Reason / Notes"
            placeholder="e.g. Returned item, order cancelled"
            value={refundReason}
            onChange={(e) => setRefundReason(e.target.value)}
          />

          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button type="button" variant="outline" size="sm" onClick={() => setIsRefundModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" isLoading={isRefunding}>
              Process Refund
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
