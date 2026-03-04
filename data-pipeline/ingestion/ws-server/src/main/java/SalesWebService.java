import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SalesWebService {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/sales", new SalesHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port 8000");
    }

    static class SalesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "[" +
                    "{"sale_id": 5, "salesman_id": 5, "salesman_name": "Maria Garcia", "city": "Madrid", "sale_value": 500.00, "sale_date": "2024-01-11 10:00:00"}," +
                    "{"sale_id": 6, "salesman_id": 5, "salesman_name": "Maria Garcia", "city": "Madrid", "sale_value": 750.00, "sale_date": "2024-01-12 11:00:00"}" +
                    "]";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
