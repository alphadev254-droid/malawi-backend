package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_user_group")
@NamedQuery(name = "SysUserGroup.findAll", query = "SELECT s FROM SysUserGroup s")
public class SysUserGroup extends BaseEntity {

	private String description;

	private String name;

	@JsonIgnore
	@OneToMany(mappedBy = "sysUserGroup")
	private List<SysUserAccount> sysUserAccounts;

	@JsonIgnore
	@OneToMany(mappedBy = "sysUserGroup")
	private List<SysUserGroupPermission> sysUserGroupPermissions;

	public SysUserGroup() {
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
		sysUserAccount.setSysUserGroup(this);

		return sysUserAccount;
	}

	public SysUserAccount removeSysUserAccount(SysUserAccount sysUserAccount) {
		getSysUserAccounts().remove(sysUserAccount);
		sysUserAccount.setSysUserGroup(null);

		return sysUserAccount;
	}

	public List<SysUserGroupPermission> getSysUserGroupPermissions() {
		return this.sysUserGroupPermissions;
	}

	public void setSysUserGroupPermissions(List<SysUserGroupPermission> sysUserGroupPermissions) {
		this.sysUserGroupPermissions = sysUserGroupPermissions;
	}

	public SysUserGroupPermission addSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission) {
		getSysUserGroupPermissions().add(sysUserGroupPermission);
		sysUserGroupPermission.setSysUserGroup(this);

		return sysUserGroupPermission;
	}

	public SysUserGroupPermission removeSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission) {
		getSysUserGroupPermissions().remove(sysUserGroupPermission);
		sysUserGroupPermission.setSysUserGroup(null);

		return sysUserGroupPermission;
	}

}