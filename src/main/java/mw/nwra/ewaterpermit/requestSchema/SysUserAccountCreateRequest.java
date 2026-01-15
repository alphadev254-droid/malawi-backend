package mw.nwra.ewaterpermit.requestSchema;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SysUserAccountCreateRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	private String username;
	private String firstName;
	private String lastName;
	private String emailAddress;
	private String password;
	private String districtId;
	private String phoneNumber;
	private String postalAddress;
	private String designation;
	private String companyRegistrationNumber;
	private String companyRegisteredName;
	private String companyTradingName;
	private String nationalId;
	private String passportNumber;
	private String salutationId;
	private String customerTypeId;
	private String passportCountryId;
	private String nationalIdx;
	private String physicalAddress;
	private String confirmPassword;
	private String[] termsAndConditions;

	public SysUserAccountCreateRequest() {
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDistrictId() {
		return districtId;
	}

	public void setDistrictId(String districtId) {
		this.districtId = districtId;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getPostalAddress() {
		return postalAddress;
	}

	public void setPostalAddress(String postalAddress) {
		this.postalAddress = postalAddress;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getCompanyRegistrationNumber() {
		return companyRegistrationNumber;
	}

	public void setCompanyRegistrationNumber(String companyRegistrationNumber) {
		this.companyRegistrationNumber = companyRegistrationNumber;
	}

	public String getCompanyRegisteredName() {
		return companyRegisteredName;
	}

	public void setCompanyRegisteredName(String companyRegisteredName) {
		this.companyRegisteredName = companyRegisteredName;
	}

	public String getCompanyTradingName() {
		return companyTradingName;
	}

	public void setCompanyTradingName(String companyTradingName) {
		this.companyTradingName = companyTradingName;
	}

	public String getNationalId() {
		return nationalId;
	}

	public void setNationalId(String nationalId) {
		this.nationalId = nationalId;
	}

	public String getPassportNumber() {
		return passportNumber;
	}

	public void setPassportNumber(String passportNumber) {
		this.passportNumber = passportNumber;
	}

	public String getSalutationId() {
		return salutationId;
	}

	public void setSalutationId(String salutationId) {
		this.salutationId = salutationId;
	}

	public String getCustomerTypeId() {
		return customerTypeId;
	}

	public void setCustomerTypeId(String customerTypeId) {
		this.customerTypeId = customerTypeId;
	}

	public String getPassportCountryId() {
		return passportCountryId;
	}

	public void setPassportCountryId(String passportCountryId) {
		this.passportCountryId = passportCountryId;
	}

	public String getNationalIdx() {
		return nationalIdx;
	}

	public void setNationalIdx(String nationalIdx) {
		this.nationalIdx = nationalIdx;
	}

	public String getPhysicalAddress() {
		return physicalAddress;
	}

	public void setPhysicalAddress(String physicalAddress) {
		this.physicalAddress = physicalAddress;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public String[] getTermsAndConditions() {
		return termsAndConditions;
	}

	public void setTermsAndConditions(String[] termsAndConditions) {
		this.termsAndConditions = termsAndConditions;
	}
}