package mw.nwra.ewaterpermit.controller;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import mw.nwra.ewaterpermit.service.CoreApplicationStepService;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.service.CoreLicenseTypeService;
import mw.nwra.ewaterpermit.service.EmailQueueService;
import mw.nwra.ewaterpermit.service.ApplicationDataLinkingService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/effluent-discharge-permits")

public class EffluentDischargePermitController {

    private static final Logger log = LoggerFactory.getLogger(EffluentDischargePermitController.class);
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
    public ResponseEntity<Map<String, Object>> applyForEffluentDischargePermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Effluent Discharge Permit license type
        CoreLicenseType effluentDischargeType = null;
        if (request.get("licenseTypeId") != null) {
            effluentDischargeType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (effluentDischargeType == null) {
            effluentDischargeType = licenseTypeService.getCoreLicenseTypeByName("Effluent Discharge Permit");
        }
        if (effluentDischargeType == null) {
            throw new EntityNotFoundException("Effluent Discharge Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(effluentDischargeType);
        
        // Set application details from request
        if (request.get("clientName") != null) application.setSourceOwnerFullname(request.get("clientName").toString());
        if (request.get("clientAddress") != null) application.setSourceVillage(request.get("clientAddress").toString());
        if (request.get("district") != null) application.setSourceTa(request.get("district").toString());
        if (request.get("dischargePointCoordinates") != null) {
            String coords = request.get("dischargePointCoordinates").toString();
            String[] coordParts = coords.split(",");
            if (coordParts.length >= 2) {
                application.setSourceEasting(coordParts[0].trim());
                application.setSourceNorthing(coordParts[1].trim());
            }
        }
        if (request.get("landLocation") != null) application.setDestVillage(request.get("landLocation").toString());
        if (request.get("landDistrict") != null) application.setDestTa(request.get("landDistrict").toString());
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("landownerName") != null) application.setDestOwnerFullname(request.get("landownerName").toString());
        
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

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(effluentDischargeType);
        if (firstStep == null) {
            // Create default first step if not found
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Application is under initial review");
            firstStep.setCoreLicenseType(effluentDischargeType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // ==> POPULATE JSON FIELDS
        // WITH ALL FORM DATA
        try {
            // Populate CLIENT_INFO JSON
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("landownerName", request.get("landownerName"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            // Populate LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("dischargePointCoordinates", request.get("dischargePointCoordinates"));
            locationInfo.put("treatmentFacilityCoordinates", request.get("treatmentFacilityCoordinates"));
            locationInfo.put("landLocation", request.get("landLocation"));
            locationInfo.put("landDistrict", request.get("landDistrict"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leasePlotNo", request.get("leasePlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            // Populate APPLICATION_METADATA JSON
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "effluent_discharge");
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            // Populate FORM_SPECIFIC_DATA JSON
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("dischargeType", request.get("dischargeType"));
            formSpecificData.put("dischargeDescription", request.get("dischargeDescription"));
            formSpecificData.put("dischargeFrequency", request.get("dischargeFrequency"));
            formSpecificData.put("dischargeVolume", request.get("dischargeVolume"));
            formSpecificData.put("treatmentMethod", request.get("treatmentMethod"));
            formSpecificData.put("receivingWaterBody", request.get("receivingWaterBody"));
            formSpecificData.put("waterBodyType", request.get("waterBodyType"));
            formSpecificData.put("waterBodyName", request.get("waterBodyName"));
            formSpecificData.put("waterBodyDistance", request.get("waterBodyDistance"));
            
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

        // Save application FIRST to get ID
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Saving application to database...");
        application = licenseApplicationService.addCoreLicenseApplication(application);
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Application saved with ID: " + application.getId());
        
        // Audit log
        auditor.audit(Action.CREATE, "EffluentDischargePermit", application.getId(), applicant, "Created effluent discharge permit application");

        // Handle emergency application data AFTER saving (need application ID for file path)
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Checking for application priority in request...");
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Request contains applicationPriority: " + request.containsKey("applicationPriority"));

        boolean needsUpdate = false;

        if (request.get("applicationPriority") != null) {
            String priority = request.get("applicationPriority").toString();
            application.setApplicationPriority(priority);
            System.out.println("[EFFLUENT DISCHARGE APPLICATION] Application priority set to: " + priority);
            needsUpdate = true;
        } else {
            // Default to NORMAL if not provided
            application.setApplicationPriority("NORMAL");
            System.out.println("[EFFLUENT DISCHARGE APPLICATION] No priority provided, defaulting to NORMAL");
            needsUpdate = true;
        }

        if ("EMERGENCY".equals(application.getApplicationPriority())) {
            System.out.println("[EFFLUENT DISCHARGE APPLICATION] Processing EMERGENCY application...");

            if (request.get("emergencyReason") != null) {
                application.setEmergencyReason(request.get("emergencyReason").toString());
                System.out.println("[EFFLUENT DISCHARGE APPLICATION] Emergency reason: " + application.getEmergencyReason());
            } else {
                System.out.println("[EFFLUENT DISCHARGE APPLICATION] WARNING: No emergency reason provided!");
            }

            // Save emergency document from base64
            if (request.get("emergencyJustificationDocument") != null) {
                try {
                    String base64File = request.get("emergencyJustificationDocument").toString();
                    System.out.println("[EFFLUENT DISCHARGE APPLICATION] Emergency document data length: " + base64File.length());
                    String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                    application.setEmergencyJustificationFile(savedFilePath);
                    System.out.println("[EFFLUENT DISCHARGE APPLICATION] Emergency document saved successfully: " + savedFilePath);
                } catch (java.io.IOException e) {
                    System.err.println("[EFFLUENT DISCHARGE APPLICATION] ERROR saving emergency document: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("[EFFLUENT DISCHARGE APPLICATION] WARNING: No emergency document provided!");
            }

            application.setEmergencySubmittedDate(new Timestamp(System.currentTimeMillis()));
            System.out.println("[EFFLUENT DISCHARGE APPLICATION] Emergency submitted date: " + application.getEmergencySubmittedDate());
        }

        // Update application with priority/emergency data
        if (needsUpdate) {
            System.out.println("[EFFLUENT DISCHARGE APPLICATION] Updating application with priority data...");
            application = licenseApplicationService.editCoreLicenseApplication(application);
            System.out.println("[EFFLUENT DISCHARGE APPLICATION] Application updated successfully");
            System.out.println("[EFFLUENT DISCHARGE APPLICATION] Final priority: " + application.getApplicationPriority());
            System.out.println("[EFFLUENT DISCHARGE APPLICATION] Final emergency file: " + application.getEmergencyJustificationFile());
        }
        
        // Link water resource unit and water use data
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Starting data linking process...");
        applicationDataLinkingService.linkWaterResourceUnit(application, request);
        applicationDataLinkingService.linkWaterUse(application, request);
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Data linking completed");

        // Get applicant details
        String applicantName = request.get("clientName") != null ? 
                request.get("clientName").toString() : 
                applicant.getFirstName() + " " + applicant.getLastName();
        String applicantEmail = request.get("email") != null ? 
                request.get("email").toString() : 
                applicant.getEmailAddress();
        
        // Queue email with application fees
        double applicationFees = effluentDischargeType.getApplicationFees() > 0 ? 
                effluentDischargeType.getApplicationFees() : 6000.0;
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Application fees: MWK " + applicationFees);

        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Queuing invoice email to: " + applicantEmail);
        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, 
                effluentDischargeType.getName(), applicationFees);
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Email queued with task ID: " + emailTaskId);
        
        // Prepare response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", "SUBMITTED");
        response.put("message", "Effluent discharge permit application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", effluentDischargeType.getName());
        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);
        
        System.out.println("[EFFLUENT DISCHARGE APPLICATION] Application completed successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping(path = "/{applicationId}/edit")
    public ResponseEntity<Map<String, Object>> editEffluentDischargePermitApplication(
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
        if (request.get("clientName") != null) application.setSourceOwnerFullname(request.get("clientName").toString());
        if (request.get("clientAddress") != null) application.setSourceVillage(request.get("clientAddress").toString());
        if (request.get("district") != null) application.setSourceTa(request.get("district").toString());
        if (request.get("dischargePointCoordinates") != null) {
            String coords = request.get("dischargePointCoordinates").toString();
            String[] coordParts = coords.split(",");
            if (coordParts.length >= 2) {
                application.setSourceEasting(coordParts[0].trim());
                application.setSourceNorthing(coordParts[1].trim());
            }
        }
        if (request.get("landLocation") != null) application.setDestVillage(request.get("landLocation").toString());
        if (request.get("landDistrict") != null) application.setDestTa(request.get("landDistrict").toString());
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("landownerName") != null) application.setDestOwnerFullname(request.get("landownerName").toString());

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
            clientInfo.put("landownerName", request.get("landownerName"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            // Update LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("dischargePointCoordinates", request.get("dischargePointCoordinates"));
            locationInfo.put("treatmentFacilityCoordinates", request.get("treatmentFacilityCoordinates"));
            locationInfo.put("landLocation", request.get("landLocation"));
            locationInfo.put("landDistrict", request.get("landDistrict"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leasePlotNo", request.get("leasePlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
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
            formSpecificData.put("dischargeType", request.get("dischargeType"));
            formSpecificData.put("dischargeDescription", request.get("dischargeDescription"));
            formSpecificData.put("dischargeFrequency", request.get("dischargeFrequency"));
            formSpecificData.put("dischargeVolume", request.get("dischargeVolume"));
            formSpecificData.put("treatmentMethod", request.get("treatmentMethod"));
            formSpecificData.put("receivingWaterBody", request.get("receivingWaterBody"));
            formSpecificData.put("waterBodyType", request.get("waterBodyType"));
            formSpecificData.put("waterBodyName", request.get("waterBodyName"));
            formSpecificData.put("waterBodyDistance", request.get("waterBodyDistance"));
            
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
        
        // Audit log
        auditor.audit(Action.UPDATE, "EffluentDischargePermit", application.getId(), applicant, "Updated effluent discharge permit application");

        // Prepare response
        String applicantName = request.get("clientName") != null ? 
                request.get("clientName").toString() : 
                applicant.getFirstName() + " " + applicant.getLastName();
        String applicantEmail = request.get("email") != null ? 
                request.get("email").toString() : 
                applicant.getEmailAddress();
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Effluent discharge permit application updated successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("updatedDate", application.getDateUpdated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Effluent Discharge Permit");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/renew")
    public ResponseEntity<Map<String, Object>> renewEffluentDischargePermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Effluent Discharge Permit license type
        CoreLicenseType effluentDischargeType = null;
        if (request.get("licenseTypeId") != null) {
            effluentDischargeType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (effluentDischargeType == null) {
            effluentDischargeType = licenseTypeService.getCoreLicenseTypeByName("Renewal Effluent Discharge Permit");
        }
        if (effluentDischargeType == null) {
            throw new EntityNotFoundException("Effluent Discharge Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(effluentDischargeType);
        
        // Set application type to RENEWAL and original license ID
        application.setApplicationType("RENEWAL");
        if (request.get("originalLicenseId") != null) {
            application.setOriginalLicenseId(request.get("originalLicenseId").toString());
        }
        
        // Set application details from request (same as apply)
        if (request.get("clientName") != null) application.setSourceOwnerFullname(request.get("clientName").toString());
        if (request.get("clientAddress") != null) application.setSourceVillage(request.get("clientAddress").toString());
        if (request.get("district") != null) application.setSourceTa(request.get("district").toString());
        if (request.get("dischargePointCoordinates") != null) {
            String coords = request.get("dischargePointCoordinates").toString();
            String[] coordParts = coords.split(",");
            if (coordParts.length >= 2) {
                application.setSourceEasting(coordParts[0].trim());
                application.setSourceNorthing(coordParts[1].trim());
            }
        }
        if (request.get("landLocation") != null) application.setDestVillage(request.get("landLocation").toString());
        if (request.get("landDistrict") != null) application.setDestTa(request.get("landDistrict").toString());
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("landownerName") != null) application.setDestOwnerFullname(request.get("landownerName").toString());
        
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

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(effluentDischargeType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Renewal application is under initial review");
            firstStep.setCoreLicenseType(effluentDischargeType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // Populate JSON fields with all form data
        try {
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("landownerName", request.get("landownerName"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("dischargePointCoordinates", request.get("dischargePointCoordinates"));
            locationInfo.put("treatmentFacilityCoordinates", request.get("treatmentFacilityCoordinates"));
            locationInfo.put("landLocation", request.get("landLocation"));
            locationInfo.put("landDistrict", request.get("landDistrict"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leasePlotNo", request.get("leasePlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "RENEWAL");
            applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("dischargeType", request.get("dischargeType"));
            formSpecificData.put("dischargeDescription", request.get("dischargeDescription"));
            formSpecificData.put("dischargeFrequency", request.get("dischargeFrequency"));
            formSpecificData.put("dischargeVolume", request.get("dischargeVolume"));
            formSpecificData.put("treatmentMethod", request.get("treatmentMethod"));
            formSpecificData.put("receivingWaterBody", request.get("receivingWaterBody"));
            formSpecificData.put("waterBodyType", request.get("waterBodyType"));
            formSpecificData.put("waterBodyName", request.get("waterBodyName"));
            formSpecificData.put("waterBodyDistance", request.get("waterBodyDistance"));
            
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
        
        // Audit log
        auditor.audit(Action.CREATE, "EffluentDischargePermit", application.getId(), applicant, "Created effluent discharge permit renewal application");

        // Prepare response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", "SUBMITTED");
        response.put("message", "Effluent discharge permit renewal application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        
        String applicantName = request.get("clientName") != null ? 
                request.get("clientName").toString() : 
                applicant.getFirstName() + " " + applicant.getLastName();
        String applicantEmail = request.get("email") != null ? 
                request.get("email").toString() : 
                applicant.getEmailAddress();
        
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Effluent Discharge Permit Renewal");
        response.put("applicationType", "RENEWAL");
        response.put("originalLicenseId", application.getOriginalLicenseId());
        
        // Queue email sending with renewal fees (using licenseFees)
        // Use application-specific license fee if set by manager, otherwise 0
        double renewalFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
        double applicationFees = effluentDischargeType.getApplicationFees() > 0 ? effluentDischargeType.getApplicationFees() : 6000.0;
        
        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, 
                "Effluent Discharge Permit Renewal", applicationFees + renewalFees);
        
        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);
        response.put("renewalFees", renewalFees);
        response.put("totalFees", applicationFees + renewalFees);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/transfer")
    public ResponseEntity<Map<String, Object>> transferEffluentDischargePermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Effluent Discharge Permit license type
        CoreLicenseType effluentDischargeType = null;
        if (request.get("licenseTypeId") != null) {
            effluentDischargeType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (effluentDischargeType == null) {
            effluentDischargeType = licenseTypeService.getCoreLicenseTypeByName("Effluent Discharge Permit");
        }
        if (effluentDischargeType == null) {
            throw new EntityNotFoundException("Effluent Discharge Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(effluentDischargeType);
        
        // Set application type to TRANSFER and original license ID
        application.setApplicationType("TRANSFER");
        if (request.get("originalLicenseId") != null) {
            application.setOriginalLicenseId(request.get("originalLicenseId").toString());
        }
        
        // Set application details from request (same as apply)
        if (request.get("clientName") != null) application.setSourceOwnerFullname(request.get("clientName").toString());
        if (request.get("clientAddress") != null) application.setSourceVillage(request.get("clientAddress").toString());
        if (request.get("district") != null) application.setSourceTa(request.get("district").toString());
        if (request.get("dischargePointCoordinates") != null) {
            String coords = request.get("dischargePointCoordinates").toString();
            String[] coordParts = coords.split(",");
            if (coordParts.length >= 2) {
                application.setSourceEasting(coordParts[0].trim());
                application.setSourceNorthing(coordParts[1].trim());
            }
        }
        if (request.get("landLocation") != null) application.setDestVillage(request.get("landLocation").toString());
        if (request.get("landDistrict") != null) application.setDestTa(request.get("landDistrict").toString());
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("landownerName") != null) application.setDestOwnerFullname(request.get("landownerName").toString());
        
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

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(effluentDischargeType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Transfer application is under initial review");
            firstStep.setCoreLicenseType(effluentDischargeType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // Populate JSON fields with all form data
        try {
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("landownerName", request.get("landownerName"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("dischargePointCoordinates", request.get("dischargePointCoordinates"));
            locationInfo.put("treatmentFacilityCoordinates", request.get("treatmentFacilityCoordinates"));
            locationInfo.put("landLocation", request.get("landLocation"));
            locationInfo.put("landDistrict", request.get("landDistrict"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leasePlotNo", request.get("leasePlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "TRANSFER");
            applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
            applicationMetadata.put("applicationFor", request.get("applicationFor"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("permitDuration", request.get("permitDuration"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("dischargeType", request.get("dischargeType"));
            formSpecificData.put("dischargeDescription", request.get("dischargeDescription"));
            formSpecificData.put("dischargeFrequency", request.get("dischargeFrequency"));
            formSpecificData.put("dischargeVolume", request.get("dischargeVolume"));
            formSpecificData.put("treatmentMethod", request.get("treatmentMethod"));
            formSpecificData.put("receivingWaterBody", request.get("receivingWaterBody"));
            formSpecificData.put("waterBodyType", request.get("waterBodyType"));
            formSpecificData.put("waterBodyName", request.get("waterBodyName"));
            formSpecificData.put("waterBodyDistance", request.get("waterBodyDistance"));
            
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
        
        // Audit log
        auditor.audit(Action.CREATE, "EffluentDischargePermit", application.getId(), applicant, "Created effluent discharge permit transfer application");

        // Prepare response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", "SUBMITTED");
        response.put("message", "Effluent discharge permit transfer application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        
        String applicantName = request.get("clientName") != null ? 
                request.get("clientName").toString() : 
                applicant.getFirstName() + " " + applicant.getLastName();
        String applicantEmail = request.get("email") != null ? 
                request.get("email").toString() : 
                applicant.getEmailAddress();
        
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Effluent Discharge Permit Transfer");
        response.put("applicationType", "TRANSFER");
        response.put("originalLicenseId", application.getOriginalLicenseId());
        
        // Queue email sending with transfer fees (using licenseFees)
        // Use application-specific license fee if set by manager, otherwise 0
        double transferFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
        double applicationFees = effluentDischargeType.getApplicationFees() > 0 ? effluentDischargeType.getApplicationFees() : 6000.0;
        
        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, 
                "Effluent Discharge Permit Transfer", applicationFees + transferFees);
        
        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);
        response.put("transferFees", transferFees);
        response.put("totalFees", applicationFees + transferFees);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/vary")
    public ResponseEntity<Map<String, Object>> varyEffluentDischargePermit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find Effluent Discharge Permit license type
        CoreLicenseType effluentDischargeType = null;
        if (request.get("licenseTypeId") != null) {
            effluentDischargeType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (effluentDischargeType == null) {
            effluentDischargeType = licenseTypeService.getCoreLicenseTypeByName("Effluent Discharge Permit");
        }
        if (effluentDischargeType == null) {
            throw new EntityNotFoundException("Effluent Discharge Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        
        // Set owner_id to current user (applicant)
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(effluentDischargeType);
        
        // Set application type to VARIATION and original license ID
        application.setApplicationType("VARIATION");
        if (request.get("originalLicenseId") != null) {
            application.setOriginalLicenseId(request.get("originalLicenseId").toString());
        }
        
        // Set application details from request (same as apply)
        if (request.get("clientName") != null) application.setSourceOwnerFullname(request.get("clientName").toString());
        if (request.get("clientAddress") != null) application.setSourceVillage(request.get("clientAddress").toString());
        if (request.get("district") != null) application.setSourceTa(request.get("district").toString());
        if (request.get("dischargePointCoordinates") != null) {
            String coords = request.get("dischargePointCoordinates").toString();
            String[] coordParts = coords.split(",");
            if (coordParts.length >= 2) {
                application.setSourceEasting(coordParts[0].trim());
                application.setSourceNorthing(coordParts[1].trim());
            }
        }
        if (request.get("landLocation") != null) application.setDestVillage(request.get("landLocation").toString());
        if (request.get("landDistrict") != null) application.setDestTa(request.get("landDistrict").toString());
        if (request.get("landArea") != null) application.setSourceHectarage(request.get("landArea").toString());
        if (request.get("landownerName") != null) application.setDestOwnerFullname(request.get("landownerName").toString());
        
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

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(effluentDischargeType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Variation application is under initial review");
            firstStep.setCoreLicenseType(effluentDischargeType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(applicationStepService.getNextStep(firstStep));

        // Populate JSON fields with all form data
        try {
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            clientInfo.put("landownerName", request.get("landownerName"));
            clientInfo.put("landownerAddress", request.get("landownerAddress"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("dischargePointCoordinates", request.get("dischargePointCoordinates"));
            locationInfo.put("treatmentFacilityCoordinates", request.get("treatmentFacilityCoordinates"));
            locationInfo.put("landLocation", request.get("landLocation"));
            locationInfo.put("landDistrict", request.get("landDistrict"));
            locationInfo.put("landArea", request.get("landArea"));
            locationInfo.put("propertyRegime", request.get("propertyRegime"));
            locationInfo.put("leasePlotNo", request.get("leasePlotNo"));
            locationInfo.put("freeholdPlotNo", request.get("freeholdPlotNo"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

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

            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("dischargeType", request.get("dischargeType"));
            formSpecificData.put("dischargeDescription", request.get("dischargeDescription"));
            formSpecificData.put("dischargeFrequency", request.get("dischargeFrequency"));
            formSpecificData.put("dischargeVolume", request.get("dischargeVolume"));
            formSpecificData.put("treatmentMethod", request.get("treatmentMethod"));
            formSpecificData.put("receivingWaterBody", request.get("receivingWaterBody"));
            formSpecificData.put("waterBodyType", request.get("waterBodyType"));
            formSpecificData.put("waterBodyName", request.get("waterBodyName"));
            formSpecificData.put("waterBodyDistance", request.get("waterBodyDistance"));
            
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
        
        // Audit log
        auditor.audit(Action.CREATE, "EffluentDischargePermit", application.getId(), applicant, "Created effluent discharge permit variation application");

        // Prepare response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", "SUBMITTED");
        response.put("message", "Effluent discharge permit variation application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        
        String applicantName = request.get("clientName") != null ? 
                request.get("clientName").toString() : 
                applicant.getFirstName() + " " + applicant.getLastName();
        String applicantEmail = request.get("email") != null ? 
                request.get("email").toString() : 
                applicant.getEmailAddress();
        
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Effluent Discharge Permit Variation");
        response.put("applicationType", "VARIATION");
        response.put("originalLicenseId", application.getOriginalLicenseId());
        
        // Queue email sending with variation fees (using licenseFees)
        // Use application-specific license fee if set by manager, otherwise 0
        double variationFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
        double applicationFees = effluentDischargeType.getApplicationFees() > 0 ? effluentDischargeType.getApplicationFees() : 6000.0;
        
        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, 
                "Effluent Discharge Permit Variation", applicationFees + variationFees);
        
        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);
        response.put("variationFees", variationFees);
        response.put("totalFees", applicationFees + variationFees);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to save emergency document from base64 data
     */
    private String saveEmergencyDocumentFromBase64(String base64Data, String applicationId) throws java.io.IOException {
        try {
            // Remove data URL prefix if present
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

            // Determine file extension
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

            // Generate filename and save
            String filename = "emergency_" + java.util.UUID.randomUUID().toString() + fileExtension;
            java.nio.file.Path filePath = uploadPath.resolve(filename);
            java.nio.file.Files.write(filePath, fileBytes);

            System.out.println("[EMERGENCY DOCUMENT] Saved to: " + filePath.toString());
            return filePath.toString();

        } catch (Exception e) {
            System.err.println("[EMERGENCY DOCUMENT] Error saving file: " + e.getMessage());
            throw new java.io.IOException("Failed to save emergency document: " + e.getMessage());
        }
    }
}
