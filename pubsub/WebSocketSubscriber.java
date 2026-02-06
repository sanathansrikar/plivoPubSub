package pubsub;

import javax.websocket.Session;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WebSocketSubscriber {
    public String clientId;
    public final Session session;
    public final BlockingQueue<String> queue = new ArrayBlockingQueue<>(50);

    public WebSocketSubscriber(Session session) {
        this.session = session;
        this.clientId = session.getId();
    }

    public WebSocketSubscriber(Session session, String clientId) {
        this.session = session;
        this.clientId = clientId != null ? clientId : session.getId();
    }

    public boolean enqueue(String msg) {
        boolean ok = queue.offer(msg);
        if (ok) flush();
        return ok;
    }

    public void flush() {
        String m;
        while ((m = queue.poll()) != null) {
            try {
                session.getAsyncRemote().sendText(m);
            } catch (Exception e) {
                // stop on send failure
                break;
            }
        }
    }
}