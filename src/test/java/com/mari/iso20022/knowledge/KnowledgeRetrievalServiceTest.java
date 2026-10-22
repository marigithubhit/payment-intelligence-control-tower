package com.mari.iso20022.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgeRetrievalServiceTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchesCuratedKnowledgeCaseInsensitivelyAndReturnsSources() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/search").param("q", "beneficiary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("beneficiary"))
                .andExpect(jsonPath("$.results", hasSize(not(0))))
                .andExpect(jsonPath("$.results[*].source", everyItem(notNullValue())))
                .andExpect(jsonPath("$.results[*].documentCode", everyItem(notNullValue())));
    }

    @Test
    void searchesPacs008() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/search").param("q", "pacs.008"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].documentCode").value("CURATED-ISO20022-PACS008"));
    }

    @Test
    void searchesIso20022CaseInsensitively() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/search").param("q", "ISO20022"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(not(0))));
    }

    @Test
    void searchesIntrBkSttlmAmt() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/search").param("q", "IntrBkSttlmAmt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].documentCode").value("CURATED-INTRBKSTTLMAMT"));
    }

    @Test
    void unknownTermReturnsEmptyResults() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/search").param("q", "unmatched-knowledge-term-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", empty()));
    }
}
