package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_customer")
@NamedQuery(name = "CoreCustomer.findAll", query = "SELECT c FROM CoreCustomer c")
public class CoreCustomer extends BaseEntity {

	@Column(name = "email_address")
	private String emailAddress;

	private String fax;

	@Column(name = "mobile_number")
	private String mobileNumber;

	private String name;

	@Column(name = "physical_address")
	private String physicalAddress;

	@Column(name = "postal_address")
	private String postalAddress;

	@Column(name = "telephone_number")
	private String telephoneNumber;

	// bi-directional many-to-one association to CoreDistrict
	@ManyToOne
	@JoinColumn(name = "district_id")
	private CoreDistrict coreDistrict;

	// bi-directional many-to-one association to CoreCustomerType
	@ManyToOne
	@JoinColumn(name = "customer_type_id")
	private CoreCustomerType coreCustomerType;

	public CoreCustomer() {
	}

	public String getEmailAddress() {
		return this.emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getFax() {
		return this.fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	public String getMobileNumber() {
		return this.mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhysicalAddress() {
		return this.physicalAddress;
	}

	public void setPhysicalAddress(String physicalAddress) {
		this.physicalAddress = physicalAddress;
	}

	public String getPostalAddress() {
		return this.postalAddress;
	}

	public void setPostalAddress(String postalAddress) {
		this.postalAddress = postalAddress;
	}

	public String gettelephoneNumber() {
		return this.telephoneNumber;
	}

	public void settelephoneNumber(String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}

	public CoreDistrict getCoreDistrict() {
		return this.coreDistrict;
	}

	public void setCoreDistrict(CoreDistrict coreDistrict) {
		this.coreDistrict = coreDistrict;
	}

	public CoreCustomerType getCoreCustomerType() {
		return this.coreCustomerType;
	}

	public void setCoreCustomerType(CoreCustomerType coreCustomerType) {
		this.coreCustomerType = coreCustomerType;
	}

}