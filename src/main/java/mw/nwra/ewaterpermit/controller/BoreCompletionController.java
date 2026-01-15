package mw.nwra.ewaterpermit.controller;

import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.*;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.sql.Timestamp;
import java.util.Date;

@RestController
@RequestMapping("/v1/license-applications")
public class BoreCompletionController {

    private static final Logger log = LoggerFactory.getLogger(BoreCompletionController.class);

    @Autowired
    private CoreLicenseApplicationService coreLicenseApplicationService;

    @Autowired
    private CoreLicenseTypeService coreLicenseTypeService;

    @Autowired
    private CoreApplicationStatusService coreApplicationStatusService;

    @Autowired
    private CoreApplicationStepService coreApplicationStepService;

    @Autowired
    private EmailQueueService emailQueueService;

    @Autowired
    private Auditor auditor;

    @Autowired
    private SysUserAccountService sysUserAccountService;

    @PostMapping(path = "/bore-completion-report")
    public Map<String, Object> createBoreCompletionReport(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT CREATING BORE COMPLETION REPORT ===");

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                throw new ForbiddenException("User not authenticated");
            }

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

            // Set basic fields for backward compatibility
            if (request.get("locationCoordinates") != null) {
                String coordinates = request.get("locationCoordinates").toString();
                String[] parts = coordinates.split(",");
                if (parts.length >= 2) {
                    application.setSourceEasting(parts[0].trim());
                    application.setSourceNorthing(parts[1].trim());
                }
            }
            if (request.get("district") != null) application.setSourceVillage(request.get("district").toString());
            if (request.get("traditionalAuthority") != null) application.setSourceTa(request.get("traditionalAuthority").toString());
            if (request.get("village") != null) application.setSourceOwnerFullname(request.get("village").toString());
            if (request.get("totalDepth") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("totalDepth").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            } else {
                application.setExistingBoreholeCount(1); // Default to 1 borehole for completion report
            }

