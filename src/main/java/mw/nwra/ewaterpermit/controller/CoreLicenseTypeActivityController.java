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
import mw.nwra.ewaterpermit.model.CoreLicenseTypeActivity;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLicenseTypeActivityService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/license-type-activities")
public class CoreLicenseTypeActivityController {

	@Autowired
	private CoreLicenseTypeActivityService coreLicenseTypeActivityService;

	@GetMapping(path = "")
	public List<CoreLicenseTypeActivity> getCoreLicenseTypeActivities(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreLicenseTypeActivity> newsCategories = this.coreLicenseTypeActivityService
				.getAllCoreLicenseTypeActivities(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreLicenseTypeActivity getCoreLicenseTypeActivityById(@PathVariable(name = "id") String id) {
		CoreLicenseTypeActivity coreLicenseTypeActivity = this.coreLicenseTypeActivityService
				.getCoreLicenseTypeActivityById(id);
		if (coreLicenseTypeActivity == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreLicenseTypeActivity;
	}

	@PostMapping(path = "")
	public CoreLicenseTypeActivity createCoreLicenseTypeActivitys(
			@RequestBody Map<String, Object> coreLicenseTypeActivityRequest) {
		CoreLicenseTypeActivity coreLicenseTypeActivity = (CoreLicenseTypeActivity) AppUtil
				.objectToClass(CoreLicenseTypeActivity.class, coreLicenseTypeActivityRequest);
		if (coreLicenseTypeActivity == null) {
			throw new ForbiddenException("Could not create the coreLicenseTypeActivity");
		}
		return this.coreLicenseTypeActivityService.addCoreLicenseTypeActivity(coreLicenseTypeActivity);
	}

	@PutMapping(path = "/{id}")
	public CoreLicenseTypeActivity updateCoreLicenseTypeActivity(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreLicenseTypeActivityRequest) {
		CoreLicenseTypeActivity coreLicenseTypeActivity = this.coreLicenseTypeActivityService
				.getCoreLicenseTypeActivityById(id);
		if (coreLicenseTypeActivity == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreLicenseTypeActivity.class,
				coreLicenseTypeActivityRequest);
		CoreLicenseTypeActivity coreLicenseTypeActivityFromObj = (CoreLicenseTypeActivity) AppUtil
				.objectToClass(CoreLicenseTypeActivity.class, coreLicenseTypeActivityRequest);
		if (coreLicenseTypeActivityFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(coreLicenseTypeActivityFromObj, coreLicenseTypeActivity, propertiesToIgnore);
		this.coreLicenseTypeActivityService.editCoreLicenseTypeActivity(coreLicenseTypeActivity);
		return coreLicenseTypeActivity;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreLicenseTypeActivity(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreLicenseTypeActivity categ = this.coreLicenseTypeActivityService.getCoreLicenseTypeActivityById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreLicenseTypeActivityService.deleteCoreLicenseTypeActivity(id);
	}
}
