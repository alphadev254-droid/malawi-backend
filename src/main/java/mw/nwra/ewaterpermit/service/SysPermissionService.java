package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysPermission;

public interface SysPermissionService {
	public List<SysPermission> getAllSysPermissions();

	public List<SysPermission> getAllSysPermissions(int page, int limit);

	public SysPermission getSysPermissionById(String sysPermissionId);

	public SysPermission getSysPermissionByName(String name);

	public void deleteSysPermission(String sysPermissionId);

	public SysPermission addSysPermission(SysPermission sysPermission);

	public SysPermission editSysPermission(SysPermission sysPermission);
}
