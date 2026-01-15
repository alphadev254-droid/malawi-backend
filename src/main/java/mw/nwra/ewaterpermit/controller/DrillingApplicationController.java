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
public class DrillingApplicationController {

    private static final Logger log = LoggerFactory.getLogger(DrillingApplicationController.class);

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

    @PostMapping(path = "/drilling-construction-permit")
    public Map<String, Object> createDrillingConstructionPermitApplication(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT CREATING DRILLING CONSTRUCTION PERMIT APPLICATION ===");
            log.info("Request data: {}", request);

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                log.error("User not authenticated");
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
            if (request.get("companyName") != null) application.setSourceOwnerFullname(request.get("companyName").toString());
            if (request.get("companyAddress") != null) application.setSourceVillage(request.get("companyAddress").toString());
            if (request.get("companyTelephone") != null) application.setSourceTa(request.get("companyTelephone").toString());
            if (request.get("companyEmail") != null) application.setSourceEasting(request.get("companyEmail").toString());
            if (request.get("businessRegNumber") != null) application.setSourceNorthing(request.get("businessRegNumber").toString());
            if (request.get("boreholesDrilled") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("boreholesDrilled").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            }

            // Set default permit duration if not provided
            if (request.get("permitDuration") == null) {
                application.setPermitDuration(5.0); // Default 5 years for drilling permit
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // ==> POPULATE JSON FIELDS WITH ALL FORM DATA
            try {
                // Populate CLIENT_INFO JSON
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("companyName", request.get("companyName"));
                clientInfo.put("companyAddress", request.get("companyAddress"));
                clientInfo.put("companyTelephone", request.get("companyTelephone"));
                clientInfo.put("companyMobile", request.get("companyMobile"));
                clientInfo.put("companyEmail", request.get("companyEmail"));
                clientInfo.put("registrationDate", request.get("registrationDate"));
                clientInfo.put("businessRegNumber", request.get("businessRegNumber"));
                clientInfo.put("issueDate", request.get("issueDate"));
                clientInfo.put("tpin", request.get("tpin"));
                clientInfo.put("companyDirectors", request.get("companyDirectors"));
                clientInfo.put("bankers", request.get("bankers"));
                clientInfo.put("referees", request.get("referees"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                // Populate EQUIPMENT_INFO JSON (as location_info)
                Map<String, Object> equipmentInfo = new java.util.HashMap<>();
                // Drilling rig details
                equipmentInfo.put("rigMakeModel", request.get("rigMakeModel"));
                equipmentInfo.put("rigYearManufacture", request.get("rigYearManufacture"));
                equipmentInfo.put("rigLastOverhaul", request.get("rigLastOverhaul"));
                equipmentInfo.put("rigRatedCapacity", request.get("rigRatedCapacity"));
                equipmentInfo.put("rigMountType", request.get("rigMountType"));
                equipmentInfo.put("rigMastCapacity", request.get("rigMastCapacity"));
                equipmentInfo.put("rigDrawWorksCapacity", request.get("rigDrawWorksCapacity"));
                equipmentInfo.put("rigDrillPipeDiameter", request.get("rigDrillPipeDiameter"));
                // Compressor details
                equipmentInfo.put("compressorMakeModel", request.get("compressorMakeModel"));
                equipmentInfo.put("compressorRatedCapacity", request.get("compressorRatedCapacity"));
                equipmentInfo.put("compressorRatedPressure", request.get("compressorRatedPressure"));
                // Pump details
                equipmentInfo.put("pumpMakeModel", request.get("pumpMakeModel"));
                equipmentInfo.put("pumpLiftCapacity", request.get("pumpLiftCapacity"));
                equipmentInfo.put("pumpSubmersibleCapacity", request.get("pumpSubmersibleCapacity"));
                equipmentInfo.put("pumpPowerSource", request.get("pumpPowerSource"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(equipmentInfo));

                // Populate APPLICATION_METADATA JSON
                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "drilling_construction");
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("applicantFullName", request.get("applicantFullName"));
                applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
                applicationMetadata.put("permitDuration", request.get("permitDuration"));
                applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                // Populate FORM_SPECIFIC_DATA JSON
                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                // Contractor experience
                formSpecificData.put("boreholesDrilled", request.get("boreholesDrilled"));
                formSpecificData.put("averageDepthDrilled", request.get("averageDepthDrilled"));
                formSpecificData.put("managingAuthorityName", request.get("managingAuthorityName"));
                // Personnel experience
                formSpecificData.put("supervisorName", request.get("supervisorName"));
                formSpecificData.put("supervisorYearsExperience", request.get("supervisorYearsExperience"));
                formSpecificData.put("supervisorAvgDepth", request.get("supervisorAvgDepth"));
                formSpecificData.put("driller1Name", request.get("driller1Name"));
                formSpecificData.put("driller1YearsExperience", request.get("driller1YearsExperience"));
                formSpecificData.put("driller2Name", request.get("driller2Name"));
                formSpecificData.put("driller2YearsExperience", request.get("driller2YearsExperience"));
                // Civil works information
                formSpecificData.put("civilCompanyName", request.get("civilCompanyName"));
                formSpecificData.put("civilCompanyAddress", request.get("civilCompanyAddress"));
                formSpecificData.put("civilCompanyMainActivity", request.get("civilCompanyMainActivity"));
                formSpecificData.put("civilWorksForCompany", request.get("civilWorksForCompany"));
                
                // Add all remaining fields (exclude emergency fields as they're stored in dedicated columns)
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) &&
                        !equipmentInfo.containsKey(entry.getKey()) &&
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
                log.info("[DRILLING APPLICATION] Application priority set to: {}", priority);
            } else {
                application.setApplicationPriority("NORMAL");
                log.info("[DRILLING APPLICATION] No priority provided, defaulting to NORMAL");
            }

            // Save application
            application = this.coreLicenseApplicationService.addCoreLicenseApplication(application);
            
            // Audit log
            auditor.audit(Action.CREATE, "DrillingPermit", application.getId(), applicant, "Created drilling construction permit application");

            // Handle emergency application fields after application is saved
            if ("EMERGENCY".equals(application.getApplicationPriority())) {
                log.info("[DRILLING APPLICATION] Processing EMERGENCY application...");

                if (request.get("emergencyReason") != null) {
                    application.setEmergencyReason(request.get("emergencyReason").toString());
                    log.info("[DRILLING APPLICATION] Emergency reason: {}", application.getEmergencyReason());
                } else {
                    log.warn("[DRILLING APPLICATION] WARNING: No emergency reason provided!");
                }

                // Save emergency document from base64
                if (request.get("emergencyJustificationDocument") != null) {
                    try {
                        String base64File = request.get("emergencyJustificationDocument").toString();
                        log.info("[DRILLING APPLICATION] Emergency document data length: {}", base64File.length());
                        String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                        application.setEmergencyJustificationFile(savedFilePath);
                        log.info("[DRILLING APPLICATION] Emergency document saved successfully: {}", savedFilePath);
                    } catch (Exception e) {
                        log.error("[DRILLING APPLICATION] ERROR saving emergency document: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("[DRILLING APPLICATION] WARNING: No emergency document provided!");
                }

                // Set emergency submission timestamp
                application.setEmergencySubmittedDate(new java.sql.Timestamp(new Date().getTime()));

                // Update the application with emergency data
                application = coreLicenseApplicationService.editCoreLicenseApplication(application);
                log.info("[DRILLING APPLICATION] Emergency data saved successfully");
            }

            // Prepare response
            String applicantName = request.get("companyName") != null ? request.get("companyName").toString() : 
                                  request.get("applicantFullName") != null ? request.get("applicantFullName").toString() : 
                                  applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("companyEmail") != null ? request.get("companyEmail").toString() : applicant.getEmailAddress();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Drilling and construction permit application submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName());

            // Queue email sending with real application fees
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 8000.0;
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, licenseType.getName(), applicationFees);

            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);

            return response;
        } catch (Exception e) {
            log.error("Error creating drilling construction permit application: {}", e.getMessage());
            throw new RuntimeException("Failed to create application: " + e.getMessage());
        }
    }

