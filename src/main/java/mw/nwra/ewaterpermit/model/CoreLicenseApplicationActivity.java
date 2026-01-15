package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_license_application_activity")
@NamedQuery(name = "CoreLicenseApplicationActivity.findAll", query = "SELECT c FROM CoreLicenseApplicationActivity c")
public class CoreLicenseApplicationActivity extends BaseEntity {

	private String description;

	// bi-directional many-to-one association to SysUserAccount
	@ManyToOne
	@JoinColumn(name = "user_account_id")
	private SysUserAccount sysUserAccount;

	// bi-directional many-to-one association to CoreLicenseApplication
	@ManyToOne
	@JoinColumn(name = "license_application_id")
	private CoreLicenseApplication coreLicenseApplication;

	// bi-directional many-to-one association to CoreLicenseTypeActivity
	@ManyToOne
	@JoinColumn(name = "license_type_activity_id")
	private CoreLicenseTypeActivity coreLicenseTypeActivity;

	public CoreLicenseApplicationActivity() {
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SysUserAccount getSysUserAccount() {
		return this.sysUserAccount;
	}

	public void setSysUserAccount(SysUserAccount sysUserAccount) {
		this.sysUserAccount = sysUserAccount;
	}

	public CoreLicenseApplication getCoreLicenseApplication() {
		return this.coreLicenseApplication;
	}

	public void setCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		this.coreLicenseApplication = coreLicenseApplication;
	}

	public CoreLicenseTypeActivity getCoreLicenseTypeActivity() {
		return this.coreLicenseTypeActivity;
	}

	public void setCoreLicenseTypeActivity(CoreLicenseTypeActivity coreLicenseTypeActivity) {
		this.coreLicenseTypeActivity = coreLicenseTypeActivity;
	}

}