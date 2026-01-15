package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;

public interface SysUserAccountService {
	public List<SysUserAccount> getAllSysUserAccounts();

	public SearchResponse getAllSysUserAccounts(int page, int limit, String query);

	public SysUserAccount getSysUserAccountById(String sysUserAccountId);

	public SysUserAccount getSysUserAccountByUsername(String username);

	public SysUserAccount getSysUserAccountByUsernameWithAssociations(String username);

	public SysUserAccount getSysUserAccountByEmailAddress(String emailAddress);

	public SysUserAccount getSysUserAccountByPhoneNumber(String phoneNumber);

	public void deleteSysUserAccount(String sysUserAccountId);

	public SysUserAccount createSysUserAccount(SysUserAccount sysUserAccount);

	public SysUserAccount updateSysUserAccount(SysUserAccount sysUserAccount);

	public long countAll();

	/**
	 * Optimized method to get officers by role - prevents N+1 queries
	 */
	public List<SysUserAccount> getOfficersByRoleOptimized(String roleOrGroupName);

	/**
	 * Get all users with associations loaded efficiently
	 */
	public List<SysUserAccount> getAllSysUserAccountsWithAssociations();

	/**
	 * Get all user group names for debugging
	 */
	public List<String> getAllUserGroupNames();
}
