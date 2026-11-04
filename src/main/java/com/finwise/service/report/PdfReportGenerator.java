package com.finwise.service.report;

import com.finwise.dto.report.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Pure Java PDF-1.4 Report Generator.
 * Zero external library dependencies. Generates 100% compliant PDF documents
 * that render cleanly in Adobe Acrobat, Chrome, Firefox, Safari, and Edge.
 */
@Component
public class PdfReportGenerator {

    private static final float PAGE_WIDTH = 595.28f;  // A4 Width
    private static final float PAGE_HEIGHT = 841.89f; // A4 Height
    private static final float MARGIN_LEFT = 36f;
    private static final float MARGIN_RIGHT = 559.28f;
    private static final float CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT; // 523.28f
    private static final float MARGIN_BOTTOM = 45f;
    private static final float TOP_CURSOR = 805f;

    // Colors (RGB floats 0.0 - 1.0)
    private static final float[] COLOR_PRIMARY_NAVY = {0.058f, 0.090f, 0.164f};   // #0F172A
    private static final float[] COLOR_SLATE_HEADER = {0.118f, 0.161f, 0.231f};   // #1E293B
    private static final float[] COLOR_MUTED_GRAY   = {0.392f, 0.455f, 0.545f};   // #64748B
    private static final float[] COLOR_BORDER_LINE  = {0.886f, 0.909f, 0.941f};   // #E2E8F0
    private static final float[] COLOR_ROW_ALT      = {0.973f, 0.980f, 0.988f};   // #F8FAFC
    private static final float[] COLOR_WHITE        = {1.0f, 1.0f, 1.0f};          // #FFFFFF
    private static final float[] COLOR_GREEN_TEXT   = {0.020f, 0.588f, 0.412f};   // #059669
    private static final float[] COLOR_RED_TEXT     = {0.863f, 0.149f, 0.149f};   // #DC2626
    private static final float[] COLOR_TEXT_DARK    = {0.058f, 0.090f, 0.164f};   // #0F172A

    // ==========================================
    // 1. MONTHLY FINANCIAL REPORT
    // ==========================================
    public byte[] generateMonthlyReport(MonthlyReportData data) {
        PdfDocument doc = new PdfDocument();
        doc.beginPage();
        drawHeader(doc, data.getMetadata(), "MONTHLY FINANCIAL REPORT - " + data.getPeriod().toUpperCase());

        // KPI Metrics Row (4 Cards)
        List<KpiCard> kpiCards = List.of(
                new KpiCard("TOTAL INCOME", data.getFormattedTotalIncome(), COLOR_GREEN_TEXT),
                new KpiCard("TOTAL EXPENSES", data.getFormattedTotalExpenses(), COLOR_RED_TEXT),
                new KpiCard("NET SAVINGS", data.getFormattedNetSavings(),
                        data.getNetSavings() != null && data.getNetSavings().signum() >= 0 ? COLOR_GREEN_TEXT : COLOR_RED_TEXT),
                new KpiCard("SAVINGS RATE", data.getFormattedSavingsRate(), COLOR_PRIMARY_NAVY)
        );
        drawKpiCards(doc, kpiCards);

        // Budget Performance Callout if available
        if (data.isHasBudget()) {
            drawSectionTitle(doc, "Budget Adherence (" + data.getBudgetName() + ")");
            List<KpiCard> budgetCards = List.of(
                    new KpiCard("BUDGET LIMIT", data.getFormattedBudgetAmount(), COLOR_PRIMARY_NAVY),
                    new KpiCard("ACTUAL SPENT", data.getFormattedBudgetSpent(), COLOR_RED_TEXT),
                    new KpiCard("REMAINING", data.getFormattedBudgetRemaining(),
                            data.getBudgetRemaining() != null && data.getBudgetRemaining().signum() >= 0 ? COLOR_GREEN_TEXT : COLOR_RED_TEXT),
                    new KpiCard("UTILIZATION", data.getFormattedBudgetUtilization() + " (" + data.getBudgetStatus() + ")", COLOR_SLATE_HEADER)
            );
            drawKpiCards(doc, budgetCards);
        }

        // Category Breakdown Section
        drawSectionTitle(doc, "Expense Category Breakdown");
        String[] catHeaders = {"Category Name", "Amount", "% of Expenses", "Transactions"};
        float[] catWidths = {200f, 130f, 100f, 93.28f};
        boolean[] catAlign = {false, true, true, true};
        List<String[]> catRows = new ArrayList<>();
        if (data.getCategoryBreakdown() != null && !data.getCategoryBreakdown().isEmpty()) {
            for (ReportCategorySummary cat : data.getCategoryBreakdown()) {
                catRows.add(new String[]{
                        cat.getCategoryName(),
                        cat.getFormattedAmount(),
                        cat.getFormattedPercentage(),
                        String.valueOf(cat.getTransactionCount())
                });
            }
        }
        drawTable(doc, catHeaders, catWidths, catRows, catAlign, "No expense categories recorded in this period.");

        // Itemized Transactions Section
        drawSectionTitle(doc, "Itemized Transactions (" + data.getTransactionCount() + " records)");
        String[] txHeaders = {"Date", "Description", "Category", "Account", "Type", "Amount"};
        float[] txWidths = {65f, 145f, 95f, 85f, 55f, 78.28f};
        boolean[] txAlign = {false, false, false, false, false, true};
        List<String[]> txRows = new ArrayList<>();
        if (data.getTransactions() != null && !data.getTransactions().isEmpty()) {
            for (ReportTransactionItem tx : data.getTransactions()) {
                txRows.add(new String[]{
                        tx.getDate(),
                        tx.getDescription(),
                        tx.getCategory(),
                        tx.getAccount(),
                        tx.getType(),
                        tx.getFormattedAmount()
                });
            }
        }
        drawTable(doc, txHeaders, txWidths, txRows, txAlign, "No transactions recorded in this period.");

        doc.finishDocument();
        return doc.toByteArray();
    }

