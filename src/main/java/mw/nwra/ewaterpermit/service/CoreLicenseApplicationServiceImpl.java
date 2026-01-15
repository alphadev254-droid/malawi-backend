package mw.nwra.ewaterpermit.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.repository.CoreLicenseApplicationRepository;
import mw.nwra.ewaterpermit.repository.SysUserAccountRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreLicenseApplicationService")
public class CoreLicenseApplicationServiceImpl implements CoreLicenseApplicationService {
	@Autowired
	CoreLicenseApplicationRepository repo;
	
	@Autowired
	SysUserAccountRepository userAccountRepository;

	@Override
	public List<CoreLicenseApplication> getAllCoreLicenseApplications() {
		return this.repo.findAllWithPayments();
	}

	@Override
	public List<CoreLicenseApplication> getAllCoreLicenseApplications(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}
	
	@Override
	public List<CoreLicenseApplication> getAllCoreLicenseApplicationsWithPayments(int page, int limit) {
		List<Object[]> results = this.repo.findAllWithPayments(PageRequest.of(page, limit));
		return results.stream().map(row -> {
			CoreLicenseApplication app = new CoreLicenseApplication();
			app.setId((String) row[0]);
			app.setApplicationType((String) row[1]);
			app.setDateSubmitted((java.sql.Timestamp) row[2]);
			app.setDateCreated((java.sql.Timestamp) row[3]);
			app.setApplicationMetadata((String) row[9]);
			app.setClientInfo((String) row[10]);
			app.setLocationInfo((String) row[11]);
			
			// Initialize collections and create payment object if payment data exists
			java.util.List<mw.nwra.ewaterpermit.model.CoreApplicationPayment> payments = new java.util.ArrayList<>();
			
			if (row[7] != null) {
				mw.nwra.ewaterpermit.model.CoreApplicationPayment payment = new mw.nwra.ewaterpermit.model.CoreApplicationPayment();
				payment.setPaymentStatus((String) row[7]);
				payment.setCoreLicenseApplication(app);
				
				// Create minimal fee type if available
				if (row[8] != null) {
					mw.nwra.ewaterpermit.model.CoreFeesType feeType = new mw.nwra.ewaterpermit.model.CoreFeesType();
					feeType.setName((String) row[8]);
					payment.setCoreFeesType(feeType);
				}
				
				payments.add(payment);
			}
			
			app.setCoreApplicationPayments(payments);
			app.setCoreApplicationDocuments(new java.util.ArrayList<>());
			app.setCoreLicenses(new java.util.ArrayList<>());
			app.setCoreLicenseWaterUses(new java.util.ArrayList<>());
			
			// Create minimal status object
			if (row[4] != null) {
				mw.nwra.ewaterpermit.model.CoreApplicationStatus status = new mw.nwra.ewaterpermit.model.CoreApplicationStatus();
				status.setName((String) row[4]);
				app.setCoreApplicationStatus(status);
			}
			
			// Create minimal license type object
			if (row[5] != null) {
				mw.nwra.ewaterpermit.model.CoreLicenseType licenseType = new mw.nwra.ewaterpermit.model.CoreLicenseType();
				licenseType.setName((String) row[5]);
				licenseType.setId((String) row[6]);
				app.setCoreLicenseType(licenseType);
			}
			
			// Create minimal step object
			if (row[12] != null) {
				mw.nwra.ewaterpermit.model.CoreApplicationStep step = new mw.nwra.ewaterpermit.model.CoreApplicationStep();
				step.setId((String) row[12]);
				step.setName((String) row[13]);
				if (row[14] != null) {
					step.setSequenceNumber((Byte) row[14]);
				}
				app.setCoreApplicationStep(step);
			}
			
			return app;
		}).collect(java.util.stream.Collectors.toList());
	}
	
