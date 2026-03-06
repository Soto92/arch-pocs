package com.example.processing;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
        lineageClient.emitStart("processing-stream", runId);
    }

    @PreDestroy
    public void onShutdown() {
        lineageClient.emitComplete("processing-stream", runId);
    }
}
