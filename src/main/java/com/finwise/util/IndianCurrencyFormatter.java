package com.finwise.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility for Indian currency and number formatting following the Lakh/Crore system.
 * Formats:
 *  - 1,25,000.00
 *  - Rs. 1,25,000.00
 *  - ₹1,25,000.00
 */
public final class IndianCurrencyFormatter {

    private IndianCurrencyFormatter() {
        // Utility class
    }

    /**
     * Formats a BigDecimal using Indian numbering grouping: ##,##,##0.00
     */
    public static String formatNumber(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        boolean isNegative = amount.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal absAmount = amount.abs().setScale(2, RoundingMode.HALF_EVEN);
        String plain = absAmount.toPlainString();
        String[] parts = plain.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "00";

        if (integerPart.length() <= 3) {
            return (isNegative ? "-" : "") + integerPart + "." + decimalPart;
        }

        String lastThree = integerPart.substring(integerPart.length() - 3);
        String remaining = integerPart.substring(0, integerPart.length() - 3);

        StringBuilder sb = new StringBuilder();
        while (remaining.length() > 2) {
            sb.insert(0, "," + remaining.substring(remaining.length() - 2));
            remaining = remaining.substring(0, remaining.length() - 2);
        }
        sb.insert(0, remaining);
        sb.append(",").append(lastThree).append(".").append(decimalPart);

        return (isNegative ? "-" : "") + sb.toString();
    }

    /**
     * Standard ASCII representation safe for standard Type 1 Helvetica fonts in PDF
     * e.g. "Rs. 1,25,000.00"
     */
    public static String formatINR(BigDecimal amount) {
        return "Rs. " + formatNumber(amount);
    }

    /**
     * UTF-8 Rupee symbol representation for Web and CSV
     * e.g. "₹1,25,000.00"
     */
    public static String formatINRWithSymbol(BigDecimal amount) {
        return "₹" + formatNumber(amount);
    }

    /**
     * Formats double percentage to 2 decimal places with % sign
     * e.g. "24.50%"
     */
    public static String formatPercentage(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
