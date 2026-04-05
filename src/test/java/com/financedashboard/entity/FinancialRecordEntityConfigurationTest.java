package com.financedashboard.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.Test;

class FinancialRecordEntityConfigurationTest {

    @Test
    void financialRecordUsesSoftDeleteAnnotations() {
        SQLDelete sqlDelete = FinancialRecord.class.getAnnotation(SQLDelete.class);
        SQLRestriction sqlRestriction = FinancialRecord.class.getAnnotation(SQLRestriction.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE financial_records SET deleted = true, deleted_at = now() WHERE id = ?");
        assertThat(sqlRestriction).isNotNull();
        assertThat(sqlRestriction.value()).isEqualTo("deleted = false");
    }
}
