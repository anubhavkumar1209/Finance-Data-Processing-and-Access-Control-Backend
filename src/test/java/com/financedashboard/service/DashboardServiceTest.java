package com.financedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financedashboard.dto.dashboard.CategoryTotalResponse;
import com.financedashboard.dto.dashboard.MonthlySummaryResponse;
import com.financedashboard.dto.dashboard.RecentTransactionResponse;
import com.financedashboard.entity.RecordType;
import com.financedashboard.repository.FinancialRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryCalculatesNetBalanceFromDatabaseAggregates() {
        when(financialRecordRepository.totalIncome()).thenReturn(new BigDecimal("1500.00"));
        when(financialRecordRepository.totalExpenses()).thenReturn(new BigDecimal("400.00"));
        when(financialRecordRepository.findCategoryTotals()).thenReturn(List.of(
                new CategoryTotalResponse("Salary", new BigDecimal("1500.00"))));
        when(financialRecordRepository.findMonthlySummaries()).thenReturn(List.<Object[]>of(
                new Object[] {"2026-04", new BigDecimal("1500.00"), new BigDecimal("400.00")}));
        Page<RecentTransactionResponse> recentTransactions = new PageImpl<>(List.of(
                new RecentTransactionResponse(1L, new BigDecimal("50.00"), RecordType.EXPENSE, "Food",
                        LocalDate.of(2026, 4, 2), "Lunch", "Jane")));
        when(financialRecordRepository.findRecentTransactions(org.springframework.data.domain.PageRequest.of(0, 5)))
                .thenReturn(recentTransactions);

        var summary = dashboardService.getSummary(5);

        assertThat(summary.totalIncome()).isEqualByComparingTo("1500.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("400.00");
        assertThat(summary.netBalance()).isEqualByComparingTo("1100.00");
        assertThat(summary.categoryTotals()).hasSize(1);
        assertThat(summary.categoryTotals().get(0).category()).isEqualTo("Salary");
        assertThat(summary.monthlySummaries()).containsExactly(
                new MonthlySummaryResponse("2026-04", new BigDecimal("1500.00"), new BigDecimal("400.00")));
        assertThat(summary.recentTransactions()).hasSize(1);
        assertThat(summary.recentTransactions().get(0).description()).isEqualTo("Lunch");
    }

    @Test
    void getSummaryCapsRecentTransactionLimitAtTwenty() {
        when(financialRecordRepository.totalIncome()).thenReturn(BigDecimal.ZERO);
        when(financialRecordRepository.totalExpenses()).thenReturn(BigDecimal.ZERO);
        when(financialRecordRepository.findCategoryTotals()).thenReturn(List.of());
        when(financialRecordRepository.findMonthlySummaries()).thenReturn(List.of());
        when(financialRecordRepository.findRecentTransactions(org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(Page.empty());

        var summary = dashboardService.getSummary(50);

        assertThat(summary.recentTransactions()).isEmpty();
        verify(financialRecordRepository).findRecentTransactions(org.springframework.data.domain.PageRequest.of(0, 20));
    }
}
