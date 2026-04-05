package com.financedashboard.dto.record;

import com.financedashboard.entity.RecordType;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record FinancialRecordFilterRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        RecordType type,
        String category,
        String search,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
}