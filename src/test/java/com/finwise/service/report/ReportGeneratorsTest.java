package com.finwise.service.report;

import com.finwise.dto.report.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGeneratorsTest {

    private PdfReportGenerator pdfGenerator;
    private CsvReportGenerator csvGenerator;
    private ReportMetadata metadata;

    @BeforeEach
    void setUp() {
        pdfGenerator = new PdfReportGenerator();
        csvGenerator = new CsvReportGenerator();

        metadata = ReportMetadata.builder()
                .title("Test Report")
                .reportType(ReportType.MONTHLY)
                .userName("Rahul Verma")
                .userEmail("rahul.verma@example.com")
                .generatedAt("03 Nov 2026, 12:45 PM")
                .generatedTimestamp(Instant.now())
                .dateRangeText("01 Nov 2026 to 30 Nov 2026")
                .startDate("2026-11-01")
                .endDate("2026-11-30")
                .currencyCode("INR")
                .currencySymbol("Rs.")
                .build();
    }

    @Test
    @DisplayName("PDF Generator produces valid PDF-1.4 file with header, trailer and content")
    void testPdfGenerationValidity() {
        MonthlyReportData data = MonthlyReportData.builder()
                .metadata(metadata)
                .period("November 2026")
                .totalIncome(new BigDecimal("125000.00"))
                .formattedTotalIncome("Rs. 1,25,000.00")
                .totalExpenses(new BigDecimal("45000.00"))
                .formattedTotalExpenses("Rs. 45,000.00")
                .netSavings(new BigDecimal("80000.00"))
                .formattedNetSavings("Rs. 80,000.00")
                .savingsRate(64.0)
                .formattedSavingsRate("64.00%")
                .netWorth(new BigDecimal("500000.00"))
                .formattedNetWorth("Rs. 5,00,000.00")
                .transactionCount(1)
                .hasBudget(true)
                .budgetName("Monthly Master")
                .budgetAmount(new BigDecimal("60000.00"))
                .formattedBudgetAmount("Rs. 60,000.00")
                .budgetSpent(new BigDecimal("45000.00"))
                .formattedBudgetSpent("Rs. 45,000.00")
                .budgetRemaining(new BigDecimal("15000.00"))
                .formattedBudgetRemaining("Rs. 15,000.00")
                .budgetUtilization(75.0)
                .formattedBudgetUtilization("75.00%")
                .budgetStatus("NORMAL")
                .categoryBreakdown(List.of(
                        ReportCategorySummary.builder()
                                .categoryName("Groceries")
                                .type("EXPENSE")
                                .amount(new BigDecimal("15000.00"))
                                .formattedAmount("Rs. 15,000.00")
                                .transactionCount(3)
                                .percentageOfTotal(33.33)
                                .formattedPercentage("33.33%")
                                .build()
                ))
                .transactions(List.of(
                        ReportTransactionItem.builder()
                                .date("02 Nov 2026")
                                .description("Supermarket shopping")
                                .category("Groceries")
                                .account("HDFC Savings")
                                .type("EXPENSE")
                                .amount(new BigDecimal("5200.00"))
                                .formattedAmount("Rs. 5,200.00")
                                .build()
                ))
                .build();

        byte[] pdfBytes = pdfGenerator.generateMonthlyReport(data);

        assertThat(pdfBytes).isNotEmpty();
        String pdfHeader = new String(pdfBytes, 0, 8, StandardCharsets.US_ASCII);
        assertThat(pdfHeader).startsWith("%PDF-1.4");

        String fullPdf = new String(pdfBytes, StandardCharsets.US_ASCII);
        assertThat(fullPdf).contains("%%EOF");
        assertThat(fullPdf).contains("/Catalog");
        assertThat(fullPdf).contains("/Pages");
        assertThat(fullPdf).contains("FINWISE");
    }

    @Test
    @DisplayName("PDF Generator handles empty report gracefully without crashing")
    void testPdfGenerationWithEmptyData() {
        MonthlyReportData emptyData = MonthlyReportData.builder()
                .metadata(metadata)
                .period("November 2026")
                .totalIncome(BigDecimal.ZERO)
                .formattedTotalIncome("Rs. 0.00")
                .totalExpenses(BigDecimal.ZERO)
                .formattedTotalExpenses("Rs. 0.00")
                .netSavings(BigDecimal.ZERO)
                .formattedNetSavings("Rs. 0.00")
                .savingsRate(0.0)
                .formattedSavingsRate("0.00%")
                .netWorth(BigDecimal.ZERO)
                .formattedNetWorth("Rs. 0.00")
                .transactionCount(0)
                .hasBudget(false)
                .build();

        byte[] pdfBytes = pdfGenerator.generateMonthlyReport(emptyData);

        assertThat(pdfBytes).isNotEmpty();
        String fullPdf = new String(pdfBytes, StandardCharsets.US_ASCII);
        assertThat(fullPdf).startsWith("%PDF-1.4");
        assertThat(fullPdf).contains("%%EOF");
        assertThat(fullPdf).contains("No transactions recorded in this period");
    }

    @Test
    @DisplayName("CSV Generator produces RFC-4180 compliant CSV with metadata and sections")
    void testCsvGeneration() {
        MonthlyReportData data = MonthlyReportData.builder()
                .metadata(metadata)
                .period("November 2026")
                .totalIncome(new BigDecimal("125000.00"))
                .formattedTotalIncome("Rs. 1,25,000.00")
                .totalExpenses(new BigDecimal("45000.00"))
                .formattedTotalExpenses("Rs. 45,000.00")
                .netSavings(new BigDecimal("80000.00"))
                .formattedNetSavings("Rs. 80,000.00")
                .savingsRate(64.0)
                .formattedSavingsRate("64.00%")
                .netWorth(new BigDecimal("500000.00"))
                .formattedNetWorth("Rs. 5,00,000.00")
                .transactionCount(1)
                .hasBudget(false)
                .transactions(List.of(
                        ReportTransactionItem.builder()
                                .date("2026-11-02")
                                .description("Grocery, Provision & Supplies")
                                .category("Food & Dining")
                                .account("HDFC Bank")
                                .type("EXPENSE")
                                .paymentMethod("UPI")
                                .amount(new BigDecimal("5200.00"))
                                .formattedAmount("Rs. 5,200.00")
                                .build()
                ))
                .build();

        byte[] csvBytes = csvGenerator.generateMonthlyReport(data);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("# FINWISE PERSONAL FINANCE MANAGEMENT PLATFORM");
        assertThat(csv).contains("# Account      : Rahul Verma (rahul.verma@example.com)");
        assertThat(csv).contains("Total Income,\"Rs. 1,25,000.00\"");
        assertThat(csv).contains("\"Grocery, Provision & Supplies\""); // Escaped for commas
        assertThat(csv).contains("\"Rs. 5,200.00\"");
    }
}
