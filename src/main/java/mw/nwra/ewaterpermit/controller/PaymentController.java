package mw.nwra.ewaterpermit.controller;

import java.util.Map;

import mw.nwra.ewaterpermit.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.requestSchema.PaymentRequest;
import mw.nwra.ewaterpermit.responseSchema.PaymentResponse;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.service.MailingService;
import mw.nwra.ewaterpermit.service.PaymentService;
import mw.nwra.ewaterpermit.service.BomaPayService;
import mw.nwra.ewaterpermit.service.BomaPayTransactionHistoryService;
import mw.nwra.ewaterpermit.service.CoreApplicationStepService;
import mw.nwra.ewaterpermit.service.CoreFeesTypeService;
import mw.nwra.ewaterpermit.service.OfficerNotificationService;
import mw.nwra.ewaterpermit.service.CoreApplicationPaymentService;
import mw.nwra.ewaterpermit.service.EmailQueueService;
import mw.nwra.ewaterpermit.service.NotificationService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

import java.sql.Timestamp;
import java.util.HashMap;

@RestController
@RequestMapping(value = "/v1/payments")

public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CoreLicenseApplicationService licenseApplicationService;

    @Autowired
    private MailingService mailingService;
    
    @Autowired
    private BomaPayService bomaPayService;
    
    @Autowired
    private BomaPayTransactionHistoryService transactionHistoryService;

    @Autowired
    private CoreApplicationStepService applicationStepService;

    @Autowired
    private CoreFeesTypeService coreFeesTypeService;

    @Autowired
    private OfficerNotificationService officerNotificationService;

    @Autowired
    private CoreApplicationPaymentService applicationPaymentService;

    @Autowired
    private EmailQueueService emailQueueService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private Auditor auditor;

    @PostMapping(path = "/boma-pay/initiate")
    public ResponseEntity<Map<String, Object>> initiateBomaPayPayment(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        String applicationId = (String) request.get("applicationId");
        String paymentType = (String) request.get("paymentType"); // APPLICATION_FEE or LICENSE_FEE
        Double amount = ((Number) request.get("amount")).doubleValue();

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        if (!application.getSysUserAccount().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        // Validate payment type against current application step
        String validationError = validatePaymentInitiation(application, paymentType);
        if (validationError != null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", validationError);
            errorResponse.put("paymentDeclined", true);
            return ResponseEntity.ok(errorResponse);
        }

        Map<String, Object> response = bomaPayService.initiatePayment(application, paymentType, amount, user);
        auditor.audit(Action.CREATE, "Payment", applicationId, user, "Initiated " + paymentType + " payment");
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/boma-pay/{orderId}/status")
    public ResponseEntity<Map<String, Object>> getBomaPayStatus(
            @PathVariable String orderId,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        Map<String, Object> response = bomaPayService.getPaymentStatus(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/boma-pay/{orderId}/return-visited")
    public ResponseEntity<Map<String, Object>> recordReturnUrlVisited(
            @PathVariable String orderId,
            @RequestHeader(name = "Authorization") String token) {

        System.out.println("🔍 Return URL visited for orderId: " + orderId);
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        try {
            // Check if return URL already visited to prevent duplicate processing
            var existingTransaction = transactionHistoryService.findByOrderId(orderId);
            if (!existingTransaction.isPresent()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Transaction not found"));
            }
            
            boolean alreadyVisited = existingTransaction.get().isReturnUrlVisited();
            boolean alreadyCompleted = "COMPLETED".equals(existingTransaction.get().getPaymentStatus());
            
            if (!alreadyVisited) {
                // Record the return URL visit only if not already recorded
                transactionHistoryService.recordReturnUrlVisited(orderId);
                System.out.println("🔍 Return URL visit recorded successfully for orderId: " + orderId);
            }
            
            // Get current payment status from BomaPay
            Map<String, Object> statusResponse = bomaPayService.getPaymentStatus(orderId);
            boolean paymentSuccessful = false;
            
            if (statusResponse.containsKey("success") && (Boolean) statusResponse.get("success")) {
                String paymentStatus = (String) statusResponse.get("status");
                if ("COMPLETED".equals(paymentStatus)) {
                    if (!alreadyCompleted) {
                        // Payment is completed and not yet processed in our system
                        String transactionReference = (String) statusResponse.get("orderId");
                        transactionHistoryService.markPaymentCompleted(orderId, transactionReference, statusResponse);
                        
                        // Process the completed payment based on payment type
                        boolean processed = processBomaPayCompletion(orderId, statusResponse);
                        if (!processed) {
                            // Payment completed but couldn't be processed due to step validation
                            return ResponseEntity.ok(Map.of(
                                "success", true, 
                                "message", "Payment completed but requires manual review",
                                "paymentCompleted", true,
                                "paymentStatus", statusResponse,
                                "requiresReview", true
                            ));
                        }
                        
                        paymentSuccessful = true;
                        System.out.println("✅ Payment completed and processed successfully for orderId: " + orderId);
                    } else {
                        paymentSuccessful = true;
                        System.out.println("✅ Payment already completed for orderId: " + orderId);
                    }
                } else {
                    // Payment failed or is in another status
                    if ("FAILED".equals(paymentStatus) || "CANCELLED".equals(paymentStatus) || "EXPIRED".equals(paymentStatus)) {
                        // Send failure notifications
                        try {
                            var transaction = existingTransaction.get();
                            CoreLicenseApplication application = transaction.getCoreLicenseApplication();
                            if (application != null) {
                                sendPaymentFailureEmail(application, transaction.getPaymentType(), orderId, "Payment " + paymentStatus.toLowerCase());
                                createPaymentFailureNotification(application, transaction.getPaymentType(), orderId);
                                System.out.println("📧 Payment failure notifications sent for orderId: " + orderId + " (Status: " + paymentStatus + ")");
                            }
                        } catch (Exception notifError) {
                            System.err.println("Failed to send payment failure notifications: " + notifError.getMessage());
                        }
                    }
                }
            } else {
                // BomaPay API call failed - consider this a payment issue
                try {
                    var transaction = existingTransaction.get();
                    CoreLicenseApplication application = transaction.getCoreLicenseApplication();
                    if (application != null && !alreadyCompleted) {
                        sendPaymentFailureEmail(application, transaction.getPaymentType(), orderId, "Unable to verify payment status");
                        createPaymentFailureNotification(application, transaction.getPaymentType(), orderId);
                        System.out.println("📧 Payment verification failure notifications sent for orderId: " + orderId);
                    }
                } catch (Exception notifError) {
                    System.err.println("Failed to send payment verification failure notifications: " + notifError.getMessage());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Return URL visit processed",
                "paymentCompleted", paymentSuccessful,
                "paymentStatus", statusResponse,
                "alreadyProcessed", alreadyCompleted
            ));
        } catch (Exception e) {
            System.err.println("🔍 Failed to process return URL for orderId: " + orderId + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("success", false, "message", "Failed to process return URL: " + e.getMessage()));
        }
    }

    @GetMapping(path = "/boma-pay/history/{applicationId}")
    public ResponseEntity<Map<String, Object>> getPaymentHistory(
            @PathVariable String applicationId,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Check access to application
        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        if (!application.getSysUserAccount().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        try {
            var paymentHistory = transactionHistoryService.getPaymentHistoryByApplication(applicationId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentHistory", paymentHistory,
                "totalTransactions", paymentHistory.size(),
                "successfulPayments", transactionHistoryService.countSuccessfulPaymentsByApplication(applicationId)
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false, 
                "message", "Failed to retrieve payment history: " + e.getMessage()
            ));
        }
    }

    // Card processing endpoint removed - using hosted payment approach only

    @PostMapping(path = "/boma-pay/webhook")
    public ResponseEntity<Map<String, Object>> handleBomaPayWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            bomaPayService.handleWebhook(webhookData);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping(path = "/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestBody PaymentRequest request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(request.getApplicationId());
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        if (!application.getSysUserAccount().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        request.setCustomerName(user.getFirstName() + " " + user.getLastName());
        request.setCustomerEmail(user.getEmailAddress());
        request.setCustomerPhoneNumber(user.getPhoneNumber());

        PaymentResponse response = paymentService.initiatePayment(request);
        auditor.audit(Action.CREATE, "Payment", response.getPaymentReference(), user, "Initiated payment for application " + request.getApplicationId());

        try {
            mailingService.send("PAYMENT_INITIATED", response.getPaymentReference(), user);
        } catch (Exception e) {
            System.err.println("Failed to send payment email: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{paymentReference}/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable String paymentReference,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        PaymentResponse response = paymentService.verifyPayment(paymentReference);

        if ("COMPLETED".equals(response.getStatus())) {
            try {
                mailingService.send("PAYMENT_COMPLETED", paymentReference, user);
            } catch (Exception e) {
                System.err.println("Failed to send payment confirmation email: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{paymentReference}/status")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable String paymentReference,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        PaymentResponse response = paymentService.getPaymentStatus(paymentReference);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/generate-invoice")
    public ResponseEntity<PaymentResponse> generateInvoice(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        String applicationId = (String) request.get("applicationId");
        String feeType = (String) request.get("feeType");

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        if (!application.getSysUserAccount().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        PaymentResponse response = paymentService.generateInvoice(application, feeType);

        try {
            mailingService.send("INVOICE_GENERATED", response.getPaymentReference(), user);
        } catch (Exception e) {
            System.err.println("Failed to send invoice email: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/process-application-payment")
    public ResponseEntity<CoreApplicationPayment> processApplicationPayment(
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "Authorization") String token) {

        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("User not authenticated");
        }

        String applicationId = (String) request.get("applicationId");
        String paymentType = (String) request.get("paymentType");

        CoreLicenseApplication application = licenseApplicationService.getCoreLicenseApplicationById(applicationId);
        if (application == null) {
            throw new EntityNotFoundException("Application not found");
        }

        if (!application.getSysUserAccount().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this application");
        }

        CoreApplicationPayment payment = paymentService.processApplicationPayment(application, paymentType);
        auditor.audit(Action.UPDATE, "Payment", payment.getId(), user, "Processed " + paymentType + " payment");
        return ResponseEntity.ok(payment);
    }

    /**
     * Send payment success email notification with professional HTML template
     */
    private void sendPaymentSuccessEmail(CoreLicenseApplication application, String paymentType, double amount, String orderId) {
        try {
            SysUserAccount applicant = application.getSysUserAccount();
            if (applicant == null || applicant.getEmailAddress() == null) {
                System.err.println("Cannot send payment success email - no applicant email found");
                return;
            }

            String applicantName = (applicant.getFirstName() != null ? applicant.getFirstName() : "") + 
                                 (applicant.getLastName() != null ? " " + applicant.getLastName() : "").trim();
            if (applicantName.isEmpty()) {
                applicantName = "Applicant";
            }

            String paymentTypeDisplay = "APPLICATION_FEE".equals(paymentType) ? "Application Fee" : "License Fee";
            String subject = "Payment Confirmation - " + paymentTypeDisplay + " Successfully Processed";

            String nextStepsMessage = "APPLICATION_FEE".equals(paymentType) ? 
                "Your application is now being processed by our licensing team. You will receive updates as your application progresses through the review process." :
                "Your license fee payment has been processed. Your license and permit documents are being prepared and will be available shortly.";

            String emailBody = createPaymentSuccessEmailTemplate(
                applicantName,
                application.getId(),
                paymentTypeDisplay,
                amount,
                orderId,
                new java.text.SimpleDateFormat("dd MMM yyyy 'at' HH:mm").format(new java.util.Date()),
                nextStepsMessage
            );

            String taskId = "payment-success-" + paymentType.toLowerCase() + "-" + application.getId() + "-" + System.currentTimeMillis();
            emailQueueService.sendEmailAsync(taskId, applicant.getEmailAddress(), subject, emailBody);
            System.out.println("✅ Payment success email queued for: " + applicant.getEmailAddress());
            
            // Save payment success notification
            try {
                UserNotification notification = new UserNotification();
                notification.setUserId(applicant.getId());
                notification.setTitle("Payment Successful - " + paymentTypeDisplay);
                notification.setMessage(String.format("Your %s payment of MWK %.2f has been successfully processed for application %s.", 
                        paymentTypeDisplay.toLowerCase(), amount, application.getId()));
                notification.setType(UserNotification.NotificationType.SUCCESS);
                notification.setCategory(UserNotification.NotificationCategory.PAYMENT);
                notification.setPriority(UserNotification.NotificationPriority.HIGH);
                notification.setActionUrl("/applications/" + application.getId());
                notification.setActionLabel("View Application");
                notification.setApplicationId(application.getId());
                
                notificationService.createNotification(notification);
                System.out.println("✅ Payment success notification saved for user: " + applicant.getId());
            } catch (Exception notifEx) {
                System.err.println("Failed to save payment success notification: " + notifEx.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Failed to send payment success email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send payment failure email notification with professional HTML template
     */
    private void sendPaymentFailureEmail(CoreLicenseApplication application, String paymentType, String orderId, String failureReason) {
        try {
            SysUserAccount applicant = application.getSysUserAccount();
            if (applicant == null || applicant.getEmailAddress() == null) {
                System.err.println("Cannot send payment failure email - no applicant email found");
                return;
            }

            String applicantName = (applicant.getFirstName() != null ? applicant.getFirstName() : "") + 
                                 (applicant.getLastName() != null ? " " + applicant.getLastName() : "").trim();
            if (applicantName.isEmpty()) {
                applicantName = "Applicant";
            }

            String paymentTypeDisplay = "APPLICATION_FEE".equals(paymentType) ? "Application Fee" : "License Fee";
            String subject = "Payment Processing Issue - " + paymentTypeDisplay;

            String emailBody = createPaymentFailureEmailTemplate(
                applicantName,
                application.getId(),
                paymentTypeDisplay,
                orderId,
                new java.text.SimpleDateFormat("dd MMM yyyy 'at' HH:mm").format(new java.util.Date()),
                failureReason
            );

            String taskId = "payment-failure-" + paymentType.toLowerCase() + "-" + application.getId() + "-" + System.currentTimeMillis();
            emailQueueService.sendEmailAsync(taskId, applicant.getEmailAddress(), subject, emailBody);
            System.out.println("📧 Payment failure email queued for: " + applicant.getEmailAddress());
            
            // Save payment failure notification
            try {
                UserNotification notification = new UserNotification();
                notification.setUserId(applicant.getId());
                notification.setTitle("Payment Issue - " + paymentTypeDisplay);
                notification.setMessage(String.format("There was an issue processing your %s payment (Ref: %s). Please try again or contact support.", 
                        paymentTypeDisplay.toLowerCase(), orderId));
                notification.setType(UserNotification.NotificationType.WARNING);
                notification.setCategory(UserNotification.NotificationCategory.PAYMENT);
                notification.setPriority(UserNotification.NotificationPriority.HIGH);
                notification.setActionUrl("/applications/" + application.getId() + "/payment");
                notification.setActionLabel("Retry Payment");
                notification.setApplicationId(application.getId());
                
                notificationService.createNotification(notification);
                System.out.println("⚠️ Payment failure notification saved for user: " + applicant.getId());
            } catch (Exception notifEx) {
                System.err.println("Failed to save payment failure notification: " + notifEx.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Failed to send payment failure email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create payment success UserNotification
     */
    private void createPaymentSuccessNotification(CoreLicenseApplication application, String paymentType, double amount) {
        try {
            SysUserAccount applicant = application.getSysUserAccount();
            if (applicant == null) {
                System.err.println("Cannot create payment success notification - no applicant found");
                return;
            }

            String paymentTypeDisplay = "APPLICATION_FEE".equals(paymentType) ? "Application Fee" : "License Fee";
            
            UserNotification notification = new UserNotification();
            notification.setUserId(applicant.getId());
            notification.setTitle("Payment Successful - " + paymentTypeDisplay);
            notification.setMessage(String.format("Your %s payment of MWK %.2f has been successfully processed for application %s.", 
                    paymentTypeDisplay.toLowerCase(), amount, application.getId()));
            notification.setType(UserNotification.NotificationType.SUCCESS);
            notification.setCategory(UserNotification.NotificationCategory.PAYMENT);
            notification.setPriority(UserNotification.NotificationPriority.HIGH);
            notification.setActionUrl("/applications/" + application.getId());
            notification.setActionLabel("View Application");
            
            notificationService.createNotification(notification);
            System.out.println("✅ Payment success notification created for user: " + applicant.getId());
            
        } catch (Exception e) {
            System.err.println("Failed to create payment success notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create payment failure UserNotification
     */
    private void createPaymentFailureNotification(CoreLicenseApplication application, String paymentType, String orderId) {
        try {
            SysUserAccount applicant = application.getSysUserAccount();
            if (applicant == null) {
                System.err.println("Cannot create payment failure notification - no applicant found");
                return;
            }

            String paymentTypeDisplay = "APPLICATION_FEE".equals(paymentType) ? "Application Fee" : "License Fee";
            
            UserNotification notification = new UserNotification();
            notification.setUserId(applicant.getId());
            notification.setTitle("Payment Issue - " + paymentTypeDisplay);
            notification.setMessage(String.format("There was an issue processing your %s payment (Ref: %s). Please try again or contact support.", 
                    paymentTypeDisplay.toLowerCase(), orderId));
            notification.setType(UserNotification.NotificationType.WARNING);
            notification.setCategory(UserNotification.NotificationCategory.PAYMENT);
            notification.setPriority(UserNotification.NotificationPriority.HIGH);
            notification.setActionUrl("/applications/" + application.getId() + "/payment");
            notification.setActionLabel("Retry Payment");
            
            notificationService.createNotification(notification);
            System.out.println("⚠️ Payment failure notification created for user: " + applicant.getId());
            
        } catch (Exception e) {
            System.err.println("Failed to create payment failure notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Validate payment initiation against current application step
     */
    private String validatePaymentInitiation(CoreLicenseApplication application, String paymentType) {
        try {
            int currentSequence = application.getCoreApplicationStep() != null ? 
                application.getCoreApplicationStep().getSequenceNumber() : -1;
            
            System.out.println("🔍 Validating payment initiation - Current sequence: " + currentSequence + ", Payment type: " + paymentType);
            
            if ("APPLICATION_FEE".equals(paymentType)) {
                if (currentSequence != 0) {
                    return "Payment cannot be processed at this time. Please ensure your application is in the correct stage for payment.";
                }
            } else if ("LICENSE_FEE".equals(paymentType)) {
                if (currentSequence != 5) {
                    return "License fee payment is not available at this stage. Please wait until your application has been approved.";
                }
            } else {
                return "Invalid payment type. Please contact support for assistance.";
            }
            
            return null; // Validation passed
        } catch (Exception e) {
            System.err.println("Error validating payment initiation: " + e.getMessage());
            return "Unable to process payment at this time. Please try again later.";
        }
    }

    /**
     * Process completed BomaPay payment based on payment type
     */
    private boolean processBomaPayCompletion(String orderId, Map<String, Object> paymentDetails) {
        try {
            System.out.println("🔍 Processing BomaPay completion for orderId: " + orderId);
            
            // Get transaction details
            var transactionOpt = transactionHistoryService.findByOrderId(orderId);
            if (!transactionOpt.isPresent()) {
                System.err.println("Transaction not found for orderId: " + orderId);
                return false;
            }
            
            BomaPayTransactionHistory transaction = transactionOpt.get();
            CoreLicenseApplication application = transaction.getCoreLicenseApplication();
            String paymentType = transaction.getPaymentType();
            
            // Validate completion against current application step
            String validationError = validatePaymentCompletion(application, paymentType);
            if (validationError != null) {
                System.err.println("Payment completion validation failed: " + validationError);
                return false; // Payment completed but cannot be processed due to step validation
            }
            
            System.out.println("🔍 Payment type: " + paymentType + ", Amount: " + transaction.getAmount());
            
            if ("APPLICATION_FEE".equals(paymentType)) {
                return processApplicationFeeCompletion(application, transaction, paymentDetails);
            } else if ("LICENSE_FEE".equals(paymentType)) {
                return processLicenseFeeCompletion(application, transaction, paymentDetails);
            } else {
                System.err.println("Unknown payment type: " + paymentType);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Failed to process BomaPay completion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Validate payment completion against current application step
     */
    private String validatePaymentCompletion(CoreLicenseApplication application, String paymentType) {
        try {
            int currentSequence = application.getCoreApplicationStep() != null ? 
                application.getCoreApplicationStep().getSequenceNumber() : -1;
            
            System.out.println("🔍 Validating payment completion - Current sequence: " + currentSequence + ", Payment type: " + paymentType);
            
            if ("APPLICATION_FEE".equals(paymentType)) {
                if (currentSequence != 0) {
                    return "Application is no longer at the payment stage. Manual review required.";
                }
            } else if ("LICENSE_FEE".equals(paymentType)) {
                if (currentSequence != 5) {
                    return "Application is not at the license fee stage. Manual review required.";
                }
            }
            
            return null; // Validation passed
        } catch (Exception e) {
            System.err.println("Error validating payment completion: " + e.getMessage());
            return "Unable to validate payment completion.";
        }
    }
    
    /**
     * Process APPLICATION_FEE completion - create CoreApplicationPayment and progress workflow
     */
    private boolean processApplicationFeeCompletion(CoreLicenseApplication application, 
                                                   BomaPayTransactionHistory transaction, 
                                                   Map<String, Object> paymentDetails) {
        try {
            System.out.println("🔍 Processing APPLICATION_FEE completion for application: " + application.getId());
            
            // Create CoreApplicationPayment record
            CoreApplicationPayment payment = new CoreApplicationPayment();
            payment.setCoreLicenseApplication(application);
            payment.setAmountPaid(transaction.getAmount());
            payment.setPaymentMethod("BOMA_PAY");
            payment.setPaymentStatus("PAID");
            payment.setCoreFeesType(coreFeesTypeService.getCoreFeesTypeByName("Application fee"));
            payment.setReceiptDocumentId("BOMA_PAY_" + transaction.getOrderId()); // Store transaction reference in receipt field
            payment.setNeedsVerification(false); // BomaPay payments don't need manual verification
            payment.setDateCreated(new Timestamp(System.currentTimeMillis()));
            
            CoreApplicationPayment savedPayment = applicationPaymentService.addCoreApplicationPayment(payment);
            System.out.println("✅ APPLICATION_FEE payment record created: " + savedPayment.getId());
            
            // Send payment success notifications
            sendPaymentSuccessEmail(application, transaction.getPaymentType(), transaction.getAmount(), transaction.getOrderId());
            createPaymentSuccessNotification(application, transaction.getPaymentType(), transaction.getAmount());
            
            // Progress application to next step using CoreApplicationStepService
            if (application.getCoreApplicationStep() != null && 
                application.getCoreApplicationStep().getSequenceNumber() == 0) {
                
                try {
                    CoreApplicationStep currentStep = application.getCoreApplicationStep();
                    CoreApplicationStep nextStep = applicationStepService.getNextStep(currentStep);
                    
                    if (nextStep != null) {
                        application.setCoreApplicationStep(nextStep);
                        licenseApplicationService.editCoreLicenseApplication(application);
                        System.out.println("✅ Application progressed from step " + currentStep.getSequenceNumber() + 
                                         " to step " + nextStep.getSequenceNumber() + " after APPLICATION_FEE payment");
                        
                        // Notify officers about new application
                        try {
                            String officerRole = "Licensing Officer"; // Step 1 is typically licensing officer
                            officerNotificationService.notifyOfficersAboutNewApplication(officerRole, application);
                        } catch (Exception e) {
                            System.err.println("Failed to notify officers: " + e.getMessage());
                        }
                    } else {
                        System.err.println("No next step found for current step: " + currentStep.getSequenceNumber());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to progress application step: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to process APPLICATION_FEE completion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Process LICENSE_FEE completion - create license and permit documents (NO step progression)
     */
    private boolean processLicenseFeeCompletion(CoreLicenseApplication application, 
                                              BomaPayTransactionHistory transaction, 
                                              Map<String, Object> paymentDetails) {
        try {
            System.out.println("🔍 Processing LICENSE_FEE completion for application: " + application.getId());
            
            // Create CoreApplicationPayment record for license fee
            CoreApplicationPayment payment = new CoreApplicationPayment();
            payment.setCoreLicenseApplication(application);
            payment.setAmountPaid(transaction.getAmount());
            payment.setPaymentMethod("BOMA_PAY");
            payment.setPaymentStatus("PAID");
            payment.setCoreFeesType(coreFeesTypeService.getCoreFeesTypeByName("License fees"));
            payment.setReceiptDocumentId("BOMA_PAY_" + transaction.getOrderId()); // Store transaction reference in receipt field
        payment.setNeedsVerification(false);
            payment.setDateCreated(new Timestamp(System.currentTimeMillis()));
            
            CoreApplicationPayment savedPayment = applicationPaymentService.addCoreApplicationPayment(payment);
            System.out.println("✅ LICENSE_FEE payment record created: " + savedPayment.getId());
            
            // Send payment success notifications
            sendPaymentSuccessEmail(application, transaction.getPaymentType(), transaction.getAmount(), transaction.getOrderId());
            createPaymentSuccessNotification(application, transaction.getPaymentType(), transaction.getAmount());
            
            // Generate license and permit (NO step progression for license fee)
            boolean licenseGenerated = generateLicenseAndPermit(application, savedPayment);
            
            return licenseGenerated;
            
        } catch (Exception e) {
            System.err.println("Failed to process LICENSE_FEE completion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Generate license and permit documents after license fee payment
     */
    private boolean generateLicenseAndPermit(CoreLicenseApplication application, CoreApplicationPayment payment) {
        try {
            System.out.println("🔍 Generating license and permit for application: " + application.getId());
            
            // TODO: Implement license generation logic based on your existing system
            // This should include:
            // 1. Create CoreLicense record with license number, dates, etc.
            // 2. Generate license document/PDF 
            // 3. Create permit document if needed
            // 4. Update application status to "COMPLETED" or "LICENSED"
            // 5. Send notification emails to applicant
            // 6. Link payment to the generated license
            
            System.out.println("🔍 License and permit generation placeholder - implement based on existing workflow");
            
            // For now, return true to indicate successful processing
            // Replace this with actual license generation logic
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to generate license and permit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create professional HTML email template for payment success
     */
    private String createPaymentSuccessEmailTemplate(String applicantName, String applicationId, 
                                                   String paymentType, double amount, String orderId, 
                                                   String paymentDate, String nextStepsMessage) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Payment Confirmation</title>
                <style>
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        line-height: 1.6; 
                        color: #333; 
                        margin: 0; 
                        padding: 0; 
                        background-color: #f4f4f4; 
                    }
                    .container { 
                        max-width: 600px; 
                        margin: 20px auto; 
                        background: white; 
                        border-radius: 8px; 
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1); 
                        overflow: hidden;
                    }
                    .header { 
                        background: linear-gradient(135deg, #1e3a8a 0%%, #3b82f6 100%%); 
                        color: white; 
                        padding: 30px 20px; 
                        text-align: center; 
                    }
                    .header h1 { 
                        margin: 0; 
                        font-size: 24px; 
                        font-weight: 600; 
                    }
                    .header .subtitle { 
                        margin: 5px 0 0 0; 
                        font-size: 14px; 
                        opacity: 0.9; 
                    }
                    .content { 
                        padding: 30px; 
                    }
                    .greeting { 
                        font-size: 18px; 
                        font-weight: 500; 
                        margin-bottom: 20px; 
                        color: #1e3a8a; 
                    }
                    .success-message { 
                        background: #ecfdf5; 
                        border-left: 4px solid #10b981; 
                        padding: 15px; 
                        margin: 20px 0; 
                        border-radius: 4px; 
                    }
                    .success-icon { 
                        color: #10b981; 
                        font-size: 20px; 
                        margin-right: 8px; 
                    }
                    .details-section { 
                        background: #f8fafc; 
                        border: 1px solid #e2e8f0; 
                        border-radius: 6px; 
                        padding: 20px; 
                        margin: 25px 0; 
                    }
                    .details-title { 
                        font-weight: 600; 
                        color: #1e3a8a; 
                        margin-bottom: 15px; 
                        font-size: 16px; 
                    }
                    .detail-row { 
                        display: flex; 
                        justify-content: space-between; 
                        margin-bottom: 8px; 
                        padding: 5px 0; 
                        border-bottom: 1px solid #e2e8f0; 
                    }
                    .detail-row:last-child { 
                        border-bottom: none; 
                        font-weight: 600; 
                        color: #1e3a8a; 
                    }
                    .detail-label { 
                        font-weight: 500; 
                        color: #64748b; 
                    }
                    .detail-value { 
                        font-weight: 500; 
                        text-align: right; 
                    }
                    .next-steps { 
                        background: #eff6ff; 
                        border-left: 4px solid #3b82f6; 
                        padding: 15px; 
                        margin: 20px 0; 
                        border-radius: 4px; 
                    }
                    .next-steps-title { 
                        font-weight: 600; 
                        color: #1e3a8a; 
                        margin-bottom: 10px; 
                    }
                    .footer { 
                        background: #f8fafc; 
                        padding: 25px; 
                        text-align: center; 
                        border-top: 1px solid #e2e8f0; 
                    }
                    .contact-info { 
                        margin: 15px 0; 
                        color: #64748b; 
                        font-size: 14px; 
                    }
                    .signature { 
                        margin-top: 20px; 
                        font-weight: 600; 
                        color: #1e3a8a; 
                    }
                    .government-seal { 
                        font-size: 12px; 
                        color: #64748b; 
                        font-style: italic; 
                        margin-top: 10px; 
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Payment Confirmation</h1>
                        <div class="subtitle">National Water Resources Authority</div>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Dear %s,</div>
                        
                        <div class="success-message">
                            <span class="success-icon">✓</span>
                            <strong>Payment Successfully Processed</strong><br>
                            We are pleased to confirm that your payment has been successfully processed.
                        </div>
                        
                        <div class="details-section">
                            <div class="details-title">Payment Details</div>
                            <div class="detail-row">
                                <span class="detail-label">Application ID:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Payment Type:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Transaction Reference:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Payment Method:</span>
                                <span class="detail-value">BomaPay</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Date & Time:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Amount Paid:</span>
                                <span class="detail-value">MWK %.2f</span>
                            </div>
                        </div>
                        
                        <div class="next-steps">
                            <div class="next-steps-title">What happens next?</div>
                            %s
                        </div>
                        
                        <p>If you have any questions or need assistance, please contact our support team using the information below.</p>
                        
                    </div>
                    
                    <div class="footer">
                        <div class="contact-info">
                            <strong>Contact Information:</strong><br>
                            Email: support@nwra.gov.mw | Phone: +265 1 771 336
                        </div>
                        
                        <div class="signature">
                            National Water Resources Authority<br>
                            Government of Malawi
                        </div>
                        
                        <div class="government-seal">
                            Official Government Communication
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, applicantName, applicationId, paymentType, orderId, paymentDate, amount, nextStepsMessage);
    }

    /**
     * Create professional HTML email template for payment failure
     */
    private String createPaymentFailureEmailTemplate(String applicantName, String applicationId, 
                                                   String paymentType, String orderId, 
                                                   String paymentDate, String failureReason) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Payment Processing Issue</title>
                <style>
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        line-height: 1.6; 
                        color: #333; 
                        margin: 0; 
                        padding: 0; 
                        background-color: #f4f4f4; 
                    }
                    .container { 
                        max-width: 600px; 
                        margin: 20px auto; 
                        background: white; 
                        border-radius: 8px; 
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1); 
                        overflow: hidden;
                    }
                    .header { 
                        background: linear-gradient(135deg, #dc2626 0%%, #ef4444 100%%); 
                        color: white; 
                        padding: 30px 20px; 
                        text-align: center; 
                    }
                    .header h1 { 
                        margin: 0; 
                        font-size: 24px; 
                        font-weight: 600; 
                    }
                    .header .subtitle { 
                        margin: 5px 0 0 0; 
                        font-size: 14px; 
                        opacity: 0.9; 
                    }
                    .content { 
                        padding: 30px; 
                    }
                    .greeting { 
                        font-size: 18px; 
                        font-weight: 500; 
                        margin-bottom: 20px; 
                        color: #1e3a8a; 
                    }
                    .warning-message { 
                        background: #fef2f2; 
                        border-left: 4px solid #ef4444; 
                        padding: 15px; 
                        margin: 20px 0; 
                        border-radius: 4px; 
                    }
                    .warning-icon { 
                        color: #ef4444; 
                        font-size: 20px; 
                        margin-right: 8px; 
                    }
                    .details-section { 
                        background: #f8fafc; 
                        border: 1px solid #e2e8f0; 
                        border-radius: 6px; 
                        padding: 20px; 
                        margin: 25px 0; 
                    }
                    .details-title { 
                        font-weight: 600; 
                        color: #1e3a8a; 
                        margin-bottom: 15px; 
                        font-size: 16px; 
                    }
                    .detail-row { 
                        display: flex; 
                        justify-content: space-between; 
                        margin-bottom: 8px; 
                        padding: 5px 0; 
                        border-bottom: 1px solid #e2e8f0; 
                    }
                    .detail-row:last-child { 
                        border-bottom: none; 
                    }
                    .detail-label { 
                        font-weight: 500; 
                        color: #64748b; 
                    }
                    .detail-value { 
                        font-weight: 500; 
                        text-align: right; 
                    }
                    .next-steps { 
                        background: #fffbeb; 
                        border-left: 4px solid #f59e0b; 
                        padding: 15px; 
                        margin: 20px 0; 
                        border-radius: 4px; 
                    }
                    .next-steps-title { 
                        font-weight: 600; 
                        color: #92400e; 
                        margin-bottom: 10px; 
                    }
                    .next-steps ol { 
                        margin: 0; 
                        padding-left: 20px; 
                    }
                    .next-steps li { 
                        margin-bottom: 5px; 
                    }
                    .footer { 
                        background: #f8fafc; 
                        padding: 25px; 
                        text-align: center; 
                        border-top: 1px solid #e2e8f0; 
                    }
                    .contact-info { 
                        margin: 15px 0; 
                        color: #64748b; 
                        font-size: 14px; 
                    }
                    .signature { 
                        margin-top: 20px; 
                        font-weight: 600; 
                        color: #1e3a8a; 
                    }
                    .government-seal { 
                        font-size: 12px; 
                        color: #64748b; 
                        font-style: italic; 
                        margin-top: 10px; 
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Payment Processing Issue</h1>
                        <div class="subtitle">National Water Resources Authority</div>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Dear %s,</div>
                        
                        <div class="warning-message">
                            <span class="warning-icon">⚠</span>
                            <strong>Payment Processing Issue</strong><br>
                            We regret to inform you that there was an issue processing your recent payment.
                        </div>
                        
                        <div class="details-section">
                            <div class="details-title">Payment Details</div>
                            <div class="detail-row">
                                <span class="detail-label">Application ID:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Payment Type:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Transaction Reference:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Date & Time:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Status:</span>
                                <span class="detail-value" style="color: #dc2626;">Payment could not be completed</span>
                            </div>
                        </div>
                        
                        <div class="next-steps">
                            <div class="next-steps-title">Next Steps:</div>
                            <ol>
                                <li>Please verify your payment details and try again</li>
                                <li>If you believe this is an error, please contact our support team with your transaction reference</li>
                                <li>You may also choose an alternative payment method</li>
                            </ol>
                        </div>
                        
                        <p>We apologize for any inconvenience caused. Our support team is ready to assist you with resolving this issue.</p>
                    </div>
                    
                    <div class="footer">
                        <div class="contact-info">
                            <strong>For Assistance Contact:</strong><br>
                            Email: support@nwra.gov.mw | Phone: +265 1 771 336
                        </div>
                        
                        <div class="signature">
                            National Water Resources Authority<br>
                            Government of Malawi
                        </div>
                        
                        <div class="government-seal">
                            Official Government Communication
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, applicantName, applicationId, paymentType, orderId, paymentDate);
    }
}
