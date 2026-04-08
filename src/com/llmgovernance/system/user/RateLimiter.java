package com.llmgovernance.system.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Simple in-memory sliding-window rate limiter.
 *
 * Keeps timestamps per user and allows up to maxRequests in windowMs.
 */
public class RateLimiter {

    private final int maxRequests;
    private final long windowMs;
    private final Map<String, List<Long>> userRequestTimestamps = new HashMap<>();

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public synchronized boolean isAllowed(String userId) {
        String key = (userId == null || userId.isBlank()) ? "anonymous" : userId.trim();
        long now = System.currentTimeMillis();
        long cutoff = now - windowMs;

        cleanupExpired(key, cutoff);

        List<Long> timestamps = userRequestTimestamps.get(key);
        if (timestamps == null) {
            timestamps = new ArrayList<>();
            userRequestTimestamps.put(key, timestamps);
        }

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.add(now);
        return true;
    }

    private void cleanupExpired(String key, long cutoff) {
        List<Long> timestamps = userRequestTimestamps.get(key);
        if (timestamps == null) {
            return;
        }

        Iterator<Long> iterator = timestamps.iterator();
        while (iterator.hasNext()) {
            Long ts = iterator.next();
            if (ts < cutoff) {
                iterator.remove();
            }
        }

        if (timestamps.isEmpty()) {
            userRequestTimestamps.remove(key);
        }
    }
}
