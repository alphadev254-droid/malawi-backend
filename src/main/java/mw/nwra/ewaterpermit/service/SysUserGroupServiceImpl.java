package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysUserGroup;
import mw.nwra.ewaterpermit.repository.SysUserGroupRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysUserGroupService")
public class SysUserGroupServiceImpl implements SysUserGroupService {

	@Autowired
	SysUserGroupRepository sysUserGroupRepository;

	@Override
	public List<SysUserGroup> getAllSysUserGroups() {
		return this.sysUserGroupRepository.findAll();
	}

	@Override
	public List<SysUserGroup> getAllSysUserGroups(int page, int limit) {
		return this.sysUserGroupRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "desc")).getContent();
	}

	@Override
	public SysUserGroup getSysUserGroupById(String sysUserGroupId) {
		return this.sysUserGroupRepository.findById(sysUserGroupId).orElse(null);
	}

	@Override
	public void deleteSysUserGroup(String sysUserGroupId) {
		this.sysUserGroupRepository.deleteById(sysUserGroupId);
	}

	@Override
	public SysUserGroup addSysUserGroup(SysUserGroup sysUserGroup) {
		return this.sysUserGroupRepository.saveAndFlush(sysUserGroup);
	}

	@Override
	public SysUserGroup editSysUserGroup(SysUserGroup sysUserGroup) {
		return this.sysUserGroupRepository.saveAndFlush(sysUserGroup);
	}

	@Override
	public SysUserGroup getSysUserGroupByName(String name) {
		return this.sysUserGroupRepository.findByName(name);
	}

	@Override
	public SysUserGroup getSysUserGroupByNameIgnoreCase(String name) {
		return this.sysUserGroupRepository.findByNameIgnoreCase(name);
	}
}
