package mw.nwra.ewaterpermit.repository;

import mw.nwra.ewaterpermit.model.BomaPayTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BomaPayTransactionHistoryRepository extends JpaRepository<BomaPayTransactionHistory, String> {
    
    Optional<BomaPayTransactionHistory> findByOrderId(String orderId);
    
    Optional<BomaPayTransactionHistory> findByOrderNumber(String orderNumber);
    
    List<BomaPayTransactionHistory> findByCoreLicenseApplicationIdOrderByInitiatedDateDesc(String applicationId);
    
    List<BomaPayTransactionHistory> findByInitiatedByUserIdOrderByInitiatedDateDesc(String userId);
    
    List<BomaPayTransactionHistory> findByPaymentStatusOrderByInitiatedDateDesc(String paymentStatus);
    
    @Query("SELECT b FROM BomaPayTransactionHistory b WHERE b.coreLicenseApplication.id = :applicationId AND b.paymentType = :paymentType ORDER BY b.initiatedDate DESC")
    List<BomaPayTransactionHistory> findByApplicationIdAndPaymentType(@Param("applicationId") String applicationId, @Param("paymentType") String paymentType);
    
    @Query("SELECT b FROM BomaPayTransactionHistory b WHERE b.coreLicenseApplication.id = :applicationId AND b.paymentStatus = 'COMPLETED' ORDER BY b.completedDate DESC")
    List<BomaPayTransactionHistory> findSuccessfulPaymentsByApplicationId(@Param("applicationId") String applicationId);
    
    @Query("SELECT COUNT(b) FROM BomaPayTransactionHistory b WHERE b.coreLicenseApplication.id = :applicationId AND b.paymentStatus = 'COMPLETED'")
    long countSuccessfulPaymentsByApplicationId(@Param("applicationId") String applicationId);
}