            // Set default permit duration if not provided
            if (request.get("permitDuration") == null) {
                application.setPermitDuration(25.0); // Default 25 years for bore completion
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // ==> POPULATE JSON FIELDS WITH ALL FORM DATA
            try {
                // Populate CLIENT_INFO JSON
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("organizationName", request.get("organizationName"));
                clientInfo.put("organizationAddress", request.get("organizationAddress"));
                clientInfo.put("responsibleOfficerName", request.get("responsibleOfficerName"));
                clientInfo.put("responsibleOfficerTitle", request.get("responsibleOfficerTitle"));
                clientInfo.put("email", request.get("email"));
                clientInfo.put("telephone", request.get("telephone"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                // Populate LOCATION_INFO JSON
                Map<String, Object> locationInfo = new java.util.HashMap<>();
                locationInfo.put("locationCoordinates", request.get("locationCoordinates"));
                locationInfo.put("district", request.get("district"));
                locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
                locationInfo.put("village", request.get("village"));
                locationInfo.put("altitude", request.get("altitude"));
                locationInfo.put("projectId", request.get("projectId"));
                locationInfo.put("waterPointOwnership", request.get("waterPointOwnership"));
                locationInfo.put("waterPointUse", request.get("waterPointUse"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

                // Populate APPLICATION_METADATA JSON
                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "bore_completion");
                applicationMetadata.put("waterPointType", request.get("waterPointType"));
                applicationMetadata.put("waterPointAbandoned", request.get("waterPointAbandoned"));
                applicationMetadata.put("dateAbandoned", request.get("dateAbandoned"));
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                // Populate FORM_SPECIFIC_DATA JSON
                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                
                // Site selection data
                formSpecificData.put("sitedByOrganization", request.get("sitedByOrganization"));
                formSpecificData.put("sitedByName", request.get("sitedByName"));
                formSpecificData.put("sitedByTitle", request.get("sitedByTitle"));
                formSpecificData.put("dateSited", request.get("dateSited"));
                formSpecificData.put("methodOfSiteSelection", request.get("methodOfSiteSelection"));
                formSpecificData.put("surveyResults", request.get("surveyResults"));
                
                // Construction data
                formSpecificData.put("contractor", request.get("contractor"));
                formSpecificData.put("drilledByName", request.get("drilledByName"));
                formSpecificData.put("drilledByTitle", request.get("drilledByTitle"));
                formSpecificData.put("methodOfDrilling", request.get("methodOfDrilling"));
                formSpecificData.put("drillingCompletionDate", request.get("drillingCompletionDate"));
                formSpecificData.put("totalDepth", request.get("totalDepth"));
                formSpecificData.put("permanentCasingDiameter", request.get("permanentCasingDiameter"));
                formSpecificData.put("typeOfPermanentCasing", request.get("typeOfPermanentCasing"));
                formSpecificData.put("boreholeSealingMaterial", request.get("boreholeSealingMaterial"));
                
                // Pump installation data
                formSpecificData.put("typeOfPump", request.get("typeOfPump"));
                formSpecificData.put("nameOfPump", request.get("nameOfPump"));
                formSpecificData.put("pumpCapacity", request.get("pumpCapacity"));
                formSpecificData.put("pumpInstallationDepth", request.get("pumpInstallationDepth"));
                formSpecificData.put("dateOfPumpInstallation", request.get("dateOfPumpInstallation"));
                
                // Hydrogeological data
                formSpecificData.put("depthToBedrock", request.get("depthToBedrock"));
                formSpecificData.put("overallGeologicalSetting", request.get("overallGeologicalSetting"));
                formSpecificData.put("lithologyData", request.get("lithologyData"));
                formSpecificData.put("waterLevels", request.get("waterLevels"));
                formSpecificData.put("yieldTestResults", request.get("yieldTestResults"));
                formSpecificData.put("waterQualityData", request.get("waterQualityData"));
                
                // Add all remaining fields (exclude emergency fields as they're stored in dedicated columns)
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) &&
                        !locationInfo.containsKey(entry.getKey()) &&
                        !applicationMetadata.containsKey(entry.getKey()) &&
                        !formSpecificData.containsKey(entry.getKey()) &&
                        !entry.getKey().equals("license_type_id") &&
                        !entry.getKey().equals("application_status_id") &&
                        !entry.getKey().equals("applicationPriority") &&
                        !entry.getKey().equals("emergencyReason") &&
                        !entry.getKey().equals("emergencyJustificationDocument")) {
                        formSpecificData.put(entry.getKey(), entry.getValue());
                    }
                }
                application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
            } catch (Exception e) {
                log.error("Error populating JSON fields: {}", e.getMessage());
            }

            // Set application priority before saving
            if (request.get("applicationPriority") != null) {
                String priority = request.get("applicationPriority").toString();
                application.setApplicationPriority(priority);
                log.info("[BORE COMPLETION] Application priority set to: {}", priority);
            } else {
                application.setApplicationPriority("NORMAL");
                log.info("[BORE COMPLETION] No priority provided, defaulting to NORMAL");
            }

            // Save application
            application = this.coreLicenseApplicationService.addCoreLicenseApplication(application);
            
            // Audit log
            auditor.audit(Action.CREATE, "BoreCompletion", application.getId(), applicant, "Created bore completion report");

            // Handle emergency application fields after application is saved
            if ("EMERGENCY".equals(application.getApplicationPriority())) {
                log.info("[BORE COMPLETION] Processing EMERGENCY application...");

                if (request.get("emergencyReason") != null) {
                    application.setEmergencyReason(request.get("emergencyReason").toString());
                    log.info("[BORE COMPLETION] Emergency reason: {}", application.getEmergencyReason());
                } else {
                    log.warn("[BORE COMPLETION] WARNING: No emergency reason provided!");
                }

                // Save emergency document from base64
                if (request.get("emergencyJustificationDocument") != null) {
                    try {
                        String base64File = request.get("emergencyJustificationDocument").toString();
                        log.info("[BORE COMPLETION] Emergency document data length: {}", base64File.length());
                        String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                        application.setEmergencyJustificationFile(savedFilePath);
                        log.info("[BORE COMPLETION] Emergency document saved successfully: {}", savedFilePath);
                    } catch (Exception e) {
                        log.error("[BORE COMPLETION] ERROR saving emergency document: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("[BORE COMPLETION] WARNING: No emergency document provided!");
                }

                // Set emergency submission timestamp
                application.setEmergencySubmittedDate(new java.sql.Timestamp(new Date().getTime()));

                // Update the application with emergency data
                application = coreLicenseApplicationService.editCoreLicenseApplication(application);
                log.info("[BORE COMPLETION] Emergency data saved successfully");
            }

            // Prepare response
            String applicantName = request.get("organizationName") != null ? 
                    request.get("organizationName").toString() : 
                    applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("email") != null ? 
                    request.get("email").toString() : 
                    applicant.getEmailAddress();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Bore completion report submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName());

            // Queue email sending with real application fees
            double applicationFees = licenseType.getApplicationFees() > 0 ? 
                    licenseType.getApplicationFees() : 3000.0;
            
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, 
                    licenseType.getName(), applicationFees);
            
            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);

