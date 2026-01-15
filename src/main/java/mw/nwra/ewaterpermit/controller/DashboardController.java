package mw.nwra.ewaterpermit.controller;


import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.service.CoreLicenseService;
import mw.nwra.ewaterpermit.service.CoreLicensePermitService;
import mw.nwra.ewaterpermit.service.WaterAllocationService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/dashboard", produces = "application/json")
public class DashboardController {
    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private CoreLicenseApplicationService licenseApplicationService;

    @Autowired
    private CoreLicenseService licenseService;

    @Autowired
    private CoreLicensePermitService licensePermitService;

    @Autowired
    private WaterAllocationService waterAllocationService;
    
    @Autowired
    private mw.nwra.ewaterpermit.service.CoreApplicationPaymentService coreApplicationPaymentService;
    
    @Autowired
    private mw.nwra.ewaterpermit.repository.CoreApplicationPaymentRepository coreApplicationPaymentRepository;

    @Autowired
    private mw.nwra.ewaterpermit.repository.CoreLicenseApplicationActivityRepository activityRepository;

    // Unified role-based dashboard endpoint
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get current authenticated user using existing pattern
            SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            
            String userRole = currentUser.getSysUserGroup() != null ? 
                currentUser.getSysUserGroup().getName() : "APPLICANT";
            String userId = currentUser.getId();
            String username = currentUser.getUsername();
            
            response.put("userRole", userRole);
            response.put("userId", userId);
            response.put("username", username);
            response.put("timestamp", System.currentTimeMillis());
            
