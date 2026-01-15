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
@Table(name = "core_license_type_activity")
@NamedQuery(name = "CoreLicenseTypeActivity.findAll", query = "SELECT c FROM CoreLicenseTypeActivity c")
public class CoreLicenseTypeActivity extends BaseEntity {

	private String description;

	private String name;

	@Column(name = "is_upload")
	private String isUpload;
	@Column(name = "is_required")
	private String isRequired;
	@Column(name = "is_user_activity")
	private String isUserActivity;

	// bi-directional many-to-one association to CoreLicenseApplicationActivity
	@JsonIgnore
	@OneToMany(mappedBy = "coreLicenseTypeActivity")
	private List<CoreLicenseApplicationActivity> coreLicenseApplicationActivities;

	// bi-directional many-to-one association to CoreLicenseType
	@ManyToOne
	@JoinColumn(name = "license_type_id")
	private CoreLicenseType coreLicenseType;

	public CoreLicenseTypeActivity() {
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

	public List<CoreLicenseApplicationActivity> getCoreLicenseApplicationActivities() {
		return this.coreLicenseApplicationActivities;
	}

	public void setCoreLicenseApplicationActivities(
			List<CoreLicenseApplicationActivity> coreLicenseApplicationActivities) {
		this.coreLicenseApplicationActivities = coreLicenseApplicationActivities;
	}

	public CoreLicenseApplicationActivity addCoreLicenseApplicationActivity(
			CoreLicenseApplicationActivity coreLicenseApplicationActivity) {
		getCoreLicenseApplicationActivities().add(coreLicenseApplicationActivity);
		coreLicenseApplicationActivity.setCoreLicenseTypeActivity(this);

		return coreLicenseApplicationActivity;
	}

	public CoreLicenseApplicationActivity removeCoreLicenseApplicationActivity(
			CoreLicenseApplicationActivity coreLicenseApplicationActivity) {
		getCoreLicenseApplicationActivities().remove(coreLicenseApplicationActivity);
		coreLicenseApplicationActivity.setCoreLicenseTypeActivity(null);

		return coreLicenseApplicationActivity;
	}

	public CoreLicenseType getCoreLicenseType() {
		return this.coreLicenseType;
	}

	public void setCoreLicenseType(CoreLicenseType coreLicenseType) {
		this.coreLicenseType = coreLicenseType;
	}

	public String getIsUpload() {
		return isUpload;
	}

	public void setIsUpload(String isUpload) {
		this.isUpload = isUpload;
	}

	public String getIsRequired() {
		return isRequired;
	}

	public void setIsRequired(String isRequired) {
		this.isRequired = isRequired;
	}

	public String getIsUserActivity() {
		return isUserActivity;
	}

	public void setIsUserActivity(String isUserActivity) {
		this.isUserActivity = isUserActivity;
	}

}