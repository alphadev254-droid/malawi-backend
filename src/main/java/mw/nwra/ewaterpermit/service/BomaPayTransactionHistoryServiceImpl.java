package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.model.BomaPayTransactionHistory;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.repository.BomaPayTransactionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BomaPayTransactionHistoryServiceImpl implements BomaPayTransactionHistoryService {

    @Autowired
    private BomaPayTransactionHistoryRepository repository;

    @Override
    public BomaPayTransactionHistory savePaymentInitiation(
            CoreLicenseApplication application, 
            String paymentType, 
            Double amount,
            String currency,
            String orderId, 
            String orderNumber,
            String formUrl,
            String initiatedByUserId) {
        
        try {
            System.out.println("🔍 SAVING PAYMENT INITIATION:");
            System.out.println("  - orderId: " + orderId);
            System.out.println("  - orderNumber: " + orderNumber);
            System.out.println("  - amount: " + amount);
            System.out.println("  - currency: " + currency);
            System.out.println("  - paymentType: " + paymentType);
            System.out.println("  - applicationId: " + application.getId());
            System.out.println("  - userId: " + initiatedByUserId);
            
            BomaPayTransactionHistory transaction = new BomaPayTransactionHistory();
            // Don't set ID manually - let JPA generate it
            transaction.setOrderId(orderId);
            transaction.setOrderNumber(orderNumber);
            transaction.setAmount(amount);
            transaction.setCurrency(currency);
            transaction.setPaymentType(paymentType);
            transaction.setPaymentStatus("REGISTERED");
            transaction.setBomaPayStatus("REGISTERED");
            transaction.setFormUrl(formUrl);
            transaction.setInitiatedByUserId(initiatedByUserId);
            transaction.setInitiatedDate(Timestamp.valueOf(LocalDateTime.now()));
            transaction.setCoreLicenseApplication(application);
            transaction.setWebhookReceived(false);
            transaction.setReturnUrlVisited(false);
            
            System.out.println("🔍 About to save transaction with ID: " + transaction.getId());
            BomaPayTransactionHistory saved = repository.save(transaction);
            System.out.println("✅ Transaction saved successfully with ID: " + saved.getId());
            
            return saved;
        } catch (Exception e) {
            System.err.println("❌ FAILED TO SAVE PAYMENT INITIATION:");
            System.err.println("  - Error: " + e.getMessage());
            System.err.println("  - Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "No cause"));
            e.printStackTrace();
            throw new RuntimeException("Failed to save payment initiation: " + e.getMessage(), e);
        }
    }

    @Override
    public BomaPayTransactionHistory updatePaymentStatus(
            String orderId, 
            String paymentStatus,
            String bomaPayStatus,
            String errorMessage,
            String errorCode) {
        
        Optional<BomaPayTransactionHistory> optionalTransaction = repository.findByOrderId(orderId);
        if (optionalTransaction.isPresent()) {
            BomaPayTransactionHistory transaction = optionalTransaction.get();
            transaction.setPaymentStatus(paymentStatus);
            transaction.setBomaPayStatus(bomaPayStatus);
            transaction.setErrorMessage(errorMessage);
            transaction.setErrorCode(errorCode);
            
            if ("COMPLETED".equals(paymentStatus)) {
                transaction.setCompletedDate(Timestamp.valueOf(LocalDateTime.now()));
            }
            
            return repository.save(transaction);
        }
        
        throw new RuntimeException("Transaction not found for orderId: " + orderId);
    }

    @Override
    public BomaPayTransactionHistory markPaymentCompleted(
            String orderId,
            String transactionReference,
            Map<String, Object> paymentDetails) {
        
        Optional<BomaPayTransactionHistory> optionalTransaction = repository.findByOrderId(orderId);
        if (optionalTransaction.isPresent()) {
            BomaPayTransactionHistory transaction = optionalTransaction.get();
            transaction.setPaymentStatus("COMPLETED");
            transaction.setTransactionReference(transactionReference);
            transaction.setCompletedDate(Timestamp.valueOf(LocalDateTime.now()));
            
            if (paymentDetails != null) {
                transaction.setBomaPayStatus(String.valueOf(paymentDetails.get("orderStatus")));
            }
            
            return repository.save(transaction);
        }
        
        throw new RuntimeException("Transaction not found for orderId: " + orderId);
    }

    @Override
    public BomaPayTransactionHistory recordWebhookReceived(String orderId, String webhookData) {
        Optional<BomaPayTransactionHistory> optionalTransaction = repository.findByOrderId(orderId);
        if (optionalTransaction.isPresent()) {
            BomaPayTransactionHistory transaction = optionalTransaction.get();
            transaction.setWebhookReceived(true);
            transaction.setWebhookData(webhookData);
            
            return repository.save(transaction);
        }
        
        throw new RuntimeException("Transaction not found for orderId: " + orderId);
    }

    @Override
    public BomaPayTransactionHistory recordReturnUrlVisited(String orderId) {
        Optional<BomaPayTransactionHistory> optionalTransaction = repository.findByOrderId(orderId);
        if (optionalTransaction.isPresent()) {
            BomaPayTransactionHistory transaction = optionalTransaction.get();
            transaction.setReturnUrlVisited(true);
            return repository.save(transaction);
        }
        
        throw new RuntimeException("Transaction not found for orderId: " + orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BomaPayTransactionHistory> findByOrderId(String orderId) {
        return repository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BomaPayTransactionHistory> findByOrderNumber(String orderNumber) {
        return repository.findByOrderNumber(orderNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BomaPayTransactionHistory> getPaymentHistoryByApplication(String applicationId) {
        return repository.findByCoreLicenseApplicationIdOrderByInitiatedDateDesc(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BomaPayTransactionHistory> getPaymentHistoryByUser(String userId) {
        return repository.findByInitiatedByUserIdOrderByInitiatedDateDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BomaPayTransactionHistory> getSuccessfulPaymentsByApplication(String applicationId) {
        return repository.findSuccessfulPaymentsByApplicationId(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSuccessfulPaymentsByApplication(String applicationId) {
        return repository.countSuccessfulPaymentsByApplicationId(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BomaPayTransactionHistory> getPaymentHistoryByApplicationAndType(String applicationId, String paymentType) {
        return repository.findByApplicationIdAndPaymentType(applicationId, paymentType);
    }
}