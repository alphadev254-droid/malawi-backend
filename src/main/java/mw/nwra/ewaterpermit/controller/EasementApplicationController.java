package mw.nwra.ewaterpermit.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Base64;
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
@RequestMapping(value = "/v1/easement-applications")
public class EasementApplicationController {

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
    public ResponseEntity<Map<String, Object>> applyForEasement(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        System.out.println("[EASEMENT APPLICATION] ========== NEW APPLICATION STARTED ==========");
        System.out.println("[EASEMENT APPLICATION] Endpoint hit: POST /v1/easement-applications/apply");
        System.out.println("[EASEMENT APPLICATION] Request payload size: " + request.size() + " fields");

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            System.out.println("[EASEMENT APPLICATION] ERROR: User not authenticated");
            throw new ForbiddenException("User not authenticated");
        }

        System.out.println("[EASEMENT APPLICATION] Authenticated user: " + applicant.getUsername() + " (ID: " + applicant.getId() + ")");

        // Find Easement license type
        CoreLicenseType easementType = null;
        if (request.get("licenseTypeId") != null) {
            easementType = licenseTypeService.getCoreLicenseTypeById(request.get("licenseTypeId").toString());
        }
        if (easementType == null) {
            easementType = licenseTypeService.getCoreLicenseTypeByName("Easement Permit");
            if (easementType == null) {
                easementType = licenseTypeService.getCoreLicenseTypeByName("Easement");
            }
            if (easementType == null) {
                easementType = licenseTypeService.getCoreLicenseTypeByName("Water Easement");
            }
        }
        if (easementType == null) {
            throw new EntityNotFoundException("Easement license type not found. Please create an 'Easement Permit' license type in the system.");
        }

        // Create new application
        CoreLicenseApplication application = new CoreLicenseApplication();
        application.setDateCreated(new Timestamp(new Date().getTime()));
        application.setSysUserAccount(applicant);
        application.setOwnerId(applicant.getId());
        application.setUserAccountId(applicant.getId());
        application.setCoreLicenseType(easementType);

        // Set basic details
        if (request.get("clientName") != null) application.setSourceOwnerFullname(request.get("clientName").toString());
        if (request.get("clientDistrict") != null) application.setSourceTa(request.get("clientDistrict").toString());
        if (request.get("waterSourceCoordinates") != null) {
            String coords = request.get("waterSourceCoordinates").toString();
            String[] coordParts = coords.split(",");
            if (coordParts.length >= 2) {
                application.setSourceEasting(coordParts[0].trim());
                application.setSourceNorthing(coordParts[1].trim());
            }
        }
        if (request.get("easementLandLocation") != null) application.setSourceVillage(request.get("easementLandLocation").toString());
        if (request.get("easementLandArea") != null) application.setSourceHectarage(request.get("easementLandArea").toString());

        // Set initial status
        CoreApplicationStatus initialStatus = applicationStatusService.getCoreApplicationStatusByName("Submitted");
        if (initialStatus == null) {
            initialStatus = applicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
        }
        if (initialStatus == null) {
            initialStatus = new CoreApplicationStatus();
            initialStatus.setName("SUBMITTED");
            initialStatus.setDescription("Application has been submitted");
            initialStatus = applicationStatusService.addCoreApplicationStatus(initialStatus);
        }
        application.setCoreApplicationStatus(initialStatus);

        CoreApplicationStep firstStep = applicationStepService.getFirstStepByLicenseType(easementType);
        if (firstStep == null) {
            firstStep = new CoreApplicationStep();
            firstStep.setName("Initial Review");
            firstStep.setDescription("Application is under initial review");
            firstStep.setCoreLicenseType(easementType);
            firstStep = applicationStepService.addCoreApplicationStep(firstStep);
        }
        application.setCoreApplicationStep(firstStep);

