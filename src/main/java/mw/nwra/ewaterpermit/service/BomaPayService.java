package mw.nwra.ewaterpermit.service;

import java.util.Map;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;

public interface BomaPayService {
    
    Map<String, Object> initiatePayment(CoreLicenseApplication application, String paymentType, Double amount, SysUserAccount user);
    
    Map<String, Object> getPaymentStatus(String orderId);
    
    void handleWebhook(Map<String, Object> webhookData);
    
    Map<String, Object> reversePayment(String orderId);
    
    void processCompletedPayment(String orderId, Map<String, Object> paymentDetails);
}