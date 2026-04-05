package com.financedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financedashboard.dto.record.FinancialRecordFilterRequest;
import com.financedashboard.dto.record.FinancialRecordRequest;
import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.FinancialRecord;
import com.financedashboard.entity.RecordType;
import com.financedashboard.entity.Role;
import com.financedashboard.entity.User;
import com.financedashboard.repository.FinancialRecordRepository;
import com.financedashboard.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class FinancialRecordServiceTest {

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FinancialRecordService financialRecordService;

    @Test
    void createRecordAssociatesCurrentUser() {
        User user = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@example.com")
                .password("secret")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        FinancialRecord savedRecord = FinancialRecord.builder()
                .id(10L)
                .amount(new BigDecimal("125.50"))
                .type(RecordType.INCOME)
                .category("Salary")
                .date(LocalDate.of(2026, 4, 1))
                .description("Monthly salary")
                .user(user)
                .build();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(financialRecordRepository.save(any(FinancialRecord.class))).thenReturn(savedRecord);

        FinancialRecordRequest request = new FinancialRecordRequest(
                new BigDecimal("125.50"),
                RecordType.INCOME,
                "Salary",
                LocalDate.of(2026, 4, 1),
                "Monthly salary");

        var response = financialRecordService.createRecord(request, "admin@example.com");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.userEmail()).isEqualTo("admin@example.com");
        verify(financialRecordRepository).save(any(FinancialRecord.class));
    }

    @Test
    void getAllRecordsReturnsPagedResults() {
        User user = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@example.com")
                .password("secret")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        FinancialRecord record = FinancialRecord.builder()
                .id(10L)
                .amount(new BigDecimal("99.99"))
                .type(RecordType.EXPENSE)
                .category("Food")
                .date(LocalDate.of(2026, 4, 2))
                .description("Lunch")
                .user(user)
                .build();

        Page<FinancialRecord> page = new PageImpl<>(java.util.List.of(record));
        when(financialRecordRepository.findAll(org.mockito.ArgumentMatchers.<Specification<com.financedashboard.entity.FinancialRecord>>any(), any(Pageable.class)))
                .thenReturn(page);

        FinancialRecordFilterRequest filter = new FinancialRecordFilterRequest(
                null, null, null, null, null, 0, 20, "date", "DESC");

        var result = financialRecordService.getAllRecords(filter);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).category()).isEqualTo("Food");
    }

    @Test
    void getRecordByIdReturnsRecordDetails() {
        User user = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@example.com")
                .password("secret")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        FinancialRecord existingRecord = FinancialRecord.builder()
                .id(10L)
                .amount(new BigDecimal("99.99"))
                .type(RecordType.EXPENSE)
                .category("Food")
                .date(LocalDate.of(2026, 4, 2))
                .description("Lunch")
                .user(user)
                .build();

        when(financialRecordRepository.findById(10L)).thenReturn(Optional.of(existingRecord));

        var response = financialRecordService.getRecordById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.type()).isEqualTo(RecordType.EXPENSE);
        assertThat(response.category()).isEqualTo("Food");
    }

    @Test
    void getAllRecordsRejectsInvalidDateRange() {
        FinancialRecordFilterRequest filter = new FinancialRecordFilterRequest(
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 1),
                null,
                null,
                null,
                0,
                20,
                "date",
                "DESC");

        assertThatThrownBy(() -> financialRecordService.getAllRecords(filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fromDate must be before or equal to toDate");
    }

    @Test
    void updateRecordReplacesEditableFields() {
        User user = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@example.com")
                .password("secret")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        FinancialRecord existingRecord = FinancialRecord.builder()
                .id(10L)
                .amount(new BigDecimal("99.99"))
                .type(RecordType.EXPENSE)
                .category("Food")
                .date(LocalDate.of(2026, 4, 2))
                .description("Lunch")
                .user(user)
                .build();

        when(financialRecordRepository.findById(10L)).thenReturn(Optional.of(existingRecord));
        when(financialRecordRepository.save(any(FinancialRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialRecordRequest request = new FinancialRecordRequest(
                new BigDecimal("150.00"),
                RecordType.INCOME,
                "Salary",
                LocalDate.of(2026, 4, 5),
                "Monthly salary");

        var response = financialRecordService.updateRecord(10L, request);

        assertThat(response.amount()).isEqualByComparingTo("150.00");
        assertThat(response.type()).isEqualTo(RecordType.INCOME);
        assertThat(response.category()).isEqualTo("Salary");
        assertThat(response.description()).isEqualTo("Monthly salary");
    }

    @Test
    void deleteRecordUsesRepositoryDeleteForSoftDelete() {
        User user = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@example.com")
                .password("secret")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        FinancialRecord existingRecord = FinancialRecord.builder()
                .id(10L)
                .amount(new BigDecimal("99.99"))
                .type(RecordType.EXPENSE)
                .category("Food")
                .date(LocalDate.of(2026, 4, 2))
                .description("Lunch")
                .user(user)
                .build();

        when(financialRecordRepository.findById(10L)).thenReturn(Optional.of(existingRecord));

        financialRecordService.deleteRecord(10L);

        verify(financialRecordRepository).delete(existingRecord);
    }
}