    // ==========================================
    // 2. ANNUAL FINANCIAL REPORT
    // ==========================================
    public byte[] generateAnnualReport(AnnualReportData data) {
        PdfDocument doc = new PdfDocument();
        doc.beginPage();
        drawHeader(doc, data.getMetadata(), "ANNUAL FINANCIAL REPORT - YEAR " + data.getYear());

        List<KpiCard> kpis = List.of(
                new KpiCard("ANNUAL INCOME", data.getFormattedTotalIncome(), COLOR_GREEN_TEXT),
                new KpiCard("ANNUAL EXPENSES", data.getFormattedTotalExpenses(), COLOR_RED_TEXT),
                new KpiCard("NET SAVINGS", data.getFormattedNetSavings(), COLOR_GREEN_TEXT),
                new KpiCard("ANNUAL SAVINGS RATE", data.getFormattedSavingsRate(), COLOR_PRIMARY_NAVY)
        );
        drawKpiCards(doc, kpis);

        List<KpiCard> avgKpis = List.of(
                new KpiCard("MONTHLY AVG INCOME", data.getFormattedMonthlyAverageIncome(), COLOR_SLATE_HEADER),
                new KpiCard("MONTHLY AVG EXPENSE", data.getFormattedMonthlyAverageExpense(), COLOR_SLATE_HEADER)
        );
        drawKpiCards(doc, avgKpis);

        drawSectionTitle(doc, "Month-by-Month Cash Flow Performance");
        String[] monthHeaders = {"Month", "Income", "Expenses", "Net Savings", "Savings Rate"};
        float[] monthWidths = {100f, 110f, 110f, 110f, 93.28f};
        boolean[] monthAlign = {false, true, true, true, true};
        List<String[]> monthRows = new ArrayList<>();
        if (data.getMonthlyBreakdown() != null && !data.getMonthlyBreakdown().isEmpty()) {
            for (AnnualReportData.AnnualMonthlyRow row : data.getMonthlyBreakdown()) {
                monthRows.add(new String[]{
                        row.getMonthName(),
                        row.getFormattedIncome(),
                        row.getFormattedExpenses(),
                        row.getFormattedNetSavings(),
                        row.getFormattedSavingsRate()
                });
            }
        }
        drawTable(doc, monthHeaders, monthWidths, monthRows, monthAlign, "No monthly activity recorded for this year.");

        drawSectionTitle(doc, "Top Annual Expense Categories");
        String[] topCatHeaders = {"Category Name", "Total Spent", "% of Annual Expenses", "Transactions"};
        float[] topCatWidths = {200f, 130f, 100f, 93.28f};
        boolean[] topCatAlign = {false, true, true, true};
        List<String[]> topCatRows = new ArrayList<>();
        if (data.getTopExpenseCategories() != null && !data.getTopExpenseCategories().isEmpty()) {
            for (ReportCategorySummary cat : data.getTopExpenseCategories()) {
                topCatRows.add(new String[]{
                        cat.getCategoryName(),
                        cat.getFormattedAmount(),
                        cat.getFormattedPercentage(),
                        String.valueOf(cat.getTransactionCount())
                });
            }
        }
        drawTable(doc, topCatHeaders, topCatWidths, topCatRows, topCatAlign, "No expense categories recorded for this year.");

        doc.finishDocument();
        return doc.toByteArray();
    }

