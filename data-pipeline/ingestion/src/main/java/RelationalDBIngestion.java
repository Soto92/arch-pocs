import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.Row;

import java.sql.Timestamp;

public class RelationalDBIngestion {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        RowTypeInfo rowTypeInfo = new RowTypeInfo(
                BasicTypeInfo.INT_TYPE_INFO,
                BasicTypeInfo.INT_TYPE_INFO,
                BasicTypeInfo.STRING_TYPE_INFO,
                BasicTypeInfo.STRING_TYPE_INFO,
                BasicTypeInfo.BIG_DEC_TYPE_INFO,
                org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo.TIMESTAMP
        );

        JdbcSource<Row> jdbcSource = JdbcSource.<Row>builder()
                .setConnectionOptions(
                        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                .withUrl("jdbc:postgresql://localhost:5432/sales")
                                .withDriverName("org.postgresql.Driver")
                                .withUsername("user")
                                .withPassword("password")
                                .build()
                )
                .setQuery("SELECT sale_id, salesman_id, salesman_name, city, sale_value, sale_date FROM sales")
                .setRowTypeInfo(rowTypeInfo)
                .build();

        env.fromSource(jdbcSource, WatermarkStrategy.noWatermarks(), "PostgreSQL Source")
                .map(Row::toString)
                .print();

        env.execute("Relational DB Ingestion Job");
    }
}
