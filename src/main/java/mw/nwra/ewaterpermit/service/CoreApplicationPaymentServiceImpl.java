package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreApplicationPayment;
import mw.nwra.ewaterpermit.model.CoreFinancialYear;
import mw.nwra.ewaterpermit.repository.CoreApplicationPaymentRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreApplicationPaymentService")
public class CoreApplicationPaymentServiceImpl implements CoreApplicationPaymentService {
	private static final Logger log = LoggerFactory.getLogger(CoreApplicationPaymentServiceImpl.class);
	
	@Autowired
	CoreApplicationPaymentRepository repo;
	
	@Autowired
	CoreFinancialYearService financialYearService;

	@Override
	public List<CoreApplicationPayment> getAllCoreApplicationPayments() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreApplicationPayment> getAllCoreApplicationPayments(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreApplicationPayment getCoreApplicationPaymentById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public List<CoreApplicationPayment> getByLicence(CoreLicenseApplication licenseApplication) {
		return this.repo.findByCoreLicenseApplicationId(licenseApplication.getId());
	}

	public List<CoreApplicationPayment> getApplicationFeePayments(String applicationId) {
		log.info("=== SERVICE: Getting application fee payments for applicationId: {}", applicationId);
		log.info("SERVICE: Calling repository.findApplicationFeePayments...");
		List<CoreApplicationPayment> payments = this.repo.findApplicationFeePayments(applicationId);
		log.info("SERVICE: Repository returned {} payments", payments != null ? payments.size() : 0);
		log.info("=== SERVICE: Application fee payments fetch completed ===");
		return payments;
	}

	public List<CoreApplicationPayment> getLicenseFeePayments(String applicationId) {
		return this.repo.findLicenseFeePayments(applicationId);
	}

	@Override
	public void deleteCoreApplicationPayment(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreApplicationPayment addCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
		// Set the active financial year when adding payment
		CoreFinancialYear activeFinancialYear = financialYearService.getActiveFinancialYear();
		if (activeFinancialYear != null) {
			coreApplicationPayment.setCoreFinancialYear(activeFinancialYear);
		}
		return this.repo.saveAndFlush(coreApplicationPayment);
	}

	@Override
	public void editCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
		// Set the active financial year when editing payment (if not already set)
		if (coreApplicationPayment.getCoreFinancialYear() == null) {
			CoreFinancialYear activeFinancialYear = financialYearService.getActiveFinancialYear();
			if (activeFinancialYear != null) {
				coreApplicationPayment.setCoreFinancialYear(activeFinancialYear);
			}
		}
        this.repo.saveAndFlush(coreApplicationPayment);
    }

	@Override
	public boolean hasLicenseFeePayment(String applicationId) {
		try {
			// Only check for manual receipt uploads that need accountant verification
			// MoMoPay payments are automatically verified, so they don't need this check
			List<CoreApplicationPayment> payments = repo.findByCoreLicenseApplicationId(applicationId);
			
			// Look for payments that have receipt documents (manual uploads) and need verification
			return payments.stream().anyMatch(payment -> 
				payment.getReceiptDocumentId() != null && 
				!payment.getReceiptDocumentId().isEmpty() &&
				payment.isNeedsVerification() &&
				payment.getAmountPaid() > 0 &&
				!"VERIFIED".equals(payment.getPaymentStatus())
			);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public List<CoreApplicationPayment> findByCoreLicense(mw.nwra.ewaterpermit.model.CoreLicense coreLicense) {
		return repo.findByCoreLicense(coreLicense);
	}

	@Override
	public List<CoreApplicationPayment> findByFinancialYearId(String financialYearId) {
		return repo.findByFinancialYearId(financialYearId);
	}

	@Override
	public boolean approvePaymentByApplicationAndFeeType(String applicationId, String feeTypeName) {
		try {
			log.info("Approving payment using optimized query - App: {}, Fee: {}", applicationId, feeTypeName);
			int rowsUpdated = repo.approvePaymentByApplicationAndFeeType(applicationId, feeTypeName);
			log.info("Payment approval rows updated: {}", rowsUpdated);
			
			// Also update payment receipt document status
			int documentsUpdated = repo.updatePaymentReceiptStatus(applicationId);
			log.info("Payment receipt document rows updated: {}", documentsUpdated);
			
			return rowsUpdated > 0;
		} catch (Exception e) {
			log.error("Error approving payment for app {} and fee {}: {}", applicationId, feeTypeName, e.getMessage());
			return false;
		}
	}

	@Override
	public Integer getApplicationStepSequence(String applicationId) {
		try {
			return repo.getApplicationStepSequence(applicationId);
		} catch (Exception e) {
			log.error("Error getting application step sequence for app {}: {}", applicationId, e.getMessage());
			return null;
		}
	}

	@Override
	public boolean updateApplicationStep(String applicationId, String nextStepId) {
		try {
			log.info("Updating application step using optimized query - App: {}, Next Step: {}", applicationId, nextStepId);
			int rowsUpdated = repo.updateApplicationStep(applicationId, nextStepId);
			log.info("Application step update rows affected: {}", rowsUpdated);
			return rowsUpdated > 0;
		} catch (Exception e) {
			log.error("Error updating application step for app {} to step {}: {}", applicationId, nextStepId, e.getMessage());
			return false;
		}
	}

	@Override
	public boolean isLicenseFeePaidForLicense(String licenseId) {
		try {
			log.info("Checking if license fees are paid for license: {}", licenseId);
			
			// Add null/invalid ID validation
			if (licenseId == null || licenseId.trim().isEmpty() || "null".equalsIgnoreCase(licenseId) || "NaN".equalsIgnoreCase(licenseId)) {
				log.warn("Invalid license ID provided: {}", licenseId);
				return false;
			}
			
			boolean isPaid = repo.isLicenseFeePaidForLicense(licenseId);
			log.info("License {} fee payment status: {}", licenseId, isPaid ? "PAID" : "NOT PAID");
			return isPaid;
		} catch (Exception e) {
			log.error("Error checking license fee payment status for license {}: {}", licenseId, e.getMessage());
			return false;
		}
	}

	@Override
	public mw.nwra.ewaterpermit.responseSchema.SearchResponse getAllPaymentsWithDetails(int page, int limit, String query) {
		org.springframework.data.domain.Page<CoreApplicationPayment> res = this.repo.findAllPaymentsWithDetails(query,
				AppUtil.getPageRequest(page, limit, "dateCreated", "desc"));
		return res.getContent().isEmpty() ? null : new mw.nwra.ewaterpermit.responseSchema.SearchResponse(res.getTotalElements(), res.getContent());
	}

	@Override
	public java.util.Map<String, java.util.List<CoreApplicationPayment>> getPaymentsByApplicationIds(java.util.List<String> applicationIds) {
		if (applicationIds == null || applicationIds.isEmpty()) {
			return java.util.Collections.emptyMap();
		}

		log.info("Batch fetching payments for {} applications", applicationIds.size());

		// Fetch all payments for the given application IDs in a single query
		java.util.List<CoreApplicationPayment> allPayments = repo.findByLicenseApplicationIdIn(applicationIds);

		// Group payments by application ID
		return allPayments.stream()
				.collect(java.util.stream.Collectors.groupingBy(
						payment -> payment.getCoreLicenseApplication().getId()
				));
	}
}
