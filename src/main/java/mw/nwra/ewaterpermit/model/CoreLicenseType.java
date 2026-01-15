package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_license_type")
@NamedQuery(name = "CoreLicenseType.findAll", query = "SELECT c FROM CoreLicenseType c")
public class CoreLicenseType extends BaseEntity {

	@Column(name = "application_fees")
	private double applicationFees;

	@Column(name = "default_validity_length")
	private int defaultValidityLength;

	private String description;

	@Column(name = "license_fees")
	private double licenseFees;

	private String name;

	private byte status;

	// bi-directional many-to-one association to CoreApplicationStep
	@JsonIgnore
	@OneToMany(mappedBy = "coreLicenseType")
	private List<CoreApplicationStep> coreApplicationSteps;

	// bi-directional many-to-one association to CoreLicenseApplication
	@JsonIgnore
	@OneToMany(mappedBy = "coreLicenseType")
	private List<CoreLicenseApplication> coreLicenseApplications;

	// bi-directional many-to-one association to CoreLicenseRequirement
	@JsonIgnore
	@OneToMany(mappedBy = "coreLicenseType")
	private List<CoreLicenseRequirement> coreLicenseRequirements;

	// bi-directional many-to-one association to CoreLicenseTypeActivity
	@JsonIgnore
	@OneToMany(mappedBy = "coreLicenseType")
	private List<CoreLicenseTypeActivity> coreLicenseTypeActivities;

	public CoreLicenseType() {
	}

	public double getApplicationFees() {
		return this.applicationFees;
	}

	public void setApplicationFees(double applicationFees) {
		this.applicationFees = applicationFees;
	}

	public int getDefaultValidityLength() {
		return this.defaultValidityLength;
	}

	public void setDefaultValidityLength(int defaultValidityLength) {
		this.defaultValidityLength = defaultValidityLength;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getLicenseFees() {
		return this.licenseFees;
	}

	public void setLicenseFees(double licenseFees) {
		this.licenseFees = licenseFees;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte getStatus() {
		return this.status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

	public List<CoreApplicationStep> getCoreApplicationSteps() {
		return this.coreApplicationSteps;
	}

	public void setCoreApplicationSteps(List<CoreApplicationStep> coreApplicationSteps) {
		this.coreApplicationSteps = coreApplicationSteps;
	}

	public CoreApplicationStep addCoreApplicationStep(CoreApplicationStep coreApplicationStep) {
		getCoreApplicationSteps().add(coreApplicationStep);
		coreApplicationStep.setCoreLicenseType(this);

		return coreApplicationStep;
	}

	public CoreApplicationStep removeCoreApplicationStep(CoreApplicationStep coreApplicationStep) {
		getCoreApplicationSteps().remove(coreApplicationStep);
		coreApplicationStep.setCoreLicenseType(null);

		return coreApplicationStep;
	}

	public List<CoreLicenseApplication> getCoreLicenseApplications() {
		return this.coreLicenseApplications;
	}

	public void setCoreLicenseApplications(List<CoreLicenseApplication> coreLicenseApplications) {
		this.coreLicenseApplications = coreLicenseApplications;
	}

	public CoreLicenseApplication addCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		getCoreLicenseApplications().add(coreLicenseApplication);
		coreLicenseApplication.setCoreLicenseType(this);

		return coreLicenseApplication;
	}

	public CoreLicenseApplication removeCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		getCoreLicenseApplications().remove(coreLicenseApplication);
		coreLicenseApplication.setCoreLicenseType(null);

		return coreLicenseApplication;
	}

	public List<CoreLicenseRequirement> getCoreLicenseRequirements() {
		return this.coreLicenseRequirements;
	}

	public void setCoreLicenseRequirements(List<CoreLicenseRequirement> coreLicenseRequirements) {
		this.coreLicenseRequirements = coreLicenseRequirements;
	}

	public CoreLicenseRequirement addCoreLicenseRequirement(CoreLicenseRequirement coreLicenseRequirement) {
		getCoreLicenseRequirements().add(coreLicenseRequirement);
		coreLicenseRequirement.setCoreLicenseType(this);

		return coreLicenseRequirement;
	}

	public CoreLicenseRequirement removeCoreLicenseRequirement(CoreLicenseRequirement coreLicenseRequirement) {
		getCoreLicenseRequirements().remove(coreLicenseRequirement);
		coreLicenseRequirement.setCoreLicenseType(null);

		return coreLicenseRequirement;
	}

	public List<CoreLicenseTypeActivity> getCoreLicenseTypeActivities() {
		return this.coreLicenseTypeActivities;
	}

	public void setCoreLicenseTypeActivities(List<CoreLicenseTypeActivity> coreLicenseTypeActivities) {
		this.coreLicenseTypeActivities = coreLicenseTypeActivities;
	}

	public CoreLicenseTypeActivity addCoreLicenseTypeActivity(CoreLicenseTypeActivity coreLicenseTypeActivity) {
		getCoreLicenseTypeActivities().add(coreLicenseTypeActivity);
		coreLicenseTypeActivity.setCoreLicenseType(this);

		return coreLicenseTypeActivity;
	}

	public CoreLicenseTypeActivity removeCoreLicenseTypeActivity(CoreLicenseTypeActivity coreLicenseTypeActivity) {
		getCoreLicenseTypeActivities().remove(coreLicenseTypeActivity);
		coreLicenseTypeActivity.setCoreLicenseType(null);

		return coreLicenseTypeActivity;
	}

}