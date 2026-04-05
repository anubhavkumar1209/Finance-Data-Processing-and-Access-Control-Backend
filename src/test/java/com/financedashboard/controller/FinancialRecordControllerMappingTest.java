package com.financedashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financedashboard.dto.record.FinancialRecordFilterRequest;
import com.financedashboard.dto.record.FinancialRecordRequest;
import com.financedashboard.dto.record.FinancialRecordResponse;
import com.financedashboard.entity.RecordType;
import com.financedashboard.service.FinancialRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FinancialRecordControllerMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private FinancialRecordService financialRecordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FinancialRecordController(financialRecordService)).build();
    }

    @Test
    void postFinancialRecordsPathIsHandledByController() throws Exception {
        when(financialRecordService.createRecord(any(FinancialRecordRequest.class), eq("admin@example.com")))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/financial-records")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", "token"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(handler().handlerType(FinancialRecordController.class))
                .andExpect(handler().methodName("createRecord"));
    }

    @Test
    void trailingSlashFinancialRecordsPathIsHandledByController() throws Exception {
        when(financialRecordService.createRecord(any(FinancialRecordRequest.class), eq("admin@example.com")))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/financial-records/")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", "token"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(handler().handlerType(FinancialRecordController.class))
                .andExpect(handler().methodName("createRecord"));
    }

    @Test
    void aliasPathIsHandledByController() throws Exception {
        when(financialRecordService.createRecord(any(FinancialRecordRequest.class), eq("admin@example.com")))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/records")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", "token"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(handler().handlerType(FinancialRecordController.class))
                .andExpect(handler().methodName("createRecord"));
    }

    @Test
    void getFinancialRecordByIdPathIsHandledByController() throws Exception {
        when(financialRecordService.getRecordById(10L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/financial-records/10"))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(FinancialRecordController.class))
                .andExpect(handler().methodName("getRecordById"));
    }

    @Test
    void listFinancialRecordsBindsSearchAndPaginationParameters() throws Exception {
        when(financialRecordService.getAllRecords(any(FinancialRecordFilterRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/financial-records")
                        .param("search", "salary")
                        .param("category", "Income")
                        .param("type", "INCOME")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "amount")
                        .param("sortDirection", "ASC"))
                .andExpect(handler().handlerType(FinancialRecordController.class))
                .andExpect(handler().methodName("getAllRecords"));

        ArgumentCaptor<FinancialRecordFilterRequest> captor = ArgumentCaptor.forClass(FinancialRecordFilterRequest.class);
        org.mockito.Mockito.verify(financialRecordService).getAllRecords(captor.capture());

        FinancialRecordFilterRequest filter = captor.getValue();
        assertThat(filter.search()).isEqualTo("salary");
        assertThat(filter.category()).isEqualTo("Income");
        assertThat(filter.type()).isEqualTo(RecordType.INCOME);
        assertThat(filter.page()).isEqualTo(1);
        assertThat(filter.size()).isEqualTo(5);
        assertThat(filter.sortBy()).isEqualTo("amount");
        assertThat(filter.sortDirection()).isEqualTo("ASC");
    }

    @Test
    void deleteFinancialRecordPathIsHandledByController() throws Exception {
        doNothing().when(financialRecordService).deleteRecord(anyLong());

        mockMvc.perform(delete("/api/financial-records/10"))
                .andExpect(status().isNoContent())
                .andExpect(handler().handlerType(FinancialRecordController.class))
                .andExpect(handler().methodName("deleteRecord"));
    }

    private FinancialRecordRequest sampleRequest() {
        return new FinancialRecordRequest(
                new BigDecimal("125.50"),
                RecordType.INCOME,
                "Salary",
                LocalDate.of(2026, 4, 1),
                "Monthly salary");
    }

    private FinancialRecordResponse sampleResponse() {
        return new FinancialRecordResponse(
                10L,
                new BigDecimal("125.50"),
                RecordType.INCOME,
                "Salary",
                LocalDate.of(2026, 4, 1),
                "Monthly salary",
                1L,
                "Admin",
                "admin@example.com",
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 1, 10, 0));
    }
}
