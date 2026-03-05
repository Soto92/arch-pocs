package com.example.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class IngestionJob {
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String topic;
    private final Path inbox;
    private final Set<String> processedFiles = new HashSet<>();

    public IngestionJob(
            JdbcTemplate jdbcTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${pipeline.kafka.topic}") String topic,
            @Value("${pipeline.files.inbox}") String inbox
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.inbox = Path.of(inbox);
    }

    @Scheduled(fixedDelayString = "${pipeline.ingestion.interval-ms:30000}")
    public void run() {
        ingestDatabase();
        ingestFiles();
        ingestWsPlaceholder();
    }

    private void ingestDatabase() {
        List<SaleEvent> events = jdbcTemplate.query(
                """
                select sale_id, city, salesman, amount, event_time
                from sales_source
                where published = false
                order by event_time asc
                limit 500
                """,
                (rs, rowNum) -> new SaleEvent(
                        rs.getString("sale_id"),
                        rs.getString("city"),
                        rs.getString("salesman"),
                        rs.getDouble("amount"),
                        rs.getTimestamp("event_time").toInstant().toString(),
                        "relational-db"
                )
        );

        for (SaleEvent event : events) {
            publish(event);
            jdbcTemplate.update("update sales_source set published = true where sale_id = ?", event.saleId());
        }
    }

    private void ingestFiles() {
        if (!Files.exists(inbox) || !Files.isDirectory(inbox)) {
            return;
        }

        try (var stream = Files.list(inbox)) {
            stream.filter(Files::isRegularFile)
                    .forEach(file -> {
                        String name = file.getFileName().toString();
                        if (processedFiles.contains(name)) {
                            return;
                        }
                        if (name.endsWith(".csv")) {
                            ingestCsv(file);
                        } else if (name.endsWith(".json")) {
                            ingestJson(file);
                        }
                        processedFiles.add(name);
                    });
        } catch (IOException ignored) {
        }
    }

    private void ingestCsv(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",");
                if (cols.length < 4) {
                    continue;
                }
                SaleEvent event = new SaleEvent(
                        UUID.randomUUID().toString(),
                        cols[0].trim(),
                        cols[1].trim(),
                        Double.parseDouble(cols[2].trim()),
                        cols[3].trim(),
                        "file-csv"
                );
                publish(event);
            }
        } catch (Exception ignored) {
        }
    }

    private void ingestJson(Path file) {
        try {
            var root = objectMapper.readTree(file.toFile());
            if (root.isArray()) {
                for (var node : root) {
                    SaleEvent event = new SaleEvent(
                            node.path("saleId").asText(UUID.randomUUID().toString()),
                            node.path("city").asText(),
                            node.path("salesman").asText(),
                            node.path("amount").asDouble(),
                            node.path("eventTime").asText(Instant.now().toString()),
                            "file-json"
                    );
                    publish(event);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void ingestWsPlaceholder() {
    }

    private void publish(SaleEvent event) {
        try {
            kafkaTemplate.send(topic, event.saleId(), objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
        }
    }
}
