package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreWaterSourceType;

public interface CoreWaterSourceTypeService {
	public List<CoreWaterSourceType> getAllCoreWaterSourceTypes();

	public List<CoreWaterSourceType> getAllCoreWaterSourceTypes(int page, int limit);

	public CoreWaterSourceType getCoreWaterSourceTypeById(String id);

	public CoreWaterSourceType getCoreWaterSourceTypeByName(String name);

	public void deleteCoreWaterSourceType(String id);

	public CoreWaterSourceType addCoreWaterSourceType(CoreWaterSourceType coreWaterSourceType);

	public CoreWaterSourceType editCoreWaterSourceType(CoreWaterSourceType coreWaterSourceType);
}
