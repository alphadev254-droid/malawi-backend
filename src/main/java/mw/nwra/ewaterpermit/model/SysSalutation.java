package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_salutation")
@NamedQuery(name = "SysSalutation.findAll", query = "SELECT s FROM SysSalutation s")
public class SysSalutation extends BaseEntity {

	private String name;

	// bi-directional many-to-one association to SysUserAccount
	@JsonIgnore
	@OneToMany(mappedBy = "sysSalutation")
	private List<SysUserAccount> sysUserAccounts;

	public SysSalutation() {
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
		sysUserAccount.setSysSalutation(this);

		return sysUserAccount;
	}

	public SysUserAccount removeSysUserAccount(SysUserAccount sysUserAccount) {
		getSysUserAccounts().remove(sysUserAccount);
		sysUserAccount.setSysSalutation(null);

		return sysUserAccount;
	}

}