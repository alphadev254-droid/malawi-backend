package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreWaterSource;
import mw.nwra.ewaterpermit.model.CoreWaterSourceType;
import mw.nwra.ewaterpermit.repository.CoreWaterSourceRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreWaterSourceService")
public class CoreWaterSourceServiceImpl implements CoreWaterSourceService {
	@Autowired
	CoreWaterSourceRepository repo;

	@Override
	public List<CoreWaterSource> getAllCoreWaterSources() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreWaterSource> getAllCoreWaterSources(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreWaterSource getCoreWaterSourceById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreWaterSource getCoreWaterSourceByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreWaterSource(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreWaterSource addCoreWaterSource(CoreWaterSource coreWaterSource) {
		return this.repo.saveAndFlush(coreWaterSource);
	}

	@Override
	public CoreWaterSource editCoreWaterSource(CoreWaterSource coreWaterSource) {
		return this.repo.saveAndFlush(coreWaterSource);
	}

	@Override
	public List<CoreWaterSource> getBySourceType(CoreWaterSourceType wSourceType) {
		return this.repo.findByCoreWaterSourceType(wSourceType);
	}
}
