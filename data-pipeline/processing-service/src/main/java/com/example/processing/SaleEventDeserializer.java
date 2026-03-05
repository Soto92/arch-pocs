package com.example.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class SaleEventDeserializer implements Deserializer<SaleEvent> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SaleEvent deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return mapper.readValue(data, SaleEvent.class);
        } catch (Exception e) {
            return null;
        }
    }
}