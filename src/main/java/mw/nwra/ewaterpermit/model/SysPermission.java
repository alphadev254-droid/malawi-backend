package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_permission")
@NamedQuery(name = "SysPermission.findAll", query = "SELECT s FROM SysPermission s")
public class SysPermission extends BaseEntity {

	private String description;

	private String name;

	// bi-directional many-to-one association to SysUserGroupPermission
	@JsonIgnore
	@OneToMany(mappedBy = "sysPermission")
	private List<SysUserGroupPermission> sysUserGroupPermissions;

	public SysPermission() {
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

	public List<SysUserGroupPermission> getSysUserGroupPermissions() {
		return this.sysUserGroupPermissions;
	}

	public void setSysUserGroupPermissions(List<SysUserGroupPermission> sysUserGroupPermissions) {
		this.sysUserGroupPermissions = sysUserGroupPermissions;
	}

	public SysUserGroupPermission addSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission) {
		getSysUserGroupPermissions().add(sysUserGroupPermission);
		sysUserGroupPermission.setSysPermission(this);

		return sysUserGroupPermission;
	}

	public SysUserGroupPermission removeSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission) {
		getSysUserGroupPermissions().remove(sysUserGroupPermission);
		sysUserGroupPermission.setSysPermission(null);

		return sysUserGroupPermission;
	}

}