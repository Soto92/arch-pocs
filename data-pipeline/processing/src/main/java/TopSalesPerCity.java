import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

public class TopSalesPerCity {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<Tuple4<String, String, BigDecimal, Timestamp>> salesStream = env.fromElements(
                Tuple4.of("New York", "John Doe", new BigDecimal("100.00"), Timestamp.from(Instant.now())),
                Tuple4.of("New York", "Jane Smith", new BigDecimal("150.00"), Timestamp.from(Instant.now())),
                Tuple4.of("New York", "John Doe", new BigDecimal("200.00"), Timestamp.from(Instant.now())),
                Tuple4.of("London", "Peter Jones", new BigDecimal("120.00"), Timestamp.from(Instant.now())),
                Tuple4.of("London", "Peter Jones", new BigDecimal("180.00"), Timestamp.from(Instant.now())),
                Tuple4.of("New York", "Jane Smith", new BigDecimal("50.00"), Timestamp.from(Instant.now())),
                Tuple4.of("London", "John Doe", new BigDecimal("300.00"), Timestamp.from(Instant.now())),
                Tuple4.of("Tokyo", "Susan Lee", new BigDecimal("250.00"), Timestamp.from(Instant.now())),
                Tuple4.of("Tokyo", "Susan Lee", new BigDecimal("120.00"), Timestamp.from(Instant.now())),
                Tuple4.of("Madrid", "Maria Garcia", new BigDecimal("500.00"), Timestamp.from(Instant.now())),
                Tuple4.of("Madrid", "Maria Garcia", new BigDecimal("750.00"), Timestamp.from(Instant.now()))
        );

        salesStream
                .assignTimestampsAndWatermarks(WatermarkStrategy.forMonotonousTimestamps())
                .keyBy(0) // city
                .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
                .reduce((value1, value2) -> {
                    if (value1.f2.compareTo(value2.f2) > 0) {
                        return value1;
                    } else {
                        return value2;
                    }
                })
                .map(t -> "City: " + t.f0 + ", Salesman: " + t.f1 + ", Sale: " + t.f2)
                .print();

        env.execute("Top Sales Per City");
    }
}
