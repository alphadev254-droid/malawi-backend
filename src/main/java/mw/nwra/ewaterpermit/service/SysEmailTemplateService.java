package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysEmailTemplate;

public interface SysEmailTemplateService {
	public List<SysEmailTemplate> getAllSysEmailTemplates();

	public List<SysEmailTemplate> getAllSysEmailTemplates(int page, int limit);

	public List<SysEmailTemplate> getSysEmailTemplatesByNameAndStatus(String name, Short status);

	public SysEmailTemplate getSysEmailTemplateById(String sysEmailTemplateId);

	public void deleteSysEmailTemplate(String sysEmailTemplateId);

	public SysEmailTemplate addSysEmailTemplate(SysEmailTemplate sysEmailTemplate);

	public SysEmailTemplate editSysEmailTemplate(SysEmailTemplate sysEmailTemplate);
}
