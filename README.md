# Plivo PubSub

Simple in-memory pub/sub server with HTTP management endpoints and a WebSocket JSON protocol.

## Overview
- HTTP endpoints:
  - `GET /topics` — list topics
  - `POST /topics` — create topic (JSON `{"name":"orders"}`)
  - `GET /health` — basic health/metrics
- WebSocket endpoint:
  - `/ws` implements a JSON protocol (subscribe / unsubscribe / publish / ping)

## Requirements
Place runtime and API jars into `libs/` before compiling/running:
- `javax-websocket-api-1.1.jar` (API)
- A WebSocket implementation compatible with `javax.websocket` (e.g. Tyrus 1.x + its runtime jars). Put implementation jars into `libs/`.

## Build & Run (local JVM)
1. Compile:
```bash
javac -cp "libs/*" -d out pubsub/*.java
```
2. Run:
```bash
java -cp "out;libs/*" pubsub.PubSubServer
```
- HTTP server listens on port `8000`.
- WebSocket server listens on port `8080`.

## Docker
- Ensure `libs/` contains required jars, then:
```bash
docker build -t plivo-pubsub .
docker run -p 8000:8000 -p 8080:8080 plivo-pubsub
```

## WebSocket Protocol (summary)
Client → Server JSON:
- Fields: `type` = `subscribe | unsubscribe | publish | ping`
- Common fields: `topic`, `client_id`, `last_n`, `request_id`
- `publish` includes `message`:
```json
{
  "type":"publish",
  "topic":"orders",
  "message": { "id":"uuid", "payload": { "order_id":"ORD-123", "amount":99.5 } },
  "request_id":"req-123"
}
```

Server → Client JSON:
- `type` = `ack | event | error | pong | info`
- Examples:
  - Ack:
    ```json
    {"type":"ack","request_id":"req-123","topic":"orders","status":"ok","ts":"2025-08-25T10:00:00Z"}
    ```
  - Event:
    ```json
    {"type":"event","topic":"orders","message":{ "id":"uuid","payload":{...} },"ts":"..."}
    ```
  - Error:
    ```json
    {"type":"error","request_id":"req-123","error":{"code":"BAD_REQUEST","message":"..."},"ts":"..."}
    ```

Error codes you may see:
- `TOPIC_NOT_FOUND`, `SLOW_CONSUMER`, `BAD_REQUEST`, `UNAUTHORIZED`, `INTERNAL`

## Tests
Compile tests (requires `junit-platform-console-standalone` jar present):
```bash
javac -cp "out;libs/*;junit-platform-console-standalone-1.10.1.jar" -d out tests/*.java
java -jar junit-platform-console-standalone-1.10.1.jar --class-path "out;libs/*" --scan-class-path
```

## Notes
- The repo expects `libs/` to be present in the project root if you choose to vendor jars. Alternatively use Maven/Gradle to manage dependencies.
- If you want, I can add a known-compatible Tyrus 1.x bundle into `libs/` and update the Dockerfile accordingly.