package pubsub;

import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopicRegistry {
    private static final TopicRegistry INSTANCE = new TopicRegistry();
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final Map<Session, WebSocketSubscriber> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSubscriber> idToSubscriber = new ConcurrentHashMap<>();

    public static TopicRegistry getInstance() {
        return INSTANCE;
    }

    public boolean createTopic(String name) {
        return topics.putIfAbsent(name, new Topic(name)) == null;
    }

    public Collection<Topic> listTopics() {
        return Collections.unmodifiableCollection(topics.values());
    }

    public int topicCount() {
        return topics.size();
    }

    public int totalSubscribers() {
        return topics.values().stream().mapToInt(t -> t.subscribers.size()).sum();
    }

    public void subscribe(String topicName, Session session, String clientId, int lastN) {
        Topic topic = topics.computeIfAbsent(topicName, Topic::new);
        WebSocketSubscriber sub = sessionMap.computeIfAbsent(session, s -> new WebSocketSubscriber(session, clientId));
        // if clientId changed, ensure mapping updated
        idToSubscriber.put(sub.clientId, sub);
        topic.subscribers.put(sub.clientId, sub);
        // replay last N messages
        List<String> recent = topic.lastN(lastN);
        recent.forEach(sub::enqueue);
    }

    public void unsubscribe(String topicName, String clientId) {
        Topic t = topics.get(topicName);
        if (t == null) return;
        t.subscribers.remove(clientId);
        idToSubscriber.remove(clientId);
    }

    public void publish(String topicName, String messageJson) {
        Topic topic = topics.computeIfAbsent(topicName, Topic::new);
        topic.addHistory(messageJson);
        topic.messages++;
        topic.subscribers.values().forEach(sub -> {
            boolean ok = sub.enqueue(buildEvent(topicName, messageJson));
            if (!ok) topic.dropped++;
        });
    }

    public void unsubscribeAll(Session session) {
        WebSocketSubscriber sub = sessionMap.remove(session);
        if (sub == null) return;
        idToSubscriber.remove(sub.clientId);
        topics.values().forEach(t -> t.subscribers.remove(sub.clientId));
    }

    private static String buildEvent(String topic, String messageJson) {
        return "{\"type\":\"event\",\"topic\":\"" + topic + "\",\"message\":" + messageJson + ",\"ts\":\"" + java.time.Instant.now().toString() + "\"}";
    }
}