        // Populate JSON fields
        try {
            // CLIENT_INFO
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("clientDistrict", request.get("clientDistrict"));
            clientInfo.put("clientTelephone", request.get("clientTelephone"));
            clientInfo.put("clientMobile", request.get("clientMobile"));
            clientInfo.put("clientEmail", request.get("clientEmail"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            // LOCATION_INFO
            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("benefitingLandOwnerName", request.get("benefitingLandOwnerName"));
            locationInfo.put("benefitingOwnerAddress", request.get("benefitingOwnerAddress"));
            locationInfo.put("benefitingLandDistrict", request.get("benefitingLandDistrict"));
            locationInfo.put("benefitingPropertyRegime", request.get("benefitingPropertyRegime"));
            locationInfo.put("benefitingPlotNo", request.get("benefitingPlotNo"));
            locationInfo.put("easementLandLocation", request.get("easementLandLocation"));
            locationInfo.put("easementLandDistrict", request.get("easementLandDistrict"));
            locationInfo.put("easementLandArea", request.get("easementLandArea"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            // APPLICATION_METADATA
            Map<String, Object> applicationMetadata = new java.util.HashMap<>();
            applicationMetadata.put("applicationType", request.getOrDefault("applicationType", "easement"));
            applicationMetadata.put("declarationDate", request.get("declarationDate"));
            applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
            applicationMetadata.put("applicantFullNames", request.get("applicantFullNames"));
            applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
            application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

            // FORM_SPECIFIC_DATA
            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            formSpecificData.put("waterSourceType", request.get("waterSourceType"));
            formSpecificData.put("waterSourceOther", request.get("waterSourceOther"));
            formSpecificData.put("waterSourceName", request.get("waterSourceName"));
            formSpecificData.put("waterSourceCoordinates", request.get("waterSourceCoordinates"));
            formSpecificData.put("waterUptakeDistrict", request.get("waterUptakeDistrict"));
            formSpecificData.put("existingPermitDetails", request.get("existingPermitDetails"));
            formSpecificData.put("topographicMap", request.get("topographicMap"));
            formSpecificData.put("easementReason", request.get("easementReason"));
            formSpecificData.put("easementForm", request.get("easementForm"));
            formSpecificData.put("easementWorks", request.get("easementWorks"));
            formSpecificData.put("easementDuration", request.get("easementDuration"));
            formSpecificData.put("titleCertificateNumber", request.get("titleCertificateNumber"));
            formSpecificData.put("affectedPeopleNames", request.get("affectedPeopleNames"));
            formSpecificData.put("peopleRefused", request.get("peopleRefused"));
            formSpecificData.put("relevantCorrespondence", request.get("relevantCorrespondence"));
            formSpecificData.put("documentsList", request.get("documentsList"));
            formSpecificData.put("supportingDocuments", request.get("supportingDocuments"));

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
            System.err.println("[EASEMENT APPLICATION] Error populating JSON fields: " + e.getMessage());
            e.printStackTrace();
        }

        // Set application priority
        if (request.get("applicationPriority") != null) {
            String priority = request.get("applicationPriority").toString();
            application.setApplicationPriority(priority);
            System.out.println("[EASEMENT APPLICATION] Application priority set to: " + priority);
        } else {
            application.setApplicationPriority("NORMAL");
            System.out.println("[EASEMENT APPLICATION] No priority provided, defaulting to NORMAL");
        }

        // Save application
        System.out.println("[EASEMENT APPLICATION] Saving application to database...");
        application = licenseApplicationService.addCoreLicenseApplication(application);
        System.out.println("[EASEMENT APPLICATION] Application saved with ID: " + application.getId());
        
        // Audit log
        auditor.audit(Action.CREATE, "EasementApplication", application.getId(), applicant, "Created easement application");

        // Handle emergency application fields after application is saved
        if ("EMERGENCY".equals(application.getApplicationPriority())) {
            System.out.println("[EASEMENT APPLICATION] Processing EMERGENCY application...");
            System.out.println("[EASEMENT APPLICATION] Emergency reason from request: " + request.get("emergencyReason"));
            System.out.println("[EASEMENT APPLICATION] Emergency document from request: " + (request.get("emergencyJustificationDocument") != null ? "Present" : "Missing"));

            if (request.get("emergencyReason") != null) {
                String emergencyReason = request.get("emergencyReason").toString();
                application.setEmergencyReason(emergencyReason);
                System.out.println("[EASEMENT APPLICATION] Emergency reason set: " + emergencyReason);
            } else {
                System.out.println("[EASEMENT APPLICATION] WARNING: No emergency reason provided!");
            }

            // Save emergency document from base64
            if (request.get("emergencyJustificationDocument") != null) {
                try {
                    String base64File = request.get("emergencyJustificationDocument").toString();
                    String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                    application.setEmergencyJustificationFile(savedFilePath);
                    System.out.println("[EASEMENT APPLICATION] Emergency document saved successfully");
                } catch (Exception e) {
                    System.err.println("[EASEMENT APPLICATION] ERROR saving emergency document: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("[EASEMENT APPLICATION] WARNING: No emergency document provided!");
            }

            // Set emergency submission timestamp
            application.setEmergencySubmittedDate(new java.sql.Timestamp(new Date().getTime()));
            System.out.println("[EASEMENT APPLICATION] Emergency submitted date set: " + application.getEmergencySubmittedDate());

            // Debug: Check values before update
            System.out.println("[EASEMENT APPLICATION] Before update - Priority: " + application.getApplicationPriority());
            System.out.println("[EASEMENT APPLICATION] Before update - Emergency reason: " + application.getEmergencyReason());
            System.out.println("[EASEMENT APPLICATION] Before update - Emergency file: " + application.getEmergencyJustificationFile());
            System.out.println("[EASEMENT APPLICATION] Before update - Emergency date: " + application.getEmergencySubmittedDate());

            // Update the application with emergency data
            try {
                application = licenseApplicationService.editCoreLicenseApplication(application);
                System.out.println("[EASEMENT APPLICATION] Emergency data saved successfully to database");
                
                // Verify the data was saved
                CoreLicenseApplication savedApp = licenseApplicationService.getCoreLicenseApplicationById(application.getId());
                System.out.println("[EASEMENT APPLICATION] Verification - Priority: " + savedApp.getApplicationPriority());
                System.out.println("[EASEMENT APPLICATION] Verification - Emergency reason: " + savedApp.getEmergencyReason());
                System.out.println("[EASEMENT APPLICATION] Verification - Emergency file: " + savedApp.getEmergencyJustificationFile());
                System.out.println("[EASEMENT APPLICATION] Verification - Emergency date: " + savedApp.getEmergencySubmittedDate());
            } catch (Exception e) {
                System.err.println("[EASEMENT APPLICATION] ERROR updating application with emergency data: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Link data
        applicationDataLinkingService.linkWaterResourceUnit(application, request);
        applicationDataLinkingService.linkWaterUse(application, request);

        String applicantName = request.getOrDefault("clientName", applicant.getFirstName() + " " + applicant.getLastName()).toString();
        String applicantEmail = request.getOrDefault("clientEmail", applicant.getEmailAddress()).toString();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", application.getId());
        response.put("status", "success");
        response.put("message", "Easement application submitted successfully");
        response.put("applicantName", applicantName);
        response.put("applicantEmail", applicantEmail);
        response.put("licenseType", easementType.getName());

        try {
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, easementType.getName(), 
                    easementType.getApplicationFees() > 0 ? easementType.getApplicationFees() : 5000.0);
            response.put("emailTaskId", emailTaskId);
        } catch (Exception e) {
            System.err.println("[EASEMENT APPLICATION] Error queuing email: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/renew")
    public ResponseEntity<Map<String, Object>> renewEasement(@RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {
        request.put("applicationType", "RENEWAL");
        return applyForEasement(request, token);
    }

    @PostMapping(path = "/vary")
    public ResponseEntity<Map<String, Object>> varyEasement(@RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {
        request.put("applicationType", "VARIATION");
        return applyForEasement(request, token);
    }

    @PutMapping(path = "/{id}/edit")
    public ResponseEntity<Map<String, Object>> updateEasement(@PathVariable String id,
            @RequestBody Map<String, Object> request, @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) throw new ForbiddenException("User not authenticated");

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(id);
        if (application == null) throw new EntityNotFoundException("Easement application not found");

        // Update priority if provided
        if (request.get("applicationPriority") != null) {
            application.setApplicationPriority(request.get("applicationPriority").toString());
        }

        try {
            Map<String, Object> clientInfo = new java.util.HashMap<>();
            clientInfo.put("clientName", request.get("clientName"));
            clientInfo.put("clientAddress", request.get("clientAddress"));
            clientInfo.put("clientDistrict", request.get("clientDistrict"));
            clientInfo.put("clientTelephone", request.get("clientTelephone"));
            clientInfo.put("clientMobile", request.get("clientMobile"));
            clientInfo.put("clientEmail", request.get("clientEmail"));
            application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

            Map<String, Object> locationInfo = new java.util.HashMap<>();
            locationInfo.put("benefitingLandOwnerName", request.get("benefitingLandOwnerName"));
            locationInfo.put("easementLandLocation", request.get("easementLandLocation"));
            locationInfo.put("easementLandArea", request.get("easementLandArea"));
            application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

            Map<String, Object> formSpecificData = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : request.entrySet()) {
                if (!clientInfo.containsKey(entry.getKey()) && !locationInfo.containsKey(entry.getKey()) &&
                        !entry.getKey().equals("applicationPriority")) {
                    formSpecificData.put(entry.getKey(), entry.getValue());
                }
            }
            application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
        } catch (Exception e) {
            System.err.println("Error updating JSON: " + e.getMessage());
        }

        application.setDateUpdated(new Timestamp(new Date().getTime()));
        application = licenseApplicationService.editCoreLicenseApplication(application);
        auditor.audit(Action.UPDATE, "EasementApplication", id, user, "Updated easement application");

        return ResponseEntity.ok(Map.of("id", id, "status", "success"));
    }

    @GetMapping(path = "/my-applications")
    public ResponseEntity<List<CoreLicenseApplication>> getMyEasementApplications(
            @RequestHeader(name = "Authorization") String token) {
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) throw new ForbiddenException("User not authenticated");

        List<CoreLicenseApplication> applications = licenseApplicationService.getCoreLicenseApplicationsByUser(user.getId());
        applications.removeIf(app -> app.getCoreLicenseType() == null ||
                !app.getCoreLicenseType().getName().toLowerCase().contains("easement"));
        return ResponseEntity.ok(applications);
    }

    @GetMapping(path = "/all")
    public ResponseEntity<List<CoreLicenseApplication>> getAllEasementApplications(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int limit,
            @RequestHeader(name = "Authorization") String token) {
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) throw new ForbiddenException("User not authenticated");

        List<CoreLicenseApplication> applications = licenseApplicationService.getAllCoreLicenseApplications(page, limit);
        applications.removeIf(app -> app.getCoreLicenseType() == null ||
                !app.getCoreLicenseType().getName().toLowerCase().contains("easement"));
        return ResponseEntity.ok(applications);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<CoreLicenseApplication> getEasementById(@PathVariable String id,
            @RequestHeader(name = "Authorization") String token) {
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) throw new ForbiddenException("User not authenticated");

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(id);
        if (application == null) throw new EntityNotFoundException("Easement application not found");
        return ResponseEntity.ok(application);
    }

    @GetMapping(path = "/{id}/status")
    public ResponseEntity<Map<String, Object>> getEasementStatus(@PathVariable String id,
            @RequestHeader(name = "Authorization") String token) {
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) throw new ForbiddenException("User not authenticated");

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(id);
        if (application == null) throw new EntityNotFoundException("Easement application not found");

        Map<String, Object> status = new java.util.HashMap<>();
        status.put("applicationId", application.getId());
        status.put("status", application.getCoreApplicationStatus() != null ?
                application.getCoreApplicationStatus().getName() : "Unknown");
        status.put("currentStep", application.getCoreApplicationStep() != null ?
                application.getCoreApplicationStep().getName() : "Unknown");
        return ResponseEntity.ok(status);
    }

    private String saveEmergencyDocumentFromBase64(String base64Data, String applicationId) throws Exception {
        String fileExtension = "pdf";
        if (base64Data.contains("image/jpeg") || base64Data.contains("image/jpg")) fileExtension = "jpg";
        else if (base64Data.contains("image/png")) fileExtension = "png";

        String base64Content = base64Data.contains(",") ? base64Data.substring(base64Data.indexOf(",") + 1) : base64Data;
        byte[] fileBytes = Base64.getDecoder().decode(base64Content);

        String uploadDir = "uploads/emergency-documents/";
        Files.createDirectories(Paths.get(uploadDir));
        String filename = "emergency_" + applicationId + "_" + System.currentTimeMillis() + "." + fileExtension;
        String filePath = uploadDir + filename;

        try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
            fos.write(fileBytes);
        }
        return filePath;
    }
}
