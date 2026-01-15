package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_disposable_domain")
@NamedQuery(name = "SysDisposableDomain.findAll", query = "SELECT s FROM SysDisposableDomain s")
public class SysDisposableDomain extends BaseEntity {
	private String name;

	public SysDisposableDomain() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}