package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreLicenseType;

public interface CoreLicenseTypeService {
	public List<CoreLicenseType> getAllCoreLicenseTypes();

	public List<CoreLicenseType> getAllCoreLicenseTypes(int page, int limit);

	public CoreLicenseType getCoreLicenseTypeById(String id);

	public CoreLicenseType getCoreLicenseTypeByName(String name);

	public void deleteCoreLicenseType(String id);

	public CoreLicenseType addCoreLicenseType(CoreLicenseType coreLicenseType);

	public CoreLicenseType editCoreLicenseType(CoreLicenseType coreLicenseType);
}
