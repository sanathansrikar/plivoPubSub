package tests;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import pubsub.Topic;
import pubsub.Subscriber;

public class FanOutTest {

    @Test
    void testFanOut() {
        Topic topic = new Topic("orders");

        Subscriber s1 = new Subscriber("c1", null);
        Subscriber s2 = new Subscriber("c2", null);

        topic.subscribers.put("c1", s1);
        topic.subscribers.put("c2", s2);

        String msg = "event";
        topic.subscribers.values().forEach(s -> s.queue.offer(msg));

        assertEquals(1, s1.queue.size());
        assertEquals(1, s2.queue.size());
    }
}
