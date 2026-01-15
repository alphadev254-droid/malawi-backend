package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreWaterResourceArea;

public interface CoreWaterResourceAreaService {
	public List<CoreWaterResourceArea> getAllCoreWaterResourceAreas();

	public List<CoreWaterResourceArea> getAllCoreWaterResourceAreas(int page, int limit);

	public CoreWaterResourceArea getCoreWaterResourceAreaById(String id);

//	public CoreWaterResourceArea getCoreWaterResourceAreaByName(String name);

	public void deleteCoreWaterResourceArea(String id);

	public CoreWaterResourceArea addCoreWaterResourceArea(CoreWaterResourceArea coreWaterResourceArea);

	public CoreWaterResourceArea editCoreWaterResourceArea(CoreWaterResourceArea coreWaterResourceArea);
}
