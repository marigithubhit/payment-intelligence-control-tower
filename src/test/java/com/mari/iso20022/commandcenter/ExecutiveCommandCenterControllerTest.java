package com.mari.iso20022.commandcenter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mari.iso20022.commandcenter.dto.CommandCenterDtos.CreateActionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExecutiveCommandCenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getOverviewReturnsHttp200AndValidStructure() throws Exception {
        mockMvc.perform(get("/api/v1/command-center/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpiSummary.totalTransactions").isNumber())
                .andExpect(jsonPath("$.kpiSummary.straightThroughProcessingRate").isNumber())
                .andExpect(jsonPath("$.slaMetrics.overallSlaComplianceRate").isNumber())
                .andExpect(jsonPath("$.dataDisclaimer").isString());
    }

    @Test
    void getRegionalReturnsHttp200AndMeaMarkets() throws Exception {
        mockMvc.perform(get("/api/v1/command-center/regional")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markets").isArray())
                .andExpect(jsonPath("$.rails").isArray());
    }

    @Test
    void getExceptionPatternsReturnsHttp200() throws Exception {
        mockMvc.perform(get("/api/v1/command-center/exception-patterns")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPriorityQueueReturnsHttp200() throws Exception {
        mockMvc.perform(get("/api/v1/command-center/priority-queue")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void actionEndpointsCreateAndRetrieveAction() throws Exception {
        CreateActionRequest request = new CreateActionRequest(
                null,
                "TXN-MVC-100",
                "MVC Test Action",
                "Testing REST action endpoint",
                "CHANNEL_REPAIR",
                "Payments Operations",
                "Lead",
                "Analyst B",
                true,
                "Latency",
                "45s"
        );

        mockMvc.perform(post("/api/v1/command-center/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PROPOSED"));

        mockMvc.perform(get("/api/v1/command-center/actions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
