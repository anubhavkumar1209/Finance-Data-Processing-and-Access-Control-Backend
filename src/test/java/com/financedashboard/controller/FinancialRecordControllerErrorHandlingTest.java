package com.financedashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financedashboard.exception.GlobalExceptionHandler;
import com.financedashboard.exception.ResourceNotFoundException;
import com.financedashboard.service.FinancialRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FinancialRecordControllerErrorHandlingTest {

    @Mock
    private FinancialRecordService financialRecordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        mockMvc = MockMvcBuilders.standaloneSetup(new FinancialRecordController(financialRecordService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createRecordRejectsInvalidInputWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/financial-records")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", "token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 0,
                                  "type": "INCOME",
                                  "category": "",
                                  "date": "2026-04-01",
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.length()").value(3));
    }

    @Test
    void createRecordRejectsInvalidEnumValueWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/financial-records")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", "token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 100,
                                  "type": "BONUS",
                                  "category": "Salary",
                                  "date": "2026-04-01",
                                  "description": "Monthly salary"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.details[0]").value("Invalid value for field 'type'"));
    }

    @Test
    void getRecordByIdReturnsNotFoundForMissingRecord() throws Exception {
        when(financialRecordService.getRecordById(99L))
                .thenThrow(new ResourceNotFoundException("Financial record not found with id: 99"));

        mockMvc.perform(get("/api/financial-records/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Financial record not found with id: 99"));
    }

    @Test
    void listRecordsRejectsInvalidFilterOperation() throws Exception {
        when(financialRecordService.getAllRecords(any()))
                .thenThrow(new IllegalArgumentException("fromDate must be before or equal to toDate"));

        mockMvc.perform(get("/api/financial-records")
                        .param("fromDate", "2026-04-10")
                        .param("toDate", "2026-04-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromDate must be before or equal to toDate"));
    }
}
