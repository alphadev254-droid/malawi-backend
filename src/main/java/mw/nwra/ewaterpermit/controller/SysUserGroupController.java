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
import mw.nwra.ewaterpermit.model.SysUserGroup;
import mw.nwra.ewaterpermit.service.SysUserGroupService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

import org.springframework.dao.DataIntegrityViolationException;

@RestController
@RequestMapping(value = "/v1/sys-user-groups")
public class SysUserGroupController {

	@Autowired
	private SysUserGroupService sysGroupService;
	
	@Autowired
	private Auditor auditor;

	@GetMapping(path = "")
	public List<SysUserGroup> getAllGroups(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<SysUserGroup> newsCategories = this.sysGroupService.getAllSysUserGroups(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("User Group not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public SysUserGroup getSysUserGroupById(@PathVariable(name = "id") String groupId) {
		SysUserGroup sysGroup = this.sysGroupService.getSysUserGroupById(groupId);
		if (sysGroup == null) {
			throw new EntityNotFoundException("User Group not found");
		}
		return sysGroup;
	}

	@PostMapping(path = "")
	public SysUserGroup createSysUserGroup(@RequestBody Map<String, Object> sysUserGroupRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserGroup sysUserGroup = (SysUserGroup) AppUtil.objectToClass(SysUserGroup.class, sysUserGroupRequest);
		if (sysUserGroup == null) {
			throw new ForbiddenException("Could not create the sys user group");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		sysUserGroup.setDateCreated(new Timestamp(new Date().getTime()));
		sysUserGroup = this.sysGroupService.addSysUserGroup(sysUserGroup);
		
		// Audit log
		auditor.audit(user, null, sysUserGroup, SysUserGroup.class, Action.CREATE.toString());
		
		return sysUserGroup;
	}

	@PutMapping(path = "/{id}")
	public SysUserGroup updateSysUserGroup(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> sysUserGroupRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserGroup sysUserGroup = this.sysGroupService.getSysUserGroupById(id);
		if (sysUserGroup == null) {
			throw new EntityNotFoundException("Role not found");
		}
		
		// Clone for audit
		SysUserGroup oldGroup = new SysUserGroup();
		BeanUtils.copyProperties(sysUserGroup, oldGroup);
		
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysUserGroup.class, sysUserGroupRequest);
		SysUserGroup sysUserGroupFromObj = (SysUserGroup) AppUtil.objectToClass(SysUserGroup.class,
				sysUserGroupRequest);

		if (sysUserGroupFromObj == null) {
			throw new ForbiddenException("Could not update the sysUserGroup sysUserGroup");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		BeanUtils.copyProperties(sysUserGroupFromObj, sysUserGroup, propertiesToIgnore);
		sysUserGroup.setDateUpdated(new Timestamp(new Date().getTime()));
		this.sysGroupService.editSysUserGroup(sysUserGroup);
		
		// Audit log
		auditor.audit(user, oldGroup, sysUserGroup, SysUserGroup.class, Action.UPDATE.toString());
		
		return sysUserGroup;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysUserGroup(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysUserGroup categ = this.sysGroupService.getSysUserGroupById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}

		try {
			this.sysGroupService.deleteSysUserGroup(id);
			
			// Audit log
			auditor.audit(ua, categ, null, SysUserGroup.class, Action.DELETE.toString());
		} catch (DataIntegrityViolationException e) {
			throw new ForbiddenException("Cannot delete user group '" + categ.getName() + "' because it has users assigned to it. Please reassign or remove all users from this group before deleting.");
		}
	}
}
