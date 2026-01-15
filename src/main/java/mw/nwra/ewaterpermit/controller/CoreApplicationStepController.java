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
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreApplicationStep;
import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreApplicationStepService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/application-steps")
public class CoreApplicationStepController {

	@Autowired
	private CoreApplicationStepService coreApplicationStepService;

	@GetMapping(path = "")
	public List<CoreApplicationStep> getCoreApplicationSteps() {
		List<CoreApplicationStep> newsCategories = this.coreApplicationStepService.getAllCoreApplicationSteps();
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping("/license-type/{id}")
	public List<CoreApplicationStep> getCoreApplicationStepsByLicenseType(@PathVariable(name = "id") String id) {
		CoreLicenseType type = new CoreLicenseType();
		type.setId(id);
		List<CoreApplicationStep> coreApplicationSteps = this.coreApplicationStepService
				.getCoreApplicationStepByLicenseType(type);
		if (coreApplicationSteps.isEmpty()) {
			throw new EntityNotFoundException("Application steps not found");
		}
		return coreApplicationSteps;
	}

	@GetMapping(path = "/{id}")
	public CoreApplicationStep getCoreApplicationStepById(@PathVariable(name = "id") String id) {
		CoreApplicationStep coreApplicationStep = this.coreApplicationStepService.getCoreApplicationStepById(id);
		if (coreApplicationStep == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreApplicationStep;
	}

	@PostMapping(path = "")
	public CoreApplicationStep createCoreApplicationStep(@RequestBody Map<String, Object> coreApplicationStepRequest) {
		CoreApplicationStep coreApplicationStep = (CoreApplicationStep) AppUtil.objectToClass(CoreApplicationStep.class,
				coreApplicationStepRequest);
		if (coreApplicationStep == null) {
			throw new ForbiddenException("Could not create the coreApplicationStep");
		}
		return this.coreApplicationStepService.addCoreApplicationStep(coreApplicationStep);
	}

	@PutMapping(path = "/{id}")
	public CoreApplicationStep updateCoreApplicationStep(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreApplicationStepRequest) {
		CoreApplicationStep coreApplicationStep = this.coreApplicationStepService.getCoreApplicationStepById(id);
		if (coreApplicationStep == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreApplicationStep.class,
				coreApplicationStepRequest);
		CoreApplicationStep coreApplicationStepFromObj = (CoreApplicationStep) AppUtil
				.objectToClass(CoreApplicationStep.class, coreApplicationStepRequest);
		if (coreApplicationStepFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(coreApplicationStepFromObj, coreApplicationStep, propertiesToIgnore);
		this.coreApplicationStepService.editCoreApplicationStep(coreApplicationStep);
		return coreApplicationStep;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreApplicationStep(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreApplicationStep categ = this.coreApplicationStepService.getCoreApplicationStepById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreApplicationStepService.deleteCoreApplicationStep(id);
	}
}
