import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SubmissionController {

    public static void start() throws Exception {

        HttpServer server =
                HttpServer.create(new InetSocketAddress(8081), 0);

        SubmissionService service = new SubmissionService();

        server.createContext("/submission", (HttpExchange exchange) -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath();
                // path is like /submission/123
                String[] parts = path.split("/");
                long id = Long.parseLong(parts[parts.length - 1]);

                String response = service.getSubmission(id);

                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        });

        server.createContext("/submit", (HttpExchange exchange) -> {

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                // Read request body
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody()));

                StringBuilder body = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }

                String sourceCode = body.toString();

                // For now we hardcode problemId and language
                int problemId = 1;
                String language = "java";

                long submissionId =
                        service.submit(problemId, language, sourceCode);

                String response =
                        "{ \"submissionId\": " + submissionId +
                                ", \"status\": \"PENDING\" }";

                exchange.sendResponseHeaders(200,
                        response.getBytes().length);

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        });

        server.start();
        System.out.println("🚀 Server started at http://localhost:8081/submit");
    }
}