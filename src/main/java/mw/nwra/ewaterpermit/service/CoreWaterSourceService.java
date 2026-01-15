package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreWaterSource;
import mw.nwra.ewaterpermit.model.CoreWaterSourceType;

public interface CoreWaterSourceService {
	public List<CoreWaterSource> getAllCoreWaterSources();

	public List<CoreWaterSource> getAllCoreWaterSources(int page, int limit);

	public CoreWaterSource getCoreWaterSourceById(String id);

	public CoreWaterSource getCoreWaterSourceByName(String name);

	public void deleteCoreWaterSource(String id);

	public CoreWaterSource addCoreWaterSource(CoreWaterSource coreWaterSource);

	public CoreWaterSource editCoreWaterSource(CoreWaterSource coreWaterSource);

	public List<CoreWaterSource> getBySourceType(CoreWaterSourceType wSourceType);
}
