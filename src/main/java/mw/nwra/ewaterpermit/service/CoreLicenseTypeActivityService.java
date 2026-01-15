package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreLicenseTypeActivity;

public interface CoreLicenseTypeActivityService {
	public List<CoreLicenseTypeActivity> getAllCoreLicenseTypeActivities();

	public List<CoreLicenseTypeActivity> getAllCoreLicenseTypeActivities(int page, int limit);

	public CoreLicenseTypeActivity getCoreLicenseTypeActivityById(String id);

	public CoreLicenseTypeActivity getCoreLicenseTypeActivityByName(String name);

	public void deleteCoreLicenseTypeActivity(String id);

	public CoreLicenseTypeActivity addCoreLicenseTypeActivity(CoreLicenseTypeActivity coreLicenseType);

	public CoreLicenseTypeActivity editCoreLicenseTypeActivity(CoreLicenseTypeActivity coreLicenseType);

}
