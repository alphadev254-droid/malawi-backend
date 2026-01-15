package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_customer_type")
@NamedQuery(name = "CoreCustomerType.findAll", query = "SELECT c FROM CoreCustomerType c")
public class CoreCustomerType extends BaseEntity {

	private String description;

	private String name;

	// bi-directional many-to-one association to CoreCustomer
	@JsonIgnore
	@OneToMany(mappedBy = "coreCustomerType")
	private List<CoreCustomer> coreCustomers;

	public CoreCustomerType() {
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

	public List<CoreCustomer> getCoreCustomers() {
		return this.coreCustomers;
	}

	public void setCoreCustomers(List<CoreCustomer> coreCustomers) {
		this.coreCustomers = coreCustomers;
	}

	public CoreCustomer addCoreCustomer(CoreCustomer coreCustomer) {
		getCoreCustomers().add(coreCustomer);
		coreCustomer.setCoreCustomerType(this);

		return coreCustomer;
	}

	public CoreCustomer removeCoreCustomer(CoreCustomer coreCustomer) {
		getCoreCustomers().remove(coreCustomer);
		coreCustomer.setCoreCustomerType(null);

		return coreCustomer;
	}

}