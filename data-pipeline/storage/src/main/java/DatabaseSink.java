import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

public class DatabaseSink {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<Tuple4<String, String, BigDecimal, Timestamp>> topSalesPerCityStream = env.fromElements(
                Tuple4.of("New York", "Jane Smith", new BigDecimal("200.00"), Timestamp.from(Instant.now()))
        );

        DataStream<Tuple4<String, String, BigDecimal, Timestamp>> topSalesmanStream = env.fromElements(
                Tuple4.of("Country", "Maria Garcia", new BigDecimal("1250.00"), Timestamp.from(Instant.now()))
        );

        topSalesPerCityStream.addSink(
                JdbcSink.sink(
                        "INSERT INTO top_sales_per_city (city, salesman_name, sale_value, window_end) VALUES (?, ?, ?, ?)",
                        (statement, tuple) -> {
                            statement.setString(1, tuple.f0);
                            statement.setString(2, tuple.f1);
                            statement.setBigDecimal(3, tuple.f2);
                            statement.setTimestamp(4, tuple.f3);
                        },
                        JdbcExecutionOptions.builder()
                                .withBatchSize(1)
                                .build(),
                        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                .withUrl("jdbc:postgresql://localhost:5432/sales")
                                .withDriverName("org.postgresql.Driver")
                                .withUsername("user")
                                .withPassword("password")
                                .build()
                )
        );

        topSalesmanStream.addSink(
                JdbcSink.sink(
                        "INSERT INTO top_salesman (salesman_name, total_sales, window_end) VALUES (?, ?, ?)",
                        (statement, tuple) -> {
                            statement.setString(1, tuple.f1);
                            statement.setBigDecimal(2, tuple.f2);
                            statement.setTimestamp(3, tuple.f3);
                        },
                        JdbcExecutionOptions.builder()
                                .withBatchSize(1)
                                .build(),
                        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                .withUrl("jdbc:postgresql://localhost:5432/sales")
                                .withDriverName("org.postgresql.Driver")
                                .withUsername("user")
                                .withPassword("password")
                                .build()
                )
        );

        env.execute("Database Sink Job");
    }
}
