package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_water_source")
@NamedQuery(name = "CoreWaterSource.findAll", query = "SELECT c FROM CoreWaterSource c")
public class CoreWaterSource extends BaseEntity {

	private String description;

	private String name;

	// bi-directional many-to-one association to CoreLicenseApplication
	@JsonIgnore
	@OneToMany(mappedBy = "coreWaterSource")
	private List<CoreLicenseApplication> coreLicenseApplications;

	// bi-directional many-to-one association to CoreWaterSourceType
	@ManyToOne
	@JoinColumn(name = "water_source_type_id")
	private CoreWaterSourceType coreWaterSourceType;

	public CoreWaterSource() {
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

	public List<CoreLicenseApplication> getCoreLicenseApplications() {
		return this.coreLicenseApplications;
	}

	public void setCoreLicenseApplications(List<CoreLicenseApplication> coreLicenseApplications) {
		this.coreLicenseApplications = coreLicenseApplications;
	}

	public CoreLicenseApplication addCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		getCoreLicenseApplications().add(coreLicenseApplication);
		coreLicenseApplication.setCoreWaterSource(this);

		return coreLicenseApplication;
	}

	public CoreLicenseApplication removeCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		getCoreLicenseApplications().remove(coreLicenseApplication);
		coreLicenseApplication.setCoreWaterSource(null);

		return coreLicenseApplication;
	}

	public CoreWaterSourceType getCoreWaterSourceType() {
		return this.coreWaterSourceType;
	}

	public void setCoreWaterSourceType(CoreWaterSourceType coreWaterSourceType) {
		this.coreWaterSourceType = coreWaterSourceType;
	}

}