    // ==========================================
    // 3. CATEGORY-WISE REPORT
    // ==========================================
    public byte[] generateCategoryReport(CategoryReportData data) {
        PdfDocument doc = new PdfDocument();
        doc.beginPage();
        drawHeader(doc, data.getMetadata(), "CATEGORY-WISE FINANCIAL REPORT");

        List<KpiCard> kpis = List.of(
                new KpiCard("TOTAL EXPENSES", data.getFormattedTotalExpenses(), COLOR_RED_TEXT),
                new KpiCard("TOTAL INCOME", data.getFormattedTotalIncome(), COLOR_GREEN_TEXT),
                new KpiCard("CATEGORIES ACTIVE", String.valueOf(data.getCategoryCount()), COLOR_PRIMARY_NAVY),
                new KpiCard("TRANSACTIONS", String.valueOf(data.getTransactionCount()), COLOR_SLATE_HEADER)
        );
        drawKpiCards(doc, kpis);

        drawSectionTitle(doc, "Category Summary Breakdown");
        String[] headers = {"Category Name", "Type", "Total Amount", "% Share", "Transactions", "Avg Transaction"};
        float[] widths = {140f, 65f, 95f, 65f, 65f, 93.28f};
        boolean[] aligns = {false, false, true, true, true, true};
        List<String[]> rows = new ArrayList<>();
        if (data.getCategorySummaries() != null && !data.getCategorySummaries().isEmpty()) {
            for (ReportCategorySummary cat : data.getCategorySummaries()) {
                rows.add(new String[]{
                        cat.getCategoryName(),
                        cat.getType(),
                        cat.getFormattedAmount(),
                        cat.getFormattedPercentage(),
                        String.valueOf(cat.getTransactionCount()),
                        cat.getFormattedAverage()
                });
            }
        }
        drawTable(doc, headers, widths, rows, aligns, "No category transactions recorded for this period.");

        drawSectionTitle(doc, "Itemized Category Ledger");
        String[] txHeaders = {"Date", "Description", "Category", "Account", "Type", "Amount"};
        float[] txWidths = {65f, 145f, 95f, 85f, 55f, 78.28f};
        boolean[] txAlign = {false, false, false, false, false, true};
        List<String[]> txRows = new ArrayList<>();
        if (data.getTransactions() != null && !data.getTransactions().isEmpty()) {
            for (ReportTransactionItem tx : data.getTransactions()) {
                txRows.add(new String[]{
                        tx.getDate(),
                        tx.getDescription(),
                        tx.getCategory(),
                        tx.getAccount(),
                        tx.getType(),
                        tx.getFormattedAmount()
                });
            }
        }
        drawTable(doc, txHeaders, txWidths, txRows, txAlign, "No transaction records found.");

        doc.finishDocument();
        return doc.toByteArray();
    }

    // ==========================================
    // 4. INCOME AND EXPENSE SUMMARY
    // ==========================================
    public byte[] generateIncomeExpenseReport(IncomeExpenseReportData data) {
        PdfDocument doc = new PdfDocument();
        doc.beginPage();
        drawHeader(doc, data.getMetadata(), "INCOME & EXPENSE CASH FLOW REPORT");

        List<KpiCard> kpis = List.of(
                new KpiCard("TOTAL INCOME", data.getFormattedTotalIncome(), COLOR_GREEN_TEXT),
                new KpiCard("TOTAL EXPENSES", data.getFormattedTotalExpenses(), COLOR_RED_TEXT),
                new KpiCard("NET CASH FLOW", data.getFormattedNetCashFlow(), COLOR_GREEN_TEXT),
                new KpiCard("SAVINGS RATE", data.getFormattedSavingsRate(), COLOR_PRIMARY_NAVY)
        );
        drawKpiCards(doc, kpis);

        drawSectionTitle(doc, "50/30/20 Needs vs Wants vs Savings Analysis");
        List<KpiCard> ruleKpis = List.of(
                new KpiCard("NEEDS (TARGET 50%)", data.getFormattedNeedsAmount() + " (" + data.getFormattedNeedsPercentage() + ")", COLOR_PRIMARY_NAVY),
                new KpiCard("WANTS (TARGET 30%)", data.getFormattedWantsAmount() + " (" + data.getFormattedWantsPercentage() + ")", COLOR_MUTED_GRAY),
                new KpiCard("SAVINGS (TARGET 20%)", data.getFormattedSavingsInvestmentsAmount() + " (" + data.getFormattedSavingsInvestmentsPercentage() + ")", COLOR_GREEN_TEXT)
        );
        drawKpiCards(doc, ruleKpis);

        drawSectionTitle(doc, "Income Breakdown by Category");
        String[] incHeaders = {"Category Name", "Total Received", "% of Total Income", "Transactions"};
        float[] incWidths = {200f, 130f, 100f, 93.28f};
        boolean[] incAlign = {false, true, true, true};
        List<String[]> incRows = new ArrayList<>();
        if (data.getIncomeCategories() != null && !data.getIncomeCategories().isEmpty()) {
            for (ReportCategorySummary cat : data.getIncomeCategories()) {
                incRows.add(new String[]{
                        cat.getCategoryName(),
                        cat.getFormattedAmount(),
                        cat.getFormattedPercentage(),
                        String.valueOf(cat.getTransactionCount())
                });
            }
        }
        drawTable(doc, incHeaders, incWidths, incRows, incAlign, "No income recorded for this period.");

        drawSectionTitle(doc, "Expense Breakdown by Category");
        String[] expHeaders = {"Category Name", "Total Spent", "% of Total Expense", "Transactions"};
        float[] expWidths = {200f, 130f, 100f, 93.28f};
        boolean[] expAlign = {false, true, true, true};
        List<String[]> expRows = new ArrayList<>();
        if (data.getExpenseCategories() != null && !data.getExpenseCategories().isEmpty()) {
            for (ReportCategorySummary cat : data.getExpenseCategories()) {
                expRows.add(new String[]{
                        cat.getCategoryName(),
                        cat.getFormattedAmount(),
                        cat.getFormattedPercentage(),
                        String.valueOf(cat.getTransactionCount())
                });
            }
        }
        drawTable(doc, expHeaders, expWidths, expRows, expAlign, "No expenses recorded for this period.");

        doc.finishDocument();
        return doc.toByteArray();
    }

