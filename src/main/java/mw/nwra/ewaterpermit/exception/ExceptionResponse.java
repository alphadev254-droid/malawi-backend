package mw.nwra.ewaterpermit.exception;

import java.util.Date;

public class ExceptionResponse {
	private String code;
	private String message;
	private String details;
	private Date timestamp;

	public ExceptionResponse() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ExceptionResponse(String code, String message, String details, Date timestamp) {
		super();
		this.code = code;
		this.message = message;
		this.details = details;
		this.timestamp = timestamp;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

}
