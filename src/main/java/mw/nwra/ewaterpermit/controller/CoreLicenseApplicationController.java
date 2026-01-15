package mw.nwra.ewaterpermit.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import mw.nwra.ewaterpermit.model.CoreApplicationPayment;
import mw.nwra.ewaterpermit.model.CoreApplicationStep;
import mw.nwra.ewaterpermit.model.CoreLicenseAssessment;
import mw.nwra.ewaterpermit.service.CoreLicenseAssessmentService;
import mw.nwra.ewaterpermit.service.PaymentService;
import mw.nwra.ewaterpermit.service.SysUserAccountService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.constant.Action;
import mw.nwra.ewaterpermit.dto.CoreLicenseApplicationSummaryDTO;
import mw.nwra.ewaterpermit.dto.PaymentSummaryDTO;
import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.repository.CoreApplicationPaymentRepository;
import mw.nwra.ewaterpermit.repository.CoreLicenseApplicationRepository;
import mw.nwra.ewaterpermit.service.CoreApplicationPaymentService;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.service.NotificationService;
import mw.nwra.ewaterpermit.model.UserNotification;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/license-applications", produces = "application/json")
public class CoreLicenseApplicationController {
    private static final Logger log = LoggerFactory.getLogger(CoreLicenseApplicationController.class);
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CoreLicenseApplicationService coreLicenseApplicationService;

    @Autowired
    private mw.nwra.ewaterpermit.service.CoreLicenseTypeService coreLicenseTypeService;

    @Autowired
    private mw.nwra.ewaterpermit.service.CoreApplicationStatusService coreApplicationStatusService;

    @Autowired
    private mw.nwra.ewaterpermit.service.CoreApplicationStepService coreApplicationStepService;

    @Autowired
    private mw.nwra.ewaterpermit.service.EmailQueueService emailQueueService;

    @Autowired
    private mw.nwra.ewaterpermit.service.OfficerNotificationService officerNotificationService;

    @Autowired
    private mw.nwra.ewaterpermit.service.CoreApplicationDocumentService documentService;

    @Autowired
    private mw.nwra.ewaterpermit.service.CoreDocumentCategoryService documentCategoryService;

    @Autowired
    private CoreLicenseAssessmentService assessmentService;

    @Autowired
    private mw.nwra.ewaterpermit.service.CoreWaterSourceService coreWaterSourceService;

    @Autowired
    private mw.nwra.ewaterpermit.service.CoreLicenseService coreLicenseService;

    @Autowired
    private SysUserAccountService sysUserAccountService;
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CoreApplicationPaymentService corePaymentService;
    
    @Autowired
    private mw.nwra.ewaterpermit.service.ApplicationDataLinkingService applicationDataLinkingService;

    @Autowired
    private CoreLicenseApplicationRepository coreLicenseApplicationRepository;

    @Autowired
    private CoreApplicationPaymentRepository coreApplicationPaymentRepository;

    @Autowired
    private Auditor auditor;

    @Autowired
    private NotificationService notificationService;

    /**
     * Water category enum for license type classification
     */
    public enum WaterCategory {
        GROUNDWATER("GROUNDWATER"),
        SURFACE_WATER("SURFACE_WATER"),
        DRILLING_WATER("DRILLING_WATER"),
        EFFLUENT("EFFLUENT"),
        UNKNOWN("UNKNOWN");

        private final String value;

        WaterCategory(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Get water category from license type name
     */
    private WaterCategory getWaterCategory(String licenseTypeName) {
        if (licenseTypeName == null) {
            return WaterCategory.UNKNOWN;
        }

        String lower = licenseTypeName.toLowerCase();

        if (lower.contains("ground water") || lower.contains("groundwater")) {
            return WaterCategory.GROUNDWATER;
        }

        if (lower.contains("surface water")) {
            return WaterCategory.SURFACE_WATER;
        }

        if (lower.contains("drilling") || lower.contains("borehole")) {
            return WaterCategory.DRILLING_WATER;
        }

        if (lower.contains("effluent") || lower.contains("discharge")) {
            return WaterCategory.EFFLUENT;
        }

        return WaterCategory.UNKNOWN;
    }

    /**
     * Find transfer license type based on original license category
     */
    private mw.nwra.ewaterpermit.model.CoreLicenseType findTransferLicenseType(WaterCategory category) {
        String transferTypeName = null;

        switch (category) {
            case GROUNDWATER:
                transferTypeName = "Transfer Ground Water License";
                break;
            case SURFACE_WATER:
                transferTypeName = "Transfer Surface Water License";
                break;
            case EFFLUENT:
                transferTypeName = "Transfer Effluent Discharge Permit";
                break;
            case DRILLING_WATER:
                // For drilling/borehole, there might not be a specific transfer type
                // Fall back to the original logic
                return null;
            default:
                return null;
        }

        if (transferTypeName != null) {
            try {
                return coreLicenseTypeService.getCoreLicenseTypeByName(transferTypeName);
            } catch (Exception e) {
                log.warn("Transfer license type not found: {}", transferTypeName);
                return null;
            }
        }

        return null;
    }

    @GetMapping(path = "/test")
    public Map<String, Object> testEndpoint() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Controller is working");
        response.put("timestamp", new java.util.Date());
        return response;
    }

    @GetMapping(path = "/count")
    public Map<String, Long> countAll() {
        Long count = this.coreLicenseApplicationService.count();
        Map<String, Long> response = new java.util.HashMap<>();
        response.put("count", count);
        return response;
    }
    @GetMapping(path = "all-applications")
    public List<Map<String, Object>> getRoleCoreLicenseApplications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestHeader(name = "Authorization", required = false) String token) {

        // Get current user for role-based filtering
        SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
        String userRole = currentUser != null && currentUser.getSysUserGroup() != null ?
                currentUser.getSysUserGroup().getName().toLowerCase() : "";

        List<CoreLicenseApplication> applications = this.coreLicenseApplicationService.getAllCoreLicenseApplications();
        log.info(userRole);
        return List.of();
        // Filter and map applications based on user role
//        return applications.stream()
//                .filter(app -> isApplicationVisibleToUser(app, userRole))
//                .map(app -> {
//                    CoreApplicationStep step = app.getCoreApplicationStep();
//                    Map<String, Object> appData = new java.util.HashMap<>();
//                    appData.put("id", app.getId());
//                    appData.put("step", step == null ? null : step.getId());
//                    appData.put("status", app.getCoreApplicationStatus() != null ? app.getCoreApplicationStatus().getName() : "UNKNOWN");
//                    appData.put("applicationDate", app.getDateCreated());
//                    // Handle user account data safely to prevent null pointer exceptions
//                    if (app.getSysUserAccount() != null) {
//                        String firstName = app.getSysUserAccount().getFirstName() != null ? app.getSysUserAccount().getFirstName() : "";
//                        String lastName = app.getSysUserAccount().getLastName() != null ? app.getSysUserAccount().getLastName() : "";
//                        String fullName = (firstName + " " + lastName).trim();
//                        if (fullName.isEmpty()) {
//                            fullName = app.getSysUserAccount().getUsername() != null ? app.getSysUserAccount().getUsername() : "Unknown";
//                        }
//                        appData.put("applicantName", fullName);
//                        appData.put("firstName", firstName);
//                        appData.put("lastName", lastName);
//                        appData.put("applicantEmail", app.getSysUserAccount().getEmailAddress() != null ? app.getSysUserAccount().getEmailAddress() : "");
//                    } else {
//                        appData.put("applicantName", "Unknown Applicant");
//                        appData.put("firstName", "");
//                        appData.put("lastName", "");
//                        appData.put("applicantEmail", "");
//                        log.warn("Application {} has null user account", app.getId());
//                    }
//                    appData.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
//                    double feeAmount = app.getCoreLicenseType() != null ? app.getCoreLicenseType().getApplicationFees() : 20000.0;
//                    appData.put("applicationFees", feeAmount);
//
//                    // Add current step sequence for debugging
//                    if (step != null) {
//                        appData.put("currentStepSequence", step.getSequenceNumber());
//                        appData.put("currentStepName", step.getName());
//                    }
//
//                    // Add JSON fields for UI display
//                    appData.put("clientInfo", app.getClientInfo());
//                    appData.put("locationInfo", app.getLocationInfo());
//                    appData.put("applicationMetadata", app.getApplicationMetadata());
//                    appData.put("formSpecificData", app.getFormSpecificData());
//
//                    // Check for payment receipt and update status
//                    Map<String, Object> paymentStatus = checkPaymentReceiptStatus(app.getId(), feeAmount);
//                    appData.put("paymentStatus", paymentStatus);
////                    double licenseFeeAmount = app.getCoreLicenseType() != null ? app.getCoreLicenseType().getLicenseFees() : 0;
////
////                    appData.put("licenseFee", licenseFeeAmount);
////
////                    Map<String, Object> LicenseFeePayStatus = checkPaymentReceiptStatus(app.getId(), licenseFeeAmount);
////                    appData.put("licenseFeeStatus", LicenseFeePayStatus);
//                    appData.put("payments", app.getCoreApplicationPayments());
//
//                    return appData;
//                }).collect(java.util.stream.Collectors.toList());
    }

