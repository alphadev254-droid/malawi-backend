package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysUserGroup;

public interface SysUserGroupService {
	public List<SysUserGroup> getAllSysUserGroups();

	public List<SysUserGroup> getAllSysUserGroups(int page, int limit);

	public SysUserGroup getSysUserGroupById(String sysUserGroupId);

	public SysUserGroup getSysUserGroupByName(String name);

	public SysUserGroup getSysUserGroupByNameIgnoreCase(String name);

	public void deleteSysUserGroup(String sysUserGroupId);

	public SysUserGroup addSysUserGroup(SysUserGroup sysUserGroup);

	public SysUserGroup editSysUserGroup(SysUserGroup sysUserGroup);
}
