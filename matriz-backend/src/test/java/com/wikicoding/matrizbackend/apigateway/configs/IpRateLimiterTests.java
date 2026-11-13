package com.wikicoding.matrizbackend.apigateway.configs;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IpRateLimiterTests {
    @Test
    void enforcesRollingWindowAndIndependentAddresses() {
        // Arrange
        var time = new AtomicLong();
        var limiter = new IpRateLimiter(time::get, 10);
        // Act
        var admitted = IntStream.range(0, 6).mapToObj(i -> limiter.acquire("a")).toList();
        boolean other = limiter.acquire("b");
        time.set(999_999_999L);
        boolean beforeExpiry = limiter.acquire("a");
        time.incrementAndGet();
        boolean atExpiry = limiter.acquire("a");
        // Assert
        assertEquals(java.util.List.of(true, true, true, true, true, false), admitted);
        assertTrue(other);
        assertFalse(beforeExpiry);
        assertTrue(atExpiry);
    }

    @Test
    void capacityNeverEvictsActiveQuotaAndExpiredEntriesAreReclaimed() {
        // Arrange
        var time = new AtomicLong();
        var limiter = new IpRateLimiter(time::get, 1);
        // Act
        boolean initial = limiter.acquire("a");
        boolean full = limiter.acquire("b");
        time.set(1_000_000_000L);
        boolean reclaimed = limiter.acquire("b");
        // Assert
        assertTrue(initial);
        assertFalse(full);
        assertTrue(reclaimed);
    }

    @Test
    void concurrentRequestsAdmitExactlyFive() throws Exception {
        // Arrange
        var limiter = new IpRateLimiter(() -> 0L, 10);
        var work = IntStream.range(0, 100)
                .<java.util.concurrent.Callable<Boolean>>mapToObj(i -> () -> limiter.acquire("a")).toList();
        // Act
        long accepted = 0;
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var result : executor.invokeAll(work)) if (result.get()) accepted++;
        }
        // Assert
        assertEquals(5, accepted);
    }
}
