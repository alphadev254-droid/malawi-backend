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
import mw.nwra.ewaterpermit.model.CoreLandRegime;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLandRegimeService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/land-regimes")
public class CoreLandRegimeController {

	@Autowired
	private CoreLandRegimeService coreLandRegimeService;

	@GetMapping(path = "")
	public List<CoreLandRegime> getCoreLandRegimes(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreLandRegime> newsCategories = this.coreLandRegimeService.getAllCoreLandRegimes(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreLandRegime getCoreLandRegimeById(@PathVariable(name = "id") String id) {
		CoreLandRegime coreLandRegime = this.coreLandRegimeService.getCoreLandRegimeById(id);
		if (coreLandRegime == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreLandRegime;
	}

	@PostMapping(path = "")
	public CoreLandRegime createCoreLandRegime(@RequestBody Map<String, Object> coreLandRegimeRequest) {
		CoreLandRegime coreLandRegime = (CoreLandRegime) AppUtil.objectToClass(CoreLandRegime.class,
				coreLandRegimeRequest);
		if (coreLandRegime == null) {
			throw new ForbiddenException("Could not create the coreLandRegime");
		}
		return this.coreLandRegimeService.addCoreLandRegime(coreLandRegime);
	}

	@PutMapping(path = "/{id}")
	public CoreLandRegime updateCoreLandRegime(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreLandRegimeRequest) {
		CoreLandRegime coreLandRegime = this.coreLandRegimeService.getCoreLandRegimeById(id);
		if (coreLandRegime == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreLandRegime.class, coreLandRegimeRequest);
		CoreLandRegime coreLandRegimeFromObj = (CoreLandRegime) AppUtil.objectToClass(CoreLandRegime.class,
				coreLandRegimeRequest);
		if (coreLandRegimeFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(coreLandRegimeFromObj, coreLandRegime, propertiesToIgnore);
		this.coreLandRegimeService.editCoreLandRegime(coreLandRegime);
		return coreLandRegime;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreLandRegime(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreLandRegime categ = this.coreLandRegimeService.getCoreLandRegimeById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreLandRegimeService.deleteCoreLandRegime(id);
	}
}
