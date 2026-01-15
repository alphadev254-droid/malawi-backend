package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.CoreLicenseApplicationActivity;

public interface CoreLicenseApplicationActivityService {
	public List<CoreLicenseApplicationActivity> getAllCoreLicenseApplicationActivities();

	public List<CoreLicenseApplicationActivity> getAllCoreLicenseApplicationActivities(int page, int limit);

	public CoreLicenseApplicationActivity getCoreLicenseApplicationActivityById(String id);

	public CoreLicenseApplicationActivity getCoreLicenseApplicationActivityByCoreLicenseApplication(
			CoreLicenseApplication application);

	public void deleteCoreLicenseApplicationActivity(String id);

	public CoreLicenseApplicationActivity addCoreLicenseApplicationActivity(
			CoreLicenseApplicationActivity coreLicenseApplicationActivity);

	public CoreLicenseApplicationActivity editCoreLicenseApplicationActivity(
			CoreLicenseApplicationActivity coreLicenseApplicationActivity);
}
