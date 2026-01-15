/**
 * 
 */
package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysPermission;
import mw.nwra.ewaterpermit.repository.SysPermissionRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysPermissionService")
public class SysPermissionServiceImpl implements SysPermissionService {
	@Autowired
	SysPermissionRepository sysPermissionRepository;

	@Override
	public List<SysPermission> getAllSysPermissions() {
		return this.sysPermissionRepository.findAll();
	}

	@Override
	public List<SysPermission> getAllSysPermissions(int page, int limit) {
		return this.sysPermissionRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "desc")).getContent();
	}

	@Override
	public SysPermission getSysPermissionById(String sysPermissionId) {
		return this.sysPermissionRepository.findById(sysPermissionId).orElse(null);
	}

	@Override
	public void deleteSysPermission(String sysPermissionId) {
		this.sysPermissionRepository.deleteById(sysPermissionId);
	}

	@Override
	public SysPermission addSysPermission(SysPermission sysPermission) {
		return this.sysPermissionRepository.saveAndFlush(sysPermission);
	}

	@Override
	public SysPermission editSysPermission(SysPermission sysPermission) {
		return this.sysPermissionRepository.saveAndFlush(sysPermission);
	}

	@Override
	public SysPermission getSysPermissionByName(String name) {
		return this.sysPermissionRepository.findByName(name);
	}
}
