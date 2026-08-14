package com.example.wine.admin.purchase;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDayCalculatorTest {

    @Test
    void addsFiveBusinessDaysAcrossWeekend() {
        LocalDate friday = LocalDate.of(2026, 8, 14);

        assertThat(BusinessDayCalculator.addBusinessDays(friday, 5))
                .isEqualTo(LocalDate.of(2026, 8, 21));
    }
}