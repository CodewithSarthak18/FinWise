import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ToastProvider } from './contexts/ToastContext';
import { ProtectedRoute } from './components/layout/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { ErrorBoundary } from './components/common/ErrorBoundary';

// Pages
import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { AccountsPage } from './pages/AccountsPage';
import { BudgetsPage } from './pages/BudgetsPage';
import { SavingsGoalsPage } from './pages/SavingsGoalsPage';
import { RecurringPaymentsPage } from './pages/RecurringPaymentsPage';
import { SubscriptionsPage } from './pages/SubscriptionsPage';
import { WhatIfPage } from './pages/WhatIfPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { ReportsPage } from './pages/ReportsPage';
import { FinancialCalendarPage } from './pages/FinancialCalendarPage';
import { SettingsPage } from './pages/SettingsPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <ToastProvider>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Protected Application Routes */}
          <Route
            element={
              <ProtectedRoute>
                <ErrorBoundary fallbackTitle="Application View Error">
                  <AppLayout />
                </ErrorBoundary>
              </ProtectedRoute>
            }
          >
            <Route
              path="/dashboard"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Dashboard View">
                  <DashboardPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/transactions"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Transactions View">
                  <TransactionsPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/accounts"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Accounts View">
                  <AccountsPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/budgets"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Budgets View">
                  <BudgetsPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/goals"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Savings Goals View">
                  <SavingsGoalsPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/recurring"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Recurring Payments View">
                  <RecurringPaymentsPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/subscriptions"
              element={
                <ErrorBoundary fallbackTitle="Error Loading AI Subscriptions View">
                  <SubscriptionsPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/calculator"
              element={
                <ErrorBoundary fallbackTitle="Error Loading What-If Calculator View">
                  <WhatIfPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/analytics"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Analytics View">
                  <AnalyticsPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/reports"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Reports View">
                  <ReportsPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/calendar"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Financial Calendar View">
                  <FinancialCalendarPage />
                </ErrorBoundary>
              }
            />
            <Route
              path="/settings"
              element={
                <ErrorBoundary fallbackTitle="Error Loading Settings View">
                  <SettingsPage />
                </ErrorBoundary>
              }
            />
          </Route>

          {/* Catch-All */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  );
};
export default App;
