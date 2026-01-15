package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_user_group_permission")
@NamedQuery(name = "SysUserGroupPermission.findAll", query = "SELECT s FROM SysUserGroupPermission s")
public class SysUserGroupPermission extends BaseEntity {

	// bi-directional many-to-one association to SysObject
	@ManyToOne
	@JoinColumn(name = "object_id")
	private SysObject sysObject;

	// bi-directional many-to-one association to SysPermission
	@ManyToOne
	@JoinColumn(name = "permission_id")
	private SysPermission sysPermission;

	// bi-directional many-to-one association to SysUserGroup
	@ManyToOne
	@JoinColumn(name = "user_group_id")
	private SysUserGroup sysUserGroup;

	public SysUserGroupPermission() {
	}

	public SysObject getSysObject() {
		return this.sysObject;
	}

	public void setSysObject(SysObject sysObject) {
		this.sysObject = sysObject;
	}

	public SysPermission getSysPermission() {
		return this.sysPermission;
	}

	public void setSysPermission(SysPermission sysPermission) {
		this.sysPermission = sysPermission;
	}

	public SysUserGroup getSysUserGroup() {
		return this.sysUserGroup;
	}

	public void setSysUserGroup(SysUserGroup sysUserGroup) {
		this.sysUserGroup = sysUserGroup;
	}

}