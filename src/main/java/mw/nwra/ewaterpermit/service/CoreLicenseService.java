package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.dto.LicenseDataDto;
import mw.nwra.ewaterpermit.dto.LicenseListDto;
import mw.nwra.ewaterpermit.dto.LicenseTransferInfoDto;
import mw.nwra.ewaterpermit.model.CoreLicense;

public interface CoreLicenseService {
	public List<CoreLicense> getAllCoreLicenses();

	public List<CoreLicense> getAllCoreLicenses(int page, int limit);

	public CoreLicense getCoreLicenseById(String id);

	public List<CoreLicense> getCoreLicensesByLicenseNumber(String number);

	public List<CoreLicense> getActiveLicensesByLicenseNumber(String number);

	public void deleteCoreLicense(String id);

	public CoreLicense addCoreLicense(CoreLicense coreLicense);

	public CoreLicense editCoreLicense(CoreLicense coreLicense);
	
	// Dashboard methods
	public Long count();
	public Long countByStatus(String status);
	public Long countLicensesExpiringInDays(int days);
	
	// Expiry methods
	public List<CoreLicense> getActiveLicensesByUserId(String userId);
	public List<CoreLicense> getExpiringLicensesByUserId(String userId, int daysAhead);
	public List<CoreLicense> getExpiredLicensesByUserId(String userId);
	public Long countActiveLicensesByUserId(String userId);
	
	// Optimized methods for my-licenses API
	public List<LicenseListDto> getAllLicensesForManager();
	public List<LicenseListDto> getActiveLicensesByOwnerId(String ownerId);

	// Optimized methods with license type filtering
	public List<LicenseListDto> getAllLicensesForManagerByLicenseType(String licenseTypeId);
	public List<LicenseListDto> getActiveLicensesByOwnerIdAndLicenseType(String ownerId, String licenseTypeId);
	
	// Optimized method for license data display
	public LicenseDataDto getLicenseDataById(String licenseId);
	
	// Method to get transfer information for a transferred license
	public LicenseTransferInfoDto getLicenseTransferInfo(String originalLicenseId);

	// Method to find original license type ID for renewal types
	public String findOriginalLicenseTypeIdForRenewal(String renewalTypeName);

	// Method to find active licenses that have expired by a given date (for scheduler)
	public List<CoreLicense> findActiveLicensesExpiredByDate(java.sql.Date expiryDate);

	// Methods to find licenses needing notifications at different stages
	public List<CoreLicense> findLicensesExpiringIn3Months();
	public List<CoreLicense> findLicensesExpiringIn2Months();
	public List<CoreLicense> findLicensesExpiringIn1Month();
	public List<CoreLicense> findLicensesExpiringIn1Week();
}
