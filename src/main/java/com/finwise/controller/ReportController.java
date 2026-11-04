package com.finwise.controller;

import com.finwise.dto.report.*;
import com.finwise.dto.response.ApiResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;

    // ==========================================
    // 1. MONTHLY FINANCIAL REPORT
    // ==========================================
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExportFormat exportFormat = parseFormat(format);
        if (exportFormat == ExportFormat.JSON) {
            MonthlyReportData data = reportService.generateMonthlyReport(principal.getId(), year, month);
            return ResponseEntity.ok(ApiResponse.success(data));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("year", year);
        params.put("month", month);

        byte[] bytes = reportService.exportReport(ReportType.MONTHLY, exportFormat, principal.getId(), params);
        String filename = reportService.getFilename(ReportType.MONTHLY, exportFormat, params);
        return buildFileResponse(bytes, exportFormat, filename);
    }

    // ==========================================
    // 2. ANNUAL FINANCIAL REPORT
    // ==========================================
    @GetMapping("/annual")
    public ResponseEntity<?> getAnnualReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExportFormat exportFormat = parseFormat(format);
        if (exportFormat == ExportFormat.JSON) {
            AnnualReportData data = reportService.generateAnnualReport(principal.getId(), year);
            return ResponseEntity.ok(ApiResponse.success(data));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("year", year);

        byte[] bytes = reportService.exportReport(ReportType.ANNUAL, exportFormat, principal.getId(), params);
        String filename = reportService.getFilename(ReportType.ANNUAL, exportFormat, params);
        return buildFileResponse(bytes, exportFormat, filename);
    }

    // ==========================================
    // 3. CATEGORY-WISE REPORT
    // ==========================================
    @GetMapping("/category-wise")
    public ResponseEntity<?> getCategoryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExportFormat exportFormat = parseFormat(format);
        if (exportFormat == ExportFormat.JSON) {
            CategoryReportData data = reportService.generateCategoryReport(principal.getId(), startDate, endDate, categoryId);
            return ResponseEntity.ok(ApiResponse.success(data));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("categoryId", categoryId);

        byte[] bytes = reportService.exportReport(ReportType.CATEGORY_WISE, exportFormat, principal.getId(), params);
        String filename = reportService.getFilename(ReportType.CATEGORY_WISE, exportFormat, params);
        return buildFileResponse(bytes, exportFormat, filename);
    }

    // ==========================================
    // 4. INCOME AND EXPENSE SUMMARY
    // ==========================================
    @GetMapping("/income-expense")
    public ResponseEntity<?> getIncomeExpenseReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExportFormat exportFormat = parseFormat(format);
        if (exportFormat == ExportFormat.JSON) {
            IncomeExpenseReportData data = reportService.generateIncomeExpenseReport(principal.getId(), startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(data));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        byte[] bytes = reportService.exportReport(ReportType.INCOME_EXPENSE, exportFormat, principal.getId(), params);
        String filename = reportService.getFilename(ReportType.INCOME_EXPENSE, exportFormat, params);
        return buildFileResponse(bytes, exportFormat, filename);
    }

    // ==========================================
    // 5. SAVINGS SUMMARY REPORT
    // ==========================================
    @GetMapping("/savings")
    public ResponseEntity<?> getSavingsReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExportFormat exportFormat = parseFormat(format);
        if (exportFormat == ExportFormat.JSON) {
            SavingsReportData data = reportService.generateSavingsReport(principal.getId(), startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(data));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        byte[] bytes = reportService.exportReport(ReportType.SAVINGS, exportFormat, principal.getId(), params);
        String filename = reportService.getFilename(ReportType.SAVINGS, exportFormat, params);
        return buildFileResponse(bytes, exportFormat, filename);
    }

    // ==========================================
    // 6. BUDGET PERFORMANCE REPORT
    // ==========================================
    @GetMapping("/budget-performance")
    public ResponseEntity<?> getBudgetPerformanceReport(
            @RequestParam(required = false) Long budgetId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExportFormat exportFormat = parseFormat(format);
        if (exportFormat == ExportFormat.JSON) {
            BudgetPerformanceReportData data = reportService.generateBudgetPerformanceReport(principal.getId(), budgetId, year, month);
            return ResponseEntity.ok(ApiResponse.success(data));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("budgetId", budgetId);
        params.put("year", year);
        params.put("month", month);

        byte[] bytes = reportService.exportReport(ReportType.BUDGET_PERFORMANCE, exportFormat, principal.getId(), params);
        String filename = reportService.getFilename(ReportType.BUDGET_PERFORMANCE, exportFormat, params);
        return buildFileResponse(bytes, exportFormat, filename);
    }

    // ==========================================
    // RESPONSE BUILDERS
    // ==========================================

    private ExportFormat parseFormat(String format) {
        if (format == null) return ExportFormat.PDF;
        try {
            return ExportFormat.valueOf(format.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ExportFormat.PDF;
        }
    }

    private ResponseEntity<byte[]> buildFileResponse(byte[] content, ExportFormat format, String filename) {
        MediaType mediaType = (format == ExportFormat.CSV) ?
                MediaType.parseMediaType("text/csv; charset=UTF-8") :
                MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(content);
    }
}
