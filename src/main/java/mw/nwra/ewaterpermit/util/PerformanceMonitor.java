package mw.nwra.ewaterpermit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Performance monitoring utility for JOIN FETCH optimization
 */
@Component
public class PerformanceMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    /**
     * Monitor method execution time and log performance metrics
     */
    public static <T> T monitor(String operationName, java.util.function.Supplier<T> operation) {
        long startTime = System.currentTimeMillis();
        
        try {
            T result = operation.get();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            log.info("=== PERFORMANCE METRICS ===");
            log.info("Operation: {}", operationName);
            log.info("Execution Time: {} ms", executionTime);
            log.info("Status: SUCCESS");
            
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            log.error("=== PERFORMANCE METRICS ===");
            log.error("Operation: {}", operationName);
            log.error("Execution Time: {} ms", executionTime);
            log.error("Status: FAILED - {}", e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * Log query count for N+1 problem detection
     */
    public static void logQueryCount(String operation, int queryCount) {
        log.info("=== QUERY COUNT METRICS ===");
        log.info("Operation: {}", operation);
        log.info("Total Queries Executed: {}", queryCount);
        
        if (queryCount > 1) {
            log.warn("POTENTIAL N+1 PROBLEM DETECTED: {} queries for single operation", queryCount);
        } else {
            log.info("OPTIMIZED: Single query execution achieved");
        }
    }
}