package com.example.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private final String usersUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Set<String> processedFiles = new HashSet<>();
    private final Set<String> processedUsers = new HashSet<>();

    public IngestionJob(
            JdbcTemplate jdbcTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${pipeline.kafka.topic}") String topic,
            @Value("${pipeline.files.inbox}") String inbox,
            @Value("${pipeline.ws.users-url}") String usersUrl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.inbox = Path.of(inbox);
        this.usersUrl = usersUrl;
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
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(usersUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }

            var root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                return;
            }

            for (var node : root) {
                String id = node.path("id").asText();
                if (id.isBlank() || processedUsers.contains(id)) {
                    continue;
                }
                String city = node.path("address").path("city").asText("Unknown");
                String salesman = node.path("name").asText("Unknown");
                double amount = 100.0 + (node.path("id").asDouble() * 37.5);
                SaleEvent event = new SaleEvent(
                        "ws-" + id,
                        city,
                        salesman,
                        amount,
                        Instant.now().toString(),
                        "ws-users-api"
                );
                publish(event);
                processedUsers.add(id);
            }
        } catch (Exception ignored) {
        }
    }

    private void publish(SaleEvent event) {
        try {
            kafkaTemplate.send(topic, event.saleId(), objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
        }
    }
}
