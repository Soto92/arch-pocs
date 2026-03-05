package com.example.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class TopologyConfig {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AggregationRepository repository;

    public TopologyConfig(AggregationRepository repository) {
        this.repository = repository;
    }

    @Bean
    public KStream<String, String> kStream(StreamsBuilder builder, @Value("${pipeline.kafka.topic}") String topic) {
        KStream<String, String> raw = builder.stream(topic, Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, SaleEvent> sales = raw.flatMapValues(this::parse);

        sales.selectKey((k, v) -> v.city() + "|" + v.salesman())
                .groupByKey(Grouped.with(Serdes.String(), Serdes.serdeFrom(new SaleEventSerializer(), new SaleEventDeserializer())))
                .aggregate(
                        () -> 0.0,
                        (key, value, aggregate) -> aggregate + value.amount(),
                        Materialized.with(Serdes.String(), Serdes.Double())
                )
                .toStream()
                .foreach((key, total) -> {
                    String[] parts = key.split("\\|", 2);
                    if (parts.length == 2) {
                        repository.upsertCitySalesTotal(parts[0], parts[1], total);
                    }
                });

        sales.selectKey((k, v) -> v.salesman())
                .groupByKey(Grouped.with(Serdes.String(), Serdes.serdeFrom(new SaleEventSerializer(), new SaleEventDeserializer())))
                .aggregate(
                        () -> 0.0,
                        (key, value, aggregate) -> aggregate + value.amount(),
                        Materialized.with(Serdes.String(), Serdes.Double())
                )
                .toStream()
                .foreach(repository::upsertSalesmanTotal);

        return raw;
    }

    private java.util.List<SaleEvent> parse(String value) {
        try {
            return java.util.List.of(objectMapper.readValue(value, SaleEvent.class));
        } catch (Exception ignored) {
            return java.util.List.of();
        }
    }
}