            switch (userRole.toUpperCase()) {
                case "APPLICANT":
                    response.put("data", getApplicantData(userId));
                    break;
                case "LICENSING_OFFICER":
                    response.put("data", getLicensingOfficerData(userId));
                    break;
                case "SENIOR_LICENSING_OFFICER":
                    response.put("data", getSeniorLicensingOfficerData(userId));
                    break;
                case "LICENSING_MANAGER":
                    response.put("data", getLicensingManagerData());
                    break;
                case "DRS":
                    response.put("data", getDRSData());
                    break;
                case "CEO":
                    response.put("data", getCEOData());
                    break;
                case "ACCOUNTANT":
                    response.put("data", getAccountantData(userId));
                    break;
                case "ADMIN":
                    response.put("data", getAdminData());
                    break;
                default:
                    response.put("data", getDefaultDashboardData());
                    break;
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    private Map<String, Object> getApplicantData(String userId) {
        Map<String, Object> data = new HashMap<>();
        
        if (userId == null || userId.isEmpty()) {
            return data;
        }
        
        // Applicant-specific data
        Long myApplications = licenseApplicationService.countByUserId(userId);
        Long myPendingApplications = licenseApplicationService.countPendingApplicationsByUserId(userId);
        Long myApprovedApplications = licenseApplicationService.countByUserIdAndStatus(userId, "APPROVED");
        Long myActiveLicenses = licenseService.countActiveLicensesByUserId(userId);
        
        data.put("myApplications", myApplications != null ? myApplications : 0);
        data.put("myPendingApplications", myPendingApplications != null ? myPendingApplications : 0);
        data.put("myApprovedApplications", myApprovedApplications != null ? myApprovedApplications : 0);
        data.put("myActiveLicenses", myActiveLicenses != null ? myActiveLicenses : 0);
        
        // Recent applications
        List<Object[]> recentApplications = licenseApplicationService.getRecentApplicationsByUserId(userId, 5);
        data.put("recentApplications", recentApplications);
        
        // Application status distribution for pie chart
        List<Object[]> statusDistribution = licenseApplicationService.getApplicationStatusDistributionByUserId(userId);
        Map<String, Long> statusDistributionMap = new HashMap<>();
        for (Object[] row : statusDistribution) {
            String status = row[0] != null ? row[0].toString() : "Unknown";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            statusDistributionMap.put(status, count);
        }
        data.put("applicationStatusDistribution", statusDistributionMap);
        
        // License expiry timeline data
        List<CoreLicense> expiringLicenses = licenseService.getExpiringLicensesByUserId(userId, 90); // Next 90 days
        List<CoreLicense> expiredLicenses = licenseService.getExpiredLicensesByUserId(userId);
        
        Map<String, Object> licenseExpiry = new HashMap<>();
        licenseExpiry.put("expiringLicenses", expiringLicenses);
        licenseExpiry.put("expiredLicenses", expiredLicenses);
        licenseExpiry.put("expiringCount", expiringLicenses.size());
        licenseExpiry.put("expiredCount", expiredLicenses.size());
        data.put("licenseExpiryTimeline", licenseExpiry);
        
        // My licenses tab data
        List<CoreLicense> myLicenses = licenseService.getActiveLicensesByUserId(userId);
        data.put("myLicenses", myLicenses);
        
        return data;
    }

    private Map<String, Object> getLicensingOfficerData(String userId) {
        Map<String, Object> data = new HashMap<>();

        // Get DISTINCT applications this user worked on from core_license_application_activity
        List<mw.nwra.ewaterpermit.model.CoreLicenseApplication> userApplications =
            activityRepository.findApplicationsByUserId(userId);

        // Total distinct applications worked on
        data.put("totalApplicationsWorkedOn", userApplications.size());

        // Count applications by status (only those the user worked on)
        Long submitted = userApplications.stream()
            .filter(app -> app.getCoreApplicationStatus() != null &&
                          "SUBMITTED".equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
            .count();

        Long fieldAssessmentApproved = userApplications.stream()
            .filter(app -> app.getCoreApplicationStatus() != null &&
                          "FIELD_ASSESSMENT_APPROVED".equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
            .count();

        data.put("submitted", submitted);
        data.put("fieldAssessmentApproved", fieldAssessmentApproved);

        // Count by activity types from core_license_application_activity
        // This requires a custom query - for now showing basic counts
        data.put("applicationsSubmitted", submitted);
        data.put("applicationsApproved", fieldAssessmentApproved);

        // Recent applications this user worked on (limited to 10)
        List<Map<String, Object>> recentApplications = userApplications.stream()
            .limit(10)
            .map(app -> {
                Map<String, Object> appMap = new HashMap<>();
                appMap.put("id", app.getId());
                appMap.put("applicationType", app.getApplicationType());
                appMap.put("dateSubmitted", app.getDateSubmitted());
                appMap.put("status", app.getCoreApplicationStatus() != null ?
                          app.getCoreApplicationStatus().getName() : "UNKNOWN");
                return appMap;
            })
            .collect(java.util.stream.Collectors.toList());

        data.put("recentApplications", recentApplications);

        // Application distribution by type (only for applications user worked on)
        Map<String, Long> applicationsByType = userApplications.stream()
            .filter(app -> app.getCoreLicenseType() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                app -> app.getCoreLicenseType().getName(),
                java.util.stream.Collectors.counting()
            ));

        data.put("applicationsByType", applicationsByType);

        return data;
    }

    private Map<String, Object> getSeniorLicensingOfficerData(String userId) {
        Map<String, Object> data = new HashMap<>();

        // Get DISTINCT applications this user worked on from core_license_application_activity
        List<mw.nwra.ewaterpermit.model.CoreLicenseApplication> userApplications =
            activityRepository.findApplicationsByUserId(userId);

        // Total distinct applications worked on
        data.put("totalApplicationsWorkedOn", userApplications.size());

        // Count applications by status (only those the user worked on)
        Long submitted = userApplications.stream()
            .filter(app -> app.getCoreApplicationStatus() != null &&
                          "SUBMITTED".equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
            .count();

        Long fieldAssessmentApproved = userApplications.stream()
            .filter(app -> app.getCoreApplicationStatus() != null &&
                          "FIELD_ASSESSMENT_APPROVED".equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
            .count();

        Long rejected = userApplications.stream()
            .filter(app -> app.getCoreApplicationStatus() != null &&
                          "REJECTED".equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
            .count();

        Long referredBack = userApplications.stream()
            .filter(app -> app.getCoreApplicationStatus() != null &&
                          "REFERRED_BACK".equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
            .count();

        Long revisionRequested = userApplications.stream()
            .filter(app -> app.getCoreApplicationStatus() != null &&
                          "REVISION_REQUESTED".equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
            .count();

        data.put("submitted", submitted);
        data.put("fieldAssessmentApproved", fieldAssessmentApproved);
        data.put("rejected", rejected);
        data.put("referredBack", referredBack);
        data.put("revisionRequested", revisionRequested);

        // Recent applications this user worked on (limited to 10)
        List<Map<String, Object>> recentApplications = userApplications.stream()
            .limit(10)
            .map(app -> {
                Map<String, Object> appMap = new HashMap<>();
                appMap.put("id", app.getId());
                appMap.put("applicationType", app.getApplicationType());
                appMap.put("dateSubmitted", app.getDateSubmitted());
                appMap.put("status", app.getCoreApplicationStatus() != null ?
                          app.getCoreApplicationStatus().getName() : "UNKNOWN");
                return appMap;
            })
            .collect(java.util.stream.Collectors.toList());

        data.put("recentApplications", recentApplications);

        // Application distribution by type (only for applications user worked on)
        Map<String, Long> applicationsByType = userApplications.stream()
            .filter(app -> app.getCoreLicenseType() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                app -> app.getCoreLicenseType().getName(),
                java.util.stream.Collectors.counting()
            ));

        data.put("applicationsByType", applicationsByType);

        return data;
    }

    private Map<String, Object> getLicensingManagerData() {
        Map<String, Object> data = new HashMap<>();

        // Manager-level statistics (ALL applications - system-wide)
        Long totalApplications = licenseApplicationService.count();
        Long totalApplicationsThisMonth = licenseApplicationService.countApplicationsThisMonth();
        Long approvedThisMonth = licenseApplicationService.countByStatusAndCurrentMonth("APPROVED");
        Long rejectedThisMonth = licenseApplicationService.countByStatusAndCurrentMonth("REJECTED");
        Long pendingManagerApproval = licenseApplicationService.countByApplicationStatusName("MANAGER_REVIEW");

        data.put("totalApplications", totalApplications != null ? totalApplications : 0);
        data.put("totalApplicationsThisMonth", totalApplicationsThisMonth != null ? totalApplicationsThisMonth : 0);
        data.put("approvedThisMonth", approvedThisMonth != null ? approvedThisMonth : 0);
        data.put("rejectedThisMonth", rejectedThisMonth != null ? rejectedThisMonth : 0);
        data.put("pendingManagerApproval", pendingManagerApproval != null ? pendingManagerApproval : 0);

        // License statistics (optimized queries to avoid N+1)
        Long totalLicenses = licenseService.count();
        Long activeLicenses = licenseService.countByStatus("ACTIVE");
        Long expiredLicenses = licenseService.countByStatus("EXPIRED");

        // Expiring licenses (optimized with direct count queries)
        Long expiringIn1Month = licenseService.countLicensesExpiringInDays(30);
        Long expiringIn2Months = licenseService.countLicensesExpiringInDays(60);
        Long expiringIn3Months = licenseService.countLicensesExpiringInDays(90);

        data.put("totalLicenses", totalLicenses != null ? totalLicenses : 0);
        data.put("activeLicenses", activeLicenses != null ? activeLicenses : 0);
        data.put("expiredLicenses", expiredLicenses != null ? expiredLicenses : 0);
        data.put("expiringIn1Month", expiringIn1Month != null ? expiringIn1Month : 0);
        data.put("expiringIn2Months", expiringIn2Months != null ? expiringIn2Months : 0);
        data.put("expiringIn3Months", expiringIn3Months != null ? expiringIn3Months : 0);

        // Performance metrics
        List<Object[]> officerPerformance = licenseApplicationService.getOfficerPerformanceMetrics();
        data.put("officerPerformance", officerPerformance);

        // Application trends
        List<Object[]> monthlyTrends = licenseApplicationService.getMonthlyApplicationsCurrentYear();
        data.put("monthlyTrends", monthlyTrends);

        return data;
    }

    private Map<String, Object> getDRSData() {
        Map<String, Object> data = new HashMap<>();

        // System-wide application statistics
        Long totalApplications = licenseApplicationService.count();
        Long totalApplicationsThisMonth = licenseApplicationService.countApplicationsThisMonth();
        Long approvedThisMonth = licenseApplicationService.countByStatusAndCurrentMonth("APPROVED");
        Long rejectedThisMonth = licenseApplicationService.countByStatusAndCurrentMonth("REJECTED");

        data.put("totalApplications", totalApplications != null ? totalApplications : 0);
        data.put("totalApplicationsThisMonth", totalApplicationsThisMonth != null ? totalApplicationsThisMonth : 0);
        data.put("approvedThisMonth", approvedThisMonth != null ? approvedThisMonth : 0);
        data.put("rejectedThisMonth", rejectedThisMonth != null ? rejectedThisMonth : 0);

        // License statistics
        Long totalLicenses = licenseService.count();
        Long activeLicenses = licenseService.countByStatus("ACTIVE");
        Long expiredLicenses = licenseService.countByStatus("EXPIRED");
        Long expiringIn3Months = licenseService.countLicensesExpiringInDays(90);

        data.put("totalLicenses", totalLicenses != null ? totalLicenses : 0);
        data.put("activeLicenses", activeLicenses != null ? activeLicenses : 0);
        data.put("expiredLicenses", expiredLicenses != null ? expiredLicenses : 0);
        data.put("expiringIn3Months", expiringIn3Months != null ? expiringIn3Months : 0);

        // Water source analysis
        List<Object[]> waterSourceAnalysis = licenseApplicationService.getApplicationsByWaterSource();
        data.put("waterSourceAnalysis", waterSourceAnalysis);

        // Regional distribution
        List<Object[]> regionalDistribution = licenseApplicationService.getApplicationsByRegion();
        data.put("regionalDistribution", regionalDistribution);

        // Application trends
        List<Object[]> monthlyTrends = licenseApplicationService.getMonthlyApplicationsCurrentYear();
        data.put("monthlyTrends", monthlyTrends);

        return data;
    }

    private Map<String, Object> getCEOData() {
        Map<String, Object> data = new HashMap<>();

        // Executive summary - system-wide statistics
        Long totalApplications = licenseApplicationService.count();
        Long totalPermits = licensePermitService.count();
        Long totalApplicationsThisMonth = licenseApplicationService.countApplicationsThisMonth();
        Long approvedThisMonth = licenseApplicationService.countByStatusAndCurrentMonth("APPROVED");

        data.put("totalApplications", totalApplications != null ? totalApplications : 0);
        data.put("totalPermits", totalPermits != null ? totalPermits : 0);
        data.put("totalRevenue", calculateTotalRevenue());
        data.put("totalApplicationsThisMonth", totalApplicationsThisMonth != null ? totalApplicationsThisMonth : 0);
        data.put("approvedThisMonth", approvedThisMonth != null ? approvedThisMonth : 0);

        // License statistics
        Long totalLicenses = licenseService.count();
        Long activeLicenses = licenseService.countByStatus("ACTIVE");
        Long expiredLicenses = licenseService.countByStatus("EXPIRED");
        Long expiringIn1Month = licenseService.countLicensesExpiringInDays(30);
        Long expiringIn3Months = licenseService.countLicensesExpiringInDays(90);

        data.put("totalLicenses", totalLicenses != null ? totalLicenses : 0);
        data.put("activeLicenses", activeLicenses != null ? activeLicenses : 0);
        data.put("expiredLicenses", expiredLicenses != null ? expiredLicenses : 0);
        data.put("expiringIn1Month", expiringIn1Month != null ? expiringIn1Month : 0);
        data.put("expiringIn3Months", expiringIn3Months != null ? expiringIn3Months : 0);

        // Strategic metrics
        List<Object[]> yearlyTrends = licenseApplicationService.getYearlyApplicationTrends();
        data.put("yearlyTrends", yearlyTrends);

        // Application trends
        List<Object[]> monthlyTrends = licenseApplicationService.getMonthlyApplicationsCurrentYear();
        data.put("monthlyTrends", monthlyTrends);

        data.put("revenueByType", List.of()); // Placeholder for now

        // High-value applications
        List<Object[]> highValueApplications = licenseApplicationService.getHighValueApplications();
        data.put("highValueApplications", highValueApplications);

        return data;
    }

    private Map<String, Object> getAccountantData(String userId) {
        Map<String, Object> data = new HashMap<>();

        try {
            // Get DISTINCT applications this user worked on (accountant activities: APPROVE_PAYMENT)
            List<mw.nwra.ewaterpermit.model.CoreLicenseApplication> userApplications =
                activityRepository.findApplicationsByUserId(userId);

            data.put("totalApplicationsWorkedOn", userApplications.size());

            // ===== OPTIMIZED: Use database aggregation queries instead of loading all payments =====

            // Get payment statistics by status (single query with aggregation)
            List<Object[]> paymentStats = coreApplicationPaymentRepository.getPaymentStatsByStatus();
            long totalPayments = 0;
            long pendingPaymentsCount = 0;
            long verifiedPayments = 0;
            double totalPendingAmount = 0.0;

            for (Object[] stat : paymentStats) {
                String status = (String) stat[0];
                Long count = ((Number) stat[1]).longValue();
                Double amount = stat[2] != null ? ((Number) stat[2]).doubleValue() : 0.0;

                totalPayments += count;

                if ("PENDING".equalsIgnoreCase(status) || "AWAITING_APPROVAL".equalsIgnoreCase(status)) {
                    pendingPaymentsCount += count;
                    totalPendingAmount += amount;
                }

                if ("VERIFIED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) {
                    verifiedPayments += count;
                }
            }

            // Calculate monthly revenue using optimized query (single database call)
            Double monthlyRevenue = coreApplicationPaymentRepository.calculateMonthlyRevenue();

            data.put("totalPayments", totalPayments);
            data.put("pendingPayments", (long) totalPendingAmount);
            data.put("pendingPaymentsCount", pendingPaymentsCount);
            data.put("verifiedPayments", verifiedPayments);
            data.put("monthlyRevenue", monthlyRevenue != null ? monthlyRevenue.longValue() : 0L);

            // Add test data to verify API is working
            data.put("testData", "ACCOUNTANT_API_WORKING");
            data.put("invoicesGenerated", totalPayments);

            // Applications requiring invoice generation
            Long approvedApplications = licenseApplicationService.countByApplicationStatusName("APPROVED");
            data.put("pendingInvoicesCount", approvedApplications != null ? approvedApplications : 0);

            // Applications requiring payment verification
            Long pendingVerification = licenseApplicationService.countByApplicationStatusName("PAYMENT_VERIFICATION");
            data.put("pendingVerificationCount", pendingVerification != null ? pendingVerification : 0);

            // Payment method distribution using optimized query (single database call)
            List<Object[]> methodStats = coreApplicationPaymentRepository.getPaymentMethodDistribution();
            Map<String, Long> paymentMethodDistribution = new HashMap<>();
            Map<String, Double> paymentMethodAmounts = new HashMap<>();

            for (Object[] stat : methodStats) {
                String method = (String) stat[0];
                Long count = ((Number) stat[1]).longValue();
                Double amount = stat[2] != null ? ((Number) stat[2]).doubleValue() : 0.0;

                paymentMethodDistribution.put(method, count);
                paymentMethodAmounts.put(method, amount);
            }

            data.put("paymentMethodDistribution", paymentMethodDistribution);
            data.put("paymentMethodAmounts", paymentMethodAmounts);

            // Recent payments using optimized query (single database call, limited to 10)
            List<Object[]> recentPayments = coreApplicationPaymentRepository.getRecentPayments(
                org.springframework.data.domain.PageRequest.of(0, 10)
            );
            data.put("recentPayments", recentPayments);

            log.info("=== OPTIMIZED ACCOUNTANT DASHBOARD ===");
            log.info("Total payments: " + totalPayments);
            log.info("Payment stats queries executed: 4 (vs loading all records)");
            log.info("=== END ACCOUNTANT DASHBOARD ===");

        } catch (Exception e) {
            log.error("Error loading accountant dashboard data: " + e.getMessage(), e);
            // Fallback with mock data for testing
            data.put("totalPayments", 0);
            data.put("pendingPayments", 0);
            data.put("pendingPaymentsCount", 0);
            data.put("verifiedPayments", 0);
            data.put("monthlyRevenue", 0);
            data.put("invoicesGenerated", 0);
            data.put("pendingInvoicesCount", 0);
            data.put("pendingVerificationCount", 0);
            data.put("paymentMethodDistribution", new HashMap<>());
            data.put("paymentMethodAmounts", new HashMap<>());
            data.put("recentPayments", new ArrayList<>());
        }

        return data;
    }

    private Map<String, Object> getAdminData() {
        Map<String, Object> data = new HashMap<>();
        
        // System-wide statistics
        Long totalUsers = licenseApplicationService.getTotalUsers();
        Long activeUsers = licenseApplicationService.getActiveUsers();
        Long systemErrors = licenseApplicationService.getSystemErrors();
        Long dataIntegrityIssues = licenseApplicationService.getDataIntegrityIssues();
        
        data.put("totalUsers", totalUsers != null ? totalUsers : 0);
        data.put("activeUsers", activeUsers != null ? activeUsers : 0);
        data.put("systemErrors", systemErrors != null ? systemErrors : 0);
        data.put("dataIntegrityIssues", dataIntegrityIssues != null ? dataIntegrityIssues : 0);
        
        // System health metrics
        List<Object[]> userActivity = licenseApplicationService.getUserActivityMetrics();
        data.put("userActivity", userActivity);
        
        // All role data combined for admin overview
        data.putAll(getSystemOverview());
        
        return data;
    }

    private Map<String, Object> getDefaultDashboardData() {
        Map<String, Object> data = new HashMap<>();
        
        // Basic statistics for unknown roles
        Long totalApplications = licenseApplicationService.count();
        Long totalPermits = licensePermitService.count();
        
        data.put("totalApplications", totalApplications != null ? totalApplications : 0);
        data.put("totalPermits", totalPermits != null ? totalPermits : 0);
        
        return data;
    }

    private Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // High-level system metrics
        Long totalApplications = licenseApplicationService.count();
        Long totalPermits = licensePermitService.count();
        
        overview.put("systemTotalApplications", totalApplications != null ? totalApplications : 0);
        overview.put("systemTotalPermits", totalPermits != null ? totalPermits : 0);
        overview.put("systemTotalRevenue", calculateTotalRevenue());
        
        return overview;
    }
    
    /**
     * Calculate total revenue using optimized database aggregation query
     * This replaces the inefficient approach of loading all payment records
     * @return Total revenue as Long
     */
    private Long calculateTotalRevenue() {
        try {
            // Use database aggregation query - much more efficient than loading all records
            Double revenue = coreApplicationPaymentRepository.calculateTotalRevenue();
            
            log.info("=== OPTIMIZED REVENUE CALCULATION ===");
            log.info("Revenue calculated using database aggregation: " + revenue);
            log.info("=== END REVENUE CALCULATION ===");
            
            return revenue != null ? revenue.longValue() : 0L;
        } catch (Exception e) {
            log.error("Error calculating revenue using aggregation query: " + e.getMessage());
            e.printStackTrace();
            return 0L;
        }
    }

    // Legacy endpoints (maintain for backward compatibility)
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Total applications
        Long totalApplications = licenseApplicationService.count();
        stats.put("totalApplications", totalApplications != null ? totalApplications : 0);

        // Total active licenses
        Long activeLicenses = licenseService.countByStatus("ACTIVE");
        stats.put("activeLicenses", activeLicenses != null ? activeLicenses : 0);

        // Total permits
        Long totalPermits = licensePermitService.count();
        stats.put("totalPermits", totalPermits != null ? totalPermits : 0);

        // Pending applications (assuming status name for pending)
        Long pendingApplications = licenseApplicationService.countByApplicationStatusName("PENDING");
        stats.put("pendingApplications", pendingApplications != null ? pendingApplications : 0);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/water-permits")
    public ResponseEntity<Map<String, Object>> getWaterPermitsData() {
        Map<String, Object> data = new HashMap<>();

        // Get applications by status for distribution chart
        List<Object[]> applicationsByStatus = licenseApplicationService.getApplicationsByStatus();
        Map<String, Long> statusDistribution = new HashMap<>();
        for (Object[] row : applicationsByStatus) {
            String status = row[0] != null ? row[0].toString() : "Unknown";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            statusDistribution.put(status, count);
        }
        data.put("statusDistribution", statusDistribution);

        // Get applications by license type for type distribution
        List<Object[]> applicationsByType = licenseApplicationService.getApplicationsByLicenseType();
        Map<String, Long> typeDistribution = new HashMap<>();
        for (Object[] row : applicationsByType) {
            String type = row[0] != null ? row[0].toString() : "Unknown";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            typeDistribution.put(type, count);
        }
        data.put("typeDistribution", typeDistribution);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/water-permits-distribution")
    public ResponseEntity<Map<String, Object>> getWaterPermitsDistribution() {
        Map<String, Object> data = new HashMap<>();

        // Get permits by district/region
        List<Object[]> permitsByRegion = licensePermitService.getPermitsByRegion();
        Map<String, Long> regionDistribution = new HashMap<>();
        for (Object[] row : permitsByRegion) {
            String region = row[0] != null ? row[0].toString() : "Unknown";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            regionDistribution.put(region, count);
        }
        data.put("regionDistribution", regionDistribution);

        // Get permits by water source type
        List<Object[]> permitsByWaterSource = licensePermitService.getPermitsByWaterSourceType();
        Map<String, Long> waterSourceDistribution = new HashMap<>();
        for (Object[] row : permitsByWaterSource) {
            String sourceType = row[0] != null ? row[0].toString() : "Unknown";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            waterSourceDistribution.put(sourceType, count);
        }
        data.put("waterSourceDistribution", waterSourceDistribution);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/water-allocation-level")
    public ResponseEntity<List<Map<String, Object>>> getWaterAllocationLevel() {
        try {
            List<Map<String, Object>> data = waterAllocationService.getWaterAllocationData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            // Fallback to mock data if service fails
            List<Map<String, Object>> fallbackData = new ArrayList<>();
            Map<String, Object> area = new HashMap<>();
            area.put("area", "No data available");
            area.put("abstraction", 0.0);
            area.put("runoff", 0.0);
            area.put("level", 0.0);
            area.put("status", "MINIMAL");
            area.put("permits", 0);
            fallbackData.add(area);
            return ResponseEntity.ok(fallbackData);
        }
    }

    // Applicant-specific endpoints for dashboard tabs
    @GetMapping("/applicant/applications")
    public ResponseEntity<Map<String, Object>> getMyApplications(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            
            String userId = currentUser.getId();
            Map<String, Object> response = new HashMap<>();
            
            // Get paginated applications
            List<Object[]> applications = licenseApplicationService.getRecentApplicationsByUserId(userId, limit);
            
            // Get status distribution for filtering
            List<Object[]> statusDistribution = licenseApplicationService.getApplicationStatusDistributionByUserId(userId);
            Map<String, Long> statusMap = new HashMap<>();
            for (Object[] row : statusDistribution) {
                String status = row[0] != null ? row[0].toString() : "Unknown";
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                statusMap.put(status, count);
            }
            
            response.put("applications", applications);
            response.put("statusDistribution", statusMap);
            response.put("totalCount", licenseApplicationService.countByUserId(userId));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    @GetMapping("/applicant/licenses")
    public ResponseEntity<Map<String, Object>> getMyLicenses(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            
            String userId = currentUser.getId();
            Map<String, Object> response = new HashMap<>();
            
            // Get all licenses for the user
            List<CoreLicense> activeLicenses = licenseService.getActiveLicensesByUserId(userId); 
            List<CoreLicense> expiringLicenses = licenseService.getExpiringLicensesByUserId(userId, 90);
            List<CoreLicense> expiredLicenses = licenseService.getExpiredLicensesByUserId(userId);
            
            response.put("activeLicenses", activeLicenses);
            response.put("expiringLicenses", expiringLicenses);
            response.put("expiredLicenses", expiredLicenses);
            response.put("totalActive", activeLicenses.size());
            response.put("totalExpiring", expiringLicenses.size());
            response.put("totalExpired", expiredLicenses.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    @GetMapping("/applicant/quick-actions")
    public ResponseEntity<Map<String, Object>> getQuickActions(
            @RequestHeader("Authorization") String token
    ) {
        try {
            SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            
            String userId = currentUser.getId();
            Map<String, Object> response = new HashMap<>();
            
            // Get actionable items for quick actions
            Long pendingApplications = licenseApplicationService.countByUserIdAndStatus(userId, "PENDING");
            Long underReviewApplications = licenseApplicationService.countByUserIdAndStatus(userId, "UNDER_REVIEW");
            Long documentsRequiredApplications = licenseApplicationService.countByUserIdAndStatus(userId, "DOCUMENTS_REQUIRED");
            List<CoreLicense> expiringLicenses = licenseService.getExpiringLicensesByUserId(userId, 30); // 30 days
            
            response.put("pendingApplications", pendingApplications != null ? pendingApplications : 0);
            response.put("underReviewApplications", underReviewApplications != null ? underReviewApplications : 0);
            response.put("documentsRequiredApplications", documentsRequiredApplications != null ? documentsRequiredApplications : 0);
            response.put("expiringLicensesCount", expiringLicenses.size());
            response.put("expiringLicenses", expiringLicenses);
            
            // Common quick actions
            List<Map<String, Object>> quickActions = new ArrayList<>();
            
            Map<String, Object> newApplication = new HashMap<>();
            newApplication.put("title", "New Application");
            newApplication.put("description", "Submit a new water permit application");
            newApplication.put("action", "new_application");
            newApplication.put("icon", "plus");
            quickActions.add(newApplication);
            
            Map<String, Object> renewLicense = new HashMap<>();
            renewLicense.put("title", "Renew License");
            renewLicense.put("description", "Renew your expiring licenses");
            renewLicense.put("action", "renew_license");
            renewLicense.put("icon", "refresh");
            renewLicense.put("badge", expiringLicenses.size() > 0 ? expiringLicenses.size() : null);
            quickActions.add(renewLicense);
            
            Map<String, Object> viewApplications = new HashMap<>();
            viewApplications.put("title", "View Applications");
            viewApplications.put("description", "Check status of your applications");
            viewApplications.put("action", "view_applications");
            viewApplications.put("icon", "list");
            quickActions.add(viewApplications);
            
            Map<String, Object> paymentHistory = new HashMap<>();
            paymentHistory.put("title", "Payment History");
            paymentHistory.put("description", "View your payment history");
            paymentHistory.put("action", "payment_history");
            paymentHistory.put("icon", "credit-card");
            quickActions.add(paymentHistory);
            
            response.put("quickActions", quickActions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }


}