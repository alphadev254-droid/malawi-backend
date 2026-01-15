package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.CoreLicenseApplicationActivity;
import mw.nwra.ewaterpermit.repository.CoreLicenseApplicationActivityRepository;

@Service(value = "coreLicenseApplicationActivityService")
public class CoreLicenseApplicationActivityServiceImpl implements CoreLicenseApplicationActivityService {
	@Autowired
	CoreLicenseApplicationActivityRepository repo;

	@Override
	public List<CoreLicenseApplicationActivity> getAllCoreLicenseApplicationActivities() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreLicenseApplicationActivity> getAllCoreLicenseApplicationActivities(int page, int limit) {
		return null;
	}

	@Override
	public CoreLicenseApplicationActivity getCoreLicenseApplicationActivityById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreLicenseApplicationActivity getCoreLicenseApplicationActivityByCoreLicenseApplication(
			CoreLicenseApplication application) {
		return this.repo.findByCoreLicenseApplication(application);
	}

	@Override
	public void deleteCoreLicenseApplicationActivity(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLicenseApplicationActivity addCoreLicenseApplicationActivity(
			CoreLicenseApplicationActivity coreLicenseApplicationActivity) {
		return this.repo.saveAndFlush(coreLicenseApplicationActivity);
	}

	@Override
	public CoreLicenseApplicationActivity editCoreLicenseApplicationActivity(
			CoreLicenseApplicationActivity coreLicenseApplicationActivity) {
		return this.repo.saveAndFlush(coreLicenseApplicationActivity);
	}
}
