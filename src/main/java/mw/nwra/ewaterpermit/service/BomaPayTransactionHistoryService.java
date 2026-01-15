package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.model.BomaPayTransactionHistory;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BomaPayTransactionHistoryService {
    
    BomaPayTransactionHistory savePaymentInitiation(
        CoreLicenseApplication application, 
        String paymentType, 
        Double amount,
        String currency,
        String orderId, 
        String orderNumber,
        String formUrl,
        String initiatedByUserId
    );
    
    BomaPayTransactionHistory updatePaymentStatus(
        String orderId, 
        String paymentStatus,
        String bomaPayStatus,
        String errorMessage,
        String errorCode
    );
    
    BomaPayTransactionHistory markPaymentCompleted(
        String orderId,
        String transactionReference,
        Map<String, Object> paymentDetails
    );
    
    BomaPayTransactionHistory recordWebhookReceived(
        String orderId,
        String webhookData
    );
    
    BomaPayTransactionHistory recordReturnUrlVisited(String orderId);
    
    Optional<BomaPayTransactionHistory> findByOrderId(String orderId);
    
    Optional<BomaPayTransactionHistory> findByOrderNumber(String orderNumber);
    
    List<BomaPayTransactionHistory> getPaymentHistoryByApplication(String applicationId);
    
    List<BomaPayTransactionHistory> getPaymentHistoryByUser(String userId);
    
    List<BomaPayTransactionHistory> getSuccessfulPaymentsByApplication(String applicationId);
    
    long countSuccessfulPaymentsByApplication(String applicationId);
    
    List<BomaPayTransactionHistory> getPaymentHistoryByApplicationAndType(String applicationId, String paymentType);
}