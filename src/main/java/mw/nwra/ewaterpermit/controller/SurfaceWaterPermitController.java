package mw.nwra.ewaterpermit.controller;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreApplicationStatus;
import mw.nwra.ewaterpermit.model.CoreApplicationStep;
import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.requestSchema.SurfaceWaterPermitRequest;
import mw.nwra.ewaterpermit.service.CoreApplicationStatusService;
import mw.nwra.ewaterpermit.service.CoreApplicationStepService;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.service.CoreLicenseTypeService;
import mw.nwra.ewaterpermit.service.MailingService;
import mw.nwra.ewaterpermit.service.PDFGenerationService;
import mw.nwra.ewaterpermit.service.CoreLicenseService;
import mw.nwra.ewaterpermit.service.ApplicationDataLinkingService;
import mw.nwra.ewaterpermit.service.EmailQueueService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1/surface-water-permits")
public class SurfaceWaterPermitController {

    @Autowired
    private CoreLicenseApplicationService licenseApplicationService;

    @Autowired
    private CoreLicenseTypeService licenseTypeService;

    @Autowired
    private CoreApplicationStatusService applicationStatusService;

    @Autowired
    private CoreApplicationStepService applicationStepService;

    @Autowired
    private MailingService mailingService;

    @Autowired
    private PDFGenerationService pdfGenerationService;

    @Autowired
    private CoreLicenseService licenseService;

    @Autowired
    private ApplicationDataLinkingService applicationDataLinkingService;

    @Autowired
    private EmailQueueService emailQueueService;

    @Autowired
    private Auditor auditor;

    @PostMapping(path = "/renew")
    public ResponseEntity<Map<String, Object>> renewSurfaceWaterPermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Debug user account details
        System.out.println("Applicant details: ID=" + applicant.getId() + ", Username=" + applicant.getUsername());
        if (applicant.getId() == null) {
            throw new IllegalStateException("User account ID is null for user: " + applicant.getUsername());
        }

        // Find Surface Water Permit license type
        CoreLicenseType surfaceWaterType = licenseTypeService.getCoreLicenseTypeByName("Surface Water Permit");
        if (surfaceWaterType == null) {
            throw new EntityNotFoundException("Surface Water Permit type not found");
        }

        // Create new renewal application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id and user_account_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());

        // Debug the values being set
        System.out.println("Setting ownerId: " + applicant.getId());
        System.out.println("Setting userAccountId: " + applicant.getId());
        System.out.println("Application ownerId after set: " + application.getOwnerId());
        System.out.println("Application userAccountId after set: " + application.getUserAccountId());
        application.setCoreLicenseType(surfaceWaterType);

        // Set application type to RENEWAL and original license ID
        application.setApplicationType("RENEWAL");
        if (request.get("originalLicenseId") != null) {
            application.setOriginalLicenseId(request.get("originalLicenseId").toString());
        }

        // Set application details from request (same as apply method)
        if (request.get("sourceEasting") != null) application.setSourceEasting(request.get("sourceEasting").toString());
        if (request.get("sourceNorthing") != null)
            application.setSourceNorthing(request.get("sourceNorthing").toString());
        if (request.get("sourceVillage") != null) application.setSourceVillage(request.get("sourceVillage").toString());
        if (request.get("sourceTa") != null) application.setSourceTa(request.get("sourceTa").toString());
        if (request.get("sourcePlotNumber") != null)
            application.setSourcePlotNumber(request.get("sourcePlotNumber").toString());
        if (request.get("sourceOwnerFullname") != null)
            application.setSourceOwnerFullname(request.get("sourceOwnerFullname").toString());
        if (request.get("sourceHectarage") != null)
            application.setSourceHectarage(request.get("sourceHectarage").toString());

        // Set destination details if provided
        if (request.get("destEasting") != null) application.setDestEasting(request.get("destEasting").toString());
        if (request.get("destNorthing") != null) application.setDestNorthing(request.get("destNorthing").toString());
        if (request.get("destVillage") != null) application.setDestVillage(request.get("destVillage").toString());
        if (request.get("destTa") != null) application.setDestTa(request.get("destTa").toString());
        if (request.get("destPlotNumber") != null)
            application.setDestPlotNumber(request.get("destPlotNumber").toString());
        if (request.get("destOwnerFullname") != null)
            application.setDestOwnerFullname(request.get("destOwnerFullname").toString());
        if (request.get("destHectarage") != null) application.setDestHectarage(request.get("destHectarage").toString());

