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
import mw.nwra.ewaterpermit.model.CoreLicenseApplicationActivity;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationActivityService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/license-application-activities")
public class CoreLicenseApplicationActivityController {

	@Autowired
	private CoreLicenseApplicationActivityService coreLicenseApplicationActivityService;

	@GetMapping(path = "")
	public List<CoreLicenseApplicationActivity> getCoreLicenseApplicationActivitys(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreLicenseApplicationActivity> newsCategories = this.coreLicenseApplicationActivityService
				.getAllCoreLicenseApplicationActivities(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreLicenseApplicationActivity getCoreLicenseApplicationActivityById(@PathVariable(name = "id") String id) {
		CoreLicenseApplicationActivity coreLicenseApplicationActivity = this.coreLicenseApplicationActivityService
				.getCoreLicenseApplicationActivityById(id);
		if (coreLicenseApplicationActivity == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreLicenseApplicationActivity;
	}

	@PostMapping(path = "")
	public CoreLicenseApplicationActivity createCoreLicenseApplicationActivity(
			@RequestBody Map<String, Object> coreLicenseApplicationActivityRequest) {
		CoreLicenseApplicationActivity coreLicenseApplicationActivity = (CoreLicenseApplicationActivity) AppUtil
				.objectToClass(CoreLicenseApplicationActivity.class, coreLicenseApplicationActivityRequest);
		if (coreLicenseApplicationActivity == null) {
			throw new ForbiddenException("Could not create the coreLicenseApplicationActivity");
		}
		return this.coreLicenseApplicationActivityService
				.addCoreLicenseApplicationActivity(coreLicenseApplicationActivity);
	}

	@PutMapping(path = "/{id}")
	public CoreLicenseApplicationActivity updateCoreLicenseApplicationActivity(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreLicenseApplicationActivityRequest) {
		CoreLicenseApplicationActivity coreLicenseApplicationActivity = this.coreLicenseApplicationActivityService
				.getCoreLicenseApplicationActivityById(id);
		if (coreLicenseApplicationActivity == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreLicenseApplicationActivity.class,
				coreLicenseApplicationActivityRequest);
		CoreLicenseApplicationActivity coreLicenseApplicationActivityFromObj = (CoreLicenseApplicationActivity) AppUtil
				.objectToClass(CoreLicenseApplicationActivity.class, coreLicenseApplicationActivityRequest);
		if (coreLicenseApplicationActivityFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(coreLicenseApplicationActivityFromObj, coreLicenseApplicationActivity,
				propertiesToIgnore);
		this.coreLicenseApplicationActivityService.editCoreLicenseApplicationActivity(coreLicenseApplicationActivity);
		return coreLicenseApplicationActivity;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreLicenseApplicationActivity(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreLicenseApplicationActivity categ = this.coreLicenseApplicationActivityService
				.getCoreLicenseApplicationActivityById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreLicenseApplicationActivityService.deleteCoreLicenseApplicationActivity(id);
	}
}
