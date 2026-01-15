package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.exception.UnauthorizedException;
import mw.nwra.ewaterpermit.model.SysConfig;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.SysConfigService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import org.springframework.web.bind.annotation.RequestHeader;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/configs")
public class SysConfigController {
	@Autowired
	private SysConfigService configService;
	
	@Autowired
	private Auditor auditor;

	@GetMapping(path = "")
	public List<SysConfig> getAllConfigurations(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<SysConfig> configs = this.configService.getAllSysConfigurations(page, limit);
		if (configs.isEmpty()) {
			throw new EntityNotFoundException("Configurations not found");
		}
		return configs;
	}

	@GetMapping(path = "/{id}")
	public SysConfig getConfigById(@PathVariable(name = "id") String configId) {
		SysConfig config = this.configService.getSysConfigById(configId);
		if (config == null) {
			throw new EntityNotFoundException("Config not found");
		}
		return config;
	}

	@PostMapping
	public SysConfig createConfig(@RequestBody Map<String, Object> configRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysConfig config = (SysConfig) AppUtil.objectToClass(SysConfig.class, configRequest);
		if (config == null) {
			throw new ForbiddenException("Could not create the config");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		config.setDateCreated(new Timestamp(new Date().getTime()));
		config = this.configService.createSysConfig(config);
		
		// Audit log
		auditor.audit(user, null, config, SysConfig.class, Action.CREATE.toString());
		
		return config;
	}

	@PutMapping(path = "/{id}")
	public SysConfig updateConfig(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> configRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysConfig config = this.configService.getSysConfigById(id);
		if (config == null) {
			throw new EntityNotFoundException("System configuration not found");
		}
		
		// Clone for audit
		SysConfig oldConfig = new SysConfig();
		BeanUtils.copyProperties(config, oldConfig);
		
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysConfig.class, configRequest);
		SysConfig configFromObj = (SysConfig) AppUtil.objectToClass(SysConfig.class, configRequest);
		if (configFromObj == null) {
			throw new ForbiddenException("Could not update the config");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		BeanUtils.copyProperties(configFromObj, config, propertiesToIgnore);
		config.setDateUpdated(new Timestamp(new Date().getTime()));
		config = this.configService.updateSysConfig(config);
		
		// Audit log
		auditor.audit(user, oldConfig, config, SysConfig.class, Action.UPDATE.toString());
		
		return config;
	}

	@GetMapping(path = "/accessdenied")
	public String accessDenied() {
		throw new UnauthorizedException("Operation denied");
	}
}
