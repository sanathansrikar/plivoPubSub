package tests;
import pubsub.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ArrayBlockingQueue;

public class BackPressureTest {

    @Test
    void testQueueOverflow() {
        ArrayBlockingQueue<String> q = new ArrayBlockingQueue<>(2);
        assertTrue(q.offer("a"));
        assertTrue(q.offer("b"));
        assertFalse(q.offer("c"));
    }
}
