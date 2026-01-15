package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysAccountStatus;

public interface SysAccountStatusService {
	public List<SysAccountStatus> getAllSysAccountStatuses();

	public List<SysAccountStatus> getAllSysAccountStatuses(int page, int limit);

	public SysAccountStatus getSysAccountStatusById(String sysAccountStatusId);

	public SysAccountStatus getSysAccountStatusByName(String name);

	public void deleteSysAccountStatus(String sysAccountStatusId);

	public SysAccountStatus addSysAccountStatus(SysAccountStatus sysAccountStatus);

	public SysAccountStatus editSysAccountStatus(SysAccountStatus sysAccountStatus);
}
