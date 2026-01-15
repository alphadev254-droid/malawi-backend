package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreDistrict;
import mw.nwra.ewaterpermit.repository.CoreDistrictRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreDistrictService")
public class CoreDistrictServiceImpl implements CoreDistrictService {

	@Autowired
	private CoreDistrictRepository coreDistrictRepository;

	@Override
	public List<CoreDistrict> getAllCoreDistricts() {
		return this.coreDistrictRepository.findAll();
	}

	@Override
	public List<CoreDistrict> getAllCoreDistricts(int page, int limit) {
		return this.coreDistrictRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "asc")).getContent();
	}

	@Override
	public CoreDistrict getCoreDistrictById(String coreDistrictId) {
		return this.coreDistrictRepository.findById(coreDistrictId).orElse(null);
	}

	@Override
	public void deleteCoreDistrict(String coreDistrictId) {
		this.coreDistrictRepository.deleteById(coreDistrictId);
	}

	@Override
	public CoreDistrict addCoreDistrict(CoreDistrict coreDistrict) {
		return this.coreDistrictRepository.saveAndFlush(coreDistrict);
	}

	@Override
	public CoreDistrict editCoreDistrict(CoreDistrict coreDistrict) {
		return this.coreDistrictRepository.saveAndFlush(coreDistrict);
	}

}
