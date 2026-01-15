package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreCustomer;
import mw.nwra.ewaterpermit.repository.CoreCustomerRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreCustomerService")
public class CoreCustomerServiceImpl implements CoreCustomerService {
	@Autowired
	CoreCustomerRepository repo;

	@Override
	public List<CoreCustomer> getAllCoreCustomers() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreCustomer> getAllCoreCustomers(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreCustomer getCoreCustomerById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreCustomer getCoreCustomerByEmail(String email) {
		return this.repo.findByEmailAddress(email);
	}

	@Override
	public void deleteCoreCustomer(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreCustomer addCoreCustomer(CoreCustomer coreCustomer) {
		return this.repo.saveAndFlush(coreCustomer);
	}

	@Override
	public CoreCustomer editCoreCustomer(CoreCustomer coreCustomer) {
		return this.repo.saveAndFlush(coreCustomer);
	}
}
