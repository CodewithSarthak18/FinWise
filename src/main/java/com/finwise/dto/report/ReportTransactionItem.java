package com.finwise.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTransactionItem {
    private String date;
    private String description;
    private String category;
    private String account;
    private String type;
    private String paymentMethod;
    private BigDecimal amount;
    private String formattedAmount;
}
