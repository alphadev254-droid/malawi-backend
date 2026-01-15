package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreFeesType;
import mw.nwra.ewaterpermit.repository.CoreFeesTypeRepository;

@Service(value = "coreFeesTypeService")
public class CoreFeesTypeServiceImpl implements CoreFeesTypeService {
	@Autowired
	CoreFeesTypeRepository repo;

	@Override
	public List<CoreFeesType> getAllCoreFeesTypes() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreFeesType> getAllCoreFeesTypes(int page, int limit) {
		return this.repo.findAll();
	}

	@Override
	public CoreFeesType getCoreFeesTypeById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreFeesType getCoreFeesTypeByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreFeesType(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreFeesType addCoreFeesType(CoreFeesType coreFeesType) {
		return this.repo.saveAndFlush(coreFeesType);
	}

	@Override
	public CoreFeesType editCoreFeesType(CoreFeesType coreFeesType) {
		return this.repo.saveAndFlush(coreFeesType);
	}
}
