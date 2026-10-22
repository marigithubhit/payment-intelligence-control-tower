package com.mari.iso20022.agent.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaGenerateClientTest {
    @Test
    void postsGenerateRequestWithConfiguredModelStreamAndTemperature() {
        OllamaProperties properties = new OllamaProperties();
        properties.setBaseUrl("http://localhost:11434");
        properties.setModel("qwen2.5:7b");
        properties.setTemperature(new BigDecimal("0.1"));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("qwen2.5:7b"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.prompt").value("user-data"))
                .andExpect(jsonPath("$.options.temperature").value(0.1))
                .andExpect(jsonPath("$.options.num_predict").value(256))
                .andRespond(withSuccess("{\"response\":\"{\\\"transactionId\\\":\\\"TXN1\\\"}\",\"done\":true}",
                        MediaType.APPLICATION_JSON));
        OllamaGenerateClient client = new OllamaGenerateClient(properties, new ObjectMapper(), builder);

        String result = client.complete(new ReasoningPrompt("system-safe", "user-data", Map.of(),
                Map.of(), java.util.Set.of(), java.util.Set.of()));

        assertEquals("{\"transactionId\":\"TXN1\"}", result);
        server.verify();
    }
}
