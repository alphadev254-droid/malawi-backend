package mw.nwra.ewaterpermit.constant;

public enum SysAccountStatusValue {
	ACTIVE("ACTIVE"), DEACTIVATED("DEACTIVATED"), CONFIRM_ACCOUNT("CONFIRM_ACCOUNT"),
	TEMPORARILY_DISABLED("TEMPORARILY_DISABLED"),;

	public final String name;

	private SysAccountStatusValue(String name) {
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
