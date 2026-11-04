package com.finwise.controller;

import com.finwise.dto.response.ApiResponse;
import com.finwise.dto.whatif.CategorySpendingOption;
import com.finwise.dto.whatif.WhatIfPresetScenario;
import com.finwise.dto.whatif.WhatIfSimulationRequest;
import com.finwise.dto.whatif.WhatIfSimulationResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.WhatIfCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calculator/what-if")
@RequiredArgsConstructor
public class WhatIfCalculatorController {

    private static final Logger log = LoggerFactory.getLogger(WhatIfCalculatorController.class);
    private final WhatIfCalculatorService whatIfCalculatorService;

    @PostMapping
    public ResponseEntity<ApiResponse<WhatIfSimulationResponse>> simulateSavings(
            @Valid @RequestBody WhatIfSimulationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Running what-if simulation for user id {}, category id {}, reduction: {}%",
                principal.getId(), request.getCategoryId(), request.getReductionPercentage());
        WhatIfSimulationResponse response = whatIfCalculatorService.simulateSavings(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Simulation calculated successfully", response));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategorySpendingOption>>> getSpendingCategories(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseMonth(month);
        log.debug("Fetching category spending options for user id {}, month {}", principal.getId(), ym);
        List<CategorySpendingOption> options = whatIfCalculatorService.getSpendingCategories(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(options));
    }

    @GetMapping("/presets")
    public ResponseEntity<ApiResponse<List<WhatIfPresetScenario>>> getPresetScenarios(
            @RequestParam Long categoryId,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseMonth(month);
        log.debug("Fetching preset scenarios for user id {}, category id {}, month {}",
                principal.getId(), categoryId, ym);
        List<WhatIfPresetScenario> presets = whatIfCalculatorService.getPresetScenarios(principal.getId(), categoryId, ym);
        return ResponseEntity.ok(ApiResponse.success(presets));
    }

    private YearMonth parseMonth(String monthStr) {
        if (monthStr == null || monthStr.isBlank()) {
            return YearMonth.now(ZoneOffset.UTC);
        }
        try {
            return YearMonth.parse(monthStr.trim());
        } catch (Exception e) {
            log.warn("Invalid month string '{}', defaulting to current month", monthStr);
            return YearMonth.now(ZoneOffset.UTC);
        }
    }
}
