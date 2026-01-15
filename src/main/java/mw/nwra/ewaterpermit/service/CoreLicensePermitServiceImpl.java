package mw.nwra.ewaterpermit.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreLicensePermit;
import mw.nwra.ewaterpermit.repository.CoreLicensePermitRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreLicensePermitService")
public class CoreLicensePermitServiceImpl implements CoreLicensePermitService {
	@Autowired
	CoreLicensePermitRepository repo;

	@Override
	public List<CoreLicensePermit> getAllCoreLicensePermits() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreLicensePermit> getAllCoreLicensePermits(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreLicensePermit getCoreLicensePermitById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public void deleteCoreLicensePermit(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLicensePermit addCoreLicensePermit(CoreLicensePermit coreLicensePermit) {
		return this.repo.saveAndFlush(coreLicensePermit);
	}

	@Override
	public void editCoreLicensePermit(CoreLicensePermit coreLicensePermit) {
        this.repo.saveAndFlush(coreLicensePermit);
    }

	@Override
	public Optional<CoreLicensePermit> getPermitByApplicationId(String applicationId) {
		return this.repo.findByApplicationId(applicationId);
	}

	@Override
	public Optional<CoreLicensePermit> getPermitByPermitNumber(String permitNumber) {
		return this.repo.findByPermitNumber(permitNumber);
	}

	@Override
	public Long count() {
		return this.repo.count();
	}

	@Override
	public List<Object[]> getPermitsByRegion() {
		return this.repo.getPermitsByRegion();
	}

	@Override
	public List<Object[]> getPermitsByWaterSourceType() {
		return this.repo.getPermitsByWaterSourceType();
	}

	@Override
	public List<Object[]> getPermitsByStatus() {
		return this.repo.getPermitsByStatus();
	}

	@Override
	public Long countByUserId(String userId) {
		return this.repo.countByUserId(userId);
	}
}