    @GetMapping(path = "")
    @Transactional(readOnly = true)
    public Map<String, Object> getCoreLicenseApplications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo,
            @RequestHeader(name = "Authorization", required = false) String token) {

        log.info("=== GETTING ALL APPLICATIONS (ADMIN) ===");
        log.info("Page: {}, Limit: {}", page, limit);

        // Get current user for role-based filtering
        SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
        String userRole = currentUser != null && currentUser.getSysUserGroup() != null ?
                currentUser.getSysUserGroup().getName().toLowerCase() : "";
        log.info("User role: {}", userRole);

        // Use role-filtered version to avoid application-level filtering
        List<CoreLicenseApplication> applications = this.coreLicenseApplicationService.getAllCoreLicenseApplicationsWithPaymentsByRole(userRole, page, limit, dateFrom, dateTo, "dateCreated", "desc");

        log.info("Fetched {} applications from database", applications.size());

        // Get total count for pagination metadata - optimize for small datasets
        long totalElements;
        if (applications.size() < limit) {
            // If we got fewer results than requested, we're on the last page
            totalElements = (long) page * limit + applications.size();
        } else {
            // Only run expensive count query when necessary for roles other than admin
            if (userRole.equals("admin")) {
                totalElements = this.coreLicenseApplicationService.count();
            } else {
                // For non-admin roles, estimate based on current results
                // This avoids running complex count queries with role filters
                totalElements = (long) (page + 1) * limit;
            }
        }
        int totalPages = (int) Math.ceil((double) totalElements / limit);
        
        log.info("Total applications for role '{}': {}, Current page: {}, Total pages: {}", userRole, totalElements, page, totalPages);
        
        // Map applications (no need for visibility filtering since it's done at SQL level)
        List<Map<String, Object>> applicationData = applications.stream()
                .map(app -> {
                    CoreApplicationStep step = app.getCoreApplicationStep();
                    Map<String, Object> appData = new java.util.HashMap<>();
                    appData.put("id", app.getId());
                    appData.put("step", step == null ? null : step.getId());
                    appData.put("status", app.getCoreApplicationStatus() != null ? app.getCoreApplicationStatus().getName() : "UNKNOWN");
                    appData.put("applicationDate", app.getDateCreated());
                    // Handle user account data safely, fallback to client_info JSON
                    if (app.getSysUserAccount() != null) {
                        String firstName = app.getSysUserAccount().getFirstName() != null ? app.getSysUserAccount().getFirstName() : "";
                        String lastName = app.getSysUserAccount().getLastName() != null ? app.getSysUserAccount().getLastName() : "";
                        String fullName = (firstName + " " + lastName).trim();
                        if (fullName.isEmpty()) {
                            fullName = app.getSysUserAccount().getUsername() != null ? app.getSysUserAccount().getUsername() : "Unknown";
                        }
                        appData.put("applicantName", fullName);
                        appData.put("firstName", firstName);
                        appData.put("lastName", lastName);
                        appData.put("applicantEmail", app.getSysUserAccount().getEmailAddress() != null ? app.getSysUserAccount().getEmailAddress() : "");
                    } else {
                        // Try to get client info from JSON field
                        try {
                            if (app.getClientInfo() != null) {
                                Map<String, Object> clientInfo = objectMapper.readValue(app.getClientInfo(), Map.class);
                                String clientName = (String) clientInfo.get("clientName");
                                String email = (String) clientInfo.get("email");
                                appData.put("applicantName", clientName != null ? clientName : "Unknown Applicant");
                                appData.put("applicantEmail", email != null ? email : "");
                                appData.put("firstName", "");
                                appData.put("lastName", "");
                            } else {
                                appData.put("applicantName", "Unknown Applicant");
                                appData.put("firstName", "");
                                appData.put("lastName", "");
                                appData.put("applicantEmail", "");
                            }
                        } catch (Exception e) {
                            appData.put("applicantName", "Unknown Applicant");
                            appData.put("firstName", "");
                            appData.put("lastName", "");
                            appData.put("applicantEmail", "");
                        }
                        log.warn("Application {} has null user account, using client_info", app.getId());
                    }
                    appData.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
                    double feeAmount = app.getCoreLicenseType() != null ? app.getCoreLicenseType().getApplicationFees() : 20000.0;
                    appData.put("applicationFees", feeAmount);

                    // Add current step sequence for debugging
                    if (step != null) {
                        appData.put("currentStepSequence", step.getSequenceNumber());
                        appData.put("currentStepName", step.getName());
                    }

                    // Add JSON fields for UI display (excluding formSpecificData to reduce packet size)
                    appData.put("clientInfo", app.getClientInfo());
                    appData.put("locationInfo", app.getLocationInfo());
                    appData.put("applicationMetadata", app.getApplicationMetadata());

                    // Add emergency application fields
                    appData.put("applicationPriority", app.getApplicationPriority());
                    appData.put("emergencyReason", app.getEmergencyReason());
                    appData.put("emergencyJustificationFile", app.getEmergencyJustificationFile());
                    appData.put("emergencySubmittedDate", app.getEmergencySubmittedDate());

                    // Log emergency fields for debugging
                    if ("EMERGENCY".equals(app.getApplicationPriority())) {
                        log.info("=== EMERGENCY APPLICATION DETECTED ===");
                        log.info("Application ID: {}", app.getId());
                        log.info("Priority: {}", app.getApplicationPriority());
                        log.info("Reason: {}", app.getEmergencyReason());
                        log.info("File Path: {}", app.getEmergencyJustificationFile());
                        log.info("Submitted Date: {}", app.getEmergencySubmittedDate());
                        log.info("File exists in response: {}", appData.get("emergencyJustificationFile"));
                    }

                    // Set default payment status to avoid N+1 queries
                    Map<String, Object> paymentStatus = new java.util.HashMap<>();
                    paymentStatus.put("amount", feeAmount);
                    paymentStatus.put("status", "PENDING");
                    paymentStatus.put("message", "Payment required");
                    appData.put("paymentStatus", paymentStatus);

                    // Add application-specific license fee (set by manager)
                    appData.put("licenseFee", app.getLicenseFee());
                    appData.put("licenseFeeSetByUserId", app.getLicenseFeeSetByUserId());
                    appData.put("licenseFeeSetDate", app.getLicenseFeeSetDate());

                    // Now using eagerly loaded payments to avoid N+1 query problem
                    appData.put("payments", app.getCoreApplicationPayments());
                    
                    // Add assessment data if available (extracted from temporary storage)
                    Map<String, Object> assessmentData = new java.util.HashMap<>();
                    try {
                        if (app.getFormSpecificData() != null && app.getFormSpecificData().startsWith("{")) {
                            Map<String, Object> tempData = objectMapper.readValue(app.getFormSpecificData(), Map.class);
                            if (tempData.containsKey("calculatedAnnualRental")) {
                                assessmentData = tempData;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore JSON parsing errors
                    }
                    appData.put("assessmentData", assessmentData);

                    return appData;
                }).collect(java.util.stream.Collectors.toList());
        
        // Return paginated response with metadata
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("data", applicationData);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);
        response.put("pageSize", limit);
        
        log.info("Returning {} applications out of {} total", applicationData.size(), totalElements);
        return response;
    }


    @GetMapping(path = "/{id}")
    public Map<String, Object> getCoreLicenseApplicationById(
            @PathVariable(name = "id") String id,
            @RequestHeader(name = "Authorization", required = false) String token) {
        try {
            log.info("=== GETTING APPLICATION BY ID FOR REVIEW ===");
            log.info("Application ID: {}", id);
            log.info("Request from: {}", token != null ? "Authenticated user" : "Anonymous");

            // Get current user info for debugging
            if (token != null) {
                SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
                if (currentUser != null) {
                    String userRole = currentUser.getSysUserGroup() != null ?
                            currentUser.getSysUserGroup().getName() : "No role";
                    log.info("Current user: {} (Role: {})", currentUser.getUsername(), userRole);
                }
            }

            CoreLicenseApplication app = this.coreLicenseApplicationService
                    .getCoreLicenseApplicationById(id);
            if (app == null) {
                log.error("Application not found with ID: {}", id);
                throw new EntityNotFoundException("Application not found with ID: " + id);
            }

            log.info("=== APPLICATION FOUND ===");
            log.info("Status: {}", app.getCoreApplicationStatus() != null ? app.getCoreApplicationStatus().getName() : "UNKNOWN");
            log.info("Step: {}", app.getCoreApplicationStep() != null ? app.getCoreApplicationStep().getName() : "None");
            log.info("Step Sequence: {}", app.getCoreApplicationStep() != null ? app.getCoreApplicationStep().getSequenceNumber() : "None");
            log.info("User: {} {}", app.getSysUserAccount().getFirstName(), app.getSysUserAccount().getLastName());

            // Return DTO with complete application details to avoid Hibernate collection sharing issues
            Map<String, Object> appData = new java.util.HashMap<>();
            appData.put("id", app.getId());
            appData.put("status", app.getCoreApplicationStatus() != null ? app.getCoreApplicationStatus().getName() : "UNKNOWN");
            appData.put("applicationDate", app.getDateCreated());
            appData.put("dateSubmitted", app.getDateSubmitted());
            
            // Add JSON fields for UI display
            appData.put("clientInfo", app.getClientInfo());
            appData.put("locationInfo", app.getLocationInfo());
            appData.put("applicationMetadata", app.getApplicationMetadata());
            appData.put("formSpecificData", app.getFormSpecificData());

            // Handle user account data safely
            if (app.getSysUserAccount() != null) {
                String firstName = app.getSysUserAccount().getFirstName() != null ? app.getSysUserAccount().getFirstName() : "";
                String lastName = app.getSysUserAccount().getLastName() != null ? app.getSysUserAccount().getLastName() : "";
                String fullName = (firstName + " " + lastName).trim();
                if (fullName.isEmpty()) {
                    fullName = app.getSysUserAccount().getUsername() != null ? app.getSysUserAccount().getUsername() : "Unknown";
                }
                appData.put("applicantName", fullName);
                appData.put("firstName", firstName);
                appData.put("lastName", lastName);
                appData.put("applicantEmail", app.getSysUserAccount().getEmailAddress() != null ? app.getSysUserAccount().getEmailAddress() : "");
            } else {
                appData.put("applicantName", "Unknown Applicant");
                appData.put("firstName", "");
                appData.put("lastName", "");
                appData.put("applicantEmail", "");
            }

            appData.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");

            if (app.getCoreApplicationStep() != null) {
                appData.put("currentStepSequence", app.getCoreApplicationStep().getSequenceNumber());
                appData.put("currentStepName", app.getCoreApplicationStep().getName());
            }

            // ==> SOURCE LOCATION DETAILS
            log.info("=== ADDING SOURCE LOCATION DETAILS ===");
            appData.put("sourceEasting", app.getSourceEasting());
            appData.put("sourceNorthing", app.getSourceNorthing());
            appData.put("sourceVillage", app.getSourceVillage());
            appData.put("sourceTa", app.getSourceTa());
            appData.put("sourceHectarage", app.getSourceHectarage());
            appData.put("sourceOwnerFullname", app.getSourceOwnerFullname());
            appData.put("sourcePlotNumber", app.getSourcePlotNumber());

            log.info("Source Details - Easting: {}, Northing: {}, Village: {}, TA: {}",
                    app.getSourceEasting(), app.getSourceNorthing(), app.getSourceVillage(), app.getSourceTa());
            log.info("Source Details - Hectarage: {}, Owner: {}, Plot: {}",
                    app.getSourceHectarage(), app.getSourceOwnerFullname(), app.getSourcePlotNumber());

            // ==> DESTINATION LOCATION DETAILS
            log.info("=== ADDING DESTINATION LOCATION DETAILS ===");
            appData.put("destEasting", app.getDestEasting());
            appData.put("destNorthing", app.getDestNorthing());
            appData.put("destVillage", app.getDestVillage());
            appData.put("destTa", app.getDestTa());
            appData.put("destHectarage", app.getDestHectarage());
            appData.put("destOwnerFullname", app.getDestOwnerFullname());
            appData.put("destPlotNumber", app.getDestPlotNumber());

            log.info("Dest Details - Easting: {}, Northing: {}, Village: {}, TA: {}",
                    app.getDestEasting(), app.getDestNorthing(), app.getDestVillage(), app.getDestTa());
            log.info("Dest Details - Hectarage: {}, Owner: {}, Plot: {}",
                    app.getDestHectarage(), app.getDestOwnerFullname(), app.getDestPlotNumber());

            // ==> PERMIT AND UTILITY DETAILS
            log.info("=== ADDING PERMIT AND UTILITY DETAILS ===");
            appData.put("permitDuration", app.getPermitDuration());
            appData.put("nearbyWaterUtilityBoard", app.getNearbyWaterUtilityBoard());
            appData.put("altWaterSource", app.getAltWaterSource());
            appData.put("altOtherWater", app.getAltOtherWater());
            appData.put("existingBoreholeCount", app.getExistingBoreholeCount());
            appData.put("boardMinutes", app.getBoardMinutes());
            appData.put("boardApprovalDate", app.getBoardApprovalDate());

            log.info("Permit Duration: {}, Utility Board: {}, Alt Water Source: {}",
                    app.getPermitDuration(), app.getNearbyWaterUtilityBoard(), app.getAltWaterSource());
            log.info("Existing Boreholes: {}, Board Minutes: {}, Board Approval: {}",
                    app.getExistingBoreholeCount(),
                    app.getBoardMinutes() != null ? "Present" : "None",
                    app.getBoardApprovalDate());

            // ==> RELATED ENTITIES
            log.info("=== ADDING RELATED ENTITY DETAILS ===");
            if (app.getCoreWaterSource() != null) {
                appData.put("waterSourceId", app.getCoreWaterSource().getId());
                appData.put("waterSourceName", app.getCoreWaterSource().getName());
                log.info("Water Source: {} (ID: {})", app.getCoreWaterSource().getName(), app.getCoreWaterSource().getId());
            }

            if (app.getSourceLandRegime() != null) {
                appData.put("sourceLandRegimeId", app.getSourceLandRegime().getId());
                appData.put("sourceLandRegimeName", app.getSourceLandRegime().getName());
                log.info("Source Land Regime: {} (ID: {})", app.getSourceLandRegime().getName(), app.getSourceLandRegime().getId());
            }

            if (app.getDestLandRegime() != null) {
                appData.put("destLandRegimeId", app.getDestLandRegime().getId());
                appData.put("destLandRegimeName", app.getDestLandRegime().getName());
                log.info("Dest Land Regime: {} (ID: {})", app.getDestLandRegime().getName(), app.getDestLandRegime().getId());
            }

            if (app.getSourceWru() != null) {
                appData.put("sourceWruId", app.getSourceWru().getId());
                appData.put("sourceWruGeoType", app.getSourceWru().getGeoType());
                log.info("Source WRU ID: {}, GeoType: {}", app.getSourceWru().getId(), app.getSourceWru().getGeoType());
            }

            if (app.getDestWru() != null) {
                appData.put("destWruId", app.getDestWru().getId());
                appData.put("destWruGeoType", app.getDestWru().getGeoType());
                log.info("Dest WRU ID: {}, GeoType: {}", app.getDestWru().getId(), app.getDestWru().getGeoType());
            }

            log.info("=== APPLICATION DATA PREPARED FOR REVIEW ===");
            log.info("Response contains {} fields", appData.size());
            log.info("Key fields: id={}, status={}, applicantName={}, licenseType={}",
                    appData.get("id"), appData.get("status"), appData.get("applicantName"), appData.get("licenseType"));
            log.info("Location data: sourceVillage={}, sourceTa={}, sourceHectarage={}",
                    appData.get("sourceVillage"), appData.get("sourceTa"), appData.get("sourceHectarage"));
            return appData;
        } catch (Exception e) {
            log.error("Error getting application by ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve application: " + e.getMessage());
        }
    }

    @GetMapping(path = "/by-license/{licenseId}")
    public Map<String, Object> getApplicationByLicenseId(
            @PathVariable(name = "licenseId") String licenseId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        try {
            log.info("=== GETTING APPLICATION BY LICENSE ID ===");
            log.info("License ID: {}", licenseId);

            // Get the application associated with this license
            Map<String, Object> application = this.coreLicenseApplicationService.getApplicationByLicenseId(licenseId);

            if (application == null) {
                throw new EntityNotFoundException("No application found for license ID: " + licenseId);
            }

            log.info("Found application ID: {}", application.get("id"));
            return application;

        } catch (Exception e) {
            log.error("Error retrieving application by license ID {}: {}", licenseId, e.getMessage());
            throw new RuntimeException("Failed to retrieve application by license ID: " + e.getMessage());
        }
    }

    @GetMapping(path = "/email-status/{taskId}")
    public Map<String, Object> getEmailStatus(@PathVariable(name = "taskId") String taskId) {
        String status = emailQueueService.getEmailStatus(taskId);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("taskId", taskId);
        response.put("status", status);
        return response;
    }

    /**
     * Send custom email to applicant (Admin functionality)
     */
    @PostMapping(path = "/send-email")
    public Map<String, Object> sendCustomEmail(
            @RequestBody Map<String, Object> emailRequest,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== ADMIN SENDING CUSTOM EMAIL ===");
            log.info("Email request: {}", emailRequest);

            // Validate admin permissions
            SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
            if (currentUser == null) {
                throw new ForbiddenException("User not authenticated");
            }

            String userRole = currentUser.getSysUserGroup() != null ?
                    currentUser.getSysUserGroup().getName().toLowerCase() : "";

            if (!"admin".equals(userRole) && !"licensing manager".equals(userRole) &&
                    !"licensing_manager".equals(userRole) && !"accountant".equals(userRole) && !"license manager".equals(userRole) &&
                    !"license_manager".equals(userRole)) {
                throw new ForbiddenException("Insufficient permissions to send emails");
            }

            // Extract email data
            String to = (String) emailRequest.get("to");
            String subject = (String) emailRequest.get("subject");
            String message = (String) emailRequest.get("message");
            String applicationId = (String) emailRequest.get("applicationId");

            // Validate required fields
            if (to == null || to.trim().isEmpty()) {
                throw new RuntimeException("Recipient email address is required");
            }
            if (subject == null || subject.trim().isEmpty()) {
                throw new RuntimeException("Email subject is required");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new RuntimeException("Email message is required");
            }

            log.info("Sending email to: {} with subject: {}", to, subject);
            log.info("Sender: {} ({})", currentUser.getUsername(), userRole);
            log.info("Application ID: {}", applicationId);

            // Generate task ID for tracking
            String taskId = "admin-email-" + System.currentTimeMillis();

            // Send email asynchronously using EmailQueueService
            emailQueueService.sendEmailAsync(taskId, to.trim(), subject.trim(), message.trim());

            log.info("Email queued successfully with task ID: {}", taskId);

            // Prepare response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Email sent successfully");
            response.put("taskId", taskId);
            response.put("to", to.trim());
            response.put("subject", subject.trim());
            response.put("sentBy", currentUser.getUsername());
            response.put("sentAt", new java.util.Date());

            if (applicationId != null) {
                response.put("applicationId", applicationId);
            }

            return response;

        } catch (ForbiddenException e) {
            log.error("Permission denied for email sending: {}", e.getMessage());
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        } catch (Exception e) {
            log.error("Error sending custom email: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to send email: " + e.getMessage());
            return errorResponse;
        }
    }

    @GetMapping(path = "/{id}/details")
    public Map<String, Object> getApplicationDetails(
            @PathVariable(name = "id") String id,
            @RequestHeader(name = "Authorization") String token) {
        
        log.info("=== APPLICATION DETAILS REQUEST ===");
        log.info("Application ID: {}", id);
        
        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            log.error("User not authenticated");
            throw new ForbiddenException("User not authenticated");
        }
        
        log.info("Authenticated user: {}", applicant.getUsername());
        
        Object result = this.coreLicenseApplicationService.getApplicationDetails(id);
        if (result == null) {
            log.error("Application not found: {}", id);
            throw new EntityNotFoundException("Application not found");
        }
        
        log.info("Query result type: {}", result.getClass().getSimpleName());
        log.info("Raw result: {}", result);

        String applicationMetadata = null;
        String formSpecificData = null;
        Double licenseFee = null;
        String licenseFeeSetByUserId = null;
        java.sql.Timestamp licenseFeeSetDate = null;

        if (result instanceof Object[]) {
            Object[] details = (Object[]) result;
            log.info("Result is Object[] with length: {}", details.length);
            applicationMetadata = (details.length > 0 && details[0] != null) ? details[0].toString() : null;
            formSpecificData = (details.length > 1 && details[1] != null) ? details[1].toString() : null;

            // Extract license fee fields (indices 2, 3, 4)
            if (details.length > 2 && details[2] != null) {
                licenseFee = ((Number) details[2]).doubleValue();
            }
            if (details.length > 3 && details[3] != null) {
                licenseFeeSetByUserId = details[3].toString();
            }
            if (details.length > 4 && details[4] != null) {
                licenseFeeSetDate = (java.sql.Timestamp) details[4];
            }
        } else {
            log.warn("Result is not Object[], treating as single value");
            applicationMetadata = result.toString();
        }

        log.info("Final applicationMetadata: {}", applicationMetadata != null ? "Present" : "NULL");
        log.info("Final formSpecificData: {}", formSpecificData != null ? "Present" : "NULL");
        log.info("Final licenseFee: {}", licenseFee);
        log.info("Final licenseFeeSetByUserId: {}", licenseFeeSetByUserId);
        log.info("Final licenseFeeSetDate: {}", licenseFeeSetDate);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("applicationMetadata", applicationMetadata);
        response.put("formSpecificData", formSpecificData);
        response.put("licenseFee", licenseFee);
        response.put("licenseFeeSetByUserId", licenseFeeSetByUserId);
        response.put("licenseFeeSetDate", licenseFeeSetDate);
        
        log.info("=== APPLICATION DETAILS RESPONSE READY ===");
        return response;
    }

    @GetMapping(path = "/my-applications")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Map<String, Object>> getMyApplications(@RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        List<Object[]> applications = this.coreLicenseApplicationService
                .getMyApplicationsOptimized(applicant.getUsername());
                log.info("Fetched {} applications for user {}", applications.size(), applicant.getUsername());

        List<Map<String, Object>> response = applications.stream().map(row -> {
            Map<String, Object> appData = new java.util.HashMap<>();

            // Map Object[] to response: id, applicationType, dateSubmitted, dateCreated, status, licenseType, licenseTypeId,
            // appFeePaymentStatus, appFeeAmount, licenseFeePaymentStatus, licenseFeeAmount, applicationMetadata, applicationFees, licenseFees,
            // applicationPriority, emergencyReason, emergencyJustificationFile, emergencySubmittedDate
            appData.put("id", row[0]);
            appData.put("applicationType", row[1]);
            appData.put("dateSubmitted", row[2]);
            appData.put("applicationDate", row[3]);
            appData.put("status", row[4] != null ? row[4] : "UNKNOWN");
            appData.put("licenseType", row[5] != null ? row[5] : "Unknown");
            appData.put("licenseTypeId", row[6]);
            appData.put("applicationMetadata", row[11]);

            // Application fee payment status (for initial application submission)
            Map<String, Object> paymentStatus = new java.util.HashMap<>();
            String appFeeStatus = row[7] != null ? row[7].toString() : "PENDING";
            Double appFeeAmount = row[8] != null ? ((Number) row[8]).doubleValue() :
                                 (row[12] != null ? ((Number) row[12]).doubleValue() : 5000.0);

            paymentStatus.put("status", appFeeStatus);
            paymentStatus.put("amount", appFeeAmount);
            paymentStatus.put("message", appFeeStatus.equals("PENDING") ? "Application fee payment required" :
                                        "Application fee status: " + appFeeStatus);
            appData.put("paymentStatus", paymentStatus);
            appData.put("applicationFees", appFeeAmount);

            // Add emergency application fields (indices 14-17)
            appData.put("applicationPriority", row[14]);
            appData.put("emergencyReason", row[15]);
            appData.put("emergencyJustificationFile", row[16]);
            appData.put("emergencySubmittedDate", row[17]);

            // Add application-specific license fee fields (indices 18-20)
            Double applicationLicenseFee = null;
            if (row.length > 18 && row[18] != null) {
                applicationLicenseFee = ((Number) row[18]).doubleValue();
                appData.put("licenseFee", applicationLicenseFee);
            } else {
                appData.put("licenseFee", null);
            }
            if (row.length > 19) {
                appData.put("licenseFeeSetByUserId", row[19]);
            }
            if (row.length > 20) {
                appData.put("licenseFeeSetDate", row[20]);
            }

            // License fee status (for approved applications)
            Map<String, Object> licenseFeeStatus = new java.util.HashMap<>();
            String licenseFeeStatusValue = row[9] != null ? row[9].toString() : "PENDING";
            // Use application-specific license fee (row[18]) instead of license type fee (row[13])
            Double licenseFeeAmount = row[10] != null ? ((Number) row[10]).doubleValue() :
                                     (applicationLicenseFee != null ? applicationLicenseFee : 0.0);

            licenseFeeStatus.put("status", licenseFeeStatusValue);
            licenseFeeStatus.put("amount", licenseFeeAmount);
            licenseFeeStatus.put("message", licenseFeeStatusValue.equals("PENDING") ? "License fee payment required" :
                                           "License fee status: " + licenseFeeStatusValue);
            appData.put("licenseFeeStatus", licenseFeeStatus);

            // Add applicant info from token
            appData.put("applicantName", applicant.getFirstName() + " " + applicant.getLastName());
            appData.put("applicantEmail", applicant.getEmailAddress());

            return appData;
        }).collect(java.util.stream.Collectors.toList());

        return response;
    }

    @PostMapping(path = "/surface-water-permit")
    public Map<String, Object> createSurfaceWaterPermitApplication(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== SURFACE WATER APPLICATION CREATION ===");
            log.info("Request data: {}", request);
            log.info("=== DETAILED REQUEST ANALYSIS ===");
            log.info("Request size: {} fields", request.size());
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                log.info("Field: '{}' = '{}'", entry.getKey(), entry.getValue());
            }

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                log.error("User not authenticated");
                throw new ForbiddenException("User not authenticated");
            }

            log.info("Applicant user: {} (ID: {})", applicant.getUsername(), applicant.getId());
            log.info("Applicant name: {} {}", applicant.getFirstName(), applicant.getLastName());
            log.info("Applicant email: {}", applicant.getEmailAddress());

            CoreLicenseApplication application = new CoreLicenseApplication();
            application.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));
            application.setSysUserAccount(applicant);
            
            // Set owner_id and user_account_id to current user (applicant)
            application.setOwnerId(applicant.getId());
            application.setUserAccountId(applicant.getId());

            // Set required license type
            mw.nwra.ewaterpermit.model.CoreLicenseType licenseType = null;
            if (request.get("license_type_id") != null) {
                licenseType = coreLicenseTypeService.getCoreLicenseTypeById(request.get("license_type_id").toString());
            }
            if (licenseType == null) {
                throw new RuntimeException("License type not found with ID: " + request.get("license_type_id"));
            }
            application.setCoreLicenseType(licenseType);

            // Set required application status
            mw.nwra.ewaterpermit.model.CoreApplicationStatus status = null;
            if (request.get("application_status_id") != null) {
                status = coreApplicationStatusService.getCoreApplicationStatusById(request.get("application_status_id").toString());
            }
            if (status == null) {
                status = coreApplicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
            }
            if (status == null) {
                throw new RuntimeException("No application status found");
            }
            application.setCoreApplicationStatus(status);

            // Set application step if available
            mw.nwra.ewaterpermit.model.CoreApplicationStep step = coreApplicationStepService.getFirstStepByLicenseType(licenseType);
            if (step != null) {
                application.setCoreApplicationStep(step);
            }

            // ==> SET SOURCE LOCATION DETAILS FROM REQUEST
            log.info("=== SETTING SOURCE LOCATION DETAILS ===");

            // Handle Postman format (direct field names)
            if (request.get("sourceEasting") != null) {
                application.setSourceEasting(request.get("sourceEasting").toString());
                log.info("Source Easting: {}", request.get("sourceEasting"));
            }
            if (request.get("sourceNorthing") != null) {
                application.setSourceNorthing(request.get("sourceNorthing").toString());
                log.info("Source Northing: {}", request.get("sourceNorthing"));
            }
            if (request.get("sourceVillage") != null) {
                application.setSourceVillage(request.get("sourceVillage").toString());
                log.info("Source Village: {}", request.get("sourceVillage"));
            }
            if (request.get("sourceTa") != null) {
                application.setSourceTa(request.get("sourceTa").toString());
                log.info("Source TA: {}", request.get("sourceTa"));
            }
            if (request.get("sourceHectarage") != null) {
                application.setSourceHectarage(request.get("sourceHectarage").toString());
                log.info("Source Hectarage: {}", request.get("sourceHectarage"));
            }
            if (request.get("sourceOwnerFullname") != null) {
                application.setSourceOwnerFullname(request.get("sourceOwnerFullname").toString());
                log.info("Source Owner: {}", request.get("sourceOwnerFullname"));
            }
            if (request.get("sourcePlotNumber") != null) {
                application.setSourcePlotNumber(request.get("sourcePlotNumber").toString());
                log.info("Source Plot: {}", request.get("sourcePlotNumber"));
            }

            // Handle Frontend format (different field names)
            if (request.get("waterSourceCoordinates") != null) {
                String coordinates = request.get("waterSourceCoordinates").toString();
                String[] parts = coordinates.split(",");
                if (parts.length >= 2) {
                    application.setSourceEasting(parts[0].trim());
                    application.setSourceNorthing(parts[1].trim());
                    log.info("Frontend coordinates - Easting: {}, Northing: {}", parts[0].trim(), parts[1].trim());
                }
            }

            if (request.get("village") != null) {
                application.setSourceVillage(request.get("village").toString());
                log.info("Frontend Village: {}", request.get("village"));
            }

            if (request.get("traditionalAuthority") != null) {
                application.setSourceTa(request.get("traditionalAuthority").toString());
                log.info("Frontend TA: {}", request.get("traditionalAuthority"));
            }

            if (request.get("landArea") != null) {
                application.setSourceHectarage(request.get("landArea").toString());
                log.info("Frontend Land Area: {}", request.get("landArea"));
            }

            if (request.get("landownerWorksExist") != null) {
                application.setSourceOwnerFullname(request.get("landownerWorksExist").toString());
                log.info("Frontend Source Owner: {}", request.get("landownerWorksExist"));
            }

            if (request.get("leaseholdPlotNo") != null) {
                application.setSourcePlotNumber(request.get("leaseholdPlotNo").toString());
                log.info("Frontend Leasehold Plot: {}", request.get("leaseholdPlotNo"));
            } else if (request.get("freeholdPlotNo") != null) {
                application.setSourcePlotNumber(request.get("freeholdPlotNo").toString());
                log.info("Frontend Freehold Plot: {}", request.get("freeholdPlotNo"));
            }

            // ==> SET DESTINATION LOCATION DETAILS FROM REQUEST
            log.info("=== SETTING DESTINATION LOCATION DETAILS ===");

            // Handle Postman format (direct field names)
            if (request.get("destEasting") != null) {
                application.setDestEasting(request.get("destEasting").toString());
                log.info("Dest Easting: {}", request.get("destEasting"));
            }
            if (request.get("destNorthing") != null) {
                application.setDestNorthing(request.get("destNorthing").toString());
                log.info("Dest Northing: {}", request.get("destNorthing"));
            }
            if (request.get("destVillage") != null) {
                application.setDestVillage(request.get("destVillage").toString());
                log.info("Dest Village: {}", request.get("destVillage"));
            }
            if (request.get("destTa") != null) {
                application.setDestTa(request.get("destTa").toString());
                log.info("Dest TA: {}", request.get("destTa"));
            }
            if (request.get("destHectarage") != null) {
                application.setDestHectarage(request.get("destHectarage").toString());
                log.info("Dest Hectarage: {}", request.get("destHectarage"));
            }
            if (request.get("destOwnerFullname") != null) {
                application.setDestOwnerFullname(request.get("destOwnerFullname").toString());
                log.info("Dest Owner: {}", request.get("destOwnerFullname"));
            }
            if (request.get("destPlotNumber") != null) {
                application.setDestPlotNumber(request.get("destPlotNumber").toString());
                log.info("Dest Plot: {}", request.get("destPlotNumber"));
            }

            // Handle Frontend format - use same values as source for destination
            if (request.get("village") != null && request.get("destVillage") == null) {
                application.setDestVillage(request.get("village").toString());
                log.info("Frontend Dest Village (same as source): {}", request.get("village"));
            }

            if (request.get("traditionalAuthority") != null && request.get("destTa") == null) {
                application.setDestTa(request.get("traditionalAuthority").toString());
                log.info("Frontend Dest TA (same as source): {}", request.get("traditionalAuthority"));
            }

            if (request.get("landArea") != null && request.get("destHectarage") == null) {
                application.setDestHectarage(request.get("landArea").toString());
                log.info("Frontend Dest Hectarage (same as source): {}", request.get("landArea"));
            }

            if (request.get("landownerWaterUseExist") != null) {
                application.setDestOwnerFullname(request.get("landownerWaterUseExist").toString());
                log.info("Frontend Dest Owner: {}", request.get("landownerWaterUseExist"));
            } else if (request.get("landownerWorksExist") != null && request.get("destOwnerFullname") == null) {
                application.setDestOwnerFullname(request.get("landownerWorksExist").toString());
                log.info("Frontend Dest Owner (same as source): {}", request.get("landownerWorksExist"));
            }

            if (request.get("landWorksLocation") != null) {
                application.setDestEasting(request.get("landWorksLocation").toString());
                log.info("Frontend Land Works Location: {}", request.get("landWorksLocation"));
            }

            if (request.get("landWaterUseLocation") != null) {
                application.setDestNorthing(request.get("landWaterUseLocation").toString());
                log.info("Frontend Land Water Use Location: {}", request.get("landWaterUseLocation"));
            }

            // ==> SET PERMIT AND UTILITY DETAILS FROM REQUEST
            log.info("=== SETTING PERMIT AND UTILITY DETAILS ===");

            // Handle Postman format
            if (request.get("permitDuration") != null) {
                try {
                    String durationStr = request.get("permitDuration").toString().trim();
                    if (durationStr.matches("\\d+(\\.\\d+)?")) {
                        application.setPermitDuration(Double.valueOf(durationStr));
                        log.info("Permit Duration: {}", durationStr);
                    } else {
                        log.warn("Invalid permit duration format, setting to null: {}", request.get("permitDuration"));
                        application.setPermitDuration(null);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid permit duration format: {}", request.get("permitDuration"));
                    application.setPermitDuration(null);
                }
            }
            if (request.get("nearbyWaterUtilityBoard") != null) {
                application.setNearbyWaterUtilityBoard(request.get("nearbyWaterUtilityBoard").toString());
                log.info("Utility Board: {}", request.get("nearbyWaterUtilityBoard"));
            }
            if (request.get("altWaterSource") != null) {
                application.setAltWaterSource(request.get("altWaterSource").toString());
                log.info("Alt Water Source: {}", request.get("altWaterSource"));
            }
            if (request.get("altOtherWater") != null) {
                application.setAltOtherWater(request.get("altOtherWater").toString());
                log.info("Alt Other Water: {}", request.get("altOtherWater"));
            }
            if (request.get("existingBoreholeCount") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("existingBoreholeCount").toString()));
                    log.info("Existing Boreholes: {}", request.get("existingBoreholeCount"));
                } catch (NumberFormatException e) {
                    log.warn("Invalid borehole count format: {}", request.get("existingBoreholeCount"));
                }
            }
            if (request.get("boardMinutes") != null) {
                application.setBoardMinutes(request.get("boardMinutes").toString());
                log.info("Board Minutes: {}", request.get("boardMinutes") != null ? "Present" : "None");
            }
            if (request.get("boardApprovalDate") != null) {
                try {
                    String dateStr = request.get("boardApprovalDate").toString();
                    application.setBoardApprovalDate(java.sql.Timestamp.valueOf(dateStr));
                    log.info("Board Approval Date: {}", dateStr);
                } catch (Exception e) {
                    log.warn("Invalid board approval date format: {}", request.get("boardApprovalDate"));
                }
            }

            // Handle Frontend format
            if (request.get("waterSourceName") != null) {
                application.setAltWaterSource(request.get("waterSourceName").toString());
                log.info("Frontend Water Source Name: {}", request.get("waterSourceName"));
            }

            if (request.get("waterDiversionLocation") != null) {
                application.setAltOtherWater(request.get("waterDiversionLocation").toString());
                log.info("Frontend Water Diversion Location: {}", request.get("waterDiversionLocation"));
            }

            if (request.get("waterUptakeDistrict") != null) {
                application.setNearbyWaterUtilityBoard(request.get("waterUptakeDistrict").toString());
                log.info("Frontend Water Uptake District: {}", request.get("waterUptakeDistrict"));
            }

            // Set default for surface water applications
            if (request.get("existingBoreholeCount") == null) {
                application.setExistingBoreholeCount(0);
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // ==> POPULATE JSON FIELDS WITH ALL FORM DATA
            log.info("=== POPULATING JSON FIELDS ===");

            // Populate CLIENT_INFO JSON
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("name", request.get("name"));
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("address", request.get("address"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("clientDistrict", request.get("clientDistrict"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("clientTelephone", request.get("clientTelephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("clientEmail", request.get("clientEmail"));
            clientInfo.put("landownerWorksExist", request.get("landownerWorksExist"));
            clientInfo.put("landownerWaterUseExist", request.get("landownerWaterUseExist"));
            clientInfo.put("landownerAddressDifferent", request.get("landownerAddressDifferent"));

            try {
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Error serializing CLIENT_INFO: {}", e.getMessage());
                throw new RuntimeException("Failed to serialize client info: " + e.getMessage());
            }

            // Populate LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("waterSourceCoordinates", request.get("waterSourceCoordinates"));
            locationInfo.put("waterSourceName", request.get("waterSourceName"));
            locationInfo.put("waterDiversionLocation", request.get("waterDiversionLocation"));
            locationInfo.put("waterUptakeDistrict", request.get("waterUptakeDistrict"));
            locationInfo.put("village", request.get("village"));
            locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leaseholdPlotNo", request.get("leaseholdPlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            locationInfo.put("landWorksLocation", request.get("landWorksLocation"));
            locationInfo.put("landWaterUseLocation", request.get("landWaterUseLocation"));
            locationInfo.put("sourceEasting", request.get("sourceEasting"));
            locationInfo.put("sourceNorthing", request.get("sourceNorthing"));
            locationInfo.put("destEasting", request.get("destEasting"));
            locationInfo.put("destNorthing", request.get("destNorthing"));

            try {
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Error serializing LOCATION_INFO: {}", e.getMessage());
                throw new RuntimeException("Failed to serialize location info: " + e.getMessage());
            }

            // Populate APPLICATION_METADATA JSON
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "surface_water");
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("purposeType", request.get("purposeType"));
            applicationMetadata.put("anticipatedStartDate", request.get("anticipatedStartDate"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());

            try {
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Error serializing APPLICATION_METADATA: {}", e.getMessage());
                throw new RuntimeException("Failed to serialize application metadata: " + e.getMessage());
            }

            // Populate FORM_SPECIFIC_DATA JSON (Surface Water specific fields)
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterSourceType", request.get("waterSourceType"));
            formSpecificData.put("waterSourceDescription", request.get("waterSourceDescription"));
            formSpecificData.put("waterUses", request.get("waterUses"));
            formSpecificData.put("totalVolume", request.get("totalVolume"));
            formSpecificData.put("waterUseStartDate", request.get("waterUseStartDate"));
            formSpecificData.put("measuringMechanism", request.get("measuringMechanism"));
            formSpecificData.put("alternativeSources", request.get("alternativeSources"));
            formSpecificData.put("otherAlternativeSource", request.get("otherAlternativeSource"));
            formSpecificData.put("existingBoreholes", request.get("existingBoreholes"));
            formSpecificData.put("existingBoreholesCount", request.get("existingBoreholesCount"));
            formSpecificData.put("existingBoreholesDetails", request.get("existingBoreholesDetails"));
            formSpecificData.put("waterUtilityArea", request.get("waterUtilityArea"));
            formSpecificData.put("waterUtilityName", request.get("waterUtilityName"));
            formSpecificData.put("topographicMap", request.get("topographicMap"));
            formSpecificData.put("developmentPlan", request.get("developmentPlan"));
            
            // Add all other form fields to preserve complete form data
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) &&
                    !locationInfo.containsKey(entry.getKey()) &&
                    !applicationMetadata.containsKey(entry.getKey()) &&
                    !entry.getKey().equals("license_type_id") &&
                    !entry.getKey().equals("application_status_id")) {
                    formSpecificData.put(entry.getKey(), entry.getValue());
                }
            }

            try {
                application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Error serializing FORM_SPECIFIC_DATA: {}", e.getMessage());
                throw new RuntimeException("Failed to serialize form specific data: " + e.getMessage());
            }

            log.info("JSON fields populated successfully");
            log.info("Client Info size: {} fields", clientInfo.size());
            log.info("Location Info size: {} fields", locationInfo.size());
            log.info("Application Metadata size: {} fields", applicationMetadata.size());
            log.info("Form Specific Data size: {} fields", formSpecificData.size());

            log.info("=== SAVING APPLICATION TO DATABASE ===");
            log.info("License Type: {} (ID: {})", licenseType.getName(), licenseType.getId());
            log.info("Application Status: {} (ID: {})", status.getName(), status.getId());
            log.info("Application Step: {} (ID: {}) Sequence: {}", step != null ? step.getName() : "None", step != null ? step.getId() : "None", step != null ? step.getSequenceNumber() : "None");
            
            // Validate all required fields before saving
            log.info("=== PRE-SAVE VALIDATION ===");
            log.info("Owner ID: {}", application.getOwnerId());
            log.info("User Account ID: {}", application.getUserAccountId());
            log.info("License Type: {}", application.getCoreLicenseType() != null ? application.getCoreLicenseType().getId() : "NULL");
            log.info("Application Status: {}", application.getCoreApplicationStatus() != null ? application.getCoreApplicationStatus().getId() : "NULL");
            log.info("SysUserAccount: {}", application.getSysUserAccount() != null ? application.getSysUserAccount().getId() : "NULL");
            log.info("Date Created: {}", application.getDateCreated());
            
            if (application.getOwnerId() == null) {
                log.error("CRITICAL: Owner ID is null - this will cause save failure");
                throw new RuntimeException("Owner ID cannot be null");
            }
            if (application.getUserAccountId() == null) {
                log.error("CRITICAL: User Account ID is null - this will cause save failure");
                throw new RuntimeException("User Account ID cannot be null");
            }
            if (application.getCoreLicenseType() == null) {
                log.error("CRITICAL: License Type is null - this will cause save failure");
                throw new RuntimeException("License Type cannot be null");
            }
            if (application.getCoreApplicationStatus() == null) {
                log.error("CRITICAL: Application Status is null - this will cause save failure");
                throw new RuntimeException("Application Status cannot be null");
            }
            if (application.getSysUserAccount() == null) {
                log.error("CRITICAL: SysUserAccount is null - this will cause save failure");
                throw new RuntimeException("SysUserAccount cannot be null");
            }
            
            log.info("All required fields validated successfully - proceeding with save");
            
            try {
                log.info("=== CALLING SERVICE SAVE METHOD ===");
                application = this.coreLicenseApplicationService.addCoreLicenseApplication(application);
                log.info("=== APPLICATION SAVED SUCCESSFULLY ===");
                log.info("Saved Application ID: {}", application.getId());
            } catch (Exception saveException) {
                log.error("=== SAVE OPERATION FAILED ===");
                log.error("Save error: {}", saveException.getMessage());
                log.error("Save error class: {}", saveException.getClass().getSimpleName());
                if (saveException.getCause() != null) {
                    log.error("Save error cause: {}", saveException.getCause().getMessage());
                }
                throw saveException;
            }

            // Audit log with detailed logging
            log.info("=== STARTING AUDIT LOG ===");
            log.info("Audit parameters:");
            log.info("- Applicant: {} (ID: {})", applicant != null ? applicant.getUsername() : "NULL", applicant != null ? applicant.getId() : "NULL");
            log.info("- Old entity: null");
            log.info("- New entity: {} (ID: {})", application != null ? "CoreLicenseApplication" : "NULL", application != null ? application.getId() : "NULL");
            log.info("- Entity class: {}", CoreLicenseApplication.class.getSimpleName());
            log.info("- Action: {}", Action.CREATE.toString());
            
            try {
                auditor.audit(applicant, null, application, CoreLicenseApplication.class, Action.CREATE.toString());
                log.info("=== AUDIT LOG COMPLETED SUCCESSFULLY ===");
            } catch (Exception auditException) {
                log.error("=== AUDIT LOG FAILED ===");
                log.error("Audit error: {}", auditException.getMessage());
                log.error("Audit error class: {}", auditException.getClass().getSimpleName());
                if (auditException.getCause() != null) {
                    log.error("Audit error cause: {}", auditException.getCause().getMessage());
                }
                // Don't fail the entire operation if audit fails
                log.warn("Continuing despite audit failure...");
            }

            log.info("=== APPLICATION SAVED SUCCESSFULLY ===");
            log.info("Application ID: {}", application.getId());
            log.info("Saved to table: core_license_application");
            log.info("Application date: {}", application.getDateCreated());

            // Handle emergency application data AFTER saving (need application ID for file path)
            log.info("=== CHECKING APPLICATION PRIORITY ===");
            log.info("Request contains applicationPriority: {}", request.containsKey("applicationPriority"));

            boolean needsUpdate = false;

            if (request.get("applicationPriority") != null) {
                String priority = request.get("applicationPriority").toString();
                application.setApplicationPriority(priority);
                log.info("Application priority set to: {}", priority);
                needsUpdate = true;
            } else {
                // Default to NORMAL if not provided
                application.setApplicationPriority("NORMAL");
                log.info("No priority provided, defaulting to NORMAL");
                needsUpdate = true;
            }

            if ("EMERGENCY".equals(application.getApplicationPriority())) {
                log.info("=== PROCESSING EMERGENCY APPLICATION ===");

                if (request.get("emergencyReason") != null) {
                    application.setEmergencyReason(request.get("emergencyReason").toString());
                    log.info("Emergency reason: {}", application.getEmergencyReason());
                } else {
                    log.warn("WARNING: No emergency reason provided!");
                }

                // Save emergency document from base64
                if (request.get("emergencyJustificationDocument") != null) {
                    try {
                        String base64File = request.get("emergencyJustificationDocument").toString();
                        log.info("Emergency document data length: {}", base64File.length());
                        String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                        application.setEmergencyJustificationFile(savedFilePath);
                        log.info("Emergency document saved successfully: {}", savedFilePath);
                    } catch (IOException e) {
                        log.error("ERROR saving emergency document: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("WARNING: No emergency document provided!");
                }

                application.setEmergencySubmittedDate(new java.sql.Timestamp(System.currentTimeMillis()));
                log.info("Emergency submitted date: {}", application.getEmergencySubmittedDate());
            }

            // Update application with priority/emergency data
            if (needsUpdate) {
                log.info("=== UPDATING APPLICATION WITH PRIORITY DATA ===");
                application = this.coreLicenseApplicationService.editCoreLicenseApplication(application);
                log.info("Application updated successfully");
                log.info("Final priority: {}", application.getApplicationPriority());
                log.info("Final emergency file: {}", application.getEmergencyJustificationFile());
            }

            // Link water resource unit and water use data
            log.info("=== STARTING DATA LINKING PROCESS ===");
            applicationDataLinkingService.linkWaterResourceUnit(application, request);
            applicationDataLinkingService.linkWaterUse(application, request);
            log.info("=== DATA LINKING COMPLETED ===");

            String applicantName = request.get("clientName") != null ? request.get("clientName").toString() : applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("clientEmail") != null ? request.get("clientEmail").toString() : applicant.getEmailAddress();

            log.info("Final applicant name: {}", applicantName);
            log.info("Final applicant email: {}", applicantEmail);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Surface water permit application submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName());

            // Queue email sending with real application fees
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 5000.0;
            log.info("=== QUEUING EMAIL ===");
            log.info("Application fees: MWK {}", applicationFees);
            log.info("Email recipient: {} ({})", applicantName, applicantEmail);

            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, licenseType.getName(), applicationFees);

            log.info("Email task ID: {}", emailTaskId);
            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);
            
            // Save application submission notification
            try {
                UserNotification notification = new UserNotification();
                notification.setUserId(applicant.getId());
                notification.setTitle("Application Submitted - " + licenseType.getName());
                notification.setMessage(String.format("Your %s application (ID: %s) has been submitted successfully. You will receive updates as it progresses through the review process.", 
                        licenseType.getName(), application.getId()));
                notification.setType(UserNotification.NotificationType.SUCCESS);
                notification.setCategory(UserNotification.NotificationCategory.APPLICATION);
                notification.setPriority(UserNotification.NotificationPriority.HIGH);
                notification.setActionUrl("/applications/" + application.getId());
                notification.setActionLabel("View Application");
                notification.setReferenceId(application.getId());
                
                // Import NotificationService if not already imported
                // notificationService.createNotification(notification);
                log.info("Application submission notification would be saved for user: {}", applicant.getId());
            } catch (Exception notifEx) {
                log.error("Failed to save application submission notification: {}", notifEx.getMessage());
            }

            log.info("=== SURFACE WATER APPLICATION CREATION COMPLETE ===");
            log.info("Response: {}", response);

            return response;
        } catch (Exception e) {
            System.err.println("Error creating surface water permit application: " + e.getMessage());
            throw new RuntimeException("Failed to create application: " + e.getMessage());
        }
    }

    @GetMapping(path = "/effluent-discharge-permit/test")
    public Map<String, Object> testEffluentDischargeEndpoint() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Effluent Discharge Permit endpoint is working");
        response.put("timestamp", new java.util.Date());
        response.put("endpoint", "/v1/license-applications/effluent-discharge-permit");
        return response;
    }

    @PostMapping(value = "/effluent-discharge-permit", produces = "application/json", consumes = "application/json")
    public Map<String, Object> createEffluentDischargePermitApplication(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT CREATING EFFLUENT DISCHARGE APPLICATION ===");
            log.info("Request data: {}", request);
            log.info("=== DETAILED REQUEST ANALYSIS ===");
            log.info("Request size: {} fields", request.size());
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                log.info("Field: '{}' = '{}'", entry.getKey(), entry.getValue());
            }

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                log.error("User not authenticated");
                throw new ForbiddenException("User not authenticated");
            }

            log.info("Applicant user: {} (ID: {})", applicant.getUsername(), applicant.getId());
            log.info("Applicant name: {} {}", applicant.getFirstName(), applicant.getLastName());
            log.info("Applicant email: {}", applicant.getEmailAddress());

            CoreLicenseApplication application = new CoreLicenseApplication();
            application.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));
            application.setSysUserAccount(applicant);
            
            // Set owner_id and user_account_id to current user (applicant)
            application.setOwnerId(applicant.getId());
            application.setUserAccountId(applicant.getId());

            // Set required license type
            mw.nwra.ewaterpermit.model.CoreLicenseType licenseType = null;
            if (request.get("license_type_id") != null) {
                licenseType = coreLicenseTypeService.getCoreLicenseTypeById(request.get("license_type_id").toString());
            }
            if (licenseType == null) {
                licenseType = coreLicenseTypeService.getCoreLicenseTypeByName("Effluent Discharge Permit");
            }
            if (licenseType == null) {
                throw new RuntimeException("License type not found: Effluent Discharge Permit");
            }
            application.setCoreLicenseType(licenseType);

            // Set required application status
            mw.nwra.ewaterpermit.model.CoreApplicationStatus status = null;
            if (request.get("application_status_id") != null) {
                status = coreApplicationStatusService.getCoreApplicationStatusById(request.get("application_status_id").toString());
            }
            if (status == null) {
                status = coreApplicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
            }
            if (status == null) {
                throw new RuntimeException("No application status found");
            }
            application.setCoreApplicationStatus(status);

            // Set application step if available
            mw.nwra.ewaterpermit.model.CoreApplicationStep step = coreApplicationStepService.getFirstStepByLicenseType(licenseType);
            if (step != null) {
                application.setCoreApplicationStep(step);
            }

            // ==> SET DISCHARGE POINT DETAILS FROM REQUEST
            log.info("=== SETTING DISCHARGE POINT DETAILS ===");

            // Handle Postman format (direct field names)
            if (request.get("dischargePointEasting") != null) {
                application.setSourceEasting(request.get("dischargePointEasting").toString());
                log.info("Discharge Point Easting: {}", request.get("dischargePointEasting"));
            }
            if (request.get("dischargePointNorthing") != null) {
                application.setSourceNorthing(request.get("dischargePointNorthing").toString());
                log.info("Discharge Point Northing: {}", request.get("dischargePointNorthing"));
            }
            if (request.get("dischargePointVillage") != null) {
                application.setSourceVillage(request.get("dischargePointVillage").toString());
                log.info("Discharge Point Village: {}", request.get("dischargePointVillage"));
            }
            if (request.get("dischargePointTa") != null) {
                application.setSourceTa(request.get("dischargePointTa").toString());
                log.info("Discharge Point TA: {}", request.get("dischargePointTa"));
            }
            if (request.get("dischargePointHectarage") != null) {
                application.setSourceHectarage(request.get("dischargePointHectarage").toString());
                log.info("Discharge Point Hectarage: {}", request.get("dischargePointHectarage"));
            }
            if (request.get("dischargePointOwnerFullname") != null) {
                application.setSourceOwnerFullname(request.get("dischargePointOwnerFullname").toString());
                log.info("Discharge Point Owner: {}", request.get("dischargePointOwnerFullname"));
            }
            if (request.get("dischargePointPlotNumber") != null) {
                application.setSourcePlotNumber(request.get("dischargePointPlotNumber").toString());
                log.info("Discharge Point Plot: {}", request.get("dischargePointPlotNumber"));
            }

            // Handle Frontend format (different field names)
            if (request.get("dischargePointCoordinates") != null) {
                String coordinates = request.get("dischargePointCoordinates").toString();
                String[] parts = coordinates.split(",");
                if (parts.length >= 2) {
                    application.setSourceEasting(parts[0].trim());
                    application.setSourceNorthing(parts[1].trim());
                    log.info("Frontend discharge coordinates - Easting: {}, Northing: {}", parts[0].trim(), parts[1].trim());
                }
            }

            if (request.get("village") != null) {
                application.setSourceVillage(request.get("village").toString());
                log.info("Frontend Village: {}", request.get("village"));
            }

            if (request.get("traditionalAuthority") != null) {
                application.setSourceTa(request.get("traditionalAuthority").toString());
                log.info("Frontend TA: {}", request.get("traditionalAuthority"));
            }

            if (request.get("facilityArea") != null) {
                application.setSourceHectarage(request.get("facilityArea").toString());
                log.info("Frontend Facility Area: {}", request.get("facilityArea"));
            }

            if (request.get("facilityOwner") != null) {
                application.setSourceOwnerFullname(request.get("facilityOwner").toString());
                log.info("Frontend Facility Owner: {}", request.get("facilityOwner"));
            }

            if (request.get("facilityPlotNo") != null) {
                application.setSourcePlotNumber(request.get("facilityPlotNo").toString());
                log.info("Frontend Facility Plot: {}", request.get("facilityPlotNo"));
            }

            // ==> SET TREATMENT FACILITY DETAILS FROM REQUEST
            log.info("=== SETTING TREATMENT FACILITY DETAILS ===");

            // Handle Postman format (direct field names)
            if (request.get("treatmentFacilityEasting") != null) {
                application.setDestEasting(request.get("treatmentFacilityEasting").toString());
                log.info("Treatment Facility Easting: {}", request.get("treatmentFacilityEasting"));
            }
            if (request.get("treatmentFacilityNorthing") != null) {
                application.setDestNorthing(request.get("treatmentFacilityNorthing").toString());
                log.info("Treatment Facility Northing: {}", request.get("treatmentFacilityNorthing"));
            }
            if (request.get("treatmentFacilityVillage") != null) {
                application.setDestVillage(request.get("treatmentFacilityVillage").toString());
                log.info("Treatment Facility Village: {}", request.get("treatmentFacilityVillage"));
            }
            if (request.get("treatmentFacilityTa") != null) {
                application.setDestTa(request.get("treatmentFacilityTa").toString());
                log.info("Treatment Facility TA: {}", request.get("treatmentFacilityTa"));
            }
            if (request.get("treatmentFacilityHectarage") != null) {
                application.setDestHectarage(request.get("treatmentFacilityHectarage").toString());
                log.info("Treatment Facility Hectarage: {}", request.get("treatmentFacilityHectarage"));
            }
            if (request.get("treatmentFacilityOwnerFullname") != null) {
                application.setDestOwnerFullname(request.get("treatmentFacilityOwnerFullname").toString());
                log.info("Treatment Facility Owner: {}", request.get("treatmentFacilityOwnerFullname"));
            }
            if (request.get("treatmentFacilityPlotNumber") != null) {
                application.setDestPlotNumber(request.get("treatmentFacilityPlotNumber").toString());
                log.info("Treatment Facility Plot: {}", request.get("treatmentFacilityPlotNumber"));
            }

            // Handle Frontend format - use same values as discharge point for treatment facility if not specified
            if (request.get("treatmentFacilityCoordinates") != null) {
                String coordinates = request.get("treatmentFacilityCoordinates").toString();
                String[] parts = coordinates.split(",");
                if (parts.length >= 2) {
                    application.setDestEasting(parts[0].trim());
                    application.setDestNorthing(parts[1].trim());
                    log.info("Frontend treatment coordinates - Easting: {}, Northing: {}", parts[0].trim(), parts[1].trim());
                }
            } else if (request.get("village") != null && request.get("treatmentFacilityVillage") == null) {
                application.setDestVillage(request.get("village").toString());
                log.info("Frontend Treatment Village (same as discharge): {}", request.get("village"));
            }

            if (request.get("traditionalAuthority") != null && request.get("treatmentFacilityTa") == null) {
                application.setDestTa(request.get("traditionalAuthority").toString());
                log.info("Frontend Treatment TA (same as discharge): {}", request.get("traditionalAuthority"));
            }

            if (request.get("treatmentOwner") != null) {
                application.setDestOwnerFullname(request.get("treatmentOwner").toString());
                log.info("Frontend Treatment Owner: {}", request.get("treatmentOwner"));
            } else if (request.get("facilityOwner") != null && request.get("treatmentFacilityOwnerFullname") == null) {
                application.setDestOwnerFullname(request.get("facilityOwner").toString());
                log.info("Frontend Treatment Owner (same as discharge): {}", request.get("facilityOwner"));
            }

            // ==> SET PERMIT AND EFFLUENT DETAILS FROM REQUEST
            log.info("=== SETTING PERMIT AND EFFLUENT DETAILS ===");

            // Handle Postman format
            if (request.get("permitDuration") != null) {
                try {
                    application.setPermitDuration(Double.valueOf(request.get("permitDuration").toString()));
                    log.info("Permit Duration: {}", request.get("permitDuration"));
                } catch (NumberFormatException e) {
                    log.warn("Invalid permit duration format: {}", request.get("permitDuration"));
                }
            }
            if (request.get("nearbyWaterUtilityBoard") != null) {
                application.setNearbyWaterUtilityBoard(request.get("nearbyWaterUtilityBoard").toString());
                log.info("Utility Board: {}", request.get("nearbyWaterUtilityBoard"));
            }
            if (request.get("effluentType") != null) {
                application.setAltWaterSource(request.get("effluentType").toString());
                log.info("Effluent Type: {}", request.get("effluentType"));
            }
            if (request.get("treatmentMethod") != null) {
                application.setAltOtherWater(request.get("treatmentMethod").toString());
                log.info("Treatment Method: {}", request.get("treatmentMethod"));
            }
            if (request.get("dischargeFrequency") != null) {
                application.setBoardMinutes(request.get("dischargeFrequency").toString());
                log.info("Discharge Frequency: {}", request.get("dischargeFrequency"));
            }

            // Handle Frontend format
            if (request.get("effluentTypeName") != null) {
                application.setAltWaterSource(request.get("effluentTypeName").toString());
                log.info("Frontend Effluent Type Name: {}", request.get("effluentTypeName"));
            }

            if (request.get("treatmentMethodName") != null) {
                application.setAltOtherWater(request.get("treatmentMethodName").toString());
                log.info("Frontend Treatment Method Name: {}", request.get("treatmentMethodName"));
            }

            if (request.get("dischargeFrequencyType") != null) {
                application.setBoardMinutes(request.get("dischargeFrequencyType").toString());
                log.info("Frontend Discharge Frequency Type: {}", request.get("dischargeFrequencyType"));
            }

            if (request.get("effluentVolumeDistrict") != null) {
                application.setNearbyWaterUtilityBoard(request.get("effluentVolumeDistrict").toString());
                log.info("Frontend Effluent Volume District: {}", request.get("effluentVolumeDistrict"));
            }

            // Set default for effluent discharge applications
            if (request.get("existingBoreholeCount") == null) {
                application.setExistingBoreholeCount(0);
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            log.info("=== SAVING APPLICATION TO DATABASE ===");
            log.info("License Type: {} (ID: {})", licenseType.getName(), licenseType.getId());
            log.info("Application Status: {} (ID: {})", status.getName(), status.getId());
            log.info("Application Step: {} (ID: {}) Sequence: {}", step != null ? step.getName() : "None", step != null ? step.getId() : "None", step != null ? step.getSequenceNumber() : "None");

            application = this.coreLicenseApplicationService.addCoreLicenseApplication(application);

            // Audit log
            auditor.audit(applicant, null, application, CoreLicenseApplication.class, Action.CREATE.toString());

            log.info("=== APPLICATION SAVED SUCCESSFULLY ===");
            log.info("Application ID: {}", application.getId());
            log.info("Saved to table: core_license_application");
            log.info("Application date: {}", application.getDateCreated());

            // Handle emergency application data AFTER saving (need application ID for file path)
            log.info("=== CHECKING APPLICATION PRIORITY ===");
            log.info("Request contains applicationPriority: {}", request.containsKey("applicationPriority"));

            boolean needsUpdate = false;

            if (request.get("applicationPriority") != null) {
                String priority = request.get("applicationPriority").toString();
                application.setApplicationPriority(priority);
                log.info("Application priority set to: {}", priority);
                needsUpdate = true;
            } else {
                // Default to NORMAL if not provided
                application.setApplicationPriority("NORMAL");
                log.info("No priority provided, defaulting to NORMAL");
                needsUpdate = true;
            }

            if ("EMERGENCY".equals(application.getApplicationPriority())) {
                log.info("=== PROCESSING EMERGENCY APPLICATION ===");

                if (request.get("emergencyReason") != null) {
                    application.setEmergencyReason(request.get("emergencyReason").toString());
                    log.info("Emergency reason: {}", application.getEmergencyReason());
                } else {
                    log.warn("WARNING: No emergency reason provided!");
                }

                // Save emergency document from base64
                if (request.get("emergencyJustificationDocument") != null) {
                    try {
                        String base64File = request.get("emergencyJustificationDocument").toString();
                        log.info("Emergency document data length: {}", base64File.length());
                        String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                        application.setEmergencyJustificationFile(savedFilePath);
                        log.info("Emergency document saved successfully: {}", savedFilePath);
                    } catch (IOException e) {
                        log.error("ERROR saving emergency document: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("WARNING: No emergency document provided!");
                }

                application.setEmergencySubmittedDate(new java.sql.Timestamp(System.currentTimeMillis()));
                log.info("Emergency submitted date: {}", application.getEmergencySubmittedDate());
            }

            // Update application with priority/emergency data
            if (needsUpdate) {
                log.info("=== UPDATING APPLICATION WITH PRIORITY DATA ===");
                application = this.coreLicenseApplicationService.editCoreLicenseApplication(application);
                log.info("Application updated successfully");
                log.info("Final priority: {}", application.getApplicationPriority());
                log.info("Final emergency file: {}", application.getEmergencyJustificationFile());
            }

            String applicantName = request.get("clientName") != null ? request.get("clientName").toString() : applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("clientEmail") != null ? request.get("clientEmail").toString() : applicant.getEmailAddress();

            log.info("Final applicant name: {}", applicantName);
            log.info("Final applicant email: {}", applicantEmail);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Effluent discharge permit application submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName());

            // Queue email sending with real application fees
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 6000.0;
            log.info("=== QUEUING EMAIL ===");
            log.info("Application fees: MWK {}", applicationFees);
            log.info("Email recipient: {} ({})", applicantName, applicantEmail);

            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, licenseType.getName(), applicationFees);

            log.info("Email task ID: {}", emailTaskId);
            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);
            
            // Save application submission notification
            try {
                UserNotification notification = new UserNotification();
                notification.setUserId(applicant.getId());
                notification.setTitle("Application Submitted - " + licenseType.getName());
                notification.setMessage(String.format("Your %s application (ID: %s) has been submitted successfully. You will receive updates as it progresses through the review process.", 
                        licenseType.getName(), application.getId()));
                notification.setType(UserNotification.NotificationType.SUCCESS);
                notification.setCategory(UserNotification.NotificationCategory.APPLICATION);
                notification.setPriority(UserNotification.NotificationPriority.HIGH);
                notification.setActionUrl("/applications/" + application.getId());
                notification.setActionLabel("View Application");
                notification.setReferenceId(application.getId());
                
                // Import NotificationService if not already imported
                // notificationService.createNotification(notification);
                log.info("Application submission notification would be saved for user: {}", applicant.getId());
            } catch (Exception notifEx) {
                log.error("Failed to save application submission notification: {}", notifEx.getMessage());
            }

            log.info("=== EFFLUENT DISCHARGE APPLICATION CREATION COMPLETE ===");
            log.info("Response: {}", response);

            return response;
        } catch (Exception e) {
            System.err.println("Error creating effluent discharge permit application: " + e.getMessage());
            throw new RuntimeException("Failed to create application: " + e.getMessage());
        }
    }

    @PostMapping(path = "/easement-application")
    public Map<String, Object> createEasementApplication(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== EASEMENT APPLICATION CREATION ===");
            log.info("Request data: {}", request);

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                log.error("User not authenticated");
                throw new ForbiddenException("User not authenticated");
            }

            log.info("Applicant user: {} (ID: {})", applicant.getUsername(), applicant.getId());

            CoreLicenseApplication application = new CoreLicenseApplication();
            application.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));
            application.setSysUserAccount(applicant);
            application.setOwnerId(applicant.getId());
            application.setUserAccountId(applicant.getId());

            // Set license type
            mw.nwra.ewaterpermit.model.CoreLicenseType licenseType = null;
            if (request.get("license_type_id") != null) {
                licenseType = coreLicenseTypeService.getCoreLicenseTypeById(request.get("license_type_id").toString());
            }
            if (licenseType == null) {
                licenseType = coreLicenseTypeService.getCoreLicenseTypeByName("Easement Permit");
            }
            if (licenseType == null) {
                licenseType = coreLicenseTypeService.getCoreLicenseTypeByName("Easement");
            }
            if (licenseType == null) {
                throw new RuntimeException("Easement license type not found. Please create 'Easement Permit' license type.");
            }
            application.setCoreLicenseType(licenseType);

            // Set status
            mw.nwra.ewaterpermit.model.CoreApplicationStatus status = null;
            if (request.get("application_status_id") != null) {
                status = coreApplicationStatusService.getCoreApplicationStatusById(request.get("application_status_id").toString());
            }
            if (status == null) {
                status = coreApplicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
            }
            if (status == null) {
                throw new RuntimeException("No application status found");
            }
            application.setCoreApplicationStatus(status);

            // Set application step
            mw.nwra.ewaterpermit.model.CoreApplicationStep step = coreApplicationStepService.getFirstStepByLicenseType(licenseType);
            if (step != null) {
                application.setCoreApplicationStep(step);
            }

            // Set basic details
            if (request.get("clientName") != null) {
                application.setSourceOwnerFullname(request.get("clientName").toString());
            }
            if (request.get("easementLandLocation") != null) {
                application.setSourceVillage(request.get("easementLandLocation").toString());
            }
            if (request.get("easementLandArea") != null) {
                application.setSourceHectarage(request.get("easementLandArea").toString());
            }
            if (request.get("waterSourceCoordinates") != null) {
                String coords = request.get("waterSourceCoordinates").toString();
                String[] coordParts = coords.split(",");
                if (coordParts.length >= 2) {
                    application.setSourceEasting(coordParts[0].trim());
                    application.setSourceNorthing(coordParts[1].trim());
                }
            }

            // Populate JSON fields for proper display in admin view
            try {
                // CLIENT_INFO - Page 1: Client Information
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("clientName", request.get("clientName"));
                clientInfo.put("clientAddress", request.get("clientAddress"));
                clientInfo.put("clientDistrict", request.get("clientDistrict"));
                clientInfo.put("clientTelephone", request.get("clientTelephone"));
                clientInfo.put("clientMobile", request.get("clientMobile"));
                clientInfo.put("clientEmail", request.get("clientEmail"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                // LOCATION_INFO - Page 2: Land Location
                Map<String, Object> locationInfo = new java.util.HashMap<>();
                // Benefiting land
                locationInfo.put("benefitingLandOwnerName", request.get("benefitingLandOwnerName"));
                locationInfo.put("benefitingOwnerAddress", request.get("benefitingOwnerAddress"));
                locationInfo.put("benefitingLandDistrict", request.get("benefitingLandDistrict"));
                locationInfo.put("benefitingPropertyRegime", request.get("benefitingPropertyRegime"));
                locationInfo.put("benefitingPlotNo", request.get("benefitingPlotNo"));
                // Easement land
                locationInfo.put("easementLandLocation", request.get("easementLandLocation"));
                locationInfo.put("easementLandDistrict", request.get("easementLandDistrict"));
                locationInfo.put("easementLandArea", request.get("easementLandArea"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

                // APPLICATION_METADATA - General metadata
                Map<String, Object> metadata = new java.util.HashMap<>();
                metadata.put("applicationType", "easement");
                metadata.put("formType", "FORM_T");
                metadata.put("submissionTimestamp", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata));

                // FORM_SPECIFIC_DATA - Pages 3-7: Easement-specific fields
                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                // Water source (Page 3)
                formSpecificData.put("waterSourceType", request.get("waterSourceType"));
                formSpecificData.put("waterSourceOther", request.get("waterSourceOther"));
                formSpecificData.put("waterSourceName", request.get("waterSourceName"));
                formSpecificData.put("waterSourceCoordinates", request.get("waterSourceCoordinates"));
                formSpecificData.put("waterUptakeDistrict", request.get("waterUptakeDistrict"));
                formSpecificData.put("existingPermitDetails", request.get("existingPermitDetails"));
                formSpecificData.put("topographicMap", request.get("topographicMap"));
                // Easement details (Page 4)
                formSpecificData.put("easementReason", request.get("easementReason"));
                formSpecificData.put("easementForm", request.get("easementForm"));
                formSpecificData.put("easementWorks", request.get("easementWorks"));
                formSpecificData.put("easementDuration", request.get("easementDuration"));
                // Affected people (Page 5)
                formSpecificData.put("titleCertificateNumber", request.get("titleCertificateNumber"));
                formSpecificData.put("affectedPeopleNames", request.get("affectedPeopleNames"));
                formSpecificData.put("peopleRefused", request.get("peopleRefused"));
                formSpecificData.put("relevantCorrespondence", request.get("relevantCorrespondence"));
                // Accompanying info (Page 6)
                formSpecificData.put("documentsList", request.get("documentsList"));
                formSpecificData.put("supportingDocuments", request.get("supportingDocuments"));
                // Declaration (Page 7)
                formSpecificData.put("declarationAgree", request.get("declarationAgree"));
                formSpecificData.put("applicantSignature", request.get("applicantSignature"));
                formSpecificData.put("applicantFullNames", request.get("applicantFullNames"));
                formSpecificData.put("declarationDate", request.get("declarationDate"));
                // Emergency (Page 8)
                formSpecificData.put("applicationPriority", request.get("applicationPriority"));
                formSpecificData.put("emergencyReason", request.get("emergencyReason"));
                formSpecificData.put("emergencyJustificationDocument", request.get("emergencyJustificationDocument"));
                application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));

                log.info("JSON fields populated successfully");
            } catch (Exception e) {
                log.error("Error populating JSON fields: {}", e.getMessage(), e);
            }

            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // Set application priority
            if (request.get("applicationPriority") != null) {
                String priority = request.get("applicationPriority").toString();
                application.setApplicationPriority(priority);
                log.info("Application priority set to: {}", priority);
            } else {
                application.setApplicationPriority("NORMAL");
                log.info("No priority provided, defaulting to NORMAL");
            }

            // Save application
            application = this.coreLicenseApplicationService.addCoreLicenseApplication(application);

            // Audit log
            if (application != null && application.getId() != null) {
                auditor.audit(Action.CREATE, "EasementApplication", application.getId(), applicant, "Created easement application");
            }

            log.info("=== EASEMENT APPLICATION SAVED ===");
            log.info("Application ID: {}", application.getId());

            // Handle emergency application fields after application is saved
            if ("EMERGENCY".equals(application.getApplicationPriority())) {
                log.info("Processing EMERGENCY application...");

                if (request.get("emergencyReason") != null) {
                    String emergencyReason = request.get("emergencyReason").toString();
                    application.setEmergencyReason(emergencyReason);
                    log.info("Emergency reason set: {}", emergencyReason);
                } else {
                    log.warn("WARNING: No emergency reason provided!");
                }

                // Save emergency document from base64
                if (request.get("emergencyJustificationDocument") != null) {
                    try {
                        String base64File = request.get("emergencyJustificationDocument").toString();
                        String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                        application.setEmergencyJustificationFile(savedFilePath);
                        log.info("Emergency document saved successfully");
                    } catch (IOException e) {
                        log.error("ERROR saving emergency document: {}", e.getMessage());
                    }
                } else {
                    log.warn("WARNING: No emergency document provided!");
                }

                // Set emergency submission timestamp
                application.setEmergencySubmittedDate(new java.sql.Timestamp(System.currentTimeMillis()));

                // Update the application with emergency data
                application = this.coreLicenseApplicationService.editCoreLicenseApplication(application);
                log.info("Emergency data saved successfully");
            }

            // Notify officers at the assigned workflow step
            try {
                if (application.getCoreApplicationStep() != null) {
                    String stepName = application.getCoreApplicationStep().getName();
                    String officerRole = mapStepToOfficerRole(stepName);

                    if (officerRole != null) {
                        log.info("=== NOTIFYING OFFICERS ABOUT NEW EASEMENT APPLICATION ===");
                        log.info("Application ID: {}", application.getId());
                        log.info("Current Step: {}", stepName);
                        log.info("Officer Role: {}", officerRole);

                        officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                        log.info("✅ Officer notification completed successfully");
                    } else {
                        log.warn("No officer role mapped for step: {}", stepName);
                    }
                } else {
                    log.warn("Easement application has no workflow step assigned");
                }
            } catch (Exception notificationError) {
                // Log error but don't fail the application
                log.error("Failed to notify officers: {}", notificationError.getMessage(), notificationError);
            }

            // Prepare response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "success");
            response.put("message", "Easement application submitted successfully");
            response.put("applicantName", request.getOrDefault("clientName", applicant.getFirstName() + " " + applicant.getLastName()));
            response.put("applicantEmail", request.getOrDefault("clientEmail", applicant.getEmailAddress()));
            response.put("licenseType", licenseType.getName());

            return response;
        } catch (Exception e) {
            System.err.println("Error creating easement application: " + e.getMessage());
            throw new RuntimeException("Failed to create easement application: " + e.getMessage());
        }
    }

    @PostMapping(path = "/set-easement-details/{applicationId}")
    public Map<String, Object> setEasementDetails(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> easementDetailsRequest,
            @RequestHeader(name = "Authorization", required = false) String token) {
        
        try {
            log.info("Setting easement details for application: {}", applicationId);
            
            CoreLicenseApplication application = this.coreLicenseApplicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                throw new RuntimeException("Application not found with ID: " + applicationId);
            }
            
            // Set the easement fields
            if (easementDetailsRequest.get("burdenedLandDescription") != null) {
                application.setBurdenedLandDescription(easementDetailsRequest.get("burdenedLandDescription").toString());
            }
            
            if (easementDetailsRequest.get("benefittedLandDescription") != null) {
                application.setBenefittedLandDescription(easementDetailsRequest.get("benefittedLandDescription").toString());
            }
            
            if (easementDetailsRequest.get("permitConditions") != null) {
                application.setPermitConditions(easementDetailsRequest.get("permitConditions").toString());
            }
            
            if (easementDetailsRequest.get("natureOfBurden") != null) {
                application.setNatureOfBurden(easementDetailsRequest.get("natureOfBurden").toString());
            }
            
            // Save the updated application
            application = this.coreLicenseApplicationService.editCoreLicenseApplication(application);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Easement details saved successfully");
            response.put("applicationId", applicationId);
            
            log.info("Easement details saved successfully for application: {}", applicationId);
            return response;
            
        } catch (Exception e) {
            log.error("Error setting easement details: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to save easement details: " + e.getMessage());
            return errorResponse;
        }
    }

    @PostMapping(path = "")
    public Map<String, Object> createCoreLicenseApplication(
            @RequestBody Map<String, Object> coreLicenseApplicationRequest,
            @RequestHeader(name = "Authorization", required = false) String token) {

        String applicationId = "app-" + System.currentTimeMillis();
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", applicationId);
        response.put("status", "SUBMITTED");
        response.put("message", "Application submitted successfully");
        response.put("applicationDate", new java.util.Date());
        response.put("applicantName", coreLicenseApplicationRequest.get("clientName"));
        response.put("licenseType", "Surface Water Permit");

        return response;
    }

    @GetMapping(path = "/easement-details/{applicationId}")
    public Map<String, Object> getEasementDetails(
            @PathVariable String applicationId,
            @RequestHeader(name = "Authorization", required = false) String token) {
        
        try {
            log.info("Getting easement details for application: {}", applicationId);
            
            CoreLicenseApplication application = this.coreLicenseApplicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                throw new RuntimeException("Application not found with ID: " + applicationId);
            }
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("applicationId", applicationId);
            response.put("burdenedLandDescription", application.getBurdenedLandDescription());
            response.put("benefittedLandDescription", application.getBenefittedLandDescription());
            response.put("permitConditions", application.getPermitConditions());
            response.put("natureOfBurden", application.getNatureOfBurden());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting easement details: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get easement details: " + e.getMessage());
            return errorResponse;
        }
    }

