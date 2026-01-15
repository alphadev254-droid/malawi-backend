package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysDisposableDomain;
import mw.nwra.ewaterpermit.repository.SysDisposableDomainRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysDisposableDomainService")
public class SysDisposableDomainServiceImpl implements SysDisposableDomainService {

	@Autowired
	private SysDisposableDomainRepository disposableDomainRepository;

	@Override
	public List<SysDisposableDomain> getAllSysDisposableDomains() {

		return this.disposableDomainRepository.findAll();
	}

	@Override
	public List<SysDisposableDomain> getAllSysDisposableDomains(int page, int limit) {
		return this.disposableDomainRepository.findAll(AppUtil.getPageRequest(page, limit, "auditDate", "desc"))
				.getContent();
	}

	@Override
	public SysDisposableDomain getSysDisposableDomainById(String disposableDomainId) {

		return this.disposableDomainRepository.findById(disposableDomainId).orElse(null);
	}

	@Override
	public void deleteSysDisposableDomain(String disposableDomainId) {
		this.disposableDomainRepository.deleteById(disposableDomainId);
	}

	@Override
	public SysDisposableDomain addSysDisposableDomain(SysDisposableDomain disposableDomain) {
		return this.disposableDomainRepository.saveAndFlush(disposableDomain);
	}

	@Override
	public SysDisposableDomain editSysDisposableDomain(SysDisposableDomain disposableDomain) {
		return this.disposableDomainRepository.saveAndFlush(disposableDomain);
	}

	@Override
	public SysDisposableDomain getSysDisposableDomainByName(String disposableDomain) {
		return this.disposableDomainRepository.findByNameIgnoreCase(disposableDomain);
	}

}
