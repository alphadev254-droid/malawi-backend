package mw.nwra.ewaterpermit.requestSchema;

import java.io.Serializable;

public class OTPVerifyRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	private String otp;
	private String payload;

	public OTPVerifyRequest() {
		super();
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
}
