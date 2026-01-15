package mw.nwra.ewaterpermit.service;

import java.util.List;
import java.util.Map;

import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;

public interface CoreLicenseApplicationService {
	public List<CoreLicenseApplication> getAllCoreLicenseApplications();

	public List<CoreLicenseApplication> getAllCoreLicenseApplications(int page, int limit);
	
	public List<CoreLicenseApplication> getAllCoreLicenseApplicationsWithPayments(int page, int limit);
	
	public List<CoreLicenseApplication> getAllCoreLicenseApplicationsWithPaymentsByRole(String userRole, int page, int limit, String dateFrom, String dateTo, String sortBy, String sortDirection);
	

	public CoreLicenseApplication getCoreLicenseApplicationById(String id);

	public Map<String, Object> getApplicationByLicenseId(String licenseId);

	public CoreLicenseApplication getCoreLicenseApplicationByOldLicense(CoreLicense licence);

	public void deleteCoreLicenseApplication(String id);

	public CoreLicenseApplication addCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication);

	public CoreLicenseApplication editCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication);

	public List<CoreLicenseApplication> getCoreLicenseApplicationByApplicant(SysUserAccount applicant);
	
	// Dashboard methods
	public Long count();
	public Long countByApplicationStatusName(String statusName);
	public List<Object[]> getApplicationsByStatus();
	public List<Object[]> getApplicationsByLicenseType();
	public List<Object[]> getMonthlyApplicationsCurrentYear();
	
	// Role-specific dashboard methods
	// Applicant methods
	public Long countByUserId(String userId);
	public Long countByUserIdAndStatus(String userId, String status);
	public Long countPendingApplicationsByUserId(String userId);
	public List<Object[]> getRecentApplicationsByUserId(String userId, int limit);
	public List<Object[]> getApplicationStatusDistributionByUserId(String userId);
	
	// Officer/Manager methods
	public List<Object[]> getRecentApplicationsByStatus(String status, int limit);
	public Long countApplicationsThisMonth();
	public Long countByStatusAndCurrentMonth(String status);
	public List<Object[]> getOfficerPerformanceMetrics();
	
	// DRS methods
	public List<Object[]> getApplicationsByWaterSource();
	public List<Object[]> getApplicationsByRegion();
	
	// CEO methods
	public List<Object[]> getYearlyApplicationTrends();
	public List<Object[]> getHighValueApplications();
	
	// Admin methods
	public Long getTotalUsers();
	public Long getActiveUsers();
	public Long getSystemErrors();
	public Long getDataIntegrityIssues();
	public List<Object[]> getUserActivityMetrics();
	
	// Optimized method for my-applications endpoint
	public List<Object[]> getMyApplicationsOptimized(String username);
	
	// Get application details for dialog
	public Object getApplicationDetails(String id);

	// OPTIMIZED: Batch fetch applications by IDs with eager loading
	public List<CoreLicenseApplication> getApplicationsByIds(List<String> ids);

	// Check if a pending or approved transfer exists for a license
	public boolean hasPendingOrApprovedTransfer(String originalLicenseId);

	// Get applications by user ID
	public List<CoreLicenseApplication> getCoreLicenseApplicationsByUser(String userId);
}
