package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreLicenseRequirement;
import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.repository.CoreLicenseRequirementRepository;

@Service(value = "coreLicenseRequirementService")
public class CoreLicenseRequirementServiceImpl implements CoreLicenseRequirementService {
	@Autowired
	CoreLicenseRequirementRepository repo;

	@Override
	public List<CoreLicenseRequirement> getAllCoreLicenseRequirements() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreLicenseRequirement> getAllCoreLicenseRequirements(int page, int limit) {
		return null;
	}

	@Override
	public CoreLicenseRequirement getCoreLicenseRequirementById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public List<CoreLicenseRequirement> getCoreLicenseRequirementByCoreLicenseType(CoreLicenseType type) {
		return this.repo.findByCoreLicenseType(type);
	}

	@Override
	public void deleteCoreLicenseRequirement(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLicenseRequirement addCoreLicenseRequirement(CoreLicenseRequirement coreLicenseRequirement) {
		return this.repo.saveAndFlush(coreLicenseRequirement);
	}

	@Override
	public CoreLicenseRequirement editCoreLicenseRequirement(CoreLicenseRequirement coreLicenseRequirement) {
		return this.repo.saveAndFlush(coreLicenseRequirement);
	}
}
