package pubsub;
import java.net.http.WebSocket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Subscriber {
    public final String clientId;
    public final WebSocket socket;
    public final BlockingQueue<String> queue =
            new ArrayBlockingQueue<>(50);

    public Subscriber(String clientId, WebSocket socket) {
        this.clientId = clientId;
        this.socket = socket;
    }
}
