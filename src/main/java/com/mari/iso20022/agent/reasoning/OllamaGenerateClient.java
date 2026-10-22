package com.mari.iso20022.agent.reasoning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

@Component
public class OllamaGenerateClient implements OllamaClient {
    private static final Logger log = LoggerFactory.getLogger(OllamaGenerateClient.class);
    private static final String GENERATE_PATH = "/api/generate";

    private final OllamaProperties properties;
    private final ObjectMapper mapper;
    private final RestClient.Builder restClientBuilder;

    @Autowired
    public OllamaGenerateClient(OllamaProperties properties, ObjectMapper mapper) {
        this(properties, mapper, null);
    }

    OllamaGenerateClient(OllamaProperties properties, ObjectMapper mapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.mapper = mapper;
        this.restClientBuilder = restClientBuilder;
        log.info("Ollama configuration target={} model={} temperature={} timeoutSeconds={}",
                targetUrl(), properties.getModel(), properties.getTemperature(), properties.getTimeoutSeconds());
    }

    @Override
    public String complete(ReasoningPrompt prompt) {
        long started = System.nanoTime();
        String target = targetUrl();
        try {
            RestClient.Builder builder = restClientBuilder == null
                    ? createClientBuilder(properties) : restClientBuilder.clone();
            RestClient client = builder.baseUrl(properties.getBaseUrl()).build();

            Map<String, Object> payload = Map.of(
                    "model", properties.getModel(),
                    "prompt", prompt.userMessage(),
                    "system", prompt.systemMessage(),
                    "stream", false,
                    "format", prompt.jsonSchema(),
                    "options", Map.of("temperature", properties.getTemperature(), "num_predict", 256));

            OllamaHttpResponse response = client.post()
                    .uri(GENERATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((request, result) -> new OllamaHttpResponse(
                            result.getStatusCode().value(), result.getBody().readAllBytes()));
            long elapsed = elapsedMillis(started);
            log.info("Ollama HTTP response target={} model={} status={} executionTimeMs={}",
                    target, properties.getModel(), response.status(), elapsed);
            if (response.status() < 200 || response.status() >= 300) {
                String kind = response.status() >= 500 ? "HTTP 5xx" : "HTTP 4xx";
                throw new OllamaCallException("Ollama returned " + kind + " status " + response.status(), null);
            }

            JsonNode json;
            try {
                json = mapper.readTree(response.body());
            } catch (JsonProcessingException ex) {
                log.warn("Ollama response invalid JSON target={} model={} executionTimeMs={} cause={}",
                        target, properties.getModel(), elapsed, ex.getOriginalMessage());
                throw new OllamaCallException("Ollama returned invalid JSON", ex);
            }
            JsonNode generated = json == null ? null : json.get("response");
            if (generated == null || !generated.isTextual() || generated.asText().isBlank()) {
                log.warn("Ollama response empty target={} model={} status={} executionTimeMs={}",
                        target, properties.getModel(), response.status(), elapsed);
                return "";
            }
            return generated.asText();
        } catch (OllamaCallException ex) {
            throw ex;
        } catch (Exception ex) {
            String kind = failureKind(ex);
            log.warn("Ollama HTTP call failed target={} model={} failureKind={} executionTimeMs={} rootCause={}",
                    target, properties.getModel(), kind, elapsedMillis(started), rootCause(ex).toString());
            throw new OllamaCallException("Ollama " + kind, ex);
        }
    }

    private String targetUrl() {
        return URI.create(properties.getBaseUrl()).resolve(GENERATE_PATH).toString();
    }

    private static RestClient.Builder createClientBuilder(OllamaProperties properties) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    private static String failureKind(Throwable exception) {
        Throwable cause = rootCause(exception);
        if (cause instanceof HttpConnectTimeoutException) return "connection timeout";
        if (cause instanceof HttpTimeoutException || cause instanceof java.net.SocketTimeoutException) return "read timeout";
        if (cause instanceof ConnectException || hasCauseType(exception, "org.springframework.web.client.ResourceAccessException")) {
            String message = cause.getMessage();
            return message != null && message.toLowerCase().contains("refused")
                    ? "connection refused" : "connection failure";
        }
        return "request failure";
    }

    private static boolean hasCauseType(Throwable exception, String typeName) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current.getClass().getName().equals(typeName)) return true;
        }
        return false;
    }

    private static Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current;
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private record OllamaHttpResponse(int status, byte[] body) { }
}
