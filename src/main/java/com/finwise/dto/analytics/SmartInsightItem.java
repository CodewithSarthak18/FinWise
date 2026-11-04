package com.finwise.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartInsightItem {

    private String id;
    private String type; // CATEGORY_SURGE, BUDGET_ALERT, SAVINGS_IMPROVEMENT, DISCRETIONARY_OPTIMIZATION, SURPLUS_ALERT, INFO
    private String title;
    private String message;
    private String severity; // INFO, WARNING, SUCCESS, ALERT
    private boolean actionable;
    private String category;
}
