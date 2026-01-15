package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/borehole-rehabilitation")
public class BoreholeRehabilitationController {

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
    private Auditor auditor;

    @PostMapping(path = "/apply")
    public ResponseEntity<Map<String, Object>> applyForBoreholeRehabilitation(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        System.out.println("[BOREHOLE REHABILITATION] Applicant: " + applicant.getUsername());

        // Find Borehole Rehabilitation Permit license type
        CoreLicenseType rehabilitationType = licenseTypeService.getCoreLicenseTypeByName("Borehole Rehabilitation Permit");
        if (rehabilitationType == null) {
            throw new EntityNotFoundException("Borehole Rehabilitation Permit type not found");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(rehabilitationType);

        System.out.println("[BOREHOLE REHABILITATION] Setting ownerId: " + applicant.getId());

        // Set initial status
        CoreApplicationStatus initialStatus = applicationStatusService.getCoreApplicationStatusByName("Submitted");
        if (initialStatus == null) {
            initialStatus = applicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
        }
        if (initialStatus == null) {
            initialStatus = new CoreApplicationStatus();
            initialStatus.setName("SUBMITTED");
            initialStatus.setDescription("Borehole rehabilitation application has been submitted");
            initialStatus = applicationStatusService.addCoreApplicationStatus(initialStatus);
        }
        application.setCoreApplicationStatus(initialStatus);

        // Set first step
        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(rehabilitationType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Application is under initial review");
            firstStep.setCoreLicenseType(rehabilitationType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // Set application priority and emergency fields
        if (request.get("applicationPriority") != null) {
            String priority = request.get("applicationPriority").toString();
            application.setApplicationPriority(priority);
            System.out.println("[BOREHOLE REHABILITATION] Application priority set to: " + priority);
        } else {
            application.setApplicationPriority("NORMAL");
            System.out.println("[BOREHOLE REHABILITATION] No priority provided, defaulting to NORMAL");
        }

        // Populate JSON fields with all form data
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

            // CLIENT_INFO JSON
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("name", request.get("name"));
            clientInfo.put("address", request.get("address"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            application.setClientInfo(objectMapper.writeValueAsString(clientInfo));

            // LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("licenceNumber", request.get("licenceNumber"));
            locationInfo.put("location", request.get("location"));
            application.setLocationInfo(objectMapper.writeValueAsString(locationInfo));

            // FORM_SPECIFIC_DATA JSON
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("depth", request.get("depth"));
            formSpecificData.put("casingDiameter", request.get("casingDiameter"));
            formSpecificData.put("staticWaterLevel", request.get("staticWaterLevel"));
            formSpecificData.put("casingSealingDetails", request.get("casingSealingDetails"));
            formSpecificData.put("strataDetails", request.get("strataDetails"));
            formSpecificData.put("rehabilitationMethods", request.get("rehabilitationMethods"));
            formSpecificData.put("boreholeReport", request.get("boreholeReport"));

            // Add all remaining fields (exclude emergency fields as they're stored in dedicated columns)
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) &&
                    !locationInfo.containsKey(entry.getKey()) &&
                    !formSpecificData.containsKey(entry.getKey()) &&
                    !entry.getKey().equals("licenseTypeId") &&
                    !entry.getKey().equals("applicationPriority") &&
                    !entry.getKey().equals("emergencyReason") &&
                    !entry.getKey().equals("emergencyJustificationDocument")) {
                    formSpecificData.put(entry.getKey(), entry.getValue());
                }
            }
            application.setFormSpecificData(objectMapper.writeValueAsString(formSpecificData));

            // APPLICATION_METADATA JSON
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "borehole_rehabilitation");
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("dateSubmitted", new Date().getTime());
            application.setApplicationMetadata(objectMapper.writeValueAsString(applicationMetadata));

        } catch (Exception e) {
            System.err.println("[BOREHOLE REHABILITATION] Error populating JSON fields: " + e.getMessage());
        }

        // Save application
        System.out.println("[BOREHOLE REHABILITATION] Saving application to database...");
        application = licenseApplicationService.addCoreLicenseApplication(application);
        System.out.println("[BOREHOLE REHABILITATION] Application saved with ID: " + application.getId());
        
        // Audit log
        auditor.audit(Action.CREATE, "BoreholeRehabilitation", application.getId(), applicant, "Created borehole rehabilitation application");

        // Handle emergency application fields after application is saved
        if ("EMERGENCY".equals(application.getApplicationPriority())) {
            System.out.println("[BOREHOLE REHABILITATION] Processing EMERGENCY application...");

            if (request.get("emergencyReason") != null) {
                application.setEmergencyReason(request.get("emergencyReason").toString());
                System.out.println("[BOREHOLE REHABILITATION] Emergency reason: " + application.getEmergencyReason());
            } else {
                System.out.println("[BOREHOLE REHABILITATION] WARNING: No emergency reason provided!");
            }

            // Save emergency document from base64
            if (request.get("emergencyJustificationDocument") != null) {
                try {
                    String base64File = request.get("emergencyJustificationDocument").toString();
                    System.out.println("[BOREHOLE REHABILITATION] Emergency document data length: " + base64File.length());
                    String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                    application.setEmergencyJustificationFile(savedFilePath);
                    System.out.println("[BOREHOLE REHABILITATION] Emergency document saved successfully: " + savedFilePath);
                } catch (Exception e) {
                    System.err.println("[BOREHOLE REHABILITATION] ERROR saving emergency document: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("[BOREHOLE REHABILITATION] WARNING: No emergency document provided!");
            }

            // Set emergency submission timestamp
            application.setEmergencySubmittedDate(new java.sql.Timestamp(new Date().getTime()));

            // Update the application with emergency data
            application = licenseApplicationService.editCoreLicenseApplication(application);
            System.out.println("[BOREHOLE REHABILITATION] Emergency data saved successfully");
        }

        String applicantName = request.getOrDefault("name", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("email", applicant.getEmailAddress()).toString();

        // Queue email sending
        double applicationFees = rehabilitationType.getApplicationFees() > 0 ? rehabilitationType.getApplicationFees() : 0.0;
        System.out.println("[BOREHOLE REHABILITATION] Application fees: MWK " + applicationFees);

        System.out.println("[BOREHOLE REHABILITATION] Queuing invoice email to: " + applicantEmail);
        String emailTaskId = emailQueueService.queueInvoiceEmail(
                application.getId(), applicantName, applicantEmail, "Borehole Rehabilitation Permit", applicationFees);
        System.out.println("[BOREHOLE REHABILITATION] Email queued with task ID: " + emailTaskId);

        // Return response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Borehole rehabilitation application submitted successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Borehole Rehabilitation Permit");
        response.put("emailTaskId", emailTaskId);
        response.put("applicationFees", applicationFees);

        System.out.println("[BOREHOLE REHABILITATION] Application completed successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping(path = "/{applicationId}/edit")
    public ResponseEntity<Map<String, Object>> editBoreholeRehabilitationApplication(
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

        // Update application priority and emergency fields
        if (request.get("applicationPriority") != null) {
            String priority = request.get("applicationPriority").toString();
            application.setApplicationPriority(priority);
            System.out.println("[BOREHOLE REHABILITATION] Application priority updated to: " + priority);
        }

        // Update JSON fields with all form data
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

            // Update CLIENT_INFO JSON
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("name", request.get("name"));
            clientInfo.put("address", request.get("address"));
            clientInfo.put("district", request.get("district"));
            clientInfo.put("telephone", request.get("telephone"));
            clientInfo.put("mobilePhone", request.get("mobilePhone"));
            clientInfo.put("email", request.get("email"));
            application.setClientInfo(objectMapper.writeValueAsString(clientInfo));

            // Update LOCATION_INFO JSON
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("licenceNumber", request.get("licenceNumber"));
            locationInfo.put("location", request.get("location"));
            application.setLocationInfo(objectMapper.writeValueAsString(locationInfo));

            // Update FORM_SPECIFIC_DATA JSON
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("depth", request.get("depth"));
            formSpecificData.put("casingDiameter", request.get("casingDiameter"));
            formSpecificData.put("staticWaterLevel", request.get("staticWaterLevel"));
            formSpecificData.put("casingSealingDetails", request.get("casingSealingDetails"));
            formSpecificData.put("strataDetails", request.get("strataDetails"));
            formSpecificData.put("rehabilitationMethods", request.get("rehabilitationMethods"));
            formSpecificData.put("boreholeReport", request.get("boreholeReport"));

            // Add all remaining fields (exclude emergency fields as they're stored in dedicated columns)
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) &&
                    !locationInfo.containsKey(entry.getKey()) &&
                    !formSpecificData.containsKey(entry.getKey()) &&
                    !entry.getKey().equals("licenseTypeId") &&
                    !entry.getKey().equals("applicationPriority") &&
                    !entry.getKey().equals("emergencyReason") &&
                    !entry.getKey().equals("emergencyJustificationDocument")) {
                    formSpecificData.put(entry.getKey(), entry.getValue());
                }
            }
            application.setFormSpecificData(objectMapper.writeValueAsString(formSpecificData));

            // Update APPLICATION_METADATA JSON
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", "borehole_rehabilitation");
            applicationMetadata.put("applicantName", request.get("applicantName"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("dateUpdated", new Date().getTime());
            application.setApplicationMetadata(objectMapper.writeValueAsString(applicationMetadata));

        } catch (Exception e) {
            System.err.println("Error updating JSON fields: " + e.getMessage());
        }

        // Handle emergency application fields
        if ("EMERGENCY".equals(application.getApplicationPriority())) {
            System.out.println("[BOREHOLE REHABILITATION] Processing EMERGENCY application update...");

            if (request.get("emergencyReason") != null) {
                application.setEmergencyReason(request.get("emergencyReason").toString());
                System.out.println("[BOREHOLE REHABILITATION] Emergency reason updated: " + application.getEmergencyReason());
            }

            // Save emergency document from base64 if provided
            if (request.get("emergencyJustificationDocument") != null) {
                try {
                    String base64File = request.get("emergencyJustificationDocument").toString();
                    System.out.println("[BOREHOLE REHABILITATION] Emergency document data length: " + base64File.length());
                    String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                    application.setEmergencyJustificationFile(savedFilePath);
                    System.out.println("[BOREHOLE REHABILITATION] Emergency document updated successfully: " + savedFilePath);
                } catch (Exception e) {
                    System.err.println("[BOREHOLE REHABILITATION] ERROR updating emergency document: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Update emergency submission timestamp
            if (application.getEmergencySubmittedDate() == null) {
                application.setEmergencySubmittedDate(new java.sql.Timestamp(new Date().getTime()));
            }
        } else {
            // If changed from EMERGENCY to NORMAL, clear emergency fields
            application.setEmergencyReason(null);
            application.setEmergencyJustificationFile(null);
            application.setEmergencySubmittedDate(null);
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
        auditor.audit(Action.UPDATE, "BoreholeRehabilitation", application.getId(), applicant, "Updated borehole rehabilitation application");

        String applicantName = request.getOrDefault("name", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("email", applicant.getEmailAddress()).toString();

        // Create response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", application.getCoreApplicationStatus().getName());
        response.put("message", "Borehole rehabilitation application updated successfully");
        response.put("applicationDate", application.getDateCreated());
        response.put("updatedDate", application.getDateUpdated());
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", "Borehole Rehabilitation Permit");

        return ResponseEntity.ok(response);
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

            // Return path with forward slashes for URL compatibility (Windows uses backslashes)
            String normalizedPath = filePath.toString().replace("\\", "/");
            System.out.println("[EMERGENCY DOCUMENT] Saved to: " + normalizedPath);
            return normalizedPath;

        } catch (Exception e) {
            System.err.println("[EMERGENCY DOCUMENT] Error saving file: " + e.getMessage());
            throw new java.io.IOException("Failed to save emergency document: " + e.getMessage());
        }
    }
}
