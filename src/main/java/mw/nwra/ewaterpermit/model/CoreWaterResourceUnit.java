package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_water_resource_unit")
@NamedQuery(name = "CoreWaterResourceUnit.findAll", query = "SELECT c FROM CoreWaterResourceUnit c")
public class CoreWaterResourceUnit extends BaseEntity {

	@Lob
	@Column(name = "geo_geometry")
	private byte[] geoGeometry;

	@Lob
	@Column(name = "geo_property")
	private byte[] geoProperty;

	@Lob
	@Column(name = "geo_type")
	private String geoType;

	// bi-directional many-to-one association to CoreWaterResourceArea
	@ManyToOne
	@JoinColumn(name = "water_resource_area_id")
	private CoreWaterResourceArea coreWaterResourceArea;

	// bi-directional many-to-one association to CoreLicenseApplication
	@JsonIgnore
	@OneToMany(mappedBy = "sourceWru")
	private List<CoreLicenseApplication> coreLicenseApplications1;

	// bi-directional many-to-one association to CoreLicenseApplication
	@JsonIgnore
	@OneToMany(mappedBy = "destWru")
	private List<CoreLicenseApplication> coreLicenseApplications2;

	public CoreWaterResourceUnit() {
	}

	public byte[] getGeoGeometry() {
		return this.geoGeometry;
	}

	public void setGeoGeometry(byte[] geoGeometry) {
		this.geoGeometry = geoGeometry;
	}

	public byte[] getGeoProperty() {
		return this.geoProperty;
	}

	public void setGeoProperty(byte[] geoProperty) {
		this.geoProperty = geoProperty;
	}

	public String getGeoType() {
		return this.geoType;
	}

	public void setGeoType(String geoType) {
		this.geoType = geoType;
	}

	public CoreWaterResourceArea getCoreWaterResourceArea() {
		return this.coreWaterResourceArea;
	}

	public void setCoreWaterResourceArea(CoreWaterResourceArea coreWaterResourceArea) {
		this.coreWaterResourceArea = coreWaterResourceArea;
	}

	public List<CoreLicenseApplication> getCoreLicenseApplications1() {
		return this.coreLicenseApplications1;
	}

	public void setCoreLicenseApplications1(List<CoreLicenseApplication> coreLicenseApplications1) {
		this.coreLicenseApplications1 = coreLicenseApplications1;
	}

	public CoreLicenseApplication addCoreLicenseApplications1(CoreLicenseApplication coreLicenseApplications1) {
		getCoreLicenseApplications1().add(coreLicenseApplications1);
		coreLicenseApplications1.setSourceWru(this);

		return coreLicenseApplications1;
	}

	public CoreLicenseApplication removeCoreLicenseApplications1(CoreLicenseApplication coreLicenseApplications1) {
		getCoreLicenseApplications1().remove(coreLicenseApplications1);
		coreLicenseApplications1.setSourceWru(null);

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
		coreLicenseApplications2.setDestWru(this);

		return coreLicenseApplications2;
	}

	public CoreLicenseApplication removeCoreLicenseApplications2(CoreLicenseApplication coreLicenseApplications2) {
		getCoreLicenseApplications2().remove(coreLicenseApplications2);
		coreLicenseApplications2.setDestWru(null);

		return coreLicenseApplications2;
	}
}