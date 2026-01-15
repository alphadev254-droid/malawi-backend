package mw.nwra.ewaterpermit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mw.nwra.ewaterpermit.model.SysConfig;
import mw.nwra.ewaterpermit.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * IP Whitelisting Filter for WRMIS API Endpoints
 * Only allows requests from approved WRMIS IP addresses to access WRMIS endpoints
 * Configuration is fetched from sys_config table (admin-configurable)
 */
@Component
public class WRMISIPWhitelistFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WRMISIPWhitelistFilter.class);

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Only apply IP whitelisting to WRMIS endpoints
        if (requestURI.startsWith("/v1/wrmis/")) {

            String clientIP = getClientIP(request);
            log.info("🔒 WRMIS Request from IP: {} to endpoint: {}", clientIP, requestURI);

            // Fetch WRMIS configuration from database
            String whitelistedIPs = null;
            try {
                SysConfig config = sysConfigService.getSystemConfig();
                if (config != null) {
                    whitelistedIPs = config.getWrmisAllowedIps();
                }
            } catch (Exception e) {
                log.error("❌ Error fetching WRMIS configuration: {}", e.getMessage());
                sendUnauthorizedResponse(response, "WRMIS configuration error");
                return;
            }

            // If wrmisAllowedIps is empty/null, allow all IPs (admin hasn't restricted access)
            if (whitelistedIPs == null || whitelistedIPs.trim().isEmpty()) {
                log.info("⚠️ WRMIS IP Whitelisting is DISABLED (empty config) - allowing all IPs");
                filterChain.doFilter(request, response);
                return;
            }

            // Parse whitelisted IPs from database
            List<String> allowedIPs = parseWhitelistedIPs(whitelistedIPs);

            // Check if client IP is whitelisted
            if (!isIPWhitelisted(clientIP, allowedIPs)) {
                log.warn("🚫 BLOCKED: Unauthorized WRMIS access attempt from IP: {}", clientIP);
                sendUnauthorizedResponse(response, "Access denied: IP not whitelisted");
                return;
            }

            log.info("✅ WRMIS IP {} is whitelisted - allowing request", clientIP);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Get the actual client IP address, considering proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String clientIP = request.getHeader("X-Forwarded-For");

        if (clientIP == null || clientIP.isEmpty() || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getHeader("X-Real-IP");
        }

        if (clientIP == null || clientIP.isEmpty() || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getRemoteAddr();
        }

        // X-Forwarded-For may contain multiple IPs, get the first one
        if (clientIP != null && clientIP.contains(",")) {
            clientIP = clientIP.split(",")[0].trim();
        }

        return clientIP;
    }

    /**
     * Parse comma-separated list of whitelisted IPs
     */
    private List<String> parseWhitelistedIPs(String whitelistedIPsString) {
        if (whitelistedIPsString == null || whitelistedIPsString.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(whitelistedIPsString.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .toList();
    }

    /**
     * Check if IP is in whitelist (supports exact match or CIDR notation)
     */
    private boolean isIPWhitelisted(String clientIP, List<String> allowedIPs) {
        // For now, exact match. Can be extended to support CIDR notation
        return allowedIPs.contains(clientIP) ||
               allowedIPs.contains("0.0.0.0") || // Allow all (for testing only!)
               "127.0.0.1".equals(clientIP) || // Always allow localhost for testing
               "0:0:0:0:0:0:0:1".equals(clientIP); // IPv6 localhost
    }

    /**
     * Send 403 Forbidden response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\": \"Forbidden\", \"message\": \"%s\", \"status\": 403}",
            message
        ));
    }
}
