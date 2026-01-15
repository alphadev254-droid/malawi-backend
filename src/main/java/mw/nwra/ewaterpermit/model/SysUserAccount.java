package mw.nwra.ewaterpermit.model;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_user_account")
@NamedQuery(name = "SysUserAccount.findAll", query = "SELECT s FROM SysUserAccount s")
public class SysUserAccount extends BaseEntity {

	@Column(name = "can_login_after")
	private Timestamp canLoginAfter;

	@Column(name = "email_address")
	private String emailAddress;

	@Column(name = "first_name")
	private String firstName;

	private String gender;

	private String lang;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "last_login")
	private Timestamp lastLogin;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "last_password_attempt")
	private Timestamp lastPasswordAttempt;

	@Column(name = "middle_name")
	private String middleName;

	@Column(name = "nick_name")
	private String nickName;

	private String password;

	@Column(name = "password_attempt_count")
	private Integer passwordAttemptCount;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "postal_address")
	private String postalAddress;

	@Column(name = "profile_photo")
	private String profilePhoto;

	private String username;

	// bi-directional many-to-one association to SysAuditEntry
	@JsonIgnore
	@OneToMany(mappedBy = "sysUserAccount")
	private List<SysAuditEntry> sysAuditEntries;

	// bi-directional many-to-one association to SysUserGroup
	@ManyToOne
	@JoinColumn(name = "user_group_id")
	private SysUserGroup sysUserGroup;

	// bi-directional many-to-one association to SysAccountStatus
	@ManyToOne
	@JoinColumn(name = "account_status_id")
	private SysAccountStatus sysAccountStatus;

	// bi-directional many-to-one association to SysSalutation
	@ManyToOne
	@JoinColumn(name = "salutation_id")
	private SysSalutation sysSalutation;

	// bi-directional many-to-one association to CoreDistrict
	@ManyToOne
	@JoinColumn(name = "district_id")
	private CoreDistrict coreDistrict;

	@Column(name = "national_id")
	private String nationalId;

	@Column(name = "passport_number")
	private String passportNumber;

	@Column(name = "designation")
	private String designation;

	@Column(name = "company_registration_number")
	private String companyRegistrationNumber;

	@Column(name = "company_registered_name")
	private String companyRegisteredName;

	@Column(name = "company_trading_name")
	private String companyTradingName;

	// bi-directional many-to-one association to SysUserAccountActivation
	@JsonIgnore
	@OneToMany(mappedBy = "sysUserAccount")
	private List<SysUserAccountActivation> sysUserAccountActivations;

	public SysUserAccount() {
	}

	public Timestamp getCanLoginAfter() {
		return this.canLoginAfter;
	}

	public void setCanLoginAfter(Timestamp canLoginAfter) {
		this.canLoginAfter = canLoginAfter;
	}

	public String getEmailAddress() {
		return this.emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getGender() {
		return this.gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getLang() {
		return this.lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public Timestamp getLastLogin() {
		return this.lastLogin;
	}

	public void setLastLogin(Timestamp lastLogin) {
		this.lastLogin = lastLogin;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Timestamp getLastPasswordAttempt() {
		return this.lastPasswordAttempt;
	}

	public void setLastPasswordAttempt(Timestamp lastPasswordAttempt) {
		this.lastPasswordAttempt = lastPasswordAttempt;
	}

	public String getMiddleName() {
		return this.middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getNickName() {
		return this.nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getPasswordAttemptCount() {
		return this.passwordAttemptCount;
	}

	public void setPasswordAttemptCount(Integer passwordAttemptCount) {
		this.passwordAttemptCount = passwordAttemptCount;
	}

	public String getPhoneNumber() {
		return this.phoneNumber;
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

	public String getProfilePhoto() {
		return this.profilePhoto;
	}

	public void setProfilePhoto(String profilePhoto) {
		this.profilePhoto = profilePhoto;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public List<SysAuditEntry> getSysAuditEntries() {
		return this.sysAuditEntries;
	}

	public void setSysAuditEntries(List<SysAuditEntry> sysAuditEntries) {
		this.sysAuditEntries = sysAuditEntries;
	}

	public SysAuditEntry addSysAuditEntry(SysAuditEntry sysAuditEntry) {
		getSysAuditEntries().add(sysAuditEntry);
		sysAuditEntry.setSysUserAccount(this);

		return sysAuditEntry;
	}

	public SysAuditEntry removeSysAuditEntry(SysAuditEntry sysAuditEntry) {
		getSysAuditEntries().remove(sysAuditEntry);
		sysAuditEntry.setSysUserAccount(null);

		return sysAuditEntry;
	}

	public SysUserGroup getSysUserGroup() {
		return this.sysUserGroup;
	}

	public void setSysUserGroup(SysUserGroup sysUserGroup) {
		this.sysUserGroup = sysUserGroup;
	}

	public SysAccountStatus getSysAccountStatus() {
		return this.sysAccountStatus;
	}

	public void setSysAccountStatus(SysAccountStatus sysAccountStatus) {
		this.sysAccountStatus = sysAccountStatus;
	}

	public SysSalutation getSysSalutation() {
		return this.sysSalutation;
	}

	public void setSysSalutation(SysSalutation sysSalutation) {
		this.sysSalutation = sysSalutation;
	}

	public CoreDistrict getCoreDistrict() {
		return this.coreDistrict;
	}

	public void setCoreDistrict(CoreDistrict coreDistrict) {
		this.coreDistrict = coreDistrict;
	}

	public List<SysUserAccountActivation> getSysUserAccountActivations() {
		return this.sysUserAccountActivations;
	}

	public void setSysUserAccountActivations(List<SysUserAccountActivation> sysUserAccountActivations) {
		this.sysUserAccountActivations = sysUserAccountActivations;
	}

	public SysUserAccountActivation addSysUserAccountActivation(SysUserAccountActivation sysUserAccountActivation) {
		getSysUserAccountActivations().add(sysUserAccountActivation);
		sysUserAccountActivation.setSysUserAccount(this);

		return sysUserAccountActivation;
	}

	public SysUserAccountActivation removeSysUserAccountActivation(SysUserAccountActivation sysUserAccountActivation) {
		getSysUserAccountActivations().remove(sysUserAccountActivation);
		sysUserAccountActivation.setSysUserAccount(null);

		return sysUserAccountActivation;
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
}