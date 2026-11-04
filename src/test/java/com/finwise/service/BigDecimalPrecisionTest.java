package com.finwise.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class BigDecimalPrecisionTest {

    @Test
    @DisplayName("Verify zero precision drift compared to IEEE-754 float/double")
    void verifyNoFloatingPointDrift() {
        // Floating point failure demo: 0.1 + 0.2 != 0.3
        double floatSum = 0.1 + 0.2;
        assertThat(floatSum).isNotEqualTo(0.3); // IEEE 754 produces 0.30000000000000004

        // FinWise BigDecimal guarantee: 0.10 + 0.20 == 0.30
        BigDecimal a = new BigDecimal("0.10");
        BigDecimal b = new BigDecimal("0.20");
        BigDecimal sum = a.add(b).setScale(2, RoundingMode.HALF_EVEN);
        assertThat(sum).isEqualByComparingTo("0.30");
    }

    @Test
    @DisplayName("Verify Banker's Rounding (HALF_EVEN) minimizes statistical bias")
    void verifyBankersRounding() {
        BigDecimal val1 = new BigDecimal("2.545");
        BigDecimal rounded1 = val1.setScale(2, RoundingMode.HALF_EVEN);
        assertThat(rounded1).isEqualByComparingTo("2.54"); // 4 is even, rounds down

        BigDecimal val2 = new BigDecimal("2.555");
        BigDecimal rounded2 = val2.setScale(2, RoundingMode.HALF_EVEN);
        assertThat(rounded2).isEqualByComparingTo("2.56"); // 5 is odd, rounds up to even 6
    }

    @Test
    @DisplayName("Verify large INR monetary calculations without overflow")
    void verifyLargeINRMonetaryCalculations() {
        // 50 Crores in INR: 50,00,00,000.00
        BigDecimal initialCorpus = new BigDecimal("500000000.00");
        BigDecimal monthlySIP = new BigDecimal("125000.50"); // 1.25 Lakhs
        BigDecimal months = new BigDecimal("120"); // 10 years

        BigDecimal totalInvested = initialCorpus.add(monthlySIP.multiply(months));
        assertThat(totalInvested).isEqualByComparingTo("515000060.00");
    }
}
