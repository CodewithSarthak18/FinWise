package com.finwise.entity;

public enum BudgetAlertStatus {
    NORMAL,        // < 80% usage (e.g. 50% normal progress)
    WARNING,       // >= 80% usage (approaching limit)
    EXCEEDED,      // >= 100% usage (limit reached)
    OVERSPENDING   // >= 120% usage (critical overspend)
}
