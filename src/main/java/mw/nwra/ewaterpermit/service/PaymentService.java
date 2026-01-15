package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.model.CoreApplicationPayment;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.requestSchema.PaymentRequest;
import mw.nwra.ewaterpermit.responseSchema.PaymentResponse;

public interface PaymentService {
    
    PaymentResponse initiatePayment(PaymentRequest request);
    
    PaymentResponse verifyPayment(String paymentReference);
    
    CoreApplicationPayment processApplicationPayment(CoreLicenseApplication application, String paymentType);
    
    PaymentResponse generateInvoice(CoreLicenseApplication application, String feeType);
    
    boolean confirmPayment(String paymentReference);
    
    PaymentResponse getPaymentStatus(String paymentReference);
    
    // BOMAPay Integration Methods
    PaymentResponse registerBOMAPIayOrder(PaymentRequest request);
    
    PaymentResponse getBOMAPIayOrderStatus(String orderId);
    
    PaymentResponse processMobileMoneyPayment(PaymentRequest request);
    
    PaymentResponse refundBOMAPIayOrder(PaymentRequest request);
    
    PaymentResponse getApplicationPaymentStatus(String applicationId);
}