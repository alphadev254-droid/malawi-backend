	package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
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
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.model.SysUserGroupPermission;
import mw.nwra.ewaterpermit.service.SysUserGroupPermissionService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/sys-user-group-permissions")
public class SysUserGroupPermissionController {

	@Autowired
	private SysUserGroupPermissionService sysUserGroupPermissionService;

	@GetMapping(path = "")
	public List<SysUserGroupPermission> getSysUserGroupPermissions(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<SysUserGroupPermission> newsCategories = this.sysUserGroupPermissionService
				.getAllSysUserGroupPermissions(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public SysUserGroupPermission getSysUserGroupPermissionById(@PathVariable(name = "id") String id) {
		SysUserGroupPermission SysUserGroupPermission = this.sysUserGroupPermissionService
				.getSysUserGroupPermissionById(id);
		if (SysUserGroupPermission == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return SysUserGroupPermission;
	}

	@PostMapping(path = "")
	public SysUserGroupPermission createSysUserGroupPermission(
			@RequestBody Map<String, Object> SysUserGroupPermissionRequest) {
		SysUserGroupPermission SysUserGroupPermission = (SysUserGroupPermission) AppUtil
				.objectToClass(SysUserGroupPermission.class, SysUserGroupPermissionRequest);
		if (SysUserGroupPermission == null) {
			throw new ForbiddenException("Could not create the SysUserGroupPermission");
		}
		SysUserGroupPermission.setDateCreated(new Timestamp(new Date().getTime()));
		return this.sysUserGroupPermissionService.addSysUserGroupPermission(SysUserGroupPermission);
	}

	@PutMapping(path = "/{id}")
	public SysUserGroupPermission updateSysUserGroupPermission(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> SysUserGroupPermissionRequest) {
		SysUserGroupPermission SysUserGroupPermission = this.sysUserGroupPermissionService
				.getSysUserGroupPermissionById(id);
		if (SysUserGroupPermission == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysUserGroupPermission.class,
				SysUserGroupPermissionRequest);
		SysUserGroupPermission SysUserGroupPermissionFromObj = (SysUserGroupPermission) AppUtil
				.objectToClass(SysUserGroupPermission.class, SysUserGroupPermissionRequest);
		if (SysUserGroupPermissionFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(SysUserGroupPermissionFromObj, SysUserGroupPermission, propertiesToIgnore);
		this.sysUserGroupPermissionService.editSysUserGroupPermission(SysUserGroupPermission);
		return SysUserGroupPermission;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysUserGroupPermission(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysUserGroupPermission categ = this.sysUserGroupPermissionService.getSysUserGroupPermissionById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.sysUserGroupPermissionService.deleteSysUserGroupPermission(id);
	}
}
