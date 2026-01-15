package mw.nwra.ewaterpermit.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringStyle;

public class AuditingToStringStyle extends ToStringStyle {

	private static final long serialVersionUID = -2321349694107264502L;

	public AuditingToStringStyle() {
		super();

		this.setUseShortClassName(true);
		this.setUseIdentityHashCode(false);
		this.setContentStart("{");
		this.setContentEnd("}");
		this.setNullText("");
	}

	@Override
	protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
		if (value instanceof Date) {
			value = new SimpleDateFormat("yyyy-MM-dd").format(value);
		}

		buffer.append(value);
	}
}
