package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreCustomer;

public interface CoreCustomerService {
	public List<CoreCustomer> getAllCoreCustomers();

	public List<CoreCustomer> getAllCoreCustomers(int page, int limit);

	public CoreCustomer getCoreCustomerById(String id);

	public CoreCustomer getCoreCustomerByEmail(String email);

	public void deleteCoreCustomer(String id);

	public CoreCustomer addCoreCustomer(CoreCustomer coreCustomer);

	public CoreCustomer editCoreCustomer(CoreCustomer coreCustomer);
}