	@Override
	public List<CoreLicenseApplication> getAllCoreLicenseApplicationsWithPaymentsByRole(String userRole, int page, int limit, String dateFrom, String dateTo, String sortBy, String sortDirection) {
		PageRequest pageRequest = PageRequest.of(page, limit);
		List<Object[]> results;
		if (dateFrom != null || dateTo != null) {
			results = this.repo.findAllWithPaymentsByRoleAndDateRange(userRole.toLowerCase().trim(), dateFrom, dateTo, pageRequest);
		} else {
			results = this.repo.findAllWithPaymentsByRole(userRole.toLowerCase().trim(), pageRequest);
		}
		// List<Object[]> results = this.repo.findAllWithPaymentsByRole(userRole.toLowerCase().trim(), PageRequest.of(page, limit));
		return results.stream().map(row -> {
			CoreLicenseApplication app = new CoreLicenseApplication();
			app.setId((String) row[0]);
			app.setApplicationType((String) row[1]);
			app.setDateSubmitted((java.sql.Timestamp) row[2]);
			app.setDateCreated((java.sql.Timestamp) row[3]);
			app.setApplicationMetadata((String) row[9]);
			app.setClientInfo((String) row[10]);
			app.setLocationInfo((String) row[11]);
			
			// Initialize collections and create payment object if payment data exists
			java.util.List<mw.nwra.ewaterpermit.model.CoreApplicationPayment> payments = new java.util.ArrayList<>();
			
			// Create payment object if ANY payment data exists
			if (row[7] != null || row[20] != null || row[21] != null) { // payment_status OR amount_paid OR payment_id exists
				mw.nwra.ewaterpermit.model.CoreApplicationPayment payment = new mw.nwra.ewaterpermit.model.CoreApplicationPayment();
				
				// Set payment status 
				payment.setPaymentStatus(row[7] != null ? (String) row[7] : "PENDING");
				payment.setCoreLicenseApplication(app);
				
				// Set payment amount - this was missing!
				if (row[20] != null) {
					payment.setAmountPaid(((Number) row[20]).doubleValue());
				} else {
					// Set default amount from license type if no payment amount
					payment.setAmountPaid(0.0);
				}
				
				// Set payment ID if available  
				if (row[21] != null) {
					payment.setId((String) row[21]);
				}
				
				// Set payment method if available
				if (row[22] != null) {
					payment.setPaymentMethod((String) row[22]);
				}
				
				// Set payment date if available
				if (row[23] != null) {
					payment.setDateCreated((java.sql.Timestamp) row[23]);
				}
				
				// Create minimal fee type if available
				if (row[8] != null) {
					mw.nwra.ewaterpermit.model.CoreFeesType feeType = new mw.nwra.ewaterpermit.model.CoreFeesType();
					feeType.setName((String) row[8]);
					payment.setCoreFeesType(feeType);
				}
				
				payments.add(payment);
			}
			
			app.setCoreApplicationPayments(payments);
			app.setCoreApplicationDocuments(new java.util.ArrayList<>());
			app.setCoreLicenses(new java.util.ArrayList<>());
			app.setCoreLicenseWaterUses(new java.util.ArrayList<>());
			
			// Create minimal status object
			if (row[4] != null) {
				mw.nwra.ewaterpermit.model.CoreApplicationStatus status = new mw.nwra.ewaterpermit.model.CoreApplicationStatus();
				status.setName((String) row[4]);
				app.setCoreApplicationStatus(status);
			}
			
			// Create minimal license type object
			if (row[5] != null) {
				mw.nwra.ewaterpermit.model.CoreLicenseType licenseType = new mw.nwra.ewaterpermit.model.CoreLicenseType();
				licenseType.setName((String) row[5]);
				licenseType.setId((String) row[6]);
				app.setCoreLicenseType(licenseType);
			}
			
			// Create minimal step object
			if (row[12] != null) {
				mw.nwra.ewaterpermit.model.CoreApplicationStep step = new mw.nwra.ewaterpermit.model.CoreApplicationStep();
				step.setId((String) row[12]);
				step.setName((String) row[13]);
				if (row[14] != null) {
					step.setSequenceNumber((Byte) row[14]);
				}
				app.setCoreApplicationStep(step);
			}
			
			// Create minimal user account object
			if (row[15] != null) {
				mw.nwra.ewaterpermit.model.SysUserAccount userAccount = new mw.nwra.ewaterpermit.model.SysUserAccount();
				userAccount.setId((String) row[15]);
				userAccount.setFirstName((String) row[16]);
				userAccount.setLastName((String) row[17]);
				userAccount.setUsername((String) row[18]);
				userAccount.setEmailAddress((String) row[19]);
				app.setSysUserAccount(userAccount);
			}

			// Set emergency application fields (indices 24-27)
			if (row.length > 24) {
				app.setApplicationPriority((String) row[24]);
				app.setEmergencyReason((String) row[25]);
				app.setEmergencyJustificationFile((String) row[26]);
				app.setEmergencySubmittedDate((java.sql.Timestamp) row[27]);
			}

			// Set license fee (index 28)
			if (row.length > 28 && row[28] != null) {
				app.setLicenseFee(((Number) row[28]).doubleValue());
			}

			// Store assessment data in application metadata for controller access
			if (row.length > 29) {
				java.util.Map<String, Object> assessmentData = new java.util.HashMap<>();
				if (row[29] != null) assessmentData.put("calculatedAnnualRental", ((Number) row[29]).doubleValue());
				if (row[30] != null) assessmentData.put("rentalQuantity", ((Number) row[30]).doubleValue());
				if (row[31] != null) assessmentData.put("rentalRate", ((Number) row[31]).doubleValue());
				if (row[32] != null) assessmentData.put("recommendedScheduleDate", row[32]);
				if (row[33] != null) assessmentData.put("assessmentNotes", row[33]);
				if (row[34] != null) assessmentData.put("licenseOfficerId", row[34]);
				if (row[35] != null) assessmentData.put("assessmentStatus", row[35]);
				if (row[36] != null) assessmentData.put("assessmentFilesUpload", row[36]);
				
				// Store in a temporary field for controller access
				try {
					String assessmentJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(assessmentData);
					app.setFormSpecificData(assessmentJson); // Temporarily store assessment data
				} catch (Exception e) {
					// Ignore JSON serialization errors
				}
			}

			return app;
		}).collect(java.util.stream.Collectors.toList());
	}

