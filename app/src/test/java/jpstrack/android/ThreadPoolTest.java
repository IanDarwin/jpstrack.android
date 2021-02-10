package jpstrack.android;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadPoolTest {
    @Test
    public void testExecuteAndWait() {
        AtomicBoolean done = new AtomicBoolean(false);
        ThreadUtils.executeAndWait( () -> { done.set(true); });
        assertTrue(done.get());
    }
}
