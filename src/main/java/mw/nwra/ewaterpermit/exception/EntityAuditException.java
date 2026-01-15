package mw.nwra.ewaterpermit.exception;

public class EntityAuditException extends Exception {

	private static final long serialVersionUID = -3388362064720239932L;

	public EntityAuditException() {
		super();
	}

	public EntityAuditException(String message) {
		super(message);
	}

	public EntityAuditException(Throwable cause) {
		super(cause);
	}

	public EntityAuditException(String message, Throwable cause) {
		super(message, cause);
	}

	public EntityAuditException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
