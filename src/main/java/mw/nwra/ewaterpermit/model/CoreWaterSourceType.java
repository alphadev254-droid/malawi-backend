package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_water_source_type")
@NamedQuery(name = "CoreWaterSourceType.findAll", query = "SELECT c FROM CoreWaterSourceType c")
public class CoreWaterSourceType extends BaseEntity {

	private String category;

	private String description;

	private String name;

	// bi-directional many-to-one association to CoreWaterSource
	@JsonIgnore
	@OneToMany(mappedBy = "coreWaterSourceType")
	private List<CoreWaterSource> coreWaterSources;

	public CoreWaterSourceType() {
	}

	public String getCategory() {
		return this.category;
	}

	public void setCategory(String category) {
		this.category = category;
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

	public List<CoreWaterSource> getCoreWaterSources() {
		return this.coreWaterSources;
	}

	public void setCoreWaterSources(List<CoreWaterSource> coreWaterSources) {
		this.coreWaterSources = coreWaterSources;
	}

	public CoreWaterSource addCoreWaterSource(CoreWaterSource coreWaterSource) {
		getCoreWaterSources().add(coreWaterSource);
		coreWaterSource.setCoreWaterSourceType(this);

		return coreWaterSource;
	}

	public CoreWaterSource removeCoreWaterSource(CoreWaterSource coreWaterSource) {
		getCoreWaterSources().remove(coreWaterSource);
		coreWaterSource.setCoreWaterSourceType(null);

		return coreWaterSource;
	}

}