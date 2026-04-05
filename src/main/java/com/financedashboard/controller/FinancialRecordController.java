package com.financedashboard.controller;

import com.financedashboard.dto.record.FinancialRecordFilterRequest;
import com.financedashboard.dto.record.FinancialRecordRequest;
import com.financedashboard.dto.record.FinancialRecordResponse;
import com.financedashboard.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/financial-records", "/api/records"})
@Tag(name = "Financial Records")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService financialRecordService;

    @PostMapping({"", "/"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a financial record")
    public ResponseEntity<FinancialRecordResponse> createRecord(@Valid @RequestBody FinancialRecordRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financialRecordService.createRecord(request, authentication.getName()));
    }

    @GetMapping({"", "/"})
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "List financial records")
    public Page<FinancialRecordResponse> getAllRecords(@Valid @ModelAttribute FinancialRecordFilterRequest filter) {
        return financialRecordService.getAllRecords(filter);
    }

    @GetMapping({"/{id}", "/{id}/"})
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get a financial record by id")
    public FinancialRecordResponse getRecordById(@PathVariable Long id) {
        return financialRecordService.getRecordById(id);
    }

    @PutMapping({"/{id}", "/{id}/"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a financial record")
    public FinancialRecordResponse updateRecord(@PathVariable Long id,
                                                @Valid @RequestBody FinancialRecordRequest request) {
        return financialRecordService.updateRecord(id, request);
    }

    @DeleteMapping({"/{id}", "/{id}/"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a financial record")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        financialRecordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
