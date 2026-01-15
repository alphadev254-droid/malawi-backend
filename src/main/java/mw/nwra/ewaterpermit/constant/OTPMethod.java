package mw.nwra.ewaterpermit.constant;

public enum OTPMethod {
	sms("sms"), call("call"), email("email");

	public final String name;

	private OTPMethod(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
