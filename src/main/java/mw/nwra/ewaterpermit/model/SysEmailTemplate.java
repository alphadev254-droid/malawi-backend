package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_email_template")
@NamedQuery(name = "SysEmailTemplate.findAll", query = "SELECT s FROM SysEmailTemplate s")
public class SysEmailTemplate extends BaseEntity {

	private String name;

	private short status;

	@Lob
	private String value;

	public SysEmailTemplate() {
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public short getStatus() {
		return this.status;
	}

	public void setStatus(short status) {
		this.status = status;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}