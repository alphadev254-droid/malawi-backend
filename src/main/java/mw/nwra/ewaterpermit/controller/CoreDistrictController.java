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
import mw.nwra.ewaterpermit.model.CoreDistrict;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreDistrictService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/districts")
public class CoreDistrictController {

	@Autowired
	private CoreDistrictService coreDistrictService;

	@GetMapping(path = "")
	public List<CoreDistrict> getCoreDistricts(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreDistrict> newsCategories = this.coreDistrictService.getAllCoreDistricts(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreDistrict getCoreDistrictById(@PathVariable(name = "id") String id) {
		CoreDistrict coreDistrict = this.coreDistrictService.getCoreDistrictById(id);
		if (coreDistrict == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreDistrict;
	}

	@PostMapping(path = "")
	public CoreDistrict createCoreDistrict(@RequestBody Map<String, Object> coreDistrictRequest) {
		CoreDistrict coreDistrict = (CoreDistrict) AppUtil.objectToClass(CoreDistrict.class, coreDistrictRequest);
		if (coreDistrict == null) {
			throw new ForbiddenException("Could not create the coreDistrict");
		}
		return this.coreDistrictService.addCoreDistrict(coreDistrict);
	}

	@PutMapping(path = "/{id}")
	public CoreDistrict updateCoreDistrict(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreDistrictRequest) {
		CoreDistrict coreDistrict = this.coreDistrictService.getCoreDistrictById(id);
		if (coreDistrict == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreDistrict.class, coreDistrictRequest);
		CoreDistrict coreDistrictFromObj = (CoreDistrict) AppUtil.objectToClass(CoreDistrict.class,
				coreDistrictRequest);
		if (coreDistrictFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(coreDistrictFromObj, coreDistrict, propertiesToIgnore);
		this.coreDistrictService.editCoreDistrict(coreDistrict);
		return coreDistrict;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreDistrict(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreDistrict categ = this.coreDistrictService.getCoreDistrictById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreDistrictService.deleteCoreDistrict(id);
	}
}
