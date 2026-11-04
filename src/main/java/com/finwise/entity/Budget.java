package com.finwise.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "budgets",
    indexes = {
        @Index(name = "idx_budgets_user_dates", columnList = "user_id, start_date, end_date, is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    @Builder.Default
    private BudgetPeriodType periodType = BudgetPeriodType.MONTHLY;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Total budget amount cannot be null")
    @DecimalMin(value = "0.01", message = "Total budget amount must be strictly positive")
    @Column(name = "total_budget_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalBudgetAmount;

    @Column(name = "notification_threshold", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal notificationThreshold = new BigDecimal("80.00");

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetCategory> budgetCategories = new ArrayList<>();

    public void addBudgetCategory(BudgetCategory category) {
        budgetCategories.add(category);
        category.setBudget(this);
    }
}
