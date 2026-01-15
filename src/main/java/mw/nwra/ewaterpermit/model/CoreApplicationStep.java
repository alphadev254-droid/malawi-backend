package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_application_step")
@NamedQuery(name = "CoreApplicationStep.findAll", query = "SELECT c FROM CoreApplicationStep c")
public class CoreApplicationStep extends BaseEntity {

	private String description;

	private String name;

	@Column(name = "sequence_number")
	private byte sequenceNumber;

	// bi-directional many-to-one association to CoreLicenseType
	@ManyToOne
	@JoinColumn(name = "license_type_id")
	private CoreLicenseType coreLicenseType;

	@JsonIgnore
	@OneToMany(mappedBy = "coreApplicationStep")
	private List<CoreLicenseApplication> coreLicenseApplications;

	public CoreApplicationStep() {
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

	public byte getSequenceNumber() {
		return this.sequenceNumber;
	}

	public void setSequenceNumber(byte sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public CoreLicenseType getCoreLicenseType() {
		return this.coreLicenseType;
	}

	public void setCoreLicenseType(CoreLicenseType coreLicenseType) {
		this.coreLicenseType = coreLicenseType;
	}

	public List<CoreLicenseApplication> getCoreLicenseApplications() {
		return this.coreLicenseApplications;
	}

	public void setCoreLicenseApplications(List<CoreLicenseApplication> coreLicenseApplications) {
		this.coreLicenseApplications = coreLicenseApplications;
	}

	public CoreLicenseApplication addCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		getCoreLicenseApplications().add(coreLicenseApplication);
		coreLicenseApplication.setCoreApplicationStep(this);

		return coreLicenseApplication;
	}

	public CoreLicenseApplication removeCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		getCoreLicenseApplications().remove(coreLicenseApplication);
		coreLicenseApplication.setCoreApplicationStep(null);

		return coreLicenseApplication;
	}

}