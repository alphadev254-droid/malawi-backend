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
import mw.nwra.ewaterpermit.model.SysEmailTemplate;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.SysEmailTemplateService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/sys-email-templates")
public class SysEmailTemplateController {

	@Autowired
	private SysEmailTemplateService sysEmailTemplateService;

	@GetMapping(path = "")
	public List<SysEmailTemplate> getSysEmailTemplates(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<SysEmailTemplate> newsCategories = this.sysEmailTemplateService.getAllSysEmailTemplates(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public SysEmailTemplate getSysEmailTemplateById(@PathVariable(name = "id") String id) {
		SysEmailTemplate sysEmailTemplate = this.sysEmailTemplateService.getSysEmailTemplateById(id);
		if (sysEmailTemplate == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return sysEmailTemplate;
	}

	@PostMapping(path = "")
	public SysEmailTemplate createSysEmailTemplate(@RequestBody Map<String, Object> sysEmailTemplateRequest) {
		SysEmailTemplate sysEmailTemplate = (SysEmailTemplate) AppUtil.objectToClass(SysEmailTemplate.class,
				sysEmailTemplateRequest);
		if (sysEmailTemplate == null) {
			throw new ForbiddenException("Could not create the Email Template");
		}
//		System.out.println(sysEmailTemplate.toString());
		return this.sysEmailTemplateService.addSysEmailTemplate(sysEmailTemplate);
	}

	@PutMapping(path = "/{id}")
	public SysEmailTemplate updateSysEmailTemplate(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> sysEmailTemplateRequest) {
		SysEmailTemplate sysEmailTemplate = this.sysEmailTemplateService.getSysEmailTemplateById(id);
		if (sysEmailTemplate == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysEmailTemplate.class, sysEmailTemplateRequest);
		SysEmailTemplate sysEmailTemplateFromObj = (SysEmailTemplate) AppUtil.objectToClass(SysEmailTemplate.class,
				sysEmailTemplateRequest);
		if (sysEmailTemplateFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(sysEmailTemplateFromObj, sysEmailTemplate, propertiesToIgnore);
		this.sysEmailTemplateService.editSysEmailTemplate(sysEmailTemplate);
		return sysEmailTemplate;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysEmailTemplate(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysEmailTemplate categ = this.sysEmailTemplateService.getSysEmailTemplateById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.sysEmailTemplateService.deleteSysEmailTemplate(id);
	}
}
