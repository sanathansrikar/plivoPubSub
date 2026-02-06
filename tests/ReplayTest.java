package tests;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import pubsub.Topic;
import pubsub.Subscriber;

public class ReplayTest {

    @Test
    void testReplayLastN() {
        Topic topic = new Topic("orders");

        for (int i = 1; i <= 5; i++) {
            topic.history.addLast("msg-" + i);
        }

        Subscriber s = new Subscriber("c1", null);
        topic.history.stream()
                .skip(topic.history.size() - 3)
                .forEach(s.queue::offer);

        assertEquals(3, s.queue.size());
    }
}
