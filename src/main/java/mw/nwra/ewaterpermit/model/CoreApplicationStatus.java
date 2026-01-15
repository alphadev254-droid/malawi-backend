package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_application_status")
@NamedQuery(name = "CoreApplicationStatus.findAll", query = "SELECT c FROM CoreApplicationStatus c")
public class CoreApplicationStatus extends BaseEntity {
	private String description;

	private String name;

	@JsonIgnore
	@OneToMany(mappedBy = "coreApplicationStatus")
	private List<CoreLicenseApplication> coreLicenseApplications;

	public CoreApplicationStatus() {
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
		coreLicenseApplication.setCoreApplicationStatus(this);

		return coreLicenseApplication;
	}

	public CoreLicenseApplication removeCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		getCoreLicenseApplications().remove(coreLicenseApplication);
		coreLicenseApplication.setCoreApplicationStatus(null);

		return coreLicenseApplication;
	}

}