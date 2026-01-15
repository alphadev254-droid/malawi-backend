package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_traditional_authority")
@NamedQuery(name = "CoreTraditionalAuthority.findAll", query = "SELECT c FROM CoreTraditionalAuthority c")
public class CoreTraditionalAuthority extends BaseEntity {
	private String name;

	// bi-directional many-to-one association to CoreDistrict
	@ManyToOne
	@JoinColumn(name = "district_id")
	private CoreDistrict coreDistrict;

	public CoreTraditionalAuthority() {
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CoreDistrict getCoreDistrict() {
		return this.coreDistrict;
	}

	public void setCoreDistrict(CoreDistrict coreDistrict) {
		this.coreDistrict = coreDistrict;
	}

}