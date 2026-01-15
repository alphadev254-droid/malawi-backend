package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysConfig;

public interface SysConfigService {
	public List<SysConfig> getAllSysConfigurations();

	public List<SysConfig> getAllSysConfigurations(int page, int limit);

	public SysConfig getSysConfigById(String sysConfigId);

	public void deleteSysConfig(String sysConfigId);

	public SysConfig createSysConfig(SysConfig sysConfig);

	public SysConfig updateSysConfig(SysConfig sysConfig);

	
	/**
	 * Get the system configuration (singleton row from sys_config table)
	 * @return The system configuration
	 * @throws RuntimeException if configuration is not found
	 */
	public SysConfig getSystemConfig();
}
