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
import mw.nwra.ewaterpermit.model.CoreWaterUse;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreWaterUseService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/water-uses", produces = MediaType.APPLICATION_JSON_VALUE)
public class CoreWaterUseController {
	private final String OBJ = "water-uses";
	private final CoreWaterUseService coreWaterUseService;

	public CoreWaterUseController(final CoreWaterUseService coreWaterUseService) {
		this.coreWaterUseService = coreWaterUseService;
	}

	@GetMapping
	public List<CoreWaterUse> findAlls(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "query", defaultValue = "") String query) {
		List<CoreWaterUse> res = this.coreWaterUseService.getAllCoreWaterUses(page, limit);
		if (res == null) {
			throw new EntityNotFoundException("wUses not found");
		}
		return res;
	}

	@GetMapping(path = "/{id}")
	public CoreWaterUse findById(@PathVariable(name = "id") String wUseId) {
		CoreWaterUse wUse = this.coreWaterUseService.getCoreWaterUseById(wUseId);
		if (wUse == null) {
			throw new EntityNotFoundException("wUse not found");
		}
		return wUse;
	}

	@PostMapping
	public CoreWaterUse create(@RequestBody Map<String, Object> wUseRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.CREATE.toString());

		CoreWaterUse wUse = (CoreWaterUse) AppUtil.objectToClass(CoreWaterUse.class, wUseRequest);
		if (wUse == null) {
			throw new ForbiddenException("Could not create the wUse");
		}
		wUse.setDateCreated(new Timestamp(new Date().getTime()));
		return this.coreWaterUseService.addCoreWaterUse(wUse);
	}

	@PutMapping(path = "/{id}")
	public CoreWaterUse update(@PathVariable(name = "id") String id, @RequestBody Map<String, Object> wUseRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.UPDATE.toString());

		CoreWaterUse wUse = this.coreWaterUseService.getCoreWaterUseById(id);
		if (wUse == null) {
			throw new EntityNotFoundException("wUse not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreWaterUse.class, wUseRequest);
		CoreWaterUse wUseFromObj = (CoreWaterUse) AppUtil.objectToClass(CoreWaterUse.class, wUseRequest);
		if (wUseFromObj == null) {
			throw new ForbiddenException("Could not update the wUse wUse");
		}
		BeanUtils.copyProperties(wUseFromObj, wUse, propertiesToIgnore);
		wUse.setDateUpdated(new Timestamp(new Date().getTime()));
		this.coreWaterUseService.editCoreWaterUse(wUse);
		return wUse;
	}

	@DeleteMapping(path = "/{id}")
	public CoreWaterUse delete(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.DELETE.toString());

		CoreWaterUse wUse = this.coreWaterUseService.getCoreWaterUseById(id);
		if (wUse == null) {
			throw new EntityNotFoundException("wUse with id '" + id + "' not found");
		}
		this.coreWaterUseService.deleteCoreWaterUse(id);
		return wUse;
	}

}
