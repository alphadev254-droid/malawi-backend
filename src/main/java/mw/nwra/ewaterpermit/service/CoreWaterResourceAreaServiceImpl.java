package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreWaterResourceArea;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceAreaRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreWaterResourceAreaService")
public class CoreWaterResourceAreaServiceImpl implements CoreWaterResourceAreaService {
	@Autowired
	CoreWaterResourceAreaRepository repo;

	@Override
	public List<CoreWaterResourceArea> getAllCoreWaterResourceAreas() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreWaterResourceArea> getAllCoreWaterResourceAreas(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreWaterResourceArea getCoreWaterResourceAreaById(String id) {
		return this.repo.findById(id).orElse(null);
	}

//	@Override
//	public CoreWaterResourceArea getCoreWaterResourceAreaByName(String name) {
//		return this.repo.findByName(name);
//	}

	@Override
	public void deleteCoreWaterResourceArea(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreWaterResourceArea addCoreWaterResourceArea(CoreWaterResourceArea coreWaterResourceArea) {
		return this.repo.saveAndFlush(coreWaterResourceArea);
	}

	@Override
	public CoreWaterResourceArea editCoreWaterResourceArea(CoreWaterResourceArea coreWaterResourceArea) {
		return this.repo.saveAndFlush(coreWaterResourceArea);
	}
}
