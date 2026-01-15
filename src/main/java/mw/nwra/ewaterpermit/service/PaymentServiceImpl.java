package mw.nwra.ewaterpermit.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreApplicationPayment;
import mw.nwra.ewaterpermit.model.CoreFeesType;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.requestSchema.PaymentRequest;
import mw.nwra.ewaterpermit.responseSchema.PaymentResponse;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Value("${bomapay.gateway.url:https://dev.bpcbt.com/payment}")
    private String bomaPayGatewayUrl;

    @Value("${bomapay.username:boma-api}")
    private String bomaPayUsername;

    @Value("${bomapay.password:hssyl-R9}")
    private String bomaPayPassword;

    @Autowired
    private CoreApplicationPaymentService applicationPaymentService;

    @Autowired
    private CoreFeesTypeService feesTypeService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        try {
            // Create payment reference
            String paymentReference = "NWRA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Prepare BOMAPay request
            Map<String, Object> bomaPayRequest = createBomaPayRequest(request, paymentReference);
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.setBasicAuth(bomaPayUsername, bomaPayPassword);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(bomaPayRequest, headers);
            
            // Call BOMAPay API
            ResponseEntity<Map> response = restTemplate.exchange(
                bomaPayGatewayUrl + "/initiate",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            // Process response
            Map<String, Object> responseBody = response.getBody();
            
            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setPaymentReference(paymentReference);
            paymentResponse.setApplicationId(request.getApplicationId());
            paymentResponse.setAmount(request.getAmount());
            paymentResponse.setCurrency(request.getCurrency());
            paymentResponse.setCustomerName(request.getCustomerName());
            paymentResponse.setCustomerEmail(request.getCustomerEmail());
            paymentResponse.setCustomerPhoneNumber(request.getCustomerPhoneNumber());
            paymentResponse.setDescription(request.getDescription());
            paymentResponse.setCreatedDate(new Timestamp(new Date().getTime()));
            
            if (responseBody != null && "SUCCESS".equals(responseBody.get("status"))) {
                paymentResponse.setStatus("PENDING");
                paymentResponse.setBomaPayReference((String) responseBody.get("reference"));
                paymentResponse.setTransactionId((String) responseBody.get("transactionId"));
            } else {
                paymentResponse.setStatus("FAILED");
                paymentResponse.setErrorMessage("Payment initiation failed: " + 
                    (responseBody != null ? responseBody.get("message") : "Unknown error"));
            }
            
            return paymentResponse;
            
        } catch (Exception e) {
            throw new ForbiddenException("Payment initiation failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse verifyPayment(String paymentReference) {
        try {
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.setBasicAuth(bomaPayUsername, bomaPayPassword);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Call BOMAPay verification API
            ResponseEntity<Map> response = restTemplate.exchange(
                bomaPayGatewayUrl + "/verify/" + paymentReference,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setPaymentReference(paymentReference);
            
            if (responseBody != null) {
                paymentResponse.setStatus((String) responseBody.get("status"));
                paymentResponse.setBomaPayReference((String) responseBody.get("reference"));
                paymentResponse.setTransactionId((String) responseBody.get("transactionId"));
                paymentResponse.setAmount(new BigDecimal(responseBody.get("amount").toString()));
                paymentResponse.setCurrency((String) responseBody.get("currency"));
                
                if ("COMPLETED".equals(responseBody.get("status"))) {
                    paymentResponse.setCompletedDate(new Timestamp(new Date().getTime()));
                }
            } else {
                paymentResponse.setStatus("FAILED");
                paymentResponse.setErrorMessage("Payment verification failed");
            }
            
            return paymentResponse;
            
        } catch (Exception e) {
            throw new ForbiddenException("Payment verification failed: " + e.getMessage());
        }
    }

    @Override
    public CoreApplicationPayment processApplicationPayment(CoreLicenseApplication application, String paymentType) {
        // Get fee type
        CoreFeesType feeType = feesTypeService.getCoreFeesTypeByName(paymentType);
        if (feeType == null) {
            throw new ForbiddenException("Fee type not found: " + paymentType);
        }
        
        // Create payment record
        CoreApplicationPayment payment = new CoreApplicationPayment();
        payment.setCoreLicenseApplication(application);
        payment.setCoreFeesType(feeType);
        // Set default amount for fee types (this should be configured in the system)
        double defaultAmount = getDefaultAmountForFeeType(paymentType);
        payment.setAmountPaid(defaultAmount);
        payment.setDateCreated(new Timestamp(new Date().getTime()));
        
        // Generate payment reference (this would need to be stored in a separate field or handled differently)
        String paymentReference = "NWRA-" + application.getId().substring(0, 8).toUpperCase() + 
                                 "-" + paymentType.substring(0, 3).toUpperCase();
        
        return applicationPaymentService.addCoreApplicationPayment(payment);
    }

    @Override
    public PaymentResponse generateInvoice(CoreLicenseApplication application, String feeType) {
        CoreFeesType fee = feesTypeService.getCoreFeesTypeByName(feeType);
        if (fee == null) {
            throw new ForbiddenException("Fee type not found: " + feeType);
        }
        
        PaymentResponse response = new PaymentResponse();
        response.setApplicationId(application.getId());
        response.setAmount(BigDecimal.valueOf(getDefaultAmountForFeeType(feeType)));
        response.setCurrency("MWK");
        response.setDescription(fee.getDescription());
        response.setStatus("INVOICE_GENERATED");
        response.setCreatedDate(new Timestamp(new Date().getTime()));
        
        // Generate invoice reference
        String invoiceReference = "INV-" + application.getId().substring(0, 8).toUpperCase() + 
                                 "-" + System.currentTimeMillis();
        response.setPaymentReference(invoiceReference);
        
        return response;
    }

    @Override
    public boolean confirmPayment(String paymentReference) {
        PaymentResponse verification = verifyPayment(paymentReference);
        return "COMPLETED".equals(verification.getStatus());
    }

    @Override
    public PaymentResponse getPaymentStatus(String paymentReference) {
        return verifyPayment(paymentReference);
    }

    private Map<String, Object> createBomaPayRequest(PaymentRequest request, String paymentReference) {
        Map<String, Object> bomaPayRequest = new HashMap<>();
        
        // Basic payment information
        bomaPayRequest.put("merchantReference", paymentReference);
        bomaPayRequest.put("amount", request.getAmount());
        bomaPayRequest.put("currency", request.getCurrency());
        bomaPayRequest.put("description", request.getDescription());
        bomaPayRequest.put("paymentMethod", request.getPaymentMethod());
        
        // Customer information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", request.getCustomerName());
        customer.put("email", request.getCustomerEmail());
        customer.put("phoneNumber", request.getCustomerPhoneNumber());
        bomaPayRequest.put("customer", customer);
        
        // NPG Interface fields for IFMIS integration
        Map<String, Object> npgFields = new HashMap<>();
        npgFields.put("BLDAT", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // Document Date
        npgFields.put("BUDAT", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // Posting Date
        npgFields.put("BLART", request.getDocumentType()); // Document Type
        npgFields.put("BUKRS", request.getCompanyCode()); // Company Code
        npgFields.put("MONAT", request.getPeriod() != null ? request.getPeriod() : String.format("%02d", LocalDate.now().getMonthValue())); // Period
        npgFields.put("WAERS", request.getCurrency()); // Currency
        npgFields.put("XBLNR", paymentReference); // Reference Number
        
        // Debit entry
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("NEWBS", request.getPostingKeyDebit()); // Posting Key Debit
        debitEntry.put("NEWKO", request.getGlAccountDebit()); // G/L Account Debit
        debitEntry.put("WRBTR", request.getAmount()); // Amount
        
        // Credit entry
        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("NEWBS", request.getPostingKeyCredit()); // Posting Key Credit
        creditEntry.put("NEWKO", request.getRevenueGlAccount()); // Revenue G/L Account
        creditEntry.put("WRBTR", request.getAmount()); // Revenue Amount
        creditEntry.put("MWSKZ", request.getTaxCode()); // Tax Code
        creditEntry.put("SGTXT", request.getDescription()); // Item Text
        
        // Additional fields
        npgFields.put("GSBER", request.getBusinessArea()); // Business Area
        npgFields.put("GEBER", request.getFund()); // Fund
        npgFields.put("GRANT_NBR", request.getGrant()); // Grant
        npgFields.put("FKBER", request.getFunctionalArea()); // Functional Area
        npgFields.put("FIPEX", request.getCommitmentItem()); // Commitment Item
        
        npgFields.put("debitEntry", debitEntry);
        npgFields.put("creditEntry", creditEntry);
        
        bomaPayRequest.put("npgFields", npgFields);
        
        return bomaPayRequest;
    }
    
    private double getDefaultAmountForFeeType(String feeType) {
        // This should be configured in the system or retrieved from a configuration table
        // For now, using default values based on the requirements
        switch (feeType.toUpperCase()) {
            case "APPLICATION_FEE":
                return 5000.00;
            case "PERMIT_FEE":
                return 10000.00;
            case "SURFACE_WATER_PERMIT":
                return 5000.00;
            case "EFFLUENT_DISCHARGE_PERMIT":
                return 7500.00;
            case "BOREHOLE_PERMIT":
                return 6000.00;
            case "RENEWAL_FEE":
                return 3000.00;
            case "TRANSFER_FEE":
                return 2500.00;
            case "VARIATION_FEE":
                return 2000.00;
            default:
                return 5000.00; // Default amount
        }
    }

    // BOMAPay Integration Implementation
    @Override
    public PaymentResponse registerBOMAPIayOrder(PaymentRequest request) {
        try {
            // Prepare BOMAPay register order request
            Map<String, Object> bomaPayRequest = new HashMap<>();
            bomaPayRequest.put("amount", request.getAmount());
            bomaPayRequest.put("currency", request.getCurrency());
            bomaPayRequest.put("language", "en");
            bomaPayRequest.put("orderNumber", request.getOrderNumber());
            bomaPayRequest.put("returnUrl", request.getReturnUrl());
            bomaPayRequest.put("failUrl", request.getFailUrl());
            bomaPayRequest.put("description", request.getDescription());
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            // Create form data
            String formData = String.format(
                "amount=%s&currency=%s&language=en&orderNumber=%s&returnUrl=%s&failUrl=%s&userName=%s&password=%s&clientId=259753456",
                request.getAmount(), request.getCurrency(), request.getOrderNumber(), 
                request.getReturnUrl(), request.getFailUrl(), bomaPayUsername, bomaPayPassword
            );
            
            HttpEntity<String> entity = new HttpEntity<>(formData, headers);
            
            // Call BOMAPay register API
            ResponseEntity<Map> response = restTemplate.exchange(
                bomaPayGatewayUrl + "/rest/register.do",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setApplicationId(request.getApplicationId());
            paymentResponse.setAmount(request.getAmount());
            paymentResponse.setCurrency(request.getCurrency());
            paymentResponse.setDescription(request.getDescription());
            paymentResponse.setCreatedDate(new Timestamp(new Date().getTime()));
            
            if (responseBody != null && responseBody.get("errorCode").equals("0")) {
                paymentResponse.setStatus("PENDING");
                paymentResponse.setOrderId((String) responseBody.get("orderId"));
                paymentResponse.setFormUrl((String) responseBody.get("formUrl"));
            } else {
                paymentResponse.setStatus("FAILED");
                paymentResponse.setErrorMessage("Order registration failed: " + 
                    (responseBody != null ? responseBody.get("errorMessage") : "Unknown error"));
            }
            
            return paymentResponse;
            
        } catch (Exception e) {
            throw new ForbiddenException("BOMAPay order registration failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse getBOMAPIayOrderStatus(String orderId) {
        try {
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            // Create form data
            String formData = String.format(
                "userName=%s&password=%s&orderId=%s&language=en", 
                bomaPayUsername, bomaPayPassword, orderId
            );
            
            HttpEntity<String> entity = new HttpEntity<>(formData, headers);
            
            // Call BOMAPay status API
            ResponseEntity<Map> response = restTemplate.exchange(
                bomaPayGatewayUrl + "/rest/getOrderStatusExtended.do",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setOrderId(orderId);
            
            if (responseBody != null) {
                paymentResponse.setOrderStatus((Integer) responseBody.get("orderStatus"));
                paymentResponse.setAmount(new BigDecimal(responseBody.get("amount").toString()));
                paymentResponse.setCurrency((String) responseBody.get("currency"));
                
                // Map order status to readable status
                Integer orderStatus = (Integer) responseBody.get("orderStatus");
                switch (orderStatus) {
                    case 0:
                        paymentResponse.setStatus("REGISTERED");
                        break;
                    case 1:
                        paymentResponse.setStatus("PRE_AUTHORIZED");
                        break;
                    case 2:
                        paymentResponse.setStatus("DEPOSITED");
                        break;
                    case 3:
                        paymentResponse.setStatus("REVERSED");
                        break;
                    case 4:
                        paymentResponse.setStatus("REFUNDED");
                        break;
                    case 5:
                        paymentResponse.setStatus("AUTHORIZED");
                        break;
                    case 6:
                        paymentResponse.setStatus("DECLINED");
                        break;
                    default:
                        paymentResponse.setStatus("UNKNOWN");
                }
            } else {
                paymentResponse.setStatus("FAILED");
                paymentResponse.setErrorMessage("Status check failed");
            }
            
            return paymentResponse;
            
        } catch (Exception e) {
            throw new ForbiddenException("BOMAPay status check failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse processMobileMoneyPayment(PaymentRequest request) {
        try {
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            // Create form data for mobile money payment
            String formData = String.format(
                "userName=%s&password=%s&orderId=%s&phone=%s&amount=%s", 
                bomaPayUsername, bomaPayPassword, request.getOrderId(), 
                request.getPhoneNumber(), request.getAmount()
            );
            
            HttpEntity<String> entity = new HttpEntity<>(formData, headers);
            
            // Call BOMAPay mobile money API
            ResponseEntity<Map> response = restTemplate.exchange(
                bomaPayGatewayUrl + "/rest/mobilePayment.do",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setOrderId(request.getOrderId());
            paymentResponse.setAmount(request.getAmount());
            paymentResponse.setPhoneNumber(request.getPhoneNumber());
            paymentResponse.setCreatedDate(new Timestamp(new Date().getTime()));
            
            if (responseBody != null && responseBody.get("errorCode").equals("0")) {
                paymentResponse.setStatus("PROCESSING");
                paymentResponse.setTransactionId((String) responseBody.get("transactionId"));
            } else {
                paymentResponse.setStatus("FAILED");
                paymentResponse.setErrorMessage("Mobile money payment failed: " + 
                    (responseBody != null ? responseBody.get("errorMessage") : "Unknown error"));
            }
            
            return paymentResponse;
            
        } catch (Exception e) {
            throw new ForbiddenException("Mobile money payment failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse refundBOMAPIayOrder(PaymentRequest request) {
        try {
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            // Create form data for refund
            String formData = String.format(
                "userName=%s&password=%s&orderId=%s&amount=%s", 
                bomaPayUsername, bomaPayPassword, request.getOrderId(), request.getAmount()
            );
            
            HttpEntity<String> entity = new HttpEntity<>(formData, headers);
            
            // Call BOMAPay refund API
            ResponseEntity<Map> response = restTemplate.exchange(
                bomaPayGatewayUrl + "/rest/refund.do",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setOrderId(request.getOrderId());
            paymentResponse.setAmount(request.getAmount());
            paymentResponse.setCreatedDate(new Timestamp(new Date().getTime()));
            
            if (responseBody != null && responseBody.get("errorCode").equals("0")) {
                paymentResponse.setStatus("REFUNDED");
                paymentResponse.setTransactionId((String) responseBody.get("transactionId"));
            } else {
                paymentResponse.setStatus("FAILED");
                paymentResponse.setErrorMessage("Refund failed: " + 
                    (responseBody != null ? responseBody.get("errorMessage") : "Unknown error"));
            }
            
            return paymentResponse;
            
        } catch (Exception e) {
            throw new ForbiddenException("Refund failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse getApplicationPaymentStatus(String applicationId) {
        try {
            // This would typically query your database for payment status
            // For now, return a mock response
            PaymentResponse response = new PaymentResponse();
            response.setApplicationId(applicationId);
            response.setPaymentStatus("PENDING");
            response.setAmountDue(BigDecimal.valueOf(5000.00));
            response.setCurrency("MWK");
            response.setDueDate(new Timestamp(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000))); // 7 days from now
            
            return response;
            
        } catch (Exception e) {
            throw new ForbiddenException("Failed to get application payment status: " + e.getMessage());
        }
    }
}