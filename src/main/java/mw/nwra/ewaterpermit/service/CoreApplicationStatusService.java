package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreApplicationStatus;

public interface CoreApplicationStatusService {
	public List<CoreApplicationStatus> getAllCoreApplicationStatuss();

	public List<CoreApplicationStatus> getAllCoreApplicationStatuses(int page, int limit);

	public CoreApplicationStatus getCoreApplicationStatusById(String id);

	public CoreApplicationStatus getCoreApplicationStatusByName(String name);

	public void deleteCoreApplicationStatus(String id);

	public CoreApplicationStatus addCoreApplicationStatus(CoreApplicationStatus coreApplicationStatus);

	public CoreApplicationStatus editCoreApplicationStatus(CoreApplicationStatus coreApplicationStatus);
}
