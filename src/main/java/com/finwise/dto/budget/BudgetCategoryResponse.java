package com.finwise.dto.budget;

import com.finwise.entity.BudgetAlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetCategoryResponse {

    private Long categoryId;
    private String categoryName;
    private String colorCode;
    private String icon;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private double utilizationPercentage;
    private BudgetAlertStatus alertStatus;
}
