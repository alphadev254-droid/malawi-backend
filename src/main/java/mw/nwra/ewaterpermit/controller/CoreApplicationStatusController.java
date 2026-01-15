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
import mw.nwra.ewaterpermit.model.CoreApplicationStatus;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreApplicationStatusService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/application-statuses")
public class CoreApplicationStatusController {

	@Autowired
	private CoreApplicationStatusService coreApplicationStatusService;
	
	@Autowired
	private Auditor auditor;

	@GetMapping(path = "")
	public List<CoreApplicationStatus> getCoreApplicationStatuss(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreApplicationStatus> newsCategories = this.coreApplicationStatusService
				.getAllCoreApplicationStatuses(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreApplicationStatus getCoreApplicationStatusById(@PathVariable(name = "id") String id) {
		CoreApplicationStatus coreApplicationStatus = this.coreApplicationStatusService
				.getCoreApplicationStatusById(id);
		if (coreApplicationStatus == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreApplicationStatus;
	}

	@PostMapping(path = "")
	public CoreApplicationStatus createCoreApplicationStatus(
			@RequestBody Map<String, Object> coreApplicationStatusRequest,
			@RequestHeader(name = "Authorization") String token) {
		CoreApplicationStatus coreApplicationStatus = (CoreApplicationStatus) AppUtil
				.objectToClass(CoreApplicationStatus.class, coreApplicationStatusRequest);
		if (coreApplicationStatus == null) {
			throw new ForbiddenException("Could not create the coreApplicationStatus");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		coreApplicationStatus = this.coreApplicationStatusService.addCoreApplicationStatus(coreApplicationStatus);
		
		// Audit log
		auditor.audit(Action.CREATE, "CoreApplicationStatus", coreApplicationStatus.getId(), user, "Created application status");
		
		return coreApplicationStatus;
	}

	@PutMapping(path = "/{id}")
	public CoreApplicationStatus updateCoreApplicationStatus(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreApplicationStatusRequest,
			@RequestHeader(name = "Authorization") String token) {
		CoreApplicationStatus coreApplicationStatus = this.coreApplicationStatusService
				.getCoreApplicationStatusById(id);
		if (coreApplicationStatus == null) {
			throw new EntityNotFoundException("Role not found");
		}
		
		// Clone for audit
		CoreApplicationStatus oldStatus = new CoreApplicationStatus();
		BeanUtils.copyProperties(coreApplicationStatus, oldStatus);
		
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreApplicationStatus.class,
				coreApplicationStatusRequest);
		CoreApplicationStatus coreApplicationStatusFromObj = (CoreApplicationStatus) AppUtil
				.objectToClass(CoreApplicationStatus.class, coreApplicationStatusRequest);
		if (coreApplicationStatusFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		BeanUtils.copyProperties(coreApplicationStatusFromObj, coreApplicationStatus, propertiesToIgnore);
		this.coreApplicationStatusService.editCoreApplicationStatus(coreApplicationStatus);
		
		// Audit log
		auditor.audit(Action.UPDATE, "CoreApplicationStatus", coreApplicationStatus.getId(), user, "Updated application status");
		
		return coreApplicationStatus;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreApplicationStatus(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreApplicationStatus categ = this.coreApplicationStatusService.getCoreApplicationStatusById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
			this.coreApplicationStatusService.deleteCoreApplicationStatus(id);
			
			// Audit log
			auditor.audit(Action.DELETE, "CoreApplicationStatus", categ.getId(), ua, "Deleted application status");
		} else {
			throw new EntityNotFoundException("Action denied");
		}
	}
}