	@Override
	public CoreLicenseApplication getCoreLicenseApplicationById(String id) {
		return this.repo.findByIdWithPayments(id).orElse(null);
	}

	@Override
	public Map<String, Object> getApplicationByLicenseId(String licenseId) {
		try {
			System.out.println("Finding application for license ID: " + licenseId);

			// Query to get the application associated with this license
			List<Object[]> results = this.repo.findApplicationByLicenseId(licenseId);

			if (results == null || results.isEmpty()) {
				System.out.println("No application found for license ID: " + licenseId);
				return null;
			}

			Object[] result = results.get(0);
			System.out.println("Found application: " + java.util.Arrays.toString(result));

			// For now, return the application ID so we can use the existing endpoint
			Map<String, Object> response = new java.util.HashMap<>();
			response.put("id", result[0]);
			response.put("licenseId", licenseId);

			// Get the full application data using existing method
			CoreLicenseApplication fullApplication = getCoreLicenseApplicationById((String) result[0]);
			if (fullApplication != null) {
				// Convert to Map format similar to controller response
				response.put("application", fullApplication);
			}

			return response;

		} catch (Exception e) {
			System.out.println("Error finding application by license ID: " + e.getMessage());
			return null;
		}
	}

	@Override
	public CoreLicenseApplication getCoreLicenseApplicationByOldLicense(CoreLicense licence) {
		return this.repo.findByCurrentLicenseWithPayments(licence);
	}

	@Override
	public void deleteCoreLicenseApplication(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLicenseApplication addCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		return this.repo.saveAndFlush(coreLicenseApplication);
	}

