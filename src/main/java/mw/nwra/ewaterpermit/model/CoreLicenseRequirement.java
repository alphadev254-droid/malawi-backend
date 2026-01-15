package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_license_requirement")
@NamedQuery(name = "CoreLicenseRequirement.findAll", query = "SELECT c FROM CoreLicenseRequirement c")
public class CoreLicenseRequirement extends BaseEntity {

	private String name;

	private String description;

	// bi-directional many-to-one association to CoreLicenseType
	@ManyToOne
	@JoinColumn(name = "license_type_id")
	private CoreLicenseType coreLicenseType;

	public CoreLicenseRequirement() {
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CoreLicenseType getCoreLicenseType() {
		return this.coreLicenseType;
	}

	public void setCoreLicenseType(CoreLicenseType coreLicenseType) {
		this.coreLicenseType = coreLicenseType;
	}

}