package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysDisposableDomain;

public interface SysDisposableDomainService {
	public List<SysDisposableDomain> getAllSysDisposableDomains();

	public List<SysDisposableDomain> getAllSysDisposableDomains(int page, int limit);

	public SysDisposableDomain getSysDisposableDomainById(String disposableDomainId);

	public SysDisposableDomain getSysDisposableDomainByName(String disposableDomain);

	public void deleteSysDisposableDomain(String disposableDomainId);

	public SysDisposableDomain addSysDisposableDomain(SysDisposableDomain disposableDomain);

	public SysDisposableDomain editSysDisposableDomain(SysDisposableDomain disposableDomain);
}
