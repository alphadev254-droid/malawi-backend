package mw.nwra.ewaterpermit.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysUserAccountActivation;
import mw.nwra.ewaterpermit.repository.SysUserAccountActivationRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysUserAccountActivationService")
public class SysUserAccountActivationServiceImpl implements SysUserAccountActivationService {

	@Autowired
	private SysUserAccountActivationRepository sysUserAccountActivationRepository;

	@Override
	public List<SysUserAccountActivation> getAllSysUserAccountActivations() {
		return this.sysUserAccountActivationRepository.findAll();
	}

	@Override
	public List<SysUserAccountActivation> getAllSysUserAccountActivations(int page, int limit) {
		return this.sysUserAccountActivationRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "desc"))
				.getContent();
	}

	@Override
	public SysUserAccountActivation getSysUserAccountActivationByTokenAndDate(String token, Timestamp dateTimeNow) {
		return this.sysUserAccountActivationRepository.findUnconfirmedRegistration(token, dateTimeNow);
	}

	@Override
	public SysUserAccountActivation getSysUserAccountActivationById(String sysUserAccountActivationId) {
		return this.sysUserAccountActivationRepository.findById(sysUserAccountActivationId).orElse(null);
	}

	@Override
	public void deleteSysUserAccountActivation(String sysUserAccountActivationId) {
		this.sysUserAccountActivationRepository.deleteById(sysUserAccountActivationId);
	}

	@Override
	public SysUserAccountActivation addSysUserAccountActivation(SysUserAccountActivation sysUserAccountActivation) {
		return this.sysUserAccountActivationRepository.saveAndFlush(sysUserAccountActivation);
	}

	@Override
	public SysUserAccountActivation editSysUserAccountActivation(SysUserAccountActivation sysUserAccountActivation) {
		return this.sysUserAccountActivationRepository.saveAndFlush(sysUserAccountActivation);
	}

	@Override
	public SysUserAccountActivation getUserAccountActivationByTokenAndDateAndEmail(String token, Timestamp dateTimeNow,
			String email) {
		return this.sysUserAccountActivationRepository.findUnconfirmedRegistration(token, dateTimeNow, email);
	}

	@Override
	public SysUserAccountActivation getSysUserAccountActivationByToken(String token) {
		return this.sysUserAccountActivationRepository.findByToken(token);
	}

}
