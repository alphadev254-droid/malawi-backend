package mw.nwra.ewaterpermit.service;

import java.sql.Timestamp;
import java.util.List;

import mw.nwra.ewaterpermit.model.SysUserAccountActivation;

public interface SysUserAccountActivationService {
	public List<SysUserAccountActivation> getAllSysUserAccountActivations();

	public List<SysUserAccountActivation> getAllSysUserAccountActivations(int page, int limit);

	public SysUserAccountActivation getSysUserAccountActivationByTokenAndDate(String token, Timestamp dateTimeNow);

	public SysUserAccountActivation getUserAccountActivationByTokenAndDateAndEmail(String token, Timestamp dateTimeNow,
			String email);

	public SysUserAccountActivation getSysUserAccountActivationById(String sysUserAccountActivationId);

	public void deleteSysUserAccountActivation(String sysUserAccountActivationId);

	public SysUserAccountActivation addSysUserAccountActivation(SysUserAccountActivation sysUserAccountActivation);

	public SysUserAccountActivation editSysUserAccountActivation(SysUserAccountActivation sysUserAccountActivation);

	public SysUserAccountActivation getSysUserAccountActivationByToken(String token);
}
