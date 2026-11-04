package com.finwise.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreComponent {

    private String name;
    private double earnedScore;
    private double maxScore;
    private int weightPercentage;
    private String metricValue;
    private String evaluation;
    private String recommendation;
}
