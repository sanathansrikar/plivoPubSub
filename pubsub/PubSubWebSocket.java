package pubsub;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@ServerEndpoint("/ws")
public class PubSubWebSocket {

    @OnOpen
    public void onOpen(Session session) {
        sendInfo(session, "connected", null);
    }

    @OnMessage
    public void onMessage(Session session, String text) {
        try {
            String type = extractStringField(text, "type").orElse("");
            String requestId = extractStringField(text, "request_id").orElse(null);

            switch (type) {
                case "subscribe" -> handleSubscribe(session, text, requestId);
                case "unsubscribe" -> handleUnsubscribe(session, text, requestId);
                case "publish" -> handlePublish(session, text, requestId);
                case "ping" -> sendPong(session, requestId);
                default -> sendError(session, requestId, "BAD_REQUEST", "unknown type: " + type);
            }
        } catch (Exception e) {
            sendError(session, null, "INTERNAL", e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        TopicRegistry.getInstance().unsubscribeAll(session);
    }

    @OnError
    public void onError(Session session, Throwable thr) {
        TopicRegistry.getInstance().unsubscribeAll(session);
        thr.printStackTrace();
    }

    private void handleSubscribe(Session session, String text, String requestId) throws IOException {
        String topic = extractStringField(text, "topic").orElse(null);
        String clientId = extractStringField(text, "client_id").orElse(session.getId());
        int lastN = extractIntField(text, "last_n").orElse(0);

        if (topic == null) {
            sendError(session, requestId, "BAD_REQUEST", "missing topic");
            return;
        }

        TopicRegistry.getInstance().subscribe(topic, session, clientId, lastN);
        sendAck(session, requestId, topic, "subscribed");
    }

    private void handleUnsubscribe(Session session, String text, String requestId) throws IOException {
        String topic = extractStringField(text, "topic").orElse(null);
        String clientId = extractStringField(text, "client_id").orElse(null);

        if (topic == null || clientId == null) {
            sendError(session, requestId, "BAD_REQUEST", "missing topic or client_id");
            return;
        }

        TopicRegistry.getInstance().unsubscribe(topic, clientId);
        sendAck(session, requestId, topic, "unsubscribed");
    }

    private void handlePublish(Session session, String text, String requestId) throws IOException {
        String topic = extractStringField(text, "topic").orElse(null);
        String messageJson = extractJsonField(text, "message").orElse(null);

        if (topic == null || messageJson == null) {
            sendError(session, requestId, "BAD_REQUEST", "missing topic or message");
            return;
        }

        // store & publish raw message JSON
        TopicRegistry.getInstance().publish(topic, messageJson);
        sendAck(session, requestId, topic, "published");
    }

    // ---- send helpers ----
    private void sendAck(Session session, String requestId, String topic, String status) {
        String json = buildSimple("{\"type\":\"ack\",\"request_id\":%s,\"topic\":%s,\"status\":\"%s\",\"ts\":%s}",
                requestId, topic, status);
        sendText(session, json);
    }

    private void sendPong(Session session, String requestId) {
        String json = buildSimple("{\"type\":\"pong\",\"request_id\":%s,\"ts\":%s}", requestId, null, null);
        sendText(session, json);
    }

    private void sendInfo(Session session, String msg, String topic) {
        String body = "{\"type\":\"info\",\"msg\":\"" + msg + "\",\"ts\":" + jsonTs() + "}";
        sendText(session, body);
    }

    private void sendError(Session session, String requestId, String code, String message) {
        String body = "{\"type\":\"error\"," +
                (requestId != null ? "\"request_id\":\"" + requestId + "\"," : "") +
                "\"error\":{\"code\":\"" + code + "\",\"message\":\"" + escape(message) + "\"}," +
                "\"ts\":" + jsonTs() + "}";
        sendText(session, body);
    }

    private void sendText(Session session, String json) {
        try {
            if (session.isOpen()) session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- JSON-ish extraction helpers (simple, supports nested objects for 'message') ----
    private static Optional<String> extractStringField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return Optional.empty();
        int start = idx + pattern.length();
        int end = json.indexOf('"', start);
        if (end == -1) return Optional.empty();
        return Optional.of(json.substring(start, end));
    }

    private static Optional<Integer> extractIntField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*";
        int idx = json.indexOf(pattern);
        if (idx == -1) return Optional.empty();
        int start = idx + pattern.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try {
            return Optional.of(Integer.parseInt(json.substring(start, end)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Extracts JSON value for a given field (object or string). For object it returns raw {...}
    private static Optional<String> extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return Optional.empty();
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon == -1) return Optional.empty();
        int i = colon + 1;
        // skip whitespace
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return Optional.empty();
        char c = json.charAt(i);
        if (c == '{' || c == '[') {
            // find matching bracket
            int depth = 0;
            char open = c, close = (c == '{' ? '}' : ']');
            int j = i;
            while (j < json.length()) {
                char ch = json.charAt(j);
                if (ch == open) depth++;
                else if (ch == close) {
                    depth--;
                    if (depth == 0) {
                        return Optional.of(json.substring(i, j + 1));
                    }
                } else if (ch == '"' ) {
                    // skip string literal
                    j++;
                    while (j < json.length()) {
                        if (json.charAt(j) == '\\') j += 2;
                        else if (json.charAt(j) == '"') break;
                        else j++;
                    }
                }
                j++;
            }
            return Optional.empty();
        } else if (c == '"') {
            // string value
            int start = i + 1;
            int end = json.indexOf('"', start);
            if (end == -1) return Optional.empty();
            return Optional.of(json.substring(start, end));
        } else {
            // primitive (number, true, false, null) -> read until comma or }
            int j = i;
            while (j < json.length() && ",}] \n\r\t".indexOf(json.charAt(j)) == -1) j++;
            return Optional.of(json.substring(i, j).trim());
        }
    }

    private static String buildSimple(String template, String requestId, String topic, String status) {
        String req = (requestId != null) ? ("\"" + escape(requestId) + "\"") : "null";
        String top = (topic != null) ? ("\"" + escape(topic) + "\"") : "null";
        String ts = jsonTs();
        return String.format(template, req, top, escape(status), ts);
    }

    private static String jsonTs() {
        return "\"" + Instant.now().toString() + "\"";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
