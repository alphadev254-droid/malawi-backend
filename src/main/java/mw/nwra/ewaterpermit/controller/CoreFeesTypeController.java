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

import mw.nwra.ewaterpermit.constant.Action;
import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreFeesType;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreFeesTypeService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;



@RestController
@RequestMapping(value = "/v1/fees-types")
public class CoreFeesTypeController {

	@Autowired
	private CoreFeesTypeService coreFeesTypeService;
	
	@Autowired
	private Auditor auditor;

	@GetMapping(path = "")
	public List<CoreFeesType> getCoreFeesTypes(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreFeesType> newsCategories = this.coreFeesTypeService.getAllCoreFeesTypes(page, limit);
		if (newsCategories == null) {
			return java.util.Collections.emptyList();
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreFeesType getCoreFeesTypeById(@PathVariable(name = "id") String id) {
		CoreFeesType coreFeesType = this.coreFeesTypeService.getCoreFeesTypeById(id);
		if (coreFeesType == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreFeesType;
	}

	@PostMapping(path = "")
	public CoreFeesType createCoreFeesType(@RequestBody Map<String, Object> coreFeesTypeRequest,
			@RequestHeader(name = "Authorization") String token) {
		CoreFeesType coreFeesType = (CoreFeesType) AppUtil.objectToClass(CoreFeesType.class, coreFeesTypeRequest);
		if (coreFeesType == null) {
			throw new ForbiddenException("Could not create the coreFeesType");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		coreFeesType = this.coreFeesTypeService.addCoreFeesType(coreFeesType);
		
		// Audit log
		auditor.audit(user, null, coreFeesType, CoreFeesType.class, Action.CREATE.toString());
		
		return coreFeesType;
	}

	@PutMapping(path = "/{id}")
	public CoreFeesType updateCoreFeesType(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreFeesTypeRequest,
			@RequestHeader(name = "Authorization") String token) {
		CoreFeesType coreFeesType = this.coreFeesTypeService.getCoreFeesTypeById(id);
		if (coreFeesType == null) {
			throw new EntityNotFoundException("Role not found");
		}
		
		// Clone for audit
		CoreFeesType oldFeesType = new CoreFeesType();
		BeanUtils.copyProperties(coreFeesType, oldFeesType);
		
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreFeesType.class, coreFeesTypeRequest);
		CoreFeesType coreFeesTypeFromObj = (CoreFeesType) AppUtil.objectToClass(CoreFeesType.class,
				coreFeesTypeRequest);
		if (coreFeesTypeFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		BeanUtils.copyProperties(coreFeesTypeFromObj, coreFeesType, propertiesToIgnore);
		this.coreFeesTypeService.editCoreFeesType(coreFeesType);
		
		// Audit log
		auditor.audit(user, oldFeesType, coreFeesType, CoreFeesType.class, Action.UPDATE.toString());
		
		return coreFeesType;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreFeesType(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreFeesType categ = this.coreFeesTypeService.getCoreFeesTypeById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
			this.coreFeesTypeService.deleteCoreFeesType(id);
			
			// Audit log
			auditor.audit(ua, categ, null, CoreFeesType.class, Action.DELETE.toString());
		} else {
			throw new EntityNotFoundException("Action denied");
		}
	}
}
