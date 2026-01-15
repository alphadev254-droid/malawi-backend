package mw.nwra.ewaterpermit.constant;

public enum TemplateName {
	PASSWORD_RESET("PASSWORD_RESET"), TOKEN_VERIFICATION("TOKEN_VERIFICATION"),
	ACCOUNT_CONFIRMATION("ACCOUNT_CONFIRMATION");

	public final String name;

	private TemplateName(String name) {
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
