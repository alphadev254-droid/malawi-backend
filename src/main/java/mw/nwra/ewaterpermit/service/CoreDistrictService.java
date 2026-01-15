package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreDistrict;

public interface CoreDistrictService {
	public List<CoreDistrict> getAllCoreDistricts();

	public List<CoreDistrict> getAllCoreDistricts(int page, int limit);

	CoreDistrict getCoreDistrictById(String coreDistrictId);

	void deleteCoreDistrict(String coreDistrictId);

	CoreDistrict addCoreDistrict(CoreDistrict coreDistrict);

	CoreDistrict editCoreDistrict(CoreDistrict coreDistrict);
}
