package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_land_regime")
@NamedQuery(name = "CoreLandRegime.findAll", query = "SELECT c FROM CoreLandRegime c")
public class CoreLandRegime extends BaseEntity {
	private String name;

	private String description;

	// bi-directional many-to-one association to CoreLicenseApplication
	@JsonIgnore
	@OneToMany(mappedBy = "sourceLandRegime")
	private List<CoreLicenseApplication> coreLicenseApplications1;

	// bi-directional many-to-one association to CoreLicenseApplication
	@JsonIgnore
	@OneToMany(mappedBy = "destLandRegime")
	private List<CoreLicenseApplication> coreLicenseApplications2;

	public CoreLandRegime() {
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<CoreLicenseApplication> getCoreLicenseApplications1() {
		return this.coreLicenseApplications1;
	}

	public void setCoreLicenseApplications1(List<CoreLicenseApplication> coreLicenseApplications1) {
		this.coreLicenseApplications1 = coreLicenseApplications1;
	}

	public CoreLicenseApplication addCoreLicenseApplications1(CoreLicenseApplication coreLicenseApplications1) {
		getCoreLicenseApplications1().add(coreLicenseApplications1);
		coreLicenseApplications1.setSourceLandRegime(this);

		return coreLicenseApplications1;
	}

	public CoreLicenseApplication removeCoreLicenseApplications1(CoreLicenseApplication coreLicenseApplications1) {
		getCoreLicenseApplications1().remove(coreLicenseApplications1);
		coreLicenseApplications1.setSourceLandRegime(null);

		return coreLicenseApplications1;
	}

	public List<CoreLicenseApplication> getCoreLicenseApplications2() {
		return this.coreLicenseApplications2;
	}

	public void setCoreLicenseApplications2(List<CoreLicenseApplication> coreLicenseApplications2) {
		this.coreLicenseApplications2 = coreLicenseApplications2;
	}

	public CoreLicenseApplication addCoreLicenseApplications2(CoreLicenseApplication coreLicenseApplications2) {
		getCoreLicenseApplications2().add(coreLicenseApplications2);
		coreLicenseApplications2.setDestLandRegime(this);

		return coreLicenseApplications2;
	}

	public CoreLicenseApplication removeCoreLicenseApplications2(CoreLicenseApplication coreLicenseApplications2) {
		getCoreLicenseApplications2().remove(coreLicenseApplications2);
		coreLicenseApplications2.setDestLandRegime(null);

		return coreLicenseApplications2;
	}

}