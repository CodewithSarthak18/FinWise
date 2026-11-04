package com.finwise.service.report;

import com.finwise.dto.report.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RFC 4180 Compliant CSV Report Generator.
 * Generates structured, readable CSV files for all financial reports
 * including metadata headers, summary KPIs, and detailed transaction lines.
 */
@Component
public class CsvReportGenerator {

    // ==========================================
    // 1. MONTHLY FINANCIAL REPORT
    // ==========================================
    public byte[] generateMonthlyReport(MonthlyReportData data) {
        StringBuilder sb = new StringBuilder();
        appendMetadataHeader(sb, data.getMetadata(), "Monthly Financial Report - " + data.getPeriod());

        // KPI Summary Block
        sb.append("--- SUMMARY METRICS ---\n");
        appendRow(sb, "Metric", "Amount (INR)");
        appendRow(sb, "Total Income", data.getFormattedTotalIncome());
        appendRow(sb, "Total Expenses", data.getFormattedTotalExpenses());
        appendRow(sb, "Net Savings", data.getFormattedNetSavings());
        appendRow(sb, "Savings Rate", data.getFormattedSavingsRate());
        appendRow(sb, "Net Worth", data.getFormattedNetWorth());
        appendRow(sb, "Total Transactions", String.valueOf(data.getTransactionCount()));
        sb.append("\n");

        // Budget Adherence if available
        if (data.isHasBudget()) {
            sb.append("--- BUDGET PERFORMANCE ---\n");
            appendRow(sb, "Budget Name", data.getBudgetName());
            appendRow(sb, "Budget Limit", data.getFormattedBudgetAmount());
            appendRow(sb, "Actual Spent", data.getFormattedBudgetSpent());
            appendRow(sb, "Remaining Amount", data.getFormattedBudgetRemaining());
            appendRow(sb, "Utilization %", data.getFormattedBudgetUtilization());
            appendRow(sb, "Status", data.getBudgetStatus());
            sb.append("\n");
        }

        // Category Breakdown
        sb.append("--- EXPENSE CATEGORY BREAKDOWN ---\n");
        appendRow(sb, "Category Name", "Total Amount (INR)", "% of Total Expenses", "Transaction Count");
        if (data.getCategoryBreakdown() != null && !data.getCategoryBreakdown().isEmpty()) {
            for (ReportCategorySummary cat : data.getCategoryBreakdown()) {
                appendRow(sb, cat.getCategoryName(), cat.getFormattedAmount(), cat.getFormattedPercentage(), String.valueOf(cat.getTransactionCount()));
            }
        } else {
            sb.append("# No expense categories recorded for this period\n");
        }
        sb.append("\n");

        // Itemized Transactions
        sb.append("--- ITEMIZED TRANSACTIONS ---\n");
        appendRow(sb, "Date", "Description", "Category", "Account", "Type", "Payment Method", "Amount (INR)");
        if (data.getTransactions() != null && !data.getTransactions().isEmpty()) {
            for (ReportTransactionItem tx : data.getTransactions()) {
                appendRow(sb, tx.getDate(), tx.getDescription(), tx.getCategory(), tx.getAccount(), tx.getType(), tx.getPaymentMethod(), tx.getFormattedAmount());
            }
        } else {
            sb.append("# No transactions recorded in this period\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==========================================
    // 2. ANNUAL FINANCIAL REPORT
    // ==========================================
    public byte[] generateAnnualReport(AnnualReportData data) {
        StringBuilder sb = new StringBuilder();
        appendMetadataHeader(sb, data.getMetadata(), "Annual Financial Report - Year " + data.getYear());

        sb.append("--- ANNUAL SUMMARY ---\n");
        appendRow(sb, "Metric", "Amount (INR)");
        appendRow(sb, "Total Annual Income", data.getFormattedTotalIncome());
        appendRow(sb, "Total Annual Expenses", data.getFormattedTotalExpenses());
        appendRow(sb, "Net Annual Savings", data.getFormattedNetSavings());
        appendRow(sb, "Annual Savings Rate", data.getFormattedSavingsRate());
        appendRow(sb, "Monthly Average Income", data.getFormattedMonthlyAverageIncome());
        appendRow(sb, "Monthly Average Expense", data.getFormattedMonthlyAverageExpense());
        sb.append("\n");

        sb.append("--- MONTH-BY-MONTH BREAKDOWN ---\n");
        appendRow(sb, "Month", "Income (INR)", "Expenses (INR)", "Net Savings (INR)", "Savings Rate");
        if (data.getMonthlyBreakdown() != null && !data.getMonthlyBreakdown().isEmpty()) {
            for (AnnualReportData.AnnualMonthlyRow row : data.getMonthlyBreakdown()) {
                appendRow(sb, row.getMonthName(), row.getFormattedIncome(), row.getFormattedExpenses(), row.getFormattedNetSavings(), row.getFormattedSavingsRate());
            }
        } else {
            sb.append("# No monthly activity recorded for this year\n");
        }
        sb.append("\n");

        sb.append("--- TOP ANNUAL EXPENSE CATEGORIES ---\n");
        appendRow(sb, "Category Name", "Total Spent (INR)", "% of Total Expenses", "Transactions");
        if (data.getTopExpenseCategories() != null && !data.getTopExpenseCategories().isEmpty()) {
            for (ReportCategorySummary cat : data.getTopExpenseCategories()) {
                appendRow(sb, cat.getCategoryName(), cat.getFormattedAmount(), cat.getFormattedPercentage(), String.valueOf(cat.getTransactionCount()));
            }
        } else {
            sb.append("# No expense categories recorded for this year\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==========================================
    // 3. CATEGORY-WISE REPORT
    // ==========================================
    public byte[] generateCategoryReport(CategoryReportData data) {
        StringBuilder sb = new StringBuilder();
        appendMetadataHeader(sb, data.getMetadata(), "Category-Wise Financial Report");

        sb.append("--- CATEGORY METRICS ---\n");
        appendRow(sb, "Metric", "Value");
        appendRow(sb, "Total Expenses", data.getFormattedTotalExpenses());
        appendRow(sb, "Total Income", data.getFormattedTotalIncome());
        appendRow(sb, "Active Categories", String.valueOf(data.getCategoryCount()));
        appendRow(sb, "Total Transactions", String.valueOf(data.getTransactionCount()));
        sb.append("\n");

        sb.append("--- CATEGORY SUMMARY ---\n");
        appendRow(sb, "Category Name", "Type", "Total Amount (INR)", "% Share", "Transactions", "Avg Transaction (INR)");
        if (data.getCategorySummaries() != null && !data.getCategorySummaries().isEmpty()) {
            for (ReportCategorySummary cat : data.getCategorySummaries()) {
                appendRow(sb, cat.getCategoryName(), cat.getType(), cat.getFormattedAmount(), cat.getFormattedPercentage(), String.valueOf(cat.getTransactionCount()), cat.getFormattedAverage());
            }
        } else {
            sb.append("# No category records found\n");
        }
        sb.append("\n");

        sb.append("--- ITEMIZED TRANSACTIONS ---\n");
        appendRow(sb, "Date", "Description", "Category", "Account", "Type", "Amount (INR)");
        if (data.getTransactions() != null && !data.getTransactions().isEmpty()) {
            for (ReportTransactionItem tx : data.getTransactions()) {
                appendRow(sb, tx.getDate(), tx.getDescription(), tx.getCategory(), tx.getAccount(), tx.getType(), tx.getFormattedAmount());
            }
        } else {
            sb.append("# No transaction records found\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==========================================
    // 4. INCOME AND EXPENSE SUMMARY
    // ==========================================
    public byte[] generateIncomeExpenseReport(IncomeExpenseReportData data) {
        StringBuilder sb = new StringBuilder();
        appendMetadataHeader(sb, data.getMetadata(), "Income & Expense Cash Flow Report");

        sb.append("--- CASH FLOW SUMMARY ---\n");
        appendRow(sb, "Metric", "Amount (INR)");
        appendRow(sb, "Total Income", data.getFormattedTotalIncome());
        appendRow(sb, "Total Expenses", data.getFormattedTotalExpenses());
        appendRow(sb, "Net Cash Flow", data.getFormattedNetCashFlow());
        appendRow(sb, "Savings Rate", data.getFormattedSavingsRate());
        sb.append("\n");

        sb.append("--- 50/30/20 BUDGET ANALYSIS ---\n");
        appendRow(sb, "Bucket", "Actual Amount (INR)", "Actual %", "Target %");
        appendRow(sb, "Needs", data.getFormattedNeedsAmount(), data.getFormattedNeedsPercentage(), "50.00%");
        appendRow(sb, "Wants", data.getFormattedWantsAmount(), data.getFormattedWantsPercentage(), "30.00%");
        appendRow(sb, "Savings & Investments", data.getFormattedSavingsInvestmentsAmount(), data.getFormattedSavingsInvestmentsPercentage(), "20.00%");
        sb.append("\n");

        sb.append("--- INCOME BY CATEGORY ---\n");
        appendRow(sb, "Category Name", "Total Amount (INR)", "% of Total Income", "Transactions");
        if (data.getIncomeCategories() != null && !data.getIncomeCategories().isEmpty()) {
            for (ReportCategorySummary cat : data.getIncomeCategories()) {
                appendRow(sb, cat.getCategoryName(), cat.getFormattedAmount(), cat.getFormattedPercentage(), String.valueOf(cat.getTransactionCount()));
            }
        } else {
            sb.append("# No income categories found\n");
        }
        sb.append("\n");

        sb.append("--- EXPENSE BY CATEGORY ---\n");
        appendRow(sb, "Category Name", "Total Amount (INR)", "% of Total Expenses", "Transactions");
        if (data.getExpenseCategories() != null && !data.getExpenseCategories().isEmpty()) {
            for (ReportCategorySummary cat : data.getExpenseCategories()) {
                appendRow(sb, cat.getCategoryName(), cat.getFormattedAmount(), cat.getFormattedPercentage(), String.valueOf(cat.getTransactionCount()));
            }
        } else {
            sb.append("# No expense categories found\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==========================================
    // 5. SAVINGS SUMMARY REPORT
    // ==========================================
    public byte[] generateSavingsReport(SavingsReportData data) {
        StringBuilder sb = new StringBuilder();
        appendMetadataHeader(sb, data.getMetadata(), "Savings & Wealth Goals Report");

        sb.append("--- SAVINGS OVERVIEW ---\n");
        appendRow(sb, "Metric", "Amount (INR)");
        appendRow(sb, "Period Net Savings", data.getFormattedNetSavingsInPeriod());
        appendRow(sb, "Period Savings Rate", data.getFormattedSavingsRateInPeriod());
        appendRow(sb, "Total Goal Target", data.getFormattedTotalGoalTargetAmount());
        appendRow(sb, "Total Accumulated Savings", data.getFormattedTotalGoalCurrentAmount());
        appendRow(sb, "Total Remaining Target", data.getFormattedTotalGoalRemainingAmount());
        appendRow(sb, "Overall Goal Progress", data.getFormattedOverallGoalProgress());
        sb.append("\n");

        sb.append("--- ACTIVE SAVINGS GOALS ---\n");
        appendRow(sb, "Goal Title", "Target Amount (INR)", "Current Saved (INR)", "Remaining (INR)", "Progress %", "Target Date", "Priority", "Status");
        if (data.getGoals() != null && !data.getGoals().isEmpty()) {
            for (SavingsReportData.SavingsGoalReportItem g : data.getGoals()) {
                appendRow(sb, g.getTitle(), g.getFormattedTargetAmount(), g.getFormattedCurrentAmount(), g.getFormattedRemainingAmount(), g.getFormattedProgressPercentage(), g.getTargetDate(), g.getPriority(), g.getStatus());
            }
        } else {
            sb.append("# No savings goals configured\n");
        }
        sb.append("\n");

        sb.append("--- PERIOD GOAL CONTRIBUTIONS ---\n");
        appendRow(sb, "Date", "Goal Title", "Source Account", "Amount (INR)", "Note");
        if (data.getContributions() != null && !data.getContributions().isEmpty()) {
            for (SavingsReportData.GoalContributionReportItem c : data.getContributions()) {
                appendRow(sb, c.getContributionDate(), c.getGoalTitle(), c.getAccountName(), c.getFormattedAmount(), c.getNote() != null ? c.getNote() : "-");
            }
        } else {
            sb.append("# No goal contributions recorded in this period\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==========================================
    // 6. BUDGET PERFORMANCE REPORT
    // ==========================================
    public byte[] generateBudgetPerformanceReport(BudgetPerformanceReportData data) {
        StringBuilder sb = new StringBuilder();
        appendMetadataHeader(sb, data.getMetadata(), "Budget Performance & Variance Report");

        if (!data.isHasBudget()) {
            sb.append("# No active budget found for the selected period\n");
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        sb.append("--- BUDGET OVERVIEW ---\n");
        appendRow(sb, "Budget Name", data.getBudgetName());
        appendRow(sb, "Period", data.getStartDate() + " to " + data.getEndDate());
        appendRow(sb, "Total Budget Amount", data.getFormattedTotalBudgetAmount());
        appendRow(sb, "Total Spent Amount", data.getFormattedTotalSpentAmount());
        appendRow(sb, "Remaining Amount", data.getFormattedRemainingAmount());
        appendRow(sb, "Utilization %", data.getFormattedUtilizationPercentage());
        appendRow(sb, "Alert Status", data.getAlertStatus());
        sb.append("\n");

        sb.append("--- CATEGORY PERFORMANCE ---\n");
        appendRow(sb, "Category Name", "Allocated (INR)", "Actual Spent (INR)", "Remaining (INR)", "Utilization %", "Status");
        if (data.getCategoryPerformance() != null && !data.getCategoryPerformance().isEmpty()) {
            for (BudgetPerformanceReportData.BudgetCategoryPerformanceItem item : data.getCategoryPerformance()) {
                appendRow(sb, item.getCategoryName(), item.getFormattedAllocatedAmount(), item.getFormattedSpentAmount(), item.getFormattedRemainingAmount(), item.getFormattedUtilizationPercentage(), item.getStatus());
            }
        } else {
            sb.append("# No category allocations configured for this budget\n");
        }
        sb.append("\n");

        if (data.getObservations() != null && !data.getObservations().isEmpty()) {
            sb.append("--- OBSERVATIONS & INSIGHTS ---\n");
            for (String obs : data.getObservations()) {
                sb.append("# ").append(obs).append("\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==========================================
    // CSV RFC-4180 FORMATTING HELPERS
    // ==========================================

    private void appendMetadataHeader(StringBuilder sb, ReportMetadata meta, String title) {
        sb.append("# ============================================================\n");
        sb.append("# FINWISE PERSONAL FINANCE MANAGEMENT PLATFORM\n");
        sb.append("# Report Title : ").append(title).append("\n");
        sb.append("# Account      : ").append(meta.getUserName()).append(" (").append(meta.getUserEmail()).append(")\n");
        sb.append("# Date Range   : ").append(meta.getDateRangeText()).append("\n");
        sb.append("# Generated At : ").append(meta.getGeneratedAt()).append("\n");
        sb.append("# Currency     : ").append(meta.getCurrencyCode()).append(" (").append(meta.getCurrencySymbol()).append(")\n");
        sb.append("# ============================================================\n\n");
    }

    private void appendRow(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeCsv(cells[i]));
        }
        sb.append("\n");
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        boolean containsComma = value.contains(",");
        boolean containsQuote = value.contains("\"");
        boolean containsNewline = value.contains("\n") || value.contains("\r");

        if (containsComma || containsQuote || containsNewline) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
