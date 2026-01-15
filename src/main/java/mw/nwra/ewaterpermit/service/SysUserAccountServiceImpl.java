package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.repository.SysUserAccountRepository;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysUserAccountService")
public class SysUserAccountServiceImpl implements SysUserAccountService {

	@Autowired(required = true)
	SysUserAccountRepository userAccountRepository;

	@Override
	public List<SysUserAccount> getAllSysUserAccounts() {
		return this.userAccountRepository.findAll();
	}

	@Override
	public SearchResponse getAllSysUserAccounts(int page, int limit, String query) {
//		return this.userAccountRepository.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc"))
//				.getContent();

		Page<SysUserAccount> res = this.userAccountRepository.findAll(query,
				AppUtil.getPageRequest(page, limit, "lastName", "asc"));
		return res.getContent().isEmpty() ? null : new SearchResponse(res.getTotalElements(), res.getContent());
	}

	@Override
	public SysUserAccount getSysUserAccountById(String sysUserAccountId) {
		return this.userAccountRepository.findById(sysUserAccountId).orElse(null);
	}

	@Override
	public SysUserAccount getSysUserAccountByUsername(String username) {
		return this.userAccountRepository.findByUsername(username);
	}

	@Override
	public SysUserAccount getSysUserAccountByUsernameWithAssociations(String username) {
		return this.userAccountRepository.findByUsernameWithAssociations(username);
	}

	@Override
	public SysUserAccount getSysUserAccountByEmailAddress(String emailAddress) {
		return this.userAccountRepository.findByEmailAddress(emailAddress);
	}

	@Override
	public SysUserAccount getSysUserAccountByPhoneNumber(String phoneNumber) {
		return this.userAccountRepository.findByPhoneNumber(phoneNumber);
	}

	@Override
	public void deleteSysUserAccount(String sysUserAccountId) {
		this.userAccountRepository.deleteById(sysUserAccountId);
	}

	@Override
	public SysUserAccount createSysUserAccount(SysUserAccount sysUserAccount) {
		return this.userAccountRepository.saveAndFlush(sysUserAccount);
	}

	@Override
	public SysUserAccount updateSysUserAccount(SysUserAccount sysUserAccount) {
		// Ensure date_created is never null during updates
		if (sysUserAccount.getDateCreated() == null) {
			// Try to get the existing record to preserve date_created
			SysUserAccount existing = this.userAccountRepository.findById(sysUserAccount.getId()).orElse(null);
			if (existing != null && existing.getDateCreated() != null) {
				sysUserAccount.setDateCreated(existing.getDateCreated());
			} else {
				// Fallback: set current timestamp
				sysUserAccount.setDateCreated(new java.sql.Timestamp(new java.util.Date().getTime()));
			}
		}
		return this.userAccountRepository.saveAndFlush(sysUserAccount);
	}

	@Override
	public long countAll() {
		return this.userAccountRepository.count();
	}

	@Override
	public List<SysUserAccount> getOfficersByRoleOptimized(String roleOrGroupName) {
		return this.userAccountRepository.findOfficersByRoleOptimized(roleOrGroupName);
	}

	@Override
	public List<SysUserAccount> getAllSysUserAccountsWithAssociations() {
		return this.userAccountRepository.findAllWithAssociations();
	}

	@Override
	public List<String> getAllUserGroupNames() {
		return this.userAccountRepository.findAllUserGroupNames();
	}

}
