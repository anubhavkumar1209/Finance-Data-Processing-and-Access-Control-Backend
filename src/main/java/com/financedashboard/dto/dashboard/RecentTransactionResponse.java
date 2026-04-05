package com.financedashboard.dto.dashboard;

import com.financedashboard.entity.RecordType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecentTransactionResponse(
        Long id,
        BigDecimal amount,
        RecordType type,
        String category,
        LocalDate date,
        String description,
        String userName
) {
}