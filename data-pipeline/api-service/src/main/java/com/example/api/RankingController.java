package com.example.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RankingController {
    private final RankingRepository repository;

    public RankingController(RankingRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/top-sales-per-city")
    public List<TopCitySale> topSalesPerCity() {
        return repository.topSalesPerCity();
    }

    @GetMapping("/top-salesman-country")
    public TopSalesmanCountry topSalesmanCountry() {
        return repository.topSalesmanCountry();
    }
}