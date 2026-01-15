package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreApplicationStatus;
import mw.nwra.ewaterpermit.model.CoreApplicationStep;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreApplicationStatusService;
import mw.nwra.ewaterpermit.service.ApplicationDataLinkingService;
import mw.nwra.ewaterpermit.service.CoreApplicationStepService;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.service.CoreLicenseTypeService;
import mw.nwra.ewaterpermit.service.EmailQueueService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/ground-water-permits")

public class GroundWaterPermitController {

    @Autowired
    private CoreLicenseApplicationService licenseApplicationService;

    @Autowired
    private CoreLicenseTypeService licenseTypeService;

    @Autowired
    private CoreApplicationStatusService applicationStatusService;

    @Autowired
    private CoreApplicationStepService applicationStepService;

    @Autowired
    private EmailQueueService emailQueueService;

    @Autowired
    private ApplicationDataLinkingService applicationDataLinkingService;

    @Autowired
    private Auditor auditor;

    @PostMapping(path = "/apply")
    public ResponseEntity<Map<String, Object>> applyForGroundWaterPermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        System.out.println("[GROUND WATER APPLICATION] ========== NEW APPLICATION STARTED ==========");
        System.out.println("[GROUND WATER APPLICATION] Endpoint hit: POST /v1/ground-water-permits/apply");
        System.out.println("[GROUND WATER APPLICATION] Request payload size: " + request.size() + " fields");
        System.out.println("[GROUND WATER APPLICATION] Request data: " + request);
        
        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            System.out.println("[GROUND WATER APPLICATION] ERROR: User not authenticated");
            throw new ForbiddenException("User not authenticated");
        }
        
        System.out.println("[GROUND WATER APPLICATION] Authenticated user: " + applicant.getUsername() + " (ID: " + applicant.getId() + ")");

        // Find Groundwater Permit license type - check if licenseTypeId is provided first
        CoreLicenseType groundWaterType = null;
        if (request.get("licenseTypeId") != null) {
            groundWaterType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (groundWaterType == null) {
            groundWaterType = licenseTypeService.getCoreLicenseTypeByName("Groundwater Permit");
        }
        if (groundWaterType == null) {
            throw new EntityNotFoundException("Groundwater Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id and user_account_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(groundWaterType);

        // Set application details from request
        if (request.get("name") != null) application.setSourceOwnerFullname(request.get("name").toString());
        if (request.get("address") != null) application.setSourceVillage(request.get("address").toString());
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
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("worksLandowner") != null)
            application.setDestOwnerFullname(request.get("worksLandowner").toString());
        if (request.get("waterUseLocation") != null)
            application.setDestVillage(request.get("waterUseLocation").toString());

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

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(groundWaterType);
        if (firstStep == null) {
            // Create default first step if not found
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Application is under initial review");
            firstStep.setCoreLicenseType(groundWaterType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // ==> POPULATE JSON FIELDS WITH ALL FORM DATA
        try {
            // Populate CLIENT_INFO JSON
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("name", request.get("name"));
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("address", request.get("address"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("worksLandowner", request.get("worksLandowner"));
            clientInfo.put("waterUseLandowner", request.get("waterUseLandowner"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            // Populate LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("gpsCoordinates", request.get("gpsCoordinates"));
            locationInfo.put("village", request.get("village"));
            locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leaseholdPlotNo", request.get("leaseholdPlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            locationInfo.put("worksLocation", request.get("worksLocation"));
            locationInfo.put("waterUseLocation", request.get("waterUseLocation"));
            locationInfo.put("plotNo", request.get("plotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            // Populate APPLICATION_METADATA JSON
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "ground_water");
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            // Populate FORM_SPECIFIC_DATA JSON (Ground Water specific fields)
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterSource", request.get("waterSource"));
            formSpecificData.put("otherWaterSource", request.get("otherWaterSource"));
            formSpecificData.put("dateDrilled", request.get("dateDrilled"));
            formSpecificData.put("driller", request.get("driller"));
            formSpecificData.put("diameter", request.get("diameter"));
            formSpecificData.put("depth", request.get("depth"));
            formSpecificData.put("linningCasings", request.get("linningCasings"));
            formSpecificData.put("testYield", request.get("testYield"));
            formSpecificData.put("waterUses", request.get("waterUses"));
            formSpecificData.put("pumpType", request.get("pumpType"));
            formSpecificData.put("otherPumpType", request.get("otherPumpType"));
            formSpecificData.put("drivingMachine", request.get("drivingMachine"));
            formSpecificData.put("brakeHorsepower", request.get("brakeHorsepower"));
            formSpecificData.put("pumpElevation", request.get("pumpElevation"));
            formSpecificData.put("pumpConnection", request.get("pumpConnection"));
            formSpecificData.put("suctionMainDiameter", request.get("suctionMainDiameter"));
            formSpecificData.put("maxSuctionHeight", request.get("maxSuctionHeight"));
            formSpecificData.put("waterLiftHeight", request.get("waterLiftHeight"));
            formSpecificData.put("deliveryPipeLength", request.get("deliveryPipeLength"));
            formSpecificData.put("pumpingHours", request.get("pumpingHours"));
            formSpecificData.put("dailyPumpingQuantity", request.get("dailyPumpingQuantity"));
            formSpecificData.put("measurementMethod", request.get("measurementMethod"));
            formSpecificData.put("alternativeSources", request.get("alternativeSources"));
            formSpecificData.put("otherAlternativeSource", request.get("otherAlternativeSource"));
            formSpecificData.put("existingBoreholes", request.get("existingBoreholes"));
            formSpecificData.put("existingBoreholesCount", request.get("existingBoreholesCount"));
            formSpecificData.put("existingBoreholesDetails", request.get("existingBoreholesDetails"));
            formSpecificData.put("waterUtilityArea", request.get("waterUtilityArea"));
            formSpecificData.put("waterUtilityName", request.get("waterUtilityName"));
            formSpecificData.put("sketchMap", request.get("sketchMap"));
            formSpecificData.put("boreholeReport", request.get("boreholeReport"));

            // Add all other form fields to preserve complete form data (exclude emergency fields as they're stored in dedicated columns)
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) &&
                        !locationInfo.containsKey(entry.getKey()) &&
                        !applicationMetadata.containsKey(entry.getKey()) &&
                        !entry.getKey().equals("licenseTypeId") &&
                        !entry.getKey().equals("applicationPriority") &&
                        !entry.getKey().equals("emergencyReason") &&
                        !entry.getKey().equals("emergencyJustificationDocument")) {
                    formSpecificData.put(entry.getKey(), entry.getValue());
                }
            }
            application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
        } catch (Exception e) {
            // Log error but don't fail the application
            System.err.println("Error populating JSON fields: " + e.getMessage());
        }

        // Set application priority before saving
        if (request.get("applicationPriority") != null) {
            String priority = request.get("applicationPriority").toString();
            application.setApplicationPriority(priority);
            System.out.println("[GROUND WATER APPLICATION] Application priority set to: " + priority);
        } else {
            application.setApplicationPriority("NORMAL");
            System.out.println("[GROUND WATER APPLICATION] No priority provided, defaulting to NORMAL");
        }

        // Save application
        System.out.println("[GROUND WATER APPLICATION] Saving application to database...");
        application = licenseApplicationService.addCoreLicenseApplication(application);
        System.out.println("[GROUND WATER APPLICATION] Application saved with ID: " + application.getId());
        
        // Audit log
        auditor.audit(Action.CREATE, "GroundWaterPermit", application.getId(), applicant, "Created groundwater permit application");

        // Handle emergency application fields after application is saved
        if ("EMERGENCY".equals(application.getApplicationPriority())) {
            System.out.println("[GROUND WATER APPLICATION] Processing EMERGENCY application...");

            if (request.get("emergencyReason") != null) {
                application.setEmergencyReason(request.get("emergencyReason").toString());
                System.out.println("[GROUND WATER APPLICATION] Emergency reason: " + application.getEmergencyReason());
            } else {
                System.out.println("[GROUND WATER APPLICATION] WARNING: No emergency reason provided!");
            }

            // Save emergency document from base64
            if (request.get("emergencyJustificationDocument") != null) {
                try {
                    String base64File = request.get("emergencyJustificationDocument").toString();
                    System.out.println("[GROUND WATER APPLICATION] Emergency document data length: " + base64File.length());
                    String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                    application.setEmergencyJustificationFile(savedFilePath);
                    System.out.println("[GROUND WATER APPLICATION] Emergency document saved successfully: " + savedFilePath);
                } catch (Exception e) {
                    System.err.println("[GROUND WATER APPLICATION] ERROR saving emergency document: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("[GROUND WATER APPLICATION] WARNING: No emergency document provided!");
            }

            // Set emergency submission timestamp
            application.setEmergencySubmittedDate(new java.sql.Timestamp(new Date().getTime()));

            // Update the application with emergency data
            application = licenseApplicationService.editCoreLicenseApplication(application);
            System.out.println("[GROUND WATER APPLICATION] Emergency data saved successfully");
        }
        
        // Link water resource unit and water use data
        System.out.println("[GROUND WATER APPLICATION] Starting data linking process...");
        applicationDataLinkingService.linkWaterResourceUnit(application, request);
        applicationDataLinkingService.linkWaterUse(application, request);
        System.out.println("[GROUND WATER APPLICATION] Data linking completed");

        String applicantName = request.getOrDefault("name", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("email", applicant.getEmailAddress()).toString();

        // Create response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Groundwater permit application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Groundwater Permit");

        // Queue email sending with real application fees
        double applicationFees = groundWaterType.getApplicationFees() > 0 ? groundWaterType.getApplicationFees() : 7500.0;

        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, "Groundwater Permit", applicationFees);

        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);

        System.out.println("[GROUND WATER APPLICATION] ========== APPLICATION COMPLETED SUCCESSFULLY ==========");
        System.out.println("[GROUND WATER APPLICATION] Final response: " + response);
        return ResponseEntity.ok(response);
    }

    @PutMapping(path = "/{applicationId}/edit")
    public ResponseEntity<Map<String, Object>> editGroundWaterPermitApplication(
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
        if (request.get("name") != null) application.setSourceOwnerFullname(request.get("name").toString());
        if (request.get("address") != null) application.setSourceVillage(request.get("address").toString());
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
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("worksLandowner") != null)
            application.setDestOwnerFullname(request.get("worksLandowner").toString());
        if (request.get("waterUseLocation") != null)
            application.setDestVillage(request.get("waterUseLocation").toString());

        // Update JSON fields with all form data
        try {
            // Update CLIENT_INFO JSON
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("name", request.get("name"));
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("address", request.get("address"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("worksLandowner", request.get("worksLandowner"));
            clientInfo.put("waterUseLandowner", request.get("waterUseLandowner"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            // Update LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("gpsCoordinates", request.get("gpsCoordinates"));
            locationInfo.put("village", request.get("village"));
            locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leaseholdPlotNo", request.get("leaseholdPlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            locationInfo.put("worksLocation", request.get("worksLocation"));
            locationInfo.put("waterUseLocation", request.get("waterUseLocation"));
            locationInfo.put("plotNo", request.get("plotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            // Update APPLICATION_METADATA JSON
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", request.get("applicationType"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateUpdated", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            // Update FORM_SPECIFIC_DATA JSON (Ground Water specific fields)
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterSource", request.get("waterSource"));
            formSpecificData.put("otherWaterSource", request.get("otherWaterSource"));
            formSpecificData.put("dateDrilled", request.get("dateDrilled"));
            formSpecificData.put("driller", request.get("driller"));
            formSpecificData.put("diameter", request.get("diameter"));
            formSpecificData.put("depth", request.get("depth"));
            formSpecificData.put("linningCasings", request.get("linningCasings"));
            formSpecificData.put("testYield", request.get("testYield"));
            formSpecificData.put("waterUses", request.get("waterUses"));
            formSpecificData.put("pumpType", request.get("pumpType"));
            formSpecificData.put("otherPumpType", request.get("otherPumpType"));
            formSpecificData.put("drivingMachine", request.get("drivingMachine"));
            formSpecificData.put("brakeHorsepower", request.get("brakeHorsepower"));
            formSpecificData.put("pumpElevation", request.get("pumpElevation"));
            formSpecificData.put("pumpConnection", request.get("pumpConnection"));
            formSpecificData.put("suctionMainDiameter", request.get("suctionMainDiameter"));
            formSpecificData.put("maxSuctionHeight", request.get("maxSuctionHeight"));
            formSpecificData.put("waterLiftHeight", request.get("waterLiftHeight"));
            formSpecificData.put("deliveryPipeLength", request.get("deliveryPipeLength"));
            formSpecificData.put("pumpingHours", request.get("pumpingHours"));
            formSpecificData.put("dailyPumpingQuantity", request.get("dailyPumpingQuantity"));
            formSpecificData.put("measurementMethod", request.get("measurementMethod"));
            formSpecificData.put("alternativeSources", request.get("alternativeSources"));
            formSpecificData.put("otherAlternativeSource", request.get("otherAlternativeSource"));
            formSpecificData.put("existingBoreholes", request.get("existingBoreholes"));
            formSpecificData.put("existingBoreholesCount", request.get("existingBoreholesCount"));
            formSpecificData.put("existingBoreholesDetails", request.get("existingBoreholesDetails"));
            formSpecificData.put("waterUtilityArea", request.get("waterUtilityArea"));
            formSpecificData.put("waterUtilityName", request.get("waterUtilityName"));
            formSpecificData.put("sketchMap", request.get("sketchMap"));
            formSpecificData.put("boreholeReport", request.get("boreholeReport"));

            // Add all other form fields to preserve complete form data
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
        
        // Audit log
        auditor.audit(Action.UPDATE, "GroundWaterPermit", application.getId(), applicant, "Updated groundwater permit application");

        String applicantName = request.getOrDefault("name", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("email", applicant.getEmailAddress()).toString();

        // Create response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Groundwater permit application updated successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("updatedDate", application.getDateUpdated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Groundwater Permit");

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

        // Filter for ground water permits only
        List<Map<String, Object>> response = applications.stream()
                .filter(app -> app.getCoreLicenseType() != null &&
                        "Groundwater Permit".equals(app.getCoreLicenseType().getName()))
                .map(app -> {
                    Map<String, Object> appData = new java.util.HashMap<>();
                    appData.put("id", app.getId());
                    appData.put("status", app.getCoreApplicationStatus() != null ? app.getCoreApplicationStatus().getName() : "UNKNOWN");
                    appData.put("applicationDate", app.getDateCreated());
                    appData.put("applicantName", app.getSysUserAccount().getFirstName() + " " + app.getSysUserAccount().getLastName());
                    appData.put("applicantEmail", app.getSysUserAccount().getEmailAddress());
                    appData.put("licenseType", app.getCoreLicenseType().getName());
                    appData.put("currentStep", app.getCoreApplicationStep() != null ? app.getCoreApplicationStep().getName() : "Unknown");
                    double feeAmount = app.getCoreLicenseType().getApplicationFees() > 0 ? app.getCoreLicenseType().getApplicationFees() : 7500.0;
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
                "paymentStatus", Map.of("status", "PENDING", "amount", 7500.0, "currency", "MWK")
        );

        return ResponseEntity.ok(status);
    }

    @PostMapping(path = "/renew")
    public ResponseEntity<Map<String, Object>> renewGroundWaterPermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Groundwater Permit license type
        CoreLicenseType groundWaterType = null;
        if (request.get("licenseTypeId") != null) {
            groundWaterType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (groundWaterType == null) {
            groundWaterType = licenseTypeService.getCoreLicenseTypeByName("Groundwater Permit");
        }
        if (groundWaterType == null) {
            throw new EntityNotFoundException("Groundwater Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id and user_account_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(groundWaterType);
        
        // Set application type to RENEWAL and original license ID
        application.setApplicationType("RENEWAL");
        if (request.get("originalLicenseId") != null) {
            application.setOriginalLicenseId(request.get("originalLicenseId").toString());
        }

        // Set application details from request (same as apply)
        if (request.get("name") != null) application.setSourceOwnerFullname(request.get("name").toString());
        if (request.get("address") != null) application.setSourceVillage(request.get("address").toString());
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
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("worksLandowner") != null)
            application.setDestOwnerFullname(request.get("worksLandowner").toString());
        if (request.get("waterUseLocation") != null)
            application.setDestVillage(request.get("waterUseLocation").toString());

        // Set initial status and step
        CoreApplicationStatus initialStatus = applicationStatusService.getCoreApplicationStatusByName("Submitted");
        if (initialStatus == null) {
            initialStatus = applicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
        }
        if (initialStatus == null) {
            initialStatus = new CoreApplicationStatus();
            initialStatus.setName("SUBMITTED");
            initialStatus.setDescription("Renewal application has been submitted");
            initialStatus = applicationStatusService.addCoreApplicationStatus(initialStatus);
        }
        application.setCoreApplicationStatus(initialStatus);

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(groundWaterType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Renewal application is under initial review");
            firstStep.setCoreLicenseType(groundWaterType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // Populate JSON fields with all form data
        try {
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("name", request.get("name"));
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("address", request.get("address"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("worksLandowner", request.get("worksLandowner"));
            clientInfo.put("waterUseLandowner", request.get("waterUseLandowner"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("gpsCoordinates", request.get("gpsCoordinates"));
            locationInfo.put("village", request.get("village"));
            locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leaseholdPlotNo", request.get("leaseholdPlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            locationInfo.put("worksLocation", request.get("worksLocation"));
            locationInfo.put("waterUseLocation", request.get("waterUseLocation"));
            locationInfo.put("plotNo", request.get("plotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "RENEWAL");
            applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterSource", request.get("waterSource"));
            formSpecificData.put("otherWaterSource", request.get("otherWaterSource"));
            formSpecificData.put("dateDrilled", request.get("dateDrilled"));
            formSpecificData.put("driller", request.get("driller"));
            formSpecificData.put("diameter", request.get("diameter"));
            formSpecificData.put("depth", request.get("depth"));
            formSpecificData.put("linningCasings", request.get("linningCasings"));
            formSpecificData.put("testYield", request.get("testYield"));
            formSpecificData.put("waterUses", request.get("waterUses"));
            formSpecificData.put("pumpType", request.get("pumpType"));
            formSpecificData.put("otherPumpType", request.get("otherPumpType"));
            formSpecificData.put("drivingMachine", request.get("drivingMachine"));
            formSpecificData.put("brakeHorsepower", request.get("brakeHorsepower"));
            formSpecificData.put("pumpElevation", request.get("pumpElevation"));
            formSpecificData.put("pumpConnection", request.get("pumpConnection"));
            formSpecificData.put("suctionMainDiameter", request.get("suctionMainDiameter"));
            formSpecificData.put("maxSuctionHeight", request.get("maxSuctionHeight"));
            formSpecificData.put("waterLiftHeight", request.get("waterLiftHeight"));
            formSpecificData.put("deliveryPipeLength", request.get("deliveryPipeLength"));
            formSpecificData.put("pumpingHours", request.get("pumpingHours"));
            formSpecificData.put("dailyPumpingQuantity", request.get("dailyPumpingQuantity"));
            formSpecificData.put("measurementMethod", request.get("measurementMethod"));
            formSpecificData.put("alternativeSources", request.get("alternativeSources"));
            formSpecificData.put("otherAlternativeSource", request.get("otherAlternativeSource"));
            formSpecificData.put("existingBoreholes", request.get("existingBoreholes"));
            formSpecificData.put("existingBoreholesCount", request.get("existingBoreholesCount"));
            formSpecificData.put("existingBoreholesDetails", request.get("existingBoreholesDetails"));
            formSpecificData.put("waterUtilityArea", request.get("waterUtilityArea"));
            formSpecificData.put("waterUtilityName", request.get("waterUtilityName"));
            formSpecificData.put("sketchMap", request.get("sketchMap"));
            formSpecificData.put("boreholeReport", request.get("boreholeReport"));

            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) &&
                        !locationInfo.containsKey(entry.getKey()) &&
                        !applicationMetadata.containsKey(entry.getKey()) &&
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
        
        // Audit log
        auditor.audit(Action.CREATE, "GroundWaterPermit", application.getId(), applicant, "Created groundwater permit renewal application");

        String applicantName = request.getOrDefault("name", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("email", applicant.getEmailAddress()).toString();

        // Create response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Groundwater permit renewal application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Groundwater Permit Renewal");
        response.put("applicationType", "RENEWAL");
        response.put("originalLicenseId", application.getOriginalLicenseId());

        // Queue email sending with renewal fees (using licenseFees)
        // Use application-specific license fee if set by manager, otherwise 0
        double renewalFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
        double applicationFees = groundWaterType.getApplicationFees() > 0 ? groundWaterType.getApplicationFees() : 2500.0;

        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, "Groundwater Permit Renewal", applicationFees + renewalFees);

        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);
        response.put("renewalFees", renewalFees);
        response.put("totalFees", applicationFees + renewalFees);

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/transfer")
    public ResponseEntity<Map<String, Object>> transferGroundWaterPermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Groundwater Permit license type
        CoreLicenseType groundWaterType = null;
        if (request.get("licenseTypeId") != null) {
            groundWaterType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (groundWaterType == null) {
            groundWaterType = licenseTypeService.getCoreLicenseTypeByName("Groundwater Permit");
        }
        if (groundWaterType == null) {
            throw new EntityNotFoundException("Groundwater Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id and user_account_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(groundWaterType);
        
        // Set application type to TRANSFER and original license ID
        application.setApplicationType("TRANSFER");
        if (request.get("originalLicenseId") != null) {
            application.setOriginalLicenseId(request.get("originalLicenseId").toString());
        }

        // Set application details from request (same as apply)
        if (request.get("name") != null) application.setSourceOwnerFullname(request.get("name").toString());
        if (request.get("address") != null) application.setSourceVillage(request.get("address").toString());
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
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("worksLandowner") != null)
            application.setDestOwnerFullname(request.get("worksLandowner").toString());
        if (request.get("waterUseLocation") != null)
            application.setDestVillage(request.get("waterUseLocation").toString());

        // Set initial status and step
        CoreApplicationStatus initialStatus = applicationStatusService.getCoreApplicationStatusByName("Submitted");
        if (initialStatus == null) {
            initialStatus = applicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
        }
        if (initialStatus == null) {
            initialStatus = new CoreApplicationStatus();
            initialStatus.setName("SUBMITTED");
            initialStatus.setDescription("Transfer application has been submitted");
            initialStatus = applicationStatusService.addCoreApplicationStatus(initialStatus);
        }
        application.setCoreApplicationStatus(initialStatus);

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(groundWaterType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Transfer application is under initial review");
            firstStep.setCoreLicenseType(groundWaterType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // Populate JSON fields with all form data
        try {
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("name", request.get("name"));
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("address", request.get("address"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("worksLandowner", request.get("worksLandowner"));
            clientInfo.put("waterUseLandowner", request.get("waterUseLandowner"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("gpsCoordinates", request.get("gpsCoordinates"));
            locationInfo.put("village", request.get("village"));
            locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leaseholdPlotNo", request.get("leaseholdPlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            locationInfo.put("worksLocation", request.get("worksLocation"));
            locationInfo.put("waterUseLocation", request.get("waterUseLocation"));
            locationInfo.put("plotNo", request.get("plotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "TRANSFER");
            applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterSource", request.get("waterSource"));
            formSpecificData.put("otherWaterSource", request.get("otherWaterSource"));
            formSpecificData.put("dateDrilled", request.get("dateDrilled"));
            formSpecificData.put("driller", request.get("driller"));
            formSpecificData.put("diameter", request.get("diameter"));
            formSpecificData.put("depth", request.get("depth"));
            formSpecificData.put("linningCasings", request.get("linningCasings"));
            formSpecificData.put("testYield", request.get("testYield"));
            formSpecificData.put("waterUses", request.get("waterUses"));
            formSpecificData.put("pumpType", request.get("pumpType"));
            formSpecificData.put("otherPumpType", request.get("otherPumpType"));
            formSpecificData.put("drivingMachine", request.get("drivingMachine"));
            formSpecificData.put("brakeHorsepower", request.get("brakeHorsepower"));
            formSpecificData.put("pumpElevation", request.get("pumpElevation"));
            formSpecificData.put("pumpConnection", request.get("pumpConnection"));
            formSpecificData.put("suctionMainDiameter", request.get("suctionMainDiameter"));
            formSpecificData.put("maxSuctionHeight", request.get("maxSuctionHeight"));
            formSpecificData.put("waterLiftHeight", request.get("waterLiftHeight"));
            formSpecificData.put("deliveryPipeLength", request.get("deliveryPipeLength"));
            formSpecificData.put("pumpingHours", request.get("pumpingHours"));
            formSpecificData.put("dailyPumpingQuantity", request.get("dailyPumpingQuantity"));
            formSpecificData.put("measurementMethod", request.get("measurementMethod"));
            formSpecificData.put("alternativeSources", request.get("alternativeSources"));
            formSpecificData.put("otherAlternativeSource", request.get("otherAlternativeSource"));
            formSpecificData.put("existingBoreholes", request.get("existingBoreholes"));
            formSpecificData.put("existingBoreholesCount", request.get("existingBoreholesCount"));
            formSpecificData.put("existingBoreholesDetails", request.get("existingBoreholesDetails"));
            formSpecificData.put("waterUtilityArea", request.get("waterUtilityArea"));
            formSpecificData.put("waterUtilityName", request.get("waterUtilityName"));
            formSpecificData.put("sketchMap", request.get("sketchMap"));
            formSpecificData.put("boreholeReport", request.get("boreholeReport"));

            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) &&
                        !locationInfo.containsKey(entry.getKey()) &&
                        !applicationMetadata.containsKey(entry.getKey()) &&
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
        
        // Audit log
        auditor.audit(Action.CREATE, "GroundWaterPermit", application.getId(), applicant, "Created groundwater permit transfer application");

        String applicantName = request.getOrDefault("name", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("email", applicant.getEmailAddress()).toString();

        // Create response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Groundwater permit transfer application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Groundwater Permit Transfer");
        response.put("applicationType", "TRANSFER");
        response.put("originalLicenseId", application.getOriginalLicenseId());

        // Queue email sending with transfer fees (using licenseFees)
        // Use application-specific license fee if set by manager, otherwise 0
        double transferFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
        double applicationFees = groundWaterType.getApplicationFees() > 0 ? groundWaterType.getApplicationFees() : 2500.0;

        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, "Groundwater Permit Transfer", applicationFees + transferFees);

        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);
        response.put("transferFees", transferFees);
        response.put("totalFees", applicationFees + transferFees);

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/vary")
    public ResponseEntity<Map<String, Object>> varyGroundWaterPermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Groundwater Permit license type
        CoreLicenseType groundWaterType = null;
        if (request.get("licenseTypeId") != null) {
            groundWaterType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (groundWaterType == null) {
            groundWaterType = licenseTypeService.getCoreLicenseTypeByName("Groundwater Permit");
        }
        if (groundWaterType == null) {
            throw new EntityNotFoundException("Groundwater Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id and user_account_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(groundWaterType);
        
        // Set application type to VARIATION and original license ID
        application.setApplicationType("VARIATION");
        if (request.get("originalLicenseId") != null) {
            application.setOriginalLicenseId(request.get("originalLicenseId").toString());
        }

        // Set application details from request (same as apply)
        if (request.get("name") != null) application.setSourceOwnerFullname(request.get("name").toString());
        if (request.get("address") != null) application.setSourceVillage(request.get("address").toString());
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
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("worksLandowner") != null)
            application.setDestOwnerFullname(request.get("worksLandowner").toString());
        if (request.get("waterUseLocation") != null)
            application.setDestVillage(request.get("waterUseLocation").toString());

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

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(groundWaterType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Variation application is under initial review");
            firstStep.setCoreLicenseType(groundWaterType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(applicationStepService.getNextStep(firstStep));

        // Populate JSON fields with all form data
        try {
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("name", request.get("name"));
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("address", request.get("address"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("worksLandowner", request.get("worksLandowner"));
            clientInfo.put("waterUseLandowner", request.get("waterUseLandowner"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("gpsCoordinates", request.get("gpsCoordinates"));
            locationInfo.put("village", request.get("village"));
            locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leaseholdPlotNo", request.get("leaseholdPlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            locationInfo.put("worksLocation", request.get("worksLocation"));
            locationInfo.put("waterUseLocation", request.get("waterUseLocation"));
            locationInfo.put("plotNo", request.get("plotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "VARIATION");
            applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterSource", request.get("waterSource"));
            formSpecificData.put("otherWaterSource", request.get("otherWaterSource"));
            formSpecificData.put("dateDrilled", request.get("dateDrilled"));
            formSpecificData.put("driller", request.get("driller"));
            formSpecificData.put("diameter", request.get("diameter"));
            formSpecificData.put("depth", request.get("depth"));
            formSpecificData.put("linningCasings", request.get("linningCasings"));
            formSpecificData.put("testYield", request.get("testYield"));
            formSpecificData.put("waterUses", request.get("waterUses"));
            formSpecificData.put("pumpType", request.get("pumpType"));
            formSpecificData.put("otherPumpType", request.get("otherPumpType"));
            formSpecificData.put("drivingMachine", request.get("drivingMachine"));
            formSpecificData.put("brakeHorsepower", request.get("brakeHorsepower"));
            formSpecificData.put("pumpElevation", request.get("pumpElevation"));
            formSpecificData.put("pumpConnection", request.get("pumpConnection"));
            formSpecificData.put("suctionMainDiameter", request.get("suctionMainDiameter"));
            formSpecificData.put("maxSuctionHeight", request.get("maxSuctionHeight"));
            formSpecificData.put("waterLiftHeight", request.get("waterLiftHeight"));
            formSpecificData.put("deliveryPipeLength", request.get("deliveryPipeLength"));
            formSpecificData.put("pumpingHours", request.get("pumpingHours"));
            formSpecificData.put("dailyPumpingQuantity", request.get("dailyPumpingQuantity"));
            formSpecificData.put("measurementMethod", request.get("measurementMethod"));
            formSpecificData.put("alternativeSources", request.get("alternativeSources"));
            formSpecificData.put("otherAlternativeSource", request.get("otherAlternativeSource"));
            formSpecificData.put("existingBoreholes", request.get("existingBoreholes"));
            formSpecificData.put("existingBoreholesCount", request.get("existingBoreholesCount"));
            formSpecificData.put("existingBoreholesDetails", request.get("existingBoreholesDetails"));
            formSpecificData.put("waterUtilityArea", request.get("waterUtilityArea"));
            formSpecificData.put("waterUtilityName", request.get("waterUtilityName"));
            formSpecificData.put("sketchMap", request.get("sketchMap"));
            formSpecificData.put("boreholeReport", request.get("boreholeReport"));

            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) &&
                        !locationInfo.containsKey(entry.getKey()) &&
                        !applicationMetadata.containsKey(entry.getKey()) &&
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
        
        // Audit log
        auditor.audit(Action.CREATE, "GroundWaterPermit", application.getId(), applicant, "Created groundwater permit variation application");

        String applicantName = request.getOrDefault("name", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("email", applicant.getEmailAddress()).toString();

        // Create response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Groundwater permit variation application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Groundwater Permit Variation");
        response.put("applicationType", "VARIATION");
        response.put("originalLicenseId", application.getOriginalLicenseId());

        // Queue email sending with variation fees (using licenseFees)
        // Use application-specific license fee if set by manager, otherwise 0
        double variationFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
        double applicationFees = groundWaterType.getApplicationFees() > 0 ? groundWaterType.getApplicationFees() : 2500.0;

        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, "Groundwater Permit Variation", applicationFees + variationFees);

        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);
        response.put("variationFees", variationFees);
        response.put("totalFees", applicationFees + variationFees);

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/{applicationId}/generate-invoice")
    public ResponseEntity<Map<String, Object>> generateInvoice(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> invoiceData) {

        try {
            // Generate invoice reference
            String invoiceReference = "INV-GW-" + System.currentTimeMillis();

            // Create invoice response
            Map<String, Object> invoice = Map.of(
                    "invoiceReference", invoiceReference,
                    "applicationId", applicationId,
                    "amount", invoiceData.getOrDefault("feeAmount", 7500.0),
                    "currency", "MWK",
                    "dueDate", invoiceData.getOrDefault("dueDate", new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)),
                    "status", "PENDING",
                    "applicantName", invoiceData.get("applicantName"),
                    "applicantEmail", invoiceData.get("applicantEmail"),
                    "licenseType", "Groundwater Permit",
                    "generatedDate", new Date()
            );

            System.out.println("Groundwater Permit Invoice generated: " + invoice);

            return ResponseEntity.ok(invoice);

        } catch (Exception e) {
            System.err.println("Error generating groundwater permit invoice: " + e.getMessage());
            throw new RuntimeException("Failed to generate invoice");
        }
    }

    /**
     * Helper method to save emergency document from base64 data
     */
    private String saveEmergencyDocumentFromBase64(String base64Data, String applicationId) throws java.io.IOException {
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
            throw new java.io.IOException("Failed to save emergency document: " + e.getMessage());
        }
    }

}
