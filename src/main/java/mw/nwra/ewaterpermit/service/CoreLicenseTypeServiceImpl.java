package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.repository.CoreLicenseTypeRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreLicenseTypeService")
public class CoreLicenseTypeServiceImpl implements CoreLicenseTypeService {
	@Autowired
	CoreLicenseTypeRepository repo;

	@Override
	public List<CoreLicenseType> getAllCoreLicenseTypes() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreLicenseType> getAllCoreLicenseTypes(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreLicenseType getCoreLicenseTypeById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreLicenseType getCoreLicenseTypeByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreLicenseType(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLicenseType addCoreLicenseType(CoreLicenseType coreLicenseType) {
		return this.repo.saveAndFlush(coreLicenseType);
	}

	@Override
	public CoreLicenseType editCoreLicenseType(CoreLicenseType coreLicenseType) {
		return this.repo.saveAndFlush(coreLicenseType);
	}
}
