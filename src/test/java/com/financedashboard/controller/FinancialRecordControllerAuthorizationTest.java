package com.financedashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class FinancialRecordControllerAuthorizationTest {

    @Test
    void readEndpointsAllowAdminAndAnalyst() {
        assertThat(preAuthorizeValue("getAllRecords")).isEqualTo("hasAnyRole('ADMIN', 'ANALYST')");
        assertThat(preAuthorizeValue("getRecordById")).isEqualTo("hasAnyRole('ADMIN', 'ANALYST')");
    }

    @Test
    void writeEndpointsRemainAdminOnly() {
        assertThat(preAuthorizeValue("createRecord")).isEqualTo("hasRole('ADMIN')");
        assertThat(preAuthorizeValue("updateRecord")).isEqualTo("hasRole('ADMIN')");
        assertThat(preAuthorizeValue("deleteRecord")).isEqualTo("hasRole('ADMIN')");
    }

    private String preAuthorizeValue(String methodName) {
        for (Method method : FinancialRecordController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                assertThat(preAuthorize).isNotNull();
                return preAuthorize.value();
            }
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }
}
