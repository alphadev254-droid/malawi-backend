package mw.nwra.ewaterpermit.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class RateLimiter {
    
    private final ConcurrentMap<String, UserRateLimit> userLimits = new ConcurrentHashMap<>();
    
    private static final int MAX_REQUESTS_PER_MINUTE = 112; // 12 requests per minute = 1 every 5 seconds
    private static final int CLEANUP_INTERVAL_MINUTES = 5;
    private LocalDateTime lastCleanup = LocalDateTime.now();
    
    public boolean isAllowed(String userId) {
        cleanupIfNeeded();
        
        UserRateLimit limit = userLimits.computeIfAbsent(userId, k -> new UserRateLimit());
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(1);
        
        // Remove requests older than 1 minute
        limit.requests.removeIf(timestamp -> timestamp.isBefore(windowStart));
        
        // Check if under limit
        if (limit.requests.size() < MAX_REQUESTS_PER_MINUTE) {
            limit.requests.add(now);
            return true;
        }
        
        return false;
    }
    
    public long getRemainingRequests(String userId) {
        UserRateLimit limit = userLimits.get(userId);
        if (limit == null) {
            return MAX_REQUESTS_PER_MINUTE;
        }
        
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(1);
        limit.requests.removeIf(timestamp -> timestamp.isBefore(windowStart));
        
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - limit.requests.size());
    }
    
    public long getSecondsUntilReset(String userId) {
        UserRateLimit limit = userLimits.get(userId);
        if (limit == null || limit.requests.isEmpty()) {
            return 0;
        }
        
        LocalDateTime oldestRequest = limit.requests.stream()
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
            
        LocalDateTime resetTime = oldestRequest.plusMinutes(1);
        LocalDateTime now = LocalDateTime.now();
        
        return Math.max(0, ChronoUnit.SECONDS.between(now, resetTime));
    }
    
    private void cleanupIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        if (ChronoUnit.MINUTES.between(lastCleanup, now) >= CLEANUP_INTERVAL_MINUTES) {
            LocalDateTime cutoff = now.minusMinutes(2);
            userLimits.entrySet().removeIf(entry -> {
                entry.getValue().requests.removeIf(timestamp -> timestamp.isBefore(cutoff));
                return entry.getValue().requests.isEmpty();
            });
            lastCleanup = now;
        }
    }
    
    private static class UserRateLimit {
        private final java.util.List<LocalDateTime> requests = new java.util.concurrent.CopyOnWriteArrayList<>();
    }
}