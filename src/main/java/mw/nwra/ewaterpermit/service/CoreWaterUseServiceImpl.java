package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreWaterUse;
import mw.nwra.ewaterpermit.repository.CoreWaterUseRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreWaterSourceType")
public class CoreWaterUseServiceImpl implements CoreWaterUseService {

	@Autowired
	CoreWaterUseRepository repo;

	@Override
	public List<CoreWaterUse> getAllCoreWaterUses() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreWaterUse> getAllCoreWaterUses(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreWaterUse getCoreWaterUseById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreWaterUse getCoreWaterUseByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreWaterUse(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreWaterUse addCoreWaterUse(CoreWaterUse coreWaterUse) {
		return this.repo.saveAndFlush(coreWaterUse);
	}

	@Override
	public CoreWaterUse editCoreWaterUse(CoreWaterUse coreWaterUse) {
		return this.repo.saveAndFlush(coreWaterUse);
	}

}
