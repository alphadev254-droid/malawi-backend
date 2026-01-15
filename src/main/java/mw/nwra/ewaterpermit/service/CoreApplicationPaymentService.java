package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreApplicationPayment;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;

public interface CoreApplicationPaymentService {
	List<CoreApplicationPayment> getAllCoreApplicationPayments();

	List<CoreApplicationPayment> getAllCoreApplicationPayments(int page, int limit);

	CoreApplicationPayment getCoreApplicationPaymentById(String id);
	List<CoreApplicationPayment> getByLicence(CoreLicenseApplication licenseApplication);

	void deleteCoreApplicationPayment(String id);

	CoreApplicationPayment addCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment);

	void editCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment);

	// Alias methods for consistency
	default CoreApplicationPayment createCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
		return addCoreApplicationPayment(coreApplicationPayment);
	}

	default List<CoreApplicationPayment> getCoreApplicationPayments(int page, int limit) {
		return getAllCoreApplicationPayments(page, limit);
	}

	/**
	 * Check if license fee payment exists for the given application
	 * @param applicationId The application ID to check
	 * @return true if license fee payment exists, false otherwise
	 */
	boolean hasLicenseFeePayment(String applicationId);

	/**
	 * Find all payments for a given CoreLicense
	 * @param coreLicense The CoreLicense to find payments for
	 * @return List of CoreApplicationPayment for the license
	 */
	List<CoreApplicationPayment> findByCoreLicense(mw.nwra.ewaterpermit.model.CoreLicense coreLicense);

	/**
	 * Find all payments for a specific financial year
	 * @param financialYearId The financial year ID
	 * @return List of CoreApplicationPayment for the financial year
	 */
	List<CoreApplicationPayment> findByFinancialYearId(String financialYearId);

	/**
	 * Approve payment by application ID and fee type using optimized query
	 * @param applicationId The application ID
	 * @param feeTypeName The fee type name (e.g., "Application fee", "License fees")
	 * @return true if payment was found and approved, false otherwise
	 */
	boolean approvePaymentByApplicationAndFeeType(String applicationId, String feeTypeName);

	/**
	 * Get application step sequence number for lightweight operations
	 * @param applicationId The application ID
	 * @return The current step sequence number or null if not found
	 */
	Integer getApplicationStepSequence(String applicationId);

	/**
	 * Update application step using optimized query
	 * @param applicationId The application ID
	 * @param nextStepId The next step ID
	 * @return true if successfully updated, false otherwise
	 */
	boolean updateApplicationStep(String applicationId, String nextStepId);

	/**
	 * Check if license fees are paid for a specific license
	 * @param licenseId The license ID to check
	 * @return true if license fees are paid, false otherwise
	 */
	boolean isLicenseFeePaidForLicense(String licenseId);

	/**
	 * Get all payments with application details (optimized with eager fetching)
	 * @param page Page number
	 * @param limit Page size
	 * @param query Search query
	 * @return SearchResponse containing payments with full application details
	 */
	mw.nwra.ewaterpermit.responseSchema.SearchResponse getAllPaymentsWithDetails(int page, int limit, String query);

	/**
	 * OPTIMIZED: Batch fetch payments for multiple applications in a single query
	 * @param applicationIds List of application IDs
	 * @return Map of applicationId -> List of payments
	 */
	java.util.Map<String, List<CoreApplicationPayment>> getPaymentsByApplicationIds(List<String> applicationIds);
}