    // ==========================================
    // 5. SAVINGS SUMMARY REPORT
    // ==========================================
    public byte[] generateSavingsReport(SavingsReportData data) {
        PdfDocument doc = new PdfDocument();
        doc.beginPage();
        drawHeader(doc, data.getMetadata(), "SAVINGS & WEALTH GOALS REPORT");

        List<KpiCard> kpis = List.of(
                new KpiCard("PERIOD NET SAVINGS", data.getFormattedNetSavingsInPeriod(), COLOR_GREEN_TEXT),
                new KpiCard("SAVINGS RATE", data.getFormattedSavingsRateInPeriod(), COLOR_PRIMARY_NAVY),
                new KpiCard("TOTAL GOAL TARGET", data.getFormattedTotalGoalTargetAmount(), COLOR_SLATE_HEADER),
                new KpiCard("ACCUMULATED SAVINGS", data.getFormattedTotalGoalCurrentAmount(), COLOR_GREEN_TEXT)
        );
        drawKpiCards(doc, kpis);

        List<KpiCard> progressCards = List.of(
                new KpiCard("REMAINING TARGET", data.getFormattedTotalGoalRemainingAmount(), COLOR_RED_TEXT),
                new KpiCard("OVERALL PROGRESS", data.getFormattedOverallGoalProgress(), COLOR_PRIMARY_NAVY)
        );
        drawKpiCards(doc, progressCards);

        drawSectionTitle(doc, "Active Savings Goals Progress");
        String[] goalHeaders = {"Goal Title", "Target Amount", "Current Saved", "Remaining", "Progress %", "Target Date", "Status"};
        float[] goalWidths = {115f, 75f, 75f, 75f, 55f, 65f, 63.28f};
        boolean[] goalAlign = {false, true, true, true, true, false, false};
        List<String[]> goalRows = new ArrayList<>();
        if (data.getGoals() != null && !data.getGoals().isEmpty()) {
            for (SavingsReportData.SavingsGoalReportItem g : data.getGoals()) {
                goalRows.add(new String[]{
                        g.getTitle(),
                        g.getFormattedTargetAmount(),
                        g.getFormattedCurrentAmount(),
                        g.getFormattedRemainingAmount(),
                        g.getFormattedProgressPercentage(),
                        g.getTargetDate(),
                        g.getStatus()
                });
            }
        }
        drawTable(doc, goalHeaders, goalWidths, goalRows, goalAlign, "No savings goals configured.");

        drawSectionTitle(doc, "Goal Contributions Recorded in Period");
        String[] contribHeaders = {"Date", "Goal Title", "Source Account", "Amount", "Note"};
        float[] contribWidths = {75f, 130f, 110f, 90f, 118.28f};
        boolean[] contribAlign = {false, false, false, true, false};
        List<String[]> contribRows = new ArrayList<>();
        if (data.getContributions() != null && !data.getContributions().isEmpty()) {
            for (SavingsReportData.GoalContributionReportItem c : data.getContributions()) {
                contribRows.add(new String[]{
                        c.getContributionDate(),
                        c.getGoalTitle(),
                        c.getAccountName(),
                        c.getFormattedAmount(),
                        c.getNote() != null ? c.getNote() : "-"
                });
            }
        }
        drawTable(doc, contribHeaders, contribWidths, contribRows, contribAlign, "No goal contributions recorded in this period.");

        doc.finishDocument();
        return doc.toByteArray();
    }

    // ==========================================
    // 6. BUDGET PERFORMANCE REPORT
    // ==========================================
    public byte[] generateBudgetPerformanceReport(BudgetPerformanceReportData data) {
        PdfDocument doc = new PdfDocument();
        doc.beginPage();
        drawHeader(doc, data.getMetadata(), "BUDGET PERFORMANCE & VARIANCE REPORT");

        if (!data.isHasBudget()) {
            drawSectionTitle(doc, "Budget Status");
            drawEmptyStateBox(doc, "No active budget found for the selected period.");
            doc.finishDocument();
            return doc.toByteArray();
        }

        List<KpiCard> kpis = List.of(
                new KpiCard("BUDGET LIMIT", data.getFormattedTotalBudgetAmount(), COLOR_PRIMARY_NAVY),
                new KpiCard("TOTAL SPENT", data.getFormattedTotalSpentAmount(), COLOR_RED_TEXT),
                new KpiCard("REMAINING", data.getFormattedRemainingAmount(),
                        data.getRemainingAmount() != null && data.getRemainingAmount().signum() >= 0 ? COLOR_GREEN_TEXT : COLOR_RED_TEXT),
                new KpiCard("UTILIZATION", data.getFormattedUtilizationPercentage(), COLOR_SLATE_HEADER)
        );
        drawKpiCards(doc, kpis);

        drawSectionTitle(doc, "Category-wise Budget Performance");
        String[] headers = {"Category Name", "Allocated", "Actual Spent", "Remaining", "Utilization", "Status"};
        float[] widths = {130f, 80f, 80f, 80f, 75f, 78.28f};
        boolean[] aligns = {false, true, true, true, true, false};
        List<String[]> rows = new ArrayList<>();
        if (data.getCategoryPerformance() != null && !data.getCategoryPerformance().isEmpty()) {
            for (BudgetPerformanceReportData.BudgetCategoryPerformanceItem item : data.getCategoryPerformance()) {
                rows.add(new String[]{
                        item.getCategoryName(),
                        item.getFormattedAllocatedAmount(),
                        item.getFormattedSpentAmount(),
                        item.getFormattedRemainingAmount(),
                        item.getFormattedUtilizationPercentage(),
                        item.getStatus()
                });
            }
        }
        drawTable(doc, headers, widths, rows, aligns, "No category allocations defined for this budget.");

        if (data.getObservations() != null && !data.getObservations().isEmpty()) {
            drawSectionTitle(doc, "Budget Insights & Smart Recommendations");
            for (String obs : data.getObservations()) {
                drawBulletPoint(doc, obs);
            }
        }

        doc.finishDocument();
        return doc.toByteArray();
    }

