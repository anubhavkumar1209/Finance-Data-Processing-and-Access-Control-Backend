package com.financedashboard.dto.record;

import com.financedashboard.entity.FinancialRecord;
import com.financedashboard.entity.RecordType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinancialRecordResponse(
        Long id,
        BigDecimal amount,
        RecordType type,
        String category,
        LocalDate date,
        String description,
        Long userId,
        String userName,
        String userEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FinancialRecordResponse from(FinancialRecord record) {
        return new FinancialRecordResponse(
                record.getId(),
                record.getAmount(),
                record.getType(),
                record.getCategory(),
                record.getDate(),
                record.getDescription(),
                record.getUser().getId(),
                record.getUser().getName(),
                record.getUser().getEmail(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}