export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER_DEBIT' | 'TRANSFER_CREDIT' | 'REFUND';
export type PaymentMode = 'UPI' | 'NET_BANKING' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'CASH' | 'NEFT_RTGS_IMPS' | 'CHEQUE' | 'OTHER';
export type AccountType = 'SAVINGS' | 'CURRENT' | 'CREDIT_CARD' | 'INVESTMENT' | 'WALLET_UPI' | 'CASH';
export type RecurringFrequency = 'DAILY' | 'WEEKLY' | 'BI_WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
export type BudgetAlertStatus = 'NORMAL' | 'WARNING_80' | 'EXCEEDED_100' | 'OVERSPENT_120';
export type GoalStatus = 'IN_PROGRESS' | 'COMPLETED' | 'PAUSED' | 'CANCELLED';
export type GoalPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  currencyCode: string;
  roles?: string[];
  createdAt?: string;
}

export interface AuthResponseData {
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: User;
}

export interface Account {
  id: number;
  accountName: string;
  accountType: AccountType;
  institutionName?: string;
  accountNumberMask?: string;
  currentBalance: string;
  initialBalance?: string;
  currencyCode: string;
  creditLimit?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  createdAt?: string;
  updatedAt?: string;
}

export interface Category {
  id: number;
  name: string;
  type: 'INCOME' | 'EXPENSE';
  icon: string;
  colorCode: string;
  isSystem: boolean;
  subcategories?: Category[];
}

export interface Transaction {
  id: number;
  accountId: number;
  accountName: string;
  categoryId: number;
  categoryName: string;
  categoryIcon?: string;
  categoryColor?: string;
  transactionType: TransactionType;
  amount: string;
  currencyCode: string;
  transactionDate: string;
  merchantName?: string;
  description?: string;
  notes?: string;
  paymentMode: PaymentMode;
  referenceNumber?: string;
  status: 'COMPLETED' | 'PENDING' | 'FAILED' | 'RECONCILED';
  createdAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast: boolean;
}

export interface Transfer {
  transferId: number;
  fromAccountId: number;
  fromAccountName: string;
  toAccountId: number;
  toAccountName: string;
  amount: string;
  transferFee: string;
  transferDate: string;
  note?: string;
  status: string;
}

export interface BudgetCategory {
  categoryId: number;
  categoryName: string;
  allocatedAmount: string;
  spentAmount: string;
  remainingAmount: string;
  utilizationPercentage: number;
  alertStatus: BudgetAlertStatus;
  colorCode?: string;
}

export interface Budget {
  id: number;
  name: string;
  periodType: 'MONTHLY' | 'CUSTOM';
  startDate: string;
  endDate: string;
  totalBudgetAmount: string;
  totalSpent: string;
  remainingBudget: string;
  utilizationPercentage: number;
  notificationThreshold: number;
  isThresholdExceeded: boolean;
  overallAlertStatus: BudgetAlertStatus;
  isActive: boolean;
  categories: BudgetCategory[];
}

export interface SavingsGoal {
  id: number;
  title: string;
  description?: string;
  targetAmount: string;
  currentAmount: string;
  remainingAmount: string;
  targetDate: string;
  completionPercentage: number;
  status: GoalStatus;
  priority: GoalPriority;
  daysRemaining: number;
  createdAt?: string;
}

export interface GoalContribution {
  id: number;
  goalId: number;
  accountId: number;
  accountName: string;
  amount: string;
  contributionDate: string;
  note?: string;
}

export interface RecurringTransaction {
  id: number;
  accountId: number;
  accountName: string;
  categoryId: number;
  categoryName: string;
  title: string;
  amount: string;
  transactionType: TransactionType;
  frequency: RecurringFrequency;
  startDate: string;
  endDate?: string;
  nextExecutionDate: string;
  autoPost: boolean;
  isActive: boolean;
  description?: string;
}

export interface UpcomingReminder {
  id: number;
  title: string;
  amount: string;
  transactionType: TransactionType;
  categoryName: string;
  accountName: string;
  nextExecutionDate: string;
  daysRemaining: number;
}

export interface MonthlySpendingTrend {
  monthYear: string;
  displayLabel: string;
  totalSpending: string;
  topCategoryName?: string;
}

export interface CategoryExpense {
  categoryId: number;
  categoryName: string;
  amount?: string | number;
  totalExpense: string;
  percentage?: number;
  percentageShare: number;
  colorCode: string;
  icon?: string;
  transactionCount?: number;
}

export interface IncomeVsExpense {
  monthYear: string;
  displayLabel: string;
  income: string;
  expense: string;
  netSavings: string;
}

export interface NeedsWantsBreakdown {
  period: string;
  totalExpenses: string;
  needsAmount: string;
  needsPercentage: number;
  wantsAmount: string;
  wantsPercentage: number;
  savingsAmount: string;
  savingsPercentage: number;
  adheresTo503020Rule: boolean;
}

export interface ScoreComponent {
  componentName: string;
  score: number;
  maxScore: number;
  status?: string;
  feedback: string;
}

export interface FinancialHealthScore {
  score: number;
  ratingGrade: string;
  evaluationSummary: string;
  components: ScoreComponent[];
  calculationDate: string;
}

export interface SmartInsight {
  title: string;
  message: string;
  type: 'POSITIVE' | 'WARNING' | 'ALERT' | 'INFO';
  category?: string;
}

export interface DashboardSummary {
  netWorth: string;
  formattedNetWorth: string;
  currentMonthIncome: string;
  formattedCurrentMonthIncome: string;
  currentMonthExpense: string;
  formattedCurrentMonthExpense: string;
  savingsRatePercentage: number;
  activeAccountsCount: number;
  activeBudgetsCount: number;
  budgetUtilizationPercentage: number;
}

export interface DetectedSubscription {
  candidateId: string;
  subscriptionName: string;
  averagePayment: string;
  frequency: string;
  frequencyLabel?: string;
  estimatedAnnualCost: string;
  lastPaymentDate: string;
  suggestedNextPaymentDate?: string;
  occurrences: number;
  occurrenceCount?: number;
  confidenceScore: number;
  status: string;
  isSuggestion: boolean;
  accountId?: number;
  accountName?: string;
  categoryId?: number;
  categoryName?: string;
  transactionDescriptions?: string[];
}

export interface WhatIfPresetScenario {
  percentage: string;
  reducedMonthlySpending: string;
  monthlySavings: string;
  estimatedYearlySavings: string;
}

export interface WhatIfSimulationResult {
  categoryId: number;
  categoryName: string;
  reductionPercentage: string;
  currentMonthlySpending: string;
  reducedMonthlySpending: string;
  monthlySavings: string;
  estimatedYearlySavings: string;
  presetScenarios: WhatIfPresetScenario[];
  disclaimer: string;
}

export interface InAppNotification {
  id: number;
  title: string;
  message: string;
  type: 'SYSTEM_ALERT' | 'BUDGET_WARNING' | 'RECURRING_DUE' | 'GOAL_ACHIEVED';
  isRead: boolean;
  actionUrl?: string;
  createdAt: string;
}
