package mw.nwra.ewaterpermit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mw.nwra.ewaterpermit.model.CoreInvoice;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoreInvoiceRepository extends JpaRepository<CoreInvoice, String> {

	@Query("SELECT i FROM CoreInvoice i WHERE "
			+ " i.invoiceNumber LIKE %:search% OR "
			+ " i.coreLicenseApplication.destOwnerFullname LIKE %:search% OR "
			+ " i.coreLicenseApplication.sourceOwnerFullname LIKE %:search%")
	Page<CoreInvoice> findAll(@Param("search") String search, Pageable pageable);

	@Query("SELECT i FROM CoreInvoice i WHERE i.coreLicenseApplication.id = :applicationId")
	List<CoreInvoice> findByApplicationId(@Param("applicationId") String applicationId);

	@Query("SELECT i FROM CoreInvoice i WHERE i.coreLicenseApplication.id = :applicationId AND i.invoiceType = :invoiceType")
	Optional<CoreInvoice> findByApplicationIdAndType(@Param("applicationId") String applicationId, @Param("invoiceType") String invoiceType);

	@Query("SELECT i FROM CoreInvoice i WHERE i.invoiceNumber = :invoiceNumber")
	Optional<CoreInvoice> findByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

	@Query("SELECT i FROM CoreInvoice i WHERE i.invoiceStatus = :status")
	List<CoreInvoice> findByStatus(@Param("status") String status);
}