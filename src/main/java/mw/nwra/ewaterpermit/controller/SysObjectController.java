package mw.nwra.ewaterpermit.controller;

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
import mw.nwra.ewaterpermit.model.SysObject;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;
import mw.nwra.ewaterpermit.service.SysObjectService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/sys-objects")
public class SysObjectController {

	@Autowired
	private SysObjectService sysObjectService;

//	@GetMapping(path = "")
//	public List<SysObject> getSysObjects(@RequestParam(value = "page", defaultValue = "0") int page,
//			@RequestParam(value = "limit", defaultValue = "50") int limit,
//			@RequestParam(value = "query", defaultValue = "") String query) {
//		List<SysObject> res = this.sysObjectService.getAllSysObjects(page, limit);
//		if (res.isEmpty()) {
//			throw new EntityNotFoundException("Object not found");
//		}
//		return res;
//	}
	@GetMapping(path = "")
	public SearchResponse getSysObjects(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "query", defaultValue = "") String query) {
		SearchResponse res = this.sysObjectService.getAllSysObjects(page, limit, query);
		if (res == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return res;
	}

	@GetMapping(path = "/{id}")
	public SysObject getSysObjectById(@PathVariable(name = "id") String id) {
		SysObject sysObject = this.sysObjectService.getSysObjectById(id);
		if (sysObject == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return sysObject;
	}

	@PostMapping(path = "")
	public SysObject createSysObject(@RequestBody Map<String, Object> sysObjectRequest) {
		SysObject sysObject = (SysObject) AppUtil.objectToClass(SysObject.class, sysObjectRequest);
		if (sysObject == null) {
			throw new ForbiddenException("Could not create the sysObject");
		}
		return this.sysObjectService.addSysObject(sysObject);
	}

	@PutMapping(path = "/{id}")
	public SysObject updateSysObject(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> sysObjectRequest) {
		SysObject sysObject = this.sysObjectService.getSysObjectById(id);
		if (sysObject == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysObject.class, sysObjectRequest);
		SysObject sysObjectFromObj = (SysObject) AppUtil.objectToClass(SysObject.class, sysObjectRequest);
		if (sysObjectFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(sysObjectFromObj, sysObject, propertiesToIgnore);
		this.sysObjectService.editSysObject(sysObject);
		return sysObject;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysObject(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysObject categ = this.sysObjectService.getSysObjectById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.sysObjectService.deleteSysObject(id);
	}
}
