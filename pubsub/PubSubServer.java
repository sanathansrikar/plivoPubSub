package pubsub;

import com.sun.net.httpserver.HttpServer;
import org.glassfish.tyrus.server.Server;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.stream.Collectors;

public class PubSubServer {

    public static void main(String[] args) throws IOException {
        // Start HTTP server for REST endpoints
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8000), 0);

        httpServer.createContext("/", exchange -> {
            respond(exchange, 200, "{\"endpoints\":[\"/topics\",\"/health\",\"/ws\"]}");
        });

        httpServer.createContext("/topics", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                String name = body.replaceAll(".*\"name\"\\s*:\\s*\"(.*?)\".*", "$1");

                boolean created = TopicRegistry.getInstance().createTopic(name);
                if (!created) {
                    exchange.sendResponseHeaders(409, -1);
                    return;
                }

                respond(exchange, 201,
                        "{\"status\":\"created\",\"topic\":\"" + name + "\"}");
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String topicsJson = TopicRegistry.getInstance().listTopics().stream()
                        .map(t -> "{\"name\":\"" + t.name + "\",\"subscribers\":" + t.subscribers.size() + "}")
                        .collect(Collectors.joining(",", "[", "]"));

                respond(exchange, 200, "{\"topics\":" + topicsJson + "}");
            }
        });

        httpServer.createContext("/health", exchange -> {
            int subs = TopicRegistry.getInstance().totalSubscribers();

            String json = """
                {
                  "topics": %d,
                  "subscribers": %d
                }
                """.formatted(TopicRegistry.getInstance().topicCount(), subs);

            respond(exchange, 200, json);
        });

        httpServer.start();
        System.out.println("HTTP server running on http://localhost:8000");

        // Start WebSocket server on port 8080
        Server wsServer = new Server("0.0.0.0", 8080, "/", null, PubSubWebSocket.class);
        try {
            wsServer.start();
            System.out.println("WebSocket server running on ws://localhost:8080/ws");
            System.out.println("Press Ctrl+C to stop...");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            wsServer.stop();
            httpServer.stop(0);
        }
    }

    private static void respond(
            com.sun.net.httpserver.HttpExchange exchange,
            int code,
            String body) throws IOException {

        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}