    // ==========================================
    // RENDERING HELPERS & CANVAS ABSTRACTIONS
    // ==========================================

    private void drawHeader(PdfDocument doc, ReportMetadata meta, String title) {
        // Top Brand Header Banner (Navy)
        float bannerHeight = 44f;
        float bannerY = TOP_CURSOR - bannerHeight;
        doc.drawFilledRect(MARGIN_LEFT, bannerY, CONTENT_WIDTH, bannerHeight, COLOR_PRIMARY_NAVY);

        doc.drawText("FINWISE", MARGIN_LEFT + 14f, bannerY + 24f, FontType.BOLD, 14f, COLOR_WHITE);
        doc.drawText("ENTERPRISE PERSONAL FINANCE PLATFORM", MARGIN_LEFT + 14f, bannerY + 11f, FontType.REGULAR, 7.5f, COLOR_MUTED_GRAY);
        doc.drawText("CONFIDENTIAL FINANCIAL REPORT", MARGIN_RIGHT - 165f, bannerY + 17f, FontType.BOLD, 8f, COLOR_WHITE);

        doc.setCursorY(bannerY - 18f);

        // Report Main Title
        doc.drawText(title, MARGIN_LEFT, doc.getCursorY(), FontType.BOLD, 14f, COLOR_PRIMARY_NAVY);
        doc.setCursorY(doc.getCursorY() - 8f);

        // Divider
        doc.drawLine(MARGIN_LEFT, doc.getCursorY(), MARGIN_RIGHT, doc.getCursorY(), COLOR_BORDER_LINE, 1f);
        doc.setCursorY(doc.getCursorY() - 14f);

        // Metadata Grid (2-column layout in bordered box)
        float metaBoxHeight = 36f;
        float metaY = doc.getCursorY() - metaBoxHeight;
        doc.drawFilledRect(MARGIN_LEFT, metaY, CONTENT_WIDTH, metaBoxHeight, COLOR_ROW_ALT);
        doc.drawRectBorder(MARGIN_LEFT, metaY, CONTENT_WIDTH, metaBoxHeight, COLOR_BORDER_LINE, 0.75f);

        float col1 = MARGIN_LEFT + 12f;
        float col2 = MARGIN_LEFT + (CONTENT_WIDTH / 2f) + 12f;

        doc.drawText("Account Holder: " + sanitize(meta.getUserName()) + " (" + sanitize(meta.getUserEmail()) + ")",
                col1, metaY + 22f, FontType.BOLD, 8.5f, COLOR_TEXT_DARK);
        doc.drawText("Date Range: " + sanitize(meta.getDateRangeText()),
                col1, metaY + 9f, FontType.REGULAR, 8.5f, COLOR_MUTED_GRAY);

        doc.drawText("Generated On: " + sanitize(meta.getGeneratedAt()),
                col2, metaY + 22f, FontType.REGULAR, 8.5f, COLOR_MUTED_GRAY);
        doc.drawText("Currency: " + sanitize(meta.getCurrencyCode()) + " (" + sanitize(meta.getCurrencySymbol()) + ")",
                col2, metaY + 9f, FontType.BOLD, 8.5f, COLOR_TEXT_DARK);

        doc.setCursorY(metaY - 18f);
    }

    private void drawSectionTitle(PdfDocument doc, String title) {
        ensureVerticalSpace(doc, 32f);
        doc.drawText(title, MARGIN_LEFT, doc.getCursorY(), FontType.BOLD, 10.5f, COLOR_PRIMARY_NAVY);
        float lineY = doc.getCursorY() - 4f;
        doc.drawLine(MARGIN_LEFT, lineY, MARGIN_LEFT + 70f, lineY, COLOR_PRIMARY_NAVY, 1.5f);
        doc.drawLine(MARGIN_LEFT + 70f, lineY, MARGIN_RIGHT, lineY, COLOR_BORDER_LINE, 0.5f);
        doc.setCursorY(lineY - 12f);
    }

