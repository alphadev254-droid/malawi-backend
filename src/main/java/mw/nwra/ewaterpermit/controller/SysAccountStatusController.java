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
import mw.nwra.ewaterpermit.model.SysAccountStatus;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.SysAccountStatusService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/sys-account-statuses")
public class SysAccountStatusController {

	@Autowired
	private SysAccountStatusService sysAccountStatusService;

	@GetMapping(path = "")
	public List<SysAccountStatus> getSysAccountStatuss(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<SysAccountStatus> newsCategories = this.sysAccountStatusService.getAllSysAccountStatuses(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public SysAccountStatus getSysAccountStatusById(@PathVariable(name = "id") String id) {
		SysAccountStatus sysAccountStatus = this.sysAccountStatusService.getSysAccountStatusById(id);
		if (sysAccountStatus == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return sysAccountStatus;
	}

	@PostMapping(path = "")
	public SysAccountStatus createSysAccountStatus(@RequestBody Map<String, Object> sysAccountStatusRequest) {
		SysAccountStatus sysAccountStatus = (SysAccountStatus) AppUtil.objectToClass(SysAccountStatus.class,
				sysAccountStatusRequest);
		if (sysAccountStatus == null) {
			throw new ForbiddenException("Could not create the sysAccountStatus");
		}
		return this.sysAccountStatusService.addSysAccountStatus(sysAccountStatus);
	}

	@PutMapping(path = "/{id}")
	public SysAccountStatus updateSysAccountStatus(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> sysAccountStatusRequest) {
		SysAccountStatus sysAccountStatus = this.sysAccountStatusService.getSysAccountStatusById(id);
		if (sysAccountStatus == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysAccountStatus.class, sysAccountStatusRequest);
		SysAccountStatus sysAccountStatusFromObj = (SysAccountStatus) AppUtil.objectToClass(SysAccountStatus.class,
				sysAccountStatusRequest);
		if (sysAccountStatusFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(sysAccountStatusFromObj, sysAccountStatus, propertiesToIgnore);
		this.sysAccountStatusService.editSysAccountStatus(sysAccountStatus);
		return sysAccountStatus;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysAccountStatus(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysAccountStatus categ = this.sysAccountStatusService.getSysAccountStatusById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.sysAccountStatusService.deleteSysAccountStatus(id);
	}
}
