package com.finwise.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportMetadata {
    private String title;
    private ReportType reportType;
    private String userName;
    private String userEmail;
    private String generatedAt;
    private Instant generatedTimestamp;
    private String dateRangeText;
    private String startDate;
    private String endDate;
    @Builder.Default
    private String currencyCode = "INR";
    @Builder.Default
    private String currencySymbol = "Rs.";
}
