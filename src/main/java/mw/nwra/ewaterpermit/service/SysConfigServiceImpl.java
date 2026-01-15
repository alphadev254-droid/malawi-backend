package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysConfig;
import mw.nwra.ewaterpermit.repository.SysConfigRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysConfigService")
public class SysConfigServiceImpl implements SysConfigService {

	@Autowired
	private SysConfigRepository sysConfigRepository;

	@Override
	public List<SysConfig> getAllSysConfigurations() {
		return this.sysConfigRepository.findAll();
	}

	@Override
	public List<SysConfig> getAllSysConfigurations(int page, int limit) {
		return this.sysConfigRepository.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc"))
				.getContent();
	}

	@Override
	public SysConfig getSysConfigById(String sysConfigId) {
		return this.sysConfigRepository.findById(sysConfigId).orElse(null);
	}

	@Override
	public void deleteSysConfig(String sysConfigId) {
		this.sysConfigRepository.deleteById(sysConfigId);

	}

	@Override
	public SysConfig createSysConfig(SysConfig sysConfig) {
		return this.sysConfigRepository.saveAndFlush(sysConfig);
	}

	@Override
	public SysConfig updateSysConfig(SysConfig sysConfig) {
		return this.sysConfigRepository.saveAndFlush(sysConfig);
	}

	@Override
	public SysConfig getSystemConfig() {
		List<SysConfig> configs = this.sysConfigRepository.findAll();
		if (configs == null || configs.isEmpty()) {
			throw new RuntimeException("System configuration not found in database. Please ensure sys_config table has at least one row.");
		}
		return configs.get(0);
	}

}
