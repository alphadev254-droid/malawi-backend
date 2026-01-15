package mw.nwra.ewaterpermit.requestSchema;

import java.io.Serializable;

public class FileUploadRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	private String data;
	private String name;
	private String caption;

	public FileUploadRequest() {
	}

	public FileUploadRequest(String data, String name, String caption) {
		this.data = data;
		this.name = name;
		this.caption = caption;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

}
