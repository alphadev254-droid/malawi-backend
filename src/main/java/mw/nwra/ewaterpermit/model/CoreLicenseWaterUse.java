package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_license_water_use")
@NamedQuery(name = "CoreLicenseWaterUse.findAll", query = "SELECT c FROM CoreLicenseWaterUse c")
public class CoreLicenseWaterUse extends BaseEntity {

	@Column(name = "amount_per_day_m3")
	private double amountPerDayM3;

	private String description;

	// bi-directional many-to-one association to CoreWaterUse
	@ManyToOne
	@JoinColumn(name = "water_use_id")
	private CoreWaterUse coreWaterUse;

	// bi-directional many-to-one association to CoreLicenseApplication
	@ManyToOne
	@JoinColumn(name = "license_application_id")
	private CoreLicenseApplication coreLicenseApplication;

	public CoreLicenseWaterUse() {
	}

	public double getAmountPerDayM3() {
		return this.amountPerDayM3;
	}

	public void setAmountPerDayM3(double amountPerDayM3) {
		this.amountPerDayM3 = amountPerDayM3;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public CoreWaterUse getCoreWaterUse() {
		return this.coreWaterUse;
	}

	public void setCoreWaterUse(CoreWaterUse coreWaterUse) {
		this.coreWaterUse = coreWaterUse;
	}

	public CoreLicenseApplication getCoreLicenseApplication() {
		return this.coreLicenseApplication;
	}

	public void setCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		this.coreLicenseApplication = coreLicenseApplication;
	}

}