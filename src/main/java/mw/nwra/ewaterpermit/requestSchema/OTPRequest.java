package mw.nwra.ewaterpermit.requestSchema;

import java.io.Serializable;

import mw.nwra.ewaterpermit.constant.OTPMethod;

public class OTPRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	private OTPMethod method;
	private String payload;

	public OTPRequest() {
		super();
	}

	public OTPMethod getMethod() {
		return method;
	}

	public void setMethod(OTPMethod method) {
		this.method = method;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
}
