package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreCustomerType;

public interface CoreCustomerTypeService {
	public List<CoreCustomerType> getAllCoreCustomerTypes();

	public List<CoreCustomerType> getAllCoreCustomerTypes(int page, int limit);

	public CoreCustomerType getCoreCustomerTypeById(String id);

	public CoreCustomerType getCoreCustomerTypeByName(String name);

	public void deleteCoreCustomerType(String id);

	public CoreCustomerType addCoreCustomerType(CoreCustomerType coreCustomerType);

	public CoreCustomerType editCoreCustomerType(CoreCustomerType coreCustomerType);
}
