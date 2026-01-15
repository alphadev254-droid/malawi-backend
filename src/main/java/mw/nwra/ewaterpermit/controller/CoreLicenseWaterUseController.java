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
import mw.nwra.ewaterpermit.model.CoreLicenseWaterUse;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLicenseWaterUseService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/license-water-uses")
public class CoreLicenseWaterUseController {

	@Autowired
	private CoreLicenseWaterUseService coreLicenseWaterUseService;

	@GetMapping(path = "")
	public List<CoreLicenseWaterUse> getCoreLicenseWaterUses(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreLicenseWaterUse> newsCategories = this.coreLicenseWaterUseService.getAllCoreLicenseWaterUses(page,
				limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreLicenseWaterUse getCoreLicenseWaterUseById(@PathVariable(name = "id") String id) {
		CoreLicenseWaterUse coreLicenseWaterUse = this.coreLicenseWaterUseService.getCoreLicenseWaterUseById(id);
		if (coreLicenseWaterUse == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreLicenseWaterUse;
	}

	@PostMapping(path = "")
	public CoreLicenseWaterUse createCoreLicenseWaterUses(@RequestBody Map<String, Object> coreLicenseWaterUseRequest) {
		CoreLicenseWaterUse coreLicenseWaterUse = (CoreLicenseWaterUse) AppUtil.objectToClass(CoreLicenseWaterUse.class,
				coreLicenseWaterUseRequest);
		if (coreLicenseWaterUse == null) {
			throw new ForbiddenException("Could not create the coreLicenseWaterUse");
		}
		return this.coreLicenseWaterUseService.addCoreLicenseWaterUse(coreLicenseWaterUse);
	}

	@PutMapping(path = "/{id}")
	public CoreLicenseWaterUse updateCoreLicenseWaterUse(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreLicenseWaterUseRequest) {
		CoreLicenseWaterUse coreLicenseWaterUse = this.coreLicenseWaterUseService.getCoreLicenseWaterUseById(id);
		if (coreLicenseWaterUse == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreLicenseWaterUse.class,
				coreLicenseWaterUseRequest);
		CoreLicenseWaterUse coreLicenseWaterUseFromObj = (CoreLicenseWaterUse) AppUtil
				.objectToClass(CoreLicenseWaterUse.class, coreLicenseWaterUseRequest);
		if (coreLicenseWaterUseFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(coreLicenseWaterUseFromObj, coreLicenseWaterUse, propertiesToIgnore);
		this.coreLicenseWaterUseService.editCoreLicenseWaterUse(coreLicenseWaterUse);
		return coreLicenseWaterUse;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreLicenseWaterUse(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreLicenseWaterUse categ = this.coreLicenseWaterUseService.getCoreLicenseWaterUseById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreLicenseWaterUseService.deleteCoreLicenseWaterUse(id);
	}
}
