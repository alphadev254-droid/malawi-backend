package mw.nwra.ewaterpermit.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.SysPermission;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.SysPermissionService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/sys-permissions")
public class SysPermissionController {

	@Autowired
	private SysPermissionService sysPermissionService;

	@GetMapping(path = "")
	public List<SysPermission> getSysPermissions(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<SysPermission> newsCategories = this.sysPermissionService.getAllSysPermissions(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public SysPermission getSysPermissionById(@PathVariable(name = "id") String id) {
		SysPermission sysPermission = this.sysPermissionService.getSysPermissionById(id);
		if (sysPermission == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return sysPermission;
	}

	@PostMapping(path = "")
	public SysPermission createSysPermission(@RequestBody Map<String, Object> sysPermissionRequest) {
		SysPermission sysPermission = (SysPermission) AppUtil.objectToClass(SysPermission.class, sysPermissionRequest);
		if (sysPermission == null) {
			throw new ForbiddenException("Could not create the sysPermission");
		}
		return this.sysPermissionService.addSysPermission(sysPermission);
	}

	@PutMapping(path = "/{id}")
	public SysPermission updateSysPermission(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> sysPermissionRequest) {
		SysPermission sysPermission = this.sysPermissionService.getSysPermissionById(id);
		if (sysPermission == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysPermission.class, sysPermissionRequest);
		SysPermission sysPermissionFromObj = (SysPermission) AppUtil.objectToClass(SysPermission.class,
				sysPermissionRequest);
		if (sysPermissionFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(sysPermissionFromObj, sysPermission, propertiesToIgnore);
		this.sysPermissionService.editSysPermission(sysPermission);
		return sysPermission;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysPermission(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysPermission categ = this.sysPermissionService.getSysPermissionById(id);
		if (categ == null) {
			throw new EntityNotFoundException(" group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.sysPermissionService.deleteSysPermission(id);
	}
}
