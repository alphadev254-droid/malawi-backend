package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreWaterSourceType;
import mw.nwra.ewaterpermit.repository.CoreWaterSourceTypeRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreWaterSourceTypeService")
public class CoreWaterSourceTypeServiceImpl implements CoreWaterSourceTypeService {
	@Autowired
	CoreWaterSourceTypeRepository repo;

	@Override
	public List<CoreWaterSourceType> getAllCoreWaterSourceTypes() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreWaterSourceType> getAllCoreWaterSourceTypes(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreWaterSourceType getCoreWaterSourceTypeById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreWaterSourceType getCoreWaterSourceTypeByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreWaterSourceType(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreWaterSourceType addCoreWaterSourceType(CoreWaterSourceType coreWaterSourceType) {
		return this.repo.saveAndFlush(coreWaterSourceType);
	}

	@Override
	public CoreWaterSourceType editCoreWaterSourceType(CoreWaterSourceType coreWaterSourceType) {
		return this.repo.saveAndFlush(coreWaterSourceType);
	}
}
