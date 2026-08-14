package com.example.wine.admin.purchase;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class BusinessDayCalculator {
    private BusinessDayCalculator() {
    }

    public static LocalDate addBusinessDays(LocalDate startDate, int businessDays) {
        LocalDate result = startDate;
        int addedDays = 0;
        while (addedDays < businessDays) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY
                    && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                addedDays++;
            }
        }
        return result;
    }
}