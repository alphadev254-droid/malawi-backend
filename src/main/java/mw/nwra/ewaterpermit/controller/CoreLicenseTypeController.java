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
import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLicenseTypeService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/license-types")
public class CoreLicenseTypeController {

	@Autowired
	private CoreLicenseTypeService coreLicenseTypeService;
	
	@Autowired
	private Auditor auditor;

	@GetMapping(path = "")
	public List<CoreLicenseType> getCoreLicenseTypes(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreLicenseType> licenseTypes = this.coreLicenseTypeService.getAllCoreLicenseTypes(page, limit);
		// Return empty list instead of throwing exception to be more frontend-friendly
		return licenseTypes;
	}

	@GetMapping(path = "/{id}")
	public CoreLicenseType getCoreLicenseTypeById(@PathVariable(name = "id") String id) {
		CoreLicenseType coreLicenseType = this.coreLicenseTypeService.getCoreLicenseTypeById(id);
		if (coreLicenseType == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreLicenseType;
	}

	@PostMapping(path = "")
	public CoreLicenseType createCoreLicenseTypes(@RequestBody Map<String, Object> coreLicenseTypeRequest,
			@RequestHeader(name = "Authorization") String token) {
		CoreLicenseType coreLicenseType = (CoreLicenseType) AppUtil.objectToClass(CoreLicenseType.class,
				coreLicenseTypeRequest);
		if (coreLicenseType == null) {
			throw new ForbiddenException("Could not create the coreLicenseType");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		coreLicenseType = this.coreLicenseTypeService.addCoreLicenseType(coreLicenseType);
		
		// Audit log
		auditor.audit(Action.CREATE, "CoreLicenseType", coreLicenseType.getId(), user, "Created license type");
		
		return coreLicenseType;
	}

	@PutMapping(path = "/{id}")
	public CoreLicenseType updateCoreLicenseType(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreLicenseTypeRequest,
			@RequestHeader(name = "Authorization") String token) {
		CoreLicenseType coreLicenseType = this.coreLicenseTypeService.getCoreLicenseTypeById(id);
		if (coreLicenseType == null) {
			throw new EntityNotFoundException("Role not found");
		}
		
		// Clone for audit
		CoreLicenseType oldLicenseType = new CoreLicenseType();
		BeanUtils.copyProperties(coreLicenseType, oldLicenseType);
		
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreLicenseType.class, coreLicenseTypeRequest);
		CoreLicenseType coreLicenseTypeFromObj = (CoreLicenseType) AppUtil.objectToClass(CoreLicenseType.class,
				coreLicenseTypeRequest);
		if (coreLicenseTypeFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		BeanUtils.copyProperties(coreLicenseTypeFromObj, coreLicenseType, propertiesToIgnore);
		this.coreLicenseTypeService.editCoreLicenseType(coreLicenseType);
		
		// Audit log
		auditor.audit(Action.UPDATE, "CoreLicenseType", coreLicenseType.getId(), user, "Updated license type");
		
		return coreLicenseType;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreLicenseType(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreLicenseType categ = this.coreLicenseTypeService.getCoreLicenseTypeById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
			this.coreLicenseTypeService.deleteCoreLicenseType(id);
			
			// Audit log
			auditor.audit(Action.DELETE, "CoreLicenseType", categ.getId(), ua, "Deleted license type");
		} else {
			throw new EntityNotFoundException("Action denied");
		}
	}
}