//    @PutMapping(path = "/{id}")
//    public Map<String, Object> updateCoreLicenseApplication(@PathVariable(name = "id") String id,
//                                                            @RequestBody Map<String, Object> updateRequest) {
//        try {
//            log.info("Updating application {} with data: {}", id, updateRequest);
//
//            CoreLicenseApplication application = this.coreLicenseApplicationService.getCoreLicenseApplicationById(id);
//            if (application == null) {
//                throw new EntityNotFoundException("Application not found with ID: " + id);
//            }
//
//            // Handle specific field updates
//            for (Map.Entry<String, Object> entry : updateRequest.entrySet()) {
//                String key = entry.getKey();
//                Object value = entry.getValue();
//
//                switch (key) {
//                    case "application_status_id":
//                        if (value != null) {
//                            mw.nwra.ewaterpermit.model.CoreApplicationStatus status =
//                                    coreApplicationStatusService.getCoreApplicationStatusById(value.toString());
//                            if (status != null) {
//                                application.setCoreApplicationStatus(status);
//                                log.info("Updated application status to: {}", status.getName());
//                            }
//                        }
//                        break;
//                    case "application_step_id":
//                        if (value != null) {
//                            mw.nwra.ewaterpermit.model.CoreApplicationStep step =
//                                    coreApplicationStepService.getCoreApplicationStepById(value.toString());
//                            if (step != null) {
//                                application.setCoreApplicationStep(step);
//                                log.info("Updated application step to: {}", step.getName());
//                            }
//                        }
//                        break;
//                    case "license_type_id":
//                        if (value != null) {
//                            mw.nwra.ewaterpermit.model.CoreLicenseType licenseType =
//                                    coreLicenseTypeService.getCoreLicenseTypeById(value.toString());
//                            if (licenseType != null) {
//                                application.setCoreLicenseType(licenseType);
//                                log.info("Updated license type to: {}", licenseType.getName());
//                            }
//                        }
//                        break;
//                    case "formSpecificData":
//                        if (value != null) {
//                            try {
//                                String formDataJson = objectMapper.writeValueAsString(value);
//                                application.setFormSpecificData(formDataJson);
//                                log.info("Updated form specific data");
//                            } catch (Exception e) {
//                                log.error("Failed to serialize form specific data: {}", e.getMessage());
//                            }
//                        }
//                        break;
//                    case "clientName":
//                    case "applicantName":
//                        if (value != null) {
//                            application.setClientName(value.toString());
//                            log.info("Updated client name to: {}", value);
//                        }
//                        break;
//                    case "clientEmail":
//                    case "applicantEmail":
//                        if (value != null) {
//                            application.setClientEmail(value.toString());
//                            log.info("Updated client email to: {}", value);
//                        }
//                        break;
//                    case "clientTelephone":
//                        if (value != null) {
//                            application.setClientTelephone(value.toString());
//                            log.info("Updated client telephone to: {}", value);
//                        }
//                        break;
//                    case "clientInfo":
//                        if (value != null) {
//                            try {
//                                String clientInfoJson = objectMapper.writeValueAsString(value);
//                                application.setClientInfo(clientInfoJson);
//                                log.info("Updated client info");
//                            } catch (Exception e) {
//                                log.error("Failed to serialize client info: {}", e.getMessage());
//                            }
//                        }
//                        break;
//                    case "locationInfo":
//                        if (value != null) {
//                            try {
//                                String locationInfoJson = objectMapper.writeValueAsString(value);
//                                application.setLocationInfo(locationInfoJson);
//                                log.info("Updated location info");
//                            } catch (Exception e) {
//                                log.error("Failed to serialize location info: {}", e.getMessage());
//                            }
//                        }
//                        break;
//                    case "applicationMetadata":
//                        if (value != null) {
//                            try {
//                                String metadataJson = objectMapper.writeValueAsString(value);
//                                application.setApplicationMetadata(metadataJson);
//                                log.info("Updated application metadata");
//                            } catch (Exception e) {
//                                log.error("Failed to serialize application metadata: {}", e.getMessage());
//                            }
//                        }
//                        break;
//                }
//            }
//
//            // Save the updated application
//            this.coreLicenseApplicationService.editCoreLicenseApplication(application);
//
//            // Return updated data as DTO
//            Map<String, Object> response = new java.util.HashMap<>();
//            response.put("id", application.getId());
//            response.put("status", application.getCoreApplicationStatus() != null ? application.getCoreApplicationStatus().getName() : "UNKNOWN");
//            response.put("message", "Application updated successfully");
//
//            if (application.getCoreApplicationStep() != null) {
//                response.put("currentStepSequence", application.getCoreApplicationStep().getSequenceNumber());
//                response.put("currentStepName", application.getCoreApplicationStep().getName());
//            }
//
//            log.info("Successfully updated application: {}", id);
//            return response;
//
//        } catch (Exception e) {
//            log.error("Error updating application {}: {}", id, e.getMessage(), e);
//            throw new RuntimeException("Failed to update application: " + e.getMessage());
//        }
//    }

    @DeleteMapping(path = "/{id}")
    public void deleteCoreLicenseApplication(@PathVariable(name = "id") String id,
                                             @RequestHeader(name = "Authorization") String token) {
        CoreLicenseApplication application = this.coreLicenseApplicationService.getCoreLicenseApplicationById(id);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user != null && user.getSysUserGroup() != null && user.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
            this.coreLicenseApplicationService.deleteCoreLicenseApplication(id);

            // Audit log
            auditor.audit(user, application, null, CoreLicenseApplication.class, Action.DELETE.toString());
        } else {
            throw new EntityNotFoundException("Action denied");
        }
    }

    /**
     * Check application fee payment status
     */
    private Map<String, Object> checkApplicationFeeStatus(String applicationId, double amount) {
        Map<String, Object> paymentStatus = new java.util.HashMap<>();
        paymentStatus.put("amount", amount);

        try {
            CoreLicenseApplication app = coreLicenseApplicationService.getCoreLicenseApplicationById(applicationId);
            if (app != null && app.getCoreApplicationPayments() != null) {
                for (var payment : app.getCoreApplicationPayments()) {
                    if (payment.getCoreFeesType() != null && "Application fee".equals(payment.getCoreFeesType().getName())) {
                        paymentStatus.put("status", payment.getPaymentStatus());
                        paymentStatus.put("message", "Application fee status: " + payment.getPaymentStatus());
                        return paymentStatus;
                    }
                }
            }
            
            paymentStatus.put("status", "PENDING");
            paymentStatus.put("message", "Payment required");
        } catch (Exception e) {
            log.error("Error checking application fee status: {}", e.getMessage());
            paymentStatus.put("status", "PENDING");
            paymentStatus.put("message", "Payment required");
        }

        return paymentStatus;
    }

    /**
     * Check license fee payment status
     */
    private Map<String, Object> checkLicenseFeeStatus(String applicationId, double amount) {
        Map<String, Object> paymentStatus = new java.util.HashMap<>();
        paymentStatus.put("amount", amount);

        try {
            CoreLicenseApplication app = coreLicenseApplicationService.getCoreLicenseApplicationById(applicationId);
            if (app != null && app.getCoreApplicationPayments() != null) {
                for (var payment : app.getCoreApplicationPayments()) {
                    if (payment.getCoreFeesType() != null && "License fees".equals(payment.getCoreFeesType().getName())) {
                        paymentStatus.put("status", payment.getPaymentStatus());
                        paymentStatus.put("message", "License fee status: " + payment.getPaymentStatus());
                        return paymentStatus;
                    }
                }
            }
            
            paymentStatus.put("status", "PENDING");
            paymentStatus.put("message", "Payment required");
        } catch (Exception e) {
            log.error("Error checking license fee status: {}", e.getMessage());
            paymentStatus.put("status", "PENDING");
            paymentStatus.put("message", "Payment required");
        }

        return paymentStatus;
    }

    /**
     * Check payment status through document verification only (legacy method)
     */
    private Map<String, Object> checkPaymentReceiptStatus(String applicationId, double amount) {
        Map<String, Object> paymentStatus = new java.util.HashMap<>();
        paymentStatus.put("amount", amount);

        try {
            log.info("=== CHECKING PAYMENT STATUS THROUGH DOCUMENTS ===");
            log.info("Application ID: {}", applicationId);

            // Get "Payment Receipt" document category
            mw.nwra.ewaterpermit.model.CoreDocumentCategory receiptCategory = getPaymentReceiptCategory();

            if (receiptCategory != null) {
                mw.nwra.ewaterpermit.model.CoreLicenseApplication app =
                        coreLicenseApplicationService.getCoreLicenseApplicationById(applicationId);

                String documentStatus = null;
                if (app != null) {
                    List<mw.nwra.ewaterpermit.model.CoreApplicationDocument> appDocuments =
                            documentService.getCoreApplicationDocumentByApplication(app);

                    // Find payment receipt document and get its actual status
                    for (mw.nwra.ewaterpermit.model.CoreApplicationDocument doc : appDocuments) {
                        if (doc.getCoreDocumentCategory() != null &&
                                "Payment Receipt".equals(doc.getCoreDocumentCategory().getName())) {
                            documentStatus = doc.getStatus();
                            log.info("Found payment receipt document: {} with status: {}", doc.getId(), documentStatus);
                            break;
                        }
                    }
                }

                if (documentStatus != null) {
                    paymentStatus.put("status", documentStatus);
                    if ("PAID".equals(documentStatus)) {
                        paymentStatus.put("message", "Payment approved and paid");
                    } else if ("AWAITING_APPROVAL".equals(documentStatus)) {
                        paymentStatus.put("message", "Payment receipt uploaded, awaiting verification");
                    } else {
                        paymentStatus.put("message", "Payment status: " + documentStatus);
                    }
                } else {
                    paymentStatus.put("status", "PENDING");
                    paymentStatus.put("message", "Payment required");
                    log.info("No payment receipt document found");
                }
            } else {
                paymentStatus.put("status", "PENDING");
                paymentStatus.put("message", "Payment required");
            }

            log.info("Payment status result: {}", paymentStatus);
        } catch (Exception e) {
            log.error("Error checking payment receipt status: {}", e.getMessage());
            paymentStatus.put("status", "PENDING");
            paymentStatus.put("message", "Payment required");
        }

        return paymentStatus;
    }

    /**
     * Get or find Payment Receipt document category
     */
    private mw.nwra.ewaterpermit.model.CoreDocumentCategory getPaymentReceiptCategory() {
        try {
            List<mw.nwra.ewaterpermit.model.CoreDocumentCategory> categories =
                    documentCategoryService.getAllCoreDocumentCategorys(0, 100);

            for (mw.nwra.ewaterpermit.model.CoreDocumentCategory category : categories) {
                if ("Payment Receipt".equals(category.getName())) {
                    return category;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting payment receipt category: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get payment receipts for a specific application
     */
    @GetMapping(path = "/{id}/payment-receipts")
    public List<Map<String, Object>> getApplicationPaymentReceipts(
            @PathVariable(name = "id") String applicationId,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        List<Map<String, Object>> receipts = new java.util.ArrayList<>();

        try {
            // Get "Payment Receipt" document category
            mw.nwra.ewaterpermit.model.CoreDocumentCategory receiptCategory = getPaymentReceiptCategory();

            if (receiptCategory != null) {
                // Get all documents for this application with receipt category
                List<mw.nwra.ewaterpermit.model.CoreApplicationDocument> allDocuments =
                        documentService.getAllCoreApplicationDocuments(0, 1000);

                for (mw.nwra.ewaterpermit.model.CoreApplicationDocument doc : allDocuments) {
                    if (doc.getCoreLicenseApplication() != null &&
                            applicationId.equals(doc.getCoreLicenseApplication().getId()) &&
                            doc.getCoreDocumentCategory() != null &&
                            receiptCategory.getId().equals(doc.getCoreDocumentCategory().getId())) {

                        Map<String, Object> receiptInfo = new java.util.HashMap<>();
                        receiptInfo.put("documentId", doc.getId());
                        receiptInfo.put("documentUrl", doc.getDocumentUrl());
                        receiptInfo.put("uploadedAt", doc.getDateCreated());
                        receiptInfo.put("category", "Payment Receipt");
                        receiptInfo.put("status", doc.getStatus());

                        receipts.add(receiptInfo);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting payment receipts: " + e.getMessage());
        }

        return receipts;
    }

    /**
     * Get application documents for an application
     */
    @GetMapping("/application-documents/{applicationId}")
    public Map<String, Object> getApplicationDocuments(@PathVariable String applicationId) {
        try {
            log.info("=== LICENSING OFFICER VIEWING DOCUMENTS ===");
            log.info("Application ID: {}", applicationId);
            log.info("Endpoint: /license-applications/application-documents/{}", applicationId);

            CoreLicenseApplication application = coreLicenseApplicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                log.error("Application not found: {}", applicationId);
                return Map.of("error", "Application not found", "code", "404");
            }

            // Get all documents for this application
            List<mw.nwra.ewaterpermit.model.CoreApplicationDocument> documents = documentService.getCoreApplicationDocumentByApplication(application);
            List<Map<String, Object>> documentList = new java.util.ArrayList<>();

            for (mw.nwra.ewaterpermit.model.CoreApplicationDocument doc : documents) {
                Map<String, Object> docInfo = new java.util.HashMap<>();
                docInfo.put("id", doc.getId());
                docInfo.put("documentUrl", doc.getDocumentUrl());
                docInfo.put("status", doc.getStatus());
                docInfo.put("dateCreated", doc.getDateCreated());

                // Add category information
                if (doc.getCoreDocumentCategory() != null) {
                    docInfo.put("categoryName", doc.getCoreDocumentCategory().getName());
                    docInfo.put("categoryId", doc.getCoreDocumentCategory().getId());
                } else {
                    docInfo.put("categoryName", "Unknown");
                    docInfo.put("categoryId", null);
                }

                // Add download URL
                docInfo.put("downloadUrl", "/api/nwra-apis/ewaterpermit-ws/v1/workflow/download-document/" + doc.getId());

                documentList.add(docInfo);
            }

            log.info("=== DOCUMENTS RETRIEVED SUCCESSFULLY ===");
            log.info("Total documents found: {}", documentList.size());
            log.info("Documents from table: core_application_document");

            for (Map<String, Object> doc : documentList) {
                log.info("Document: {} - Category: {} - Status: {}",
                        doc.get("id"), doc.get("categoryName"), doc.get("status"));
            }

            return Map.of("documents", documentList);

        } catch (Exception e) {
            log.error("Error getting application documents: {}", e.getMessage(), e);
            return Map.of("error", "Failed to get application documents: " + e.getMessage(), "code", "500");
        }
    }

    /**
     * Get applications by status (not filtered by workflow step)
     */
    @GetMapping("/by-status/{status}")
    public List<Map<String, Object>> getApplicationsByStatus(
            @PathVariable String status,
            @RequestHeader(name = "Authorization", required = false) String token) {
        try {
            log.info("=== FETCHING APPLICATIONS BY STATUS ===");
            log.info("Requested status: {}", status);
            log.info("Endpoint: /license-applications/by-status/{}", status);

            List<CoreLicenseApplication> applications = this.coreLicenseApplicationService.getAllCoreLicenseApplications();
            log.info("Total applications in database: {}", applications.size());

            // Filter by application status only (ignore workflow step filtering)
            List<Map<String, Object>> filteredApps = applications.stream()
                    .filter(app -> app.getCoreApplicationStatus() != null &&
                            status.equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
                    .map(app -> {
                        CoreApplicationStep step = app.getCoreApplicationStep();
                        Map<String, Object> appData = new java.util.HashMap<>();
                        appData.put("id", app.getId());
                        appData.put("step", step == null ? null : step.getId());
                        appData.put("status", app.getCoreApplicationStatus().getName());
                        appData.put("applicationDate", app.getDateCreated());
                        // Handle user account data safely
                        if (app.getSysUserAccount() != null) {
                            String firstName = app.getSysUserAccount().getFirstName() != null ? app.getSysUserAccount().getFirstName() : "";
                            String lastName = app.getSysUserAccount().getLastName() != null ? app.getSysUserAccount().getLastName() : "";
                            String fullName = (firstName + " " + lastName).trim();
                            if (fullName.isEmpty()) {
                                fullName = app.getSysUserAccount().getUsername() != null ? app.getSysUserAccount().getUsername() : "Unknown";
                            }
                            appData.put("applicantName", fullName);
                            appData.put("applicantEmail", app.getSysUserAccount().getEmailAddress() != null ? app.getSysUserAccount().getEmailAddress() : "No email");
                        } else {
                            appData.put("applicantName", "Unknown Applicant");
                            appData.put("applicantEmail", "No email");
                        }
                        appData.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
                        double feeAmount = app.getCoreLicenseType() != null ? app.getCoreLicenseType().getApplicationFees() : 20000.0;
                        appData.put("applicationFees", feeAmount);

                        // Add current step info
                        if (step != null) {
                            appData.put("currentStepSequence", step.getSequenceNumber());
                            appData.put("currentStepName", step.getName());
                        }

                        // Add schedule date from assessment for CEO view
                        if ("AUTHORIZED_SCHEDULE".equals(status)) {
                            try {
                                CoreLicenseAssessment assessment = assessmentService.findByApplicationId(app.getId());
                                if (assessment != null && assessment.getRecommendedScheduleDate() != null) {
                                    appData.put("recommendedScheduleDate", assessment.getRecommendedScheduleDate());
                                } else {
                                    appData.put("recommendedScheduleDate", null);
                                }
                            } catch (Exception e) {
                                log.warn("Error fetching schedule date for app {}: {}", app.getId(), e.getMessage());
                                appData.put("recommendedScheduleDate", null);
                            }
                        }

                        return appData;
                    }).collect(java.util.stream.Collectors.toList());

            log.info("=== APPLICATIONS FILTERED BY STATUS ===");
            log.info("Applications matching status '{}': {}", status, filteredApps.size());
            log.info("Data retrieved from table: core_license_application");

            return filteredApps;
        } catch (Exception e) {
            log.error("Error fetching applications by status: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }


    @GetMapping("/by-status")
    public List<Map<String, Object>> getApplicationsByStatusv2(
            @RequestParam List<String> status,
            @RequestParam(required = false) Integer workflowStep,
            @RequestHeader(name = "Authorization", required = false) String token) {
        try {
            List<CoreLicenseApplication> applications = this.coreLicenseApplicationService.getAllCoreLicenseApplications();


            // Filter by application status and optionally by workflow step
            List<Map<String, Object>> filteredApps = applications.stream()
                    .filter(app -> app.getCoreApplicationStatus() != null &&
                            status.stream().anyMatch(s -> s.equalsIgnoreCase(app.getCoreApplicationStatus().getName()))
                            &&
                            (workflowStep == null || (app.getCoreApplicationStep() != null &&
                                    app.getCoreApplicationStep().getSequenceNumber() == workflowStep)
                            )
                    )
                    .map(app -> {
                        CoreApplicationStep step = app.getCoreApplicationStep();
                        Map<String, Object> appData = new java.util.HashMap<>();
                        appData.put("id", app.getId());
                        appData.put("step", step == null ? null : step.getId());
                        appData.put("status", app.getCoreApplicationStatus().getName());
                        appData.put("applicationDate", app.getDateCreated());
                        // Handle user account data safely
                        if (app.getSysUserAccount() != null) {
                            String firstName = app.getSysUserAccount().getFirstName() != null ? app.getSysUserAccount().getFirstName() : "";
                            String lastName = app.getSysUserAccount().getLastName() != null ? app.getSysUserAccount().getLastName() : "";
                            String fullName = (firstName + " " + lastName).trim();
                            if (fullName.isEmpty()) {
                                fullName = app.getSysUserAccount().getUsername() != null ? app.getSysUserAccount().getUsername() : "Unknown";
                            }
                            appData.put("applicantName", fullName);
                            appData.put("applicantEmail", app.getSysUserAccount().getEmailAddress() != null ? app.getSysUserAccount().getEmailAddress() : "No email");
                        } else {
                            appData.put("applicantName", "Unknown Applicant");
                            appData.put("applicantEmail", "No email");
                        }
                        appData.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
                        double feeAmount = app.getCoreLicenseType() != null ? app.getCoreLicenseType().getApplicationFees() : 20000.0;
                        appData.put("applicationFees", feeAmount);

                        // Add current step info
                        if (step != null) {
                            appData.put("currentStepSequence", step.getSequenceNumber());
                            appData.put("currentStepName", step.getName());
                        }

                        // Add schedule date from assessment for CEO view
                        if (status.contains("AUTHORIZED_SCHEDULE")) {
                            try {
                                CoreLicenseAssessment assessment = assessmentService.findByApplicationId(app.getId());
                                if (assessment != null && assessment.getRecommendedScheduleDate() != null) {
                                    appData.put("recommendedScheduleDate", assessment.getRecommendedScheduleDate());
                                } else {
                                    appData.put("recommendedScheduleDate", null);
                                }
                            } catch (Exception e) {
                                log.warn("Error fetching schedule date for app {}: {}", app.getId(), e.getMessage());
                                appData.put("recommendedScheduleDate", null);
                            }
                        }

                        return appData;
                    }).collect(java.util.stream.Collectors.toList());

            log.info("=== APPLICATIONS FILTERED BY STATUS ===");
            log.info("Applications matching statuses {}: {}", status, filteredApps.size());
            log.info("Data retrieved from table: core_license_application");

            return filteredApps;
        } catch (Exception e) {
            log.error("Error fetching applications by status: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Determine if application is visible to user based on role and workflow sequence
     */
    private boolean isApplicationVisibleToUser(CoreLicenseApplication app, String userRole) {
        if (app == null) {
            log.warn("Application is null - filtering out");
            return false;
        }
        
        log.info("App ID: {}, Step object: {}", app.getId(), app.getCoreApplicationStep());
        
        if (app.getCoreApplicationStep() == null) {
            log.warn("Application {} has no step assigned", app.getId());
            return userRole.equals("admin");
        }

        int sequenceNumber = app.getCoreApplicationStep().getSequenceNumber();
        log.info("=== CHECKING APPLICATION VISIBILITY ===");
        log.info("Application ID: {}", app.getId());
        log.info("User Role: '{}'", userRole);
        log.info("Step Sequence: {}", sequenceNumber);
        log.info("Step Name: {}", app.getCoreApplicationStep().getName());

        boolean isVisible = false;
        String normalizedRole = userRole.toLowerCase().trim();

        isVisible = switch (normalizedRole) {
            case "accountant" ->
                    (sequenceNumber == 0 || sequenceNumber == 5) &&
                            app.getCoreApplicationPayments().stream()
                                    .anyMatch(payment -> payment.getPaymentStatus().equalsIgnoreCase("AWAITING_APPROVAL"));
            case "licensing officer", "licensing_officer", "license officer", "license_officer" -> sequenceNumber == 1;
            case "licensing manager", "licensing_manager", "license manager", "license_manager" -> sequenceNumber == 2;
            case "drs" -> sequenceNumber == 3;
            case "ceo" -> sequenceNumber == 4;
            case "admin" -> true; // Admin can see all applications
            default -> {
                log.warn("Unknown role '{}' - no applications will be visible", userRole);
                yield false;
            }
        };

        log.info("Application visible to '{}': {}", userRole, isVisible);
        return isVisible;
    }

    /**
     * Validate license reference number for transfer (Frontend validation endpoint)
     */
    @GetMapping(path = "/validate-license/{licenseNumber}")
    public ResponseEntity<Map<String, Object>> validateLicenseForTransfer(
            @PathVariable(name = "licenseNumber") String licenseNumber,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== VALIDATING LICENSE FOR FRONTEND ===");
            log.info("License number: {}", licenseNumber);

            // Authenticate user
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            if (user == null) {
                throw new ForbiddenException("User not authenticated");
            }

            // Validate license exists
            List<mw.nwra.ewaterpermit.model.CoreLicense> licenses = coreLicenseService.getCoreLicensesByLicenseNumber(licenseNumber.trim());
            if (licenses == null || licenses.isEmpty()) {
                log.error("License not found: {}", licenseNumber);
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "License not found with reference number: " + licenseNumber);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Get the most recent license (first in the ordered list)
            mw.nwra.ewaterpermit.model.CoreLicense license = licenses.get(0);
            if (licenses.size() > 1) {
                log.warn("Multiple licenses found with number {}. Using the most recent one (ID: {})", licenseNumber, license.getId());
            }

            // Check if license is expired
            if (license.getExpirationDate() != null) {
                java.util.Date today = new java.util.Date();
                if (license.getExpirationDate().before(today)) {
                    log.error("License {} is expired", licenseNumber);
                    Map<String, Object> errorResponse = new java.util.HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "License " + licenseNumber + " has expired and cannot be transferred");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Check for existing transfer applications
            List<CoreLicenseApplication> allApplications = coreLicenseApplicationService.getAllCoreLicenseApplications();
            for (CoreLicenseApplication app : allApplications) {
                if ("TRANSFER".equals(app.getApplicationType()) && 
                    license.getId().equals(app.getOriginalLicenseId()) &&
                    app.getCoreApplicationStatus() != null &&
                    !"REJECTED".equals(app.getCoreApplicationStatus().getName()) &&
                    !"CANCELLED".equals(app.getCoreApplicationStatus().getName())) {
                    log.error("License {} already has pending transfer", licenseNumber);
                    Map<String, Object> errorResponse = new java.util.HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "License " + licenseNumber + " already has a pending transfer application");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // License is valid for transfer
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "License is valid for transfer");
            response.put("licenseId", license.getId());
            response.put("licenseNumber", license.getLicenseNumber());
            response.put("dateIssued", license.getDateIssued());
            response.put("expirationDate", license.getExpirationDate());
            
            if (license.getCoreLicenseApplication() != null && 
                license.getCoreLicenseApplication().getCoreLicenseType() != null) {
                response.put("licenseType", license.getCoreLicenseApplication().getCoreLicenseType().getName());
            } else {
                response.put("licenseType", "Unknown");
            }

            log.info("License validation successful for: {}", licenseNumber);
            return ResponseEntity.ok(response);

        } catch (ForbiddenException e) {
            log.error("Authentication error: {}", e.getMessage());
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Authentication required");
            return ResponseEntity.status(401).body(errorResponse);
        } catch (Exception e) {
            log.error("Error validating license: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Unable to validate license: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Transfer license ownership endpoint
     * Validates license reference number, replicates original application, uses form data from change ownership form
     */
    @PostMapping(path = "/transfer")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> transferLicense(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== PROCESSING LICENSE TRANSFER ===");
            log.info("Request data: {}", request);

            // Authenticate user
            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                throw new ForbiddenException("User not authenticated");
            }

            log.info("Transfer requested by: {} (ID: {})", applicant.getUsername(), applicant.getId());

            // Validate license reference number
            String licenceReferenceNo = (String) request.get("licenceReferenceNo");
            if (licenceReferenceNo == null || licenceReferenceNo.trim().isEmpty()) {
                throw new RuntimeException("License reference number is required");
            }
            
            // Check if license exists in core_license table
            List<mw.nwra.ewaterpermit.model.CoreLicense> licenses = coreLicenseService.getCoreLicensesByLicenseNumber(licenceReferenceNo.trim());
            if (licenses == null || licenses.isEmpty()) {
                log.error("License not found with reference number: {}", licenceReferenceNo);
                throw new RuntimeException("License not found with reference number: " + licenceReferenceNo + ". Please verify the license reference number is correct.");
            }
            
            // Get the most recent license (first in the ordered list)
            mw.nwra.ewaterpermit.model.CoreLicense originalLicense = licenses.get(0);
            if (licenses.size() > 1) {
                log.warn("Multiple licenses found with number {}. Using the most recent one (ID: {})", licenceReferenceNo, originalLicense.getId());
            }

            log.info("License found: ID={}", originalLicense.getId());
            
            // Validate license is active and not expired
            if (originalLicense.getExpirationDate() != null) {
                java.util.Date today = new java.util.Date();
                if (originalLicense.getExpirationDate().before(today)) {
                    log.error("License {} is expired (expiration date: {})", licenceReferenceNo, originalLicense.getExpirationDate());
                    throw new RuntimeException("License " + licenceReferenceNo + " has expired and cannot be transferred. Expiration date: " + originalLicense.getExpirationDate());
                }
            }

            log.info("Found original license: ID={}, Application ID={}", 
                    originalLicense.getId(), 
                    originalLicense.getCoreLicenseApplication() != null ? originalLicense.getCoreLicenseApplication().getId() : "None");

            // Get the original license application
            CoreLicenseApplication originalApplication = originalLicense.getCoreLicenseApplication();
            if (originalApplication == null) {
                throw new RuntimeException("Original license application not found for license: " + licenceReferenceNo);
            }

            // Check if license has already been transferred (prevent duplicate transfers) - OPTIMIZED
            if (coreLicenseApplicationService.hasPendingOrApprovedTransfer(originalLicense.getId())) {
                log.error("License {} already has a pending/approved transfer application", licenceReferenceNo);
                throw new RuntimeException("License " + licenceReferenceNo + " already has a pending or approved transfer application. Only one transfer per license is allowed.");
            }

            log.info("License validation passed - proceeding with transfer");
            log.info("Original application found: {}", originalApplication.getId());

            // Create new transfer application by replicating original
            CoreLicenseApplication transferApplication = new CoreLicenseApplication();
            transferApplication.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));
            transferApplication.setSysUserAccount(applicant);
            // Don't set ownerId here - will be set later with proper logic

            // Set application type to TRANSFER
            transferApplication.setApplicationType("TRANSFER");
            transferApplication.setOriginalLicenseId(originalLicense.getId());

            // Determine the appropriate transfer license type based on original license category
            String originalLicenseTypeName = originalApplication.getCoreLicenseType().getName();
            WaterCategory category = getWaterCategory(originalLicenseTypeName);
            
            log.info("Original license type: {}", originalLicenseTypeName);
            log.info("Determined water category: {}", category);
            
            // Try to find specific transfer license type
            mw.nwra.ewaterpermit.model.CoreLicenseType transferLicenseType = findTransferLicenseType(category);
            
            if (transferLicenseType != null) {
                log.info("Using specific transfer license type: {}", transferLicenseType.getName());
                transferApplication.setCoreLicenseType(transferLicenseType);
            } else {
                log.warn("No specific transfer license type found for category {}, using original license type", category);
                transferApplication.setCoreLicenseType(originalApplication.getCoreLicenseType());
            }

            // Set default status to SUBMITTED
            mw.nwra.ewaterpermit.model.CoreApplicationStatus status = coreApplicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
            if (status == null) {
                throw new RuntimeException("SUBMITTED status not found");
            }
            transferApplication.setCoreApplicationStatus(status);

            // Set workflow step to first step (sequence 0 - accountant/payment verification)
            // Use the transfer license type for workflow determination
            mw.nwra.ewaterpermit.model.CoreLicenseType licenseTypeForWorkflow = transferApplication.getCoreLicenseType();
            mw.nwra.ewaterpermit.model.CoreApplicationStep firstStep = coreApplicationStepService.getFirstStepByLicenseType(licenseTypeForWorkflow);
            if (firstStep != null) {
                transferApplication.setCoreApplicationStep(firstStep);
                log.info("Set transfer application to step: {} (sequence: {}) using license type: {}",
                        firstStep.getName(),
                        firstStep.getSequenceNumber(),
                        licenseTypeForWorkflow.getName());
            }

            // Replicate original application data
            transferApplication.setSourceEasting(originalApplication.getSourceEasting());
            transferApplication.setSourceNorthing(originalApplication.getSourceNorthing());
            transferApplication.setSourceVillage(originalApplication.getSourceVillage());
            transferApplication.setSourceTa(originalApplication.getSourceTa());
            transferApplication.setSourceHectarage(originalApplication.getSourceHectarage());
            transferApplication.setSourceOwnerFullname(originalApplication.getSourceOwnerFullname());
            transferApplication.setSourcePlotNumber(originalApplication.getSourcePlotNumber());

            transferApplication.setDestEasting(originalApplication.getDestEasting());
            transferApplication.setDestNorthing(originalApplication.getDestNorthing());
            transferApplication.setDestVillage(originalApplication.getDestVillage());
            transferApplication.setDestTa(originalApplication.getDestTa());
            transferApplication.setDestHectarage(originalApplication.getDestHectarage());
            transferApplication.setDestOwnerFullname(originalApplication.getDestOwnerFullname());
            transferApplication.setDestPlotNumber(originalApplication.getDestPlotNumber());

            transferApplication.setPermitDuration(originalApplication.getPermitDuration());
            transferApplication.setNearbyWaterUtilityBoard(originalApplication.getNearbyWaterUtilityBoard());
            transferApplication.setAltWaterSource(originalApplication.getAltWaterSource());
            transferApplication.setAltOtherWater(originalApplication.getAltOtherWater());
            transferApplication.setExistingBoreholeCount(originalApplication.getExistingBoreholeCount());
            transferApplication.setBoardMinutes(originalApplication.getBoardMinutes());
            transferApplication.setBoardApprovalDate(originalApplication.getBoardApprovalDate());

            // Set water source and land regimes from original
            transferApplication.setCoreWaterSource(originalApplication.getCoreWaterSource());
            transferApplication.setSourceLandRegime(originalApplication.getSourceLandRegime());
            transferApplication.setDestLandRegime(originalApplication.getDestLandRegime());
            transferApplication.setSourceWru(originalApplication.getSourceWru());
            transferApplication.setDestWru(originalApplication.getDestWru());

            // Replicate location_info JSON data from original application
            transferApplication.setLocationInfo(originalApplication.getLocationInfo());

            // Use client info and metadata from change ownership form
            String clientInfo = buildClientInfoFromForm(request);
            String applicationMetadata = buildApplicationMetadataFromForm(request);
            String formSpecificData = buildFormSpecificDataFromForm(request);

            transferApplication.setClientInfo(clientInfo);
            transferApplication.setApplicationMetadata(applicationMetadata);
            transferApplication.setFormSpecificData(formSpecificData);

            // Set transfer user ID - determine target user from request or use current applicant
            log.info("Setting up ownership transfer:");
            log.info("- Original owner ID: {}", originalApplication.getOwnerId());
            
            String targetUserId = null;
            

            // Check if transferToEmail is provided and look up user ID
            if (request.get("recipientEmail") != null) {
                String transferToEmail = request.get("recipientEmail").toString();
                log.info("- Looking up user by email: {}", transferToEmail);
                
                SysUserAccount targetUser = sysUserAccountService.getSysUserAccountByEmailAddress(transferToEmail);
                if (targetUser != null) {
                    targetUserId = targetUser.getId();
                    log.info("- Found user ID for email {}: {}", transferToEmail, targetUserId);
                } else {
                    log.error("- No user found with email: {}", transferToEmail);
                    throw new RuntimeException("No user found with email address: " + transferToEmail + ". The transfer recipient must have an account in the system.");
                }
            }
            // Default to current applicant (existing behavior)
            else {
                String jsonString = objectMapper.writeValueAsString(request);
                log.info("- Using current applicant as transfer target: {} {}", targetUserId,jsonString);
                throw new RuntimeException("No target user found");

            }
            
            log.info("- Transfer target user ID: {}", targetUserId);
            
            // Set the transfer target user ID
            transferApplication.setTransferToUserId(targetUserId);
            
            // Keep the original license owner as the application owner initially
            // This will be changed to the new owner when the transfer is approved
            transferApplication.setOwnerId(originalApplication.getOwnerId());
            transferApplication.setUserAccountId(originalApplication.getOwnerId());
            
            log.info("Transfer application setup: ownership will transfer from {} to {} upon approval", 
                    originalApplication.getOwnerId(), targetUserId);


            // Save the transfer application
            transferApplication = coreLicenseApplicationService.addCoreLicenseApplication(transferApplication);

            // Audit log
            auditor.audit(applicant, null, transferApplication, CoreLicenseApplication.class, Action.CREATE.toString());

            log.info("Transfer application created successfully: ID={}", transferApplication.getId());

            // Duplicate assessment from original application
            try {
                log.info("=== DUPLICATING ASSESSMENT ===");
                log.info("Looking for assessment with license_application_id: {}", originalApplication.getId());
                
                // Find assessment by original application ID using the correct service method
                CoreLicenseAssessment originalAssessment = assessmentService.findByApplicationId(originalApplication.getId());
                
                if (originalAssessment != null) {
                    log.info("Found original assessment: ID={}", originalAssessment.getId());
                    
                    // Create new assessment by duplicating original
                    CoreLicenseAssessment newAssessment = new CoreLicenseAssessment();
                    
                    // Copy all assessment data
                    newAssessment.setLicenseApplicationId(transferApplication.getId()); // Link to transfer application
                    newAssessment.setAssessmentStatus(originalAssessment.getAssessmentStatus());
                    newAssessment.setAssessmentFilesUpload(originalAssessment.getAssessmentFilesUpload());
                    newAssessment.setCalculatedAnnualRental(originalAssessment.getCalculatedAnnualRental());
                    newAssessment.setRentalQuantity(originalAssessment.getRentalQuantity());
                    newAssessment.setRentalRate(originalAssessment.getRentalRate());
                    newAssessment.setRecommendedScheduleDate(originalAssessment.getRecommendedScheduleDate());
                    newAssessment.setAssessmentNotes(originalAssessment.getAssessmentNotes());
                    newAssessment.setLicenseOfficerId(originalAssessment.getLicenseOfficerId());
                    newAssessment.setLicenseManagerId(originalAssessment.getLicenseManagerId());
                    newAssessment.setSeniorLicenseOfficerId(originalAssessment.getSeniorLicenseOfficerId());
                    newAssessment.setDrsId(originalAssessment.getDrsId());
                    newAssessment.setAccountantId(originalAssessment.getAccountantId());
                    
                    // Save the duplicated assessment
                    newAssessment = assessmentService.save(newAssessment);
                    log.info("Assessment duplicated successfully: Original ID={}, New ID={}", originalAssessment.getId(), newAssessment.getId());
                } else {
                    log.warn("No assessment found for original application ID: {}", originalApplication.getId());
                }
            } catch (Exception assessmentError) {
                log.error("Failed to duplicate assessment: {}", assessmentError.getMessage(), assessmentError);
                // Don't fail the transfer if assessment duplication fails
            }

            // Send email notification to applicant
            String applicantName = applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = applicant.getEmailAddress();
            String licenseTypeName = transferApplication.getCoreLicenseType().getName();

            // Get application fees from license type (defaults to 0 if not set)
            double applicationFees = transferApplication.getCoreLicenseType().getApplicationFees() > 0
                    ? transferApplication.getCoreLicenseType().getApplicationFees()
                    : 0.0;

            log.info("Sending invoice email to: {} ({})", applicantName, applicantEmail);
            log.info("License type: {}, Application fees: MWK {}", licenseTypeName, applicationFees);

            try {
                String emailTaskId = emailQueueService.queueInvoiceEmail(
                        transferApplication.getId(),
                        applicantName,
                        applicantEmail,
                        licenseTypeName,
                        applicationFees
                );
                log.info("Invoice email queued successfully with task ID: {}", emailTaskId);
            } catch (Exception emailError) {
                // Log error but don't fail the application
                log.error("Failed to send invoice email: {}", emailError.getMessage(), emailError);
            }

            // Notify officers at the assigned workflow step
            try {
                if (transferApplication.getCoreApplicationStep() != null) {
                    String stepName = transferApplication.getCoreApplicationStep().getName();
                    String officerRole = mapStepToOfficerRole(stepName);

                    if (officerRole != null) {
                        log.info("=== NOTIFYING OFFICERS ABOUT NEW TRANSFER APPLICATION ===");
                        log.info("Application ID: {}", transferApplication.getId());
                        log.info("Current Step: {}", stepName);
                        log.info("Officer Role: {}", officerRole);

                        officerNotificationService.notifyOfficersAboutNewApplication(officerRole, transferApplication);
                        log.info("✅ Officer notification completed successfully");
                    } else {
                        log.warn("No officer role mapped for step: {}", stepName);
                    }
                } else {
                    log.warn("Transfer application has no workflow step assigned");
                }
            } catch (Exception notificationError) {
                // Log error but don't fail the application
                log.error("Failed to notify officers: {}", notificationError.getMessage(), notificationError);
            }

            // Prepare response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "License transfer application submitted successfully");
            response.put("id", transferApplication.getId());
            response.put("applicationType", "TRANSFER");
            response.put("originalLicenseId", originalLicense.getId());
            response.put("licenceReferenceNo", licenceReferenceNo);
            response.put("applicantName", applicant.getFirstName() + " " + applicant.getLastName());
            response.put("applicantEmail", applicant.getEmailAddress());
            response.put("status", "SUBMITTED");
            response.put("applicationDate", transferApplication.getDateCreated());
            response.put("licenseType", originalApplication.getCoreLicenseType().getName());
            response.put("applicationFees", 0.0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing license transfer: {}", e.getMessage(), e);
            // Mark transaction for rollback to prevent orphaned application records
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Build client info JSON from change ownership form data
     */
    private String buildClientInfoFromForm(Map<String, Object> request) {
        Map<String, Object> clientInfo = new java.util.HashMap<>();
        
        clientInfo.put("clientName", request.get("name"));
        clientInfo.put("email", request.get("email"));
        clientInfo.put("address", request.get("address"));
        clientInfo.put("telephone", request.get("telephone"));
        clientInfo.put("mobileNumber", request.get("mobileNumber"));
        clientInfo.put("designation", request.get("designation"));
        clientInfo.put("postalAddress", request.get("postalAddress"));
        clientInfo.put("district", request.get("district"));
        clientInfo.put("mainActivity", request.get("mainActivity"));
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo);
        } catch (Exception e) {
            log.error("Error serializing client info: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Build application metadata JSON from change ownership form data
     */
    private String buildApplicationMetadataFromForm(Map<String, Object> request) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        
        metadata.put("applicantName", request.get("name"));
        metadata.put("licenceReferenceNo", request.get("licenceReferenceNo"));
        metadata.put("effectiveDate", request.get("effectiveDate"));
        metadata.put("transferReasons", request.get("transferReasons"));
        metadata.put("declarationDate", request.get("declarationDate"));
        metadata.put("applicationType", "TRANSFER");
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Error serializing application metadata: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Build form specific data JSON from change ownership form data
     */
    private String buildFormSpecificDataFromForm(Map<String, Object> request) {
        Map<String, Object> formData = new java.util.HashMap<>();

        // Include all form fields for complete transfer record
        formData.putAll(request);
        formData.put("formType", "CHANGE_OWNERSHIP_FORM");
        formData.put("submissionTimestamp", new java.util.Date());

        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formData);
        } catch (Exception e) {
            log.error("Error serializing form specific data: {}", e.getMessage());
            return "{}";
        }
    }

    @Autowired
    private mw.nwra.ewaterpermit.repository.CoreLicenseApplicationActivityRepository activityRepository;

    /**
     * Get application history - applications handled by current user's role
     * ULTRA-OPTIMIZED: Uses single native SQL query like the fast /e-services/applications endpoint
     */
    @GetMapping(path = "/application-history")
    @Transactional(readOnly = true)
    public Map<String, Object> getApplicationHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo,
            @RequestHeader(name = "Authorization") String token) {

        log.info("=== GETTING APPLICATION HISTORY (ULTRA-OPTIMIZED) ===");
        log.info("Page: {}, Limit: {}, DateFrom: {}, DateTo: {}", page, limit, dateFrom, dateTo);

        // Get current user and role
        SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        String userRole = currentUser.getSysUserGroup() != null ?
                currentUser.getSysUserGroup().getName() : "";
        log.info("User role: '{}'", userRole);
        log.info("User role length: {}", userRole.length());
        log.info("User role lowercase: '{}'", userRole.toLowerCase());
        log.info("User group ID: {}", currentUser.getSysUserGroup() != null ? currentUser.getSysUserGroup().getId() : "null");

        // Check if date filtering is requested
        boolean hasDateFilter = (dateFrom != null && !dateFrom.trim().isEmpty()) ||
                                (dateTo != null && !dateTo.trim().isEmpty());

        if (hasDateFilter) {
            log.info("Date filtering enabled - using optimized date-filtered queries");
        }

        // Single native SQL query with pagination - same approach as the fast endpoint
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, limit);
        org.springframework.data.domain.Page<Object[]> resultsPage;

        // Check if user is DRS or admin - they should see all applications
        if (userRole.toLowerCase().contains("drs")) {
            log.info("DRS role detected - fetching ALL applications without role filter");
            if (hasDateFilter) {
                resultsPage = activityRepository.findAllApplicationHistoryWithDateFilter(dateFrom, dateTo, pageable);
                log.info("Query returned {} results out of {} total (ALL applications with date filter)",
                        resultsPage.getNumberOfElements(), resultsPage.getTotalElements());
            } else {
                resultsPage = activityRepository.findAllApplicationHistory(pageable);
                log.info("Query returned {} results out of {} total (ALL applications)",
                        resultsPage.getNumberOfElements(), resultsPage.getTotalElements());
            }
        }
        else if(userRole.toLowerCase().contains("admin")) {
            log.info("admin role detected - fetching ALL applications without role filter");
            if (hasDateFilter) {
                resultsPage = activityRepository.findAllApplicationHistoryWithDateFilter(dateFrom, dateTo, pageable);
                log.info("Query returned {} results out of {} total (ALL applications with date filter)",
                        resultsPage.getNumberOfElements(), resultsPage.getTotalElements());
            } else {
                resultsPage = activityRepository.findAllApplicationHistory(pageable);
                log.info("Query returned {} results out of {} total (ALL applications)",
                        resultsPage.getNumberOfElements(), resultsPage.getTotalElements());
            }
        }
        else {
            log.info("Executing query with role parameter: '{}'", userRole);
            if (hasDateFilter) {
                resultsPage = activityRepository.findApplicationHistoryByRoleWithDateFilter(userRole, dateFrom, dateTo, pageable);
                log.info("Query returned {} results out of {} total (with date filter)",
                        resultsPage.getNumberOfElements(), resultsPage.getTotalElements());
            } else {
                resultsPage = activityRepository.findApplicationHistoryByRole(userRole, pageable);
                log.info("Query returned {} results out of {} total",
                        resultsPage.getNumberOfElements(), resultsPage.getTotalElements());
            }
        }

        log.info("Found {} total applications for role: {}", resultsPage.getTotalElements(), userRole);

        // Map Object[] results to response format - same as the fast endpoint
        List<Map<String, Object>> response = resultsPage.getContent().stream().map(row -> {
            Map<String, Object> appData = new java.util.HashMap<>();

            // Map fields from Object[] array (same order as SELECT clause)
            int i = 0;
            appData.put("id", row[i++]);                           // app.id
            appData.put("type", row[i++]);                         // app.application_type
            appData.put("dateSubmitted", row[i++]);                // app.date_submitted
            appData.put("applicationDate", row[i++]);              // app.date_created
            appData.put("status", row[i++]);                       // status.name
            appData.put("licenseType", row[i++]);                  // type.name
            i++;                                                   // type.id (skip)
            appData.put("paymentStatus", row[i++]);                // payments.payment_status
            appData.put("feeType", row[i++]);                      // fees.name
            appData.put("applicationMetadata", row[i++]);          // app.application_metadata
            appData.put("clientInfo", row[i++]);                   // app.client_info
            appData.put("locationInfo", row[i++]);                 // app.location_info
            appData.put("formSpecificData", row[i++]);             // app.form_specific_data
            appData.put("step", row[i++]);                         // step.id
            appData.put("stepName", row[i++]);                     // step.name
            appData.put("stepSequence", row[i++]);                 // step.sequence_number

            String userId = (String) row[i++];                     // user_account.id
            String firstName = (String) row[i++];                  // user_account.first_name
            String lastName = (String) row[i++];                   // user_account.last_name
            String username = (String) row[i++];                   // user_account.username
            String email = (String) row[i++];                      // user_account.email_address

            // Build applicant name
            String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
            fullName = fullName.trim();
            if (fullName.isEmpty()) {
                fullName = username != null ? username : "Unknown";
            }
            appData.put("applicantName", fullName);
            appData.put("applicantEmail", email != null ? email : "");

            appData.put("amountPaid", row[i++]);                   // payments.amount_paid
            appData.put("paymentId", row[i++]);                    // payments.id
            appData.put("paymentMethod", row[i++]);                // payments.payment_method
            appData.put("paymentDate", row[i++]);                  // payments.date_created

            // Emergency fields
            appData.put("applicationPriority", row[i++]);          // app.application_priority
            appData.put("emergencyReason", row[i++]);              // app.emergency_reason
            appData.put("emergencyJustificationFile", row[i++]);   // app.emergency_justification_file
            appData.put("emergencySubmittedDate", row[i++]);       // app.emergency_submitted_date

            // License fee
            appData.put("licenseFee", row[i++]);                   // app.license_fee

            return appData;
        }).collect(Collectors.toList());

        // Build response with pagination metadata
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("applications", response);
        result.put("totalElements", resultsPage.getTotalElements());
        result.put("totalPages", resultsPage.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", limit);

        log.info("Returning {} applications for history (page {} of {}) - SINGLE QUERY!",
                response.size(), page, resultsPage.getTotalPages());

        return result;
    }

    /**
     * Helper method to save emergency document from base64 data
     */
    private String saveEmergencyDocumentFromBase64(String base64Data, String applicationId) throws IOException {
        try {
            // Remove data URL prefix if present (data:application/pdf;base64,...)
            String base64Content = base64Data;
            if (base64Data.contains(",")) {
                String[] parts = base64Data.split(",");
                base64Content = parts.length > 1 ? parts[1] : parts[0];
            }

            // Decode base64
            byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Content);

            // Create directory
            String uploadDir = "uploads/emergency/" + applicationId;
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            java.nio.file.Files.createDirectories(uploadPath);

            // Determine file extension from data URL or default to pdf
            String fileExtension = ".pdf";
            if (base64Data.contains("data:")) {
                String mimeType = base64Data.substring(base64Data.indexOf(":") + 1, base64Data.indexOf(";"));
                if (mimeType.contains("image/jpeg") || mimeType.contains("image/jpg")) {
                    fileExtension = ".jpg";
                } else if (mimeType.contains("image/png")) {
                    fileExtension = ".png";
                } else if (mimeType.contains("application/msword")) {
                    fileExtension = ".doc";
                } else if (mimeType.contains("application/vnd.openxmlformats")) {
                    fileExtension = ".docx";
                }
            }

            // Generate filename
            String filename = "emergency_" + java.util.UUID.randomUUID().toString() + fileExtension;
            java.nio.file.Path filePath = uploadPath.resolve(filename);

            // Write file
            java.nio.file.Files.write(filePath, fileBytes);

            log.info("Emergency document saved to: {}", filePath.toString());
            return filePath.toString();

        } catch (Exception e) {
            log.error("Error saving emergency document: {}", e.getMessage());
            throw new IOException("Failed to save emergency document: " + e.getMessage());
        }
    }

    /**
     * Maps a workflow step name to the corresponding officer role for notifications
     * @param stepName The name of the workflow step
     * @return The officer role/group name, or null if no mapping exists
     */
    private String mapStepToOfficerRole(String stepName) {
        if (stepName == null) {
            return null;
        }

        String stepLower = stepName.toLowerCase();

        // Map common workflow steps to officer roles
        // IMPORTANT: Check more specific patterns BEFORE general patterns
        if (stepLower.contains("payment") || stepLower.contains("accountant") ||
            stepLower.contains("receipt") || stepLower.contains("financial")) {
            return "accountant";
        } else if (stepLower.contains("senior") && (stepLower.contains("license") ||
                   stepLower.contains("licensing"))) {
            // Check for senior license officer BEFORE regular license officer
            return "senior_licensing_officer";
        } else if (stepLower.contains("ceo") || stepLower.contains("technical committee") ||
                   stepLower.contains("final")) {
            // Check for CEO/Technical Committee BEFORE general "technical" check
            return "ceo";
        } else if (stepLower.contains("license officer") || stepLower.contains("technical")) {
            return "licensing_officer";
        } else if (stepLower.contains("manager") || stepLower.contains("approval") ||
                   stepLower.contains("decision") || stepLower.contains("consultation") ||
                   stepLower.contains("stakeholder")) {
            return "licensing_manager";
        } else if (stepLower.contains("assessment") || stepLower.contains("evaluation")) {
            return "technical_officer";
        } else if (stepLower.contains("drs") || stepLower.contains("management review") ||
                   stepLower.contains("authorization")) {
            return "drs";
        } else {
            // Default to licensing officer for unknown steps
            log.info("No specific role mapping found for step '{}', defaulting to licensing_officer", stepName);
            return "licensing_officer";
        }
    }
}