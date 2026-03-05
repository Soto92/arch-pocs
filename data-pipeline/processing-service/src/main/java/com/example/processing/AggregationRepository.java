package com.example.processing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AggregationRepository {
    private final JdbcTemplate jdbcTemplate;

    public AggregationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertCitySalesTotal(String city, String salesman, double total) {
        jdbcTemplate.update(
                """
                insert into city_sales_totals(city, salesman, total_amount)
                values (?, ?, ?)
                on conflict (city, salesman)
                do update set total_amount = excluded.total_amount
                """,
                city, salesman, total
        );

        jdbcTemplate.update(
                """
                insert into top_sales_per_city(city, salesman, total_amount)
                select city, salesman, total_amount
                from (
                    select city,
                           salesman,
                           total_amount,
                           row_number() over (partition by city order by total_amount desc, salesman asc) as rn
                    from city_sales_totals
                    where city = ?
                ) ranked
                where rn = 1
                on conflict (city)
                do update set salesman = excluded.salesman,
                              total_amount = excluded.total_amount
                """,
                city
        );
    }

    public void upsertSalesmanTotal(String salesman, double total) {
        jdbcTemplate.update(
                """
                insert into salesman_totals(salesman, total_amount)
                values (?, ?)
                on conflict (salesman)
                do update set total_amount = excluded.total_amount
                """,
                salesman, total
        );

        jdbcTemplate.update(
                """
                insert into top_salesman_country(id, salesman, total_amount)
                select 1, salesman, total_amount
                from salesman_totals
                order by total_amount desc, salesman asc
                limit 1
                on conflict (id)
                do update set salesman = excluded.salesman,
                              total_amount = excluded.total_amount
                """
        );
    }
}