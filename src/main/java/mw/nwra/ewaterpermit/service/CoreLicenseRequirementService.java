package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreLicenseRequirement;
import mw.nwra.ewaterpermit.model.CoreLicenseType;

public interface CoreLicenseRequirementService {
	public List<CoreLicenseRequirement> getAllCoreLicenseRequirements();

	public List<CoreLicenseRequirement> getAllCoreLicenseRequirements(int page, int limit);

	public CoreLicenseRequirement getCoreLicenseRequirementById(String id);

	public List<CoreLicenseRequirement> getCoreLicenseRequirementByCoreLicenseType(CoreLicenseType type);

	public void deleteCoreLicenseRequirement(String id);

	public CoreLicenseRequirement addCoreLicenseRequirement(CoreLicenseRequirement coreLicense);

	public CoreLicenseRequirement editCoreLicenseRequirement(CoreLicenseRequirement coreLicense);
}
