package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreWaterUse;

public interface CoreWaterUseService {
	public List<CoreWaterUse> getAllCoreWaterUses();

	public List<CoreWaterUse> getAllCoreWaterUses(int page, int limit);

	public CoreWaterUse getCoreWaterUseById(String id);

	public CoreWaterUse getCoreWaterUseByName(String name);

	public void deleteCoreWaterUse(String id);

	public CoreWaterUse addCoreWaterUse(CoreWaterUse coreWaterUse);

	public CoreWaterUse editCoreWaterUse(CoreWaterUse coreWaterUse);
}
