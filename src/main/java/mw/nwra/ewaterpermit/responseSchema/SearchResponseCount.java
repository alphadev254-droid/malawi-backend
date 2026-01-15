package mw.nwra.ewaterpermit.responseSchema;

import java.io.Serializable;

public class SearchResponseCount implements Serializable {
	private static final long serialVersionUID = 1L;
	private String id;
	private String title;
	private String count;

	public SearchResponseCount(String id, String title, String count) {
		super();
		this.id = id;
		this.title = title;
		this.count = count;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCount() {
		return count;
	}

	public void setCount(String count) {
		this.count = count;
	}

}
