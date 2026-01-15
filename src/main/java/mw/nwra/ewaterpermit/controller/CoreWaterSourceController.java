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
import mw.nwra.ewaterpermit.model.CoreWaterSource;
import mw.nwra.ewaterpermit.model.CoreWaterSourceType;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreWaterSourceService;
import mw.nwra.ewaterpermit.service.CoreWaterSourceTypeService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/water-sources", produces = MediaType.APPLICATION_JSON_VALUE)
public class CoreWaterSourceController {
	private final String OBJ = "water-sources";
	private final CoreWaterSourceService coreWaterSourceService;

	private final CoreWaterSourceTypeService coreWaterSourceTypeService;

	public CoreWaterSourceController(final CoreWaterSourceService coreWaterSourceService,
			final CoreWaterSourceTypeService coreWaterSourceTypeService) {
		this.coreWaterSourceService = coreWaterSourceService;
		this.coreWaterSourceTypeService = coreWaterSourceTypeService;
	}

	@GetMapping
	public List<CoreWaterSource> findAlls(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "query", defaultValue = "") String query) {
		List<CoreWaterSource> res = this.coreWaterSourceService.getAllCoreWaterSources(page, limit);
		if (res == null) {
			throw new EntityNotFoundException("wSources not found");
		}
		return res;
	}

	@GetMapping(path = "/{id}")
	public CoreWaterSource findById(@PathVariable(name = "id") String wSourceId) {
		CoreWaterSource wSource = this.coreWaterSourceService.getCoreWaterSourceById(wSourceId);
		if (wSource == null) {
			throw new EntityNotFoundException("wSource not found");
		}
		return wSource;
	}

	@GetMapping(path = "/{id}/type")
	public List<CoreWaterSource> findBySourceType(@PathVariable(name = "id") String wSourceId) {

		CoreWaterSourceType wSourceType = this.coreWaterSourceTypeService.getCoreWaterSourceTypeById(wSourceId);
		if (wSourceType == null) {
			throw new EntityNotFoundException("wSource not found");
		}
		List<CoreWaterSource> wSources = this.coreWaterSourceService.getBySourceType(wSourceType);
		return wSources;
	}

	@PostMapping
	public CoreWaterSource create(@RequestBody Map<String, Object> wSourceRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.CREATE.toString());

		CoreWaterSource wSource = (CoreWaterSource) AppUtil.objectToClass(CoreWaterSource.class, wSourceRequest);
		if (wSource == null) {
			throw new ForbiddenException("Could not create the wSource");
		}
		wSource.setDateCreated(new Timestamp(new Date().getTime()));
		return this.coreWaterSourceService.addCoreWaterSource(wSource);
	}

	@PutMapping(path = "/{id}")
	public CoreWaterSource update(@PathVariable(name = "id") String id, @RequestBody Map<String, Object> wSourceRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.UPDATE.toString());

		CoreWaterSource wSource = this.coreWaterSourceService.getCoreWaterSourceById(id);
		if (wSource == null) {
			throw new EntityNotFoundException("wSource not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreWaterSource.class, wSourceRequest);
		CoreWaterSource wSourceFromObj = (CoreWaterSource) AppUtil.objectToClass(CoreWaterSource.class, wSourceRequest);
		if (wSourceFromObj == null) {
			throw new ForbiddenException("Could not update the wSource wSource");
		}
		BeanUtils.copyProperties(wSourceFromObj, wSource, propertiesToIgnore);
		wSource.setDateUpdated(new Timestamp(new Date().getTime()));
		this.coreWaterSourceService.editCoreWaterSource(wSource);
		return wSource;
	}

	@DeleteMapping(path = "/{id}")
	public CoreWaterSource delete(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.DELETE.toString());

		CoreWaterSource wSource = this.coreWaterSourceService.getCoreWaterSourceById(id);
		if (wSource == null) {
			throw new EntityNotFoundException("wSource with id '" + id + "' not found");
		}
		this.coreWaterSourceService.deleteCoreWaterSource(id);
		return wSource;
	}

}
