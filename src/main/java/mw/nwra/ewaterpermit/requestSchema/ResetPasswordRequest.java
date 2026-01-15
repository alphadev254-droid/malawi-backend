package mw.nwra.ewaterpermit.requestSchema;

import java.io.Serializable;

public class ResetPasswordRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	private String password;
	private String token;

	public ResetPasswordRequest() {
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}