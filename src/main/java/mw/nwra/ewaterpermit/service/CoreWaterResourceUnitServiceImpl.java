package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreWaterResourceArea;
import mw.nwra.ewaterpermit.model.CoreWaterResourceUnit;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceUnitRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreWaterResourceUnitService")
public class CoreWaterResourceUnitServiceImpl implements CoreWaterResourceUnitService {
	@Autowired
	CoreWaterResourceUnitRepository repo;

	@Override
	public List<CoreWaterResourceUnit> getAllCoreWaterResourceUnits() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreWaterResourceUnit> getAllCoreWaterResourceUnits(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreWaterResourceUnit getCoreWaterResourceUnitById(String id) {
		return this.repo.findById(id).orElse(null);
	}

//	@Override
//	public CoreWaterResourceUnit getCoreWaterResourceUnitByName(String name) {
//		return this.repo.findByName(name);
//	}

	@Override
	public void deleteCoreWaterResourceUnit(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreWaterResourceUnit addCoreWaterResourceUnit(CoreWaterResourceUnit coreWaterResourceUnit) {
		return this.repo.saveAndFlush(coreWaterResourceUnit);
	}

	@Override
	public CoreWaterResourceUnit editCoreWaterResourceUnit(CoreWaterResourceUnit coreWaterResourceUnit) {
		return this.repo.saveAndFlush(coreWaterResourceUnit);
	}

	@Override
	public List<CoreWaterResourceUnit> getCoreWaterResourceUnitByArea(String wraId) {
		CoreWaterResourceArea area = new CoreWaterResourceArea();
		area.setId(wraId);
		return this.repo.findByCoreWaterResourceArea(area);
	}
}
