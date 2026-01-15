package mw.nwra.ewaterpermit.constant;

public enum YesNo {
	YES("Y"), NO("N");

	private String name;

	YesNo(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
