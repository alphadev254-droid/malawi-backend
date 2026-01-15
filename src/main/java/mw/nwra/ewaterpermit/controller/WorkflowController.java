package mw.nwra.ewaterpermit.controller;

import mw.nwra.ewaterpermit.dto.LicenseDataDto;
import mw.nwra.ewaterpermit.model.*;
import mw.nwra.ewaterpermit.service.*;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import mw.nwra.ewaterpermit.constant.Action;


@RestController
@RequestMapping("/v1/workflow")

public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private CoreLicenseApplicationService applicationService;

    @Autowired
    private CoreApplicationDocumentService documentService;

    @Autowired
    private CoreDocumentCategoryService documentCategoryService;

    @Autowired
    private CoreApplicationPaymentService paymentService;


    @Autowired
    private CoreApplicationStepService applicationStepService;

    @Autowired
    private CoreLicenseApplicationActivityService coreLicenseApplicationActivityService;

    @Autowired
    private CoreLicenseTypeActivityService coreLicenseTypeActivityService;

    @Autowired
    private CoreApplicationStatusService coreApplicationStatusService;

    @Autowired
    private CoreLicenseAssessmentService assessmentService;

    @Autowired
    private EmailQueueService emailQueueService;

    @Autowired
    private CoreLicensePermitService coreLicensePermitService;

    @Autowired
    private CoreInvoiceService coreInvoiceService;

    @Autowired
    private SysUserAccountService userAccountService;

    @Autowired
    private OfficerNotificationService officerNotificationService;

    @Autowired
    private CoreLicenseService coreLicenseService;

    @Autowired
    private CoreFeesTypeService coreFeesTypeService;

    @Autowired
    private CoreFinancialYearService coreFinancialYearService;
    private SysUserAccountService sysUserAccountService;
    @Autowired
    private CoreLicenseApplicationService coreLicenseApplicationService;
    
    @Autowired
    private Auditor auditor;

    /**
     * Get user profile data for autofilling application forms
     */
    @GetMapping("/user-profile-data")
    public ResponseEntity<?> getUserProfileData(
            @RequestHeader("Authorization") String token
    ) {
        try {
            log.info("Getting user profile data with token: {}", token != null ? "[TOKEN_PROVIDED]" : "[NO_TOKEN]");

            SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
            if (currentUser == null) {
                log.warn("User not found for provided token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            log.info("User found: {}", currentUser.getUsername());

            // Create profile data for autofilling forms with null safety
            Map<String, Object> profileData = new HashMap<>();

            String firstName = currentUser.getFirstName() != null ? currentUser.getFirstName() : "";
            String lastName = currentUser.getLastName() != null ? currentUser.getLastName() : "";
            String companyName = (firstName + " " + lastName).trim();
            // Match form field names exactly
            profileData.put("name", firstName); // Form uses "name" field
            profileData.put("address", currentUser.getPostalAddress() != null ? currentUser.getPostalAddress() : "");
            profileData.put("district", currentUser.getCoreDistrict() != null ? currentUser.getCoreDistrict().getName() : "");
            profileData.put("telephone", currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
            profileData.put("mobilePhone", currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : ""); // Form uses "mobilePhone"
            profileData.put("email", currentUser.getEmailAddress() != null ? currentUser.getEmailAddress() : "");

            // Keep legacy fields for backward compatibility
            profileData.put("companyName", companyName.isEmpty() ? "Company Name" : companyName);
            profileData.put("mobileNumber", currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");

            log.info("Profile data created successfully");
            return ResponseEntity.ok(profileData);
        } catch (Exception e) {
            log.error("Error getting user profile data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get profile data: " + e.getMessage()));
        }
    }

//    private CoreApplicationStepServiceImpl coreApplicationStepService;

    /**
     * Upload payment receipt for an application
     */
    @PostMapping("/upload-receipt/{applicationId}")
    public ResponseEntity<?> uploadPaymentReceipt(
            @PathVariable String applicationId,
            @RequestParam("receipt") MultipartFile file,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "license_requirement_id", required = false) String licenseRequirementId) {


        try {
            log.info("=== UPLOADING PAYMENT RECEIPT ===");
            log.info("Application ID: {}", applicationId);
            log.info("Payment Method: {}", paymentMethod);
            log.info("File: {}", file.getOriginalFilename());
            log.info("Notes: {}", notes);
            log.info("Amount: {}", amount);
            log.info("License Requirement ID: {}", licenseRequirementId);

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No file uploaded"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (!isValidFileType(contentType)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid file type. Only images and PDFs are allowed."));
            }

            // Validate file size (5MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File size exceeds 5MB limit"));
            }

            // Find the application
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                log.error("License Requirement ID  not found: {}", applicationId);
                return ResponseEntity.notFound().build();
            }

            // Create receipts directory if it doesn't exist
            String uploadDir = "uploads/receipts/" + applicationId;
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("=== FILE SAVED SUCCESSFULLY ===");
            log.info("File saved to: {}", filePath.toAbsolutePath());
            log.info("File size: {} bytes", Files.size(filePath));

            // Get or create "Payment Receipt" document category
            CoreDocumentCategory receiptCategory = getOrCreateReceiptCategory();
            log.info("=== DOCUMENT CATEGORY ===");
            log.info("Category: {} (ID: {})", receiptCategory.getName(), receiptCategory.getId());

            // Create document record
            CoreApplicationDocument receiptDocument = new CoreApplicationDocument();
            receiptDocument.setDocumentUrl(filePath.toString());
            receiptDocument.setCoreLicenseApplication(application);
            receiptDocument.setCoreDocumentCategory(receiptCategory);
            receiptDocument.setDateCreated(new Timestamp(System.currentTimeMillis()));

            log.info("=== SAVING DOCUMENT TO DATABASE ===");
            log.info("Document URL: {}", filePath.toString());
            log.info("Application ID: {}", application.getId());
            log.info("Category ID: {}", receiptCategory.getId());

            // Save document
            CoreApplicationDocument savedDocument = documentService.createCoreApplicationDocument(receiptDocument);

            log.info("=== DOCUMENT SAVED SUCCESSFULLY ===");
            log.info("Document ID: {}", savedDocument.getId());
            log.info("Saved to table: core_application_document");
            log.info("Document status: {}", savedDocument.getStatus());

            // Create payment record for manual receipt uploads that need verification
            log.info("=== CREATING PAYMENT RECORD FOR VERIFICATION ===");
            log.info("Payment method: {}", paymentMethod);
            log.info("Amount: {}", amount);
            log.info("Document ID for verification: {}", savedDocument.getId());
            List<CoreApplicationPayment> existingPayment = paymentService.getByLicence(application);
            CoreApplicationPayment savedPayment = null;
            if (!existingPayment.isEmpty()) {
                for (CoreApplicationPayment payment : existingPayment) {
                    // Update payment if it's PENDING or REJECTED (to allow re-upload after rejection)
                    if ("PENDING".equals(payment.getPaymentStatus()) || "REJECTED".equals(payment.getPaymentStatus())) {
                        payment.setCoreLicenseApplication(application);
                        payment.setAmountPaid(amount != null ? amount : 0.0);
                        payment.setPaymentMethod(paymentMethod);
                        payment.setPaymentStatus("AWAITING_APPROVAL");
                        payment.setReceiptDocumentId(savedDocument.getId()); // Update to NEW document ID
                        payment.setNeedsVerification(true);
                        paymentService.editCoreApplicationPayment(payment);
                        savedPayment = payment;
                        log.info("Updated existing payment record (was {}) with new receipt document ID: {}",
                                 payment.getPaymentStatus(), savedDocument.getId());
                        break;
                    }
                }

                if (savedPayment == null) {
                    throw new RuntimeException("No pending or rejected payment found to update");
                }
            } else {
                CoreApplicationPayment payment = new CoreApplicationPayment();
                payment.setCoreLicenseApplication(application);
                payment.setAmountPaid(amount != null ? amount : 0.0);
                payment.setPaymentMethod(paymentMethod);
                payment.setPaymentStatus("AWAITING_APPROVAL");
                payment.setCoreFeesType(coreFeesTypeService.getCoreFeesTypeByName("Application fee"));
                payment.setReceiptDocumentId(savedDocument.getId());
                payment.setNeedsVerification(true);
                payment.setDateCreated(new Timestamp(System.currentTimeMillis()));
                // Save payment record
                savedPayment = paymentService.addCoreApplicationPayment(payment);
                log.info("Payment record created with ID: {}", savedPayment.getId());
            }


            // Update application with receipt information
            Map<String, Object> receiptInfo = new HashMap<>();
            receiptInfo.put("documentId", savedDocument.getId());
            receiptInfo.put("filename", uniqueFilename);
            receiptInfo.put("originalFilename", originalFilename);
            receiptInfo.put("paymentMethod", paymentMethod);
            receiptInfo.put("uploadedAt", LocalDateTime.now().toString());
            receiptInfo.put("notes", notes);
            receiptInfo.put("amount", amount);
            receiptInfo.put("licenseRequirementId", licenseRequirementId);
            receiptInfo.put("status", "AWAITING_APPROVAL");
            receiptInfo.put("paymentId", savedPayment.getId());

            // Notify accountants about new payment receipt upload
            try {
                log.info("=== NOTIFYING ACCOUNTANTS ABOUT NEW RECEIPT UPLOAD ===");
                log.info("Application ID: {}", applicationId);
                log.info("Receipt uploaded by applicant, notifying accountants for verification");

                officerNotificationService.notifyAccountantsAboutReceiptUpload(application);
                log.info("Accountant notification sent successfully");
            } catch (Exception notificationError) {
                log.error("Error notifying accountants about receipt upload for application {}: {}",
                        applicationId, notificationError.getMessage());
                // Continue with the response even if notification fails
            }

            // Send confirmation email to applicant about receipt upload
            try {
                log.info("=== SENDING RECEIPT CONFIRMATION EMAIL TO APPLICANT ===");
                String applicantName = application.getSysUserAccount().getFirstName() + " " +
                                     application.getSysUserAccount().getLastName();
                String applicantEmail = application.getSysUserAccount().getEmailAddress();

                log.info("Applicant: {} ({})", applicantName, applicantEmail);
                log.info("Receipt amount: MWK {}", amount);
                log.info("Payment method: {}", paymentMethod);

                // Convert "other" payment method to more user-friendly text
                String displayPaymentMethod = "other".equalsIgnoreCase(paymentMethod) ? "Receipt Upload" : paymentMethod;

                String emailTaskId = emailQueueService.queueReceiptConfirmationEmail(
                    applicationId, applicantName, applicantEmail,
                    application.getCoreLicenseType().getName(), amount, displayPaymentMethod);

                log.info("Receipt confirmation email queued with task ID: {}", emailTaskId);
                receiptInfo.put("emailTaskId", emailTaskId);
            } catch (Exception emailError) {
                log.error("Error sending receipt confirmation email for application {}: {}",
                        applicationId, emailError.getMessage());
                // Continue with the response even if email fails
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Receipt uploaded successfully");
            response.put("receiptInfo", receiptInfo);
            updateAssessmentHandler(applicationId, token);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error uploading receipt file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading payment receipt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/save-assessment/{applicationId}")
    public ResponseEntity<?> saveAssessment(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            log.info("=== STEP 1: VALIDATING APPLICATION ===");
            log.info("Application ID: {}", applicationId);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                log.error("Application not found: {}", applicationId);
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }
            log.info("Application found: {}", application.getId());

            log.info("=== STEP 2: GETTING USER ===");
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            log.info("User ID: {}", user != null ? user.getId() : "null");

            log.info("=== STEP 3: CREATING ASSESSMENT ===");
            CoreLicenseAssessment assessment = new CoreLicenseAssessment();

            assessment.setLicenseApplicationId(applicationId);
            log.info("Set license_application_id: {}", applicationId);

            assessment.setAssessmentNotes(request.get("assessmentNotes").toString());
            log.info("Set assessment_notes: {}", request.get("assessmentNotes"));

            assessment.setRentalQuantity(new BigDecimal(request.get("rentalQuantity").toString()));
            log.info("Set rental_quantity: {}", request.get("rentalQuantity"));

            assessment.setRentalRate(new BigDecimal(request.get("rentalRate").toString()));
            log.info("Set rental_rate: {}", request.get("rentalRate"));

            assessment.setCalculatedAnnualRental(new BigDecimal(request.get("calculatedRental").toString()));
            log.info("Set calculated_annual_rental: {}", request.get("calculatedRental"));

            assessment.setLicenseOfficerId(user != null ? user.getId() : null);
            log.info("Set license_officer_id: {}", user != null ? user.getId() : null);

            String action = request.getOrDefault("action", "submit").toString();
            assessment.setAssessmentStatus("submit".equals(action) ? "COMPLETED" : "DRAFT");
            log.info("Set assessment_status: {}", assessment.getAssessmentStatus());

            if (request.get("recommendedDate") != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    Date parsedDate = sdf.parse(request.get("recommendedDate").toString());
                    assessment.setRecommendedScheduleDate(parsedDate);
                    log.info("Set recommended_schedule_date: {}", parsedDate);
                } catch (Exception e) {
                    log.warn("Error parsing date: {}", e.getMessage());
                }
            }

            log.info("=== STEP 4: SAVING TO DATABASE ===");
            CoreLicenseAssessment savedAssessment = assessmentService.save(assessment);

            updateAssessmentHandler(applicationId, token);
            log.info("=== STEP 5: SUCCESS ===");
            log.info("Assessment saved with ID: {}", savedAssessment.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Assessment saved successfully");
            response.put("assessmentId", savedAssessment.getId());
            response.put("applicationId", applicationId);
            response.put("action", action);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("=== ERROR ===");
            log.error("Error: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    /**
     * Confirm applicant document (for license officers) - JSON version
     */
    @PostMapping(value = "/confirm-document", consumes = "application/json")
    public ResponseEntity<?> confirmDocumentJson(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {

        String licenseApplicationId = (String) request.get("license_application_id");
        String notes = (String) request.get("notes");
        String action = (String) request.get("action");

        String documentUrl = (String) request.get("documentUrl");

        return processConfirmDocument(licenseApplicationId, notes, documentUrl, null, token, action);
    }

    /**
     * Confirm applicant document (for license officers) - Multipart version
     */
    @PostMapping(value = "/confirm-document", consumes = "multipart/form-data")
    public ResponseEntity<?> confirmDocumentMultipart(
            @RequestParam("license_application_id") String licenseApplicationId,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "document", required = false) MultipartFile document,
            @RequestHeader("Authorization") String token) {

        return processConfirmDocument(licenseApplicationId, notes, null, document, token, null);
    }

    /**
     * Process document confirmation - shared logic
     */
    private ResponseEntity<?> processConfirmDocument(
            String licenseApplicationId,
            String notes,
            String documentUrl,
            MultipartFile document,
            String token,
            String action) {

        try {
            log.info("=== CONFIRMING DOCUMENT ===");
            log.info("Application ID: {}", licenseApplicationId);
            log.info("Document URL: {}", documentUrl);
            log.info("Notes: {}", notes);
            log.info("Has file upload: {}", document != null && !document.isEmpty());

            // Get user from token
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            // Step 1: Get application and current step
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(licenseApplicationId);
            if (application == null) {
                log.error("Application not found with ID: {}", licenseApplicationId);
                return ResponseEntity.notFound().build();
            }
            // Clear referral fields when document is confirmed (application moves forward)
            application.clearReferral();
//            if (action != null) {
//                if (action.equals("forward_to_drs")) {
//                    application.setStatus(coreApplicationStatusService.getCoreApplicationStatusByName("PENDING_SCHEDULE_AUTHORIZATION"));
//                } else {
//                    log.info("");
//                }
//            }

            // Update last handled by user ID
            application.setLastHandledByUserId(user.getId());

            // Update assessment handler based on user role
            updateAssessmentHandler(licenseApplicationId, token);

            log.info("Application found: {}", application.getId());
            log.info("Application status: {}", application.getCoreApplicationStatus() != null ? application.getCoreApplicationStatus().getName() : "null");
            log.info("License type: {}", application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "null");

            CoreApplicationStep currentStep = application.getCoreApplicationStep();
            if (currentStep == null) {
                log.error("Application {} has no current step assigned", licenseApplicationId);

                // Try to assign the first step for this license type
                CoreApplicationStep firstStep = getApplicationStepBySequence(application.getCoreLicenseType(), 1);
                if (firstStep != null) {
                    log.info("Assigning first step to application: {}", firstStep.getName());
                    application.setCoreApplicationStep(firstStep);
                    applicationService.editCoreLicenseApplication(application);
                    currentStep = firstStep;
                } else {
                    log.error("No steps found for license type: {}", application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "null");
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Application has no current step and no steps found for license type"));
                }
            }

            log.info("Current step: {} (sequence: {})", currentStep.getName(), currentStep.getSequenceNumber());

            // Step 2: Get current sequence number
            int currentSequenceNumber = currentStep.getSequenceNumber();
            log.info("Current sequence number: {}", currentSequenceNumber);

            // Step 3: Find license type activity for current sequence
            log.info("Looking for license type activity for sequence: {}", currentSequenceNumber);
            CoreLicenseTypeActivity licenseTypeActivity = findLicenseTypeActivityBySequence(
                    application.getCoreLicenseType(), currentSequenceNumber);

            if (licenseTypeActivity == null) {
                log.error("No license type activity found for sequence: {} and license type: {}",
                        currentSequenceNumber, application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "null");

                // Try to find any activity for this license type
                CoreLicenseTypeActivity anyActivity = findAnyLicenseTypeActivity(application.getCoreLicenseType());
                if (anyActivity != null) {
                    log.info("Using fallback activity: {}", anyActivity.getName());
                    licenseTypeActivity = anyActivity;
                } else {
                    log.error("No license type activities found at all for license type");
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "No license type activity found for sequence: " + currentSequenceNumber));
                }
            } else {
                log.info("Found license type activity: {}", licenseTypeActivity.getName());
            }

            // Handle file upload if present
            String finalDocumentUrl = documentUrl;
            if (document != null && !document.isEmpty()) {
                if (!isValidFileType(document.getContentType())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid file type. Only images and PDFs are allowed."));
                }

                String uploadDir = "uploads/confirmations/" + licenseApplicationId;
                Path uploadPath = Paths.get(uploadDir);
                Files.createDirectories(uploadPath);

                String originalFilename = document.getOriginalFilename();
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
                Path filePath = uploadPath.resolve(uniqueFilename);

                Files.copy(document.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                finalDocumentUrl = filePath.toString();
                log.info("Document saved: {}", finalDocumentUrl);
            }

            // Step 4: Record the activity
            CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
            activity.setCoreLicenseApplication(application);
            activity.setSysUserAccount(user);
            activity.setCoreLicenseTypeActivity(licenseTypeActivity);

            String description = notes != null ? notes : "Document confirmed";
            if (finalDocumentUrl != null) {
                description += " - Document: " + finalDocumentUrl;
            }
            activity.setDescription(description);
            activity.setDateCreated(new Timestamp(System.currentTimeMillis()));

            coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
            log.info("Activity recorded successfully");

            // Step 5: Get next step (increment sequence number)
            int nextSequenceNumber = currentSequenceNumber + 1;
            CoreApplicationStep nextStep = getApplicationStepBySequence(
                    application.getCoreLicenseType(), nextSequenceNumber);

            // Step 6: Update application step
            if (nextStep != null) {
                application.setCoreApplicationStep(nextStep);
                applicationService.editCoreLicenseApplication(application);
                log.info("Application step updated to sequence: {}", nextSequenceNumber);
            } else {
                log.warn("No next step found for sequence: {}", nextSequenceNumber);
            }
            updateAssessmentHandler(application.getId(), token);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document confirmed successfully");
            response.put("currentSequence", currentSequenceNumber);
            response.put("nextSequence", nextSequenceNumber);
            response.put("activityId", activity.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error confirming document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Process workflow action (approve, reject, refer back, etc.)
     */
    @PostMapping("/process-action/{applicationId}")
    public ResponseEntity<?> processWorkflowAction(
            @PathVariable String applicationId,
            @RequestParam("action") String action,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "userRole", required = false) String userRole) {

        try {
            log.info("=== PROCESSING WORKFLOW ACTION ===");
            log.info("Application ID: {}", applicationId);
            log.info("Action: {}", action);
            log.info("User Role: {}", userRole);
            log.info("Notes: {}", notes);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            // For now, we'll process the workflow action
            // In a full implementation, you'd update the application status appropriately
            // String newStatus = determineNewStatus(action, userRole, currentStatus);

            // Update application
            applicationService.editCoreLicenseApplication(application);

            String newStatus = "PROCESSED"; // Placeholder status

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Workflow action processed successfully");
            response.put("newStatus", newStatus);
            response.put("action", action);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing workflow action", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Upload field assessment results
     */
    @PostMapping("/upload-field-results/{applicationId}")
    public ResponseEntity<?> uploadFieldResults(
            @PathVariable String applicationId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "notes", required = false) String notes) {

        try {
            log.info("=== UPLOADING FIELD RESULTS ===");
            log.info("Application ID: {}", applicationId);
            log.info("Number of files: {}", files.length);
            log.info("Notes: {}", notes);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            // Create field results directory
            String uploadDir = "uploads/field-results/" + applicationId;
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            StringBuilder uploadedFiles = new StringBuilder();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    // Validate file
                    if (!isValidFileType(file.getContentType())) {
                        continue; // Skip invalid files
                    }

                    String originalFilename = file.getOriginalFilename();
                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
                    Path filePath = uploadPath.resolve(uniqueFilename);

                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    uploadedFiles.append(originalFilename).append(", ");
                }
            }

            // Update application with field results
            // In a full implementation, you would properly track field assessment results
            applicationService.editCoreLicenseApplication(application);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Field results uploaded successfully");
            response.put("filesUploaded", uploadedFiles.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading field results", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Calculate annual rentals
     */
    @PostMapping("/calculate-rentals/{applicationId}")
    public ResponseEntity<?> calculateRentals(
            @PathVariable String applicationId,
            @RequestParam("quantity") Double quantity,
            @RequestParam("rate") Double rate,
            @RequestParam(value = "notes", required = false) String notes) {

        try {
            log.info("=== CALCULATING RENTALS ===");
            log.info("Application ID: {}", applicationId);
            log.info("Quantity: {}", quantity);
            log.info("Rate: {}", rate);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            double annualRental = quantity * rate * 365;

            // Update application with rental calculation
            // In a full implementation, you would store this in a separate rental calculation entity
            applicationService.editCoreLicenseApplication(application);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Rentals calculated successfully");
            response.put("quantity", quantity);
            response.put("rate", rate);
            response.put("annualRental", annualRental);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calculating rentals", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.startsWith("image/")
        );
    }

    private CoreDocumentCategory getOrCreateReceiptCategory() {
        try {
            // Try to find existing "Payment Receipt" category
            List<CoreDocumentCategory> categories = documentCategoryService.getCoreDocumentCategories(0, 100);
            for (CoreDocumentCategory category : categories) {
                if ("Payment Receipt".equals(category.getName())) {
                    return category;
                }
            }

            // Create new category if not found
            CoreDocumentCategory newCategory = new CoreDocumentCategory();
            newCategory.setName("Payment Receipt");
            newCategory.setDescription("Payment receipt documents uploaded by applicants");
            newCategory.setDateCreated(new Timestamp(System.currentTimeMillis()));

            return documentCategoryService.createCoreDocumentCategory(newCategory);
        } catch (Exception e) {
            log.error("Error getting/creating receipt category", e);
            throw new RuntimeException("Failed to get receipt category");
        }
    }

    // Removed getOrCreatePaymentRecord method - using document-only verification

    /**
     * Get payment receipts for an application
     */
    @GetMapping("/payment-receipts/{applicationId}")
    public ResponseEntity<?> getPaymentReceipts(@PathVariable String applicationId) {
        try {
            log.info("Getting payment receipts for application: {}", applicationId);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            // Get receipt category
            CoreDocumentCategory receiptCategory = getOrCreateReceiptCategory();

            // Get all documents for this application with receipt category
            List<CoreApplicationDocument> allDocuments = documentService.getCoreApplicationDocuments(0, 1000);
            List<Map<String, Object>> receipts = new ArrayList<>();

            for (CoreApplicationDocument doc : allDocuments) {
                if (doc.getCoreLicenseApplication() != null &&
                        applicationId.equals(doc.getCoreLicenseApplication().getId()) &&
                        doc.getCoreDocumentCategory() != null &&
                        receiptCategory.getId().equals(doc.getCoreDocumentCategory().getId())) {

                    Map<String, Object> receiptInfo = new HashMap<>();
                    receiptInfo.put("documentId", doc.getId());
                    receiptInfo.put("documentUrl", doc.getDocumentUrl());
                    receiptInfo.put("uploadedAt", doc.getDateCreated());
                    receiptInfo.put("status", doc.getStatus());
                    receiptInfo.put("category", "Payment Receipt");

                    receipts.add(receiptInfo);
                }
            }

            return ResponseEntity.ok(Map.of("receipts", receipts));

        } catch (Exception e) {
            log.error("Error getting payment receipts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Approve payment receipt (for accountants)
     */
    @PostMapping("/approve-payment/{feeType}/{applicationId}")
    public ResponseEntity<?> approvePayment(
            @PathVariable String applicationId,
            @PathVariable String feeType,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestHeader(name = "Authorization", required = false) String token) {

        try {
            log.info("[OPTIMIZED] Approving payment for application: {}", applicationId);

            // Map fee type parameter to database name
            String feeTypeName = Map.of(
                    "APPLICATION_FEE", "Application fee",
                    "LICENCE_FEE", "License fees"
            ).get(feeType);

            if (feeTypeName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid fee type: " + feeType));
            }

            // 1. Approve payment using optimized service method
            boolean paymentApproved = paymentService.approvePaymentByApplicationAndFeeType(applicationId, feeTypeName);
            if (!paymentApproved) {
                return ResponseEntity.badRequest().body(Map.of("error", "No payment found to approve for fee type: " + feeTypeName));
            }

            // Log activity to activity table
            try {
                SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
                CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);

                if (currentUser != null && application != null) {
                    // Get or create the activity type for payment approval
                    CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "APPROVE_PAYMENT");

                    if (activityType != null) {
                        CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                        activity.setCoreLicenseApplication(application);
                        activity.setSysUserAccount(currentUser);
                        activity.setCoreLicenseTypeActivity(activityType);
                        activity.setDescription("Approved " + feeTypeName + " payment" + (notes != null ? ": " + notes : ""));
                        activity.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));

                        coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                        log.info("✅ Activity logged for payment approval by user: {}", currentUser.getUsername());
                    } else {
                        log.warn("Could not get/create activity type for payment approval");
                    }
                }
            } catch (Exception activityError) {
                log.error("Failed to log activity (non-blocking): {}", activityError.getMessage());
            }

            // 2. Check current step and determine if step progression is needed
            Integer currentSequence = paymentService.getApplicationStepSequence(applicationId);
            if (currentSequence == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found or step not set"));
            }

            // 3. Handle step progression for APPLICATION_FEE at step 0
            if (Objects.equals(feeType, "APPLICATION_FEE") && currentSequence == 0) {
                log.info("=== PROGRESSING APPLICATION TO NEXT STEP AFTER PAYMENT APPROVAL ===");

                String nextStepId = applicationStepService.getNextStepIdForApplication(applicationId);
                if (nextStepId != null) {
                    boolean stepUpdated = paymentService.updateApplicationStep(applicationId, nextStepId);
                    if (stepUpdated) {
                        log.info("✅ Application successfully moved to next step: {}", nextStepId);

                        // Send officer notifications asynchronously to avoid blocking
                        try {
                            // We need basic application info for notifications - use lightweight fetch
                            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
                            if (application != null) {
                                String officerRole = mapStepToOfficerRole("Licensing Officer"); // Step 1 is typically licensing officer
                                log.info("🔔 Notifying officers in role: '{}'", officerRole);
                                officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                                log.info("✅ Officer notification completed");
                            }
                        } catch (Exception notificationError) {
                            log.error("Error notifying officers (non-blocking): {}", notificationError.getMessage());
                        }

                        log.info("=== APPLICATION PROGRESSION COMPLETED ===");
                    } else {
                        log.error("Failed to update application step");
                    }
                } else {
                    log.error("No next step found for application");
                }
            } else if (currentSequence == 6) {
                log.info("Application at final step - no progression needed");
            } else if (!Objects.equals(feeType, "APPLICATION_FEE")) {
                log.info("License fee payment approved - no step progression needed");
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Not authorized for this activity at current step"));
            }

            // Send payment approval confirmation email to applicant
            try {
                log.info("=== SENDING PAYMENT APPROVAL EMAIL TO APPLICANT ===");
                CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
                if (application != null) {
                    String applicantName = application.getSysUserAccount().getFirstName() + " " +
                                         application.getSysUserAccount().getLastName();
                    String applicantEmail = application.getSysUserAccount().getEmailAddress();

                    log.info("Applicant: {} ({})", applicantName, applicantEmail);
                    log.info("Fee type approved: {}", feeTypeName);

                    String emailTaskId = emailQueueService.queuePaymentApprovalEmail(
                        applicationId, applicantName, applicantEmail,
                        application.getCoreLicenseType().getName(), feeTypeName);

                    log.info("Payment approval email queued with task ID: {}", emailTaskId);
                }
            } catch (Exception emailError) {
                log.error("Error sending payment approval email for application {}: {}",
                        applicationId, emailError.getMessage());
                // Continue with the response even if email fails
            }

            // 4. Send success response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment approved successfully");
            response.put("paymentStatus", "PAID");
            response.put("applicationId", applicationId);

            log.info("[OPTIMIZED] Payment approval completed for application: {}", applicationId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error approving payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Reject payment receipt (for accountants)
     */
    @PostMapping("/reject-payment/{applicationId}")
    public ResponseEntity<?> rejectPayment(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization", required = false) String token) {

        try {
            log.info("Rejecting payment for application: {}", applicationId);

            String reason = request.containsKey("reason") ? request.get("reason").toString() : "Payment receipt rejected";
            log.info("Rejection reason: {}", reason);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            // Update document status
            log.info("=== REJECTING PAYMENT THROUGH DOCUMENT STATUS ===");
            List<CoreApplicationDocument> appDocuments = documentService.getCoreApplicationDocumentByApplication(application);
            for (CoreApplicationDocument doc : appDocuments) {
                if (doc.getCoreDocumentCategory() != null &&
                        "Payment Receipt".equals(doc.getCoreDocumentCategory().getName())) {
                    doc.setStatus("REJECTED");
                    documentService.editCoreApplicationDocument(doc);
                    log.info("Updated document status to REJECTED for document: {}", doc.getId());
                }
            }

            // Update payment status to REJECTED
            log.info("=== UPDATING PAYMENT STATUS TO REJECTED ===");
            List<CoreApplicationPayment> payments = paymentService.getByLicence(application);
            for (CoreApplicationPayment payment : payments) {
                if ("AWAITING_APPROVAL".equals(payment.getPaymentStatus())) {
                    payment.setPaymentStatus("REJECTED");
                    paymentService.editCoreApplicationPayment(payment);
                    log.info("Updated payment status to REJECTED for payment ID: {}", payment.getId());
                }
            }

            // Send email notification to applicant about payment rejection
            try {
                if (application.getSysUserAccount() != null &&
                        application.getSysUserAccount().getEmailAddress() != null) {

                    String applicantEmail = application.getSysUserAccount().getEmailAddress();
                    String applicantName = getApplicantFullName(application.getSysUserAccount());
                    String licenseType = application.getCoreLicenseType() != null ?
                            application.getCoreLicenseType().getName() : "Water Permit";

                    String subject = "Payment Receipt Rejected - " + licenseType + " Application";
                    String emailBody = String.format("""
                            <!DOCTYPE html>
                            <html lang="en">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Payment Receipt Rejected</title>
                                <style>
                                    body {
                                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                        line-height: 1.6;
                                        margin: 0;
                                        padding: 20px;
                                        background-color: #f8f9fa;
                                        color: #333;
                                    }
                                    .container {
                                        max-width: 600px;
                                        margin: 0 auto;
                                        background-color: #ffffff;
                                        border-radius: 8px;
                                        overflow: hidden;
                                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                    }
                                    .header {
                                        background-color: #0ea5e9;
                                        color: white;
                                        padding: 15px 30px;
                                        text-align: center;
                                    }
                                    .header .logo {
                                        width: 80px;
                                        height: 80px;
                                        margin-bottom: 10px;
                                        background-color: white;
                                        padding: 8px;
                                        border-radius: 6px;
                                    }
                                    .header h1 {
                                        margin: 0;
                                        font-size: 24px;
                                        font-weight: 600;
                                    }
                                    .header p {
                                        margin: 8px 0 0 0;
                                        font-size: 16px;
                                        opacity: 0.9;
                                    }
                                    .content {
                                        padding: 40px 30px;
                                    }
                                    .greeting {
                                        font-size: 16px;
                                        margin-bottom: 24px;
                                    }
                                    .status {
                                        background-color: #fee2e2;
                                        color: #991b1b;
                                        padding: 12px 20px;
                                        border-radius: 6px;
                                        text-align: center;
                                        font-weight: 600;
                                        margin: 24px 0;
                                    }
                                    .details {
                                        background-color: #f8fafc;
                                        border: 1px solid #e2e8f0;
                                        border-radius: 6px;
                                        padding: 24px;
                                        margin: 24px 0;
                                    }
                                    .details h3 {
                                        margin: 0 0 20px 0;
                                        font-size: 18px;
                                        color: #1e293b;
                                    }
                                    .detail-table {
                                        width: 100%%;
                                        border-collapse: collapse;
                                    }
                                    .detail-row {
                                        border-bottom: 1px solid #e2e8f0;
                                    }
                                    .detail-row:last-child {
                                        border-bottom: none;
                                    }
                                    .detail-row td {
                                        padding: 12px 8px;
                                        vertical-align: top;
                                    }
                                    .detail-label {
                                        color: #64748b;
                                        width: 45%%;
                                    }
                                    .detail-value {
                                        color: #1e293b;
                                        font-weight: 500;
                                        text-align: right;
                                        width: 55%%;
                                    }
                                    .rejection-notice {
                                        background-color: #fee2e2;
                                        border: 1px solid #dc2626;
                                        border-radius: 6px;
                                        padding: 20px;
                                        margin: 24px 0;
                                    }
                                    .rejection-notice p {
                                        margin: 0;
                                        color: #991b1b;
                                    }
                                    .action-notice {
                                        background-color: #fef3c7;
                                        border: 1px solid #f59e0b;
                                        border-radius: 6px;
                                        padding: 20px;
                                        margin: 24px 0;
                                    }
                                    .action-notice p {
                                        margin: 0;
                                        color: #92400e;
                                        font-weight: 500;
                                    }
                                    .footer {
                                        background-color: #f1f5f9;
                                        padding: 30px;
                                        text-align: center;
                                        border-top: 1px solid #e2e8f0;
                                    }
                                    .footer p {
                                        margin: 0;
                                        color: #64748b;
                                        font-size: 14px;
                                    }
                                    .footer .org-name {
                                        font-weight: 600;
                                        color: #1e293b;
                                        margin-bottom: 4px;
                                    }
                                    .disclaimer {
                                        font-size: 12px;
                                        color: #94a3b8;
                                        margin-top: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <img src="cid:logo" alt="NWRA Logo" style="width: 80px; height: 80px; margin-bottom: 10px; background-color: white; padding: 8px; border-radius: 6px; display: block; margin-left: auto; margin-right: auto;">
                                        <p>National Water Resources Authority</p>
                                        <h1>💳 Payment Receipt Rejected</h1>
                                    </div>

                                    <div class="content">
                                        <div class="greeting">
                                            Dear <strong>%s</strong>,
                                        </div>

                                        <p>We regret to inform you that your payment receipt for the <strong>%s</strong> application has been rejected.</p>

                                        <div class="status">
                                            Payment Status: REJECTED
                                        </div>

                                        <div class="details">
                                            <h3>Application Summary</h3>
                                            <table class="detail-table">
                                                <tr class="detail-row">
                                                    <td class="detail-label">Application ID</td>
                                                    <td class="detail-value">%s</td>
                                                </tr>
                                                <tr class="detail-row">
                                                    <td class="detail-label">License Type</td>
                                                    <td class="detail-value">%s</td>
                                                </tr>
                                            </table>
                                        </div>

                                        <div class="rejection-notice">
                                            <p><strong>Rejection Reason:</strong></p>
                                            <p>%s</p>
                                        </div>

                                        <div class="action-notice">
                                            <p><strong>Next Steps:</strong></p>
                                            <p>Please upload a new, valid payment receipt through your application portal to continue processing your application.</p>
                                        </div>

                                        <p>You can access your application by logging into the system with your application ID.</p>

                                        <p>For any questions about the payment requirements, please contact our support team.</p>

                                        <p>Thank you for your understanding.</p>
                                    </div>

                                    <div class="footer">
                                        <p class="org-name">National Water Resources Authority</p>
                                        <p>License Application System</p>
                                        <p class="disclaimer">
                                            This is an automated message. Please do not reply to this email.
                                        </p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                            applicantName, licenseType, applicationId, licenseType, reason
                    );

                    String taskId = "payment-rejected-" + applicationId + "-" + System.currentTimeMillis();
                    emailQueueService.sendEmailAsync(taskId, applicantEmail, subject, emailBody);

                    log.info("💳 Payment rejection email sent to: {} for application: {}", applicantEmail, applicationId);
                } else {
                    log.warn("No email address found for applicant of application: {}", applicationId);
                }
            } catch (Exception emailError) {
                log.error("Error sending payment rejection email for application {}: {}", applicationId, emailError.getMessage());
                // Continue with the response even if email fails
            }

            // Log activity to activity table
            try {
                SysUserAccount currentUser = AppUtil.getLoggedInUser(token);

                if (currentUser != null && application != null) {
                    CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                    activity.setCoreLicenseApplication(application);
                    activity.setSysUserAccount(currentUser);
                    activity.setDescription("Rejected payment receipt: " + reason);
                    activity.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));

                    coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                    log.info("✅ Activity logged for payment rejection by user: {}", currentUser.getUsername());
                }
            } catch (Exception activityError) {
                log.error("Failed to log activity (non-blocking): {}", activityError.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment rejected");
            response.put("paymentStatus", "REJECTED");
            response.put("applicationId", applicationId);
            response.put("reason", reason);
            response.put("verificationMethod", "DOCUMENT_STATUS");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error rejecting payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * View/Download payment receipt document
     */
    @GetMapping("/view-receipt/{documentId}")
    public ResponseEntity<Resource> viewReceipt(@PathVariable String documentId) {
        try {
            log.info("Viewing receipt document: {}", documentId);

            // Get document record
            CoreApplicationDocument document = documentService.getCoreApplicationDocumentById(documentId);
            if (document == null) {
                log.error("Document not found with ID: {}", documentId);
                return ResponseEntity.notFound().build();
            }

            log.info("Document found: {}", document.getDocumentUrl());

            // Get file path - handle both absolute and relative paths
            Path filePath;
            String documentUrl = document.getDocumentUrl();

            if (documentUrl.startsWith("/") || documentUrl.contains(":")) {
                // Absolute path
                filePath = Paths.get(documentUrl);
            } else {
                // Relative path - try project root first, then fall back to backend directory
                Path backendDir = Paths.get(System.getProperty("user.dir"));
                Path projectRoot = backendDir.getParent();
                filePath = projectRoot.resolve(documentUrl);

                // Check if file exists at project root
                if (!Files.exists(filePath)) {
                    log.warn("File not found at project root: {}, trying backend directory", filePath.toAbsolutePath());
                    // Fallback: try from backend directory
                    filePath = backendDir.resolve(documentUrl);
                }
            }

            log.info("Looking for file at: {}", filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                log.error("File not found at path: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.error("Resource not readable: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("Serving file: {} with content type: {}", filePath.getFileName(), contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error viewing receipt document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get applications with pending payment approvals (for accountants)
     */
    @GetMapping("/pending-payments")
    public ResponseEntity<?> getPendingPayments() {
        try {
            log.info("=== ACCOUNTANT ACCESSING PENDING PAYMENTS ===");
            log.info("Fetching payments with AWAITING_APPROVAL status from core_application_payment...");

            List<CoreApplicationPayment> allPayments = paymentService.getCoreApplicationPayments(0, 1000);
            List<Map<String, Object>> pendingPayments = new ArrayList<>();

            for (CoreApplicationPayment payment : allPayments) {
                // Only check core_application_payment status - no document checking
                if ("AWAITING_APPROVAL".equals(payment.getPaymentStatus()) &&
                        payment.getCoreLicenseApplication() != null) {

                    CoreLicenseApplication app = payment.getCoreLicenseApplication();
                    Map<String, Object> paymentInfo = new HashMap<>();
                    paymentInfo.put("applicationId", app.getId());
                    paymentInfo.put("paymentId", payment.getId());

                    String applicantName = "Unknown";
                    if (app.getSysUserAccount() != null) {
                        String firstName = app.getSysUserAccount().getFirstName() != null ? app.getSysUserAccount().getFirstName() : "";
                        String lastName = app.getSysUserAccount().getLastName() != null ? app.getSysUserAccount().getLastName() : "";
                        applicantName = (firstName + " " + lastName).trim();
                        if (applicantName.isEmpty()) {
                            applicantName = app.getSysUserAccount().getUsername();
                        }
                    }
                    paymentInfo.put("applicantName", applicantName);
                    paymentInfo.put("id", payment.getId());
                    paymentInfo.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
                    paymentInfo.put("paymentMethod", payment.getPaymentMethod());
                    paymentInfo.put("amountPaid", payment.getAmountPaid());
                    paymentInfo.put("receiptDocumentId", payment.getReceiptDocumentId());
                    paymentInfo.put("uploadedAt", payment.getDateCreated());

                    pendingPayments.add(paymentInfo);
                }
            }

            log.info("Found {} applications with AWAITING_APPROVAL payments", pendingPayments.size());
            return ResponseEntity.ok(Map.of("pendingPayments", pendingPayments));

        } catch (Exception e) {
            log.error("Error getting pending payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/pending-l-payments/{applicationId}")
    public ResponseEntity<?> lecencePayment(
            @PathVariable String applicationId
    ) {
        try {
            log.info("Getting applications with pending payment approvals");

            List<CoreApplicationPayment> allPayments = ((CoreApplicationPaymentServiceImpl) paymentService).getLicenseFeePayments(applicationId);
            List<Map<String, Object>> pendingPayments = new ArrayList<>();
            for (CoreApplicationPayment payment : allPayments) {

                CoreLicenseApplication app = payment.getCoreLicenseApplication();
                Map<String, Object> paymentInfo = new HashMap<>();
                paymentInfo.put("applicationId", app.getId());
                String applicantName = "Unknown";
                if (app.getSysUserAccount() != null) {
                    String firstName = app.getSysUserAccount().getFirstName() != null ? app.getSysUserAccount().getFirstName() : "";
                    String lastName = app.getSysUserAccount().getLastName() != null ? app.getSysUserAccount().getLastName() : "";
                    applicantName = (firstName + " " + lastName).trim();
                    if (applicantName.isEmpty()) {
                        applicantName = app.getSysUserAccount().getUsername();
                    }
                }
                paymentInfo.put("applicantName", applicantName);
                paymentInfo.put("id", payment.getId());
                paymentInfo.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
                paymentInfo.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "Manual Upload");

                // For AWAITING_APPROVAL payments, show the license fee amount that needs to be paid
                Double licenseFeeAmount = null;

                // Use application-specific license fee if set by manager, otherwise 0
                if (app.getLicenseFee() != null) {
                    licenseFeeAmount = app.getLicenseFee();
                } else if (payment.getAmountPaid() > 0) {
                    licenseFeeAmount = payment.getAmountPaid();
                } else {
                    licenseFeeAmount = 0.0;
                }
                paymentInfo.put("amountPaid", licenseFeeAmount);
                paymentInfo.put("amountToBePaid", licenseFeeAmount);

                paymentInfo.put("receiptDocumentId", payment.getReceiptDocumentId());
                paymentInfo.put("uploadedAt", payment.getDateCreated());
                paymentInfo.put("paymentStatus", payment.getPaymentStatus());
                paymentInfo.put("notes", payment.getVerificationNotes() != null ? payment.getVerificationNotes() : "");

                pendingPayments.add(paymentInfo);

            }

            return ResponseEntity.ok(Map.of("pendingPayments", pendingPayments));

        } catch (Exception e) {
            log.error("Error getting pending payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/pending-a-payments/{applicationId}")
    public ResponseEntity<?> applicationPayment(
            @PathVariable String applicationId
    ) {
        try {
            log.info("=== ACCOUNTANT CLICKED VIEW APPLICATION PAYMENT ===");
            log.info("Application ID: {}", applicationId);
            log.info("Endpoint: /pending-a-payments/{}", applicationId);
            log.info("Starting to fetch application fee payments...");

            log.info("Step 1: Calling getApplicationFeePayments for applicationId: {}", applicationId);
            List<CoreApplicationPayment> allPayments = ((CoreApplicationPaymentServiceImpl) paymentService).getApplicationFeePayments(applicationId);
            log.info("Step 1 COMPLETED: Found {} application fee payments", allPayments != null ? allPayments.size() : 0);

            List<Map<String, Object>> pendingPayments = new ArrayList<>();
            log.info("Step 2: Processing payments to extract receipt information...");

            int paymentIndex = 0;
            for (CoreApplicationPayment payment : allPayments) {
                paymentIndex++;
                log.info("Step 2.{}: Processing payment {} with ID: {}", paymentIndex, paymentIndex, payment.getId());

                log.info("Step 2.{}.1: Getting license application for payment {}", paymentIndex, payment.getId());
                CoreLicenseApplication app = payment.getCoreLicenseApplication();
                log.info("Step 2.{}.1 COMPLETED: License application ID: {}", paymentIndex, app != null ? app.getId() : "null");

                Map<String, Object> paymentInfo = new HashMap<>();
                paymentInfo.put("applicationId", app.getId());

                log.info("Step 2.{}.2: Getting applicant name for payment {}", paymentIndex, payment.getId());
                String applicantName = "Unknown";
                if (app.getSysUserAccount() != null) {
                    String firstName = app.getSysUserAccount().getFirstName() != null ? app.getSysUserAccount().getFirstName() : "";
                    String lastName = app.getSysUserAccount().getLastName() != null ? app.getSysUserAccount().getLastName() : "";
                    applicantName = (firstName + " " + lastName).trim();
                    if (applicantName.isEmpty()) {
                        applicantName = app.getSysUserAccount().getUsername();
                    }
                }
                log.info("Step 2.{}.2 COMPLETED: Applicant name: {}", paymentIndex, applicantName);

                paymentInfo.put("applicantName", applicantName);
                paymentInfo.put("id", payment.getId());
                paymentInfo.put("licenseType", app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : "Unknown");
                paymentInfo.put("paymentMethod", payment.getPaymentMethod());
                paymentInfo.put("amountPaid", payment.getAmountPaid());
                paymentInfo.put("receiptDocumentId", payment.getReceiptDocumentId());
                paymentInfo.put("uploadedAt", payment.getDateCreated());

                log.info("Step 2.{}.3: Adding payment info to response list", paymentIndex);
                pendingPayments.add(paymentInfo);
                log.info("Step 2.{} COMPLETED: Payment {} processed successfully", paymentIndex, payment.getId());
            }

            log.info("Step 3: Returning response with {} pending payments", pendingPayments.size());
            log.info("=== VIEW APPLICATION PAYMENT REQUEST COMPLETED SUCCESSFULLY ===");
            return ResponseEntity.ok(Map.of("pendingPayments", pendingPayments));

        } catch (Exception e) {
            log.error("Error getting pending payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Find license type activity by sequence number
     */
    private CoreLicenseTypeActivity findLicenseTypeActivityBySequence(
            CoreLicenseType licenseType, int sequenceNumber) {
        try {
            if (licenseType == null) {
                log.warn("License type is null");
                return null;
            }

            List<CoreLicenseTypeActivity> activities = coreLicenseTypeActivityService.getAllCoreLicenseTypeActivities(0, 100);
            log.info("Found {} total license type activities", activities.size());

            for (CoreLicenseTypeActivity activity : activities) {
                if (activity.getCoreLicenseType() != null &&
                        licenseType.getId().equals(activity.getCoreLicenseType().getId())) {
                    log.info("Found matching activity: {} for license type: {}", activity.getName(), licenseType.getName());
                    return activity;
                }
            }
            log.warn("No activities found for license type: {}", licenseType.getName());
        } catch (Exception e) {
            log.error("Error finding license type activity by sequence", e);
        }
        return null;
    }

    /**
     * Find any license type activity for the given license type
     */
    private CoreLicenseTypeActivity findAnyLicenseTypeActivity(CoreLicenseType licenseType) {
        try {
            if (licenseType == null) return null;

            List<CoreLicenseTypeActivity> activities = coreLicenseTypeActivityService.getAllCoreLicenseTypeActivities(0, 100);
            for (CoreLicenseTypeActivity activity : activities) {
                if (activity.getCoreLicenseType() != null &&
                        licenseType.getId().equals(activity.getCoreLicenseType().getId())) {
                    return activity;
                }
            }
        } catch (Exception e) {
            log.error("Error finding license type activity", e);
        }
        return null;
    }

    /**
     * Get application step by sequence number for a license type
     */
    private CoreApplicationStep getApplicationStepBySequence(CoreLicenseType licenseType, int sequenceNumber) {
        try {
            if (licenseType == null) return null;

            List<CoreApplicationStep> steps = applicationStepService.getAllCoreApplicationSteps(0, 100);
            for (CoreApplicationStep step : steps) {
                if (step.getCoreLicenseType() != null &&
                        licenseType.getId().equals(step.getCoreLicenseType().getId()) &&
                        step.getSequenceNumber() == sequenceNumber) {
                    return step;
                }
            }
        } catch (Exception e) {
            log.error("Error getting application step by sequence", e);
        }
        return null;
    }

    /**
     * Approve application - move to next step or complete
     */
    @PostMapping("/approve/{applicationId}")
    public ResponseEntity<?> approveApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request) {
        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            CoreApplicationStep currentStep = application.getCoreApplicationStep();
            if (currentStep == null) {
                log.error("Application {} has no current step for approval", applicationId);
                // Assign first step if none exists
                CoreApplicationStep firstStep = getApplicationStepBySequence(application.getCoreLicenseType(), 1);
                if (firstStep != null) {
                    application.setCoreApplicationStep(firstStep);
                    applicationService.editCoreLicenseApplication(application);
                    currentStep = firstStep;
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", "Application has no workflow steps configured"));
                }
            }

            CoreApplicationStep nextStep = getApplicationStepBySequence(application.getCoreLicenseType(), currentStep.getSequenceNumber() + 1);

            if (nextStep != null) {
                // Move to next step
                application.setCoreApplicationStep(nextStep);
            } else {
                // Final approval - update status to approved
                CoreApplicationStatus approvedStatus = coreApplicationStatusService.getCoreApplicationStatusByName("Approved");
                if (approvedStatus != null) {
                    application.setCoreApplicationStatus(approvedStatus);
                }
            }

            applicationService.editCoreLicenseApplication(application);

            // Notify officers about application moving to next stage
            try {
                if (nextStep != null) {
                    log.info("=== NOTIFYING OFFICERS ABOUT APPLICATION APPROVAL ===");
                    log.info("Application moved to step: {}", nextStep.getName());

                    String officerRole = mapStepToOfficerRole(nextStep.getName());
                    if (officerRole != null) {
                        log.info("Notifying officers in role: {}", officerRole);
                        officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                    }
                }
            } catch (Exception notificationError) {
                log.error("Error notifying officers for approved application {}: {}", applicationId, notificationError.getMessage());
                // Continue with the response even if officer notification fails
            }

            // Log activity
            CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "APPROVE");
            if (activityType != null) {
                CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                activity.setCoreLicenseApplication(application);
                activity.setCoreLicenseTypeActivity(activityType);
                String description = "Application approved" + (nextStep != null ? " and moved to " + nextStep.getName() : " - Final approval");
                if (request.containsKey("comment")) {
                    description += ". Comment: " + request.get("comment").toString();
                }
                activity.setDescription(description);
                activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
            }

            return ResponseEntity.ok(Map.of("message", "Application approved successfully"));
        } catch (Exception e) {
            log.error("Error approving application: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Refer back application to previous user (internal referral system)
     */
    @PostMapping("/refer-back/{applicationId}")
    public ResponseEntity<?> referBackApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }


            SysUserAccount currentUser = getCurrentUser(token);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user"));
            }
            application.clearReferral();

            String referralReason = request.containsKey("comment") ?
                    request.get("comment").toString() : "Clarification needed";

            String action = request.containsKey("action") ?
                    request.get("action").toString() : "";

            String statusName = request.containsKey("status") ?
                    request.get("status").toString() : "";

            if (action.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Action is required"));
            }

            Integer sequence = switch (currentUser.getSysUserGroup().getName()) {
                case "ceo" -> 5;
                case "drs" -> 4;
                case "licensing_manager" -> 3;
                case "senior_licensing_officer" -> 2;
                case "licensing_officer" -> 1;
                case "accountant" -> 0;
                default -> throw new Exception("Cant refer back this application");
            };


            if (application.getCoreApplicationStep().getSequenceNumber() != sequence) {
                throw new Exception("Cant refer back this application");
            }


            application.setReferredFromUserId(currentUser.getId());

            // Determine who to refer back to based on current user's role and assessment data
            String referToUserId = determineReferralTarget(applicationId, currentUser);
            application.setReferredToUserId(referToUserId);

            application.setReferralReason(referralReason);
            application.setReferralDate(new Timestamp(System.currentTimeMillis()));
//            // Update last handled by to current user
            application.setLastHandledByUserId(currentUser.getId());
            CoreApplicationStep currentStep = application.getCoreApplicationStep();
            CoreApplicationStep previousStep = applicationStepService.getPreviousStep(currentStep);
            if ("to_license_officer".equals(action)) {
                application.setCoreApplicationStep(previousStep);
                applicationService.editCoreLicenseApplication(application);

                // Send email notification to the referred user for to_license_officer action
                try {
                    String referredToUserId = application.getReferredToUserId();
                    if (referredToUserId != null && !referredToUserId.isEmpty()) {
                        SysUserAccount referredUser = userAccountService.getSysUserAccountById(referredToUserId);
                        if (referredUser != null && referredUser.getEmailAddress() != null && !referredUser.getEmailAddress().isEmpty()) {
                            String referredUserEmail = referredUser.getEmailAddress();
                            String referredUserName = (referredUser.getFirstName() != null ? referredUser.getFirstName() : "") +
                                    " " + (referredUser.getLastName() != null ? referredUser.getLastName() : "");

                            String currentUserName = (currentUser.getFirstName() != null ? currentUser.getFirstName() : "") +
                                    " " + (currentUser.getLastName() != null ? currentUser.getLastName() : "");

                            String currentUserRole = currentUser.getSysUserGroup() != null ?
                                    currentUser.getSysUserGroup().getName() : "System User";

                            String licenseType = application.getCoreLicenseType() != null ?
                                    application.getCoreLicenseType().getName() : "Water Permit";

                            String subject = "Application Referred Back - Action Required: " + licenseType + " Application #" + applicationId;

                            String emailBody = String.format("""
                                            <!DOCTYPE html>
                                            <html lang="en">
                                            <head>
                                            <meta charset="UTF-8">
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                            <title>Application Referral</title>
                                            <style>
                                            body {
                                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                            line-height: 1.4;
                                            margin: 0;
                                            padding: 10px;
                                            background-color: #f8f9fa;
                                            color: #333;
                                            }
                                            .container {
                                            max-width: 600px;
                                            margin: 0 auto;
                                            background-color: #ffffff;
                                            border-radius: 8px;
                                            overflow: hidden;
                                            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                            }
                                            .header {
                                            background-color: #0ea5e9;
                                            color: white;
                                            padding: 15px 30px;
                                            text-align: center;
                                            }
                                            .header .logo {
                                            width: 80px;
                                            height: 80px;
                                            margin-bottom: 10px;
                                            background-color: white;
                                            padding: 8px;
                                            border-radius: 6px;
                                            }
                                            .header h1 {
                                            margin: 0;
                                            font-size: 24px;
                                            font-weight: 600;
                                            }
                                            .header p {
                                            margin: 8px 0 0 0;
                                            font-size: 16px;
                                            opacity: 0.9;
                                            }
                                            .content {
                                            padding: 20px 30px;
                                            }
                                            .greeting {
                                            font-size: 16px;
                                            margin-bottom: 16px;
                                            }
                                            .status {
                                            background-color: #fef3c7;
                                            color: #92400e;
                                            padding: 10px 15px;
                                            border-radius: 6px;
                                            text-align: center;
                                            font-weight: 600;
                                            margin: 16px 0;
                                            }
                                            .details {
                                            background-color: #f8fafc;
                                            border: 1px solid #e2e8f0;
                                            border-radius: 6px;
                                            padding: 16px;
                                            margin: 16px 0;
                                            }
                                            .details h3 {
                                            margin: 0 0 12px 0;
                                            font-size: 18px;
                                            color: #1e293b;
                                            }
                                            .detail-row {
                                            display: flex;
                                            justify-content: space-between;
                                            padding: 8px 0;
                                            border-bottom: 1px solid #e2e8f0;
                                            }
                                            .detail-row:last-child {
                                            border-bottom: none;
                                            }
                                            .detail-label {
                                            color: #64748b;
                                            }
                                            .detail-value {
                                            color: #1e293b;
                                            font-weight: 500;
                                            }
                                            .action-notice {
                                            background-color: #dbeafe;
                                            border: 1px solid #3b82f6;
                                            border-radius: 6px;
                                            padding: 20px;
                                            margin: 24px 0;
                                            text-align: center;
                                            }
                                            .action-notice p {
                                            margin: 0;
                                            color: #1e40af;
                                            font-weight: 500;
                                            }
                                            .footer {
                                            background-color: #f1f5f9;
                                            padding: 30px;
                                            text-align: center;
                                            border-top: 1px solid #e2e8f0;
                                            }
                                            .footer p {
                                            margin: 0;
                                            color: #64748b;
                                            font-size: 14px;
                                            }
                                            .footer .org-name {
                                            font-weight: 600;
                                            color: #1e293b;
                                            margin-bottom: 4px;
                                            }
                                            .disclaimer {
                                            font-size: 12px;
                                            color: #94a3b8;
                                            margin-top: 20px;
                                            }
                                            </style>
                                            </head>
                                            <body>
                                            <div class="container">
                                            <div class="header">
                                            <img src="cid:logo" alt="NWRA Logo" class="logo">
                                            <p>National Water Resources Authority</p>
                                            <h1>Application Referral</h1>
                                            </div>
                                            
                                            <div class="content">
                                                <div class="greeting">
                                                    Dear <strong>%s</strong>,
                                                </div>
                                            
                                                <p>An application has been referred back to you for review and action.</p>
                                            
                                                <div class="status">
                                                    Action Required - Review Application
                                                </div>
                                            
                                                <div class="details">
                                                    <h3>Application Details</h3>
                                                    <div class="detail-row">
                                                        <span class="detail-label">Application ID</span>
                                                        <span class="detail-value">%s</span>
                                                    </div>
                                                    <div class="detail-row">
                                                        <span class="detail-label">License Type</span>
                                                        <span class="detail-value">%s</span>
                                                    </div>
                                                    <div class="detail-row">
                                                        <span class="detail-label">Referred by</span>
                                                        <span class="detail-value">%s (%s)</span>
                                                    </div>
                                                    <div class="detail-row">
                                                        <span class="detail-label">Date</span>
                                                        <span class="detail-value">%s</span>
                                                    </div>
                                                </div>
                                            
                                                <div class="details">
                                                    <h3>Reason for Referral</h3>
                                                    <p>%s</p>
                                                </div>
                                            
                                                <div class="action-notice">
                                                    <p>Please log into the NWRA e-Water Permit system to review the application and take the necessary action.</p>
                                                </div>
                                            
                                                <p>If you have any questions, please contact the referring officer.</p>
                                            </div>
                                            
                                            <div class="footer">
                                                <p class="org-name">National Water Resources Authority</p>
                                                <p>License Application System</p>
                                                <p class="disclaimer">
                                                    This is an automated message. Please do not reply to this email.
                                                </p>
                                            </div>
                                            </div>
                                            </body>
                                            </html>
                                            """,
                                    referredUserName.trim(),
                                    applicationId,
                                    licenseType,
                                    currentUserName.trim(),
                                    currentUserRole,
                                    new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm").format(new Date()),
                                    referralReason
                            );

                            // Generate unique task ID for email tracking
                            String taskId = "REFERRAL_TO_LO_" + applicationId + "_" + System.currentTimeMillis();

                            // Send email using the email queue service
                            emailQueueService.sendEmailAsync(taskId, referredUserEmail, subject, emailBody);

                            log.info("Referral notification email sent to license officer: {} for application: {}", referredUserEmail, applicationId);
                        } else {
                            log.warn("No email address found for referred license officer {} of application: {}", referredToUserId, applicationId);
                        }
                    } else {
                        log.warn("No referred user ID found for license officer referral of application: {}", applicationId);
                    }
                } catch (Exception emailError) {
                    log.error("Error sending referral notification email to license officer for application {}: {}", applicationId, emailError.getMessage());
                    // Continue with the response even if email fails
                }

                return ResponseEntity.ok(Map.of(
                        "message", "Application referred back successfully",
                        "applicationId", applicationId,
                        "referredTo", application.getReferredToUserId(),
                        "reason", referralReason
                ));
            }
            CoreApplicationStatus referredBackStatus = coreApplicationStatusService.getCoreApplicationStatusByName(statusName);
            if (referredBackStatus != null) {
                application.setCoreApplicationStatus(referredBackStatus);
                log.info("Updated status to {}", statusName);
            } else {
                log.warn("{} status not found in database", statusName);
            }

            // If current user is licensing manager and current step is > 2, don't go below step 2 (Senior License Officer)
            if (currentUser.getSysUserGroup() != null &&
                    "licensing_manager".equalsIgnoreCase(currentUser.getSysUserGroup().getName()) &&
                    currentStep.getSequenceNumber() > 2 &&
                    (previousStep == null || previousStep.getSequenceNumber() < 2)) {
                // Keep at senior license officer level (sequence 2)
                CoreApplicationStep seniorLicenseOfficerStep = getApplicationStepBySequence(currentStep.getCoreLicenseType(), 2);
                if (seniorLicenseOfficerStep != null) {
                    application.setCoreApplicationStep(seniorLicenseOfficerStep);
                } else {
                    application.setCoreApplicationStep(currentStep); // Keep current if step 2 not found
                }
            } else if (previousStep != null) {
                application.setCoreApplicationStep(previousStep);
            }
            applicationService.editCoreLicenseApplication(application);
            // Log activity
            CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "REFER_BACK");
            if (activityType != null) {
                CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                activity.setCoreLicenseApplication(application);
                activity.setSysUserAccount(currentUser);
                activity.setCoreLicenseTypeActivity(activityType);
                activity.setDescription("Application referred back for clarification: " + referralReason);
                activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
            }

            // Send email notification to the referred user
            try {
                String referredToUserId = application.getReferredToUserId();
                if (referredToUserId != null && !referredToUserId.isEmpty()) {
                    SysUserAccount referredUser = userAccountService.getSysUserAccountById(referredToUserId);
                    if (referredUser != null && referredUser.getEmailAddress() != null && !referredUser.getEmailAddress().isEmpty()) {
                        String referredUserEmail = referredUser.getEmailAddress();
                        String referredUserName = (referredUser.getFirstName() != null ? referredUser.getFirstName() : "") +
                                " " + (referredUser.getLastName() != null ? referredUser.getLastName() : "");

                        String currentUserName = (currentUser.getFirstName() != null ? currentUser.getFirstName() : "") +
                                " " + (currentUser.getLastName() != null ? currentUser.getLastName() : "");

                        String currentUserRole = currentUser.getSysUserGroup() != null ?
                                currentUser.getSysUserGroup().getName() : "System User";

                        String licenseType = application.getCoreLicenseType() != null ?
                                application.getCoreLicenseType().getName() : "Water Permit";

                        String subject = "Application Referred Back - Action Required: " + licenseType + " Application #" + applicationId;

                        String emailBody = String.format("""
                                        <!DOCTYPE html>
                                        <html lang="en">
                                        <head>
                                        <meta charset="UTF-8">
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <title>Application Referral</title>
                                        <style>
                                        body {
                                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                        line-height: 1.4;
                                        margin: 0;
                                        padding: 10px;
                                        background-color: #f8f9fa;
                                        color: #333;
                                        }
                                        .container {
                                        max-width: 600px;
                                        margin: 0 auto;
                                        background-color: #ffffff;
                                        border-radius: 8px;
                                        overflow: hidden;
                                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                        }
                                        .header {
                                        background-color: #0ea5e9;
                                        color: white;
                                        padding: 15px 30px;
                                        text-align: center;
                                        }
                                        .header .logo {
                                        width: 80px;
                                        height: 80px;
                                        margin-bottom: 10px;
                                        background-color: white;
                                        padding: 8px;
                                        border-radius: 6px;
                                        }
                                        .header h1 {
                                        margin: 0;
                                        font-size: 24px;
                                        font-weight: 600;
                                        }
                                        .header p {
                                        margin: 8px 0 0 0;
                                        font-size: 16px;
                                        opacity: 0.9;
                                        }
                                        .content {
                                        padding: 20px 30px;
                                        }
                                        .greeting {
                                        font-size: 16px;
                                        margin-bottom: 16px;
                                        }
                                        .status {
                                        background-color: #fef3c7;
                                        color: #92400e;
                                        padding: 10px 15px;
                                        border-radius: 6px;
                                        text-align: center;
                                        font-weight: 600;
                                        margin: 16px 0;
                                        }
                                        .details {
                                        background-color: #f8fafc;
                                        border: 1px solid #e2e8f0;
                                        border-radius: 6px;
                                        padding: 16px;
                                        margin: 16px 0;
                                        }
                                        .details h3 {
                                        margin: 0 0 12px 0;
                                        font-size: 18px;
                                        color: #1e293b;
                                        }
                                        .detail-row {
                                        display: flex;
                                        justify-content: space-between;
                                        padding: 8px 0;
                                        border-bottom: 1px solid #e2e8f0;
                                        }
                                        .detail-row:last-child {
                                        border-bottom: none;
                                        }
                                        .detail-label {
                                        color: #64748b;
                                        }
                                        .detail-value {
                                        color: #1e293b;
                                        font-weight: 500;
                                        }
                                        .action-notice {
                                        background-color: #dbeafe;
                                        border: 1px solid #3b82f6;
                                        border-radius: 6px;
                                        padding: 20px;
                                        margin: 24px 0;
                                        text-align: center;
                                        }
                                        .action-notice p {
                                        margin: 0;
                                        color: #1e40af;
                                        font-weight: 500;
                                        }
                                        .footer {
                                        background-color: #f1f5f9;
                                        padding: 30px;
                                        text-align: center;
                                        border-top: 1px solid #e2e8f0;
                                        }
                                        .footer p {
                                        margin: 0;
                                        color: #64748b;
                                        font-size: 14px;
                                        }
                                        .footer .org-name {
                                        font-weight: 600;
                                        color: #1e293b;
                                        margin-bottom: 4px;
                                        }
                                        .disclaimer {
                                        font-size: 12px;
                                        color: #94a3b8;
                                        margin-top: 20px;
                                        }
                                        </style>
                                        </head>
                                        <body>
                                        <div class="container">
                                        <div class="header">
                                        <img src="cid:logo" alt="NWRA Logo" class="logo">
                                        <p>National Water Resources Authority</p>
                                        <h1>Application Referral</h1>
                                        </div>
                                        
                                        <div class="content">
                                            <div class="greeting">
                                                Dear <strong>%s</strong>,
                                            </div>
                                        
                                            <p>An application has been referred back to you for review and action.</p>
                                        
                                            <div class="status">
                                                Action Required - Review Application
                                            </div>
                                        
                                            <div class="details">
                                                <h3>Application Details</h3>
                                                <div class="detail-row">
                                                    <span class="detail-label">Application ID</span>
                                                    <span class="detail-value">%s</span>
                                                </div>
                                                <div class="detail-row">
                                                    <span class="detail-label">License Type</span>
                                                    <span class="detail-value">%s</span>
                                                </div>
                                                <div class="detail-row">
                                                    <span class="detail-label">Referred by</span>
                                                    <span class="detail-value">%s (%s)</span>
                                                </div>
                                                <div class="detail-row">
                                                    <span class="detail-label">Date</span>
                                                    <span class="detail-value">%s</span>
                                                </div>
                                            </div>
                                        
                                            <div class="details">
                                                <h3>Reason for Referral</h3>
                                                <p>%s</p>
                                            </div>
                                        
                                            <div class="action-notice">
                                                <p>Please log into the NWRA e-Water Permit system to review the application and take the necessary action.</p>
                                            </div>
                                        
                                            <p>If you have any questions, please contact the referring officer.</p>
                                        </div>
                                        
                                        <div class="footer">
                                            <p class="org-name">National Water Resources Authority</p>
                                            <p>License Application System</p>
                                            <p class="disclaimer">
                                                This is an automated message. Please do not reply to this email.
                                            </p>
                                        </div>
                                        </div>
                                        </body>
                                        </html>
                                        """,
                                referredUserName.trim(),
                                applicationId,
                                licenseType,
                                currentUserName.trim(),
                                currentUserRole,
                                new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm").format(new Date()),
                                referralReason
                        );

                        // Generate unique task ID for email tracking
                        String taskId = "REFERRAL_" + applicationId + "_" + System.currentTimeMillis();

                        // Send email using the email queue service
                        emailQueueService.sendEmailAsync(taskId, referredUserEmail, subject, emailBody);

                        log.info("Referral notification email sent to: {} for application: {}", referredUserEmail, applicationId);
                    } else {
                        log.warn("No email address found for referred user {} of application: {}", referredToUserId, applicationId);
                    }
                } else {
                    log.warn("No referred user ID found for application: {}", applicationId);
                }
            } catch (Exception emailError) {
                log.error("Error sending referral notification email for application {}: {}", applicationId, emailError.getMessage());
                // Continue with the response even if email fails
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Application referred back successfully",
                    "applicationId", applicationId,
                    "referredTo", application.getReferredToUserId(),
                    "reason", referralReason
            ));

        } catch (Exception e) {
            log.error("Error referring back application", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to refer back: " + e.getMessage()));
        }
    }

    /**
     * Reject application
     * Only accessible by: licensing_officer, senior_licensing_officer, drs, licensing_manager, ceo
     */
    @PostMapping("/reject/{applicationId}")
    public ResponseEntity<?> rejectApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            // Get current user and check role authorization
            SysUserAccount currentUser = getCurrentUser(token);
            if (currentUser == null || currentUser.getSysUserGroup() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String userRole = currentUser.getSysUserGroup().getName().toLowerCase();
            List<String> allowedRoles = Arrays.asList(
                "licensing_officer",
                "senior_licensing_officer",
                "drs",
                "licensing_manager",
                "ceo"
            );

            if (!allowedRoles.contains(userRole)) {
                log.warn("User with role '{}' attempted to reject application {}", userRole, applicationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not have permission to reject applications. Only licensing officers, senior licensing officers, DRS, licensing managers, and CEOs can reject applications."));
            }

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Check if application is already rejected
            if (application.getCoreApplicationStatus() != null &&
                "REJECTED".equalsIgnoreCase(application.getCoreApplicationStatus().getName())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Application is already rejected"));
            }

            // Update status to rejected
            CoreApplicationStatus rejectedStatus = coreApplicationStatusService.getCoreApplicationStatusByName("REJECTED");
            if (rejectedStatus != null) {
                application.setCoreApplicationStatus(rejectedStatus);
            }

            applicationService.editCoreLicenseApplication(application);

            // Log activity
            CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "REJECT");
            if (activityType != null) {
                CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                activity.setCoreLicenseApplication(application);
                activity.setSysUserAccount(getCurrentUser(token));
                activity.setCoreLicenseTypeActivity(activityType);
                String description = "Application rejected";
                if (request.containsKey("comment")) {
                    description += ". Reason: " + request.get("comment").toString();
                }
                activity.setDescription(description);
                activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
            }

            // Send email notification to applicant about application rejection
            try {
                if (application.getSysUserAccount() != null &&
                        application.getSysUserAccount().getEmailAddress() != null) {

                    String applicantEmail = application.getSysUserAccount().getEmailAddress();
                    String applicantName = getApplicantFullName(application.getSysUserAccount());
                    String licenseType = application.getCoreLicenseType() != null ?
                            application.getCoreLicenseType().getName() : "Water Permit";
                    String rejectionReason = request.containsKey("comment") ?
                            request.get("comment").toString() : "Application does not meet our requirements";

                    String subject = "Application Rejected - " + licenseType + " Application";
                    String emailBody = String.format("""
                            <!DOCTYPE html>
                            <html lang="en">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Application Rejected</title>
                                <style>
                                    body {
                                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                        line-height: 1.6;
                                        margin: 0;
                                        padding: 20px;
                                        background-color: #f8f9fa;
                                        color: #333;
                                    }
                                    .container {
                                        max-width: 600px;
                                        margin: 0 auto;
                                        background-color: #ffffff;
                                        border-radius: 8px;
                                        overflow: hidden;
                                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                    }
                                    .header {
                                        background-color: #0ea5e9;
                                        color: white;
                                        padding: 15px 30px;
                                        text-align: center;
                                    }
                                    .header .logo {
                                        width: 80px;
                                        height: 80px;
                                        margin-bottom: 10px;
                                        background-color: white;
                                        padding: 8px;
                                        border-radius: 6px;
                                    }
                                    .header h1 {
                                        margin: 0;
                                        font-size: 24px;
                                        font-weight: 600;
                                    }
                                    .header p {
                                        margin: 8px 0 0 0;
                                        font-size: 16px;
                                        opacity: 0.9;
                                    }
                                    .content {
                                        padding: 40px 30px;
                                    }
                                    .greeting {
                                        font-size: 16px;
                                        margin-bottom: 24px;
                                    }
                                    .status {
                                        background-color: #fee2e2;
                                        color: #991b1b;
                                        padding: 12px 20px;
                                        border-radius: 6px;
                                        text-align: center;
                                        font-weight: 600;
                                        margin: 24px 0;
                                    }
                                    .details {
                                        background-color: #f8fafc;
                                        border: 1px solid #e2e8f0;
                                        border-radius: 6px;
                                        padding: 24px;
                                        margin: 24px 0;
                                    }
                                    .details h3 {
                                        margin: 0 0 20px 0;
                                        font-size: 18px;
                                        color: #1e293b;
                                    }
                                    .detail-table {
                                        width: 100%%;
                                        border-collapse: collapse;
                                    }
                                    .detail-row {
                                        border-bottom: 1px solid #e2e8f0;
                                    }
                                    .detail-row:last-child {
                                        border-bottom: none;
                                    }
                                    .detail-row td {
                                        padding: 12px 8px;
                                        vertical-align: top;
                                    }
                                    .detail-label {
                                        color: #64748b;
                                        width: 45%%;
                                    }
                                    .detail-value {
                                        color: #1e293b;
                                        font-weight: 500;
                                        text-align: right;
                                        width: 55%%;
                                    }
                                    .rejection-notice {
                                        background-color: #fee2e2;
                                        border: 1px solid #dc2626;
                                        border-radius: 6px;
                                        padding: 20px;
                                        margin: 24px 0;
                                    }
                                    .rejection-notice p {
                                        margin: 0;
                                        color: #991b1b;
                                    }
                                    .next-steps {
                                        background-color: #fef3c7;
                                        border: 1px solid #f59e0b;
                                        border-radius: 6px;
                                        padding: 20px;
                                        margin: 24px 0;
                                    }
                                    .next-steps h3 {
                                        margin: 0 0 12px 0;
                                        color: #92400e;
                                    }
                                    .next-steps ol {
                                        margin: 0;
                                        padding-left: 20px;
                                        color: #92400e;
                                    }
                                    .footer {
                                        background-color: #f1f5f9;
                                        padding: 30px;
                                        text-align: center;
                                        border-top: 1px solid #e2e8f0;
                                    }
                                    .footer p {
                                        margin: 0;
                                        color: #64748b;
                                        font-size: 14px;
                                    }
                                    .footer .org-name {
                                        font-weight: 600;
                                        color: #1e293b;
                                        margin-bottom: 4px;
                                    }
                                    .disclaimer {
                                        font-size: 12px;
                                        color: #94a3b8;
                                        margin-top: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <img src="cid:logo" alt="NWRA Logo" style="width: 80px; height: 80px; margin-bottom: 10px; background-color: white; padding: 8px; border-radius: 6px; display: block; margin-left: auto; margin-right: auto;">
                                        <p>National Water Resources Authority</p>
                                        <h1>❌ Application Rejected</h1>
                                    </div>

                                    <div class="content">
                                        <div class="greeting">
                                            Dear <strong>%s</strong>,
                                        </div>

                                        <p>We regret to inform you that your %s application has been rejected during our review process.</p>

                                        <div class="status">
                                            Application Status: REJECTED
                                        </div>

                                        <div class="details">
                                            <h3>Application Summary</h3>
                                            <table class="detail-table">
                                                <tr class="detail-row">
                                                    <td class="detail-label">Application ID</td>
                                                    <td class="detail-value">%s</td>
                                                </tr>
                                                <tr class="detail-row">
                                                    <td class="detail-label">License Type</td>
                                                    <td class="detail-value">%s</td>
                                                </tr>
                                            </table>
                                        </div>

                                        <div class="rejection-notice">
                                            <p><strong>Rejection Reason:</strong></p>
                                            <p>%s</p>
                                        </div>

                                        <div class="next-steps">
                                            <h3>What to do next:</h3>
                                            <ol>
                                                <li>Review the rejection reason carefully</li>
                                                <li>Address the identified issues in your application</li>
                                                <li>Resubmit your application with the necessary corrections</li>
                                                <li>Ensure all required documentation and information is complete</li>
                                            </ol>
                                        </div>

                                        <p>You can submit a new application by logging into the system.</p>

                                        <p>If you need clarification about the rejection reason or have questions about the resubmission process, please contact our support team.</p>

                                        <p>Thank you for your understanding.</p>
                                    </div>

                                    <div class="footer">
                                        <p class="org-name">National Water Resources Authority</p>
                                        <p>License Application System</p>
                                        <p class="disclaimer">
                                            This is an automated message. Please do not reply to this email.
                                        </p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                            applicantName, licenseType, applicationId, licenseType, rejectionReason
                    );

                    String taskId = "application-rejected-" + applicationId + "-" + System.currentTimeMillis();
                    emailQueueService.sendEmailAsync(taskId, applicantEmail, subject, emailBody);

                    log.info("❌ Application rejection email sent to: {} for application: {}", applicantEmail, applicationId);
                } else {
                    log.warn("No email address found for applicant of application: {}", applicationId);
                }
            } catch (Exception emailError) {
                log.error("Error sending application rejection email for application {}: {}", applicationId, emailError.getMessage());
                // Continue with the response even if email fails
            }

            return ResponseEntity.ok(Map.of("message", "Application rejected successfully"));
        } catch (Exception e) {
            log.error("Error rejecting application: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/request-revision/{applicationId}")
    public ResponseEntity<?> requestRevision(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Update status to needs revision
            CoreApplicationStatus revisionStatus = coreApplicationStatusService.getCoreApplicationStatusByName("NEEDS_REVISION");
            if (revisionStatus != null) {
                application.setCoreApplicationStatus(revisionStatus);
            }

            applicationService.editCoreLicenseApplication(application);

            // Log activity
            CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "REQUEST_REVISION");
            if (activityType != null) {
                CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                activity.setCoreLicenseApplication(application);
                activity.setSysUserAccount(getCurrentUser(token));
                activity.setCoreLicenseTypeActivity(activityType);
                String description = "Application requires revision";
                if (request.containsKey("comment")) {
                    description += ". Details: " + request.get("comment").toString();
                }
                activity.setDescription(description);
                activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
            }

            // Send email notification to applicant about revision request
            try {
                if (application.getSysUserAccount() != null &&
                        application.getSysUserAccount().getEmailAddress() != null) {

                    String applicantEmail = application.getSysUserAccount().getEmailAddress();
                    String applicantName = getApplicantFullName(application.getSysUserAccount());
                    String licenseType = application.getCoreLicenseType() != null ?
                            application.getCoreLicenseType().getName() : "Water Permit";
                    String revisionDetails = request.containsKey("comment") ?
                            request.get("comment").toString() : "Please review and update your application as needed";

                    String subject = "Application Revision Required - " + licenseType + " Application";
                    String emailBody = String.format("""
                            <!DOCTYPE html>
                            <html lang="en">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Application Revision Required</title>
                                <style>
                                    body {
                                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                        line-height: 1.6;
                                        margin: 0;
                                        padding: 20px;
                                        background-color: #f8f9fa;
                                        color: #333;
                                    }
                                    .container {
                                        max-width: 600px;
                                        margin: 0 auto;
                                        background-color: #ffffff;
                                        border-radius: 8px;
                                        overflow: hidden;
                                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                    }
                                    .header {
                                        background-color: #f59e0b;
                                        color: white;
                                        padding: 15px 30px;
                                        text-align: center;
                                    }
                                    .header .logo {
                                        width: 80px;
                                        height: 80px;
                                        margin-bottom: 10px;
                                        background-color: white;
                                        padding: 8px;
                                        border-radius: 6px;
                                    }
                                    .header h1 {
                                        margin: 0;
                                        font-size: 24px;
                                        font-weight: 600;
                                    }
                                    .header p {
                                        margin: 8px 0 0 0;
                                        font-size: 16px;
                                        opacity: 0.9;
                                    }
                                    .content {
                                        padding: 40px 30px;
                                    }
                                    .greeting {
                                        font-size: 16px;
                                        margin-bottom: 24px;
                                    }
                                    .status {
                                        background-color: #fef3c7;
                                        color: #92400e;
                                        padding: 12px 20px;
                                        border-radius: 6px;
                                        text-align: center;
                                        font-weight: 600;
                                        margin: 24px 0;
                                    }
                                    .details {
                                        background-color: #f8fafc;
                                        border: 1px solid #e2e8f0;
                                        border-radius: 6px;
                                        padding: 24px;
                                        margin: 24px 0;
                                    }
                                    .details h3 {
                                        margin: 0 0 20px 0;
                                        font-size: 18px;
                                        color: #1e293b;
                                    }
                                    .detail-table {
                                        width: 100%%;
                                        border-collapse: collapse;
                                    }
                                    .detail-row {
                                        border-bottom: 1px solid #e2e8f0;
                                    }
                                    .detail-row:last-child {
                                        border-bottom: none;
                                    }
                                    .detail-row td {
                                        padding: 12px 8px;
                                        vertical-align: top;
                                    }
                                    .detail-label {
                                        color: #64748b;
                                        width: 45%%;
                                    }
                                    .detail-value {
                                        color: #1e293b;
                                        font-weight: 500;
                                        text-align: right;
                                        width: 55%%;
                                    }
                                    .revision-notice {
                                        background-color: #fef3c7;
                                        border: 1px solid #f59e0b;
                                        border-radius: 6px;
                                        padding: 20px;
                                        margin: 24px 0;
                                    }
                                    .revision-notice p {
                                        margin: 0;
                                        color: #92400e;
                                    }
                                    .next-steps {
                                        background-color: #dbeafe;
                                        border: 1px solid #3b82f6;
                                        border-radius: 6px;
                                        padding: 20px;
                                        margin: 24px 0;
                                    }
                                    .next-steps h3 {
                                        margin: 0 0 12px 0;
                                        color: #1e40af;
                                    }
                                    .next-steps ol {
                                        margin: 0;
                                        padding-left: 20px;
                                        color: #1e40af;
                                    }
                                    .footer {
                                        background-color: #f1f5f9;
                                        padding: 30px;
                                        text-align: center;
                                        border-top: 1px solid #e2e8f0;
                                    }
                                    .footer p {
                                        margin: 0;
                                        color: #64748b;
                                        font-size: 14px;
                                    }
                                    .footer .org-name {
                                        font-weight: 600;
                                        color: #1e293b;
                                        margin-bottom: 4px;
                                    }
                                    .disclaimer {
                                        font-size: 12px;
                                        color: #94a3b8;
                                        margin-top: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <img src="cid:logo" alt="NWRA Logo" style="width: 80px; height: 80px; margin-bottom: 10px; background-color: white; padding: 8px; border-radius: 6px; display: block; margin-left: auto; margin-right: auto;">
                                        <p>National Water Resources Authority</p>
                                        <h1>⚠ Revision Required</h1>
                                    </div>

                                    <div class="content">
                                        <div class="greeting">
                                            Dear <strong>%s</strong>,
                                        </div>

                                        <p>Your %s application requires some revisions before we can continue processing.</p>

                                        <div class="status">
                                            Application Status: REVISION REQUIRED
                                        </div>

                                        <div class="details">
                                            <h3>Application Summary</h3>
                                            <table class="detail-table">
                                                <tr class="detail-row">
                                                    <td class="detail-label">Application ID</td>
                                                    <td class="detail-value">%s</td>
                                                </tr>
                                                <tr class="detail-row">
                                                    <td class="detail-label">License Type</td>
                                                    <td class="detail-value">%s</td>
                                                </tr>
                                            </table>
                                        </div>

                                        <div class="revision-notice">
                                            <p><strong>Revision Details:</strong></p>
                                            <p>%s</p>
                                        </div>

                                        <div class="next-steps">
                                            <h3>What to do next:</h3>
                                            <ol>
                                                <li>Review the revision details carefully</li>
                                                <li>Make the necessary updates to your application</li>
                                                <li>Resubmit your application with the requested changes</li>
                                                <li>Ensure all required documentation is updated as needed</li>
                                            </ol>
                                        </div>

                                        <p><strong>Important:</strong> Since you have already paid the application fee, no additional payment is required. Simply make the requested revisions and resubmit.</p>

                                        <p>You can access your application by logging into the system with your application ID.</p>

                                        <p>If you need clarification about any revision requirements, please contact our support team.</p>

                                        <p>Thank you for your cooperation.</p>
                                    </div>

                                    <div class="footer">
                                        <p class="org-name">National Water Resources Authority</p>
                                        <p>License Application System</p>
                                        <p class="disclaimer">
                                            This is an automated message. Please do not reply to this email.
                                        </p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                            applicantName, licenseType, applicationId, licenseType, revisionDetails
                    );

                    String taskId = "revision-" + applicationId + "-" + System.currentTimeMillis();
                    emailQueueService.sendEmailAsync(taskId, applicantEmail, subject, emailBody);
                    log.info("✉️ Revision request email queued for applicant: {}", applicantEmail);
                }
            } catch (Exception emailError) {
                log.error("Error sending revision request email for application {}: {}", applicationId, emailError.getMessage());
                // Continue with the response even if email fails
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Application revision requested successfully",
                    "applicationId", applicationId,
                    "status", "NEEDS_REVISION"
            ));

        } catch (Exception e) {
            log.error("Error requesting application revision", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to request revision: " + e.getMessage()));
        }
    }


    @PostMapping("/approve-field-assessment/{applicationId}")
    public ResponseEntity<?> approveFieldAssessment(
            @PathVariable String applicationId,
            @RequestHeader("Authorization") String token) {
        try {
            log.info("=== APPROVE FIELD ASSESSMENT ENDPOINT REACHED ===");
            log.info("Application ID: {}", applicationId);
            log.info("WARNING: This should NOT be called for refer back action!");
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            SysUserAccount currentUser = getCurrentUser(token);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user"));
            }
            int currentStep = application.getCoreApplicationStep().getSequenceNumber();
            String userRole = currentUser.getSysUserGroup() != null ? currentUser.getSysUserGroup().getName().toLowerCase() : "unknown";
            
            log.info("Current Step: {}, User Role: {}", currentStep, userRole);
            
            // Validate authorization based on step and role
            if ((currentStep == 3 && !userRole.contains("manager")) || 
                (currentStep == 2 && !userRole.contains("senior_licensing_officer"))) {
                throw new Exception("Unauthorized activity for current step and role");
            }
            
            if (currentStep != 2 && currentStep != 3) {
                throw new Exception("Field assessment approval only allowed at steps 2 or 3");
            }

            // Clear referral fields when approving (application moves forward)
            application.clearReferral();
            application.setLastHandledByUserId(currentUser.getId());
            application.setCoreApplicationStep(applicationStepService.getPreviousStep(application.getCoreApplicationStep()));

            // Update status to next step (you might need to adjust this based on your workflow)
            CoreApplicationStatus approvedStatus = coreApplicationStatusService.getCoreApplicationStatusByName("FIELD_ASSESSMENT_APPROVED");
            if (approvedStatus != null) {
                application.setCoreApplicationStatus(approvedStatus);
            }

            applicationService.editCoreLicenseApplication(application);

            // Notify officers at the new step about the application
            try {
                if (application.getCoreApplicationStep() != null) {
                    String officerRole = mapStepToOfficerRole(application.getCoreApplicationStep().getName());
                    if (officerRole != null) {
                        log.info("=== TAKING APPLICATION TO NEXT LEVEL AFTER FIELD ASSESSMENT APPROVAL ===");
                        log.info("Application ID: {}", applicationId);
                        log.info("New Step: {} (Sequence: {})", application.getCoreApplicationStep().getName(), application.getCoreApplicationStep().getSequenceNumber());
                        log.info("🔔 About to notify officers in role: '{}' about application at new level", officerRole);
                        officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                        log.info("✅ Officer notification execution completed for role: {}", officerRole);
                        log.info("=== APPLICATION LEVEL PROGRESSION COMPLETED ===");
                    }
                }
            } catch (Exception notificationError) {
                log.error("Error notifying officers after field assessment approval for application {}: {}",
                        applicationId, notificationError.getMessage());
            }

            // Log activity
            CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "APPROVE_FIELD_ASSESSMENT");
            if (activityType != null) {
                CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                activity.setCoreLicenseApplication(application);
                activity.setSysUserAccount(currentUser);
                activity.setCoreLicenseTypeActivity(activityType);
                activity.setDescription("Field assessment approved by " + userRole + " and application moved to step " + application.getCoreApplicationStep().getSequenceNumber());
                activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Field assessment approved successfully",
                    "applicationId", applicationId
            ));

        } catch (Exception e) {
            log.error("Error approving field assessment", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to approve: " + e.getMessage()));
        }
    }

    @GetMapping("/referral-info/{applicationId}")
    public ResponseEntity<?> getReferralInfo(
            @PathVariable String applicationId,
            @RequestHeader("Authorization") String token) {
        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            SysUserAccount currentUser = getCurrentUser(token);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user"));
            }

            Map<String, Object> referralInfo = new HashMap<>();
            referralInfo.put("hasActiveReferral", application.hasActiveReferral());
            referralInfo.put("isReferredToCurrentUser", application.isReferredToUser(currentUser.getId()));
            referralInfo.put("referralReason", application.getReferralReason());
            referralInfo.put("referralDate", application.getReferralDate());
            referralInfo.put("referredFromUserId", application.getReferredFromUserId());

            // Get referrer user info if available
            if (application.getReferredFromUserId() != null) {
                // You might want to create a user service method to get user by ID
                // For now, this is a placeholder
                referralInfo.put("referredFromUserName", "User " + application.getReferredFromUserId());
            }

            return ResponseEntity.ok(referralInfo);

        } catch (Exception e) {
            log.error("Error getting referral info", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get referral info: " + e.getMessage()));
        }
    }

    private SysUserAccount getCurrentUser(String token) {
        return AppUtil.getLoggedInUser(token);
    }

    /**
     * Update assessment handler based on current user role and application workflow
     *
     * @param applicationId The application ID
     * @param token         Authorization token to get current user
     */
    private void updateAssessmentHandler(String applicationId, String token) {
        try {
            CoreLicenseAssessment assessment = assessmentService.findByApplicationId(applicationId);
            SysUserAccount user = getCurrentUser(token);

            if (assessment != null && user != null && user.getSysUserGroup() != null) {
                String userRole = user.getSysUserGroup().getName().toLowerCase();
                String userId = user.getId();

                log.info("Updating assessment handler for role: {} and user: {}", userRole, userId);

                switch (userRole) {
                    case "licensing_officer":
                        if (assessment.getLicenseOfficerId() == null) {
                            assessment.setLicenseOfficerId(userId);
                            log.info("Set license officer ID: {}", userId);
                        }
                        break;

                    case "senior_licensing_officer":
                        if (assessment.getSeniorLicenseOfficerId() == null) {
                            assessment.setSeniorLicenseOfficerId(userId);
                            log.info("Set senior license officer ID: {}", userId);
                        }
                        break;

                    case "licensing_manager":
                    case "license_manager":
                        if (assessment.getLicenseManagerId() == null) {
                            assessment.setLicenseManagerId(userId);
                            log.info("Set license manager ID: {}", userId);
                        }
                        break;

                    case "drs":
                        if (assessment.getDrsId() == null) {
                            assessment.setDrsId(userId);
                            log.info("Set DRS ID: {}", userId);
                        }
                        break;

                    case "accountant":
                        if (assessment.getAccountantId() == null) {
                            assessment.setAccountantId(userId);
                            log.info("Set accountant ID: {}", userId);
                        }
                        break;

                    default:
                        log.info("No assessment handler update needed for role: {}", userRole);
                        return;
                }

                assessmentService.save(assessment);
                log.info("Assessment handler updated successfully for application: {}", applicationId);
            }
        } catch (Exception e) {
            log.error("Error updating assessment handler for application {}: {}", applicationId, e.getMessage());
            // Don't throw exception to avoid breaking the main workflow
        }
    }

    /**
     * Determine who to refer back to based on current user's role and assessment data
     */
    private String determineReferralTarget(String applicationId, SysUserAccount currentUser) {
        try {
            String currentUserRole = currentUser.getSysUserGroup() != null ?
                    currentUser.getSysUserGroup().getName().toLowerCase() : "";

            log.info("Determining referral target for user role: {}", currentUserRole);

            // Get assessment data to find the appropriate user to refer back to
            CoreLicenseAssessment assessment = assessmentService.findByApplicationId(applicationId);

            if (assessment == null) {
                log.warn("No assessment found for application: {}, using default lastHandledByUserId", applicationId);
                CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
                return application != null ? application.getLastHandledByUserId() : null;
            }

            String referToUserId = null;

            switch (currentUserRole) {
                case "drs":
                    // DRS refers back to Manager
                    if (assessment.getLicenseManagerId() != null) {
                        referToUserId = assessment.getLicenseManagerId();
                        log.info("DRS referring back to license manager: {}", referToUserId);
                    }
                    break;

                case "licensing_manager":
                case "license_manager":
                    // Manager refers back to Senior License Officer
                    if (assessment.getSeniorLicenseOfficerId() != null) {
                        referToUserId = assessment.getSeniorLicenseOfficerId();
                        log.info("License manager referring back to senior license officer: {}", referToUserId);
                    }
                    break;

                case "senior_licensing_officer":
                    // Senior License Officer refers back to License Officer
                    if (assessment.getLicenseOfficerId() != null) {
                        referToUserId = assessment.getLicenseOfficerId();
                        log.info("Senior license officer referring back to license officer: {}", referToUserId);
                    }
                    break;

                case "ceo":
                    // CEO refers back to DRS
                    if (assessment.getDrsId() != null) {
                        referToUserId = assessment.getDrsId();
                        log.info("CEO referring back to DRS: {}", referToUserId);
                    }
                    break;

                case "accountant":
                    // Accountant could refer back to CEO or licensing officer depending on the stage
                    if (assessment.getLicenseOfficerId() != null) {
                        referToUserId = assessment.getLicenseOfficerId();
                        log.info("Accountant referring back to license officer: {}", referToUserId);
                    }
                    break;

                default:
                    log.warn("Unknown user role for referral: {}", currentUserRole);
                    break;
            }

            // Fallback to lastHandledByUserId if no specific user found
            if (referToUserId == null) {
                CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
                referToUserId = application != null ? application.getLastHandledByUserId() : null;
                log.info("Using fallback referral target (lastHandledByUserId): {}", referToUserId);
            }

            return referToUserId;

        } catch (Exception e) {
            log.error("Error determining referral target: {}", e.getMessage(), e);
            // Fallback to original logic
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            return application != null ? application.getLastHandledByUserId() : null;
        }
    }

    /**
     * Get available steps for referring back
     */
    @GetMapping("/available-steps/{applicationId}")
    public ResponseEntity<?> getAvailableStepsForReferBack(@PathVariable String applicationId) {
        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            CoreApplicationStep currentStep = application.getCoreApplicationStep();
            List<CoreApplicationStep> allSteps = applicationStepService.getCoreApplicationStepByLicenseType(application.getCoreLicenseType());

            // Filter steps that come before current step
            List<Map<String, Object>> availableSteps = new ArrayList<>();
            for (CoreApplicationStep step : allSteps) {
                if (step.getSequenceNumber() < currentStep.getSequenceNumber()) {
                    Map<String, Object> stepInfo = new HashMap<>();
                    stepInfo.put("id", step.getId());
                    stepInfo.put("name", step.getName());
                    stepInfo.put("description", step.getDescription());
                    stepInfo.put("sequenceNumber", step.getSequenceNumber());
                    availableSteps.add(stepInfo);
                }
            }

            return ResponseEntity.ok(Map.of("availableSteps", availableSteps));
        } catch (Exception e) {
            log.error("Error getting available steps: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get activity history for an application
     */
    @GetMapping("/activities/{applicationId}")
    public ResponseEntity<?> getApplicationActivities(@PathVariable String applicationId) {
        try {
            log.info("Getting activity history for application: {}", applicationId);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Get all activities for this application
            List<CoreLicenseApplicationActivity> allActivities = coreLicenseApplicationActivityService.getAllCoreLicenseApplicationActivities();
            List<Map<String, Object>> activities = new ArrayList<>();

            for (CoreLicenseApplicationActivity activity : allActivities) {
                if (activity.getCoreLicenseApplication() != null &&
                        applicationId.equals(activity.getCoreLicenseApplication().getId())) {

                    Map<String, Object> activityInfo = new HashMap<>();
                    activityInfo.put("id", activity.getId());
                    activityInfo.put("description", activity.getDescription());
                    activityInfo.put("dateCreated", activity.getDateCreated());

                    // Add activity type based on description
                    String description = activity.getDescription().toLowerCase();
                    if (description.contains("approved")) {
                        activityInfo.put("activity", "APPROVE");
                    } else if (description.contains("rejected")) {
                        activityInfo.put("activity", "REJECT");
                    } else if (description.contains("referred back")) {
                        activityInfo.put("activity", "REFER_BACK");
                    } else {
                        activityInfo.put("activity", "GENERAL");
                    }

                    // Add user information
                    if (activity.getSysUserAccount() != null) {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("firstName", activity.getSysUserAccount().getFirstName());
                        userInfo.put("lastName", activity.getSysUserAccount().getLastName());
                        if (activity.getSysUserAccount().getSysUserGroup() != null) {
                            Map<String, Object> userGroup = new HashMap<>();
                            userGroup.put("name", activity.getSysUserAccount().getSysUserGroup().getName());
                            userInfo.put("sysUserGroup", userGroup);
                        }
                        activityInfo.put("userAccount", userInfo);
                    }

                    activities.add(activityInfo);
                }
            }

            return ResponseEntity.ok(Map.of("activities", activities));
        } catch (Exception e) {
            log.error("Error getting application activities: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get next steps in workflow for an application
     */
    @GetMapping("/next-steps/{applicationId}")
    public ResponseEntity<?> getNextWorkflowSteps(@PathVariable String applicationId) {
        try {
            log.info("Getting next workflow steps for application: {}", applicationId);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            CoreApplicationStep currentStep = application.getCoreApplicationStep();
            if (currentStep == null) {
                return ResponseEntity.ok(Map.of("nextSteps", new ArrayList<>()));
            }

            List<CoreApplicationStep> allSteps = applicationStepService.getCoreApplicationStepByLicenseType(application.getCoreLicenseType());

            // Filter steps that come after current step
            List<Map<String, Object>> nextSteps = new ArrayList<>();
            for (CoreApplicationStep step : allSteps) {
                if (step.getSequenceNumber() > currentStep.getSequenceNumber()) {
                    Map<String, Object> stepInfo = new HashMap<>();
                    stepInfo.put("id", step.getId());
                    stepInfo.put("name", step.getName());
                    stepInfo.put("description", step.getDescription());
                    stepInfo.put("sequenceNumber", step.getSequenceNumber());
                    nextSteps.add(stepInfo);
                }
            }

            return ResponseEntity.ok(Map.of("nextSteps", nextSteps));
        } catch (Exception e) {
            log.error("Error getting next workflow steps: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get or create a CoreLicenseTypeActivity for workflow activities
     */
    private CoreLicenseTypeActivity getOrCreateWorkflowActivity(CoreLicenseApplication application, String activityName) {
        try {
            // Try to find existing activity for this license type
            List<CoreLicenseTypeActivity> activities = coreLicenseTypeActivityService.getAllCoreLicenseTypeActivities();
            for (CoreLicenseTypeActivity activity : activities) {
                if (activity.getCoreLicenseType() != null &&
                        application.getCoreLicenseType().getId().equals(activity.getCoreLicenseType().getId()) &&
                        activityName.equals(activity.getName())) {
                    return activity;
                }
            }

            // Create new activity if not found
            CoreLicenseTypeActivity newActivity = new CoreLicenseTypeActivity();
            newActivity.setName(activityName);
            newActivity.setDescription("Workflow activity: " + activityName);
            newActivity.setCoreLicenseType(application.getCoreLicenseType());
            newActivity.setIsRequired("Y");
            newActivity.setIsUpload("N");
            newActivity.setIsUserActivity("Y");
            newActivity.setDateCreated(new Timestamp(System.currentTimeMillis()));

            return coreLicenseTypeActivityService.addCoreLicenseTypeActivity(newActivity);
        } catch (Exception e) {
            log.error("Error getting/creating workflow activity", e);
            // Return null and let the calling method handle it gracefully
            return null;
        }
    }

    /**
     * Complete assessment (upload files, calculate rentals, set schedule)
     */
    @PostMapping("/complete-assessment/{applicationId}")
    public ResponseEntity<?> completeAssessment(
            @PathVariable String applicationId,
            @RequestParam(value = "assessmentFiles", required = false) MultipartFile[] files,
            @RequestParam("assessmentNotes") String assessmentNotes,
            @RequestParam("rentalQuantity") Double rentalQuantity,
            @RequestParam("rentalRate") Double rentalRate,
            @RequestParam("calculatedRental") Double calculatedRental,
            @RequestParam("recommendedDate") String recommendedDate,
            @RequestParam("scheduleNotes") String scheduleNotes,
            @RequestParam(value = "action", defaultValue = "submit") String action,
            @RequestParam(value = "existingFilePaths", required = false) String existingFilePaths,
            @RequestHeader("Authorization") String token) {
        try {
            // Get user and application
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);

            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }
            if (application.getCoreApplicationStep().getSequenceNumber() != 1) {
                throw new Exception("Cant refer back this application");
            }

            // Save assessment files
            String uploadDir = "uploads/assessments/" + applicationId;
            Path uploadPath = Paths.get(uploadDir);
            log.info("Creating upload directory: {}", uploadPath.toAbsolutePath());

            try {
                Files.createDirectories(uploadPath);
                log.info("Upload directory created successfully");
            } catch (Exception e) {
                log.error("Failed to create upload directory: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to create upload directory: " + e.getMessage()));
            }

            List<String> filePaths = new ArrayList<>();
            
            // Handle new file uploads
            if (files != null) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String originalFilename = file.getOriginalFilename();
                        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
                        Path filePath = uploadPath.resolve(uniqueFilename);
                        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                        filePaths.add(filePath.toString());
                    }
                }
            }
            
            // Handle existing file paths (for edit mode)
            if (existingFilePaths != null && !existingFilePaths.trim().isEmpty() && filePaths.isEmpty()) {
                String[] fileNames = existingFilePaths.split(",");
                for (String fileName : fileNames) {
                    if (!fileName.trim().isEmpty()) {
                        Path existingFilePath = uploadPath.resolve(fileName.trim());
                        filePaths.add(existingFilePath.toString());
                    }
                }
                log.info("Using existing file paths: {}", filePaths);
            }

            // Save assessment data to database
            log.info("=== SAVING ASSESSMENT DATA TO DATABASE ===");
            log.info("Application ID: {}", applicationId);

            // Check if assessment already exists
            CoreLicenseAssessment existingAssessment = assessmentService.findByApplicationId(applicationId);
            CoreLicenseAssessment assessment;
            
            if (existingAssessment != null) {
                // Update existing assessment (allow for both submit and update actions)
                assessment = existingAssessment;
                log.info("Updating existing assessment: {}", assessment.getId());
            } else {
                // Create new assessment
                assessment = new CoreLicenseAssessment();
                assessment.setLicenseApplicationId(applicationId);
                log.info("Creating new assessment for application: {}", applicationId);
            }
            assessment.setAssessmentNotes(assessmentNotes);
            assessment.setRentalQuantity(new BigDecimal(rentalQuantity));
            assessment.setRentalRate(new BigDecimal(rentalRate));
            assessment.setCalculatedAnnualRental(new BigDecimal(calculatedRental));
            assessment.setAssessmentFilesUpload(String.join(",", filePaths)); // Store as comma-separated
            assessment.setLicenseOfficerId(user != null ? user.getId() : null);

            // Parse and set recommended date
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date parsedDate = sdf.parse(recommendedDate);
                assessment.setRecommendedScheduleDate(parsedDate);
            } catch (Exception e) {
                log.warn("Error parsing recommended date: {}", e.getMessage());
            }

            // Set assessment status based on action
            String assessmentStatus = "submit".equals(action) ? "COMPLETED" : "DRAFT";
            assessment.setAssessmentStatus(assessmentStatus);

            // Save assessment to database
            try {
                log.info("Saving assessment for application: {}", applicationId);
                CoreLicenseAssessment savedAssessment = assessmentService.save(assessment);
                log.info("Assessment saved with ID: {}", savedAssessment.getId());
            } catch (Exception e) {
                log.error("Error saving assessment: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to save assessment: " + e.getMessage()));
            }

            // Handle different actions
            log.info("Processing action: {}", action);

            String newStatusName = switch (action) {
                case "draft" -> "ASSESSMENT_DRAFT";
                case "refer_back" -> "REFERRED_BACK";
                default -> "AWAITING_MANAGER_REVIEW";
            };

            // Update application status and referral fields
            CoreApplicationStatus newStatus =
                    coreApplicationStatusService.getCoreApplicationStatusByName(newStatusName);
            if (newStatus != null) {
                application.setCoreApplicationStatus(newStatus);
                application.clearReferral();
                CoreApplicationStep nextStep = applicationStepService.getNextStep(application.getCoreApplicationStep());
                if (nextStep != null) {
                    application.setCoreApplicationStep(nextStep);
                    log.info("Updated application step to: {} (sequence: {})", nextStep.getName(), nextStep.getSequenceNumber());
                } else {
                    log.warn("No next step found for current step. Application step remains unchanged.");
                }
                assert user != null;
                application.setLastHandledByUserId(user.getId());

                applicationService.editCoreLicenseApplication(application);
                log.info("Updated application status to: {}", newStatusName);

                // Notify officers at the new step about the application (only for submit action)
                if ("submit".equals(action)) {
                    try {
                        if (application.getCoreApplicationStep() != null) {
                            String officerRole = mapStepToOfficerRole(application.getCoreApplicationStep().getName());
                            if (officerRole != null) {
                                log.info("=== TAKING APPLICATION TO NEXT LEVEL AFTER ASSESSMENT COMPLETION ===");
                                log.info("Application ID: {}", applicationId);
                                log.info("New Step: {} (Sequence: {})", application.getCoreApplicationStep().getName(), application.getCoreApplicationStep().getSequenceNumber());
                                log.info("🔔 About to notify officers in role: '{}' about application at new level", officerRole);
                                officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                                log.info("✅ Officer notification execution completed for role: {}", officerRole);
                                log.info("=== APPLICATION LEVEL PROGRESSION COMPLETED ===");
                            }
                        }
                    } catch (Exception notificationError) {
                        log.error("Error notifying officers after assessment completion for application {}: {}",
                                applicationId, notificationError.getMessage());
                    }
                }
            } else {
                log.error("Status '{}' not found in database. Please run the SQL scripts to add missing statuses.", newStatusName);
                // Continue without status update to avoid breaking the flow
            }

            Map<String, Object> response = new HashMap<>();
            String message;
            switch (action) {
                case "draft":
                    message = "Assessment saved as draft";
                    break;
                case "refer_back":
                    message = "Application referred back";
                    break;
                case "submit":
                default:
                    message = "Assessment submitted for manager review";
                    break;
            }
            updateAssessmentHandler(application.getId(), token);

            response.put("message", message);
            response.put("action", action);
            response.put("filesUploaded", filePaths.size());
            response.put("calculatedRental", calculatedRental);
            response.put("status", newStatusName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error completing assessment: " + e.getMessage(), e);
            log.error("Stack trace: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to complete assessment: " + e.getMessage()));
        }
    }

    /**
     * Update existing assessment data
     */
    @PostMapping("/update-assessment/{applicationId}")
    public ResponseEntity<?> updateAssessment(
            @PathVariable String applicationId,
            @RequestParam(value = "assessmentFiles", required = false) MultipartFile[] files,
            @RequestParam("assessmentNotes") String assessmentNotes,
            @RequestParam("rentalQuantity") Double rentalQuantity,
            @RequestParam("rentalRate") Double rentalRate,
            @RequestParam("calculatedRental") Double calculatedRental,
            @RequestParam("recommendedDate") String recommendedDate,
            @RequestParam("scheduleNotes") String scheduleNotes,
            @RequestParam(value = "filesToRemove", required = false) String filesToRemoveJson,
            @RequestParam(value = "action", defaultValue = "update") String action,
            @RequestHeader("Authorization") String token) {
        try {
            log.info("=== UPDATING ASSESSMENT ===");
            log.info("Application ID: {}", applicationId);
            log.info("Files to remove: {}", filesToRemoveJson);

            // Get user and application
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);

            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Get existing assessment
            CoreLicenseAssessment existingAssessment = assessmentService.findByApplicationId(applicationId);
            if (existingAssessment == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No existing assessment found to update"));
            }

            // Handle file removal if specified
            List<String> currentFilePaths = new ArrayList<>();
            if (existingAssessment.getAssessmentFilesUpload() != null && !existingAssessment.getAssessmentFilesUpload().isEmpty()) {
                currentFilePaths = new ArrayList<>(Arrays.asList(existingAssessment.getAssessmentFilesUpload().split(",")));
            }

            // Remove specified files
            if (filesToRemoveJson != null && !filesToRemoveJson.isEmpty()) {
                try {
                    // Parse the JSON array of file IDs/paths to remove
                    // For now, assume it's a simple comma-separated list or JSON array
                    String[] filesToRemove = filesToRemoveJson.replace("[", "").replace("]", "").replace("\"", "").split(",");
                    for (String fileToRemoveRaw : filesToRemove) {
                        final String fileToRemove = fileToRemoveRaw.trim();
                        if (!fileToRemove.isEmpty()) {
                            currentFilePaths.removeIf(path -> path.contains(fileToRemove) || path.endsWith(fileToRemove));

                            // Try to delete the physical file
                            try {
                                Path fileToDelete = Paths.get(fileToRemove);
                                if (Files.exists(fileToDelete)) {
                                    Files.delete(fileToDelete);
                                    log.info("Deleted file: {}", fileToRemove);
                                }
                            } catch (Exception e) {
                                log.warn("Could not delete file {}: {}", fileToRemove, e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error processing files to remove: {}", e.getMessage());
                }
            }

            // Handle new file uploads
            if (files != null && files.length > 0) {
                String uploadDir = "uploads/assessments/" + applicationId;
                Path uploadPath = Paths.get(uploadDir);

                try {
                    Files.createDirectories(uploadPath);
                    log.info("Upload directory ensured: {}", uploadPath.toAbsolutePath());
                } catch (Exception e) {
                    log.error("Failed to create upload directory: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to create upload directory: " + e.getMessage()));
                }

                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String originalFilename = file.getOriginalFilename();
                        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
                        Path filePath = uploadPath.resolve(uniqueFilename);
                        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                        currentFilePaths.add(filePath.toString());
                        log.info("Added new file: {}", filePath.toString());
                    }
                }
            }

            // Update assessment data
            existingAssessment.setAssessmentNotes(assessmentNotes);
            existingAssessment.setRentalQuantity(new BigDecimal(rentalQuantity));
            existingAssessment.setRentalRate(new BigDecimal(rentalRate));
            existingAssessment.setCalculatedAnnualRental(new BigDecimal(calculatedRental));
            existingAssessment.setAssessmentFilesUpload(String.join(",", currentFilePaths));

            // Parse and set recommended date
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date parsedDate = sdf.parse(recommendedDate);
                existingAssessment.setRecommendedScheduleDate(parsedDate);
            } catch (Exception e) {
                log.warn("Error parsing recommended date: {}", e.getMessage());
            }

            // Update assessment status
            existingAssessment.setAssessmentStatus("UPDATED");

            application.clearReferral();
            application.setCoreApplicationStep(applicationStepService.getNextStep(application.getCoreApplicationStep()));
            application.setLastHandledByUserId(user.getId());
            application.setCoreApplicationStatus(coreApplicationStatusService.getCoreApplicationStatusByName("AWAITING_MANAGER_REVIEW"));
            applicationService.editCoreLicenseApplication(application);
            updateAssessmentHandler(application.getId(), token);
            // Save updated assessment
            try {
                log.info("Updating assessment for application: {}", applicationId);
                CoreLicenseAssessment updatedAssessment = assessmentService.save(existingAssessment);
                log.info("Assessment updated successfully with ID: {}", updatedAssessment.getId());
            } catch (Exception e) {
                log.error("Error updating assessment: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update assessment: " + e.getMessage()));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Assessment updated successfully");
            response.put("action", action);
            response.put("filesRemoved", filesToRemoveJson != null ? filesToRemoveJson : "none");
            response.put("newFilesAdded", files != null ? files.length : 0);
            response.put("calculatedRental", calculatedRental);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating assessment: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update assessment: " + e.getMessage()));
        }
    }

    /**
     * Download assessment file
     */
//    @GetMapping("/download-assessment-file/{applicationId}/{fileName}")
//    public ResponseEntity<Resource> downloadAssessmentFile(
//            @PathVariable String applicationId,
//            @PathVariable String fileName) {
//        try {
//            log.info("=== DOWNLOADING ASSESSMENT FILE ===");
//            log.info("Application ID: {}", applicationId);
//            log.info("File name: {}", fileName);
//
//            // Validate user access
////            mw.nwra.ewaterpermit.model.SysUserAccount user = mw.nwra.ewaterpermit.util.AppUtil.getLoggedInUser(token);
////            if (user == null) {
////                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
////            }
//
//            // Build file path
//            String uploadDir = "uploads/assessments/" + applicationId;
//            Path filePath = Paths.get(uploadDir, fileName);
//
//            log.info("Looking for file at: {}", filePath.toAbsolutePath());
//
//            if (!Files.exists(filePath)) {
//                log.warn("File not found: {}", filePath.toAbsolutePath());
//                return ResponseEntity.notFound().build();
//            }
//
//            // Create resource
//            Resource resource = new UrlResource(filePath.toUri());
//            if (!resource.exists() || !resource.isReadable()) {
//                log.warn("File not readable: {}", filePath.toAbsolutePath());
//                return ResponseEntity.notFound().build();
//            }
//
//            // Determine content type
//            String contentType = Files.probeContentType(filePath);
//            if (contentType == null) {
//                contentType = "application/octet-stream";
//            }
//
//            log.info("Serving file: {} with content type: {}", fileName, contentType);
//
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
//                    .header(HttpHeaders.CONTENT_TYPE, contentType)
//                    .body(resource);
//
//        } catch (Exception e) {
//            log.error("Error downloading assessment file: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

    /**
     * Get assessment data for an application
     */
    @GetMapping("/assessment/{applicationId}")
    public ResponseEntity<?> getAssessmentData(
            @PathVariable String applicationId,
            @RequestHeader("Authorization") String token) {
        try {
            log.info("=== FETCHING ASSESSMENT DATA ===");
            log.info("Application ID: {}", applicationId);

            // Get current user to log who is accessing
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            if (user != null && user.getSysUserGroup() != null) {
                log.info("Accessed by user role: {}", user.getSysUserGroup().getName());
            }

            // Fetch real assessment data from database
            Map<String, Object> assessmentData = new HashMap<>();

            // Get the application
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            
            // Fetch assessment directly from the current application (works for both regular and transfer applications)
            CoreLicenseAssessment assessment = assessmentService.findByApplicationId(applicationId);
            log.info("Assessment found in database: {} (Application ID: {})", assessment != null, applicationId);

            // If no assessment exists and user is licensing officer, notify manager about assessment request
            if (assessment == null && user != null && user.getSysUserGroup() != null &&
                    "licensing_officer".equals(user.getSysUserGroup().getName())) {
                try {
                    if (application != null) {
                        log.info("=== NOTIFYING MANAGER ABOUT ASSESSMENT REQUEST ===");
                        log.info("License officer accessing assessment page - notifying licensing manager");
                        officerNotificationService.notifyOfficersAboutNewApplication("licensing_manager", application);
                        log.info("✅ Manager notification sent for assessment request");
                    }
                } catch (Exception notificationError) {
                    log.error("Error notifying manager about assessment request: {}", notificationError.getMessage());
                }
            }

            // Get files from assessment directory
            List<Map<String, Object>> files = new ArrayList<>();
            if (assessment != null && assessment.getAssessmentFilesUpload() != null) {
                String[] filePaths = assessment.getAssessmentFilesUpload().split(",");
                for (String filePath : filePaths) {
                    if (!filePath.trim().isEmpty()) {
                        Path path = Paths.get(filePath.trim());
                        String fileName = path.getFileName().toString();
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", fileName);
                        fileInfo.put("url", "/api/nwra-apis/ewaterpermit-ws/v1/workflow/download-assessment-file/" + applicationId + "/" + fileName);
                        files.add(fileInfo);
                        log.info("Assessment file: {}", fileName);
                    }
                }
            }

            // Return real assessment data
            assessmentData.put("files", files);
            if (assessment != null) {
                assessmentData.put("notes", assessment.getAssessmentNotes());
                assessmentData.put("quantity", assessment.getRentalQuantity());
                assessmentData.put("rate", assessment.getRentalRate());
                assessmentData.put("calculatedRental", assessment.getCalculatedAnnualRental());
                assessmentData.put("recommendedDate", assessment.getRecommendedScheduleDate());
                assessmentData.put("scheduleNotes", assessment.getAssessmentNotes()); // Using notes as schedule notes for now
                assessmentData.put("dataSource", "DATABASE");
            } else {
                log.warn("No assessment data found for application: {}", applicationId);
                assessmentData.put("notes", "No assessment data found");
                assessmentData.put("quantity", 0);
                assessmentData.put("rate", 0);
                assessmentData.put("calculatedRental", 0);
                assessmentData.put("recommendedDate", null);
                assessmentData.put("scheduleNotes", "No assessment data found");
                assessmentData.put("dataSource", "NO_DATA");
            }

            log.info("Assessment data prepared: {}", assessmentData);
            return ResponseEntity.ok(assessmentData);
        } catch (Exception e) {
            log.error("Error fetching assessment data: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Download assessment file
     */
    @GetMapping("/download-assessment-file/{applicationId}/{filename}")
    public ResponseEntity<Resource> downloadAssessmentFile(
            @PathVariable String applicationId,
            @PathVariable String filename) {
        try {
            // Try both relative and absolute paths
            Path filePath = Paths.get("uploads/assessments/" + applicationId + "/" + filename);

            if (!Files.exists(filePath)) {
                // Try absolute path from current directory
                filePath = Paths.get(System.getProperty("user.dir"), "uploads/assessments/" + applicationId + "/" + filename);
            }

            log.info("Looking for assessment file at: {}", filePath.toAbsolutePath());

            // If file not found and this is a transfer application, try original application
            if (!Files.exists(filePath)) {
                try {
                    CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
                    if (application != null && "TRANSFER".equals(application.getApplicationType()) && 
                        application.getOriginalLicenseId() != null) {
                        
                        log.info("Transfer application detected, trying original license files");
                        CoreLicense originalLicense = coreLicenseService.getCoreLicenseById(application.getOriginalLicenseId());
                        
                        if (originalLicense != null && originalLicense.getCoreLicenseApplication() != null) {
                            String originalApplicationId = originalLicense.getCoreLicenseApplication().getId();
                            log.info("Trying original application ID: {}", originalApplicationId);
                            
                            // Try original application path
                            filePath = Paths.get("uploads/assessments/" + originalApplicationId + "/" + filename);
                            if (!Files.exists(filePath)) {
                                filePath = Paths.get(System.getProperty("user.dir"), "uploads/assessments/" + originalApplicationId + "/" + filename);
                            }
                            
                            log.info("Trying original assessment file at: {}", filePath.toAbsolutePath());
                        }
                    }
                } catch (Exception fallbackError) {
                    log.error("Error in transfer fallback: {}", fallbackError.getMessage());
                }
            }

            if (!Files.exists(filePath)) {
                log.error("Assessment file not found: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error downloading assessment file: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Set license fee for application (Manager only)
     */
    @PostMapping("/set-license-fee/{applicationId}")
    public ResponseEntity<?> setLicenseFee(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            if (user == null || user.getSysUserGroup() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            // Check if user is a licensing manager
            String userRole = user.getSysUserGroup().getName().toLowerCase();
            if (!userRole.contains("manager") && !userRole.equals("licensing_manager") && !userRole.equals("license_manager")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only licensing managers can set license fees"));
            }

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Extract license fee from request
            Double licenseFee = null;
            if (request.get("licenseFee") != null) {
                licenseFee = ((Number) request.get("licenseFee")).doubleValue();
            }

            if (licenseFee == null || licenseFee < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid license fee amount"));
            }

            // Set license fee and tracking fields
            application.setLicenseFee(licenseFee);
            application.setLicenseFeeSetByUserId(user.getId());
            application.setLicenseFeeSetDate(new Timestamp(System.currentTimeMillis()));

            applicationService.editCoreLicenseApplication(application);

            // Log activity
            try {
                CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                activity.setCoreLicenseApplication(application);
                activity.setSysUserAccount(user);
                activity.setDescription("Manager set license fee: MWK " + String.format("%.2f", licenseFee));
                activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                log.info("✅ Activity logged for license fee setting by user: {}", user.getUsername());
            } catch (Exception activityError) {
                log.error("Failed to log license fee activity (non-blocking): {}", activityError.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "License fee set successfully");
            response.put("licenseFee", licenseFee);
            response.put("setByUser", user.getFirstName() + " " + user.getLastName());
            response.put("setDate", application.getLicenseFeeSetDate());

            log.info("License fee set to MWK {} for application {} by user {}", licenseFee, applicationId, user.getUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error setting license fee: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manager review of assessment
     */
    @PostMapping("/manager-review/{applicationId}")
    public ResponseEntity<?> managerReview(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            String action = request.get("action") != null ? request.get("action").toString() : "";
            String managerNotes = request.get("managerNotes") != null ? request.get("managerNotes").toString() : "";

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }
            
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            int currentStep = application.getCoreApplicationStep().getSequenceNumber();
            String userRole = user.getSysUserGroup() != null ? user.getSysUserGroup().getName().toLowerCase() : "unknown";
            
            log.info("=== MANAGER REVIEW ENDPOINT ===");
            log.info("Application ID: {}", applicationId);
            log.info("Current Step: {}", currentStep);
            log.info("Action: {}", action);
            log.info("User Role: {}", userRole);
            log.info("User: {}", user.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            application.clearReferral();
            application.setLastHandledByUserId(user.getId());
            // Handle License Officer actions at step 1
            if (currentStep == 1 && "request_field_assessment".equals(action)) {
                // Move to next step (Senior License Officer at step 2)
                CoreApplicationStep nextStep = applicationStepService.getNextStep(application.getCoreApplicationStep());
                if (nextStep != null) {
                    application.setCoreApplicationStep(nextStep);
                    applicationService.editCoreLicenseApplication(application);
                    log.info("License Officer requested field assessment - moved to step: {} (sequence {})", nextStep.getName(), nextStep.getSequenceNumber());

                    // Notify officers at the new step
                    try {
                        String officerRole = mapStepToOfficerRole(nextStep.getName());
                        if (officerRole != null) {
                            log.info("=== NOTIFYING OFFICERS AFTER LICENSE OFFICER REVIEW ===");
                            log.info("Notifying role '{}' for step '{}'", officerRole, nextStep.getName());
                            officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                        }
                    } catch (Exception notificationError) {
                        log.error("Error notifying officers after license officer review for application {}: {}",
                                applicationId, notificationError.getMessage());
                    }

                    // Log activity
                    try {
                        CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                        activity.setCoreLicenseApplication(application);
                        activity.setSysUserAccount(user);
                        String description = "License Officer requested field assessment and forwarded to Senior License Officer";
                        if (managerNotes != null && !managerNotes.trim().isEmpty()) {
                            description += ": " + managerNotes;
                        }
                        activity.setDescription(description);
                        activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                        coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                        log.info("✅ Activity logged for license officer review by user: {}", user.getUsername());
                    } catch (Exception activityError) {
                        log.error("Failed to log license officer activity (non-blocking): {}", activityError.getMessage());
                    }

                    response.put("success", true);
                    response.put("status", application.getCoreApplicationStatus().getName());
                    return ResponseEntity.ok(response);
                }
                throw new Exception("Unable to find next step");
            }

            // Handle Senior License Officer actions at step 2
            if (currentStep == 2 && "request_field_assessment".equals(action)) {
                // Move to next step (Manager at step 3)
                CoreApplicationStep nextStep = applicationStepService.getNextStep(application.getCoreApplicationStep());
                if (nextStep != null) {
                    application.setCoreApplicationStep(nextStep);
                    applicationService.editCoreLicenseApplication(application);
                    log.info("Senior License Officer approved - moved to step: {} (sequence {})", nextStep.getName(), nextStep.getSequenceNumber());

                    // Notify officers at the new step
                    try {
                        String officerRole = mapStepToOfficerRole(nextStep.getName());
                        if (officerRole != null) {
                            log.info("=== NOTIFYING OFFICERS AFTER SENIOR OFFICER REVIEW ===");
                            log.info("Notifying role '{}' for step '{}'", officerRole, nextStep.getName());
                            officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                        }
                    } catch (Exception notificationError) {
                        log.error("Error notifying officers after senior officer review for application {}: {}",
                                applicationId, notificationError.getMessage());
                    }

                    // Log activity
                    try {
                        CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                        activity.setCoreLicenseApplication(application);
                        activity.setSysUserAccount(user);
                        String description = "Senior License Officer approved and forwarded to Manager";
                        if (managerNotes != null && !managerNotes.trim().isEmpty()) {
                            description += ": " + managerNotes;
                        }
                        activity.setDescription(description);
                        activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                        coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                        log.info("✅ Activity logged for senior officer review by user: {}", user.getUsername());
                    } catch (Exception activityError) {
                        log.error("Failed to log senior officer review activity (non-blocking): {}", activityError.getMessage());
                    }

                    response.put("success", true);
                    response.put("message", "Application approved and forwarded to Manager");
                    response.put("status", application.getCoreApplicationStatus().getName());
                    return ResponseEntity.ok(response);
                }
                throw new Exception("Unable to find next step");
            }

            // Handle Manager actions at step 3
            if (currentStep == 3 && ("approve".equals(action) || "forward_to_drs".equals(action))) {
                // Update status to PENDING_SCHEDULE_AUTHORIZATION for DRS
                CoreApplicationStatus authStatus =
                        coreApplicationStatusService.getCoreApplicationStatusByName("PENDING_SCHEDULE_AUTHORIZATION");
                if (authStatus != null) {
                    application.setCoreApplicationStatus(authStatus);

                    // Clear referral fields and update last handled user
                    application.clearReferral();
                    application.setLastHandledByUserId(user.getId());

                    applicationService.editCoreLicenseApplication(application);
                    log.info("Updated status to PENDING_SCHEDULE_AUTHORIZATION");
                } else {
                    log.warn("PENDING_SCHEDULE_AUTHORIZATION status not found in database");
                }

                // Move to next workflow step (DRS)
                CoreApplicationStep currentStepObj = application.getCoreApplicationStep();
                if (currentStepObj != null) {
                    CoreApplicationStep nextStep = applicationStepService.getNextStep(application.getCoreApplicationStep());
                    if (nextStep != null) {
                        application.setCoreApplicationStep(nextStep);
                        applicationService.editCoreLicenseApplication(application);
                        log.info("Moved to next step: {} (sequence {})", nextStep.getName(), nextStep.getSequenceNumber());

                        // Notify officers at the new step about the application
                        try {
                            String officerRole = mapStepToOfficerRole(nextStep.getName());
                            if (officerRole != null) {
                                log.info("=== NOTIFYING OFFICERS AFTER MANAGER REVIEW ===");
                                log.info("Notifying role '{}' for step '{}'", officerRole, nextStep.getName());
                                officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                            }
                        } catch (Exception notificationError) {
                            log.error("Error notifying officers after manager review for application {}: {}",
                                    applicationId, notificationError.getMessage());
                        }
                    }
                }

                // Log manager review activity with notes
                try {
                    CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                    activity.setCoreLicenseApplication(application);
                    activity.setSysUserAccount(user);
                    String description = "Manager approved application and forwarded to DRS";
                    if (managerNotes != null && !managerNotes.trim().isEmpty()) {
                        description += ": " + managerNotes;
                    }
                    activity.setDescription(description);
                    activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                    coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                    log.info("✅ Activity logged for manager review by user: {}", user.getUsername());
                } catch (Exception activityError) {
                    log.error("Failed to log manager review activity (non-blocking): {}", activityError.getMessage());
                }

                updateAssessmentHandler(application.getId(), token);

                response.put("success", true);
                response.put("message", "Schedule forwarded to DRS for authorization");
                response.put("status", "PENDING_SCHEDULE_AUTHORIZATION");
                return ResponseEntity.ok(response);
            }

            // Handle Manager actions at step 2 - approve assessment and move back to step 1
            if (currentStep == 2 && "approve_assessment".equals(action)) {
                CoreApplicationStep previousStep = applicationStepService.getPreviousStep(application.getCoreApplicationStep());
                if (previousStep != null) {
                    application.setCoreApplicationStep(previousStep);
                    applicationService.editCoreLicenseApplication(application);
                    log.info("Manager approved assessment - moved back to step 1");

                    // Log activity
                    try {
                        CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                        activity.setCoreLicenseApplication(application);
                        activity.setSysUserAccount(user);
                        String description = "Manager approved assessment and returned to License Officer";
                        if (managerNotes != null && !managerNotes.trim().isEmpty()) {
                            description += ": " + managerNotes;
                        }
                        activity.setDescription(description);
                        activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                        coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                        log.info("✅ Activity logged for manager approval by user: {}", user.getUsername());
                    } catch (Exception activityError) {
                        log.error("Failed to log manager approval activity (non-blocking): {}", activityError.getMessage());
                    }

                    response.put("success", true);
                    response.put("message", "Assessment approved and returned to License Officer");
                    response.put("status", application.getCoreApplicationStatus().getName());
                    return ResponseEntity.ok(response);
                }
                throw new Exception("Unable to find previous step");
            } else {
                String errorMsg = String.format("Unauthorized activity - Step: %d, Action: '%s', User Role: '%s', Valid combinations: [Step1+request_field_assessment, Step2+request_field_assessment+approve_assessment, Step3+approve/forward_to_drs]", 
                    currentStep, action, userRole);
                log.error(errorMsg);
                throw new Exception(errorMsg);
            }
        } catch (Exception e) {
            log.error("Error processing manager review: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DRS authorization of schedule
     */
    @PostMapping("/drs-authorization/{applicationId}")
    public ResponseEntity<?> drsAuthorization(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            String action = request.get("action") != null ? request.get("action").toString() : "";
            String drsNotes = request.get("drsNotes") != null ? request.get("drsNotes").toString() : "";


            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }
            if (application.getCoreApplicationStep().getSequenceNumber() != 4) {
                throw new Exception("UnAuthorised Activity");
            }
            if ("authorize_schedule".equals(action)) {
                // Update status to READY_FOR_APPROVAL for CEO
                CoreApplicationStatus readyStatus =
                        coreApplicationStatusService.getCoreApplicationStatusByName("READY_FOR_APPROVAL");
                SysUserAccount user = AppUtil.getLoggedInUser(token);
                if (readyStatus != null) {
                    application.setCoreApplicationStatus(readyStatus);

                    // Clear referral fields and update last handled user
                    application.clearReferral();
                    application.setLastHandledByUserId(user.getId());

                    log.info("Updated status to READY_FOR_APPROVAL");
                } else {
                    log.warn("READY_FOR_APPROVAL status not found in database");
                }

                // Move to next workflow step (CEO)
                CoreApplicationStep currentStep = application.getCoreApplicationStep();
                if (currentStep != null) {
                    CoreApplicationStep nextStep = applicationStepService.getNextStep(application.getCoreApplicationStep());
                    if (nextStep != null) {
                        log.info("=== TAKING APPLICATION TO NEXT LEVEL AFTER DRS AUTHORIZATION ===");
                        log.info("Application ID: {}", applicationId);
                        log.info("Current Step: {} (Sequence: {})", currentStep.getName(), currentStep.getSequenceNumber());
                        log.info("Next Step: {} (Sequence: {})", nextStep.getName(), nextStep.getSequenceNumber());

                        application.setCoreApplicationStep(nextStep);
                        log.info("✅ Application successfully moved to next step: {}", nextStep.getName());

                        // Notify officers at the new step about the application
                        try {
                            String officerRole = mapStepToOfficerRole(nextStep.getName());
                            if (officerRole != null) {
                                log.info("🔔 About to notify officers in role: '{}' about application at new level", officerRole);
                                officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                                log.info("✅ Officer notification execution completed for role: {}", officerRole);
                            }
                        } catch (Exception notificationError) {
                            log.error("Error notifying officers after DRS authorization for application {}: {}",
                                    applicationId, notificationError.getMessage());
                        }
                        log.info("=== APPLICATION LEVEL PROGRESSION COMPLETED ===");
                    }
                }

                updateAssessmentHandler(applicationId, token);
                applicationService.editCoreLicenseApplication(application);

                // Log DRS authorization activity with notes
                try {
                    log.info("=== ATTEMPTING TO LOG DRS ACTIVITY ===");
                    log.info("User: {} (ID: {})", user.getUsername(), user.getId());
                    log.info("User Group: {}", user.getSysUserGroup() != null ? user.getSysUserGroup().getName() : "null");
                    log.info("Application ID: {}", application.getId());
                    
                    // Get or create the activity type for DRS authorization
                    CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "DRS_AUTHORIZATION");
                    
                    CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                    activity.setCoreLicenseApplication(application);
                    activity.setSysUserAccount(user);
                    activity.setCoreLicenseTypeActivity(activityType);
                    String description = "DRS authorized schedule and forwarded to CEO";
                    if (drsNotes != null && !drsNotes.trim().isEmpty()) {
                        description += ": " + drsNotes;
                    }
                    activity.setDescription(description);
                    activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                    
                    log.info("About to save activity with description: {}", description);
                    CoreLicenseApplicationActivity savedActivity = coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                    log.info("✅ Activity saved successfully with ID: {}", savedActivity != null ? savedActivity.getId() : "null");
                    log.info("✅ Activity logged for DRS authorization by user: {}", user.getUsername());
                } catch (Exception activityError) {
                    log.error("Failed to log DRS authorization activity: {}", activityError.getMessage(), activityError);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "authorize_schedule".equals(action) ? "Schedule authorized and forwarded to CEO" : "Schedule authorization processed");
            response.put("status", "authorize_schedule".equals(action) ? "READY_FOR_APPROVAL" : "PROCESSED");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing DRS authorization: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * CEO decision on application
     */
    @PostMapping("/ceo-decision/{applicationId}")
    public ResponseEntity<?> ceoDecision(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            String action = request.get("action") != null ? request.get("action").toString() : "";
            String boardMinutes = request.get("boardMinutes") != null ? request.get("boardMinutes").toString() : "";
            String boardApprovalDate = request.get("boardApprovalDate") != null ? request.get("boardApprovalDate").toString() : "";

            log.info("=== CEO DECISION ===");
            log.info("Application ID: {}", applicationId);
            log.info("Action: {}", action);
            log.info("Board Minutes: {}", boardMinutes);
            log.info("Board Approval Date: {}", boardApprovalDate);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }
            // Check if user has CEO role instead of just step sequence
            SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
            if (currentUser == null || currentUser.getSysUserGroup() == null ||
                    !"ceo".equalsIgnoreCase(currentUser.getSysUserGroup().getName())) {
                throw new Exception("UnAuthorised activity - CEO role required");
            }

            // Allow CEO to act on applications at step 4 or if they are the designated approver
            if (application.getCoreApplicationStep().getSequenceNumber() != 4) {
                log.warn("CEO attempting to approve application at step {}, expected step 4",
                        application.getCoreApplicationStep().getSequenceNumber());
                // Still allow if user is CEO, but log the discrepancy
            }

            // Save CEO decision to database
            SysUserAccount user = AppUtil.getLoggedInUser(token);

            // TODO: Create CoreCEODecision entity and service
            // For now, log the decision data
            log.info("Saving CEO decision to database:");
            log.info("- Application ID: {}", applicationId);
            log.info("- Decision: {}", action);
            log.info("- Board Minutes: {}", boardMinutes);
            log.info("- Board Approval Date: {}", boardApprovalDate);
            log.info("- CEO User ID: {}", user != null ? user.getId() : "null");

            if ("approve".equals(action)) {
                // Save board minutes and approval date
                application.setBoardMinutes(boardMinutes);
                application.setBoardApprovalDate(new Timestamp(System.currentTimeMillis()));
                log.info("Saved board minutes and approval date");

                // Update status to APPROVED
                CoreApplicationStatus approvedStatus =
                        coreApplicationStatusService.getCoreApplicationStatusByName("APPROVED");
                if (approvedStatus != null) {
                    application.setCoreApplicationStatus(approvedStatus);

                    // Clear referral fields and update last handled user
                    application.clearReferral();
                    assert user != null;
                    application.setLastHandledByUserId(user.getId());

                    // Handle ownership transfer for TRANSFER applications
                    if ("TRANSFER".equals(application.getApplicationType()) && application.getTransferToUserId() != null) {
                        String oldOwnerId = application.getOwnerId();
                        String newOwnerId = application.getTransferToUserId();

                        log.info("=== PROCESSING OWNERSHIP TRANSFER ===");
                        log.info("Application Type: TRANSFER");
                        log.info("Transferring ownership from user ID: {} to user ID: {}", oldOwnerId, newOwnerId);

                        // Transfer ownership to the new user
                        application.setOwnerId(newOwnerId);

                        // Save the application immediately to ensure ownership transfer is persisted
                        applicationService.editCoreLicenseApplication(application);
                        log.info("Application saved with new owner ID: {}", newOwnerId);

                        log.info("Ownership successfully transferred to user ID: {}", newOwnerId);
                    }

                    log.info("Updated status to APPROVED");
                } else {
                    log.warn("APPROVED status not found in database");
                }

                // Move to next workflow step (Accountant)
                CoreApplicationStep currentStep = application.getCoreApplicationStep();
                if (currentStep != null) {
                    CoreApplicationStep nextStep = applicationStepService.getNextStep(application.getCoreApplicationStep());
                    if (nextStep != null) {
                        log.info("=== TAKING APPLICATION TO NEXT LEVEL AFTER CEO APPROVAL ===");
                        log.info("Application ID: {}", applicationId);
                        log.info("Current Step: {} (Sequence: {})", currentStep.getName(), currentStep.getSequenceNumber());
                        log.info("Next Step: {} (Sequence: {})", nextStep.getName(), nextStep.getSequenceNumber());

                        application.setCoreApplicationStep(nextStep);
                        log.info("✅ Application successfully moved to next step: {}", nextStep.getName());

                        // Notify officers at the new step about the application
                        try {
                            String officerRole = mapStepToOfficerRole(nextStep.getName());
                            if (officerRole != null) {
                                log.info("🔔 About to notify officers in role: '{}' about application at new level", officerRole);
                                officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                                log.info("✅ Officer notification execution completed for role: {}", officerRole);
                            }
                        } catch (Exception notificationError) {
                            log.error("Error notifying officers after CEO approval for application {}: {}",
                                    applicationId, notificationError.getMessage());
                        }
                        log.info("=== APPLICATION LEVEL PROGRESSION COMPLETED ===");
                    }
                }

                applicationService.editCoreLicenseApplication(application);

                // Log CEO approval activity
                CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "CEO_APPROVAL");
                if (activityType != null) {
                    CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                    activity.setCoreLicenseApplication(application);
                    activity.setSysUserAccount(user);
                    activity.setCoreLicenseTypeActivity(activityType);
                    activity.setDescription("CEO approved application" + (boardMinutes != null && !boardMinutes.trim().isEmpty() ? ": " + boardMinutes : ""));
                    activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                    coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                }

                // Send approval notification to applicant
                try {
                    if (application.getSysUserAccount() != null &&
                            application.getSysUserAccount().getEmailAddress() != null) {

                        String applicantEmail = application.getSysUserAccount().getEmailAddress();
                        String applicantName = getApplicantFullName(application.getSysUserAccount());
                        String licenseType = application.getCoreLicenseType() != null ?
                                application.getCoreLicenseType().getName() : "Water Permit";

                        String subject = "Application Approved - License Payment Required: " + licenseType;
                        String emailBody = String.format("""
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                        <meta charset="UTF-8">
                                        <title>Application Approved</title>
                                        <style>
                                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                                        .header { background: #0ea5e9; color: white; padding: 20px; text-align: center; }
                                        .header .logo { width: 80px; height: 80px; margin-bottom: 10px; background-color: white; padding: 8px; border-radius: 6px; }
                                        .content { padding: 20px; background: #f9f9f9; }
                                        .footer { padding: 20px; text-align: center; color: #666; }
                                        .highlight { background: #dbeafe; padding: 15px; border-radius: 5px; margin: 15px 0; }
                                        </style>
                                        </head>
                                        <body>
                                        <div class="container">
                                        <div class="header">
                                        <img src="cid:logo" alt="NWRA Logo" class="logo">
                                        <p>National Water Resources Authority</p>
                                        <h1> Application Approved!</h1>
                                        </div>
                                        <div class="content">
                                        <p>Dear <strong>%s</strong>,</p>
                                        <p>Congratulations! Your %s application has been <strong>APPROVED</strong> by our CEO.</p>
                                        <div class="highlight">
                                        <p><strong>Application ID:</strong> %s</p>
                                        <p><strong>Status:</strong> APPROVED - Payment Required</p>
                                        </div>
                                        <p><strong>Next Steps:</strong></p>
                                        <ol>
                                        <li>Log into the NWRA e-Water Permit system</li>
                                        <li>Navigate to your application</li>
                                        <li>Make the required license fee payment</li>
                                        <li>Upload your payment receipt</li>
                                        <li>Your license will be issued after payment verification</li>
                                        </ol>
                                        </div>
                                        <div class="footer">
                                        <p>National Water Resources Authority<br>License Application System</p>
                                        </div>
                                        </div>
                                        </body>
                                        </html>
                                        """,
                                applicantName, licenseType, applicationId
                        );

                        String taskId = "ceo-approval-" + applicationId + "-" + System.currentTimeMillis();
                        emailQueueService.sendEmailAsync(taskId, applicantEmail, subject, emailBody);

                        log.info("✅ CEO approval notification sent to applicant: {}", applicantEmail);
                    }
                } catch (Exception emailError) {
                    log.error("Error sending CEO approval notification: {}", emailError.getMessage());
                }

                // Auto-generate license and permit for approved application
                String appType = application.getApplicationType();
                if ("NEW".equals(appType)) {
                    // NEW applications get new license numbers and include fees
                    try {
                        createLicenseAndInvoice(application, user);
                        log.info("License, permit and invoice created for NEW application");
                    } catch (Exception permitError) {
                        log.error("Error creating license, permit and invoice for NEW: {}", permitError.getMessage());
                        // Continue with approval even if creation fails
                    }
                } else if ("RENEWAL".equals(appType)) {
                    // RENEW applications maintain original license number but include fees
                    try {
                        createLicenseForRenewal(application, user);
                        log.info("License, permit and invoice created for RENEW application");
                    } catch (Exception permitError) {
                        log.error("Error creating license, permit and invoice for RENEW: {}", permitError.getMessage());
                        // Continue with approval even if creation fails
                    }
                } else if ("TRANSFER".equals(appType) || "VARIATION".equals(appType)) {
                    // TRANSFER and VARIATION maintain original license number with no fees
                    log.info("Creating license and permit for {} application (no fees required)", appType);
                    try {
                        createLicenseForVariationOrTransfer(application);
                        log.info("License and permit created for {} application", appType);
                    } catch (Exception permitError) {
                        log.error("Error creating license and permit for {}: {}", appType, permitError.getMessage());
                        // Continue with approval even if license creation fails
                    }
                } else {
                    // Default fallback for any other application types
                    try {
                        createLicenseAndInvoice(application, user);
                        log.info("License, permit and invoice created for {} application", appType);
                    } catch (Exception permitError) {
                        log.error("Error creating license, permit and invoice for {}: {}", appType, permitError.getMessage());
                        // Continue with approval even if creation fails
                    }
                }
            } else if ("refer_back".equals(action)) {
                // TODO: Save CEO referral data to database table
                log.info("Saving CEO referral - Board Minutes: {}", boardMinutes);
                // Return application - could go back to previous step or set specific status
                log.info("Application returned by CEO");

                // Update status to indicate return (could be DRS review or License Manager review)
                CoreApplicationStatus returnStatus =
                        coreApplicationStatusService.getCoreApplicationStatusByName("PENDING_SCHEDULE_AUTHORIZATION");
                if (returnStatus != null) {
                    application.setCoreApplicationStatus(returnStatus);
                }

                // Move back to previous step
                CoreApplicationStep currentStep = application.getCoreApplicationStep();
                if (currentStep != null) {
                    CoreApplicationStep previousStep = getApplicationStepBySequence(
                            application.getCoreLicenseType(), currentStep.getSequenceNumber() - 1);
                    if (previousStep != null) {
                        application.setCoreApplicationStep(previousStep);
                        log.info("Moved back to step: {} (sequence {})", previousStep.getName(), previousStep.getSequenceNumber());
                    }
                }

                applicationService.editCoreLicenseApplication(application);

                // Log CEO referral activity
                CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "APPROVE");
                if (activityType != null) {
                    CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                    activity.setCoreLicenseApplication(application);
                    activity.setSysUserAccount(user);
                    activity.setCoreLicenseTypeActivity(activityType);
                    activity.setDescription("CEO referred back application" + (boardMinutes != null && !boardMinutes.trim().isEmpty() ? ": " + boardMinutes : ""));
                    activity.setDateCreated(new Timestamp(System.currentTimeMillis()));
                    coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", action.equals("approve") ? "Application approved by CEO" : "Application referred back by CEO");
            response.put("status", action.equals("approve") ? "APPROVED" : "REFERRED_BACK");
            response.put("boardMinutes", boardMinutes);
            response.put("boardApprovalDate", boardApprovalDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing CEO decision: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generic document download endpoint
     */
    @GetMapping("/download-document/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) {
        try {
            log.info("Downloading document: {}", documentId);

            // Get document record
            CoreApplicationDocument document = documentService.getCoreApplicationDocumentById(documentId);
            if (document == null) {
                log.error("Document not found with ID: {}", documentId);
                return ResponseEntity.notFound().build();
            }

            // Get file path - handle both absolute and relative paths
            Path filePath;
            String documentUrl = document.getDocumentUrl();

            if (documentUrl.startsWith("/") || documentUrl.contains(":")) {
                filePath = Paths.get(documentUrl);
            } else {
                filePath = Paths.get(System.getProperty("user.dir"), documentUrl);
            }

            log.info("Looking for document at: {}", filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                log.error("Document file not found: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Schedule application for approval
     */
    @PostMapping("/schedule-approval/{applicationId}")
    public ResponseEntity<?> scheduleForApproval(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Move to next step in workflow
            CoreApplicationStep currentStep = application.getCoreApplicationStep();
            if (currentStep != null) {
                CoreApplicationStep nextStep = getApplicationStepBySequence(
                        application.getCoreLicenseType(), currentStep.getSequenceNumber() + 1);
                if (nextStep != null) {
                    application.setCoreApplicationStep(nextStep);
                    applicationService.editCoreLicenseApplication(application);
                }
            }
            updateAssessmentHandler(applicationId, token);

            return ResponseEntity.ok(Map.of("message", "Application scheduled for approval successfully"));
        } catch (Exception e) {
            log.error("Error scheduling application: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Status determination logic - can be expanded later when full workflow is implemented
    private String determineNewStatus(String action, String userRole, String currentStatus) {
        // Simplified status determination for now
        switch (action.toLowerCase()) {
            case "approve":
                return "APPROVED";
            case "reject":
                return "REJECTED";
            case "refer_back":
                return "REFERRED_BACK";
            default:
                return "PROCESSED";
        }
    }

    /**
     * Helper method to get applicant's full name
     */
    // private String getApplicantFullName(SysUserAccount userAccount) {
    //     if (userAccount == null) {
    //         return "Applicant";
    //     }

    //     String firstName = userAccount.getFirstName() != null ? userAccount.getFirstName() : "";
    //     String lastName = userAccount.getLastName() != null ? userAccount.getLastName() : "";
    //     String fullName = (firstName + " " + lastName).trim();

    //     if (fullName.isEmpty()) {
    //         return userAccount.getUsername() != null ? userAccount.getUsername() : "Applicant";
    //     }

    //     return fullName;
    // }

    //    @PostMapping("/request-revision/{applicationId}")
//    public ResponseEntity<?> requestRevision(
//            @PathVariable String applicationId,
//            @RequestBody Map<String, String> request,
//            @RequestHeader("Authorization") String token) {
//        try {
//            log.info("Requesting revision for application: {}", applicationId);
//            SysUserAccount user = getCurrentUser(token);
//            if (user == null)
//                return ResponseEntity.badRequest().body(Map.of("error", "Not authorised"));
//
//            String comment = request.get("comment");
//            if (comment == null || comment.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of("error", "Comment is required for revision request"));
//            }
//
//            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
//            if (application == null) {
//                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
//            }
//
//            // Get current user (assuming you have authentication context)
//            String currentUserId = user.getId();
//
//            // Update application status to NEEDS_REVISION
//            CoreApplicationStatus needsRevisionStatus = coreApplicationStatusService.getCoreApplicationStatusByName("NEEDS_REVISION");
//            if (needsRevisionStatus != null) {
//                application.setCoreApplicationStatus(needsRevisionStatus);
//                application.setStatus("NEEDS_REVISION");
//            }
//
//            // Set the revision comment (you might want to add a field for this)
//            // For now, we'll use the comment in the activity log
//
//            // Set last handled by current user
//            application.setLastHandledByUserId(currentUserId);
//
//            // Clear any existing referral fields since this is a revision request to applicant
//            application.clearReferral();
//
//            applicationService.editCoreLicenseApplication(application);
//
//            // Create activity log entry
//            CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
//            activity.setCoreLicenseApplication(application);
////            activity.seA("REQUEST_REVISION");
//            activity.setDescription("Revision requested: " + comment);
//            activity.setDescription(comment);
//            activity.setDateCreated(new java.sql.Timestamp(System.currentTimeMillis()));
//
//            // Set user if available
//            if (currentUserId != null) {
//                try {
//                    mw.nwra.ewaterpermit.model.SysUserAccount userAccount =
//                            new mw.nwra.ewaterpermit.model.SysUserAccount();
//                    userAccount.setId(currentUserId);
//                    activity.setSysUserAccount(userAccount);
//                } catch (Exception userError) {
//                    log.warn("Could not set user for activity: " + userError.getMessage());
//                }
//            }
//
//            coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
//
//            // Send email notification to applicant
//            try {
//                sendRevisionNotificationEmail(application, comment);
//            } catch (Exception emailError) {
//                log.warn("Failed to send revision notification email: " + emailError.getMessage());
//            }
//
//            log.info("Successfully requested revision for application {}", applicationId);
//            return ResponseEntity.ok(Map.of(
//                    "message", "Revision requested successfully",
//                    "status", "NEEDS_REVISION"
//            ));
//        } catch (Exception e) {
//            log.error("Error requesting revision: " + e.getMessage());
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }

    /**
     * Get revision details for an application
     */
    @GetMapping("/revision-details/{applicationId}")
    public ResponseEntity<?> getRevisionDetails(@PathVariable String applicationId) {
        try {
            log.info("Getting revision details for application: {}", applicationId);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Check if application needs revision
            boolean needsRevision = "NEEDS_REVISION".equalsIgnoreCase(application.getCoreApplicationStatus().getName());

            if (!needsRevision) {
                return ResponseEntity.ok(Map.of(
                        "needsRevision", false,
                        "message", "Application does not need revision"
                ));
            }

            // Get the latest revision request from activity log
            List<CoreLicenseApplicationActivity> activities =
                    coreLicenseApplicationActivityService.getAllCoreLicenseApplicationActivities()
                            .stream()
                            .filter(activity -> activity.getCoreLicenseApplication() != null &&
                                    activity.getCoreLicenseApplication().getId().equals(applicationId))
                            .filter(activity -> "REQUEST_REVISION".equals(activity.getCoreLicenseTypeActivity()))
                            .sorted((a, b) -> b.getDateCreated().compareTo(a.getDateCreated()))
                            .toList();

            Map<String, Object> revisionDetails = new HashMap<>();
            revisionDetails.put("needsRevision", true);
            revisionDetails.put("applicationId", applicationId);
            revisionDetails.put("status", application.getCoreApplicationStatus().getName());

            if (!activities.isEmpty()) {
                CoreLicenseApplicationActivity latestRevision = activities.get(0);
                revisionDetails.put("revisionReason", latestRevision.getDescription());
                revisionDetails.put("revisionDescription", latestRevision.getDescription());
                revisionDetails.put("revisionDate", latestRevision.getDateCreated());

                if (latestRevision.getSysUserAccount() != null) {
                    revisionDetails.put("requestedBy",
                            latestRevision.getSysUserAccount().getFirstName() + " " +
                                    latestRevision.getSysUserAccount().getLastName());
                }
            } else {
                revisionDetails.put("revisionReason", "Revision requested - please review and update your application");
            }

            return ResponseEntity.ok(revisionDetails);
        } catch (Exception e) {
            log.error("Error getting revision details: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Resubmit revised application
     */
    @PostMapping("/resubmit-application/{applicationId}")
    public ResponseEntity<?> resubmitApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            log.info("Resubmitting revised application: {}", applicationId);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Verify application is in revision status
            if (!"NEEDS_REVISION".equalsIgnoreCase(application.getCoreApplicationStatus().getName())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application is not in revision status"));
            }

            // Get current user (applicant)
            String currentUserId = getCurrentUser(token).getId();

            // Update application data with revised information
            // This would typically involve updating all the form fields
            // For now, we'll just change the status back to SUBMITTED

            CoreApplicationStatus submittedStatus = coreApplicationStatusService.getCoreApplicationStatusByName("SUBMITTED");
            if (submittedStatus != null) {
                application.setCoreApplicationStatus(submittedStatus);
                application.setStatus(submittedStatus);
            }

            // Update last handled by user (applicant)
            application.setLastHandledByUserId(currentUserId);

            // Update the application date to current time to show resubmission
//            application.setAu(new java.sql.Timestamp(System.currentTimeMillis()));

            applicationService.editCoreLicenseApplication(application);
            updateAssessmentHandler(applicationId, token);

            // Notify officers at the first workflow step about the resubmitted application
            try {
                CoreApplicationStep firstStep = getApplicationStepBySequence(application.getCoreLicenseType(), 1);
                if (firstStep != null) {
                    String officerRole = mapStepToOfficerRole(firstStep.getName());
                    if (officerRole != null) {
                        log.info("=== NOTIFYING OFFICERS AFTER APPLICATION RESUBMISSION ===");
                        log.info("Notifying role '{}' for step '{}'", officerRole, firstStep.getName());
                        officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                    }
                }
            } catch (Exception notificationError) {
                log.error("Error notifying officers after application resubmission for application {}: {}",
                        applicationId, notificationError.getMessage());
            }

            // Create activity log entry for resubmission
            CoreLicenseTypeActivity activityType = getOrCreateWorkflowActivity(application, "RESUBMIT");
            if (activityType != null) {
                CoreLicenseApplicationActivity activity = new CoreLicenseApplicationActivity();
                activity.setCoreLicenseApplication(application);
                activity.setCoreLicenseTypeActivity(activityType);
                activity.setDescription("Application updated and resubmitted by applicant");
                activity.setDateCreated(new Timestamp(System.currentTimeMillis()));

                if (currentUserId != null) {
                    try {
                        SysUserAccount userAccount = new SysUserAccount();
                        userAccount.setId(currentUserId);
                        activity.setSysUserAccount(userAccount);
                    } catch (Exception userError) {
                        log.warn("Could not set user for activity: " + userError.getMessage());
                    }
                }

                coreLicenseApplicationActivityService.addCoreLicenseApplicationActivity(activity);
            }

            log.info("Successfully resubmitted application {}", applicationId);
            return ResponseEntity.ok(Map.of(
                    "message", "Application resubmitted successfully",
                    "status", "SUBMITTED"
            ));
        } catch (Exception e) {
            log.error("Error resubmitting application: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Send revision notification email to applicant
     */
    private void sendRevisionNotificationEmail(CoreLicenseApplication application, String revisionReason) {
        try {
            if (application.getSysUserAccount().getEmailAddress() == null || application.getSysUserAccount().getEmailAddress().trim().isEmpty()) {
                log.warn("No applicant email found for application {}", application.getId());
                return;
            }

            String subject = "Revision Required - " + (application.getCoreLicenseType() != null ?
                    application.getCoreLicenseType().getName() : "Water Permit") + " Application";
            SysUserAccount sysUser = application.getSysUserAccount();
            String emailBody = String.format("""
                            <!DOCTYPE html>
                            <html lang="en">
                            <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Application Revision Required</title>
                            <style>
                            body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            line-height: 1.4;
                            margin: 0;
                            padding: 10px;
                            background-color: #f8f9fa;
                            color: #333;
                            }
                            .container {
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: #ffffff;
                            border-radius: 8px;
                            overflow: hidden;
                            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                            }
                            .header {
                            background-color: #f59e0b;
                            color: white;
                            padding: 15px 30px;
                            text-align: center;
                            }
                            .header h1 {
                            margin: 0;
                            font-size: 24px;
                            font-weight: 600;
                            }
                            .header p {
                            margin: 8px 0 0 0;
                            font-size: 16px;
                            opacity: 0.9;
                            }
                            .content {
                            padding: 20px 30px;
                            }
                            .greeting {
                            font-size: 16px;
                            margin-bottom: 16px;
                            }
                            .status {
                            background-color: #fef3c7;
                            color: #92400e;
                            padding: 10px 15px;
                            border-radius: 6px;
                            text-align: center;
                            font-weight: 600;
                            margin: 16px 0;
                            }
                            .details {
                            background-color: #f8fafc;
                            border: 1px solid #e2e8f0;
                            border-radius: 6px;
                            padding: 16px;
                            margin: 16px 0;
                            }
                            .details h3 {
                            margin: 0 0 12px 0;
                            font-size: 18px;
                            color: #1e293b;
                            }
                            .next-steps {
                            background-color: #dbeafe;
                            border: 1px solid #3b82f6;
                            border-radius: 6px;
                            padding: 20px;
                            margin: 24px 0;
                            }
                            .next-steps h3 {
                            margin: 0 0 12px 0;
                            color: #1e40af;
                            }
                            .next-steps ol {
                            margin: 0;
                            padding-left: 20px;
                            color: #1e40af;
                            }
                            .footer {
                            background-color: #f1f5f9;
                            padding: 30px;
                            text-align: center;
                            border-top: 1px solid #e2e8f0;
                            }
                            .footer p {
                            margin: 0;
                            color: #64748b;
                            font-size: 14px;
                            }
                            .footer .org-name {
                            font-weight: 600;
                            color: #1e293b;
                            margin-bottom: 4px;
                            }
                            .disclaimer {
                            font-size: 12px;
                            color: #94a3b8;
                            margin-top: 20px;
                            }
                            </style>
                            </head>
                            <body>
                            <div class="container">
                            <div class="header">
                            <h1>Revision Required</h1>
                            <p>National Water Resources Authority</p>
                            </div>
                            
                            <div class="content">
                                <div class="greeting">
                                    Dear <strong>%s</strong>,
                                </div>
                            
                                <p>Your %s application requires revision before it can be processed further.</p>
                            
                                <div class="status">
                                    Revision Required - Application ID: %s
                                </div>
                            
                                <div class="details">
                                    <h3>Revision Details</h3>
                                    <p>%s</p>
                                </div>
                            
                                <div class="next-steps">
                                    <h3>What to do next:</h3>
                                    <ol>
                                        <li>Log in to your account at the NWRA portal</li>
                                        <li>Go to "My Applications" section</li>
                                        <li>Click "Edit Application" for this application</li>
                                        <li>Make the necessary changes as requested</li>
                                        <li>Resubmit your application for review</li>
                                    </ol>
                                </div>
                            
                                <p>If you need clarification about the revision requirements, please contact our office.</p>
                            
                                <p>Thank you for your cooperation.</p>
                            </div>
                            
                            <div class="footer">
                                <p class="org-name">National Water Resources Authority</p>
                                <p>License Application System</p>
                                <p class="disclaimer">
                                    This is an automated message. Please do not reply to this email.
                                </p>
                            </div>
                            </div>
                            </body>
                            </html>
                            """,
                    sysUser.getFirstName() != null ? sysUser.getFirstName() : "Applicant",
                    application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "Water Permit",
                    application.getId(),
                    revisionReason
            );
            String taskId = "revision-" + application.getId() + "-" + System.currentTimeMillis();
            // Queue email for sending
            emailQueueService.sendEmailAsync(
                    taskId,
                    sysUser.getEmailAddress(),
                    subject,
                    emailBody
            );
            log.info("Revision request email queued for applicant: {}", sysUser.getEmailAddress());

            log.info("Revision notification email queued for application {}", application.getId());
        } catch (Exception e) {
            log.error("Error sending revision notification email: " + e.getMessage());
            throw new RuntimeException("Failed to send revision notification email", e);
        }
    }

    /**
     * Update last handled by user ID for an application
     */
    @PostMapping("/update-last-handler/{applicationId}")
    public ResponseEntity<?> updateLastHandler(
            @PathVariable String applicationId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String token) {
        try {
            log.info("Updating last handler for application: {}", applicationId);

            String lastHandledByUserId = getCurrentUser(token).getId();
            if (lastHandledByUserId == null || lastHandledByUserId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "lastHandledByUserId is required"));
            }

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Application not found"));
            }

            // Update the last handled by user ID
            application.setLastHandledByUserId(lastHandledByUserId);
            applicationService.editCoreLicenseApplication(application);
            updateAssessmentHandler(applicationId, token);
            log.info("Successfully updated last handler for application {} to user {}", applicationId, lastHandledByUserId);
            return ResponseEntity.ok(Map.of("message", "Last handler updated successfully"));
        } catch (Exception e) {
            log.error("Error updating last handler: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Determine the appropriate status when referring back based on the action
     */
//    private String determineReferBackStatus(String action) {
//        return switch (action) {
//            case "check_schedule" -> "AWAITING_MANAGER_REVIEW";
//            case "authorize_schedule" -> "PENDING_SCHEDULE_AUTHORIZATION";
//            case "board_approval" -> "READY_FOR_APPROVAL";
//            case "approve_field_assessment" -> "FIELD_ASSESSMENT_APPROVED";
//            default -> "REFERRED_BACK";
//        };
//    }

    /**
     * Create permit and invoice automatically when application is approved by CEO
     */

    private void createLicenseAndInvoice(CoreLicenseApplication application, SysUserAccount user) {
        try {
            log.info("Creating license and invoice for approved application: {}", application.getId());

            // Get license fees - use application-specific fee if set by manager, otherwise 0
            double invoiceAmount = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;

            // Get License fees type from database
            CoreFeesType licenseFeesType = coreFeesTypeService.getCoreFeesTypeById("3539d392-e8a0-11ef-b39b-c84bd6560e35");
            if (licenseFeesType == null) {
                licenseFeesType = coreFeesTypeService.getCoreFeesTypeByName("License fees");
            }

            // Create payment record first
            CoreApplicationPayment licensePayment = new CoreApplicationPayment();
            licensePayment.setCoreLicenseApplication(application);
            licensePayment.setCoreFeesType(licenseFeesType);
            licensePayment.setAmountPaid(invoiceAmount);
            licensePayment.setPaymentStatus("PENDING");
            licensePayment.setPaymentMethod("INVOICE_GENERATED");
            licensePayment.setNeedsVerification(false);
            licensePayment.setDateCreated(new Timestamp(System.currentTimeMillis()));

            // Save payment record (will be updated with license reference later)
            licensePayment = paymentService.createCoreApplicationPayment(licensePayment);
            log.info("License payment record created: Amount = {}, Status = PENDING", invoiceAmount);

            // Create license invoice with payment already linked
            CoreInvoice licenseInvoice = new CoreInvoice();
            licenseInvoice.setCoreLicenseApplication(application);
            licenseInvoice.setCoreApplicationPayment(licensePayment);
            licenseInvoice.setInvoiceType("LICENSE_FEE");
            licenseInvoice.setInvoiceStatus("PENDING");
            licenseInvoice.setAmount(invoiceAmount);
            licenseInvoice.setIssueDate(new Timestamp(System.currentTimeMillis()));
            licenseInvoice.setDescription("License fee for " + (application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "Water Permit"));
            licenseInvoice.setDateCreated(new Timestamp(System.currentTimeMillis()));
            licenseInvoice.setDateUpdated(new Timestamp(System.currentTimeMillis()));

            // Generate invoice number and set defaults
            licenseInvoice.generateInvoiceNumber();
            licenseInvoice.calculateDueDate();
            licenseInvoice.setDefaultPaymentInstructions();

            // Save invoice once with all data
            licenseInvoice = coreInvoiceService.addCoreInvoice(licenseInvoice);
            log.info("License invoice created: Number = {}, Amount = {}", licenseInvoice.getInvoiceNumber(), invoiceAmount);

            log.info("License payment record created: Amount = {}, Status = PENDING", invoiceAmount);

            // Create license record in PENDING_PAYMENT status
            CoreLicense license = new CoreLicense();
            license.setCoreLicenseApplication(application);
            license.setDocumentUrl("");
            license.setDateIssued(new java.sql.Date(System.currentTimeMillis()));

            // Generate license number (e.g., NWRA-2025-12345678)
            String licenseNumber = "NWRA-" + new SimpleDateFormat("yyyy").format(new Date()) + "-" +
                    String.format("%08d", (int) (Math.random() * 100000000));
            license.setLicenseNumber(licenseNumber);

            // Calculate expiry date based on license type (e.g., 5 years from now, minus 1 day)
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, application.getCoreLicenseType().getDefaultValidityLength()); // Add years
            cal.add(Calendar.DAY_OF_MONTH, -1); // Subtract 1 day (so if issued on 12th, expires on 11th)
            license.setExpirationDate(new java.sql.Date(cal.getTimeInMillis()));

            license.setDateCreated(new Timestamp(System.currentTimeMillis()));

            // Save license using service
            license = coreLicenseService.addCoreLicense(license);
            log.info("License record saved: ID = {}, Number = {}", license.getId(), license.getLicenseNumber());

            // Link the payment to the license and update
            licensePayment.setCoreLicense(license);
            paymentService.editCoreApplicationPayment(licensePayment);

            // Also create permit record for backward compatibility
            CoreLicensePermit permit = new CoreLicensePermit();
            permit.setCoreLicenseApplication(application);
            permit.setCoreApplicationPayment(licensePayment);
            permit.setInvoiceAmount(invoiceAmount);
            permit.setPermitStatus("PENDING_PAYMENT");
            permit.setIssueDate(new Timestamp(System.currentTimeMillis()));
            permit.setDateCreated(new Timestamp(System.currentTimeMillis()));
            permit.setDateUpdated(new Timestamp(System.currentTimeMillis()));
            permit.setInvoiceGeneratedDate(new Timestamp(System.currentTimeMillis()));
            permit.setPaymentVerified(false);
            permit.setPermitDownloadable(false);

            // Generate permit number
            permit.generatePermitNumber();

            // Calculate expiry date
            permit.calculateExpiryDate();

            // Set default conditions and QR code
            permit.setDefaultConditions();
            permit.generateQRCodeData();

            // Save permit
            permit = coreLicensePermitService.addCoreLicensePermit(permit);
            log.info("Permit record also created: ID = {}, Number = {}", permit.getId(), permit.getPermitNumber());

            log.info("License and permit will be downloadable only after payment verification");

            // Send email to applicant about invoice
            try {
                sendInvoiceNotificationEmail(application, invoiceAmount);
            } catch (Exception emailError) {
                log.warn("Failed to send invoice notification email: {}", emailError.getMessage());
            }

            log.info("License and invoice creation completed for application: {}", application.getId());
        } catch (Exception e) {
            log.error("Error in createLicenseAndInvoice: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create license record for RENEWAL applications
     * Maintains the original license number while creating a new license record with extended expiry
     */
    private void createLicenseForRenewal(CoreLicenseApplication application, SysUserAccount user) {
        try {
            log.info("Creating license for RENEWAL application: {}", application.getId());

            // Get the original license information
            String originalLicenseId = application.getOriginalLicenseId();
            CoreLicense originalLicense = null;

            if (originalLicenseId != null && !originalLicenseId.trim().isEmpty()) {
                originalLicense = coreLicenseService.getCoreLicenseById(originalLicenseId);
                if (originalLicense == null) {
                    log.warn("Original license not found with ID: {}, searching by license number", originalLicenseId);
                    // Try to find by license number if ID lookup fails
                    List<CoreLicense> licenses = coreLicenseService.getCoreLicensesByLicenseNumber(originalLicenseId);
                    if (!licenses.isEmpty()) {
                        originalLicense = licenses.get(0);
                    }
                }
            }

            if (originalLicense == null) {
                throw new RuntimeException("Original license not found for renewal application: " + application.getId());
            }

            log.info("Found original license: {} (Status: {})", originalLicense.getLicenseNumber(), originalLicense.getStatus());

            // Deactivate the original license with accountability
            originalLicense.setStatus("RENEWED");
            originalLicense.setDateUpdated(new java.sql.Timestamp(System.currentTimeMillis()));
            coreLicenseService.editCoreLicense(originalLicense);
            log.info("Deactivated original license: {}", originalLicense.getLicenseNumber());

            // Get license fees - use application-specific fee if set by manager, otherwise 0
            double invoiceAmount = application.getLicenseFee() != null ? application.getLicenseFee() : 0.0;

            // Get License fees type from database
            CoreFeesType licenseFeesType = coreFeesTypeService.getCoreFeesTypeById("3539d392-e8a0-11ef-b39b-c84bd6560e35");
            if (licenseFeesType == null) {
                licenseFeesType = coreFeesTypeService.getCoreFeesTypeByName("License fees");
            }

            // Create payment record for renewal fees
            CoreApplicationPayment licensePayment = new CoreApplicationPayment();
            licensePayment.setCoreLicenseApplication(application);
            licensePayment.setCoreFeesType(licenseFeesType);
            licensePayment.setAmountPaid(invoiceAmount);
            licensePayment.setPaymentStatus("PENDING");
            licensePayment.setPaymentMethod("INVOICE_GENERATED");
            licensePayment.setNeedsVerification(false);
            licensePayment.setDateCreated(new Timestamp(System.currentTimeMillis()));
            licensePayment = paymentService.createCoreApplicationPayment(licensePayment);

            // Create license invoice
            CoreInvoice licenseInvoice = new CoreInvoice();
            licenseInvoice.setCoreLicenseApplication(application);
            licenseInvoice.setCoreApplicationPayment(licensePayment);
            licenseInvoice.setInvoiceType("LICENSE_RENEWAL_FEE");
            licenseInvoice.setInvoiceStatus("PENDING");
            licenseInvoice.setAmount(invoiceAmount);
            licenseInvoice.setIssueDate(new Timestamp(System.currentTimeMillis()));
            licenseInvoice.setDescription("License renewal fee for " + originalLicense.getLicenseNumber());
            licenseInvoice.setDateCreated(new Timestamp(System.currentTimeMillis()));
            licenseInvoice.setDateUpdated(new Timestamp(System.currentTimeMillis()));
            licenseInvoice.generateInvoiceNumber();
            licenseInvoice.calculateDueDate();
            licenseInvoice.setDefaultPaymentInstructions();
            licenseInvoice = coreInvoiceService.addCoreInvoice(licenseInvoice);

            // Create new license record with same license number but extended expiry
            CoreLicense renewedLicense = new CoreLicense();
            renewedLicense.setCoreLicenseApplication(application);
            renewedLicense.setDocumentUrl("");
            renewedLicense.setDateIssued(new java.sql.Date(System.currentTimeMillis()));

            // CRITICAL: Maintain the same license number
            renewedLicense.setLicenseNumber(originalLicense.getLicenseNumber());

            // Set license tracking fields
            renewedLicense.setStatus("ACTIVE");
            renewedLicense.setParentLicenseId(originalLicense.getId());
            renewedLicense.setLicenseVersion(originalLicense.getLicenseVersion() + 1);

            // Calculate new expiry date (extend from original expiry, not current date, minus 1 day)
            Calendar cal = Calendar.getInstance();
            cal.setTime(originalLicense.getExpirationDate());
            cal.add(Calendar.YEAR, application.getCoreLicenseType().getDefaultValidityLength());
            cal.add(Calendar.DAY_OF_MONTH, -1); // Subtract 1 day
            renewedLicense.setExpirationDate(new java.sql.Date(cal.getTimeInMillis()));
            renewedLicense.setDateCreated(new Timestamp(System.currentTimeMillis()));

            // Save renewed license
            renewedLicense = coreLicenseService.addCoreLicense(renewedLicense);
            log.info("Renewed license created: ID = {}, Number = {} (Version {})",
                    renewedLicense.getId(), renewedLicense.getLicenseNumber(), renewedLicense.getLicenseVersion());

            // Link payment to renewed license
            licensePayment.setCoreLicense(renewedLicense);
            paymentService.editCoreApplicationPayment(licensePayment);

            // Create permit record
            CoreLicensePermit permit = new CoreLicensePermit();
            permit.setCoreLicenseApplication(application);
            permit.setCoreApplicationPayment(licensePayment);
            permit.setInvoiceAmount(invoiceAmount);
            permit.setPermitStatus("PENDING_PAYMENT");
            permit.setIssueDate(new Timestamp(System.currentTimeMillis()));
            permit.setDateCreated(new Timestamp(System.currentTimeMillis()));
            permit.setDateUpdated(new Timestamp(System.currentTimeMillis()));
            permit.setInvoiceGeneratedDate(new Timestamp(System.currentTimeMillis()));
            permit.setPaymentVerified(false);
            permit.setPermitDownloadable(false);
            permit.generatePermitNumber();
            permit.calculateExpiryDate();
            permit.setDefaultConditions();
            permit.generateQRCodeData();
            permit = coreLicensePermitService.addCoreLicensePermit(permit);

            // Send email notification
            try {
                sendRenewalInvoiceNotificationEmail(application, originalLicense.getLicenseNumber(), invoiceAmount);
            } catch (Exception emailError) {
                log.warn("Failed to send renewal invoice notification email: {}", emailError.getMessage());
            }

            log.info("License renewal completed: Original license {} deactivated, new version {} created",
                    originalLicense.getLicenseNumber(), renewedLicense.getLicenseVersion());

        } catch (Exception e) {
            log.error("Error in createLicenseForRenewal: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Send renewal invoice notification email to applicant
     */
    private void sendRenewalInvoiceNotificationEmail(CoreLicenseApplication application, String licenseNumber, double invoiceAmount) {
        try {
            if (application.getSysUserAccount() != null && application.getSysUserAccount().getEmailAddress() != null) {
                String applicantEmail = application.getSysUserAccount().getEmailAddress();
                String applicantName = getApplicantFullName(application.getSysUserAccount());
                String licenseType = application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "Water Permit";

                String subject = "License Renewal Approved - Invoice Generated: " + licenseNumber;
                String emailBody = String.format("""
                                <!DOCTYPE html>
                                <html lang="en">
                                <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>License Renewal Approved</title>
                                <style>
                                body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                line-height: 1.4;
                                margin: 0;
                                padding: 10px;
                                background-color: #f8f9fa;
                                color: #333;
                                }
                                .container {
                                max-width: 600px;
                                margin: 0 auto;
                                background-color: #ffffff;
                                border-radius: 8px;
                                overflow: hidden;
                                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                }
                                .header {
                                background-color: #10b981;
                                color: white;
                                padding: 15px 30px;
                                text-align: center;
                                }
                                .header .logo {
                                width: 80px;
                                height: 80px;
                                margin-bottom: 10px;
                                background-color: white;
                                padding: 8px;
                                border-radius: 6px;
                                }
                                .header h1 {
                                margin: 0;
                                font-size: 24px;
                                font-weight: 600;
                                }
                                .header p {
                                margin: 8px 0 0 0;
                                font-size: 16px;
                                opacity: 0.9;
                                }
                                .content {
                                padding: 20px 30px;
                                }
                                .greeting {
                                font-size: 16px;
                                margin-bottom: 16px;
                                }
                                .status {
                                background-color: #d1fae5;
                                color: #065f46;
                                padding: 10px 15px;
                                border-radius: 6px;
                                text-align: center;
                                font-weight: 600;
                                margin: 16px 0;
                                }
                                .details {
                                background-color: #f8fafc;
                                border: 1px solid #e2e8f0;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                                }
                                .details h3 {
                                margin: 0 0 12px 0;
                                font-size: 18px;
                                color: #1e293b;
                                }
                                .license-info {
                                display: flex;
                                justify-content: space-between;
                                margin: 8px 0;
                                }
                                .license-info span:first-child {
                                font-weight: 600;
                                color: #475569;
                                }
                                .amount {
                                font-size: 18px;
                                font-weight: 700;
                                color: #059669;
                                }
                                .next-steps {
                                background-color: #dbeafe;
                                border: 1px solid #3b82f6;
                                border-radius: 6px;
                                padding: 20px;
                                margin: 24px 0;
                                }
                                .next-steps h3 {
                                margin: 0 0 12px 0;
                                color: #1e40af;
                                }
                                .next-steps ol {
                                margin: 0;
                                padding-left: 20px;
                                color: #1e40af;
                                }
                                .important {
                                background-color: #fef3c7;
                                border: 1px solid #f59e0b;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                                }
                                .important strong {
                                color: #92400e;
                                }
                                .footer {
                                background-color: #f1f5f9;
                                padding: 30px;
                                text-align: center;
                                border-top: 1px solid #e2e8f0;
                                }
                                .footer p {
                                margin: 0;
                                color: #64748b;
                                font-size: 14px;
                                }
                                .footer .org-name {
                                font-weight: 600;
                                color: #1e293b;
                                margin-bottom: 4px;
                                }
                                .disclaimer {
                                font-size: 12px;
                                color: #94a3b8;
                                margin-top: 20px;
                                }
                                </style>
                                </head>
                                <body>
                                <div class="container">
                                <div class="header">
                                <img src="cid:logo" alt="NWRA Logo" class="logo">
                                <p>National Water Resources Authority</p>
                                <h1>License Renewal Approved</h1>
                                </div>
                                
                                <div class="content">
                                    <div class="greeting">
                                        Dear <strong>%s</strong>,
                                    </div>
                                
                                    <p>Your %s renewal application has been approved!</p>
                                
                                    <div class="status">
                                        License Renewal Approved
                                    </div>
                                
                                    <div class="details">
                                        <h3>License Details</h3>
                                        <div class="license-info">
                                            <span>License Number:</span>
                                            <span><strong>%s (RENEWED)</strong></span>
                                        </div>
                                        <div class="license-info">
                                            <span>Renewal Fee:</span>
                                            <span class="amount">MWK %,.2f</span>
                                        </div>
                                    </div>
                                
                                    <div class="next-steps">
                                        <h3>Next Steps:</h3>
                                        <ol>
                                            <li>Pay the renewal fee using the invoice generated</li>
                                            <li>Upload payment receipt in your application portal</li>
                                            <li>Your renewed license will be available for download after payment verification</li>
                                        </ol>
                                    </div>
                                
                                    <div class="important">
                                        <strong>Important:</strong> Your license number remains the same, but the validity period has been extended.
                                    </div>
                                </div>
                                
                                <div class="footer">
                                    <p class="org-name">National Water Resources Authority</p>
                                    <p>License Application System</p>
                                    <p class="disclaimer">
                                        This is an automated message. Please do not reply to this email.
                                    </p>
                                </div>
                                </div>
                                </body>
                                </html>
                                """,
                        applicantName, licenseType, licenseNumber, invoiceAmount
                );

                String taskId = "renewal-approved-" + application.getId() + "-" + System.currentTimeMillis();
                emailQueueService.sendEmailAsync(taskId, applicantEmail, subject, emailBody);
                log.info("Renewal invoice email sent to: {} for license: {}", applicantEmail, licenseNumber);
            }
        } catch (Exception e) {
            log.error("Error sending renewal invoice email: {}", e.getMessage());
        }
    }

    /**
     * Create license record for TRANSFER and VARIATION applications
     * Maintains the original license number while creating a new license record
     */
    private void createLicenseForVariationOrTransfer(CoreLicenseApplication application) {
        try {
            log.info("Creating license for {} application: {}", application.getApplicationType(), application.getId());

            // Get the original license information
            String originalLicenseId = application.getOriginalLicenseId();
            CoreLicense originalLicense = null;

            if (originalLicenseId != null && !originalLicenseId.trim().isEmpty()) {
                originalLicense = coreLicenseService.getCoreLicenseById(originalLicenseId);
                if (originalLicense == null) {
                    log.warn("Original license not found with ID: {}, searching by license number", originalLicenseId);
                    // Try to find by license number if ID lookup fails
                    List<CoreLicense> licenses = coreLicenseService.getCoreLicensesByLicenseNumber(originalLicenseId);
                    if (!licenses.isEmpty()) {
                        originalLicense = licenses.get(0);
                    }
                }
            }

            if (originalLicense != null) {
                // Deactivate the original license with accountability
                String newStatus = "TRANSFER".equals(application.getApplicationType()) ? "TRANSFERRED" : "VARIED";
                originalLicense.setStatus(newStatus);
                originalLicense.setDateUpdated(new java.sql.Timestamp(System.currentTimeMillis()));
                coreLicenseService.editCoreLicense(originalLicense);
                log.info("Deactivated original license: {} (Status: {})", originalLicense.getLicenseNumber(), newStatus);
            }

            // Create new license record
            CoreLicense newLicense = new CoreLicense();
            newLicense.setCoreLicenseApplication(application);
            newLicense.setDocumentUrl("");
            newLicense.setDateIssued(new java.sql.Date(System.currentTimeMillis()));

            // Maintain the original license number for consistency
            String licenseNumber;
            if (originalLicense != null && originalLicense.getLicenseNumber() != null) {
                licenseNumber = originalLicense.getLicenseNumber();
                log.info("Maintaining original license number: {}", licenseNumber);
            } else {
                // Fallback: generate new license number if original not found
                licenseNumber = "NWRA-" + new SimpleDateFormat("yyyy").format(new Date()) + "-" +
                        String.format("%08d", (int) (Math.random() * 100000000));
                log.warn("Original license number not found, generated new one: {}", licenseNumber);
            }
            newLicense.setLicenseNumber(licenseNumber);

            // Set license tracking fields
            newLicense.setStatus("ACTIVE");
            if (originalLicense != null) {
                newLicense.setParentLicenseId(originalLicense.getId());
                newLicense.setLicenseVersion(originalLicense.getLicenseVersion() + 1);
            } else {
                newLicense.setLicenseVersion(1);
            }

            // Calculate expiry date based on license type and application type
            Calendar cal = Calendar.getInstance();
            if ("VARIATION".equals(application.getApplicationType()) && originalLicense != null) {
                // For variation, maintain the original expiry date
                newLicense.setExpirationDate(originalLicense.getExpirationDate());
                log.info("Maintaining original expiry date for variation: {}", originalLicense.getExpirationDate());
            } else if ("TRANSFER".equals(application.getApplicationType()) && originalLicense != null) {
                // For transfer, maintain the original expiry date
                newLicense.setExpirationDate(originalLicense.getExpirationDate());
                log.info("Maintaining original expiry date for transfer: {}", originalLicense.getExpirationDate());
            } else {
                // Fallback: calculate new expiry date (minus 1 day)
                cal.add(Calendar.YEAR, application.getCoreLicenseType().getDefaultValidityLength());
                cal.add(Calendar.DAY_OF_MONTH, -1); // Subtract 1 day
                newLicense.setExpirationDate(new java.sql.Date(cal.getTimeInMillis()));
            }

            newLicense.setDateCreated(new Timestamp(System.currentTimeMillis()));

            // Save license using service
            newLicense = coreLicenseService.addCoreLicense(newLicense);
            log.info("License record saved for {}: ID = {}, Number = {}",
                    application.getApplicationType(), newLicense.getId(), newLicense.getLicenseNumber());

            // Create permit record for backward compatibility (no payment required)
            CoreLicensePermit permit = new CoreLicensePermit();
            permit.setCoreLicenseApplication(application);
            permit.setCoreApplicationPayment(null); // No payment required for TRANSFER/VARIATION
            permit.setInvoiceAmount(0.0);
            permit.setPermitStatus("APPROVED"); // Directly approved as no payment needed
            permit.setIssueDate(new Timestamp(System.currentTimeMillis()));
            permit.setDateCreated(new Timestamp(System.currentTimeMillis()));
            permit.setDateUpdated(new Timestamp(System.currentTimeMillis()));
            permit.setInvoiceGeneratedDate(new Timestamp(System.currentTimeMillis()));
            permit.setPaymentVerified(true); // No payment verification needed
            permit.setPermitDownloadable(true); // Immediately downloadable

            // Generate permit number
            permit.generatePermitNumber();

            // Set expiry date same as license
//            permit.setExpiryDate(newLicense.getExpirationDate());
            permit.setExpiryDate(new Timestamp(newLicense.getExpirationDate().getTime()));

            // Set default conditions and QR code
            permit.setDefaultConditions();
            permit.generateQRCodeData();

            // Save permit
            permit = coreLicensePermitService.addCoreLicensePermit(permit);
            log.info("Permit record created for {}: ID = {}, Number = {}",
                    application.getApplicationType(), permit.getId(), permit.getPermitNumber());

            // For transfer applications, update the transfer recipient information
            if ("TRANSFER".equals(application.getApplicationType()) && application.getTransferToUserId() != null) {
                // Verify the application ownership was transferred correctly
                log.info("=== VERIFYING TRANSFER OWNERSHIP ===");
                log.info("Application owner ID: {}", application.getOwnerId());
                log.info("Transfer to user ID: {}", application.getTransferToUserId());
                log.info("License {} created for transferred application", licenseNumber);

                if (!application.getTransferToUserId().equals(application.getOwnerId())) {
                    log.error("OWNERSHIP TRANSFER ISSUE: Application owner ID ({}) does not match transfer target ({})",
                            application.getOwnerId(), application.getTransferToUserId());
                }
            }

            log.info("License and permit creation completed for {} application: {}",
                    application.getApplicationType(), application.getId());

        } catch (Exception e) {
            log.error("Error in createLicenseForVariationOrTransfer: {}", e.getMessage(), e);
            throw e;
        }
    }


    /**
     * Send renewal invoice notification email to applicant
     */
    private void sendRenewalInvoiceNotificationEmail(CoreLicenseApplication application, double invoiceAmount, String licenseNumber) {
        try {
            if (application.getSysUserAccount() != null &&
                    application.getSysUserAccount().getEmailAddress() != null) {

                String applicantEmail = application.getSysUserAccount().getEmailAddress();
                String applicantName = getApplicantFullName(application.getSysUserAccount());
                String licenseType = application.getCoreLicenseType() != null ?
                        application.getCoreLicenseType().getName() : "Water Permit";

                String subject = "License Renewal Approved - Invoice Generated for " + licenseType;
                String emailBody = String.format("""
                                <!DOCTYPE html>
                                <html lang="en">
                                <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>License Renewal Approved</title>
                                <style>
                                body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                line-height: 1.4;
                                margin: 0;
                                padding: 10px;
                                background-color: #f8f9fa;
                                color: #333;
                                }
                                .container {
                                max-width: 600px;
                                margin: 0 auto;
                                background-color: #ffffff;
                                border-radius: 8px;
                                overflow: hidden;
                                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                }
                                .header {
                                background-color: #10b981;
                                color: white;
                                padding: 15px 30px;
                                text-align: center;
                                }
                                .header h1 {
                                margin: 0;
                                font-size: 24px;
                                font-weight: 600;
                                }
                                .header p {
                                margin: 8px 0 0 0;
                                font-size: 16px;
                                opacity: 0.9;
                                }
                                .content {
                                padding: 20px 30px;
                                }
                                .greeting {
                                font-size: 16px;
                                margin-bottom: 16px;
                                }
                                .status {
                                background-color: #d1fae5;
                                color: #065f46;
                                padding: 10px 15px;
                                border-radius: 6px;
                                text-align: center;
                                font-weight: 600;
                                margin: 16px 0;
                                }
                                .good-news {
                                background-color: #ecfdf5;
                                border: 1px solid #10b981;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                                text-align: center;
                                }
                                .good-news h2 {
                                margin: 0 0 8px 0;
                                color: #065f46;
                                font-size: 20px;
                                }
                                .details {
                                background-color: #f8fafc;
                                border: 1px solid #e2e8f0;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                                }
                                .details h3 {
                                margin: 0 0 12px 0;
                                font-size: 18px;
                                color: #1e293b;
                                }
                                .license-info {
                                display: flex;
                                justify-content: space-between;
                                margin: 8px 0;
                                }
                                .license-info span:first-child {
                                font-weight: 600;
                                color: #475569;
                                }
                                .payment-section {
                                background-color: #fef3c7;
                                border: 1px solid #f59e0b;
                                border-radius: 6px;
                                padding: 20px;
                                margin: 24px 0;
                                }
                                .payment-section h3 {
                                margin: 0 0 16px 0;
                                color: #92400e;
                                font-size: 18px;
                                }
                                .payment-details {
                                margin: 12px 0;
                                }
                                .amount {
                                font-size: 18px;
                                font-weight: 700;
                                color: #059669;
                                }
                                .next-steps {
                                background-color: #dbeafe;
                                border: 1px solid #3b82f6;
                                border-radius: 6px;
                                padding: 20px;
                                margin: 24px 0;
                                }
                                .next-steps h3 {
                                margin: 0 0 12px 0;
                                color: #1e40af;
                                }
                                .next-steps ol {
                                margin: 0;
                                padding-left: 20px;
                                color: #1e40af;
                                }
                                .important {
                                background-color: #fef3c7;
                                border: 1px solid #f59e0b;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                                }
                                .important strong {
                                color: #92400e;
                                }
                                .footer {
                                background-color: #f1f5f9;
                                padding: 30px;
                                text-align: center;
                                border-top: 1px solid #e2e8f0;
                                }
                                .footer p {
                                margin: 0;
                                color: #64748b;
                                font-size: 14px;
                                }
                                .footer .org-name {
                                font-weight: 600;
                                color: #1e293b;
                                margin-bottom: 4px;
                                }
                                .disclaimer {
                                font-size: 12px;
                                color: #94a3b8;
                                margin-top: 20px;
                                }
                                </style>
                                </head>
                                <body>
                                <div class="container">
                                <div class="header">
                                <h1>License Renewal Approved</h1>
                                <p>National Water Resources Authority</p>
                                </div>
                                
                                <div class="content">
                                    <div class="greeting">
                                        Dear <strong>%s</strong>,
                                    </div>
                                
                                    <div class="good-news">
                                        <h2>Great News!</h2>
                                        <p>Your %s renewal application has been approved by the CEO and Board.</p>
                                    </div>
                                
                                    <div class="status">
                                        License Renewal Approved - Application ID: %s
                                    </div>
                                
                                    <div class="details">
                                        <h3>License Details</h3>
                                        <div class="license-info">
                                            <span>License Number:</span>
                                            <span><strong>%s (maintained from original)</strong></span>
                                        </div>
                                        <div class="license-info">
                                            <span>Application ID:</span>
                                            <span>%s</span>
                                        </div>
                                    </div>
                                
                                    <div class="payment-section">
                                        <h3>Next Steps - Renewal Fee Payment</h3>
                                        <div class="payment-details">
                                            • An invoice has been generated for your renewal fee<br>
                                            • Invoice Amount: <span class="amount">MWK %,.2f</span><br>
                                            • Payment Status: Pending
                                        </div>
                                    </div>
                                
                                    <div class="next-steps">
                                        <h3>Payment Instructions:</h3>
                                        <ol>
                                            <li>Log into your account to view and download the invoice</li>
                                            <li>Pay the renewal fee using one of our approved payment methods</li>
                                            <li>If paying online, your payment will be automatically verified</li>
                                            <li>If paying manually (bank transfer/cash), upload your payment receipt</li>
                                            <li>Once payment is verified, your renewed license permit will be available for download</li>
                                        </ol>
                                    </div>
                                
                                    <div class="important">
                                        <strong>Important:</strong> Your renewed license permit will only be available for download after payment verification.
                                    </div>
                                
                                    <p>Thank you for renewing with NWRA.</p>
                                </div>
                                
                                <div class="footer">
                                    <p class="org-name">National Water Resources Authority</p>
                                    <p>License Application System</p>
                                    <p class="disclaimer">
                                        This is an automated message. Please do not reply to this email.
                                    </p>
                                </div>
                                </div>
                                </body>
                                </html>
                                """,
                        applicantName, licenseType, application.getId(), licenseNumber, application.getId(), invoiceAmount
                );

                String taskId = "renewal-invoice-generated-" + application.getId() + "-" + System.currentTimeMillis();
                emailQueueService.sendEmailAsync(taskId, applicantEmail, subject, emailBody);

                log.info("Renewal invoice notification email sent to: {} for application: {}", applicantEmail, application.getId());
            } else {
                log.warn("No email address found for applicant of renewal application: {}", application.getId());
            }
        } catch (Exception e) {
            log.error("Error sending renewal invoice notification email: {}", e.getMessage());
            throw new RuntimeException("Failed to send renewal invoice notification email", e);
        }
    }

    /**
     * Send invoice notification email to applicant
     */
    private void sendInvoiceNotificationEmail(CoreLicenseApplication application, double invoiceAmount) {
        try {
            if (application.getSysUserAccount() != null &&
                    application.getSysUserAccount().getEmailAddress() != null) {

                String applicantEmail = application.getSysUserAccount().getEmailAddress();
                String applicantName = getApplicantFullName(application.getSysUserAccount());
                String licenseType = application.getCoreLicenseType() != null ?
                        application.getCoreLicenseType().getName() : "Water Permit";

                String subject = "License Approved - Invoice Generated for " + licenseType;
                String emailBody = String.format("""
                                <!DOCTYPE html>
                                <html lang="en">
                                <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>License Approved</title>
                                <style>
                                body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                line-height: 1.4;
                                margin: 0;
                                padding: 10px;
                                background-color: #f8f9fa;
                                color: #333;
                                }
                                .container {
                                max-width: 600px;
                                margin: 0 auto;
                                background-color: #ffffff;
                                border-radius: 8px;
                                overflow: hidden;
                                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                }
                                .header {
                                background-color: #10b981;
                                color: white;
                                padding: 15px 30px;
                                text-align: center;
                                }
                                .header .logo {
                                width: 80px;
                                height: 80px;
                                margin-bottom: 10px;
                                background-color: white;
                                padding: 8px;
                                border-radius: 6px;
                                }
                                .header h1 {
                                margin: 0;
                                font-size: 24px;
                                font-weight: 600;
                                }
                                .header p {
                                margin: 8px 0 0 0;
                                font-size: 16px;
                                opacity: 0.9;
                                }
                                .content {
                                padding: 20px 30px;
                                }
                                .greeting {
                                font-size: 16px;
                                margin-bottom: 16px;
                                }
                                .status {
                                background-color: #d1fae5;
                                color: #065f46;
                                padding: 10px 15px;
                                border-radius: 6px;
                                text-align: center;
                                font-weight: 600;
                                margin: 16px 0;
                                }
                                .good-news {
                                background-color: #ecfdf5;
                                border: 1px solid #10b981;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                                text-align: center;
                                }
                                .good-news h2 {
                                margin: 0 0 8px 0;
                                color: #065f46;
                                font-size: 20px;
                                }
                                .details {
                                background-color: #f8fafc;
                                border: 1px solid #e2e8f0;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                                }
                                .details h3 {
                                margin: 0 0 12px 0;
                                font-size: 18px;
                                color: #1e293b;
                                }
                                .license-info {
                                display: flex;
                                justify-content: space-between;
                                margin: 8px 0;
                                }
                                .license-info span:first-child {
                                font-weight: 600;
                                color: #475569;
                                }
                                .payment-section {
                                background-color: #fef3c7;
                                border: 1px solid #f59e0b;
                                border-radius: 6px;
                                padding: 20px;
                                margin: 24px 0;
                                }
                                .payment-section h3 {
                                margin: 0 0 16px 0;
                                color: #92400e;
                                font-size: 18px;
                                }
                                .payment-details {
                                margin: 12px 0;
                                }
                                .amount {
                                font-size: 18px;
                                font-weight: 700;
                                color: #059669;
                                }
                                .next-steps {
                                background-color: #dbeafe;
                                border: 1px solid #3b82f6;
                                border-radius: 6px;
                                padding: 20px;
                                margin: 24px 0;
                                }
                                .next-steps h3 {
                                margin: 0 0 12px 0;
                                color: #1e40af;
                                }
                                .next-steps ol {
                                margin: 0;
                                padding-left: 20px;
                                color: #1e40af;
                                }
                                .important {
                                background-color: #fef3c7;
                                border: 1px solid #f59e0b;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                                }
                                .important strong {
                                color: #92400e;
                                }
                                .footer {
                                background-color: #f1f5f9;
                                padding: 30px;
                                text-align: center;
                                border-top: 1px solid #e2e8f0;
                                }
                                .footer p {
                                margin: 0;
                                color: #64748b;
                                font-size: 14px;
                                }
                                .footer .org-name {
                                font-weight: 600;
                                color: #1e293b;
                                margin-bottom: 4px;
                                }
                                .disclaimer {
                                font-size: 12px;
                                color: #94a3b8;
                                margin-top: 20px;
                                }
                                </style>
                                </head>
                                <body>
                                <div class="container">
                                <div class="header">
                                <img src="cid:logo" alt="NWRA Logo" class="logo">
                                <p>National Water Resources Authority</p>
                                <h1>License Approved</h1>
                                </div>
                                
                                <div class="content">
                                    <div class="greeting">
                                        Dear <strong>%s</strong>,
                                    </div>
                                
                                    <div class="good-news">
                                        <h2>Congratulations!</h2>
                                        <p>Your %s application has been approved and license created.</p>
                                    </div>
                                
                                    <div class="status">
                                        License Approved - Application ID: %s
                                    </div>
                                
                                    <div class="payment-section">
                                        <h3>Next Steps - License Fee Payment</h3>
                                        <div class="payment-details">
                                            • An invoice has been generated for your license fee<br>
                                            • Invoice Amount: <span class="amount">MWK %,.2f</span><br>
                                            • Payment Status: Pending
                                        </div>
                                    </div>
                                
                                    <div class="next-steps">
                                        <h3>Payment Instructions:</h3>
                                        <ol>
                                            <li>Log into your account to view and download the invoice</li>
                                            <li>Pay the license fee using one of our approved payment methods</li>
                                            <li>If paying online, your payment will be automatically verified</li>
                                            <li>If paying manually (bank transfer/cash), upload your payment receipt</li>
                                            <li>Once payment is verified, your license permit will be available for pickup</li>
                                        </ol>
                                    </div>
                                
                                </div>
                                
                                <div class="footer">
                                    <p class="org-name">National Water Resources Authority</p>
                                    <p>License Application System</p>
                                    <p class="disclaimer">
                                        This is an automated message. Please do not reply to this email.
                                    </p>
                                </div>
                                </div>
                                </body>
                                </html>
                                """,

                        applicantName, licenseType, application.getId(), invoiceAmount
                );

                String taskId = "invoice-generated-" + application.getId() + "-" + System.currentTimeMillis();
                emailQueueService.sendEmailAsync(taskId, applicantEmail, subject, emailBody);

                log.info("Invoice notification email sent to: {} for application: {}", applicantEmail, application.getId());
            } else {
                log.warn("No email address found for applicant of application: {}", application.getId());
            }
        } catch (Exception e) {
            log.error("Error sending invoice notification email: {}", e.getMessage());
            throw new RuntimeException("Failed to send invoice notification email", e);
        }
    }

    /**
     * Get permit data by application ID for license invoice preview
     */
    @GetMapping("/permit-data/{applicationId}")
    public ResponseEntity<?> getPermitDataByApplicationId(@PathVariable String applicationId) {
        try {
            log.info("Getting permit data for application: {}", applicationId);

            Optional<CoreLicensePermit> permitOpt =
                    coreLicensePermitService.getPermitByApplicationId(applicationId);

            if (permitOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CoreLicensePermit permit = permitOpt.get();

            // Check payment and verification status
            if (!permit.isPaymentVerified()) {
                // Check if payment exists but needs verification
                boolean paymentExists = hasLicenseFeePayment(applicationId);

                Map<String, Object> paymentInfo = new HashMap<>();
                paymentInfo.put("permitAvailable", true);

                if (paymentExists) {
                    // Payment made but not verified by accountant
                    paymentInfo.put("paymentRequired", false);
                    paymentInfo.put("verificationPending", true);
                    paymentInfo.put("message", "Payment received. Awaiting accountant verification for license access.");
                } else {
                    // No payment made yet
                    paymentInfo.put("paymentRequired", true);
                    paymentInfo.put("verificationPending", false);
                    paymentInfo.put("message", "Payment is required to view the license permit");
                }

                paymentInfo.put("applicationId", applicationId);
                return ResponseEntity.ok(paymentInfo);
            }
            CoreLicenseApplication application = permit.getCoreLicenseApplication();

            // Create response data suitable for license component
            Map<String, Object> permitData = new HashMap<>();
            permitData.put("permitNumber", permit.getPermitNumber());
            permitData.put("permitStatus", permit.getPermitStatus());
            permitData.put("issueDate", permit.getIssueDate());
            permitData.put("expiryDate", permit.getExpiryDate());
            permitData.put("invoiceAmount", permit.getInvoiceAmount());
            permitData.put("qrCodeData", permit.getQrCodeData());
            permitData.put("conditionsText", permit.getConditionsText());
            permitData.put("directorName", permit.getDirectorName());
            permitData.put("contactPhone", permit.getContactPhone());
            permitData.put("contactEmail", permit.getContactEmail());

            // Application details
            if (application != null) {
                permitData.put("applicationId", application.getId());
                if (application.getSysUserAccount() != null) {
                    String firstName = application.getSysUserAccount().getFirstName() != null ?
                            application.getSysUserAccount().getFirstName() : "";
                    String lastName = application.getSysUserAccount().getLastName() != null ?
                            application.getSysUserAccount().getLastName() : "";
                    String applicantName = (firstName + " " + lastName).trim();
                    if (applicantName.isEmpty()) {
                        applicantName = application.getSysUserAccount().getUsername();
                    }
                    permitData.put("applicantName", applicantName);
                    permitData.put("applicantEmail", application.getSysUserAccount().getEmailAddress());
                }

                if (application.getCoreLicenseType() != null) {
                    permitData.put("licenseType", application.getCoreLicenseType().getName());
                }

                // Additional application details that might be needed
                permitData.put("sourceOwnerFullname", application.getSourceOwnerFullname());
                permitData.put("destOwnerFullname", application.getDestOwnerFullname());
                permitData.put("destPlotNumber", application.getDestPlotNumber());
            }

            log.info("Successfully retrieved permit data for application: {}", applicationId);
            return ResponseEntity.ok(permitData);

        } catch (Exception e) {
            log.error("Error getting permit data for application {}: {}", applicationId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/license-data/{licenseId}")
    public ResponseEntity<?> getLicenseDataByLicenseId(@PathVariable String licenseId) {
        try {
            log.info("Getting license data for license: {}", licenseId);

            // Use optimized service method with lightweight DTO
            LicenseDataDto licenseData = coreLicenseService.getLicenseDataById(licenseId);
            
            // Get application data for ease permit fields
            Map<String, Object> applicationByLicenseId = applicationService.getApplicationByLicenseId(licenseId);
            if (applicationByLicenseId != null && applicationByLicenseId.get("application") != null) {
                CoreLicenseApplication application = (CoreLicenseApplication) applicationByLicenseId.get("application");
                
                // Create enhanced response with ease permit fields
                Map<String, Object> enhancedData = new HashMap<>();
                enhancedData.put("permitNumber", licenseData.getPermitNumber());
                enhancedData.put("applicantName", licenseData.getApplicantName());
                enhancedData.put("sourceOwnerFullname", licenseData.getSourceOwnerFullname());
                enhancedData.put("issueDate", licenseData.getIssueDate());
                enhancedData.put("expiryDate", licenseData.getExpiryDate());
                enhancedData.put("directorName", licenseData.getDirectorName());
                enhancedData.put("contactPhone", licenseData.getContactPhone());
                enhancedData.put("contactEmail", licenseData.getContactEmail());
                enhancedData.put("qrCodeData", licenseData.getQrCodeData());
                enhancedData.put("conditionsText", licenseData.getConditionsText());
                enhancedData.put("licenseAvailable", licenseData.isLicenseAvailable());
                enhancedData.put("paymentRequired", licenseData.isPaymentRequired());
                enhancedData.put("verificationPending", licenseData.isVerificationPending());
                enhancedData.put("message", licenseData.getMessage());
                enhancedData.put("paymentStatus", licenseData.getPaymentStatus());
                enhancedData.put("amountPaid", licenseData.getAmountPaid());
                
                // Add ease permit specific fields
                enhancedData.put("benefittedLandDescription", application.getBenefittedLandDescription());
                enhancedData.put("permitConditions", application.getPermitConditions());
                enhancedData.put("burdenedLandDescription", application.getBurdenedLandDescription());
                enhancedData.put("natureOfBurden", application.getNatureOfBurden());
                
                return ResponseEntity.ok(enhancedData);
            }
            
            log.info("Successfully retrieved license data for license: {}", licenseId);
            return ResponseEntity.ok(licenseData);

        } catch (Exception e) {
            log.error("Error getting license data for license {}: {}", licenseId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get invoice data by application ID and type for invoice preview
     */
    @GetMapping("/invoice-data/{applicationId}")
    public ResponseEntity<?> getInvoiceDataByApplicationId(
            @PathVariable String applicationId,
            @RequestParam(value = "type", defaultValue = "APPLICATION_FEE") String invoiceType) {
        try {
            log.info("Getting invoice data for application: {} of type: {}", applicationId, invoiceType);

            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            // Try to get existing invoice first
            Optional<CoreInvoice> invoiceOpt =
                    coreInvoiceService.getInvoiceByApplicationIdAndType(applicationId, invoiceType);

            CoreInvoice invoice;
            if (invoiceOpt.isPresent()) {
                invoice = invoiceOpt.get();
            } else {
                // Create application fee invoice if it doesn't exist and type is APPLICATION_FEE
                if ("APPLICATION_FEE".equals(invoiceType)) {
                    invoice = createApplicationInvoice(application);
                } else {
                    return ResponseEntity.notFound().build();
                }
            }

            // Create response data suitable for invoice component
            Map<String, Object> invoiceData = new HashMap<>();
            invoiceData.put("invoiceNumber", invoice.getInvoiceNumber());
            invoiceData.put("invoiceType", invoice.getInvoiceType());
            invoiceData.put("invoiceStatus", invoice.getInvoiceStatus());
            invoiceData.put("amount", invoice.getAmount());
            invoiceData.put("currency", invoice.getCurrency());
            invoiceData.put("issueDate", invoice.getIssueDate());
            invoiceData.put("dueDate", invoice.getDueDate());
            invoiceData.put("description", invoice.getDescription());
            invoiceData.put("bankAccount", invoice.getBankAccount());
            invoiceData.put("bankName", invoice.getBankName());
            invoiceData.put("branchCode", invoice.getBranchCode());
            invoiceData.put("swiftCode", invoice.getSwiftCode());
            invoiceData.put("paymentInstructions", invoice.getPaymentInstructions());

            // Application details
            invoiceData.put("applicationId", application.getId());
            if (application.getSysUserAccount() != null) {
                String firstName = application.getSysUserAccount().getFirstName() != null ?
                        application.getSysUserAccount().getFirstName() : "";
                String lastName = application.getSysUserAccount().getLastName() != null ?
                        application.getSysUserAccount().getLastName() : "";
                String applicantName = (firstName + " " + lastName).trim();
                if (applicantName.isEmpty()) {
                    applicantName = application.getSysUserAccount().getUsername();
                }
                invoiceData.put("applicantName", applicantName);
                invoiceData.put("applicantEmail", application.getSysUserAccount().getEmailAddress());
            }

            if (application.getCoreLicenseType() != null) {
                invoiceData.put("licenseType", application.getCoreLicenseType().getName());
            }

            invoiceData.put("applicationDate", application.getDateCreated());

            log.info("Successfully retrieved invoice data for application: {}", applicationId);
            return ResponseEntity.ok(invoiceData);

        } catch (Exception e) {
            log.error("Error getting invoice data for application {}: {}", applicationId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create application fee invoice if it doesn't exist
     */
    private CoreInvoice createApplicationInvoice(CoreLicenseApplication application) {
        try {
            double amount = application.getCoreLicenseType().getApplicationFees();

            CoreInvoice invoice = new CoreInvoice();
            invoice.setId(UUID.randomUUID().toString());
            invoice.setCoreLicenseApplication(application);
            invoice.setInvoiceType("APPLICATION_FEE");
            invoice.setInvoiceStatus("PENDING");
            invoice.setAmount(amount);
            invoice.setIssueDate(new Timestamp(System.currentTimeMillis()));
            invoice.setDescription("Application fee for " + (application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "Water Permit"));
            invoice.setDateCreated(new Timestamp(System.currentTimeMillis()));
            invoice.setDateUpdated(new Timestamp(System.currentTimeMillis()));

            // Generate invoice number and set defaults
            invoice.generateInvoiceNumber();
            invoice.calculateDueDate();
            invoice.setDefaultPaymentInstructions();

            // Save invoice once with all data
            return coreInvoiceService.addCoreInvoice(invoice);
        } catch (Exception e) {
            log.error("Error creating application invoice: {}", e.getMessage());
            throw new RuntimeException("Failed to create application invoice", e);
        }
    }

    /**
     * Check if license fee payment exists for the application
     */
    private boolean hasLicenseFeePayment(String applicationId) {
        try {
            // Check CoreInvoice for LICENSE_FEE type with payment reference
            Optional<CoreInvoice> invoiceOpt =
                    coreInvoiceService.getInvoiceByApplicationIdAndType(applicationId, "LICENSE_FEE");

            if (invoiceOpt.isPresent()) {
                CoreInvoice invoice = invoiceOpt.get();
                // Check if invoice has associated payment
                return !Objects.equals(invoice.getInvoiceStatus(), "PENDING") && invoice.getCoreApplicationPayment() != null;
            }

            // Also check CoreApplicationPayment table directly for license fees
            // This covers cases where payment exists but might not be linked to invoice yet
            return paymentService.hasLicenseFeePayment(applicationId);

        } catch (Exception e) {
            log.error("Error checking license fee payment for application {}: {}", applicationId, e.getMessage());
            return false;
        }
    }

    /**
     * Accountant endpoint to verify manual payment receipts
     */
    @PostMapping("/verify-payment/{paymentId}")
    public ResponseEntity<?> verifyPayment(
            @PathVariable String paymentId,
            @RequestParam(value = "verified", defaultValue = "true") boolean verified,
            @RequestParam(value = "notes", required = false) String verificationNotes) {
        try {
            log.info("Verifying payment: {} with status: {}", paymentId, verified);

            // Get the payment record
            CoreApplicationPayment payment = paymentService.getCoreApplicationPaymentById(paymentId);
            if (payment == null) {
                return ResponseEntity.notFound().build();
            }

            // Update payment verification status
            payment.setPaymentStatus(verified ? "VERIFIED" : "REJECTED");
            payment.setVerificationNotes(verificationNotes);
            payment.setVerificationDate(new Timestamp(System.currentTimeMillis()));
            payment.setNeedsVerification(false);

            // TODO: Set verified by user ID from authentication context
            // payment.setVerifiedByUserId(getCurrentUserId(request));

            // Save the updated payment
            paymentService.editCoreApplicationPayment(payment);

            // If verified, update the license permit payment verification status
            if (verified) {
                CoreLicenseApplication application = payment.getCoreLicenseApplication();
                if (application != null) {
                    Optional<CoreLicensePermit> permitOpt =
                            coreLicensePermitService.getPermitByApplicationId(application.getId());

                    if (permitOpt.isPresent()) {
                        CoreLicensePermit permit = permitOpt.get();
                        permit.setPaymentVerified(true);
                        coreLicensePermitService.editCoreLicensePermit(permit);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("paymentId", paymentId);
            response.put("status", payment.getPaymentStatus());
            response.put("message", verified ? "Payment verified successfully" : "Payment rejected");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying payment {}: {}", paymentId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to verify payment: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Map application step names to officer roles/groups
     * This method defines which officer groups should be notified for each workflow step
     */
    private String mapStepToOfficerRole(String stepName) {
        log.info("🗺️ Mapping step '{}' to officer role...", stepName);
        if (stepName == null) {
            log.warn("Step name is null, returning null role");
            return null;
        }

        String stepLower = stepName.toLowerCase();
        log.info("startign mapping Step name: '{}', Lowercase: '{}'", stepName, stepLower);

        // Map common workflow steps to officer roles
        // IMPORTANT: Check more specific patterns BEFORE general patterns
        String role;
        if (stepLower.contains("payment") || stepLower.contains("accountant") || stepLower.contains("receipt") || stepLower.contains("financial")) {
            role = "accountant";
        } else if (stepLower.contains("senior") && (stepLower.contains("license") || stepLower.contains("licensing"))) {
            // Check for senior license officer BEFORE regular license officer
            role = "senior_licensing_officer";
        } else if (stepLower.contains("ceo") || stepLower.contains("technical committee") || stepLower.contains("final")) {
            // Check for CEO/Technical Committee BEFORE general "technical" check
            role = "ceo";
        } else if (stepLower.contains("license officer") || stepLower.contains("technical")) {
            role = "licensing_officer";
        } else if (stepLower.contains("manager") || stepLower.contains("approval") || stepLower.contains("decision") || stepLower.contains("consultation") || stepLower.contains("stakeholder")) {
            role = "licensing_manager";
        } else if (stepLower.contains("assessment") || stepLower.contains("evaluation")) {
            role = "technical_officer";
        } else if (stepLower.contains("drs") || stepLower.contains("management review") || stepLower.contains("authorization")) {
            role = "drs";
        } else {
            // Default to licensing officer for unknown steps
            log.info("No specific role mapping found for step '{}', defaulting to licensing_officer", stepName);
            role = "licensing_officer";
        }

        log.info("✅ Step '{}' mapped to role: '{}'", stepName, role);
        return role;
    }

    /**
     * View/Download payment receipt by payment ID
     */
    @GetMapping("/view-payment-receipt/{paymentId}")
    public ResponseEntity<Resource> viewPaymentRececfipt(@PathVariable String paymentId) {
        try {
            log.info("=== ACCOUNTANT CLICKED VERIFY PAYMENT ===");
            log.info("Payment ID: {}", paymentId);

            // Get payment record from core_application_payment
            CoreApplicationPayment payment = paymentService.getCoreApplicationPaymentById(paymentId);
            if (payment == null) {
                log.error("Payment not found with ID: {}", paymentId);
                return ResponseEntity.notFound().build();
            }

            log.info("Payment found: {}, Receipt Document ID: {}", paymentId, payment.getReceiptDocumentId());

            // Get receipt_document_id from payment
            String receiptDocumentId = payment.getReceiptDocumentId();
            if (receiptDocumentId == null || receiptDocumentId.isEmpty()) {
                log.error("No receipt document ID found for payment: {}", paymentId);
                return ResponseEntity.notFound().build();
            }

            // Get document record using receipt_document_id
            CoreApplicationDocument document = documentService.getCoreApplicationDocumentById(receiptDocumentId);
            if (document == null) {
                log.error("Document not found with ID: {}", receiptDocumentId);
                return ResponseEntity.notFound().build();
            }

            log.info("Document found: {}", document.getDocumentUrl());

            // Get file path from document_url and fetch from uploads
            Path filePath;
            String documentUrl = document.getDocumentUrl();

            if (documentUrl.startsWith("/") || documentUrl.contains(":")) {
                filePath = Paths.get(documentUrl);
            } else {
                filePath = Paths.get(System.getProperty("user.dir"), documentUrl);
            }

            log.info("Looking for receipt file at: {}", filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                log.error("Receipt file not found at path: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.error("Receipt resource not readable: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("Serving receipt file: {} with content type: {}", filePath.getFileName(), contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error viewing payment receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // /**
    //  * Map step names to user group names for notifications
    //  */
    // private String mapStepToOfficerRole(String stepName) {
    //     return switch (stepName.toLowerCase()) {
    //         case "licensing officer", "initial review", "license officer review" -> "licensing_officer";
    //         case "senior licensing officer", "senior license officer review", "senior officer review" -> "senior_licensing_officer";
    //         case "licensing manager", "manager review", "license manager review" -> "licensing_manager";
    //         case "drs", "drs review", "director review" -> "drs";
    //         case "ceo", "ceo review", "final approval" -> "ceo";
    //         case "accountant", "payment verification" -> "accountant";
    //         default -> null;
    //     };
    // }

    // private String mapStepToOfficerRole(String stepName) {
    //     return switch (stepName.toLowerCase()) {
    //         case "licensing officer", "initial review", "license officer review" -> "licensing_officer";
    //         case "senior licensing officer", "senior license officer review", "senior officer review" -> "senior_licensing_officer";
    //         case "licensing manager", "manager review", "license manager review" -> "licensing_manager";
    //         case "drs", "drs review", "director review" -> "drs";
    //         case "ceo", "ceo review", "final approval" -> "ceo";
    //         case "accountant", "payment verification" -> "accountant";
    //         default -> null;
    //     };
    // }

    private String getApplicantFullName(SysUserAccount userAccount) {
        if (userAccount == null) return "Unknown";
        String firstName = userAccount.getFirstName() != null ? userAccount.getFirstName() : "";
        String lastName = userAccount.getLastName() != null ? userAccount.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? userAccount.getUsername() : fullName;
    }
}
