import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

public class TopSalesman {

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
                .map(t -> Tuple2.of(t.f1, t.f2))
                .returns(new org.apache.flink.api.java.typeutils.TupleTypeInfo<>(org.apache.flink.api.common.typeinfo.BasicTypeInfo.STRING_TYPE_INFO, org.apache.flink.api.common.typeinfo.BasicTypeInfo.BIG_DEC_TYPE_INFO))
                .keyBy(0) // salesman
                .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
                .reduce((value1, value2) -> Tuple2.of(value1.f0, value1.f1.add(value2.f1)))
                .windowAll(TumblingProcessingTimeWindows.of(Time.seconds(10)))
                .reduce((value1, value2) -> {
                    if (value1.f1.compareTo(value2.f1) > 0) {
                        return value1;
                    } else {
                        return value2;
                    }
                })
                .map(t -> "Top Salesman: " + t.f0 + ", Total Sales: " + t.f1)
                .print();

        env.execute("Top Salesman in the Country");
    }
}
