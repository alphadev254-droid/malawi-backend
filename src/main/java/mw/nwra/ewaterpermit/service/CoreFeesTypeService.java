package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreFeesType;

public interface CoreFeesTypeService {
	public List<CoreFeesType> getAllCoreFeesTypes();

	public List<CoreFeesType> getAllCoreFeesTypes(int page, int limit);

	public CoreFeesType getCoreFeesTypeById(String id);

	public CoreFeesType getCoreFeesTypeByName(String name);

	public void deleteCoreFeesType(String id);

	public CoreFeesType addCoreFeesType(CoreFeesType coreFeesType);

	public CoreFeesType editCoreFeesType(CoreFeesType coreFeesType);
}
