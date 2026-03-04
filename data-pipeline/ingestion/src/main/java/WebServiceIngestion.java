import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.types.Row;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WebServiceIngestion {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.addSource(new WebServiceSource())
                .map(row -> Row.of(
                        row.getInt("sale_id"),
                        row.getInt("salesman_id"),
                        row.getString("salesman_name"),
                        row.getString("city"),
                        row.getBigDecimal("sale_value"),
                        Timestamp.valueOf(LocalDateTime.parse(row.getString("sale_date"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                ))
                .returns(new RowTypeInfo(
                        BasicTypeInfo.INT_TYPE_INFO,
                        BasicTypeInfo.INT_TYPE_INFO,
                        BasicTypeInfo.STRING_TYPE_INFO,
                        BasicTypeInfo.STRING_TYPE_INFO,
                        BasicTypeInfo.BIG_DEC_TYPE_INFO,
                        org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo.TIMESTAMP
                ))
                .map(Row::toString)
                .print();

        env.execute("Web Service Ingestion Job");
    }

    private static class WebServiceSource implements SourceFunction<JSONObject> {
        private volatile boolean isRunning = true;

        @Override
        public void run(SourceContext<JSONObject> ctx) throws Exception {
            while (isRunning) {
                URL url = new URL("http://localhost:8000/sales");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                conn.disconnect();

                JSONArray jsonArray = new JSONArray(content.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    ctx.collect(jsonArray.getJSONObject(i));
                }

                // In a real scenario, you would not want to poll this fast.
                // This is just for demonstration purposes.
                Thread.sleep(5000);
            }
        }

        @Override
        public void cancel() {
            isRunning = false;
        }
    }
}