        // Set permit details
        if (request.get("permitDuration") != null) {
            try {
                application.setPermitDuration(Double.valueOf(request.get("permitDuration").toString()));
            } catch (NumberFormatException e) {
                // Log warning but continue
            }
        }
        if (request.get("nearbyWaterUtilityBoard") != null)
            application.setNearbyWaterUtilityBoard(request.get("nearbyWaterUtilityBoard").toString());
        if (request.get("altWaterSource") != null)
            application.setAltWaterSource(request.get("altWaterSource").toString());
        if (request.get("altOtherWater") != null) application.setAltOtherWater(request.get("altOtherWater").toString());
        if (request.get("existingBoreholeCount") != null) {
            try {
                application.setExistingBoreholeCount(Integer.valueOf(request.get("existingBoreholeCount").toString()));
            } catch (NumberFormatException e) {
                // Log warning but continue
            }
        }

        // Set initial status and step
        CoreApplicationStatus initialStatus = applicationStatusService.getCoreApplicationStatusByName("Submitted");
        if (initialStatus == null) {
            initialStatus = applicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
        }
        if (initialStatus == null) {
            // Create default status if not found
            initialStatus = new CoreApplicationStatus();
            initialStatus.setName("SUBMITTED");
            initialStatus.setDescription("Renewal application has been submitted");
            initialStatus = applicationStatusService.addCoreApplicationStatus(initialStatus);
        }
        application.setCoreApplicationStatus(initialStatus);

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(surfaceWaterType);
        if (firstStep == null) {
            // Create default first step if not found
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Renewal application is under initial review");
            firstStep.setCoreLicenseType(surfaceWaterType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // Save application
        application = licenseApplicationService.addCoreLicenseApplication(application);
        auditor.audit(Action.CREATE, "SurfaceWaterPermit", application.getId(), applicant, "Renewed surface water permit");

        // Send email notification to applicant
        try {
            mailingService.send("APPLICATION_SUBMITTED", application.getId(), applicant);
        } catch (Exception e) {
            // Log error but don't fail the application
            System.err.println("Failed to send email notification: " + e.getMessage());
        }

        // Calculate renewal fees (might be different from application fees)
//        double renewalFees = surfaceWaterType.getRenewalFees() > 0 ? surfaceWaterType.getRenewalFees() : surfaceWaterType.getApplicationFees();
        double renewalFees = 10000;
        // Return response
        Map<String, Object> response = Map.of(
                "id", application.getId(),
                "status", application.getCoreApplicationStatus().getName(),
                "message", "Surface water permit renewal submitted successfully",
                "applicationDate", application.getDateCreated(),
                "applicantName", request.getOrDefault("clientName", applicant.getFirstName() + " " + applicant.getLastName()),
                "applicantEmail", request.getOrDefault("clientEmail", applicant.getEmailAddress()),
                "licenseType", "Surface Water Permit",
                "applicationType", "RENEWAL",
                "originalLicenseId", application.getOriginalLicenseId(),
                "renewalFees", renewalFees
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/apply")
    public ResponseEntity<Map<String, Object>> applyForSurfaceWaterPermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Surface Water Permit license type
        CoreLicenseType surfaceWaterType = licenseTypeService.getCoreLicenseTypeByName("Surface Water Permit");
        if (surfaceWaterType == null) {
            throw new EntityNotFoundException("Surface Water Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id and user_account_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());

        // Debug the values being set
        System.out.println("Setting ownerId: " + applicant.getId());
        System.out.println("Setting userAccountId: " + applicant.getId());
        System.out.println("Application ownerId after set: " + application.getOwnerId());
        System.out.println("Application userAccountId after set: " + application.getUserAccountId());
        application.setCoreLicenseType(surfaceWaterType);

        // Set application details from request
        if (request.get("sourceEasting") != null) application.setSourceEasting(request.get("sourceEasting").toString());
        if (request.get("sourceNorthing") != null)
            application.setSourceNorthing(request.get("sourceNorthing").toString());
        if (request.get("sourceVillage") != null) application.setSourceVillage(request.get("sourceVillage").toString());
        if (request.get("sourceTa") != null) application.setSourceTa(request.get("sourceTa").toString());
        if (request.get("sourcePlotNumber") != null)
            application.setSourcePlotNumber(request.get("sourcePlotNumber").toString());
        if (request.get("sourceOwnerFullname") != null)
            application.setSourceOwnerFullname(request.get("sourceOwnerFullname").toString());
        if (request.get("sourceHectarage") != null)
            application.setSourceHectarage(request.get("sourceHectarage").toString());

        // Set destination details if provided
        if (request.get("destEasting") != null) application.setDestEasting(request.get("destEasting").toString());
        if (request.get("destNorthing") != null) application.setDestNorthing(request.get("destNorthing").toString());
        if (request.get("destVillage") != null) application.setDestVillage(request.get("destVillage").toString());
        if (request.get("destTa") != null) application.setDestTa(request.get("destTa").toString());
        if (request.get("destPlotNumber") != null)
            application.setDestPlotNumber(request.get("destPlotNumber").toString());
        if (request.get("destOwnerFullname") != null)
            application.setDestOwnerFullname(request.get("destOwnerFullname").toString());
        if (request.get("destHectarage") != null) application.setDestHectarage(request.get("destHectarage").toString());

        // Set initial status and step
        CoreApplicationStatus initialStatus = applicationStatusService.getCoreApplicationStatusByName("Submitted");
        if (initialStatus == null) {
            initialStatus = applicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
        }
        if (initialStatus == null) {
            // Create default status if not found
            initialStatus = new CoreApplicationStatus();
            initialStatus.setName("SUBMITTED");
            initialStatus.setDescription("Application has been submitted");
            initialStatus = applicationStatusService.addCoreApplicationStatus(initialStatus);
        }
        application.setCoreApplicationStatus(initialStatus);

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(surfaceWaterType);
        if (firstStep == null) {
            // Create default first step if not found
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Application is under initial review");
            firstStep.setCoreLicenseType(surfaceWaterType);
            // firstStep.setStepOrder(1); // Method not available
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // Save application FIRST to get ID
        System.out.println("[SURFACE WATER APPLICATION] Saving application to database...");
        application = licenseApplicationService.addCoreLicenseApplication(application);
        System.out.println("[SURFACE WATER APPLICATION] Application saved with ID: " + application.getId());
        auditor.audit(Action.CREATE, "SurfaceWaterPermit", application.getId(), applicant, "Applied for surface water permit");

        // Handle emergency application data AFTER saving (need application ID for file path)
        System.out.println("[SURFACE WATER APPLICATION] Checking for application priority in request...");
        System.out.println("[SURFACE WATER APPLICATION] Request contains applicationPriority: " + request.containsKey("applicationPriority"));
        System.out.println("[SURFACE WATER APPLICATION] Raw applicationPriority value: " + request.get("applicationPriority"));
        System.out.println("[SURFACE WATER APPLICATION] Request keys: " + request.keySet());

        boolean needsUpdate = false;

        if (request.get("applicationPriority") != null) {
            String priority = request.get("applicationPriority").toString();
            System.out.println("[SURFACE WATER APPLICATION] Priority from request: '" + priority + "'");
            System.out.println("[SURFACE WATER APPLICATION] Priority length: " + priority.length());
            System.out.println("[SURFACE WATER APPLICATION] Priority equals EMERGENCY: " + "EMERGENCY".equals(priority));
            application.setApplicationPriority(priority);
            System.out.println("[SURFACE WATER APPLICATION] Application priority set to: '" + priority + "'");
            System.out.println("[SURFACE WATER APPLICATION] Application.getApplicationPriority(): '" + application.getApplicationPriority() + "'");
            needsUpdate = true;
        } else {
            // Default to NORMAL if not provided
            application.setApplicationPriority("NORMAL");
            System.out.println("[SURFACE WATER APPLICATION] No priority provided, defaulting to NORMAL");
            needsUpdate = true;
        }

        if ("EMERGENCY".equals(application.getApplicationPriority())) {
            System.out.println("[SURFACE WATER APPLICATION] Processing EMERGENCY application...");

            if (request.get("emergencyReason") != null) {
                application.setEmergencyReason(request.get("emergencyReason").toString());
                System.out.println("[SURFACE WATER APPLICATION] Emergency reason: " + application.getEmergencyReason());
            } else {
                System.out.println("[SURFACE WATER APPLICATION] WARNING: No emergency reason provided!");
            }

            // Save emergency document from base64
            if (request.get("emergencyJustificationDocument") != null) {
                try {
                    String base64File = request.get("emergencyJustificationDocument").toString();
                    System.out.println("[SURFACE WATER APPLICATION] Emergency document data length: " + base64File.length());
                    String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                    application.setEmergencyJustificationFile(savedFilePath);
                    System.out.println("[SURFACE WATER APPLICATION] Emergency document saved successfully: " + savedFilePath);
                } catch (IOException e) {
                    System.err.println("[SURFACE WATER APPLICATION] ERROR saving emergency document: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("[SURFACE WATER APPLICATION] WARNING: No emergency document provided!");
            }

            application.setEmergencySubmittedDate(new Timestamp(System.currentTimeMillis()));
            System.out.println("[SURFACE WATER APPLICATION] Emergency submitted date: " + application.getEmergencySubmittedDate());
        }

        // Update application with priority/emergency data
        if (needsUpdate) {
            System.out.println("[SURFACE WATER APPLICATION] Updating application with priority data...");
            application = licenseApplicationService.editCoreLicenseApplication(application);
            System.out.println("[SURFACE WATER APPLICATION] Application updated successfully");
            System.out.println("[SURFACE WATER APPLICATION] Final priority: " + application.getApplicationPriority());
            System.out.println("[SURFACE WATER APPLICATION] Final emergency file: " + application.getEmergencyJustificationFile());
        }
        
        // Link water resource unit and water use data
        System.out.println("[SURFACE WATER APPLICATION] Starting data linking process...");
        applicationDataLinkingService.linkWaterResourceUnit(application, request);
        applicationDataLinkingService.linkWaterUse(application, request);
        System.out.println("[SURFACE WATER APPLICATION] Data linking completed");

        String applicantName = request.getOrDefault("clientName", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("clientEmail", applicant.getEmailAddress()).toString();
        
        // Queue email sending with real application fees
        double applicationFees = surfaceWaterType.getApplicationFees() > 0 ? surfaceWaterType.getApplicationFees() : 8000.0;
        System.out.println("[SURFACE WATER APPLICATION] Application fees: MWK " + applicationFees);

        System.out.println("[SURFACE WATER APPLICATION] Queuing invoice email to: " + applicantEmail);
        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, "Surface Water Permit", applicationFees);
        System.out.println("[SURFACE WATER APPLICATION] Email queued with task ID: " + emailTaskId);

        // Return response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Surface water permit application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Surface Water Permit");
        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);

        System.out.println("[SURFACE WATER APPLICATION] Application completed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/vary")
    public ResponseEntity<Map<String, Object>> varySurfaceWaterPermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Surface Water Permit license type
        CoreLicenseType surfaceWaterType = null;
        if (request.get("licenseTypeId") != null) {
            surfaceWaterType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (surfaceWaterType == null) {
            surfaceWaterType = licenseTypeService.getCoreLicenseTypeByName("Surface Water Permit");
        }
        if (surfaceWaterType == null) {
            throw new EntityNotFoundException("Surface Water Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id and user_account_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());

        // Debug the values being set
        System.out.println("Setting ownerId: " + applicant.getId());
        System.out.println("Setting userAccountId: " + applicant.getId());
        System.out.println("Application ownerId after set: " + application.getOwnerId());
        System.out.println("Application userAccountId after set: " + application.getUserAccountId());
        application.setCoreLicenseType(surfaceWaterType);
        
        // Set application type to VARIATION and original license ID
        application.setApplicationType("VARIATION");
        if (request.get("originalLicenseId") != null) {
            application.setOriginalLicenseId(request.get("originalLicenseId").toString());
        }

        // Set application details from request (same as apply)
        if (request.get("clientName") != null) application.setSourceOwnerFullname(request.get("clientName").toString());
        if (request.get("clientAddress") != null) application.setSourceVillage(request.get("clientAddress").toString());
        if (request.get("district") != null) application.setSourceTa(request.get("district").toString());
        if (request.get("gpsCoordinates") != null) {
            String coords = request.get("gpsCoordinates").toString();
            String[] coordParts = coords.split(",");
            if (coordParts.length >= 2) {
                application.setSourceEasting(coordParts[0].trim());
                application.setSourceNorthing(coordParts[1].trim());
            }
        }
        if (request.get("village") != null) application.setSourceVillage(request.get("village").toString());
        if (request.get("traditionalAuthority") != null)
            application.setSourceTa(request.get("traditionalAuthority").toString());
        if (request.get("plotNo") != null) application.setSourcePlotNumber(request.get("plotNo").toString());

        // Set initial status and step
        CoreApplicationStatus initialStatus = applicationStatusService.getCoreApplicationStatusByName("Submitted");
        if (initialStatus == null) {
            initialStatus = applicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
        }
        if (initialStatus == null) {
            initialStatus = new CoreApplicationStatus();
            initialStatus.setName("SUBMITTED");
            initialStatus.setDescription("Variation application has been submitted");
            initialStatus = applicationStatusService.addCoreApplicationStatus(initialStatus);
        }
        application.setCoreApplicationStatus(initialStatus);

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(surfaceWaterType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Variation application is under initial review");
            firstStep.setCoreLicenseType(surfaceWaterType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(applicationStepService.getNextStep(firstStep));

        // Populate JSON fields with all form data
        try {
            // Populate CLIENT_INFO JSON
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("clientEmail", request.get("clientEmail"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            // Populate LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("gpsCoordinates", request.get("gpsCoordinates"));
            locationInfo.put("plotNo", request.get("plotNo"));
            locationInfo.put("waterSource", request.get("waterSource"));
            locationInfo.put("waterSourceType", request.get("waterSourceType"));
            locationInfo.put("abstractionPoint", request.get("abstractionPoint"));
            locationInfo.put("abstractionMethod", request.get("abstractionMethod"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            // Populate APPLICATION_METADATA JSON
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "VARIATION");
            applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            // Populate FORM_SPECIFIC_DATA JSON
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterUsePurpose", request.get("waterUsePurpose"));
            formSpecificData.put("estimatedQuantity", request.get("estimatedQuantity"));
            formSpecificData.put("quantityUnit", request.get("quantityUnit"));
            formSpecificData.put("operationPeriod", request.get("operationPeriod"));
            formSpecificData.put("irrigationArea", request.get("irrigationArea"));
            formSpecificData.put("cropType", request.get("cropType"));
            formSpecificData.put("seasonalUse", request.get("seasonalUse"));
            formSpecificData.put("storageCapacity", request.get("storageCapacity"));
            formSpecificData.put("returnFlowDetails", request.get("returnFlowDetails"));
            
            // Add all remaining fields
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) && 
                    !locationInfo.containsKey(entry.getKey()) && 
                    !applicationMetadata.containsKey(entry.getKey()) &&
                    !formSpecificData.containsKey(entry.getKey()) &&
                    !entry.getKey().equals("licenseTypeId")) {
                    formSpecificData.put(entry.getKey(), entry.getValue());
                }
            }
            application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
        } catch (Exception e) {
            System.err.println("Error populating JSON fields: " + e.getMessage());
        }

        // Save application
        application = licenseApplicationService.addCoreLicenseApplication(application);

        String applicantName = request.getOrDefault("clientName", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("clientEmail", applicant.getEmailAddress()).toString();

        // Create response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Surface water permit variation application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Surface Water Permit Variation");
        response.put("applicationType", "VARIATION");
        response.put("originalLicenseId", application.getOriginalLicenseId());

        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/my-applications")
    public ResponseEntity<List<Map<String, Object>>> getMyApplications(
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        List<CoreLicenseApplication> applications = licenseApplicationService
                .getCoreLicenseApplicationByApplicant(applicant);

        // Convert to simplified response format
        List<Map<String, Object>> response = applications.stream().map(app -> {
            Map<String, Object> appData = new java.util.HashMap<>();
            appData.put("id", app.getId());
            appData.put("status", app.getCoreApplicationStatus() != null ? app.getCoreApplicationStatus().getName() : "UNKNOWN");
            appData.put("applicationDate", app.getDateCreated());
            appData.put("applicantName", app.getSysUserAccount().getFirstName() + " " + app.getSysUserAccount().getLastName());
            appData.put("applicantEmail", app.getSysUserAccount().getEmailAddress());
            appData.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
            appData.put("currentStep", app.getCoreApplicationStep() != null ? app.getCoreApplicationStep().getName() : "Unknown");
            double feeAmount = app.getCoreLicenseType() != null ? app.getCoreLicenseType().getApplicationFees() : 20000.0;
            appData.put("applicationFees", feeAmount);
            appData.put("paymentStatus", Map.of("status", "PENDING", "amount", feeAmount));
            return appData;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{applicationId}/status")
    public ResponseEntity<Map<String, Object>> getApplicationStatus(
            @PathVariable String applicationId,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        // Ensure user can only view their own applications
        if (!application.getSysUserAccount().getId().equals(applicant.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        Map<String, Object> status = Map.of(
                "applicationId", application.getId(),
                "status", application.getCoreApplicationStatus() != null ? application.getCoreApplicationStatus().getName() : "UNKNOWN",
                "currentStep", application.getCoreApplicationStep() != null ? application.getCoreApplicationStep().getName() : "Unknown",
                "dateSubmitted", application.getDateCreated(),
                "stepDescription", application.getCoreApplicationStep() != null ? application.getCoreApplicationStep().getDescription() : "No description available",
                "paymentStatus", Map.of("status", "PENDING", "amount", 5000.0, "currency", "MWK")
        );

        return ResponseEntity.ok(status);
    }

    @PutMapping(path = "/{applicationId}/approve")
    public ResponseEntity<CoreLicenseApplication> approveApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> approvalData,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount approver = AppUtil.getLoggedInUser(token);
        if (approver == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Check if user has approval permissions
        if (!AppUtil.hasPermission(approver, "APPROVE_APPLICATIONS")) {
            throw new ForbiddenException("Insufficient permissions to approve applications");
        }

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        // Move to next step in workflow
        CoreApplicationStep nextStep = applicationStepService.getNextStep(application.getCoreApplicationStep());
        if (nextStep != null) {
            application.setCoreApplicationStep(nextStep);
        }

        // Update status if final approval
        if (nextStep == null || nextStep.getName().equals("Final Approval")) {
            CoreApplicationStatus approvedStatus = applicationStatusService.getCoreApplicationStatusByName("Approved");
            if (approvedStatus != null) {
                application.setCoreApplicationStatus(approvedStatus);
            }
        }

        application.setDateUpdated(new Timestamp(new Date().getTime()));
        application = licenseApplicationService.editCoreLicenseApplication(application);

        // Send notification to applicant
        try {
            mailingService.send("APPLICATION_APPROVED", application.getId(), application.getSysUserAccount());
        } catch (Exception e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
        }

        return ResponseEntity.ok(application);
    }

    @PutMapping(path = "/{applicationId}/reject")
    public ResponseEntity<CoreLicenseApplication> rejectApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> rejectionData,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount approver = AppUtil.getLoggedInUser(token);
        if (approver == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Check if user has approval permissions
        if (!AppUtil.hasPermission(approver, "APPROVE_APPLICATIONS")) {
            throw new ForbiddenException("Insufficient permissions to reject applications");
        }

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        // Set rejected status
        CoreApplicationStatus rejectedStatus = applicationStatusService.getCoreApplicationStatusByName("Rejected");
        if (rejectedStatus != null) {
            application.setCoreApplicationStatus(rejectedStatus);
        }

        application.setDateUpdated(new Timestamp(new Date().getTime()));
        application = licenseApplicationService.editCoreLicenseApplication(application);

        // Send notification to applicant
        try {
            mailingService.send("APPLICATION_REJECTED", application.getId(), application.getSysUserAccount());
        } catch (Exception e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
        }

        return ResponseEntity.ok(application);
    }

    @PutMapping(path = "/{applicationId}/refer-back")
    public ResponseEntity<CoreLicenseApplication> referBackApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> referralData,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount approver = AppUtil.getLoggedInUser(token);
        if (approver == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Check if user has approval permissions
        if (!AppUtil.hasPermission(approver, "APPROVE_APPLICATIONS")) {
            throw new ForbiddenException("Insufficient permissions to refer back applications");
        }

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        // Move to previous step in workflow
        CoreApplicationStep previousStep = applicationStepService.getPreviousStep(application.getCoreApplicationStep());
        if (previousStep != null) {
            application.setCoreApplicationStep(previousStep);
        }

        // Set referred back status
        CoreApplicationStatus referredStatus = applicationStatusService.getCoreApplicationStatusByName("Referred Back");
        if (referredStatus != null) {
            application.setCoreApplicationStatus(referredStatus);
        }

        application.setDateUpdated(new Timestamp(new Date().getTime()));
        application = licenseApplicationService.editCoreLicenseApplication(application);

        // Send notification to applicant
        try {
            mailingService.send("APPLICATION_REFERRED_BACK", application.getId(), application.getSysUserAccount());
        } catch (Exception e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
        }

        return ResponseEntity.ok(application);
    }

    @GetMapping(path = "/permits/{licenseId}/download")
    public ResponseEntity<byte[]> downloadPermitPDF(
            @PathVariable String licenseId,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        CoreLicense license = licenseService.getCoreLicenseById(licenseId);
        if (license == null) {
            throw new EntityNotFoundException("License not found");
        }

        // Ensure user can only download their own permits
        if (!license.getCoreLicenseApplication().getSysUserAccount().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this permit");
        }

        byte[] pdfBytes = pdfGenerationService.generateSurfaceWaterPermitPDF(license);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/pdf");
        headers.set("Content-Disposition", "attachment; filename=surface_water_permit_" + licenseId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping(path = "/{applicationId}/generate-invoice")
    public ResponseEntity<Map<String, Object>> generateInvoice(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> invoiceData) {

        try {
            // Generate invoice reference
            String invoiceReference = "INV-" + System.currentTimeMillis();

            // Create invoice response
            Map<String, Object> invoice = Map.of(
                    "invoiceReference", invoiceReference,
                    "applicationId", applicationId,
                    "amount", invoiceData.getOrDefault("feeAmount", 5000.0),
                    "currency", "MWK",
                    "dueDate", invoiceData.getOrDefault("dueDate", new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)),
                    "status", "PENDING",
                    "applicantName", invoiceData.get("applicantName"),
                    "applicantEmail", invoiceData.get("applicantEmail"),
                    "licenseType", invoiceData.get("licenseType"),
                    "generatedDate", new Date()
            );

            System.out.println("Invoice generated: " + invoice);

            return ResponseEntity.ok(invoice);

        } catch (Exception e) {
            System.err.println("Error generating invoice: " + e.getMessage());
            throw new RuntimeException("Failed to generate invoice");
        }
    }

    @PostMapping(path = "/{applicationId}/process-payment")
    public ResponseEntity<Map<String, Object>> processPayment(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> paymentData,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        // Ensure user can only pay for their own applications
        if (!application.getSysUserAccount().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        // Process payment (integrate with payment gateway)
        String paymentReference = "PAY-" + System.currentTimeMillis();

        // Update application status after successful payment
        CoreApplicationStatus paidStatus = applicationStatusService.getCoreApplicationStatusByName("Payment Completed");
        if (paidStatus == null) {
            paidStatus = applicationStatusService.getCoreApplicationStatusByName("PAID");
        }
        if (paidStatus != null) {
            application.setCoreApplicationStatus(paidStatus);
            application.setDateUpdated(new Timestamp(new Date().getTime()));
            licenseApplicationService.editCoreLicenseApplication(application);
        }

        // Send notification
        try {
            mailingService.send("PAYMENT_COMPLETED", application.getId(), user);
        } catch (Exception e) {
            System.err.println("Failed to send payment notification: " + e.getMessage());
        }

        Map<String, Object> response = Map.of(
                "applicationId", applicationId,
                "paymentReference", paymentReference,
                "status", "COMPLETED",
                "amount", paymentData.get("amount"),
                "currency", paymentData.get("currency"),
                "processedDate", new Date().toString()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(path = "/{applicationId}/edit")
    public ResponseEntity<Map<String, Object>> editSurfaceWaterPermitApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find the existing application
        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found with ID: " + applicationId);
        }

        // Ensure user can only edit their own applications
        if (!application.getSysUserAccount().getId().equals(applicant.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        // Update application details from request (same logic as apply method)
        if (request.get("sourceEasting") != null) application.setSourceEasting(request.get("sourceEasting").toString());
        if (request.get("sourceNorthing") != null)
            application.setSourceNorthing(request.get("sourceNorthing").toString());
        if (request.get("sourceVillage") != null) application.setSourceVillage(request.get("sourceVillage").toString());
        if (request.get("sourceTa") != null) application.setSourceTa(request.get("sourceTa").toString());
        if (request.get("sourcePlotNumber") != null)
            application.setSourcePlotNumber(request.get("sourcePlotNumber").toString());
        if (request.get("sourceOwnerFullname") != null)
            application.setSourceOwnerFullname(request.get("sourceOwnerFullname").toString());
        if (request.get("sourceHectarage") != null)
            application.setSourceHectarage(request.get("sourceHectarage").toString());

        // Update destination details if provided
        if (request.get("destEasting") != null) application.setDestEasting(request.get("destEasting").toString());
        if (request.get("destNorthing") != null) application.setDestNorthing(request.get("destNorthing").toString());
        if (request.get("destVillage") != null) application.setDestVillage(request.get("destVillage").toString());
        if (request.get("destTa") != null) application.setDestTa(request.get("destTa").toString());
        if (request.get("destPlotNumber") != null)
            application.setDestPlotNumber(request.get("destPlotNumber").toString());
        if (request.get("destOwnerFullname") != null)
            application.setDestOwnerFullname(request.get("destOwnerFullname").toString());
        if (request.get("destHectarage") != null) application.setDestHectarage(request.get("destHectarage").toString());

        // Update permit details
        if (request.get("permitDuration") != null) {
            try {
                application.setPermitDuration(Double.valueOf(request.get("permitDuration").toString()));
            } catch (NumberFormatException e) {
                // Log warning but continue
            }
        }
        if (request.get("nearbyWaterUtilityBoard") != null)
            application.setNearbyWaterUtilityBoard(request.get("nearbyWaterUtilityBoard").toString());
        if (request.get("altWaterSource") != null)
            application.setAltWaterSource(request.get("altWaterSource").toString());
        if (request.get("altOtherWater") != null) application.setAltOtherWater(request.get("altOtherWater").toString());
        if (request.get("existingBoreholeCount") != null) {
            try {
                application.setExistingBoreholeCount(Integer.valueOf(request.get("existingBoreholeCount").toString()));
            } catch (NumberFormatException e) {
                // Log warning but continue
            }
        }

        // Update JSON fields with all form data
        try {
            // Update CLIENT_INFO JSON
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("clientEmail", request.get("clientEmail"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            // Update LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("gpsCoordinates", request.get("gpsCoordinates"));
            locationInfo.put("plotNo", request.get("plotNo"));
            locationInfo.put("waterSource", request.get("waterSource"));
            locationInfo.put("waterSourceType", request.get("waterSourceType"));
            locationInfo.put("abstractionPoint", request.get("abstractionPoint"));
            locationInfo.put("abstractionMethod", request.get("abstractionMethod"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            // Update APPLICATION_METADATA JSON
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", request.get("applicationType"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateUpdated", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            // Update FORM_SPECIFIC_DATA JSON
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterUsePurpose", request.get("waterUsePurpose"));
            formSpecificData.put("estimatedQuantity", request.get("estimatedQuantity"));
            formSpecificData.put("quantityUnit", request.get("quantityUnit"));
            formSpecificData.put("operationPeriod", request.get("operationPeriod"));
            formSpecificData.put("irrigationArea", request.get("irrigationArea"));
            formSpecificData.put("cropType", request.get("cropType"));
            formSpecificData.put("seasonalUse", request.get("seasonalUse"));
            formSpecificData.put("storageCapacity", request.get("storageCapacity"));
            formSpecificData.put("returnFlowDetails", request.get("returnFlowDetails"));
            
            // Add all remaining fields
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) && 
                    !locationInfo.containsKey(entry.getKey()) && 
                    !applicationMetadata.containsKey(entry.getKey()) &&
                    !formSpecificData.containsKey(entry.getKey()) &&
                    !entry.getKey().equals("licenseTypeId")) {
                    formSpecificData.put(entry.getKey(), entry.getValue());
                }
            }
            application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
        } catch (Exception e) {
            System.err.println("Error updating JSON fields: " + e.getMessage());
        }

        // Reset status to SUBMITTED for reprocessing
        CoreApplicationStatus submittedStatus = applicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
        if (submittedStatus != null) {
            application.setCoreApplicationStatus(submittedStatus);
        }

        // Update timestamp
        application.setDateUpdated(new Timestamp(new Date().getTime()));

        // Save updated application
        application = licenseApplicationService.editCoreLicenseApplication(application);
        auditor.audit(Action.UPDATE, "SurfaceWaterPermit", applicationId, applicant, "Updated surface water permit application");

        String applicantName = request.getOrDefault("clientName", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("clientEmail", applicant.getEmailAddress()).toString();

        // Create response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Surface water permit application updated successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("updatedDate", application.getDateUpdated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Surface Water Permit");

        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/all")
    public ResponseEntity<List<Map<String, Object>>> getAllApplications(
            @RequestHeader(name = "Authorization") String token,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Check if user has admin permissions
        if (!AppUtil.hasPermission(user, "VIEW_ALL_APPLICATIONS")) {
            throw new ForbiddenException("Insufficient permissions to view all applications");
        }

        List<CoreLicenseApplication> applications = licenseApplicationService.getAllCoreLicenseApplications();

        // Convert to simplified response format
        List<Map<String, Object>> response = applications.stream().map(app -> {
            Map<String, Object> appData = new java.util.HashMap<>();
            appData.put("id", app.getId());
            appData.put("status", app.getCoreApplicationStatus() != null ? app.getCoreApplicationStatus().getName() : "UNKNOWN");
            appData.put("applicationDate", app.getDateCreated());
            appData.put("applicantName", app.getSysUserAccount().getFirstName() + " " + app.getSysUserAccount().getLastName());
            appData.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
            appData.put("currentStep", app.getCoreApplicationStep() != null ? app.getCoreApplicationStep().getName() : "Unknown");
            appData.put("paymentStatus", Map.of("status", "PENDING", "amount", 5000.0));
            return appData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
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

            System.out.println("[EMERGENCY DOCUMENT] Saved to: " + filePath.toString());
            return filePath.toString();

        } catch (Exception e) {
            System.err.println("[EMERGENCY DOCUMENT] Error saving file: " + e.getMessage());
            throw new IOException("Failed to save emergency document: " + e.getMessage());
        }
    }
}