	@Override
	public CoreLicenseApplication editCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		return this.repo.saveAndFlush(coreLicenseApplication);
	}

	@Override
	public List<CoreLicenseApplication> getCoreLicenseApplicationByApplicant(SysUserAccount applicant) {
		return this.repo.findBySysUserAccountWithPayments(applicant);
	}

	@Override
	public List<Object[]> getMyApplicationsOptimized(String username) {
		return this.repo.findAllByUsernameWithAllAssociations(username);
	}

	@Override
	public Object getApplicationDetails(String id) {
		return this.repo.findApplicationDetailsById(id);
	}

	@Override
	public Long count() {
		return this.repo.count();
	}

	@Override
	public Long countByApplicationStatusName(String statusName) {
		return this.repo.countByApplicationStatusName(statusName);
	}

	@Override
	public List<Object[]> getApplicationsByStatus() {
		return this.repo.getApplicationsByStatus();
	}

	@Override
	public List<Object[]> getApplicationsByLicenseType() {
		return this.repo.getApplicationsByLicenseType();
	}

	@Override
	public List<Object[]> getMonthlyApplicationsCurrentYear() {
		return this.repo.getMonthlyApplicationsCurrentYear();
	}

	// Role-specific dashboard methods
	@Override
	public Long countByUserId(String userId) {
		return this.repo.countByUserId(userId);
	}

	@Override
	public Long countByUserIdAndStatus(String userId, String status) {
		return this.repo.countByUserIdAndStatus(userId, status);
	}

	@Override
	public Long countPendingApplicationsByUserId(String userId) {
		return this.repo.countPendingApplicationsByUserId(userId);
	}

	@Override
	public List<Object[]> getRecentApplicationsByUserId(String userId, int limit) {
		return this.repo.getRecentApplicationsByUserId(userId, PageRequest.of(0, limit));
	}

	@Override
	public List<Object[]> getApplicationStatusDistributionByUserId(String userId) {
		return this.repo.getApplicationStatusDistributionByUserId(userId);
	}

	@Override
	public List<Object[]> getRecentApplicationsByStatus(String status, int limit) {
		return this.repo.getRecentApplicationsByStatus(status, PageRequest.of(0, limit));
	}

	@Override
	public Long countApplicationsThisMonth() {
		return this.repo.countApplicationsThisMonth();
	}

	@Override
	public Long countByStatusAndCurrentMonth(String status) {
		return this.repo.countByStatusAndCurrentMonth(status);
	}

	@Override
	public List<Object[]> getOfficerPerformanceMetrics() {
		return this.repo.getOfficerPerformanceMetrics();
	}

	@Override
	public List<Object[]> getApplicationsByWaterSource() {
		return this.repo.getApplicationsByWaterSource();
	}

	@Override
	public List<Object[]> getApplicationsByRegion() {
		return this.repo.getApplicationsByRegion();
	}

	@Override
	public List<Object[]> getYearlyApplicationTrends() {
		return this.repo.getYearlyApplicationTrends();
	}

	@Override
	public List<Object[]> getHighValueApplications() {
		return this.repo.getHighValueApplications(PageRequest.of(0, 10));
	}

	@Override
	public Long getTotalUsers() {
		return this.userAccountRepository.count();
	}

	@Override
	public Long getActiveUsers() {
		return this.userAccountRepository.countBySysAccountStatus_Name("ACTIVE");
	}

	@Override
	public Long getSystemErrors() {
		// For now, return a placeholder. This would typically be in a SystemService
		return 0L;
	}

	@Override
	public Long getDataIntegrityIssues() {
		// For now, return a placeholder. This would typically be in a SystemService
		return 0L;
	}

	@Override
	public List<Object[]> getUserActivityMetrics() {
		// For now, return empty list. This would typically be in a UserService
		return List.of();
	}

	@Override
	public List<CoreLicenseApplication> getApplicationsByIds(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		// Multi-step eager loading to prevent N+1 queries
		// Step 1: Load applications with status, type, step, water source
		List<CoreLicenseApplication> apps = this.repo.findByIdInWithEagerLoading(ids);

		if (!apps.isEmpty()) {
			// Step 2: Eagerly load user accounts with their nested relationships
			// Hibernate's session cache ensures these attach to already-loaded apps
			this.repo.fetchUserAccounts(ids);

			// Step 3: Eagerly load water resource units with their areas
			this.repo.fetchWaterResourceUnits(ids);
		}

		return apps;
	}

	@Override
	public boolean hasPendingOrApprovedTransfer(String originalLicenseId) {
		return this.repo.existsPendingOrApprovedTransferForLicense(originalLicenseId);
	}

	@Override
	public List<CoreLicenseApplication> getCoreLicenseApplicationsByUser(String userId) {
		SysUserAccount user = userAccountRepository.findById(userId).orElse(null);
		if (user == null) {
			return new java.util.ArrayList<>();
		}
		return this.repo.findBySysUserAccount(user);
	}
}
