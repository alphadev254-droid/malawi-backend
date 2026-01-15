package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreApplicationStatus;
import mw.nwra.ewaterpermit.repository.CoreApplicationStatusRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreApplicationStatusService")
public class CoreApplicationStatusServiceImpl implements CoreApplicationStatusService {
	@Autowired
	CoreApplicationStatusRepository repo;

	@Override
	public List<CoreApplicationStatus> getAllCoreApplicationStatuss() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreApplicationStatus> getAllCoreApplicationStatuses(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreApplicationStatus getCoreApplicationStatusById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreApplicationStatus getCoreApplicationStatusByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreApplicationStatus(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreApplicationStatus addCoreApplicationStatus(CoreApplicationStatus coreApplicationStatus) {
		return this.repo.saveAndFlush(coreApplicationStatus);
	}

	@Override
	public CoreApplicationStatus editCoreApplicationStatus(CoreApplicationStatus coreApplicationStatus) {
		return this.repo.saveAndFlush(coreApplicationStatus);
	}
}