    private void drawKpiCards(PdfDocument doc, List<KpiCard> cards) {
        if (cards == null || cards.isEmpty()) return;
        ensureVerticalSpace(doc, 48f);

        int count = cards.size();
        float gap = 8f;
        float cardWidth = (CONTENT_WIDTH - (gap * (count - 1))) / count;
        float cardHeight = 40f;
        float cardY = doc.getCursorY() - cardHeight;

        for (int i = 0; i < count; i++) {
            KpiCard card = cards.get(i);
            float cardX = MARGIN_LEFT + (i * (cardWidth + gap));

            // Card background and subtle border
            doc.drawFilledRect(cardX, cardY, cardWidth, cardHeight, COLOR_ROW_ALT);
            doc.drawRectBorder(cardX, cardY, cardWidth, cardHeight, COLOR_BORDER_LINE, 0.75f);

            // Left accent bar
            doc.drawFilledRect(cardX, cardY, 3f, cardHeight, card.valueColor != null ? card.valueColor : COLOR_PRIMARY_NAVY);

            // Label
            doc.drawText(sanitize(card.label), cardX + 8f, cardY + 25f, FontType.BOLD, 7f, COLOR_MUTED_GRAY);

            // Value
            doc.drawText(sanitize(card.value), cardX + 8f, cardY + 10f, FontType.BOLD, 10f,
                    card.valueColor != null ? card.valueColor : COLOR_TEXT_DARK);
        }

        doc.setCursorY(cardY - 16f);
    }

    private void drawTable(PdfDocument doc, String[] headers, float[] colWidths, List<String[]> rows, boolean[] rightAlign, String emptyMessage) {
        ensureVerticalSpace(doc, 45f);

        float headerHeight = 20f;
        float rowHeight = 16f;

        // Render Header Row
        drawTableHeaderRow(doc, headers, colWidths, rightAlign, headerHeight);

        if (rows == null || rows.isEmpty()) {
            ensureVerticalSpace(doc, rowHeight);
            float emptyY = doc.getCursorY() - rowHeight;
            doc.drawFilledRect(MARGIN_LEFT, emptyY, CONTENT_WIDTH, rowHeight, COLOR_ROW_ALT);
            doc.drawRectBorder(MARGIN_LEFT, emptyY, CONTENT_WIDTH, rowHeight, COLOR_BORDER_LINE, 0.5f);
            doc.drawText(emptyMessage, MARGIN_LEFT + 12f, emptyY + 5f, FontType.ITALIC, 8.5f, COLOR_MUTED_GRAY);
            doc.setCursorY(emptyY - 14f);
            return;
        }

        // Render Data Rows with pagination
        for (int r = 0; r < rows.size(); r++) {
            String[] rowData = rows.get(r);

            // Check if page break needed
            if (doc.getCursorY() - rowHeight < MARGIN_BOTTOM + 20f) {
                doc.beginPage();
                doc.setCursorY(TOP_CURSOR - 20f);
                // Re-draw table header on subsequent page
                drawTableHeaderRow(doc, headers, colWidths, rightAlign, headerHeight);
            }

            float rowY = doc.getCursorY() - rowHeight;
            float[] bgColor = (r % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;

            doc.drawFilledRect(MARGIN_LEFT, rowY, CONTENT_WIDTH, rowHeight, bgColor);
            doc.drawRectBorder(MARGIN_LEFT, rowY, CONTENT_WIDTH, rowHeight, COLOR_BORDER_LINE, 0.4f);

            float currentX = MARGIN_LEFT;
            for (int c = 0; c < rowData.length; c++) {
                float w = colWidths[c];
                boolean alignRight = rightAlign != null && c < rightAlign.length && rightAlign[c];
                String text = sanitize(rowData[c]);

                if (alignRight) {
                    float textWidth = estimateTextWidth(text, 8f);
                    float textX = currentX + w - textWidth - 6f;
                    doc.drawText(text, Math.max(currentX + 4f, textX), rowY + 5f, FontType.REGULAR, 8f, COLOR_TEXT_DARK);
                } else {
                    doc.drawText(truncate(text, (int)(w / 4.8f)), currentX + 6f, rowY + 5f, FontType.REGULAR, 8f, COLOR_TEXT_DARK);
                }
                currentX += w;
            }

            doc.setCursorY(rowY);
        }

        doc.setCursorY(doc.getCursorY() - 14f);
    }

    private void drawTableHeaderRow(PdfDocument doc, String[] headers, float[] colWidths, boolean[] rightAlign, float headerHeight) {
        float headerY = doc.getCursorY() - headerHeight;
        doc.drawFilledRect(MARGIN_LEFT, headerY, CONTENT_WIDTH, headerHeight, COLOR_SLATE_HEADER);

        float currentX = MARGIN_LEFT;
        for (int i = 0; i < headers.length; i++) {
            float w = colWidths[i];
            boolean alignRight = rightAlign != null && i < rightAlign.length && rightAlign[i];
            String text = sanitize(headers[i]);

            if (alignRight) {
                float textWidth = estimateTextWidth(text, 8f);
                float textX = currentX + w - textWidth - 6f;
                doc.drawText(text, Math.max(currentX + 4f, textX), headerY + 6f, FontType.BOLD, 8f, COLOR_WHITE);
            } else {
                doc.drawText(text, currentX + 6f, headerY + 6f, FontType.BOLD, 8f, COLOR_WHITE);
            }
            currentX += w;
        }

        doc.setCursorY(headerY);
    }

    private void drawBulletPoint(PdfDocument doc, String text) {
        ensureVerticalSpace(doc, 16f);
        float y = doc.getCursorY() - 12f;
        doc.drawFilledRect(MARGIN_LEFT + 4f, y + 3f, 3f, 3f, COLOR_PRIMARY_NAVY);
        doc.drawText(sanitize(text), MARGIN_LEFT + 14f, y + 1f, FontType.REGULAR, 8.5f, COLOR_TEXT_DARK);
        doc.setCursorY(y - 4f);
    }

    private void drawEmptyStateBox(PdfDocument doc, String message) {
        float boxHeight = 40f;
        float boxY = doc.getCursorY() - boxHeight;
        doc.drawFilledRect(MARGIN_LEFT, boxY, CONTENT_WIDTH, boxHeight, COLOR_ROW_ALT);
        doc.drawRectBorder(MARGIN_LEFT, boxY, CONTENT_WIDTH, boxHeight, COLOR_BORDER_LINE, 0.75f);
        doc.drawText(sanitize(message), MARGIN_LEFT + 16f, boxY + 16f, FontType.REGULAR, 9f, COLOR_MUTED_GRAY);
        doc.setCursorY(boxY - 14f);
    }

    private void ensureVerticalSpace(PdfDocument doc, float needed) {
        if (doc.getCursorY() - needed < MARGIN_BOTTOM + 20f) {
            doc.beginPage();
            doc.setCursorY(TOP_CURSOR - 20f);
        }
    }

    private float estimateTextWidth(String text, float fontSize) {
        if (text == null) return 0f;
        return text.length() * (fontSize * 0.52f);
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, Math.max(1, maxChars - 2)) + "..";
    }

