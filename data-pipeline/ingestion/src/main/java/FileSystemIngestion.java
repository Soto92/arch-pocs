import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.Row;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileSystemIngestion {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        FileSource<String> fileSource = FileSource.forRecordStream(
                new TextLineInputFormat(),
                new Path("../data/files/sales.csv")
        ).build();

        env.fromSource(fileSource, WatermarkStrategy.noWatermarks(), "File Source")
                .map(line -> {
                    String[] fields = line.split(",");
                    // Skip header
                    if (fields[0].equals("sale_id")) {
                        return null;
                    }
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    return Row.of(
                            Integer.parseInt(fields[0]),
                            Integer.parseInt(fields[1]),
                            fields[2],
                            fields[3],
                            new BigDecimal(fields[4]),
                            Timestamp.valueOf(LocalDateTime.parse(fields[5], formatter))
                    );
                })
                .returns(new RowTypeInfo(
                        BasicTypeInfo.INT_TYPE_INFO,
                        BasicTypeInfo.INT_TYPE_INFO,
                        BasicTypeInfo.STRING_TYPE_INFO,
                        BasicTypeInfo.STRING_TYPE_INFO,
                        BasicTypeInfo.BIG_DEC_TYPE_INFO,
                        org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo.TIMESTAMP
                ))
                .filter(java.util.Objects::nonNull)
                .map(Row::toString)
                .print();

        env.execute("File System Ingestion Job");
    }
}
