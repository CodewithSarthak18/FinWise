package com.finwise.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialHealthScoreResponse {

    private int overallScore; // 0 to 100
    private String ratingBand; // "EXCELLENT", "GOOD", "FAIR", "NEEDS_ATTENTION"
    private String summary;
    private ScoreComponent savingsRateComponent; // 40 points
    private ScoreComponent budgetAdherenceComponent; // 35 points
    private ScoreComponent spendingConsistencyComponent; // 25 points
    private String formulaExplanation;
    private String disclaimer;
    private List<String> keyActionItems;
}
