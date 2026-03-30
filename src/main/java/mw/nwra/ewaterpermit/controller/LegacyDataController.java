package mw.nwra.ewaterpermit.controller;

import mw.nwra.ewaterpermit.dto.WRMISApprovedPermitDTO;
import mw.nwra.ewaterpermit.dto.WRMISPermitApplicationDTO;
import mw.nwra.ewaterpermit.service.LegacyFileDataService;
import mw.nwra.ewaterpermit.service.WRMISAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/wrmis/legacy")
public class LegacyDataController {

    private static final Logger log = LoggerFactory.getLogger(LegacyDataController.class);

    @Autowired
    private LegacyFileDataService legacyFileDataService;

    @Autowired
    private WRMISAuthService wrmisAuthService;

    @Value("${wrmis.legacy.xlsx-path}")
    private String xlsxPath;

    @Value("${wrmis.legacy.csv-path}")
    private String csvPath;

    // ===================== APPROVED PERMITS =====================

    @GetMapping("/approved-permits")
    public ResponseEntity<Map<String, Object>> getApprovedPermits(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) return unauthorizedResponse();

            List<WRMISApprovedPermitDTO> permits = legacyFileDataService.getApprovedPermits(dateFrom, dateTo);

            log.info("Legacy approved permits returned: {}", permits.size());
            return okResponse(permits, Map.of(
                    "dateFrom", dateFrom != null ? dateFrom.toString() : "null",
                    "dateTo", dateTo != null ? dateTo.toString() : "null"
            ));
        } catch (Exception e) {
            log.error("Error fetching legacy approved permits: {}", e.getMessage(), e);
            return errorResponse("Error fetching approved permits: " + e.getMessage());
        }
    }

    @GetMapping("/approved-permits/by-date")
    public ResponseEntity<Map<String, Object>> getApprovedPermitsByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) return unauthorizedResponse();

            List<WRMISApprovedPermitDTO> permits = legacyFileDataService.getApprovedPermitsByDate(date);

            log.info("Legacy approved permits by date {}: {}", date, permits.size());
            return okResponse(permits, Map.of("date", date.toString()));
        } catch (Exception e) {
            log.error("Error fetching legacy approved permits by date: {}", e.getMessage(), e);
            return errorResponse("Error fetching approved permits by date: " + e.getMessage());
        }
    }

    @GetMapping("/approved-permits/by-email")
    public ResponseEntity<Map<String, Object>> getApprovedPermitsByEmail(
            @RequestParam String email,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) return unauthorizedResponse();

            List<WRMISApprovedPermitDTO> permits = legacyFileDataService.getApprovedPermitsByEmail(email, dateFrom, dateTo);

            log.info("Legacy approved permits by email {}: {}", email, permits.size());
            return okResponse(permits, Map.of(
                    "email", email,
                    "dateFrom", dateFrom != null ? dateFrom.toString() : "null",
                    "dateTo", dateTo != null ? dateTo.toString() : "null"
            ));
        } catch (Exception e) {
            log.error("Error fetching legacy approved permits by email: {}", e.getMessage(), e);
            return errorResponse("Error fetching approved permits by email: " + e.getMessage());
        }
    }

    @GetMapping("/approved-permits/by-license-number")
    public ResponseEntity<Map<String, Object>> getApprovedPermitByLicenseNumber(
            @RequestParam String licenseNumber,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) return unauthorizedResponse();

            WRMISApprovedPermitDTO permit = legacyFileDataService.getApprovedPermitByLicenseNumber(licenseNumber);

            if (permit == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No legacy permit found with license number: " + licenseNumber);
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", permit);
            response.put("query", Map.of("licenseNumber", licenseNumber));
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching legacy permit by license number: {}", e.getMessage(), e);
            return errorResponse("Error fetching permit by license number: " + e.getMessage());
        }
    }

    // ===================== PERMIT APPLICATIONS =====================

    @GetMapping("/permit-applications")
    public ResponseEntity<Map<String, Object>> getPermitApplications(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) return unauthorizedResponse();

            List<WRMISPermitApplicationDTO> applications = legacyFileDataService.getPermitApplications(dateFrom, dateTo);

            log.info("Legacy permit applications returned: {}", applications.size());
            return okResponse(applications, Map.of(
                    "dateFrom", dateFrom != null ? dateFrom.toString() : "null",
                    "dateTo", dateTo != null ? dateTo.toString() : "null"
            ));
        } catch (Exception e) {
            log.error("Error fetching legacy permit applications: {}", e.getMessage(), e);
            return errorResponse("Error fetching permit applications: " + e.getMessage());
        }
    }

    @GetMapping("/permit-applications/by-date")
    public ResponseEntity<Map<String, Object>> getPermitApplicationsByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) return unauthorizedResponse();

            List<WRMISPermitApplicationDTO> applications = legacyFileDataService.getPermitApplicationsByDate(date);

            log.info("Legacy permit applications by date {}: {}", date, applications.size());
            return okResponse(applications, Map.of("date", date.toString()));
        } catch (Exception e) {
            log.error("Error fetching legacy permit applications by date: {}", e.getMessage(), e);
            return errorResponse("Error fetching permit applications by date: " + e.getMessage());
        }
    }

    @GetMapping("/permit-applications/by-email")
    public ResponseEntity<Map<String, Object>> getPermitApplicationsByEmail(
            @RequestParam String email,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (!validateToken(authHeader)) return unauthorizedResponse();

            List<WRMISPermitApplicationDTO> applications = legacyFileDataService.getPermitApplicationsByEmail(email, dateFrom, dateTo);

            log.info("Legacy permit applications by email {}: {}", email, applications.size());
            return okResponse(applications, Map.of(
                    "email", email,
                    "dateFrom", dateFrom != null ? dateFrom.toString() : "null",
                    "dateTo", dateTo != null ? dateTo.toString() : "null"
            ));
        } catch (Exception e) {
            log.error("Error fetching legacy permit applications by email: {}", e.getMessage(), e);
            return errorResponse("Error fetching permit applications by email: " + e.getMessage());
        }
    }

    // ===================== HEALTH =====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "WRMIS Legacy File-Based Data");
        response.put("version", "1.0");
        response.put("timestamp", System.currentTimeMillis());
        response.put("sources", Map.of(
                "xlsx", xlsxPath,
                "csv", csvPath
        ));
        response.put("recordCounts", Map.of(
                "approvedPermits", legacyFileDataService.getTotalApprovedPermitsCount(),
                "permitApplications", legacyFileDataService.getTotalPermitApplicationsCount()
        ));
        response.put("endpoints", List.of(
                "/v1/wrmis/legacy/approved-permits",
                "/v1/wrmis/legacy/approved-permits/by-date",
                "/v1/wrmis/legacy/approved-permits/by-email",
                "/v1/wrmis/legacy/approved-permits/by-license-number",
                "/v1/wrmis/legacy/permit-applications",
                "/v1/wrmis/legacy/permit-applications/by-date",
                "/v1/wrmis/legacy/permit-applications/by-email"
        ));
        return ResponseEntity.ok(response);
    }

    // ===================== HELPERS =====================

    private boolean validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header on legacy endpoint");
            return false;
        }
        return wrmisAuthService.validateToken(authHeader.substring(7));
    }

    private ResponseEntity<Map<String, Object>> okResponse(List<?> data, Map<String, String> query) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", data.size());
        response.put("data", data);
        response.put("query", query);
        response.put("source", "legacy-file");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> unauthorizedResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Unauthorized");
        response.put("message", "Invalid or missing JWT token");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Internal Server Error");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
