package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreWaterResourceUnit;

public interface CoreWaterResourceUnitService {
	public List<CoreWaterResourceUnit> getAllCoreWaterResourceUnits();

	public List<CoreWaterResourceUnit> getAllCoreWaterResourceUnits(int page, int limit);

	public CoreWaterResourceUnit getCoreWaterResourceUnitById(String id);

//	public CoreWaterResourceUnit getCoreWaterResourceUnitByName(String name);

	public void deleteCoreWaterResourceUnit(String id);

	public CoreWaterResourceUnit addCoreWaterResourceUnit(CoreWaterResourceUnit coreWaterResourceUnit);

	public CoreWaterResourceUnit editCoreWaterResourceUnit(CoreWaterResourceUnit coreWaterResourceUnit);

	public List<CoreWaterResourceUnit> getCoreWaterResourceUnitByArea(String wraId);
}
