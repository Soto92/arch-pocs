package com.example.processing;

public record SaleEvent(
        String saleId,
        String city,
        String salesman,
        double amount,
        String eventTime,
        String source
) {
}