package com.financedashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class DashboardControllerAuthorizationTest {

    @Test
    void dashboardSummaryAllowsViewerAnalystAndAdmin() {
        PreAuthorize preAuthorize = DashboardController.class.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')");
    }
}
