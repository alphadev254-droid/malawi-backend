package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_account_status")
@NamedQuery(name = "SysAccountStatus.findAll", query = "SELECT s FROM SysAccountStatus s")
public class SysAccountStatus extends BaseEntity {

	private String description;

	private String name;

	@JsonIgnore
	@OneToMany(mappedBy = "sysAccountStatus")
	private List<SysUserAccount> sysUserAccounts;

	public SysAccountStatus() {
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<SysUserAccount> getSysUserAccounts() {
		return this.sysUserAccounts;
	}

	public void setSysUserAccounts(List<SysUserAccount> sysUserAccounts) {
		this.sysUserAccounts = sysUserAccounts;
	}

	public SysUserAccount addSysUserAccount(SysUserAccount sysUserAccount) {
		getSysUserAccounts().add(sysUserAccount);
		sysUserAccount.setSysAccountStatus(this);

		return sysUserAccount;
	}

	public SysUserAccount removeSysUserAccount(SysUserAccount sysUserAccount) {
		getSysUserAccounts().remove(sysUserAccount);
		sysUserAccount.setSysAccountStatus(null);

		return sysUserAccount;
	}

}