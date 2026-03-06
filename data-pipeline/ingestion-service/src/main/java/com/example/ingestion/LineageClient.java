package com.example.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

@Component
public class LineageClient {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String endpoint;
    private final String namespace;
    private final String producer;
    private final boolean enabled;

    public LineageClient(
            @Value("${pipeline.lineage.enabled:true}") boolean enabled,
            @Value("${pipeline.lineage.endpoint:http://localhost:5000/api/v1/lineage}") String endpoint,
            @Value("${pipeline.lineage.namespace:data-pipeline}") String namespace,
            @Value("${pipeline.lineage.producer:https://github.com/openlineage/openlineage}") String producer
    ) {
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.namespace = namespace;
        this.producer = producer;
    }

    public void emitStart(String jobName, String runId) {
        emit("START", jobName, runId);
    }

    public void emitComplete(String jobName, String runId) {
        emit("COMPLETE", jobName, runId);
    }

    public void emitFail(String jobName, String runId) {
        emit("FAIL", jobName, runId);
    }

    private void emit(String eventType, String jobName, String runId) {
        if (!enabled) {
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "eventType", eventType,
                    "eventTime", Instant.now().toString(),
                    "run", Map.of("runId", runId),
                    "job", Map.of("namespace", namespace, "name", jobName),
                    "producer", producer
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }
}
