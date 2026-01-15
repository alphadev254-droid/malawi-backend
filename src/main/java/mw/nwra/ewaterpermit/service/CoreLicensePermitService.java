package mw.nwra.ewaterpermit.service;

import java.util.List;
import java.util.Optional;

import mw.nwra.ewaterpermit.model.CoreLicensePermit;

public interface CoreLicensePermitService {
	List<CoreLicensePermit> getAllCoreLicensePermits();

	List<CoreLicensePermit> getAllCoreLicensePermits(int page, int limit);

	CoreLicensePermit getCoreLicensePermitById(String id);

	void deleteCoreLicensePermit(String id);

	CoreLicensePermit addCoreLicensePermit(CoreLicensePermit coreLicensePermit);

	void editCoreLicensePermit(CoreLicensePermit coreLicensePermit);

	// Additional methods specific to permits
	Optional<CoreLicensePermit> getPermitByApplicationId(String applicationId);
	
	Optional<CoreLicensePermit> getPermitByPermitNumber(String permitNumber);

	// Alias methods for consistency
	default CoreLicensePermit createCoreLicensePermit(CoreLicensePermit coreLicensePermit) {
		return addCoreLicensePermit(coreLicensePermit);
	}

	default List<CoreLicensePermit> getCoreLicensePermits(int page, int limit) {
		return getAllCoreLicensePermits(page, limit);
	}
	
	// Dashboard methods
	Long count();
	List<Object[]> getPermitsByRegion();
	List<Object[]> getPermitsByWaterSourceType();
	List<Object[]> getPermitsByStatus();
	
	// Role-specific dashboard methods
	Long countByUserId(String userId);
}