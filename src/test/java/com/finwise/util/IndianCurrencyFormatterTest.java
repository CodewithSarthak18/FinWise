package com.finwise.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IndianCurrencyFormatterTest {

    @Test
    @DisplayName("Format numbers using Indian Lakh and Crore grouping")
    void testIndianNumberFormatting() {
        // Zero
        assertThat(IndianCurrencyFormatter.formatNumber(BigDecimal.ZERO)).isEqualTo("0.00");
        assertThat(IndianCurrencyFormatter.formatNumber(null)).isEqualTo("0.00");

        // Hundreds
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("500.00"))).isEqualTo("500.00");
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("999.99"))).isEqualTo("999.99");

        // Thousands
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("1234.56"))).isEqualTo("1,234.56");
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("15000.00"))).isEqualTo("15,000.00");

        // Lakhs
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("125000.00"))).isEqualTo("1,25,000.00");
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("1250000.00"))).isEqualTo("12,50,000.00");

        // Crores
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("10000000.00"))).isEqualTo("1,00,00,000.00");
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("125000000.50"))).isEqualTo("12,50,00,000.50");

        // Negative numbers
        assertThat(IndianCurrencyFormatter.formatNumber(new BigDecimal("-45000.25"))).isEqualTo("-45,000.25");
    }

    @Test
    @DisplayName("Format INR currency with Rs. and Rupee symbol")
    void testCurrencyPrefixes() {
        BigDecimal val = new BigDecimal("125000.00");
        assertThat(IndianCurrencyFormatter.formatINR(val)).isEqualTo("Rs. 1,25,000.00");
        assertThat(IndianCurrencyFormatter.formatINRWithSymbol(val)).isEqualTo("₹1,25,000.00");
    }

    @Test
    @DisplayName("Format percentage")
    void testPercentageFormatting() {
        assertThat(IndianCurrencyFormatter.formatPercentage(24.5)).isEqualTo("24.50%");
        assertThat(IndianCurrencyFormatter.formatPercentage(0.0)).isEqualTo("0.00%");
        assertThat(IndianCurrencyFormatter.formatPercentage(100.0)).isEqualTo("100.00%");
    }
}
