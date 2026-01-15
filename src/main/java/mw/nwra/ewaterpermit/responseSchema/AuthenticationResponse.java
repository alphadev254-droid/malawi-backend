package mw.nwra.ewaterpermit.responseSchema;

import java.io.Serializable;
import java.util.List;

import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.model.SysUserGroupPermission;

public class AuthenticationResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String token;
	private final SysUserAccount userAccount;
	private final List<SysUserGroupPermission> permissions;

	public AuthenticationResponse(String token, SysUserAccount userAccount, List<SysUserGroupPermission> permissions) {
		super();
		this.token = token;
		this.userAccount = userAccount;
		this.permissions = permissions;
	}

	public String getToken() {
		return token;
	}

	public SysUserAccount getUserAccount() {
		return userAccount;
	}

	public List<SysUserGroupPermission> getPermissions() {
		return permissions;
	}
}
