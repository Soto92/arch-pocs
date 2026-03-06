package com.example.processing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AggregationRepository {
    private final JdbcTemplate jdbcTemplate;

    public AggregationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertCitySalesTotal(String city, String salesman, String source, double total) {
        jdbcTemplate.update(
                """
                insert into city_sales_totals(city, salesman, source, total_amount)
                values (?, ?, ?, ?)
                on conflict (city, salesman, source)
                do update set total_amount = excluded.total_amount
                """,
                city, salesman, source, total
        );

        jdbcTemplate.update(
                """
                insert into top_sales_per_city(city, salesman, source, total_amount)
                select city, salesman, source, total_amount
                from (
                    select city,
                           salesman,
                           source,
                           total_amount,
                           row_number() over (partition by city order by total_amount desc, salesman asc, source asc) as rn
                    from city_sales_totals
                    where city = ?
                ) ranked
                where rn = 1
                on conflict (city)
                do update set salesman = excluded.salesman,
                              source = excluded.source,
                              total_amount = excluded.total_amount
                """,
                city
        );
    }

    public void upsertSalesmanTotal(String salesman, String source, double total) {
        jdbcTemplate.update(
                """
                insert into salesman_totals(salesman, source, total_amount)
                values (?, ?, ?)
                on conflict (salesman, source)
                do update set total_amount = excluded.total_amount
                """,
                salesman, source, total
        );

        jdbcTemplate.update(
                """
                insert into top_salesman_country(id, salesman, source, total_amount)
                select 1, salesman, source, total_amount
                from salesman_totals
                order by total_amount desc, salesman asc, source asc
                limit 1
                on conflict (id)
                do update set salesman = excluded.salesman,
                              source = excluded.source,
                              total_amount = excluded.total_amount
                """
        );
    }
}
