package com.example.auth_service.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RateLimitingService {
    private final Map<String, List<Long>> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000; // 15 minutes

    /**
     * Check if the identifier (username:ip) is allowed to make a request
     * @param identifier - Format: "username:ip" or just "ip"
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String identifier) {
        long now = System.currentTimeMillis();
        List<Long> attemptTimes = attempts.computeIfAbsent(identifier, k -> new CopyOnWriteArrayList<>());
        
        // Remove attempts older than the time window
        attemptTimes.removeIf(time -> (now - time) > WINDOW_MS);
        
        // Check if we've exceeded the limit
        if (attemptTimes.size() >= MAX_ATTEMPTS) {
            return false;
        }
        
        // Add current attempt
        attemptTimes.add(now);
        return true;
    }

    /**
     * Get remaining time in seconds until the rate limit resets
     * @param identifier - Format: "username:ip" or just "ip"
     * @return remaining seconds, or 0 if not rate limited
     */
    public long getRemainingSeconds(String identifier) {
        List<Long> attemptTimes = attempts.get(identifier);
        if (attemptTimes == null || attemptTimes.isEmpty()) {
            return 0;
        }
        
        long now = System.currentTimeMillis();
        long oldestAttempt = attemptTimes.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0);
        
        if (oldestAttempt == 0) {
            return 0;
        }
        
        long elapsed = now - oldestAttempt;
        long remaining = WINDOW_MS - elapsed;
        
        return remaining > 0 ? (remaining / 1000) : 0;
    }

    /**
     * Clear attempts for an identifier (useful after successful login)
     * @param identifier - Format: "username:ip" or just "ip"
     */
    public void clearAttempts(String identifier) {
        attempts.remove(identifier);
    }

    /**
     * Clean up old entries to prevent memory leak
     * This should be called periodically (e.g., via @Scheduled)
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(time -> (now - time) > WINDOW_MS);
            return entry.getValue().isEmpty();
        });
    }

    /**
     * Scheduled cleanup to run every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void scheduledCleanup() {
        cleanup();
    }
}

