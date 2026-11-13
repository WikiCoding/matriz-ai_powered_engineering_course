package com.wikicoding.matrizbackend.apigateway.configs;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Strict rolling second, bounded storage, and atomic admission/cleanup. */
public final class IpRateLimiter {
    private static final long WINDOW = 1_000_000_000L;
    private final Map<String, ArrayDeque<Long>> windows = new HashMap<>();
    private final LongSupplier clock;
    private final int capacity;
    private long lastCleanup;

    public IpRateLimiter(LongSupplier clock, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        this.clock = clock;
        this.capacity = capacity;
        this.lastCleanup = clock.getAsLong();
    }

    public synchronized boolean acquire(String ip) {
        long now = clock.getAsLong();
        if (now - lastCleanup >= WINDOW) {
            windows.values().removeIf(times -> now - times.peekLast() >= WINDOW);
            lastCleanup = now;
        }
        var times = windows.get(ip);
        if (times == null) {
            // Never evict an active quota to admit a new address.
            if (windows.size() >= capacity) return false;
            times = new ArrayDeque<>(5);
            windows.put(ip, times);
        }
        while (!times.isEmpty() && now - times.peekFirst() >= WINDOW) times.removeFirst();
        if (times.size() == 5) return false;
        times.addLast(now);
        return true;
    }
}
