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
                // 1. Rate Limiting (Day 13)
                String clientIP = exchange.getRemoteAddress().getAddress().getHostAddress();
                try (var jedis = RedisUtil.getClient()) {
                    long requests = jedis.incr("rate_limit:" + clientIP);
                    if (requests == 1) {
                        jedis.expire("rate_limit:" + clientIP, 60); // 60s window
                    }
                    if (requests > 5) { // Max 5 requests per minute
                        String error = "{ \"error\": \"Rate limit exceeded. Try again later.\" }";
                        exchange.sendResponseHeaders(429, error.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(error.getBytes());
                        os.close();
                        return;
                    }
                } catch (Exception ignored) {
                    // Proceed if Redis is temporarily down
                }

                // Read request body + 2. Max Size Limit (Day 13)
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody()));

                StringBuilder body = new StringBuilder();
                String line;
                int totalSize = 0;
                final int MAX_CODE_SIZE = 50 * 1024; // 50 KB

                while ((line = reader.readLine()) != null) {
                    body.append(line);
                    totalSize += line.length();
                    if (totalSize > MAX_CODE_SIZE) {
                        String error = "{ \"error\": \"Payload too large. Maximum 50KB allowed.\" }";
                        exchange.sendResponseHeaders(413, error.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(error.getBytes());
                        os.close();
                        return;
                    }
                }

                String sourceCode = body.toString();

                // 3. Language Allowlist (Day 13) - naive check since it's JSON
                if (!sourceCode.contains("\"language\":\"java\"") &&
                    !sourceCode.contains("\"language\":\"python\"") &&
                    !sourceCode.contains("\"language\":\"cpp\"")) {
                    String error = "{ \"error\": \"Unsupported language. Allowed: java, python, cpp\" }";
                    exchange.sendResponseHeaders(400, error.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(error.getBytes());
                    os.close();
                    return;
                }

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