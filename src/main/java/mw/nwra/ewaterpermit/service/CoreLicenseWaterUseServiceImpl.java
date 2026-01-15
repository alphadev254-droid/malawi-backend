package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreLicenseWaterUse;
import mw.nwra.ewaterpermit.model.CoreWaterUse;
import mw.nwra.ewaterpermit.repository.CoreLicenseWaterUseRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreLicenseWaterUseService")
public class CoreLicenseWaterUseServiceImpl implements CoreLicenseWaterUseService {
	@Autowired
	CoreLicenseWaterUseRepository repo;

	@Override
	public List<CoreLicenseWaterUse> getAllCoreLicenseWaterUses() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreLicenseWaterUse> getAllCoreLicenseWaterUses(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreLicenseWaterUse getCoreLicenseWaterUseById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreLicenseWaterUse getCoreLicenseWaterUseByCoreWaterUse(CoreWaterUse waterUse) {
		return this.repo.findByCoreWaterUse(waterUse);
	}

	@Override
	public void deleteCoreLicenseWaterUse(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLicenseWaterUse addCoreLicenseWaterUse(CoreLicenseWaterUse coreLicenseWaterUse) {
		return this.repo.saveAndFlush(coreLicenseWaterUse);
	}

	@Override
	public CoreLicenseWaterUse editCoreLicenseWaterUse(CoreLicenseWaterUse coreLicenseWaterUse) {
		return this.repo.saveAndFlush(coreLicenseWaterUse);
	}

	@Override
	public List<Object[]> getLargestRevenueLicences(int limit) {
		return this.repo.getLargestRevenueLicences(limit);
	}
}