    private String sanitize(String text) {
        if (text == null) return "";
        // Replace unicode Rupee sign with standard "Rs."
        return text.replace("₹", "Rs. ")
                   .replace("\n", " ")
                   .replace("\r", "")
                   .trim();
    }

    // ==========================================
    // INTERNAL DATA STRUCTURES & PDF CANVAS
    // ==========================================

    private static class KpiCard {
        final String label;
        final String value;
        final float[] valueColor;

        KpiCard(String label, String value, float[] valueColor) {
            this.label = label;
            this.value = value;
            this.valueColor = valueColor;
        }
    }

    private enum FontType {
        REGULAR("/F1"),
        BOLD("/F2"),
        ITALIC("/F3");

        final String pdfName;
        FontType(String pdfName) {
            this.pdfName = pdfName;
        }
    }

    /**
     * Pure Java in-memory PDF Document Builder
     */
    private static class PdfDocument {
        private final List<StringBuilder> pageStreams = new ArrayList<>();
        private StringBuilder currentStream;
        private float cursorY = TOP_CURSOR;

        public void beginPage() {
            currentStream = new StringBuilder();
            pageStreams.add(currentStream);
            cursorY = TOP_CURSOR;
        }

        public float getCursorY() {
            return cursorY;
        }

        public void setCursorY(float y) {
            this.cursorY = y;
        }

        public void drawFilledRect(float x, float y, float w, float h, float[] rgb) {
            currentStream.append(String.format(Locale.US, "%.3f %.3f %.3f rg\n", rgb[0], rgb[1], rgb[2]))
                         .append(String.format(Locale.US, "%.2f %.2f %.2f %.2f re f\n", x, y, w, h));
        }

        public void drawRectBorder(float x, float y, float w, float h, float[] rgb, float lineWidth) {
            currentStream.append(String.format(Locale.US, "%.2f w\n", lineWidth))
                         .append(String.format(Locale.US, "%.3f %.3f %.3f RG\n", rgb[0], rgb[1], rgb[2]))
                         .append(String.format(Locale.US, "%.2f %.2f %.2f %.2f re s\n", x, y, w, h));
        }

