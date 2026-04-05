package com.financedashboard.service;

import com.financedashboard.dto.dashboard.DashboardSummaryResponse;
import com.financedashboard.dto.dashboard.MonthlySummaryResponse;
import com.financedashboard.dto.dashboard.RecentTransactionResponse;
import com.financedashboard.repository.FinancialRecordRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository financialRecordRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(int recentLimit) {
        int limit = recentLimit < 1 ? 5 : Math.min(recentLimit, 20);

        BigDecimal totalIncome = financialRecordRepository.totalIncome();
        BigDecimal totalExpenses = financialRecordRepository.totalExpenses();
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        List<MonthlySummaryResponse> monthlySummaries = financialRecordRepository.findMonthlySummaries().stream()
                .map(row -> new MonthlySummaryResponse(
                        row[0].toString(),
                        row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString()),
                        row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString())))
                .toList();

        List<RecentTransactionResponse> recentTransactions = financialRecordRepository
                .findRecentTransactions(PageRequest.of(0, limit))
                .getContent();

        return new DashboardSummaryResponse(
                totalIncome,
                totalExpenses,
                netBalance,
                financialRecordRepository.findCategoryTotals(),
                monthlySummaries,
                recentTransactions);
    }
}