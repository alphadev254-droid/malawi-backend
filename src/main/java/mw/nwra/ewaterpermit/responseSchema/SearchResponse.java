package mw.nwra.ewaterpermit.responseSchema;

import java.io.Serializable;

public class SearchResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long count;
	private Object data;

	public SearchResponse(Long count, Object data) {
		super();
		this.count = count;
		this.data = data;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
