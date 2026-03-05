package com.example.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RankingRepository {
    private final JdbcTemplate jdbcTemplate;

    public RankingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TopCitySale> topSalesPerCity() {
        return jdbcTemplate.query(
                "select city, salesman, total_amount from top_sales_per_city order by city asc",
                (rs, rowNum) -> new TopCitySale(
                        rs.getString("city"),
                        rs.getString("salesman"),
                        rs.getDouble("total_amount")
                )
        );
    }

    public TopSalesmanCountry topSalesmanCountry() {
        List<TopSalesmanCountry> rows = jdbcTemplate.query(
                "select salesman, total_amount from top_salesman_country where id = 1",
                (rs, rowNum) -> new TopSalesmanCountry(
                        rs.getString("salesman"),
                        rs.getDouble("total_amount")
                )
        );
        return rows.isEmpty() ? new TopSalesmanCountry("", 0.0) : rows.get(0);
    }
}
