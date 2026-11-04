package com.finwise.service;

import com.finwise.dto.report.*;

import java.time.LocalDate;
import java.util.Map;

public interface ReportService {

    MonthlyReportData generateMonthlyReport(Long userId, Integer year, Integer month);

    AnnualReportData generateAnnualReport(Long userId, Integer year);

    CategoryReportData generateCategoryReport(Long userId, LocalDate startDate, LocalDate endDate, Long categoryId);

    IncomeExpenseReportData generateIncomeExpenseReport(Long userId, LocalDate startDate, LocalDate endDate);

    SavingsReportData generateSavingsReport(Long userId, LocalDate startDate, LocalDate endDate);

    BudgetPerformanceReportData generateBudgetPerformanceReport(Long userId, Long budgetId, Integer year, Integer month);

    byte[] exportReport(ReportType type, ExportFormat format, Long userId, Map<String, Object> params);

    String getFilename(ReportType type, ExportFormat format, Map<String, Object> params);
}
