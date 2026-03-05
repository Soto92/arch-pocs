package com.example.ingestion;

public record SaleEvent(
        String saleId,
        String city,
        String salesman,
        double amount,
        String eventTime,
        String source
) {
}