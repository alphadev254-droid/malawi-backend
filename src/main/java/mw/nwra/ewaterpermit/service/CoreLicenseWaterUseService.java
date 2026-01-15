package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreLicenseWaterUse;
import mw.nwra.ewaterpermit.model.CoreWaterUse;

public interface CoreLicenseWaterUseService {
	public List<CoreLicenseWaterUse> getAllCoreLicenseWaterUses();

	public List<CoreLicenseWaterUse> getAllCoreLicenseWaterUses(int page, int limit);

	public CoreLicenseWaterUse getCoreLicenseWaterUseById(String id);

	public CoreLicenseWaterUse getCoreLicenseWaterUseByCoreWaterUse(CoreWaterUse WaterUse);

	public void deleteCoreLicenseWaterUse(String id);

	public CoreLicenseWaterUse addCoreLicenseWaterUse(CoreLicenseWaterUse coreTraditionalAuthority);

	public CoreLicenseWaterUse editCoreLicenseWaterUse(CoreLicenseWaterUse coreTraditionalAuthority);

	public List<Object[]> getLargestRevenueLicences(int limit);
}
