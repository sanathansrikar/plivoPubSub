package pubsub;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Topic {
    public final String name;
    public final Map<String, WebSocketSubscriber> subscribers = new ConcurrentHashMap<>();
    public final Deque<String> history = new ArrayDeque<>();
    public final ReentrantLock lock = new ReentrantLock();

    public int messages = 0;
    public int dropped = 0;
    public static final int HISTORY_LIMIT = 100;

    public Topic(String name) {
        this.name = name;
    }

    public void addHistory(String msg) {
        lock.lock();
        try {
            history.addLast(msg);
            if (history.size() > HISTORY_LIMIT) history.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    public List<String> lastN(int n) {
        lock.lock();
        try {
            int start = Math.max(0, history.size() - n);
            return new ArrayList<>(new ArrayList<>(history).subList(start, history.size()));
        } finally {
            lock.unlock();
        }
    }
}
