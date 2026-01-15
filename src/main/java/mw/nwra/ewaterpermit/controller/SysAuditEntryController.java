package mw.nwra.ewaterpermit.controller;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.SysAuditEntry;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;
import mw.nwra.ewaterpermit.service.SysAuditEntryService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/sys-audits")
public class SysAuditEntryController {

	@Autowired
	private SysAuditEntryService sysAuditService;

	@GetMapping(path = "")
	public SearchResponse getSysAuditEntrys(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "query", defaultValue = "") String query) {
		SearchResponse res = this.sysAuditService.getAllSysAuditEntrys(page, limit, query);
		if (res == null) {
			throw new EntityNotFoundException("Audit not found");
		}
		return res;
	}

	@GetMapping(path = "/{id}")
	public SysAuditEntry getSysAuditEntryById(@PathVariable(name = "id") String id) {
		SysAuditEntry sysObject = this.sysAuditService.getSysAuditEntryById(id);
		if (sysObject == null) {
			throw new EntityNotFoundException("Audit not found");
		}
		return sysObject;
	}

	@PostMapping(path = "")
	public SysAuditEntry createSysAuditEntry(@RequestBody Map<String, Object> sysObjectRequest) {
		SysAuditEntry sysObject = (SysAuditEntry) AppUtil.objectToClass(SysAuditEntry.class, sysObjectRequest);
		if (sysObject == null) {
			throw new ForbiddenException("Could not create the sysObject");
		}
		return this.sysAuditService.addSysAuditEntry(sysObject);
	}

	@PutMapping(path = "/{id}")
	public SysAuditEntry updateSysAuditEntry(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> sysObjectRequest) {
		SysAuditEntry sysObject = this.sysAuditService.getSysAuditEntryById(id);
		if (sysObject == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysAuditEntry.class, sysObjectRequest);
		SysAuditEntry sysObjectFromObj = (SysAuditEntry) AppUtil.objectToClass(SysAuditEntry.class, sysObjectRequest);
		if (sysObjectFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(sysObjectFromObj, sysObject, propertiesToIgnore);
		this.sysAuditService.editSysAuditEntry(sysObject);
		return sysObject;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysAuditEntry(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysAuditEntry categ = this.sysAuditService.getSysAuditEntryById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.sysAuditService.deleteSysAuditEntry(id);
	}

}
