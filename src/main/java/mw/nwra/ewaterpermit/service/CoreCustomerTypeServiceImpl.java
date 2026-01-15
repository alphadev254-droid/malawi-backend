package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreCustomerType;
import mw.nwra.ewaterpermit.repository.CoreCustomerTypeRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreCustomerTypeService")
public class CoreCustomerTypeServiceImpl implements CoreCustomerTypeService {
	@Autowired
	CoreCustomerTypeRepository repo;

	@Override
	public List<CoreCustomerType> getAllCoreCustomerTypes() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreCustomerType> getAllCoreCustomerTypes(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreCustomerType getCoreCustomerTypeById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreCustomerType getCoreCustomerTypeByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreCustomerType(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreCustomerType addCoreCustomerType(CoreCustomerType coreCustomerType) {
		return this.repo.saveAndFlush(coreCustomerType);
	}

	@Override
	public CoreCustomerType editCoreCustomerType(CoreCustomerType coreCustomerType) {
		return this.repo.saveAndFlush(coreCustomerType);
	}
}
