package mw.nwra.ewaterpermit.constant;

public enum CustomError {
	UNKNOWN_USER("Unknown user"),
	INTERNAL_SERVER_ERROR("Request can not be fulfilled. Make sure all requests conforms to the API documentation"),
	RECORD_ALREADY_EXISTS(
			"Validation failed. A record already exists or the passed data does not contain required fields"),
	FAILED_VALIDATION("Validation failed");

	private String name;

	CustomError(String name) {
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
