package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreLicenseTypeActivity;
import mw.nwra.ewaterpermit.repository.CoreLicenseTypeActivityRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreLicenseTypeActivityService")
public class CoreLicenseTypeActivityServiceImpl implements CoreLicenseTypeActivityService {
	@Autowired
	CoreLicenseTypeActivityRepository repo;

	@Override
	public List<CoreLicenseTypeActivity> getAllCoreLicenseTypeActivities() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreLicenseTypeActivity> getAllCoreLicenseTypeActivities(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreLicenseTypeActivity getCoreLicenseTypeActivityById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreLicenseTypeActivity getCoreLicenseTypeActivityByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreLicenseTypeActivity(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLicenseTypeActivity addCoreLicenseTypeActivity(CoreLicenseTypeActivity coreLicenseTypeActivity) {
		return this.repo.saveAndFlush(coreLicenseTypeActivity);
	}

	@Override
	public CoreLicenseTypeActivity editCoreLicenseTypeActivity(CoreLicenseTypeActivity coreLicenseTypeActivity) {
		return this.repo.saveAndFlush(coreLicenseTypeActivity);
	}
}
