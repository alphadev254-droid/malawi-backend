package mw.nwra.ewaterpermit.model;

import java.sql.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "core_license")
@NamedQuery(name = "CoreLicense.findAll", query = "SELECT c FROM CoreLicense c")
public class CoreLicense extends BaseEntity {
	@Temporal(TemporalType.DATE)
	@Column(name = "date_issued")
	private Date dateIssued;

	@Column(name = "document_url")
	private String documentUrl;

	@Temporal(TemporalType.DATE)
	@Column(name = "expiration_date")
	private Date expirationDate;

	@Column(name = "license_number")
	private String licenseNumber;

	@Column(name = "status")
	private String status = "ACTIVE";

	@Column(name = "parent_license_id")
	private String parentLicenseId;

	@Column(name = "license_version")
	private Integer licenseVersion = 1;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date_updated")
	private java.sql.Timestamp dateUpdated;

	// Notification tracking fields
	@Temporal(TemporalType.DATE)
	@Column(name = "notification_3_months_sent")
	private Date notification3MonthsSent;

	@Temporal(TemporalType.DATE)
	@Column(name = "notification_2_months_sent")
	private Date notification2MonthsSent;

	@Temporal(TemporalType.DATE)
	@Column(name = "notification_1_month_sent")
	private Date notification1MonthSent;

	@Temporal(TemporalType.DATE)
	@Column(name = "notification_1_week_sent")
	private Date notification1WeekSent;

	// bi-directional many-to-one association to CoreLicenseApplication
	@ManyToOne
	@JoinColumn(name = "license_application_id")
	private CoreLicenseApplication coreLicenseApplication;

	// bi-directional one-to-many association to CoreApplicationPayment
	@JsonIgnore
	@OneToMany(mappedBy = "coreLicense")
	private List<CoreApplicationPayment> coreApplicationPayments;

	public CoreLicense() {
	}

	public Date getDateIssued() {
		return this.dateIssued;
	}

	public void setDateIssued(Date dateIssued) {
		this.dateIssued = dateIssued;
	}

	public String getDocumentUrl() {
		return this.documentUrl;
	}

	public void setDocumentUrl(String documentUrl) {
		this.documentUrl = documentUrl;
	}

	public Date getExpirationDate() {
		return this.expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getLicenseNumber() {
		return this.licenseNumber;
	}

	public void setLicenseNumber(String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getParentLicenseId() {
		return this.parentLicenseId;
	}

	public void setParentLicenseId(String parentLicenseId) {
		this.parentLicenseId = parentLicenseId;
	}

	public Integer getLicenseVersion() {
		return this.licenseVersion;
	}

	public void setLicenseVersion(Integer licenseVersion) {
		this.licenseVersion = licenseVersion;
	}

	public java.sql.Timestamp getDateUpdated() {
		return this.dateUpdated;
	}

	public void setDateUpdated(java.sql.Timestamp dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	public CoreLicenseApplication getCoreLicenseApplication() {
		return this.coreLicenseApplication;
	}

	public void setCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		this.coreLicenseApplication = coreLicenseApplication;
	}

	public List<CoreApplicationPayment> getCoreApplicationPayments() {
		return this.coreApplicationPayments;
	}

	public void setCoreApplicationPayments(List<CoreApplicationPayment> coreApplicationPayments) {
		this.coreApplicationPayments = coreApplicationPayments;
	}

	public CoreApplicationPayment addCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
		getCoreApplicationPayments().add(coreApplicationPayment);
		coreApplicationPayment.setCoreLicense(this);

		return coreApplicationPayment;
	}

	public CoreApplicationPayment removeCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
		getCoreApplicationPayments().remove(coreApplicationPayment);
		coreApplicationPayment.setCoreLicense(null);

		return coreApplicationPayment;
	}

	// Notification tracking getters and setters
	public Date getNotification3MonthsSent() {
		return this.notification3MonthsSent;
	}

	public void setNotification3MonthsSent(Date notification3MonthsSent) {
		this.notification3MonthsSent = notification3MonthsSent;
	}

	public Date getNotification2MonthsSent() {
		return this.notification2MonthsSent;
	}

	public void setNotification2MonthsSent(Date notification2MonthsSent) {
		this.notification2MonthsSent = notification2MonthsSent;
	}

	public Date getNotification1MonthSent() {
		return this.notification1MonthSent;
	}

	public void setNotification1MonthSent(Date notification1MonthSent) {
		this.notification1MonthSent = notification1MonthSent;
	}

	public Date getNotification1WeekSent() {
		return this.notification1WeekSent;
	}

	public void setNotification1WeekSent(Date notification1WeekSent) {
		this.notification1WeekSent = notification1WeekSent;
	}

}