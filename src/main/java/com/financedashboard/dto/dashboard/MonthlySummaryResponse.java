package com.financedashboard.dto.dashboard;

import java.math.BigDecimal;

public record MonthlySummaryResponse(
        String month,
        BigDecimal income,
        BigDecimal expense
) {
    public BigDecimal net() {
        return income.subtract(expense);
    }
}