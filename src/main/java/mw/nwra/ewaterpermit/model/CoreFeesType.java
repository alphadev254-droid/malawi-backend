package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_fees_type")
@NamedQuery(name = "CoreFeesType.findAll", query = "SELECT c FROM CoreFeesType c")
public class CoreFeesType extends BaseEntity {

	private String description;

	private String name;

	// bi-directional many-to-one association to CoreApplicationPayment
	@JsonIgnore
	@OneToMany(mappedBy = "coreFeesType")
	private List<CoreApplicationPayment> coreApplicationPayments;

	public CoreFeesType() {
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

	public List<CoreApplicationPayment> getCoreApplicationPayments() {
		return this.coreApplicationPayments;
	}

	public void setCoreApplicationPayments(List<CoreApplicationPayment> coreApplicationPayments) {
		this.coreApplicationPayments = coreApplicationPayments;
	}

	public CoreApplicationPayment addCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
		getCoreApplicationPayments().add(coreApplicationPayment);
		coreApplicationPayment.setCoreFeesType(this);

		return coreApplicationPayment;
	}

	public CoreApplicationPayment removeCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
		getCoreApplicationPayments().remove(coreApplicationPayment);
		coreApplicationPayment.setCoreFeesType(null);

		return coreApplicationPayment;
	}

}