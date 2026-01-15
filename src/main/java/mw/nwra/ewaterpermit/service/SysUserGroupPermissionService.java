package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysUserGroup;
import mw.nwra.ewaterpermit.model.SysUserGroupPermission;

public interface SysUserGroupPermissionService {
	public List<SysUserGroupPermission> getAllSysUserGroupPermissions();

	public List<SysUserGroupPermission> getAllSysUserGroupPermissions(int page, int limit);

	public SysUserGroupPermission getSysUserGroupPermissionById(String sysUserGroupPermissionId);

	public List<SysUserGroupPermission> getSysUserGroupPermissionBySysUserGroup(SysUserGroup userGroup);

	public void deleteSysUserGroupPermission(String sysUserGroupPermissionId);

	public SysUserGroupPermission addSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission);

	public SysUserGroupPermission editSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission);
}