    @PostMapping(path = "/drilling-construction-permit/renew")
    public Map<String, Object> renewDrillingConstructionPermitApplication(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT RENEWING DRILLING CONSTRUCTION PERMIT APPLICATION ===");
            log.info("Request data: {}", request);

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                log.error("User not authenticated");
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
            if (request.get("companyName") != null) application.setSourceOwnerFullname(request.get("companyName").toString());
            if (request.get("companyAddress") != null) application.setSourceVillage(request.get("companyAddress").toString());
            if (request.get("companyTelephone") != null) application.setSourceTa(request.get("companyTelephone").toString());
            if (request.get("companyEmail") != null) application.setSourceEasting(request.get("companyEmail").toString());
            if (request.get("businessRegNumber") != null) application.setSourceNorthing(request.get("businessRegNumber").toString());
            if (request.get("boreholesDrilled") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("boreholesDrilled").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            }

            // Set default permit duration if not provided
            if (request.get("permitDuration") == null) {
                application.setPermitDuration(5.0); // Default 5 years for drilling permit
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // Populate JSON fields with all form data
            try {
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("companyName", request.get("companyName"));
                clientInfo.put("companyAddress", request.get("companyAddress"));
                clientInfo.put("companyTelephone", request.get("companyTelephone"));
                clientInfo.put("companyMobile", request.get("companyMobile"));
                clientInfo.put("companyEmail", request.get("companyEmail"));
                clientInfo.put("registrationDate", request.get("registrationDate"));
                clientInfo.put("businessRegNumber", request.get("businessRegNumber"));
                clientInfo.put("issueDate", request.get("issueDate"));
                clientInfo.put("tpin", request.get("tpin"));
                clientInfo.put("companyDirectors", request.get("companyDirectors"));
                clientInfo.put("bankers", request.get("bankers"));
                clientInfo.put("referees", request.get("referees"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                Map<String, Object> equipmentInfo = new java.util.HashMap<>();
                equipmentInfo.put("rigMakeModel", request.get("rigMakeModel"));
                equipmentInfo.put("rigYearManufacture", request.get("rigYearManufacture"));
                equipmentInfo.put("rigLastOverhaul", request.get("rigLastOverhaul"));
                equipmentInfo.put("rigRatedCapacity", request.get("rigRatedCapacity"));
                equipmentInfo.put("rigMountType", request.get("rigMountType"));
                equipmentInfo.put("rigMastCapacity", request.get("rigMastCapacity"));
                equipmentInfo.put("rigDrawWorksCapacity", request.get("rigDrawWorksCapacity"));
                equipmentInfo.put("rigDrillPipeDiameter", request.get("rigDrillPipeDiameter"));
                equipmentInfo.put("compressorMakeModel", request.get("compressorMakeModel"));
                equipmentInfo.put("compressorRatedCapacity", request.get("compressorRatedCapacity"));
                equipmentInfo.put("compressorRatedPressure", request.get("compressorRatedPressure"));
                equipmentInfo.put("pumpMakeModel", request.get("pumpMakeModel"));
                equipmentInfo.put("pumpLiftCapacity", request.get("pumpLiftCapacity"));
                equipmentInfo.put("pumpSubmersibleCapacity", request.get("pumpSubmersibleCapacity"));
                equipmentInfo.put("pumpPowerSource", request.get("pumpPowerSource"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(equipmentInfo));

                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "RENEWAL");
                applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("applicantFullName", request.get("applicantFullName"));
                applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
                applicationMetadata.put("permitDuration", request.get("permitDuration"));
                applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                formSpecificData.put("boreholesDrilled", request.get("boreholesDrilled"));
                formSpecificData.put("averageDepthDrilled", request.get("averageDepthDrilled"));
                formSpecificData.put("managingAuthorityName", request.get("managingAuthorityName"));
                formSpecificData.put("supervisorName", request.get("supervisorName"));
                formSpecificData.put("supervisorYearsExperience", request.get("supervisorYearsExperience"));
                formSpecificData.put("supervisorAvgDepth", request.get("supervisorAvgDepth"));
                formSpecificData.put("driller1Name", request.get("driller1Name"));
                formSpecificData.put("driller1YearsExperience", request.get("driller1YearsExperience"));
                formSpecificData.put("driller2Name", request.get("driller2Name"));
                formSpecificData.put("driller2YearsExperience", request.get("driller2YearsExperience"));
                formSpecificData.put("civilCompanyName", request.get("civilCompanyName"));
                formSpecificData.put("civilCompanyAddress", request.get("civilCompanyAddress"));
                formSpecificData.put("civilCompanyMainActivity", request.get("civilCompanyMainActivity"));
                formSpecificData.put("civilWorksForCompany", request.get("civilWorksForCompany"));
                
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) && 
                        !equipmentInfo.containsKey(entry.getKey()) && 
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
            auditor.audit(Action.CREATE, "DrillingPermit", application.getId(), applicant, "Created drilling construction permit renewal application");

            // Prepare response
            String applicantName = request.get("companyName") != null ? request.get("companyName").toString() : 
                                  request.get("applicantFullName") != null ? request.get("applicantFullName").toString() : 
                                  applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("companyEmail") != null ? request.get("companyEmail").toString() : applicant.getEmailAddress();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Drilling and construction permit renewal application submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName() + " Renewal");
            response.put("applicationType", "RENEWAL");
            response.put("originalLicenseId", application.getOriginalLicenseId());

            // Queue email sending with renewal fees (using licenseFees)
            // Use application-specific license fee if set by manager, otherwise 0
            double renewalFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 8000.0;
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, licenseType.getName() + " Renewal", applicationFees + renewalFees);

            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);
            response.put("renewalFees", renewalFees);
            response.put("totalFees", applicationFees + renewalFees);

            return response;
        } catch (Exception e) {
            log.error("Error renewing drilling construction permit application: {}", e.getMessage());
            throw new RuntimeException("Failed to renew application: " + e.getMessage());
        }
    }

    @PostMapping(path = "/drilling-construction-permit/transfer")
    public Map<String, Object> transferDrillingConstructionPermitApplication(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT TRANSFERRING DRILLING CONSTRUCTION PERMIT APPLICATION ===");
            log.info("Request data: {}", request);

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                log.error("User not authenticated");
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
            if (request.get("companyName") != null) application.setSourceOwnerFullname(request.get("companyName").toString());
            if (request.get("companyAddress") != null) application.setSourceVillage(request.get("companyAddress").toString());
            if (request.get("companyTelephone") != null) application.setSourceTa(request.get("companyTelephone").toString());
            if (request.get("companyEmail") != null) application.setSourceEasting(request.get("companyEmail").toString());
            if (request.get("businessRegNumber") != null) application.setSourceNorthing(request.get("businessRegNumber").toString());
            if (request.get("boreholesDrilled") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("boreholesDrilled").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            }

            // Set default permit duration if not provided
            if (request.get("permitDuration") == null) {
                application.setPermitDuration(5.0); // Default 5 years for drilling permit
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // Populate JSON fields with all form data
            try {
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("companyName", request.get("companyName"));
                clientInfo.put("companyAddress", request.get("companyAddress"));
                clientInfo.put("companyTelephone", request.get("companyTelephone"));
                clientInfo.put("companyMobile", request.get("companyMobile"));
                clientInfo.put("companyEmail", request.get("companyEmail"));
                clientInfo.put("registrationDate", request.get("registrationDate"));
                clientInfo.put("businessRegNumber", request.get("businessRegNumber"));
                clientInfo.put("issueDate", request.get("issueDate"));
                clientInfo.put("tpin", request.get("tpin"));
                clientInfo.put("companyDirectors", request.get("companyDirectors"));
                clientInfo.put("bankers", request.get("bankers"));
                clientInfo.put("referees", request.get("referees"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                Map<String, Object> equipmentInfo = new java.util.HashMap<>();
                equipmentInfo.put("rigMakeModel", request.get("rigMakeModel"));
                equipmentInfo.put("rigYearManufacture", request.get("rigYearManufacture"));
                equipmentInfo.put("rigLastOverhaul", request.get("rigLastOverhaul"));
                equipmentInfo.put("rigRatedCapacity", request.get("rigRatedCapacity"));
                equipmentInfo.put("rigMountType", request.get("rigMountType"));
                equipmentInfo.put("rigMastCapacity", request.get("rigMastCapacity"));
                equipmentInfo.put("rigDrawWorksCapacity", request.get("rigDrawWorksCapacity"));
                equipmentInfo.put("rigDrillPipeDiameter", request.get("rigDrillPipeDiameter"));
                equipmentInfo.put("compressorMakeModel", request.get("compressorMakeModel"));
                equipmentInfo.put("compressorRatedCapacity", request.get("compressorRatedCapacity"));
                equipmentInfo.put("compressorRatedPressure", request.get("compressorRatedPressure"));
                equipmentInfo.put("pumpMakeModel", request.get("pumpMakeModel"));
                equipmentInfo.put("pumpLiftCapacity", request.get("pumpLiftCapacity"));
                equipmentInfo.put("pumpSubmersibleCapacity", request.get("pumpSubmersibleCapacity"));
                equipmentInfo.put("pumpPowerSource", request.get("pumpPowerSource"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(equipmentInfo));

                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "TRANSFER");
                applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("applicantFullName", request.get("applicantFullName"));
                applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
                applicationMetadata.put("permitDuration", request.get("permitDuration"));
                applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                formSpecificData.put("boreholesDrilled", request.get("boreholesDrilled"));
                formSpecificData.put("averageDepthDrilled", request.get("averageDepthDrilled"));
                formSpecificData.put("managingAuthorityName", request.get("managingAuthorityName"));
                formSpecificData.put("supervisorName", request.get("supervisorName"));
                formSpecificData.put("supervisorYearsExperience", request.get("supervisorYearsExperience"));
                formSpecificData.put("supervisorAvgDepth", request.get("supervisorAvgDepth"));
                formSpecificData.put("driller1Name", request.get("driller1Name"));
                formSpecificData.put("driller1YearsExperience", request.get("driller1YearsExperience"));
                formSpecificData.put("driller2Name", request.get("driller2Name"));
                formSpecificData.put("driller2YearsExperience", request.get("driller2YearsExperience"));
                formSpecificData.put("civilCompanyName", request.get("civilCompanyName"));
                formSpecificData.put("civilCompanyAddress", request.get("civilCompanyAddress"));
                formSpecificData.put("civilCompanyMainActivity", request.get("civilCompanyMainActivity"));
                formSpecificData.put("civilWorksForCompany", request.get("civilWorksForCompany"));
                
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) && 
                        !equipmentInfo.containsKey(entry.getKey()) && 
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
            auditor.audit(Action.CREATE, "DrillingPermit", application.getId(), applicant, "Created drilling construction permit transfer application");

            // Prepare response
            String applicantName = request.get("companyName") != null ? request.get("companyName").toString() : 
                                  request.get("applicantFullName") != null ? request.get("applicantFullName").toString() : 
                                  applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("companyEmail") != null ? request.get("companyEmail").toString() : applicant.getEmailAddress();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Drilling and construction permit transfer application submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName() + " Transfer");
            response.put("applicationType", "TRANSFER");
            response.put("originalLicenseId", application.getOriginalLicenseId());

            // Queue email sending with transfer fees (using licenseFees)
            // Use application-specific license fee if set by manager, otherwise 0
            double transferFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 8000.0;
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, licenseType.getName() + " Transfer", applicationFees + transferFees);

            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);
            response.put("transferFees", transferFees);
            response.put("totalFees", applicationFees + transferFees);

            return response;
        } catch (Exception e) {
            log.error("Error transferring drilling construction permit application: {}", e.getMessage());
            throw new RuntimeException("Failed to transfer application: " + e.getMessage());
        }
    }

    @PostMapping(path = "/drilling-construction-permit/vary")
    public Map<String, Object> varyDrillingConstructionPermitApplication(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        try {
            log.info("=== APPLICANT VARYING DRILLING CONSTRUCTION PERMIT APPLICATION ===");
            log.info("Request data: {}", request);

            SysUserAccount applicant = AppUtil.getLoggedInUser(token);
            if (applicant == null) {
                log.error("User not authenticated");
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
            if (request.get("companyName") != null) application.setSourceOwnerFullname(request.get("companyName").toString());
            if (request.get("companyAddress") != null) application.setSourceVillage(request.get("companyAddress").toString());
            if (request.get("companyTelephone") != null) application.setSourceTa(request.get("companyTelephone").toString());
            if (request.get("companyEmail") != null) application.setSourceEasting(request.get("companyEmail").toString());
            if (request.get("businessRegNumber") != null) application.setSourceNorthing(request.get("businessRegNumber").toString());
            if (request.get("boreholesDrilled") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("boreholesDrilled").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            }

            // Set default permit duration if not provided
            if (request.get("permitDuration") == null) {
                application.setPermitDuration(5.0); // Default 5 years for drilling permit
            }

            // Set submitted date
            application.setDateSubmitted(new java.sql.Timestamp(new java.util.Date().getTime()));

            // Populate JSON fields with all form data
            try {
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("companyName", request.get("companyName"));
                clientInfo.put("companyAddress", request.get("companyAddress"));
                clientInfo.put("companyTelephone", request.get("companyTelephone"));
                clientInfo.put("companyMobile", request.get("companyMobile"));
                clientInfo.put("companyEmail", request.get("companyEmail"));
                clientInfo.put("registrationDate", request.get("registrationDate"));
                clientInfo.put("businessRegNumber", request.get("businessRegNumber"));
                clientInfo.put("issueDate", request.get("issueDate"));
                clientInfo.put("tpin", request.get("tpin"));
                clientInfo.put("companyDirectors", request.get("companyDirectors"));
                clientInfo.put("bankers", request.get("bankers"));
                clientInfo.put("referees", request.get("referees"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                Map<String, Object> equipmentInfo = new java.util.HashMap<>();
                equipmentInfo.put("rigMakeModel", request.get("rigMakeModel"));
                equipmentInfo.put("rigYearManufacture", request.get("rigYearManufacture"));
                equipmentInfo.put("rigLastOverhaul", request.get("rigLastOverhaul"));
                equipmentInfo.put("rigRatedCapacity", request.get("rigRatedCapacity"));
                equipmentInfo.put("rigMountType", request.get("rigMountType"));
                equipmentInfo.put("rigMastCapacity", request.get("rigMastCapacity"));
                equipmentInfo.put("rigDrawWorksCapacity", request.get("rigDrawWorksCapacity"));
                equipmentInfo.put("rigDrillPipeDiameter", request.get("rigDrillPipeDiameter"));
                equipmentInfo.put("compressorMakeModel", request.get("compressorMakeModel"));
                equipmentInfo.put("compressorRatedCapacity", request.get("compressorRatedCapacity"));
                equipmentInfo.put("compressorRatedPressure", request.get("compressorRatedPressure"));
                equipmentInfo.put("pumpMakeModel", request.get("pumpMakeModel"));
                equipmentInfo.put("pumpLiftCapacity", request.get("pumpLiftCapacity"));
                equipmentInfo.put("pumpSubmersibleCapacity", request.get("pumpSubmersibleCapacity"));
                equipmentInfo.put("pumpPowerSource", request.get("pumpPowerSource"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(equipmentInfo));

                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "VARIATION");
                applicationMetadata.put("originalLicenseId", request.get("originalLicenseId"));
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("applicantFullName", request.get("applicantFullName"));
                applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
                applicationMetadata.put("permitDuration", request.get("permitDuration"));
                applicationMetadata.put("dateSubmitted", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                formSpecificData.put("boreholesDrilled", request.get("boreholesDrilled"));
                formSpecificData.put("averageDepthDrilled", request.get("averageDepthDrilled"));
                formSpecificData.put("managingAuthorityName", request.get("managingAuthorityName"));
                formSpecificData.put("supervisorName", request.get("supervisorName"));
                formSpecificData.put("supervisorYearsExperience", request.get("supervisorYearsExperience"));
                formSpecificData.put("supervisorAvgDepth", request.get("supervisorAvgDepth"));
                formSpecificData.put("driller1Name", request.get("driller1Name"));
                formSpecificData.put("driller1YearsExperience", request.get("driller1YearsExperience"));
                formSpecificData.put("driller2Name", request.get("driller2Name"));
                formSpecificData.put("driller2YearsExperience", request.get("driller2YearsExperience"));
                formSpecificData.put("civilCompanyName", request.get("civilCompanyName"));
                formSpecificData.put("civilCompanyAddress", request.get("civilCompanyAddress"));
                formSpecificData.put("civilCompanyMainActivity", request.get("civilCompanyMainActivity"));
                formSpecificData.put("civilWorksForCompany", request.get("civilWorksForCompany"));
                
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) && 
                        !equipmentInfo.containsKey(entry.getKey()) && 
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
            auditor.audit(Action.CREATE, "DrillingPermit", application.getId(), applicant, "Created drilling construction permit variation application");

            // Prepare response
            String applicantName = request.get("companyName") != null ? request.get("companyName").toString() : 
                                  request.get("applicantFullName") != null ? request.get("applicantFullName").toString() : 
                                  applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("companyEmail") != null ? request.get("companyEmail").toString() : applicant.getEmailAddress();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", "SUBMITTED");
            response.put("message", "Drilling and construction permit variation application submitted successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", licenseType.getName() + " Variation");
            response.put("applicationType", "VARIATION");
            response.put("originalLicenseId", application.getOriginalLicenseId());

            // Queue email sending with variation fees (using licenseFees)
            // Use application-specific license fee if set by manager, otherwise 0
            double variationFees = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;
            double applicationFees = licenseType.getApplicationFees() > 0 ? licenseType.getApplicationFees() : 8000.0;
            String emailTaskId = emailQueueService.queueInvoiceEmail(
                    application.getId(), applicantName, applicantEmail, licenseType.getName() + " Variation", applicationFees + variationFees);

            response.put("emailTaskId", emailTaskId);
            response.put("applicationFees", applicationFees);
            response.put("variationFees", variationFees);
            response.put("totalFees", applicationFees + variationFees);

            return response;
        } catch (Exception e) {
            log.error("Error varying drilling construction permit application: {}", e.getMessage());
            throw new RuntimeException("Failed to vary application: " + e.getMessage());
        }
    }

    @PutMapping(path = "/drilling-construction-permit/{applicationId}/edit")
    public ResponseEntity<Map<String, Object>> editDrillingConstructionPermitApplication(
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
            if (request.get("companyName") != null) application.setSourceOwnerFullname(request.get("companyName").toString());
            if (request.get("companyAddress") != null) application.setSourceVillage(request.get("companyAddress").toString());
            if (request.get("companyTelephone") != null) application.setSourceTa(request.get("companyTelephone").toString());
            if (request.get("companyEmail") != null) application.setSourceEasting(request.get("companyEmail").toString());
            if (request.get("businessRegNumber") != null) application.setSourceNorthing(request.get("businessRegNumber").toString());
            if (request.get("boreholesDrilled") != null) {
                try {
                    application.setExistingBoreholeCount(Integer.valueOf(request.get("boreholesDrilled").toString()));
                } catch (NumberFormatException e) {
                    application.setExistingBoreholeCount(0);
                }
            }

            // Update JSON fields with all form data
            try {
                // Update CLIENT_INFO JSON
                Map<String, Object> clientInfo = new java.util.HashMap<>();
                clientInfo.put("companyName", request.get("companyName"));
                clientInfo.put("companyAddress", request.get("companyAddress"));
                clientInfo.put("companyTelephone", request.get("companyTelephone"));
                clientInfo.put("companyMobile", request.get("companyMobile"));
                clientInfo.put("companyEmail", request.get("companyEmail"));
                clientInfo.put("registrationDate", request.get("registrationDate"));
                clientInfo.put("businessRegNumber", request.get("businessRegNumber"));
                clientInfo.put("issueDate", request.get("issueDate"));
                clientInfo.put("tpin", request.get("tpin"));
                clientInfo.put("companyDirectors", request.get("companyDirectors"));
                clientInfo.put("bankers", request.get("bankers"));
                clientInfo.put("referees", request.get("referees"));
                application.setClientInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientInfo));

                // Update EQUIPMENT_INFO JSON (as location_info)
                Map<String, Object> equipmentInfo = new java.util.HashMap<>();
                equipmentInfo.put("rigMakeModel", request.get("rigMakeModel"));
                equipmentInfo.put("rigYearManufacture", request.get("rigYearManufacture"));
                equipmentInfo.put("rigLastOverhaul", request.get("rigLastOverhaul"));
                equipmentInfo.put("rigRatedCapacity", request.get("rigRatedCapacity"));
                equipmentInfo.put("rigMountType", request.get("rigMountType"));
                equipmentInfo.put("rigMastCapacity", request.get("rigMastCapacity"));
                equipmentInfo.put("rigDrawWorksCapacity", request.get("rigDrawWorksCapacity"));
                equipmentInfo.put("rigDrillPipeDiameter", request.get("rigDrillPipeDiameter"));
                equipmentInfo.put("compressorMakeModel", request.get("compressorMakeModel"));
                equipmentInfo.put("compressorRatedCapacity", request.get("compressorRatedCapacity"));
                equipmentInfo.put("compressorRatedPressure", request.get("compressorRatedPressure"));
                equipmentInfo.put("pumpMakeModel", request.get("pumpMakeModel"));
                equipmentInfo.put("pumpLiftCapacity", request.get("pumpLiftCapacity"));
                equipmentInfo.put("pumpSubmersibleCapacity", request.get("pumpSubmersibleCapacity"));
                equipmentInfo.put("pumpPowerSource", request.get("pumpPowerSource"));
                application.setLocationInfo(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(equipmentInfo));

                // Update APPLICATION_METADATA JSON
                Map<String, Object> applicationMetadata = new java.util.HashMap<>();
                applicationMetadata.put("applicationType", "drilling_construction");
                applicationMetadata.put("declarationDate", request.get("declarationDate"));
                applicationMetadata.put("applicantFullName", request.get("applicantFullName"));
                applicationMetadata.put("applicantSignature", request.get("applicantSignature"));
                applicationMetadata.put("permitDuration", request.get("permitDuration"));
                applicationMetadata.put("dateUpdated", new java.util.Date().getTime());
                application.setApplicationMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(applicationMetadata));

                // Update FORM_SPECIFIC_DATA JSON
                Map<String, Object> formSpecificData = new java.util.HashMap<>();
                formSpecificData.put("boreholesDrilled", request.get("boreholesDrilled"));
                formSpecificData.put("averageDepthDrilled", request.get("averageDepthDrilled"));
                formSpecificData.put("managingAuthorityName", request.get("managingAuthorityName"));
                formSpecificData.put("supervisorName", request.get("supervisorName"));
                formSpecificData.put("supervisorYearsExperience", request.get("supervisorYearsExperience"));
                formSpecificData.put("supervisorAvgDepth", request.get("supervisorAvgDepth"));
                formSpecificData.put("driller1Name", request.get("driller1Name"));
                formSpecificData.put("driller1YearsExperience", request.get("driller1YearsExperience"));
                formSpecificData.put("driller2Name", request.get("driller2Name"));
                formSpecificData.put("driller2YearsExperience", request.get("driller2YearsExperience"));
                formSpecificData.put("civilCompanyName", request.get("civilCompanyName"));
                formSpecificData.put("civilCompanyAddress", request.get("civilCompanyAddress"));
                formSpecificData.put("civilCompanyMainActivity", request.get("civilCompanyMainActivity"));
                formSpecificData.put("civilWorksForCompany", request.get("civilWorksForCompany"));
                
                // Add all remaining fields (exclude emergency fields as they're stored in dedicated columns)
                for (Map.Entry<String, Object> entry : request.entrySet()) {
                    if (!clientInfo.containsKey(entry.getKey()) &&
                        !equipmentInfo.containsKey(entry.getKey()) &&
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
                log.error("Error updating JSON fields: {}", e.getMessage());
            }

            // Update application priority
            if (request.get("applicationPriority") != null) {
                String priority = request.get("applicationPriority").toString();
                application.setApplicationPriority(priority);
                log.info("[DRILLING APPLICATION] Application priority updated to: {}", priority);
            }

            // Handle emergency application fields
            if ("EMERGENCY".equals(application.getApplicationPriority())) {
                log.info("[DRILLING APPLICATION] Processing EMERGENCY application update...");

                if (request.get("emergencyReason") != null) {
                    application.setEmergencyReason(request.get("emergencyReason").toString());
                    log.info("[DRILLING APPLICATION] Emergency reason updated: {}", application.getEmergencyReason());
                }

                // Save emergency document from base64 if provided
                if (request.get("emergencyJustificationDocument") != null) {
                    try {
                        String base64File = request.get("emergencyJustificationDocument").toString();
                        log.info("[DRILLING APPLICATION] Emergency document data length: {}", base64File.length());
                        String savedFilePath = saveEmergencyDocumentFromBase64(base64File, application.getId());
                        application.setEmergencyJustificationFile(savedFilePath);
                        log.info("[DRILLING APPLICATION] Emergency document updated successfully: {}", savedFilePath);
                    } catch (Exception e) {
                        log.error("[DRILLING APPLICATION] ERROR updating emergency document: {}", e.getMessage(), e);
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
            mw.nwra.ewaterpermit.model.CoreApplicationStatus submittedStatus = coreApplicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
            if (submittedStatus != null) {
                application.setCoreApplicationStatus(submittedStatus);
            }

            // Update timestamp
            application.setDateUpdated(new Timestamp(new Date().getTime()));

            // Save updated application
            application = coreLicenseApplicationService.editCoreLicenseApplication(application);
            
            // Audit log
            auditor.audit(Action.UPDATE, "DrillingPermit", application.getId(), applicant, "Updated drilling construction permit application");

            // Prepare response
            String applicantName = request.get("companyName") != null ? request.get("companyName").toString() : 
                                  request.get("applicantFullName") != null ? request.get("applicantFullName").toString() : 
                                  applicant.getFirstName() + " " + applicant.getLastName();
            String applicantEmail = request.get("companyEmail") != null ? request.get("companyEmail").toString() : applicant.getEmailAddress();
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", application.getId());
            response.put("status", application.getCoreApplicationStatus().getName());
            response.put("message", "Drilling and construction permit application updated successfully");
            response.put("applicationDate", application.getDateCreated());
            response.put("updatedDate", application.getDateUpdated());
            response.put("applicantName", applicantName);
            response.put("applicantEmail", applicantEmail);
            response.put("licenseType", application.getCoreLicenseType().getName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating drilling construction permit application: {}", e.getMessage());
            throw new RuntimeException("Failed to update application: " + e.getMessage());
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