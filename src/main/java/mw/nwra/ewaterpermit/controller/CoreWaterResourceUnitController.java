package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
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
import mw.nwra.ewaterpermit.model.CoreWaterResourceUnit;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreWaterResourceUnitService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/water-resource-units", produces = MediaType.APPLICATION_JSON_VALUE)
public class CoreWaterResourceUnitController {
	private final String OBJ = "water-resource-units";
	private final CoreWaterResourceUnitService coreWruService;

	public CoreWaterResourceUnitController(final CoreWaterResourceUnitService coreWruService) {
		this.coreWruService = coreWruService;
	}

	@GetMapping
	public List<CoreWaterResourceUnit> findAlls(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "query", defaultValue = "") String query) {
		List<CoreWaterResourceUnit> res = this.coreWruService.getAllCoreWaterResourceUnits(page, limit);
		if (res == null) {
			throw new EntityNotFoundException("wras not found");
		}
		return res;
	}

	@GetMapping(path = "/{id}")
	public CoreWaterResourceUnit findById(@PathVariable(name = "id") String wruId) {
		CoreWaterResourceUnit wra = this.coreWruService.getCoreWaterResourceUnitById(wruId);
		if (wra == null) {
			throw new EntityNotFoundException("wra not found");
		}
		return wra;
	}

	@GetMapping(path = "/{id}/area")
	public List<CoreWaterResourceUnit> findByArea(@PathVariable(name = "id") String wraId) {
		List<CoreWaterResourceUnit> wra = this.coreWruService.getCoreWaterResourceUnitByArea(wraId);
		if (wra.isEmpty()) {
			throw new EntityNotFoundException("wra not found");
		}
		return wra;
	}

	@PostMapping
	public CoreWaterResourceUnit create(@RequestBody Map<String, Object> wraRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.CREATE.toString());

		CoreWaterResourceUnit wra = (CoreWaterResourceUnit) AppUtil.objectToClass(CoreWaterResourceUnit.class,
				wraRequest);
		if (wra == null) {
			throw new ForbiddenException("Could not create the wra");
		}
		wra.setDateCreated(new Timestamp(new Date().getTime()));
		return this.coreWruService.addCoreWaterResourceUnit(wra);
	}

	@PutMapping(path = "/{id}")
	public CoreWaterResourceUnit update(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> wraRequest, @RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.UPDATE.toString());

		CoreWaterResourceUnit wra = this.coreWruService.getCoreWaterResourceUnitById(id);
		if (wra == null) {
			throw new EntityNotFoundException("wra not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreWaterResourceUnit.class, wraRequest);
		CoreWaterResourceUnit wraFromObj = (CoreWaterResourceUnit) AppUtil.objectToClass(CoreWaterResourceUnit.class,
				wraRequest);
		if (wraFromObj == null) {
			throw new ForbiddenException("Could not update the wra wra");
		}
		BeanUtils.copyProperties(wraFromObj, wra, propertiesToIgnore);
		wra.setDateUpdated(new Timestamp(new Date().getTime()));
		this.coreWruService.editCoreWaterResourceUnit(wra);
		return wra;
	}

	@DeleteMapping(path = "/{id}")
	public CoreWaterResourceUnit delete(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.DELETE.toString());

		CoreWaterResourceUnit wra = this.coreWruService.getCoreWaterResourceUnitById(id);
		if (wra == null) {
			throw new EntityNotFoundException("wra with id '" + id + "' not found");
		}
		this.coreWruService.deleteCoreWaterResourceUnit(id);
		return wra;
	}

}
