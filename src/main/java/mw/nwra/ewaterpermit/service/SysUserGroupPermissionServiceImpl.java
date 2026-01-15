package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysUserGroup;
import mw.nwra.ewaterpermit.model.SysUserGroupPermission;
import mw.nwra.ewaterpermit.repository.SysUserGroupPermissionRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysUserGroupPermissionService")
public class SysUserGroupPermissionServiceImpl implements SysUserGroupPermissionService {

	@Autowired
	SysUserGroupPermissionRepository sysUserGroupPermissionRepository;

	@Override
	public List<SysUserGroupPermission> getAllSysUserGroupPermissions() {
		return this.sysUserGroupPermissionRepository.findAll();
	}

	@Override
	public List<SysUserGroupPermission> getAllSysUserGroupPermissions(int page, int limit) {
		return this.sysUserGroupPermissionRepository.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc"))
				.getContent();
	}

	@Override
	public SysUserGroupPermission getSysUserGroupPermissionById(String sysUserGroupPermissionId) {
		return this.sysUserGroupPermissionRepository.findById(sysUserGroupPermissionId).orElse(null);
	}

	@Override
	public void deleteSysUserGroupPermission(String sysUserGroupPermissionId) {
		this.sysUserGroupPermissionRepository.deleteById(sysUserGroupPermissionId);
	}

	@Override
	public SysUserGroupPermission addSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission) {
		return this.sysUserGroupPermissionRepository.saveAndFlush(sysUserGroupPermission);
	}

	@Override
	public SysUserGroupPermission editSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission) {
		return this.sysUserGroupPermissionRepository.saveAndFlush(sysUserGroupPermission);
	}

	@Override
	public List<SysUserGroupPermission> getSysUserGroupPermissionBySysUserGroup(SysUserGroup userGroup) {
		return this.sysUserGroupPermissionRepository.findBySysUserGroup(userGroup);
	}
}
