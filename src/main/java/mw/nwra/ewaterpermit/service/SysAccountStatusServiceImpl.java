package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysAccountStatus;
import mw.nwra.ewaterpermit.repository.SysAccountStatusRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysAccountStatusService")
public class SysAccountStatusServiceImpl implements SysAccountStatusService {

	@Autowired
	private SysAccountStatusRepository sysAccountStatusRepository;

	@Override
	public List<SysAccountStatus> getAllSysAccountStatuses() {

		return this.sysAccountStatusRepository.findAll();
	}

	@Override
	public List<SysAccountStatus> getAllSysAccountStatuses(int page, int limit) {
		return this.sysAccountStatusRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "asc")).getContent();
	}

	@Override
	public SysAccountStatus getSysAccountStatusById(String sysAccountStatusId) {

		return this.sysAccountStatusRepository.findById(sysAccountStatusId).orElse(null);
	}

	@Override
	public void deleteSysAccountStatus(String sysAccountStatusId) {
		this.sysAccountStatusRepository.deleteById(sysAccountStatusId);
	}

	@Override
	public SysAccountStatus addSysAccountStatus(SysAccountStatus sysAccountStatus) {
		return this.sysAccountStatusRepository.saveAndFlush(sysAccountStatus);
	}

	@Override
	public SysAccountStatus editSysAccountStatus(SysAccountStatus sysAccountStatus) {
		return this.sysAccountStatusRepository.saveAndFlush(sysAccountStatus);
	}

	@Override
	public SysAccountStatus getSysAccountStatusByName(String name) {
		return this.sysAccountStatusRepository.findByName(name);
	}

}
