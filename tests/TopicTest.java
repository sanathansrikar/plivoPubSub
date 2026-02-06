package tests;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import pubsub.Topic;

public class TopicTest {

    @Test
    void testTopicCreation() {
        Topic t = new Topic("orders");
        assertEquals("orders", t.name);
        assertEquals(0, t.subscribers.size());
    }
}
