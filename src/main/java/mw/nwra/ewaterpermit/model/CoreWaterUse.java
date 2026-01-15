package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_water_use")
@NamedQuery(name = "CoreWaterUse.findAll", query = "SELECT c FROM CoreWaterUse c")
public class CoreWaterUse extends BaseEntity {

	private String description;

	private String name;

	// bi-directional many-to-one association to CoreLicenseWaterUse
	@JsonIgnore
	@OneToMany(mappedBy = "coreWaterUse")
	private List<CoreLicenseWaterUse> coreLicenseWaterUses;

	public CoreWaterUse() {
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

	public List<CoreLicenseWaterUse> getCoreLicenseWaterUses() {
		return this.coreLicenseWaterUses;
	}

	public void setCoreLicenseWaterUses(List<CoreLicenseWaterUse> coreLicenseWaterUses) {
		this.coreLicenseWaterUses = coreLicenseWaterUses;
	}

	public CoreLicenseWaterUse addCoreLicenseWaterUs(CoreLicenseWaterUse coreLicenseWaterUs) {
		getCoreLicenseWaterUses().add(coreLicenseWaterUs);
		coreLicenseWaterUs.setCoreWaterUse(this);

		return coreLicenseWaterUs;
	}

	public CoreLicenseWaterUse removeCoreLicenseWaterUs(CoreLicenseWaterUse coreLicenseWaterUs) {
		getCoreLicenseWaterUses().remove(coreLicenseWaterUs);
		coreLicenseWaterUs.setCoreWaterUse(null);

		return coreLicenseWaterUs;
	}

}