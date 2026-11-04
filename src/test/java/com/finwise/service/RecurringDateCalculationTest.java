package com.finwise.service;

import com.finwise.entity.RecurringFrequency;
import com.finwise.service.impl.RecurringTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Recurring Transaction Date Calculation Unit Tests")
class RecurringDateCalculationTest {

    private RecurringTransactionServiceImpl recurringService;

    @BeforeEach
    void setUp() {
        // Instantiate without mock repositories as calculateNextExecutionDate is purely algorithmic
        recurringService = new RecurringTransactionServiceImpl(
                null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Should correctly calculate next daily date")
    void shouldCalculateDailyDate() {
        LocalDate current = LocalDate.of(2026, 3, 15);
        LocalDate next = recurringService.calculateNextExecutionDate(current, RecurringFrequency.DAILY);
        assertThat(next).isEqualTo(LocalDate.of(2026, 3, 16));
    }

    @Test
    @DisplayName("Should correctly calculate next weekly date")
    void shouldCalculateWeeklyDate() {
        LocalDate current = LocalDate.of(2026, 3, 1);
        LocalDate next = recurringService.calculateNextExecutionDate(current, RecurringFrequency.WEEKLY);
        assertThat(next).isEqualTo(LocalDate.of(2026, 3, 8));
    }

    @Test
    @DisplayName("Should correctly calculate next bi-weekly date")
    void shouldCalculateBiWeeklyDate() {
        LocalDate current = LocalDate.of(2026, 3, 1);
        LocalDate next = recurringService.calculateNextExecutionDate(current, RecurringFrequency.BI_WEEKLY);
        assertThat(next).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    @DisplayName("Should correctly calculate standard monthly date")
    void shouldCalculateStandardMonthlyDate() {
        LocalDate current = LocalDate.of(2026, 5, 10);
        LocalDate next = recurringService.calculateNextExecutionDate(current, RecurringFrequency.MONTHLY);
        assertThat(next).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    @DisplayName("Should handle month-end transition from Jan 31 to Feb 28 in non-leap year")
    void shouldHandleJan31ToFeb28NonLeapYear() {
        LocalDate jan31 = LocalDate.of(2026, 1, 31);
        LocalDate febNext = recurringService.calculateNextExecutionDate(jan31, RecurringFrequency.MONTHLY);
        assertThat(febNext).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("Should handle month-end transition from Jan 31 to Feb 29 in leap year (2024)")
    void shouldHandleJan31ToFeb29LeapYear() {
        LocalDate jan31Leap = LocalDate.of(2024, 1, 31);
        LocalDate febNextLeap = recurringService.calculateNextExecutionDate(jan31Leap, RecurringFrequency.MONTHLY);
        assertThat(febNextLeap).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    @DisplayName("Should handle 31-day month to 30-day month (March 31 to April 30)")
    void shouldHandleMarch31ToApril30() {
        LocalDate mar31 = LocalDate.of(2026, 3, 31);
        LocalDate aprNext = recurringService.calculateNextExecutionDate(mar31, RecurringFrequency.MONTHLY);
        assertThat(aprNext).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("Should handle 31-day month to 30-day month (August 31 to September 30)")
    void shouldHandleAugust31ToSeptember30() {
        LocalDate aug31 = LocalDate.of(2026, 8, 31);
        LocalDate sepNext = recurringService.calculateNextExecutionDate(aug31, RecurringFrequency.MONTHLY);
        assertThat(sepNext).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    @DisplayName("Should correctly calculate quarterly date")
    void shouldCalculateQuarterlyDate() {
        LocalDate current = LocalDate.of(2026, 1, 15);
        LocalDate next = recurringService.calculateNextExecutionDate(current, RecurringFrequency.QUARTERLY);
        assertThat(next).isEqualTo(LocalDate.of(2026, 4, 15));
    }

    @Test
    @DisplayName("Should correctly calculate yearly date")
    void shouldCalculateYearlyDate() {
        LocalDate current = LocalDate.of(2026, 7, 1);
        LocalDate next = recurringService.calculateNextExecutionDate(current, RecurringFrequency.YEARLY);
        assertThat(next).isEqualTo(LocalDate.of(2027, 7, 1));
    }

    @Test
    @DisplayName("Should handle leap day (Feb 29) to next year Feb 28")
    void shouldHandleLeapDayToNextYear() {
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        LocalDate nextYear = recurringService.calculateNextExecutionDate(leapDay, RecurringFrequency.YEARLY);
        assertThat(nextYear).isEqualTo(LocalDate.of(2025, 2, 28));
    }

    @ParameterizedTest
    @CsvSource({
        "DAILY, 2026-02-28, 2026-03-01",
        "WEEKLY, 2026-02-23, 2026-03-02",
        "MONTHLY, 2026-01-30, 2026-02-28",
        "QUARTERLY, 2026-10-31, 2027-01-31",
        "YEARLY, 2024-02-29, 2025-02-28"
    })
    @DisplayName("Parameterized test for various frequency boundary calculations")
    void testVariousFrequencyBoundaries(RecurringFrequency frequency, String startStr, String expectedStr) {
        LocalDate start = LocalDate.parse(startStr);
        LocalDate expected = LocalDate.parse(expectedStr);
        LocalDate actual = recurringService.calculateNextExecutionDate(start, frequency);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should default safely when inputs are null")
    void shouldHandleNullInputsGracefully() {
        LocalDate today = LocalDate.now();
        LocalDate result = recurringService.calculateNextExecutionDate(null, null);
        assertThat(result).isEqualTo(today.plusMonths(1));
    }
}