        public void drawLine(float x1, float y1, float x2, float y2, float[] rgb, float lineWidth) {
            currentStream.append(String.format(Locale.US, "%.2f w\n", lineWidth))
                         .append(String.format(Locale.US, "%.3f %.3f %.3f RG\n", rgb[0], rgb[1], rgb[2]))
                         .append(String.format(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", x1, y1, x2, y2));
        }

        public void drawText(String text, float x, float y, FontType font, float size, float[] rgb) {
            if (text == null || text.isEmpty()) return;
            String escaped = escapePdfString(text);
            currentStream.append(String.format(Locale.US, "%.3f %.3f %.3f rg\n", rgb[0], rgb[1], rgb[2]))
                         .append("BT\n")
                         .append(String.format(Locale.US, "%s %.2f Tf\n", font.pdfName, size))
                         .append(String.format(Locale.US, "%.2f %.2f Td\n", x, y))
                         .append("(")
                         .append(escaped)
                         .append(") Tj\n")
                         .append("ET\n");
        }

        private String escapePdfString(String str) {
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                if (c == '(') sb.append("\\(");
                else if (c == ')') sb.append("\\)");
                else if (c == '\\') sb.append("\\\\");
                else if (c >= 32 && c <= 126) sb.append(c);
                else sb.append(' '); // replace non-ASCII with space
            }
            return sb.toString();
        }

        public void finishDocument() {
            // Draw footers on all pages
            int totalPages = pageStreams.size();
            for (int i = 0; i < totalPages; i++) {
                StringBuilder ps = pageStreams.get(i);
                float lineY = MARGIN_BOTTOM;
                ps.append(String.format(Locale.US, "0.50 w\n%.3f %.3f %.3f RG\n",
                        COLOR_BORDER_LINE[0], COLOR_BORDER_LINE[1], COLOR_BORDER_LINE[2]))
                  .append(String.format(Locale.US, "%.2f %.2f m %.2f %.2f l S\n",
                        MARGIN_LEFT, lineY, MARGIN_RIGHT, lineY));

                // Left footer text
                String leftFoot = "Generated by FinWise Financial Intelligence - Confidential";
                ps.append(String.format(Locale.US, "%.3f %.3f %.3f rg\nBT\n/F1 7.50 Tf\n%.2f %.2f Td\n(%s) Tj\nET\n",
                        COLOR_MUTED_GRAY[0], COLOR_MUTED_GRAY[1], COLOR_MUTED_GRAY[2],
                        MARGIN_LEFT, lineY - 14f, leftFoot));

                // Right footer text: Page X of Y
                String rightFoot = String.format("Page %d of %d", i + 1, totalPages);
                float rfWidth = rightFoot.length() * (7.5f * 0.55f);
                ps.append(String.format(Locale.US, "%.3f %.3f %.3f rg\nBT\n/F2 7.50 Tf\n%.2f %.2f Td\n(%s) Tj\nET\n",
                        COLOR_MUTED_GRAY[0], COLOR_MUTED_GRAY[1], COLOR_MUTED_GRAY[2],
                        MARGIN_RIGHT - rfWidth, lineY - 14f, rightFoot));
            }
        }

        public byte[] toByteArray() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                int numPages = pageStreams.size();

                // Object 1: Catalog
                // Object 2: Pages container
                // Object 3: Resources (Fonts)
                // Pages: Object index 4 + (2 * i) = Page, 4 + (2 * i) + 1 = Content
                int totalObjects = 3 + (numPages * 2);

                List<Long> byteOffsets = new ArrayList<>();
                byteOffsets.add(0L); // 0-th object placeholder

                writeString(out, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

                // Obj 1: Catalog
                byteOffsets.add((long) out.size());
                writeString(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

                // Obj 2: Pages
                byteOffsets.add((long) out.size());
                StringBuilder kids = new StringBuilder();
                for (int i = 0; i < numPages; i++) {
                    int pageObjNum = 4 + (i * 2);
                    kids.append(pageObjNum).append(" 0 R ");
                }
                writeString(out, String.format(Locale.US,
                        "2 0 obj\n<< /Type /Pages /Kids [%s] /Count %d >>\nendobj\n",
                        kids.toString().trim(), numPages));

                // Obj 3: Font Resources
                byteOffsets.add((long) out.size());
                writeString(out, "3 0 obj\n" +
                        "<< /Font <<\n" +
                        "  /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\n" +
                        "  /F2 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\n" +
                        "  /F3 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique >>\n" +
                        ">> >>\nendobj\n");

                // Page Objects & Content Streams
                for (int i = 0; i < numPages; i++) {
                    int pageObjNum = 4 + (i * 2);
                    int contentObjNum = pageObjNum + 1;

                    byte[] contentBytes = pageStreams.get(i).toString().getBytes(StandardCharsets.US_ASCII);

                    // Page Dict
                    byteOffsets.add((long) out.size());
                    writeString(out, String.format(Locale.US,
                            "%d 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f] /Contents %d 0 R /Resources 3 0 R >>\nendobj\n",
                            pageObjNum, PAGE_WIDTH, PAGE_HEIGHT, contentObjNum));

                    // Content Stream
                    byteOffsets.add((long) out.size());
                    writeString(out, String.format(Locale.US,
                            "%d 0 obj\n<< /Length %d >>\nstream\n", contentObjNum, contentBytes.length));
                    out.write(contentBytes);
                    writeString(out, "\nendstream\nendobj\n");
                }

                // XREF Table
                long xrefOffset = out.size();
                writeString(out, String.format(Locale.US, "xref\n0 %d\n0000000000 65535 f \n", totalObjects + 1));
                for (int i = 1; i <= totalObjects; i++) {
                    writeString(out, String.format(Locale.US, "%010d 00000 n \n", byteOffsets.get(i)));
                }

                // Trailer
                writeString(out, String.format(Locale.US,
                        "trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n",
                        totalObjects + 1, xrefOffset));

            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize PDF document: " + e.getMessage(), e);
            }
            return out.toByteArray();
        }

        private void writeString(ByteArrayOutputStream out, String str) throws IOException {
            out.write(str.getBytes(StandardCharsets.US_ASCII));
        }
    }
}
