package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_object")
@NamedQuery(name = "SysObject.findAll", query = "SELECT s FROM SysObject s")
public class SysObject extends BaseEntity {

	private String description;

	private String name;

	// bi-directional many-to-one association to SysMenu
	@JsonIgnore
	@OneToMany(mappedBy = "sysObject")
	private List<SysMenu> sysMenus;

	// bi-directional many-to-one association to SysUserGroupPermission
	@JsonIgnore
	@OneToMany(mappedBy = "sysObject")
	private List<SysUserGroupPermission> sysUserGroupPermissions;

	public SysObject() {
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

	public List<SysMenu> getSysMenus() {
		return this.sysMenus;
	}

	public void setSysMenus(List<SysMenu> sysMenus) {
		this.sysMenus = sysMenus;
	}

	public SysMenu addSysMenus(SysMenu sysMenus) {
		getSysMenus().add(sysMenus);
		sysMenus.setSysObject(this);

		return sysMenus;
	}

	public SysMenu removeSysMenus(SysMenu sysMenus) {
		getSysMenus().remove(sysMenus);
		sysMenus.setSysObject(null);

		return sysMenus;
	}

	public List<SysUserGroupPermission> getSysUserGroupPermissions() {
		return this.sysUserGroupPermissions;
	}

	public void setSysUserGroupPermissions(List<SysUserGroupPermission> sysUserGroupPermissions) {
		this.sysUserGroupPermissions = sysUserGroupPermissions;
	}

	public SysUserGroupPermission addSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission) {
		getSysUserGroupPermissions().add(sysUserGroupPermission);
		sysUserGroupPermission.setSysObject(this);

		return sysUserGroupPermission;
	}

	public SysUserGroupPermission removeSysUserGroupPermission(SysUserGroupPermission sysUserGroupPermission) {
		getSysUserGroupPermissions().remove(sysUserGroupPermission);
		sysUserGroupPermission.setSysObject(null);

		return sysUserGroupPermission;
	}

}