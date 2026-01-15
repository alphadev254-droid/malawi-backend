package mw.nwra.ewaterpermit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import mw.nwra.ewaterpermit.model.BomaPayTransactionHistory;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.model.UserNotification;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationCategory;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationPriority;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationType;
import mw.nwra.ewaterpermit.service.NotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class BomaPayServiceImpl implements BomaPayService {

    @Autowired
    private BomaPayTransactionHistoryService transactionHistoryService;
    
    @Autowired
    private NotificationService notificationService;

    @Value("${bomapay.base.url:https://dev.bpcbt.com/payment}")
    private String bomaPayBaseUrl;

    @Value("${bomapay.username:test_user}")
    private String bomaPayUsername;

    @Value("${bomapay.password:test_user_password}")
    private String bomaPayPassword;

    @Value("${bomapay.currency:454}")
    private String currency; // 454 = MWK (Malawi Kwacha)

    @Value("${bomapay.client.id:259753456}")
    private String clientId;

    @Value("${bomapay.merchant.login:OurBestMerchantLogin}")
    private String merchantLogin;

    @Value("${bomapay.return.url:http://localhost:4200/payments/return}")
    private String returnUrl;

    @Value("${bomapay.fail.url:http://localhost:4200/payments/failed}")
    private String failUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Map<String, Object> initiatePayment(CoreLicenseApplication application, String paymentType, Double amount, SysUserAccount user) {
        try {
            String orderNumber = generateOrderNumber(application.getId(), paymentType);
            String description = generatePaymentDescription(application, paymentType);
            
            // Convert amount to minor units (multiply by 100 for MWK)
            Long amountInMinorUnits = Math.round(amount * 100);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("amount", amountInMinorUnits.toString());
            formData.add("currency", currency);
            formData.add("language", "en");
            formData.add("orderNumber", orderNumber);
            formData.add("returnUrl", returnUrl); // BomaPay will add orderId automatically
            formData.add("userName", bomaPayUsername);
            formData.add("password", bomaPayPassword);
            formData.add("clientId", clientId);

            // BomaPay API call

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                bomaPayBaseUrl + "/rest/register.do", 
                request, 
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            
            // Process BomaPay response
            
            // Check for successful response - either errorCode "0" OR presence of orderId and formUrl
            if (responseBody != null && 
                ("0".equals(String.valueOf(responseBody.get("errorCode"))) || 
                 (responseBody.get("orderId") != null && responseBody.get("formUrl") != null))) {
                // Payment order created successfully - HOSTED APPROACH
                String orderId = String.valueOf(responseBody.get("orderId"));
                String formUrl = String.valueOf(responseBody.get("formUrl"));
                
                // Save payment initiation to transaction history
                try {
                    System.out.println("🔍 PAYMENT INITIATION SUCCESSFUL - Saving to transaction history");
                    BomaPayTransactionHistory transaction = transactionHistoryService.savePaymentInitiation(
                        application, 
                        paymentType, 
                        amount, 
                        currency, 
                        orderId, 
                        orderNumber, 
                        formUrl, 
                        user.getId()
                    );
                    System.out.println("✅ Payment initiation saved to transaction history with ID: " + transaction.getId());
                } catch (Exception e) {
                    System.err.println("❌ Failed to save payment initiation to history: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Create payment initiation notification
                createPaymentInitiationNotification(user, application, paymentType, amount, orderNumber);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("orderId", orderId);
                result.put("formUrl", formUrl); // This is the hosted payment page
                result.put("orderNumber", orderNumber);
                result.put("amount", amount);
                result.put("description", description);
                result.put("status", "REGISTERED");
                result.put("paymentType", "HOSTED"); // Indicate this is hosted payment
                
                return result;
            } else {
                // Handle error - save failed initiation to history
                String errorMessage = responseBody != null ? String.valueOf(responseBody.get("errorMessage")) : "Unknown error";
                String errorCode = responseBody != null ? String.valueOf(responseBody.get("errorCode")) : "UNKNOWN";
                
                try {
                    // Save failed payment attempt
                    BomaPayTransactionHistory transaction = transactionHistoryService.savePaymentInitiation(
                        application, 
                        paymentType, 
                        amount, 
                        currency, 
                        "FAILED_" + UUID.randomUUID().toString(), // Generate a fake orderId for failed attempts
                        orderNumber, 
                        null, // No formUrl for failed attempts
                        user.getId()
                    );
                    
                    // Update with error details
                    transactionHistoryService.updatePaymentStatus(
                        transaction.getOrderId(), 
                        "FAILED", 
                        "FAILED", 
                        errorMessage, 
                        errorCode
                    );
                } catch (Exception e) {
                    System.err.println("Failed to save payment error to history: " + e.getMessage());
                }
                
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", errorMessage);
                errorResult.put("errorCode", errorCode);
                
                return errorResult;
            }

        } catch (Exception e) {
            // Save exception to payment history
            try {
                String orderNumber = generateOrderNumber(application.getId(), paymentType);
                BomaPayTransactionHistory transaction = transactionHistoryService.savePaymentInitiation(
                    application, 
                    paymentType, 
                    amount, 
                    currency, 
                    "EXCEPTION_" + UUID.randomUUID().toString(), 
                    orderNumber, 
                    null, 
                    user.getId()
                );
                
                transactionHistoryService.updatePaymentStatus(
                    transaction.getOrderId(), 
                    "FAILED", 
                    "EXCEPTION", 
                    "Failed to initiate payment: " + e.getMessage(), 
                    "EXCEPTION"
                );
            } catch (Exception historyException) {
                System.err.println("Failed to save payment exception to history: " + historyException.getMessage());
            }
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Failed to initiate payment: " + e.getMessage());
            
            return errorResult;
        }
    }

    @Override
    public Map<String, Object> getPaymentStatus(String orderId) {
        System.out.println("🔍 Checking payment status for orderId: " + orderId);
        
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("userName", bomaPayUsername);
            formData.add("password", bomaPayPassword);
            formData.add("language", "en");
            formData.add("orderId", orderId);

            System.out.println("🔍 Sending status request to BomaPay with orderId: " + orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                bomaPayBaseUrl + "/rest/getOrderStatusExtended.do", 
                request, 
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            System.out.println("🔍 BomaPay response: " + responseBody);
            
            if (responseBody != null && "0".equals(String.valueOf(responseBody.get("errorCode")))) {
                // Map BomaPay status to our status
                String bomaPayStatus = String.valueOf(responseBody.get("orderStatus"));
                String mappedStatus = mapBomaPayStatus(bomaPayStatus);
                
                System.out.println("🔍 Mapped status: " + mappedStatus + " (BomaPay status: " + bomaPayStatus + ")");
                
                // Update transaction history with latest status
                try {
                    System.out.println("🔍 Updating transaction history for orderId: " + orderId);
                    transactionHistoryService.updatePaymentStatus(
                        orderId,
                        mappedStatus,
                        bomaPayStatus,
                        null,
                        null
                    );
                    System.out.println("🔍 Transaction history updated successfully");
                    
                    // If payment is completed, mark as such
                    if ("COMPLETED".equals(mappedStatus)) {
                        System.out.println("🔍 Payment completed - marking as completed and creating notification");
                        transactionHistoryService.markPaymentCompleted(
                            orderId,
                            String.valueOf(responseBody.get("orderNumber")),
                            responseBody
                        );
                        
                        // Create payment success notification
                        try {
                            // Get transaction to find user and application details
                            var transaction = transactionHistoryService.findByOrderId(orderId);
                            if (transaction.isPresent()) {
                                BomaPayTransactionHistory txn = transaction.get();
                                System.out.println("🔍 Creating success notification for user: " + txn.getInitiatedByUserId());
                                createPaymentSuccessNotification(
                                    txn.getInitiatedByUserId(), 
                                    txn.getCoreLicenseApplication(),
                                    txn.getPaymentType(),
                                    txn.getAmount(),
                                    txn.getOrderNumber()
                                );
                            } else {
                                System.err.println("🔍 Transaction not found for orderId: " + orderId);
                            }
                        } catch (Exception e) {
                            System.err.println("🔍 Failed to create payment success notification: " + e.getMessage());
                            e.printStackTrace();
                        }
                        
                        // TODO: INTEGRATION POINT - When payment status check shows COMPLETED,
                        //       this is another place where we need to integrate with the existing
                        //       upload receipt approval workflow for successful payments.
                    } else if ("DECLINED".equals(mappedStatus) || "REVERSED".equals(mappedStatus)) {
                        System.out.println("🔍 Payment failed - creating failure notification");
                        // Create payment failure notification
                        try {
                            var transaction = transactionHistoryService.findByOrderId(orderId);
                            if (transaction.isPresent()) {
                                BomaPayTransactionHistory txn = transaction.get();
                                System.out.println("🔍 Creating failure notification for user: " + txn.getInitiatedByUserId());
                                createPaymentFailureNotification(
                                    txn.getInitiatedByUserId(), 
                                    txn.getCoreLicenseApplication(),
                                    txn.getPaymentType(),
                                    txn.getAmount(),
                                    txn.getOrderNumber(),
                                    mappedStatus
                                );
                            } else {
                                System.err.println("🔍 Transaction not found for orderId: " + orderId);
                            }
                        } catch (Exception e) {
                            System.err.println("🔍 Failed to create payment failure notification: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("🔍 Failed to update payment status in history: " + e.getMessage());
                    e.printStackTrace();
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("orderId", orderId);
                result.put("orderStatus", responseBody.get("orderStatus"));
                result.put("amount", responseBody.get("amount"));
                result.put("currency", responseBody.get("currency"));
                result.put("orderNumber", responseBody.get("orderNumber"));
                result.put("paymentAmountInfo", responseBody.get("paymentAmountInfo"));
                result.put("status", mappedStatus);
                
                return result;
            } else {
                System.err.println("🔍 BomaPay API error response: " + responseBody);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", responseBody != null ? responseBody.get("errorMessage") : "Unknown error");
                
                return errorResult;
            }

        } catch (Exception e) {
            System.err.println("🔍 Exception in getPaymentStatus: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Failed to get payment status: " + e.getMessage());
            
            return errorResult;
        }
    }

    // Card processing removed - using hosted payment approach only

    @Override
    public void handleWebhook(Map<String, Object> webhookData) {
        try {
            String orderId = (String) webhookData.get("orderId");
            String orderNumber = (String) webhookData.get("orderNumber");
            String status = (String) webhookData.get("status");
            
            System.out.println("BomaPay Webhook received:");
            System.out.println("Order ID: " + orderId);
            System.out.println("Order Number: " + orderNumber);
            System.out.println("Status: " + status);
            
            // Record webhook received and update payment status
            try {
                // Record that webhook was received
                transactionHistoryService.recordWebhookReceived(orderId, webhookData.toString());
                
                // Update payment status based on webhook data
                String mappedStatus = mapBomaPayStatus(status);
                transactionHistoryService.updatePaymentStatus(
                    orderId,
                    mappedStatus,
                    status,
                    null,
                    null
                );
                
                // If payment completed, mark as such
                if ("COMPLETED".equals(mappedStatus)) {
                    transactionHistoryService.markPaymentCompleted(
                        orderId,
                        orderNumber,
                        webhookData
                    );
                    
                    // Create payment success notification
                    try {
                        var transaction = transactionHistoryService.findByOrderId(orderId);
                        if (transaction.isPresent()) {
                            BomaPayTransactionHistory txn = transaction.get();
                            createPaymentSuccessNotification(
                                txn.getInitiatedByUserId(), 
                                txn.getCoreLicenseApplication(),
                                txn.getPaymentType(),
                                txn.getAmount(),
                                txn.getOrderNumber()
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to create payment success notification: " + e.getMessage());
                    }
                } else if ("DECLINED".equals(mappedStatus) || "REVERSED".equals(mappedStatus)) {
                    // Create payment failure notification
                    try {
                        var transaction = transactionHistoryService.findByOrderId(orderId);
                        if (transaction.isPresent()) {
                            BomaPayTransactionHistory txn = transaction.get();
                            createPaymentFailureNotification(
                                txn.getInitiatedByUserId(), 
                                txn.getCoreLicenseApplication(),
                                txn.getPaymentType(),
                                txn.getAmount(),
                                txn.getOrderNumber(),
                                mappedStatus
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to create payment failure notification: " + e.getMessage());
                    }
                }
                
                // TODO: Send email notifications to users
                // TODO: Update application payment status for successful payments
                // TODO: Create CoreApplicationPayment record when payment is successful
                // TODO: INTEGRATION POINT - When payment is COMPLETED, this is where we need to integrate
                //       with the existing upload receipt approval workflow. User will provide details on
                //       how the existing upload receipt process handles application approval, and we should
                //       replicate that behavior here for successful BomaPay payments.
                
            } catch (Exception e) {
                System.err.println("Failed to update payment history from webhook: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error processing BomaPay webhook: " + e.getMessage());
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    @Override
    public Map<String, Object> reversePayment(String orderId) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("userName", bomaPayUsername);
            formData.add("password", bomaPayPassword);
            formData.add("orderId", orderId);
            formData.add("language", "en");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                bomaPayBaseUrl + "/rest/reverse.do", 
                request, 
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && "0".equals(String.valueOf(responseBody.get("errorCode")))) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("orderId", orderId);
                result.put("status", "REVERSED");
                
                return result;
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", responseBody != null ? responseBody.get("errorMessage") : "Reversal failed");
                
                return errorResult;
            }

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Failed to reverse payment: " + e.getMessage());
            
            return errorResult;
        }
    }

    private String generateOrderNumber(String applicationId, String paymentType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("NWRA-%s-%s-%s", paymentType, applicationId, timestamp);
    }

    private String generatePaymentDescription(CoreLicenseApplication application, String paymentType) {
        String licenseType = application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "License";
        
        if ("LICENSE_FEE".equals(paymentType)) {
            return String.format("License Fee Payment - %s (ID: %s)", licenseType, application.getId());
        } else {
            return String.format("Application Fee Payment - %s (ID: %s)", licenseType, application.getId());
        }
    }

    private String mapBomaPayStatus(String bomaPayStatus) {
        switch (bomaPayStatus) {
            case "0": return "REGISTERED";
            case "1": return "PROCESSING";
            case "2": return "COMPLETED";
            case "3": return "REVERSED";
            case "4": return "REFUNDED";
            case "5": return "ACS_REDIRECT";
            case "6": return "DECLINED";
            default: return "UNKNOWN";
        }
    }

    private void createPaymentInitiationNotification(SysUserAccount user, CoreLicenseApplication application, String paymentType, Double amount, String orderNumber) {
        try {
            String licenseType = application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "License";
            String paymentTypeText = "APPLICATION_FEE".equals(paymentType) ? "Application Fee" : "License Fee";
            
            UserNotification notification = new UserNotification();
            notification.setUserId(user.getId());
            notification.setTitle("Payment Initiated");
            notification.setMessage(String.format("Your %s payment of MWK %.2f for %s has been initiated. Order: %s", 
                paymentTypeText, amount, licenseType, orderNumber));
            notification.setType(NotificationType.INFO);
            notification.setCategory(NotificationCategory.PAYMENT);
            notification.setPriority(NotificationPriority.MEDIUM);
            notification.setActionUrl("/e-services/my-applications");
            notification.setActionLabel("View Applications");
            
            notificationService.createNotification(notification);
        } catch (Exception e) {
            System.err.println("Failed to create payment initiation notification: " + e.getMessage());
        }
    }

    private void createPaymentSuccessNotification(String userId, CoreLicenseApplication application, String paymentType, Double amount, String orderNumber) {
        try {
            String licenseType = application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "License";
            String paymentTypeText = "APPLICATION_FEE".equals(paymentType) ? "Application Fee" : "License Fee";
            
            UserNotification notification = new UserNotification();
            notification.setUserId(userId);
            notification.setTitle("Payment Successful!");
            notification.setMessage(String.format("Your %s payment of MWK %.2f for %s has been successfully processed. Order: %s", 
                paymentTypeText, amount, licenseType, orderNumber));
            notification.setType(NotificationType.SUCCESS);
            notification.setCategory(NotificationCategory.PAYMENT);
            notification.setPriority(NotificationPriority.HIGH);
            notification.setActionUrl("/e-services/my-applications");
            notification.setActionLabel("View Applications");
            
            notificationService.createNotification(notification);
        } catch (Exception e) {
            System.err.println("Failed to create payment success notification: " + e.getMessage());
        }
    }

    private void createPaymentFailureNotification(String userId, CoreLicenseApplication application, String paymentType, Double amount, String orderNumber, String status) {
        try {
            String licenseType = application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "License";
            String paymentTypeText = "APPLICATION_FEE".equals(paymentType) ? "Application Fee" : "License Fee";
            String statusText = "DECLINED".equals(status) ? "declined" : "reversed";
            
            UserNotification notification = new UserNotification();
            notification.setUserId(userId);
            notification.setTitle("Payment Failed");
            notification.setMessage(String.format("Your %s payment of MWK %.2f for %s was %s. Order: %s. Please try again or contact support.", 
                paymentTypeText, amount, licenseType, statusText, orderNumber));
            notification.setType(NotificationType.ERROR);
            notification.setCategory(NotificationCategory.PAYMENT);
            notification.setPriority(NotificationPriority.HIGH);
            notification.setActionUrl("/e-services/my-applications");
            notification.setActionLabel("Try Again");
            
            notificationService.createNotification(notification);
        } catch (Exception e) {
            System.err.println("Failed to create payment failure notification: " + e.getMessage());
        }
    }

    @Override
    public void processCompletedPayment(String orderId, Map<String, Object> paymentDetails) {
        try {
            System.out.println("🔍 Processing completed payment for orderId: " + orderId);
            
            // Get the transaction from our history
            var transactionOpt = transactionHistoryService.findByOrderId(orderId);
            if (!transactionOpt.isPresent()) {
                System.err.println("🔍 Transaction not found for orderId: " + orderId);
                return;
            }
            
            BomaPayTransactionHistory transaction = transactionOpt.get();
            CoreLicenseApplication application = transaction.getCoreLicenseApplication();
            
            // TODO: INTEGRATION POINT - Here you would integrate with your existing payment processing logic
            // This is where you would:
            // 1. Create CoreApplicationPayment record
            // 2. Update application status (if needed)
            // 3. Trigger any approval workflows
            // 4. Send confirmation emails
            // 5. Update any other related entities
            
            System.out.println("🔍 Payment processing completed for application: " + application.getId());
            System.out.println("🔍 Payment type: " + transaction.getPaymentType());
            System.out.println("🔍 Amount: " + transaction.getAmount());
            
            // Log the successful processing
            System.out.println("✅ Successfully processed completed payment for orderId: " + orderId);
            
        } catch (Exception e) {
            System.err.println("🔍 Failed to process completed payment for orderId: " + orderId + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}