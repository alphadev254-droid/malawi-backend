package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_water_resource_area")
@NamedQuery(name = "CoreWaterResourceArea.findAll", query = "SELECT c FROM CoreWaterResourceArea c")
public class CoreWaterResourceArea extends BaseEntity {
	@Lob
	@Column(name = "geo_geometry")
	private byte[] geoGeometry;

	@Lob
	@Column(name = "geo_property")
	private byte[] geoProperty;

	@Column(name = "geo_type")
	private String geoType;

	@Column(name = "display_name")
	private String displayName;

	// bi-directional many-to-one association to CoreWaterResourceUnit
	@JsonIgnore
	@OneToMany(mappedBy = "coreWaterResourceArea")
	private List<CoreWaterResourceUnit> coreWaterResourceUnits;

	public CoreWaterResourceArea() {
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

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public List<CoreWaterResourceUnit> getCoreWaterResourceUnits() {
		return this.coreWaterResourceUnits;
	}

	public void setCoreWaterResourceUnits(List<CoreWaterResourceUnit> coreWaterResourceUnits) {
		this.coreWaterResourceUnits = coreWaterResourceUnits;
	}

	public CoreWaterResourceUnit addCoreWaterResourceUnit(CoreWaterResourceUnit coreWaterResourceUnit) {
		getCoreWaterResourceUnits().add(coreWaterResourceUnit);
		coreWaterResourceUnit.setCoreWaterResourceArea(this);

		return coreWaterResourceUnit;
	}

	public CoreWaterResourceUnit removeCoreWaterResourceUnit(CoreWaterResourceUnit coreWaterResourceUnit) {
		getCoreWaterResourceUnits().remove(coreWaterResourceUnit);
		coreWaterResourceUnit.setCoreWaterResourceArea(null);

		return coreWaterResourceUnit;
	}

}