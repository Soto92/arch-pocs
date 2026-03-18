package com.example.processing;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ProcessingLineageLifecycle {
    private final LineageClient lineageClient;
    private final String runId = UUID.randomUUID().toString();

    public ProcessingLineageLifecycle(LineageClient lineageClient) {
        this.lineageClient = lineageClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        List<Map<String, Object>> inputs = List.of(
                dataset("kafka", "sales.raw")
        );
        List<Map<String, Object>> outputs = List.of(
                dataset("postgres", "pipeline.top_sales_per_city"),
                dataset("postgres", "pipeline.top_salesman_country")
        );
        lineageClient.emitStart("processing-stream", runId, inputs, outputs);
    }

    @PreDestroy
    public void onShutdown() {
        List<Map<String, Object>> inputs = List.of(
                dataset("kafka", "sales.raw")
        );
        List<Map<String, Object>> outputs = List.of(
                dataset("postgres", "pipeline.top_sales_per_city"),
                dataset("postgres", "pipeline.top_salesman_country")
        );
        lineageClient.emitComplete("processing-stream", runId, inputs, outputs);
    }

    private Map<String, Object> dataset(String namespace, String name) {
        return Map.of(
                "namespace", namespace,
                "name", name
        );
    }
}