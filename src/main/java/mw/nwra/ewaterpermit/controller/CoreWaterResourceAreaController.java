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
import mw.nwra.ewaterpermit.model.CoreWaterResourceArea;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreWaterResourceAreaService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/water-resource-areas", produces = MediaType.APPLICATION_JSON_VALUE)
public class CoreWaterResourceAreaController {
	private final String OBJ = "water-resource-areas";
	private final CoreWaterResourceAreaService coreWraService;

	public CoreWaterResourceAreaController(final CoreWaterResourceAreaService coreWraService) {
		this.coreWraService = coreWraService;
	}

	@GetMapping
	public List<CoreWaterResourceArea> findAlls(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "25") int limit,
			@RequestParam(value = "query", defaultValue = "") String query,
			@RequestParam(value = "all", defaultValue = "false") boolean all) {
		
		// If 'all' parameter is true, return all records without pagination
		if (all) {
			List<CoreWaterResourceArea> res = this.coreWraService.getAllCoreWaterResourceAreas();
			return res != null ? res : List.of();
		}
		
		// Allow up to 25 items per request
		if (limit > 25) {
			limit = 25;
		}

		// Fetch the requested data
		List<CoreWaterResourceArea> res = this.coreWraService.getAllCoreWaterResourceAreas(page, limit);
		if (res == null) {
			return List.of();
		}
		return res;
	}

	@GetMapping(path = "/{id}")
	public CoreWaterResourceArea findById(@PathVariable(name = "id") String wraId) {
		CoreWaterResourceArea wra = this.coreWraService.getCoreWaterResourceAreaById(wraId);
		if (wra == null) {
			throw new EntityNotFoundException("wra not found");
		}
		return wra;
	}

	@PostMapping
	public CoreWaterResourceArea create(@RequestBody Map<String, Object> wraRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.CREATE.toString());

		CoreWaterResourceArea wra = (CoreWaterResourceArea) AppUtil.objectToClass(CoreWaterResourceArea.class,
				wraRequest);
		if (wra == null) {
			throw new ForbiddenException("Could not create the wra");
		}
		wra.setDateCreated(new Timestamp(new Date().getTime()));
		return this.coreWraService.addCoreWaterResourceArea(wra);
	}

	@PutMapping(path = "/{id}")
	public CoreWaterResourceArea update(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> wraRequest, @RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.UPDATE.toString());

		CoreWaterResourceArea wra = this.coreWraService.getCoreWaterResourceAreaById(id);
		if (wra == null) {
			throw new EntityNotFoundException("wra not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreWaterResourceArea.class, wraRequest);
		CoreWaterResourceArea wraFromObj = (CoreWaterResourceArea) AppUtil.objectToClass(CoreWaterResourceArea.class,
				wraRequest);
		if (wraFromObj == null) {
			throw new ForbiddenException("Could not update the wra wra");
		}
		BeanUtils.copyProperties(wraFromObj, wra, propertiesToIgnore);
		wra.setDateUpdated(new Timestamp(new Date().getTime()));
		this.coreWraService.editCoreWaterResourceArea(wra);
		return wra;
	}

	@DeleteMapping(path = "/{id}")
	public CoreWaterResourceArea delete(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.DELETE.toString());

		CoreWaterResourceArea wra = this.coreWraService.getCoreWaterResourceAreaById(id);
		if (wra == null) {
			throw new EntityNotFoundException("wra with id '" + id + "' not found");
		}
		this.coreWraService.deleteCoreWaterResourceArea(id);
		return wra;
	}

}
