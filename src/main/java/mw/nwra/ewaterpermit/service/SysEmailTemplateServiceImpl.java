package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysEmailTemplate;
import mw.nwra.ewaterpermit.repository.SysEmailTemplateRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysEmailTemplateService")
public class SysEmailTemplateServiceImpl implements SysEmailTemplateService {
	@Autowired
	private SysEmailTemplateRepository sysEmailTemplateRepository;

	@Override
	public List<SysEmailTemplate> getAllSysEmailTemplates() {
		return this.sysEmailTemplateRepository.findAll();
	}

	@Override
	public List<SysEmailTemplate> getAllSysEmailTemplates(int page, int limit) {
		return this.sysEmailTemplateRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "desc"))
				.getContent();
	}

	@Override
	public List<SysEmailTemplate> getSysEmailTemplatesByNameAndStatus(String name, Short status) {
		return this.sysEmailTemplateRepository.findByNameIgnoreCaseAndStatus(name, status);
	}

	@Override
	public SysEmailTemplate getSysEmailTemplateById(String sysEmailTemplateId) {
		return this.sysEmailTemplateRepository.findById(sysEmailTemplateId).orElse(null);
	}

	@Override
	public void deleteSysEmailTemplate(String sysEmailTemplateId) {
		this.sysEmailTemplateRepository.deleteById(sysEmailTemplateId);
	}

	@Override
	public SysEmailTemplate addSysEmailTemplate(SysEmailTemplate sysEmailTemplate) {
		return this.sysEmailTemplateRepository.saveAndFlush(sysEmailTemplate);
	}

	@Override
	public SysEmailTemplate editSysEmailTemplate(SysEmailTemplate sysEmailTemplate) {
		return this.sysEmailTemplateRepository.saveAndFlush(sysEmailTemplate);
	}

}