            return response;
        } catch (Exception e) {
            log.error("Error creating bore completion report: {}", e.getMessage());
            throw new RuntimeException("Failed to create report: " + e.getMessage());
        }
    }

    @PostMapping(path = "/bore-completion-report/renew")
    public Map<String, Object> renewBoreCompletionReport(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT RENEWING BORE COMPLETION REPORT ===");

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                throw new ForbiddenException("User not authenticated");
            }

            CoreLicenseApplication application = new CoreLicenseApplication();
            application.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));
            application.setSysUserAccount(applicant);

            // Set owner_id and user_account_id to current user (applicant)
            application.setOwnerId(applicant.getId());
            application.setUserAccountId(applicant.getId());
            
            // Set application type to RENEWAL and original license ID
            application.setApplicationType("RENEWAL");
            if (request.get("originalLicenseId") != null) {
                application.setOriginalLicenseId(request.get("originalLicenseId").toString());
            }

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

            // Set basic fields for backward compatibility (same as apply)
            if (request.get("locationCoordinates") != null) {
                String coordinates = request.get("locationCoordinates").toString();
                String[] parts = coordinates.split(",");
                if (parts.length >= 2) {
                    application.setSourceEasting(parts[0].trim());
                    application.setSourceNorthing(parts[1].trim());
                }
            }
            if (request.get("district") != null) application.setSourceVillage(request.get("district").toString());
            if (request.get("traditionalAuthority") != null) application.setSourceTa(request.get("traditionalAuthority").toString());
            if (request.get("village") != null) application.setSourceOwnerFullname(request.get("village").toString());
            if (request.get("totalDepth") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("totalDepth").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            } else {
                application.setExistingBoreholeCount(1); // Default to 1 borehole for completion report
            }

            // Set default permit duration if not provided
            if (request.get("permitDuration") == null) {
                application.setPermitDuration(25.0); // Default 25 years for bore completion
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // Populate JSON fields with all form data
            try {
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("organizationName", request.get("organizationName"));
                clientInfo.put("organizationAddress", request.get("organizationAddress"));
                clientInfo.put("responsibleOfficerName", request.get("responsibleOfficerName"));
                clientInfo.put("responsibleOfficerTitle", request.get("responsibleOfficerTitle"));
                clientInfo.put("email", request.get("email"));
                clientInfo.put("telephone", request.get("telephone"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                Map<String, Object> locationInfo = new java.util.HashMap<>();
                locationInfo.put("locationCoordinates", request.get("locationCoordinates"));
                locationInfo.put("district", request.get("district"));
                locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
                locationInfo.put("village", request.get("village"));
                locationInfo.put("altitude", request.get("altitude"));
                locationInfo.put("projectId", request.get("projectId"));
                locationInfo.put("waterPointOwnership", request.get("waterPointOwnership"));
                locationInfo.put("waterPointUse", request.get("waterPointUse"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "RENEWAL");
                applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
                applicationMetadata.put("waterPointType", request.get("waterPointType"));
                applicationMetadata.put("waterPointAbandoned", request.get("waterPointAbandoned"));
                applicationMetadata.put("dateAbandoned", request.get("dateAbandoned"));
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                formSpecificData.put("sitedByOrganization", request.get("sitedByOrganization"));
                formSpecificData.put("sitedByName", request.get("sitedByName"));
                formSpecificData.put("sitedByTitle", request.get("sitedByTitle"));
                formSpecificData.put("dateSited", request.get("dateSited"));
                formSpecificData.put("methodOfSiteSelection", request.get("methodOfSiteSelection"));
                formSpecificData.put("surveyResults", request.get("surveyResults"));
                formSpecificData.put("contractor", request.get("contractor"));
                formSpecificData.put("drilledByName", request.get("drilledByName"));
                formSpecificData.put("drilledByTitle", request.get("drilledByTitle"));
                formSpecificData.put("methodOfDrilling", request.get("methodOfDrilling"));
                formSpecificData.put("drillingCompletionDate", request.get("drillingCompletionDate"));
                formSpecificData.put("totalDepth", request.get("totalDepth"));
                formSpecificData.put("permanentCasingDiameter", request.get("permanentCasingDiameter"));
                formSpecificData.put("typeOfPermanentCasing", request.get("typeOfPermanentCasing"));
                formSpecificData.put("boreholeSealingMaterial", request.get("boreholeSealingMaterial"));
                formSpecificData.put("typeOfPump", request.get("typeOfPump"));
                formSpecificData.put("nameOfPump", request.get("nameOfPump"));
                formSpecificData.put("pumpCapacity", request.get("pumpCapacity"));
                formSpecificData.put("pumpInstallationDepth", request.get("pumpInstallationDepth"));
                formSpecificData.put("dateOfPumpInstallation", request.get("dateOfPumpInstallation"));
                formSpecificData.put("depthToBedrock", request.get("depthToBedrock"));
                formSpecificData.put("overallGeologicalSetting", request.get("overallGeologicalSetting"));
                formSpecificData.put("lithologyData", request.get("lithologyData"));
                formSpecificData.put("waterLevels", request.get("waterLevels"));
                formSpecificData.put("yieldTestResults", request.get("yieldTestResults"));
                formSpecificData.put("waterQualityData", request.get("waterQualityData"));
                
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) && 
                        !locationInfo.containsKey(entry.getKey()) && 
                        !applicationMetadata.containsKey(entry.getKey()) &&
                        !formSpecificData.containsKey(entry.getKey()) &&
                        !entry.getKey().equals("license_type_id") &&
                        !entry.getKey().equals("application_status_id")) {
                        formSpecificData.put(entry.getKey(), entry.getValue());
                    }
                }
                application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
            } catch (Exception e) {
                log.error("Error populating JSON fields: {}", e.getMessage());
            }

            // Save application
            application = this.coreLicenseApplicationService.addCoreLicenseApplication(application);
            
            // Audit log
            auditor.audit(Action.CREATE, "BoreCompletion", application.getId(), applicant, "Created bore completion report renewal");

            // Prepare response
            String applicantName = request.get("organizationName") != null ? 
                    request.get("organizationName").toString() : 
                    applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("email") != null ? 
                    request.get("email").toString() : 
                    applicant.getEmailAddress();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Bore completion report renewal submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName() + " Renewal");
            response.put("applicationType", "RENEWAL");
            response.put("originalLicenseId", application.getOriginalLicenseId());

            // Queue email sending with renewal fees (using licenseFees)
            // Use application-specific license fee if set by manager, otherwise 0
            double renewalFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 3000.0;
            
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, 
                    licenseType.getName() + " Renewal", applicationFees + renewalFees);
            
            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);
            response.put("renewalFees", renewalFees);
            response.put("totalFees", applicationFees + renewalFees);

            return response;
        } catch (Exception e) {
            log.error("Error renewing bore completion report: {}", e.getMessage());
            throw new RuntimeException("Failed to renew report: " + e.getMessage());
        }
    }

    @PostMapping(path = "/bore-completion-report/transfer")
    public Map<String, Object> transferBoreCompletionReport(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT TRANSFERRING BORE COMPLETION REPORT ===");

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                throw new ForbiddenException("User not authenticated");
            }

            CoreLicenseApplication application = new CoreLicenseApplication();
            application.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));
            application.setSysUserAccount(applicant);

            // Set owner_id and user_account_id to current user (applicant)
            application.setOwnerId(applicant.getId());
            application.setUserAccountId(applicant.getId());
            
            // Set application type to TRANSFER and original license ID
            application.setApplicationType("TRANSFER");
            if (request.get("originalLicenseId") != null) {
                application.setOriginalLicenseId(request.get("originalLicenseId").toString());
            }

            // Set transfer recipient user ID by looking up recipient email
            if (request.get("recipientEmail") != null) {
                String recipientEmail = request.get("recipientEmail").toString();
                try {
                    SysUserAccount recipientUser = sysUserAccountService.getSysUserAccountByEmailAddress(recipientEmail);
                    if (recipientUser != null) {
                        application.setTransferToUserId(recipientUser.getId());
                        log.info("Transfer recipient found: {} -> {}", recipientEmail, recipientUser.getId());
                    } else {
                        log.warn("Transfer recipient not found for email: {}", recipientEmail);
                        throw new RuntimeException("Transfer recipient not found with email: " + recipientEmail);
                    }
                } catch (Exception e) {
                    log.error("Error looking up transfer recipient: {}", e.getMessage());
                    throw new RuntimeException("Failed to validate transfer recipient: " + e.getMessage());
                }
            } else {
                log.warn("No recipient email provided for transfer");
                throw new RuntimeException("Recipient email is required for transfer");
            }

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

            // Set basic fields for backward compatibility (same as apply)
            if (request.get("locationCoordinates") != null) {
                String coordinates = request.get("locationCoordinates").toString();
                String[] parts = coordinates.split(",");
                if (parts.length >= 2) {
                    application.setSourceEasting(parts[0].trim());
                    application.setSourceNorthing(parts[1].trim());
                }
            }
            if (request.get("district") != null) application.setSourceVillage(request.get("district").toString());
            if (request.get("traditionalAuthority") != null) application.setSourceTa(request.get("traditionalAuthority").toString());
            if (request.get("village") != null) application.setSourceOwnerFullname(request.get("village").toString());
            if (request.get("totalDepth") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("totalDepth").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            } else {
                application.setExistingBoreholeCount(1); // Default to 1 borehole for completion report
            }

            // Set default permit duration if not provided
            if (request.get("permitDuration") == null) {
                application.setPermitDuration(25.0); // Default 25 years for bore completion
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // Populate JSON fields with all form data
            try {
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("organizationName", request.get("organizationName"));
                clientInfo.put("organizationAddress", request.get("organizationAddress"));
                clientInfo.put("responsibleOfficerName", request.get("responsibleOfficerName"));
                clientInfo.put("responsibleOfficerTitle", request.get("responsibleOfficerTitle"));
                clientInfo.put("email", request.get("email"));
                clientInfo.put("telephone", request.get("telephone"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                Map<String, Object> locationInfo = new java.util.HashMap<>();
                locationInfo.put("locationCoordinates", request.get("locationCoordinates"));
                locationInfo.put("district", request.get("district"));
                locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
                locationInfo.put("village", request.get("village"));
                locationInfo.put("altitude", request.get("altitude"));
                locationInfo.put("projectId", request.get("projectId"));
                locationInfo.put("waterPointOwnership", request.get("waterPointOwnership"));
                locationInfo.put("waterPointUse", request.get("waterPointUse"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "TRANSFER");
                applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
                applicationMetadata.put("transferToUserId", application.getTransferToUserId());
                applicationMetadata.put("recipientEmail", request.get("recipientEmail"));
                applicationMetadata.put("waterPointType", request.get("waterPointType"));
                applicationMetadata.put("waterPointAbandoned", request.get("waterPointAbandoned"));
                applicationMetadata.put("dateAbandoned", request.get("dateAbandoned"));
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                formSpecificData.put("sitedByOrganization", request.get("sitedByOrganization"));
                formSpecificData.put("sitedByName", request.get("sitedByName"));
                formSpecificData.put("sitedByTitle", request.get("sitedByTitle"));
                formSpecificData.put("dateSited", request.get("dateSited"));
                formSpecificData.put("methodOfSiteSelection", request.get("methodOfSiteSelection"));
                formSpecificData.put("surveyResults", request.get("surveyResults"));
                formSpecificData.put("contractor", request.get("contractor"));
                formSpecificData.put("drilledByName", request.get("drilledByName"));
                formSpecificData.put("drilledByTitle", request.get("drilledByTitle"));
                formSpecificData.put("methodOfDrilling", request.get("methodOfDrilling"));
                formSpecificData.put("drillingCompletionDate", request.get("drillingCompletionDate"));
                formSpecificData.put("totalDepth", request.get("totalDepth"));
                formSpecificData.put("permanentCasingDiameter", request.get("permanentCasingDiameter"));
                formSpecificData.put("typeOfPermanentCasing", request.get("typeOfPermanentCasing"));
                formSpecificData.put("boreholeSealingMaterial", request.get("boreholeSealingMaterial"));
                formSpecificData.put("typeOfPump", request.get("typeOfPump"));
                formSpecificData.put("nameOfPump", request.get("nameOfPump"));
                formSpecificData.put("pumpCapacity", request.get("pumpCapacity"));
                formSpecificData.put("pumpInstallationDepth", request.get("pumpInstallationDepth"));
                formSpecificData.put("dateOfPumpInstallation", request.get("dateOfPumpInstallation"));
                formSpecificData.put("depthToBedrock", request.get("depthToBedrock"));
                formSpecificData.put("overallGeologicalSetting", request.get("overallGeologicalSetting"));
                formSpecificData.put("lithologyData", request.get("lithologyData"));
                formSpecificData.put("waterLevels", request.get("waterLevels"));
                formSpecificData.put("yieldTestResults", request.get("yieldTestResults"));
                formSpecificData.put("waterQualityData", request.get("waterQualityData"));
                
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) && 
                        !locationInfo.containsKey(entry.getKey()) && 
                        !applicationMetadata.containsKey(entry.getKey()) &&
                        !formSpecificData.containsKey(entry.getKey()) &&
                        !entry.getKey().equals("license_type_id") &&
                        !entry.getKey().equals("application_status_id")) {
                        formSpecificData.put(entry.getKey(), entry.getValue());
                    }
                }
                application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
            } catch (Exception e) {
                log.error("Error populating JSON fields: {}", e.getMessage());
            }

            // Save application
            application = this.coreLicenseApplicationService.addCoreLicenseApplication(application);
            
            // Audit log
            auditor.audit(Action.CREATE, "BoreCompletion", application.getId(), applicant, "Created bore completion report transfer");

            // Prepare response
            String applicantName = request.get("organizationName") != null ? 
                    request.get("organizationName").toString() : 
                    applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("email") != null ? 
                    request.get("email").toString() : 
                    applicant.getEmailAddress();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Bore completion report transfer submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName() + " Transfer");
            response.put("applicationType", "TRANSFER");
            response.put("originalLicenseId", application.getOriginalLicenseId());
            response.put("transferToUserId", application.getTransferToUserId());
            response.put("recipientEmail", request.get("recipientEmail"));

            // Queue email sending with transfer fees (using licenseFees)
            // Use application-specific license fee if set by manager, otherwise 0
            double transferFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 3000.0;
            
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, 
                    licenseType.getName() + " Transfer", applicationFees + transferFees);
            
            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);
            response.put("transferFees", transferFees);
            response.put("totalFees", applicationFees + transferFees);

            return response;
        } catch (Exception e) {
            log.error("Error transferring bore completion report: {}", e.getMessage());
            throw new RuntimeException("Failed to transfer report: " + e.getMessage());
        }
    }

    @PostMapping(path = "/bore-completion-report/vary")
    public Map<String, Object> varyBoreCompletionReport(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT VARYING BORE COMPLETION REPORT ===");

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                throw new ForbiddenException("User not authenticated");
            }

            CoreLicenseApplication application = new CoreLicenseApplication();
            application.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));
            application.setSysUserAccount(applicant);

            // Set owner_id and user_account_id to current user (applicant)
            application.setOwnerId(applicant.getId());
            application.setUserAccountId(applicant.getId());
            
            // Set application type to VARIATION and original license ID
            application.setApplicationType("VARIATION");
            if (request.get("originalLicenseId") != null) {
                application.setOriginalLicenseId(request.get("originalLicenseId").toString());
            }

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
                application.setCoreApplicationStep(coreApplicationStepService.getNextStep(step));
            }

            // Set basic fields for backward compatibility (same as apply)
            if (request.get("locationCoordinates") != null) {
                String coordinates = request.get("locationCoordinates").toString();
                String[] parts = coordinates.split(",");
                if (parts.length >= 2) {
                    application.setSourceEasting(parts[0].trim());
                    application.setSourceNorthing(parts[1].trim());
                }
            }
            if (request.get("district") != null) application.setSourceVillage(request.get("district").toString());
            if (request.get("traditionalAuthority") != null) application.setSourceTa(request.get("traditionalAuthority").toString());
            if (request.get("village") != null) application.setSourceOwnerFullname(request.get("village").toString());
            if (request.get("totalDepth") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("totalDepth").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            } else {
                application.setExistingBoreholeCount(1); // Default to 1 borehole for completion report
            }

            // Set default permit duration if not provided
            if (request.get("permitDuration") == null) {
                application.setPermitDuration(25.0); // Default 25 years for bore completion
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // Populate JSON fields with all form data
            try {
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("organizationName", request.get("organizationName"));
                clientInfo.put("organizationAddress", request.get("organizationAddress"));
                clientInfo.put("responsibleOfficerName", request.get("responsibleOfficerName"));
                clientInfo.put("responsibleOfficerTitle", request.get("responsibleOfficerTitle"));
                clientInfo.put("email", request.get("email"));
                clientInfo.put("telephone", request.get("telephone"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                Map<String, Object> locationInfo = new java.util.HashMap<>();
                locationInfo.put("locationCoordinates", request.get("locationCoordinates"));
                locationInfo.put("district", request.get("district"));
                locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
                locationInfo.put("village", request.get("village"));
                locationInfo.put("altitude", request.get("altitude"));
                locationInfo.put("projectId", request.get("projectId"));
                locationInfo.put("waterPointOwnership", request.get("waterPointOwnership"));
                locationInfo.put("waterPointUse", request.get("waterPointUse"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "VARIATION");
                applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
                applicationMetadata.put("waterPointType", request.get("waterPointType"));
                applicationMetadata.put("waterPointAbandoned", request.get("waterPointAbandoned"));
                applicationMetadata.put("dateAbandoned", request.get("dateAbandoned"));
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                formSpecificData.put("sitedByOrganization", request.get("sitedByOrganization"));
                formSpecificData.put("sitedByName", request.get("sitedByName"));
                formSpecificData.put("sitedByTitle", request.get("sitedByTitle"));
                formSpecificData.put("dateSited", request.get("dateSited"));
                formSpecificData.put("methodOfSiteSelection", request.get("methodOfSiteSelection"));
                formSpecificData.put("surveyResults", request.get("surveyResults"));
                formSpecificData.put("contractor", request.get("contractor"));
                formSpecificData.put("drilledByName", request.get("drilledByName"));
                formSpecificData.put("drilledByTitle", request.get("drilledByTitle"));
                formSpecificData.put("methodOfDrilling", request.get("methodOfDrilling"));
                formSpecificData.put("drillingCompletionDate", request.get("drillingCompletionDate"));
                formSpecificData.put("totalDepth", request.get("totalDepth"));
                formSpecificData.put("permanentCasingDiameter", request.get("permanentCasingDiameter"));
                formSpecificData.put("typeOfPermanentCasing", request.get("typeOfPermanentCasing"));
                formSpecificData.put("boreholeSealingMaterial", request.get("boreholeSealingMaterial"));
                formSpecificData.put("typeOfPump", request.get("typeOfPump"));
                formSpecificData.put("nameOfPump", request.get("nameOfPump"));
                formSpecificData.put("pumpCapacity", request.get("pumpCapacity"));
                formSpecificData.put("pumpInstallationDepth", request.get("pumpInstallationDepth"));
                formSpecificData.put("dateOfPumpInstallation", request.get("dateOfPumpInstallation"));
                formSpecificData.put("depthToBedrock", request.get("depthToBedrock"));
                formSpecificData.put("overallGeologicalSetting", request.get("overallGeologicalSetting"));
                formSpecificData.put("lithologyData", request.get("lithologyData"));
                formSpecificData.put("waterLevels", request.get("waterLevels"));
                formSpecificData.put("yieldTestResults", request.get("yieldTestResults"));
                formSpecificData.put("waterQualityData", request.get("waterQualityData"));
                
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) && 
                        !locationInfo.containsKey(entry.getKey()) && 
                        !applicationMetadata.containsKey(entry.getKey()) &&
                        !formSpecificData.containsKey(entry.getKey()) &&
                        !entry.getKey().equals("license_type_id") &&
                        !entry.getKey().equals("application_status_id")) {
                        formSpecificData.put(entry.getKey(), entry.getValue());
                    }
                }
                application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
            } catch (Exception e) {
                log.error("Error populating JSON fields: {}", e.getMessage());
            }

            // Save application
            application = this.coreLicenseApplicationService.addCoreLicenseApplication(application);
            
            // Audit log
            auditor.audit(Action.CREATE, "BoreCompletion", application.getId(), applicant, "Created bore completion report variation");

            // Prepare response
            String applicantName = request.get("organizationName") != null ? 
                    request.get("organizationName").toString() : 
                    applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("email") != null ? 
                    request.get("email").toString() : 
                    applicant.getEmailAddress();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Bore completion report variation submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName() + " Variation");
            response.put("applicationType", "VARIATION");
            response.put("originalLicenseId", application.getOriginalLicenseId());

            // Queue email sending with variation fees (using licenseFees)
            // Use application-specific license fee if set by manager, otherwise 0
            double variationFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 3000.0;
            
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, 
                    licenseType.getName() + " Variation", applicationFees + variationFees);
            
            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);
            response.put("variationFees", variationFees);
            response.put("totalFees", applicationFees + variationFees);

            return response;
        } catch (Exception e) {
            log.error("Error varying bore completion report: {}", e.getMessage());
            throw new RuntimeException("Failed to vary report: " + e.getMessage());
        }
    }

    @PutMapping(path = "/bore-completion-report/{applicationId}/edit")
    public ResponseEntity<Map<String, Object>> editBoreCompletionReport(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount applicant = AppUtil.getLoggedInUser(token);
        if (applicant == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Find the existing application
        CoreLicenseApplication application = coreLicenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new RuntimeException("Application not found with ID: " + applicationId);
        }

        // Ensure user can only edit their own applications
        if (!application.getSysUserAccount().getId().equals(applicant.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        try {
            // Update application details from request (same logic as apply method)
            if (request.get("locationCoordinates") != null) {
                String coordinates = request.get("locationCoordinates").toString();
                String[] parts = coordinates.split(",");
                if (parts.length >= 2) {
                    application.setSourceEasting(parts[0].trim());
                    application.setSourceNorthing(parts[1].trim());
                }
            }
            if (request.get("district") != null) application.setSourceVillage(request.get("district").toString());
            if (request.get("traditionalAuthority") != null) application.setSourceTa(request.get("traditionalAuthority").toString());
            if (request.get("village") != null) application.setSourceOwnerFullname(request.get("village").toString());
            if (request.get("totalDepth") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("totalDepth").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            }

            // Update JSON fields with all form data
            try {
                // Update CLIENT_INFO JSON
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("organizationName", request.get("organizationName"));
                clientInfo.put("organizationAddress", request.get("organizationAddress"));
                clientInfo.put("responsibleOfficerName", request.get("responsibleOfficerName"));
                clientInfo.put("responsibleOfficerTitle", request.get("responsibleOfficerTitle"));
                clientInfo.put("email", request.get("email"));
                clientInfo.put("telephone", request.get("telephone"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                // Update LOCATION_INFO JSON
                Map<String, Object> locationInfo = new java.util.HashMap<>();
                locationInfo.put("locationCoordinates", request.get("locationCoordinates"));
                locationInfo.put("district", request.get("district"));
                locationInfo.put("traditionalAuthority", request.get("traditionalAuthority"));
                locationInfo.put("village", request.get("village"));
                locationInfo.put("altitude", request.get("altitude"));
                locationInfo.put("projectId", request.get("projectId"));
                locationInfo.put("waterPointOwnership", request.get("waterPointOwnership"));
                locationInfo.put("waterPointUse", request.get("waterPointUse"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(locationInfo));

                // Update APPLICATION_METADATA JSON
                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "bore_completion");
                applicationMetadata.put("waterPointType", request.get("waterPointType"));
                applicationMetadata.put("waterPointAbandoned", request.get("waterPointAbandoned"));
                applicationMetadata.put("dateAbandoned", request.get("dateAbandoned"));
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("dateUpdated", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                // Update FORM_SPECIFIC_DATA JSON
                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                formSpecificData.put("sitedByOrganization", request.get("sitedByOrganization"));
                formSpecificData.put("sitedByName", request.get("sitedByName"));
                formSpecificData.put("sitedByTitle", request.get("sitedByTitle"));
                formSpecificData.put("dateSited", request.get("dateSited"));
                formSpecificData.put("methodOfSiteSelection", request.get("methodOfSiteSelection"));
                formSpecificData.put("surveyResults", request.get("surveyResults"));
                formSpecificData.put("contractor", request.get("contractor"));
                formSpecificData.put("drilledByName", request.get("drilledByName"));
                formSpecificData.put("drilledByTitle", request.get("drilledByTitle"));
                formSpecificData.put("methodOfDrilling", request.get("methodOfDrilling"));
                formSpecificData.put("drillingCompletionDate", request.get("drillingCompletionDate"));
                formSpecificData.put("totalDepth", request.get("totalDepth"));
                formSpecificData.put("permanentCasingDiameter", request.get("permanentCasingDiameter"));
                formSpecificData.put("typeOfPermanentCasing", request.get("typeOfPermanentCasing"));
                formSpecificData.put("boreholeSealingMaterial", request.get("boreholeSealingMaterial"));
                formSpecificData.put("typeOfPump", request.get("typeOfPump"));
                formSpecificData.put("nameOfPump", request.get("nameOfPump"));
                formSpecificData.put("pumpCapacity", request.get("pumpCapacity"));
                formSpecificData.put("pumpInstallationDepth", request.get("pumpInstallationDepth"));
                formSpecificData.put("dateOfPumpInstallation", request.get("dateOfPumpInstallation"));
                formSpecificData.put("depthToBedrock", request.get("depthToBedrock"));
                formSpecificData.put("overallGeologicalSetting", request.get("overallGeologicalSetting"));
                formSpecificData.put("lithologyData", request.get("lithologyData"));
                formSpecificData.put("waterLevels", request.get("waterLevels"));
                formSpecificData.put("yieldTestResults", request.get("yieldTestResults"));
                formSpecificData.put("waterQualityData", request.get("waterQualityData"));
                
                // Add all remaining fields
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) && 
                        !locationInfo.containsKey(entry.getKey()) && 
                        !applicationMetadata.containsKey(entry.getKey()) &&
                        !formSpecificData.containsKey(entry.getKey()) &&
                        !entry.getKey().equals("license_type_id") &&
                        !entry.getKey().equals("application_status_id")) {
                        formSpecificData.put(entry.getKey(), entry.getValue());
                    }
                }
                application.setFormSpecificData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formSpecificData));
            } catch (Exception e) {
                log.error("Error updating JSON fields: {}", e.getMessage());
            }

            // Reset status to SUBMITTED for reprocessing
            mw.nwra.ewaterpermit.model.CoreApplicationStatus submittedStatus = coreApplicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
            if (submittedStatus != null) {
                application.setCoreApplicationStatus(submittedStatus);
            }

            // Update timestamp
            application.setDateUpdated(new Timestamp(new Date().getTime()));

            // Save updated application
            application = coreLicenseApplicationService.editCoreLicenseApplication(application);
            
            // Audit log
            auditor.audit(Action.UPDATE, "BoreCompletion", application.getId(), applicant, "Updated bore completion report");

            // Prepare response
            String applicantName = request.get("organizationName") != null ? 
                    request.get("organizationName").toString() : 
                    applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("email") != null ? 
                    request.get("email").toString() : 
                    applicant.getEmailAddress();
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", application.getCoreApplicationStatus().getName());
            response.put("message", "Bore completion report updated successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("updatedDate", application.getDateUpdated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", application.getCoreLicenseType().getName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating bore completion report: {}", e.getMessage());
            throw new RuntimeException("Failed to update report: " + e.getMessage());
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

            log.info("[EMERGENCY DOCUMENT] Saved to: {}", filePath.toString());
            return filePath.toString();

        } catch (Exception e) {
            log.error("[EMERGENCY DOCUMENT] Error saving file: {}", e.getMessage());
            throw new java.io.IOException("Failed to save emergency document: " + e.getMessage());
        }
    }
}