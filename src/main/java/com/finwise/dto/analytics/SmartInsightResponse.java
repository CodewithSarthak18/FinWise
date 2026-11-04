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
public class SmartInsightResponse {

    private String period;
    private boolean hasEnoughData;
    private int totalInsightsCount;
    private List<SmartInsightItem> insights;
}
