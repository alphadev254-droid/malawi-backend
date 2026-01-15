package mw.nwra.ewaterpermit.controller;

import mw.nwra.ewaterpermit.dto.WRMISApprovedPermitDTO;
import mw.nwra.ewaterpermit.dto.WRMISPermitApplicationDTO;
import mw.nwra.ewaterpermit.service.WRMISAuthService;
import mw.nwra.ewaterpermit.service.WRMISDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WRMIS Data Integration Controller
 * Provides secure endpoints for WRMIS to retrieve permit application and approved permit data
 *
 * Security: IP Whitelisting + JWT Token Authentication
 *
 * Authentication Flow:
 * 1. WRMIS first calls /v1/wrmis/auth/token to get JWT token
 * 2. WRMIS includes token in Authorization header for all data requests
 * 3. Requests are validated by WRMISIPWhitelistFilter (IP check) and this controller (JWT check)
 */
@RestController
@RequestMapping("/v1/wrmis/data")
public class WRMISDataController {

    private static final Logger log = LoggerFactory.getLogger(WRMISDataController.class);

    @Autowired
    private WRMISDataService wrmisDataService;

    @Autowired
    private WRMISAuthService wrmisAuthService;

    /**
     * Get Permit Application Data
     * Query by date range
     *
     * @param dateFrom Start date (optional, format: yyyy-MM-dd)
     * @param dateTo End date (optional, format: yyyy-MM-dd)
     * @param authHeader Authorization header with Bearer token
     * @return List of permit applications
     */
    @GetMapping("/permit-applications")
    public ResponseEntity<Map<String, Object>> getPermitApplications(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // Validate JWT token
            if (!validateToken(authHeader)) {
                return unauthorizedResponse("Invalid or missing JWT token");
            }

            log.info("📊 WRMIS: Fetching permit applications (dateFrom: {}, dateTo: {})", dateFrom, dateTo);

            List<WRMISPermitApplicationDTO> applications = wrmisDataService.getPermitApplications(dateFrom, dateTo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", applications.size());
            response.put("data", applications);
            response.put("query", Map.of(
                    "dateFrom", dateFrom != null ? dateFrom.toString() : "null",
                    "dateTo", dateTo != null ? dateTo.toString() : "null"
            ));
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ WRMIS: Returned {} permit applications", applications.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ WRMIS: Error fetching permit applications: {}", e.getMessage(), e);
            return errorResponse("Error fetching permit applications: " + e.getMessage());
        }
    }

    /**
     * Get Permit Applications by Email
     *
     * @param email Applicant email address
     * @param dateFrom Start date (optional)
     * @param dateTo End date (optional)
     * @param authHeader Authorization header
     * @return List of permit applications for the email
     */
    @GetMapping("/permit-applications/by-email")
    public ResponseEntity<Map<String, Object>> getPermitApplicationsByEmail(
            @RequestParam String email,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) {
                return unauthorizedResponse("Invalid or missing JWT token");
            }

            log.info("📧 WRMIS: Fetching permit applications for email: {}", email);

            List<WRMISPermitApplicationDTO> applications = wrmisDataService.getPermitApplicationsByEmail(email, dateFrom, dateTo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", applications.size());
            response.put("data", applications);
            response.put("query", Map.of(
                    "email", email,
                    "dateFrom", dateFrom != null ? dateFrom.toString() : "null",
                    "dateTo", dateTo != null ? dateTo.toString() : "null"
            ));
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ WRMIS: Returned {} permit applications for email: {}", applications.size(), email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ WRMIS: Error fetching permit applications by email: {}", e.getMessage(), e);
            return errorResponse("Error fetching permit applications: " + e.getMessage());
        }
    }

    /**
     * Get Permit Applications by Specific Date
     *
     * @param date Specific date (format: yyyy-MM-dd)
     * @param authHeader Authorization header
     * @return List of permit applications on that date
     */
    @GetMapping("/permit-applications/by-date")
    public ResponseEntity<Map<String, Object>> getPermitApplicationsByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) {
                return unauthorizedResponse("Invalid or missing JWT token");
            }

            log.info("📅 WRMIS: Fetching permit applications for date: {}", date);

            List<WRMISPermitApplicationDTO> applications = wrmisDataService.getPermitApplicationsByDate(date);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", applications.size());
            response.put("data", applications);
            response.put("query", Map.of("date", date.toString()));
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ WRMIS: Returned {} permit applications for date: {}", applications.size(), date);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ WRMIS: Error fetching permit applications by date: {}", e.getMessage(), e);
            return errorResponse("Error fetching permit applications: " + e.getMessage());
        }
    }

    /**
     * Get Approved Permits
     * Query by date range
     *
     * @param dateFrom Start date (optional, format: yyyy-MM-dd)
     * @param dateTo End date (optional, format: yyyy-MM-dd)
     * @param authHeader Authorization header
     * @return List of approved permits
     */
    @GetMapping("/approved-permits")
    public ResponseEntity<Map<String, Object>> getApprovedPermits(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) {
                return unauthorizedResponse("Invalid or missing JWT token");
            }

            log.info("📊 WRMIS: Fetching approved permits (dateFrom: {}, dateTo: {})", dateFrom, dateTo);

            List<WRMISApprovedPermitDTO> permits = wrmisDataService.getApprovedPermits(dateFrom, dateTo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", permits.size());
            response.put("data", permits);
            response.put("query", Map.of(
                    "dateFrom", dateFrom != null ? dateFrom.toString() : "null",
                    "dateTo", dateTo != null ? dateTo.toString() : "null"
            ));
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ WRMIS: Returned {} approved permits", permits.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ WRMIS: Error fetching approved permits: {}", e.getMessage(), e);
            return errorResponse("Error fetching approved permits: " + e.getMessage());
        }
    }

    /**
     * Get Approved Permits by Email
     *
     * @param email Permit holder email address
     * @param dateFrom Start date (optional)
     * @param dateTo End date (optional)
     * @param authHeader Authorization header
     * @return List of approved permits for the email
     */
    @GetMapping("/approved-permits/by-email")
    public ResponseEntity<Map<String, Object>> getApprovedPermitsByEmail(
            @RequestParam String email,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) {
                return unauthorizedResponse("Invalid or missing JWT token");
            }

            log.info("📧 WRMIS: Fetching approved permits for email: {}", email);

            List<WRMISApprovedPermitDTO> permits = wrmisDataService.getApprovedPermitsByEmail(email, dateFrom, dateTo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", permits.size());
            response.put("data", permits);
            response.put("query", Map.of(
                    "email", email,
                    "dateFrom", dateFrom != null ? dateFrom.toString() : "null",
                    "dateTo", dateTo != null ? dateTo.toString() : "null"
            ));
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ WRMIS: Returned {} approved permits for email: {}", permits.size(), email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ WRMIS: Error fetching approved permits by email: {}", e.getMessage(), e);
            return errorResponse("Error fetching approved permits: " + e.getMessage());
        }
    }

    /**
     * Get Approved Permit by License Number
     *
     * @param licenseNumber License/Permit number
     * @param authHeader Authorization header
     * @return Approved permit details
     */
    @GetMapping("/approved-permits/by-license-number")
    public ResponseEntity<Map<String, Object>> getApprovedPermitByLicenseNumber(
            @RequestParam String licenseNumber,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) {
                return unauthorizedResponse("Invalid or missing JWT token");
            }

            log.info("🔍 WRMIS: Fetching approved permit by license number: {}", licenseNumber);

            WRMISApprovedPermitDTO permit = wrmisDataService.getApprovedPermitByLicenseNumber(licenseNumber);

            if (permit == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No permit found with license number: " + licenseNumber);
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", permit);
            response.put("query", Map.of("licenseNumber", licenseNumber));
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ WRMIS: Returned approved permit for license: {}", licenseNumber);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ WRMIS: Error fetching approved permit by license number: {}", e.getMessage(), e);
            return errorResponse("Error fetching approved permit: " + e.getMessage());
        }
    }

    /**
     * Get Approved Permits by Specific Date
     *
     * @param date Specific date (format: yyyy-MM-dd)
     * @param authHeader Authorization header
     * @return List of approved permits issued on that date
     */
    @GetMapping("/approved-permits/by-date")
    public ResponseEntity<Map<String, Object>> getApprovedPermitsByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) {
                return unauthorizedResponse("Invalid or missing JWT token");
            }

            log.info("📅 WRMIS: Fetching approved permits for date: {}", date);

            List<WRMISApprovedPermitDTO> permits = wrmisDataService.getApprovedPermitsByDate(date);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", permits.size());
            response.put("data", permits);
            response.put("query", Map.of("date", date.toString()));
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ WRMIS: Returned {} approved permits for date: {}", permits.size(), date);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ WRMIS: Error fetching approved permits by date: {}", e.getMessage(), e);
            return errorResponse("Error fetching approved permits: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for WRMIS data service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "WRMIS Data Integration");
        response.put("version", "1.0");
        response.put("timestamp", System.currentTimeMillis());
        response.put("endpoints", List.of(
                "/v1/wrmis/data/permit-applications",
                "/v1/wrmis/data/permit-applications/by-email",
                "/v1/wrmis/data/permit-applications/by-date",
                "/v1/wrmis/data/approved-permits",
                "/v1/wrmis/data/approved-permits/by-email",
                "/v1/wrmis/data/approved-permits/by-license-number",
                "/v1/wrmis/data/approved-permits/by-date"
        ));
        return ResponseEntity.ok(response);
    }

    // ========== Private Helper Methods ==========

    /**
     * Validate JWT token from Authorization header
     */
    private boolean validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ WRMIS: Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);
        boolean isValid = wrmisAuthService.validateToken(token);

        if (!isValid) {
            log.warn("⚠️ WRMIS: Invalid or expired JWT token");
        }

        return isValid;
    }

    /**
     * Create unauthorized response
     */
    private ResponseEntity<Map<String, Object>> unauthorizedResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Unauthorized");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Create error response
     */
    private ResponseEntity<Map<String, Object>> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Internal Server Error");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
