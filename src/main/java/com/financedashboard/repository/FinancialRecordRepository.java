package com.financedashboard.repository;

import com.financedashboard.dto.dashboard.CategoryTotalResponse;
import com.financedashboard.dto.dashboard.RecentTransactionResponse;
import com.financedashboard.entity.FinancialRecord;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long>, JpaSpecificationExecutor<FinancialRecord> {

    @Query("select coalesce(sum(fr.amount), 0) from FinancialRecord fr where fr.type = com.financedashboard.entity.RecordType.INCOME")
    BigDecimal totalIncome();

    @Query("select coalesce(sum(fr.amount), 0) from FinancialRecord fr where fr.type = com.financedashboard.entity.RecordType.EXPENSE")
    BigDecimal totalExpenses();

    @Query("""
            select new com.financedashboard.dto.dashboard.CategoryTotalResponse(fr.category, coalesce(sum(fr.amount), 0))
            from FinancialRecord fr
            group by fr.category
            order by sum(fr.amount) desc
            """)
    List<CategoryTotalResponse> findCategoryTotals();

    @Query(value = """
            select to_char(date_trunc('month', fr.date::timestamp), 'YYYY-MM') as month,
                   coalesce(sum(case when fr.type = 'INCOME' then fr.amount else 0 end), 0) as income,
                   coalesce(sum(case when fr.type = 'EXPENSE' then fr.amount else 0 end), 0) as expense
            from financial_records fr
            where fr.deleted = false
            group by date_trunc('month', fr.date::timestamp)
            order by date_trunc('month', fr.date::timestamp) desc
            """, nativeQuery = true)
    List<Object[]> findMonthlySummaries();

    @Query("""
            select new com.financedashboard.dto.dashboard.RecentTransactionResponse(
                fr.id, fr.amount, fr.type, fr.category, fr.date, fr.description, u.name
            )
            from FinancialRecord fr
            join fr.user u
            order by fr.date desc, fr.id desc
            """)
    Page<RecentTransactionResponse> findRecentTransactions(Pageable pageable);
}