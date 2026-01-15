package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_district")
@NamedQuery(name = "CoreDistrict.findAll", query = "SELECT c FROM CoreDistrict c")
public class CoreDistrict extends BaseEntity {

	private String name;

	private String region;

	// bi-directional many-to-one association to CoreCustomer
	@JsonIgnore
	@OneToMany(mappedBy = "coreDistrict")
	private List<CoreCustomer> coreCustomers;

	// bi-directional many-to-one association to CoreTraditionalAuthority
	@JsonIgnore
	@OneToMany(mappedBy = "coreDistrict")
	private List<CoreTraditionalAuthority> coreTraditionalAuthorities;

	public CoreDistrict() {
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public List<CoreCustomer> getCoreCustomers() {
		return this.coreCustomers;
	}

	public void setCoreCustomers(List<CoreCustomer> coreCustomers) {
		this.coreCustomers = coreCustomers;
	}

	public CoreCustomer addCoreCustomer(CoreCustomer coreCustomer) {
		getCoreCustomers().add(coreCustomer);
		coreCustomer.setCoreDistrict(this);

		return coreCustomer;
	}

	public CoreCustomer removeCoreCustomer(CoreCustomer coreCustomer) {
		getCoreCustomers().remove(coreCustomer);
		coreCustomer.setCoreDistrict(null);

		return coreCustomer;
	}

	public List<CoreTraditionalAuthority> getCoreTraditionalAuthorities() {
		return this.coreTraditionalAuthorities;
	}

	public void setCoreTraditionalAuthorities(List<CoreTraditionalAuthority> coreTraditionalAuthorities) {
		this.coreTraditionalAuthorities = coreTraditionalAuthorities;
	}

	public CoreTraditionalAuthority addCoreTraditionalAuthority(CoreTraditionalAuthority coreTraditionalAuthority) {
		getCoreTraditionalAuthorities().add(coreTraditionalAuthority);
		coreTraditionalAuthority.setCoreDistrict(this);

		return coreTraditionalAuthority;
	}

	public CoreTraditionalAuthority removeCoreTraditionalAuthority(CoreTraditionalAuthority coreTraditionalAuthority) {
		getCoreTraditionalAuthorities().remove(coreTraditionalAuthority);
		coreTraditionalAuthority.setCoreDistrict(null);

		return coreTraditionalAuthority;
	}

}