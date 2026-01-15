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
import mw.nwra.ewaterpermit.model.CoreLicenseRequirement;
import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLicenseRequirementService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/license-requirements")
public class CoreLicenseRequirementController {

	@Autowired
	private CoreLicenseRequirementService coreLicenseRequirementService;

	@GetMapping(path = "")
	public List<CoreLicenseRequirement> getCoreLicenseRequirements(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreLicenseRequirement> newsCategories = this.coreLicenseRequirementService
				.getAllCoreLicenseRequirements(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreLicenseRequirement getCoreLicenseRequirementById(@PathVariable(name = "id") String id) {
		CoreLicenseRequirement coreLicenseRequirement = this.coreLicenseRequirementService
				.getCoreLicenseRequirementById(id);
		if (coreLicenseRequirement == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreLicenseRequirement;
	}

	@GetMapping(path = "/{id}/license-type")
	public List<CoreLicenseRequirement> getCoreLicenseRequirementByType(@PathVariable(name = "id") String id) {
		CoreLicenseType lType = new CoreLicenseType();
		lType.setId(id);
		List<CoreLicenseRequirement> coreLicenseRequirement = this.coreLicenseRequirementService
				.getCoreLicenseRequirementByCoreLicenseType(lType);

		// Return empty list instead of throwing exception to be more frontend-friendly
		return coreLicenseRequirement;
	}

	@PostMapping(path = "")
	public CoreLicenseRequirement createCoreLicenseRequirements(
			@RequestBody Map<String, Object> coreLicenseRequirementRequest) {
		CoreLicenseRequirement coreLicenseRequirement = (CoreLicenseRequirement) AppUtil
				.objectToClass(CoreLicenseRequirement.class, coreLicenseRequirementRequest);
		if (coreLicenseRequirement == null) {
			throw new ForbiddenException("Could not create the coreLicenseRequirement");
		}
		return this.coreLicenseRequirementService.addCoreLicenseRequirement(coreLicenseRequirement);
	}

	@PutMapping(path = "/{id}")
	public CoreLicenseRequirement updateCoreLicenseRequirement(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreLicenseRequirementRequest) {
		CoreLicenseRequirement coreLicenseRequirement = this.coreLicenseRequirementService
				.getCoreLicenseRequirementById(id);
		if (coreLicenseRequirement == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreLicenseRequirement.class,
				coreLicenseRequirementRequest);
		CoreLicenseRequirement coreLicenseRequirementFromObj = (CoreLicenseRequirement) AppUtil
				.objectToClass(CoreLicenseRequirement.class, coreLicenseRequirementRequest);
		if (coreLicenseRequirementFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(coreLicenseRequirementFromObj, coreLicenseRequirement, propertiesToIgnore);
		this.coreLicenseRequirementService.editCoreLicenseRequirement(coreLicenseRequirement);
		return coreLicenseRequirement;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreLicenseRequirement(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreLicenseRequirement categ = this.coreLicenseRequirementService.getCoreLicenseRequirementById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreLicenseRequirementService.deleteCoreLicenseRequirement(id);
	}
}
