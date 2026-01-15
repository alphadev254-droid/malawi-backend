package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_license_permit")
@NamedQuery(name = "CoreLicensePermit.findAll", query = "SELECT c FROM CoreLicensePermit c")
public class CoreLicensePermit extends BaseEntity {

	@Column(name = "permit_number")
	private String permitNumber;

	@Column(name = "permit_status")
	private String permitStatus = "PENDING_PAYMENT";

	@Column(name = "issue_date")
	private java.sql.Timestamp issueDate;

	@Column(name = "expiry_date")
	private java.sql.Timestamp expiryDate;

	@Column(name = "permit_document_path")
	private String permitDocumentPath;

	@Column(name = "invoice_amount")
	private double invoiceAmount;

	@Column(name = "invoice_generated_date")
	private java.sql.Timestamp invoiceGeneratedDate;

	@Column(name = "payment_verified")
	private boolean paymentVerified = false;

	@Column(name = "payment_verified_by_user_id")
	private String paymentVerifiedByUserId;

	@Column(name = "payment_verification_date")
	private java.sql.Timestamp paymentVerificationDate;

	@Column(name = "permit_downloadable")
	private boolean permitDownloadable = false;

	@Column(name = "qr_code_data")
	private String qrCodeData;

	@Column(name = "conditions_text", columnDefinition = "TEXT")
	private String conditionsText;

	@Column(name = "director_name")
	private String directorName = "Dwight Kambuku PhD";

	@Column(name = "contact_phone")
	private String contactPhone = "+265 (0) 995 511 963";

	@Column(name = "contact_email")
	private String contactEmail = "ceo@nwra.mw";

	// bi-directional many-to-one association to CoreLicenseApplication
	@ManyToOne
	@JoinColumn(name = "license_application_id")
	private CoreLicenseApplication coreLicenseApplication;

	// bi-directional many-to-one association to CoreApplicationPayment
	@ManyToOne
	@JoinColumn(name = "payment_id")
	private CoreApplicationPayment coreApplicationPayment;

	public CoreLicensePermit() {
	}

	public String getPermitNumber() {
		return this.permitNumber;
	}

	public void setPermitNumber(String permitNumber) {
		this.permitNumber = permitNumber;
	}

	public String getPermitStatus() {
		return this.permitStatus;
	}

	public void setPermitStatus(String permitStatus) {
		this.permitStatus = permitStatus;
	}

	public java.sql.Timestamp getIssueDate() {
		return this.issueDate;
	}

	public void setIssueDate(java.sql.Timestamp issueDate) {
		this.issueDate = issueDate;
	}

	public java.sql.Timestamp getExpiryDate() {
		return this.expiryDate;
	}

	public void setExpiryDate(java.sql.Timestamp expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getPermitDocumentPath() {
		return this.permitDocumentPath;
	}

	public void setPermitDocumentPath(String permitDocumentPath) {
		this.permitDocumentPath = permitDocumentPath;
	}

	public double getInvoiceAmount() {
		return this.invoiceAmount;
	}

	public void setInvoiceAmount(double invoiceAmount) {
		this.invoiceAmount = invoiceAmount;
	}

	public java.sql.Timestamp getInvoiceGeneratedDate() {
		return this.invoiceGeneratedDate;
	}

	public void setInvoiceGeneratedDate(java.sql.Timestamp invoiceGeneratedDate) {
		this.invoiceGeneratedDate = invoiceGeneratedDate;
	}

	public boolean isPaymentVerified() {
		return this.paymentVerified;
	}

	public void setPaymentVerified(boolean paymentVerified) {
		this.paymentVerified = paymentVerified;
	}

	public String getPaymentVerifiedByUserId() {
		return this.paymentVerifiedByUserId;
	}

	public void setPaymentVerifiedByUserId(String paymentVerifiedByUserId) {
		this.paymentVerifiedByUserId = paymentVerifiedByUserId;
	}

	public java.sql.Timestamp getPaymentVerificationDate() {
		return this.paymentVerificationDate;
	}

	public void setPaymentVerificationDate(java.sql.Timestamp paymentVerificationDate) {
		this.paymentVerificationDate = paymentVerificationDate;
	}

	public boolean isPermitDownloadable() {
		return this.permitDownloadable;
	}

	public void setPermitDownloadable(boolean permitDownloadable) {
		this.permitDownloadable = permitDownloadable;
	}

	public CoreLicenseApplication getCoreLicenseApplication() {
		return this.coreLicenseApplication;
	}

	public void setCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		this.coreLicenseApplication = coreLicenseApplication;
	}

	public CoreApplicationPayment getCoreApplicationPayment() {
		return this.coreApplicationPayment;
	}

	public void setCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
		this.coreApplicationPayment = coreApplicationPayment;
	}

	/**
	 * Generate permit number based on application details
	 */
	public void generatePermitNumber() {
		if (this.coreLicenseApplication != null) {
			String prefix = "NWRA";
			String year = String.valueOf(java.time.LocalDate.now().getYear());
			String appId = this.coreLicenseApplication.getId().substring(0, 8).toUpperCase();
			this.permitNumber = prefix + "-" + year + "-" + appId;
		}
	}

	/**
	 * Calculate expiry date based on license type validity
	 */
	public void calculateExpiryDate() {
		if (this.coreLicenseApplication != null &&
			this.coreLicenseApplication.getCoreLicenseType() != null &&
			this.issueDate != null) {

			int validityLength = this.coreLicenseApplication.getCoreLicenseType().getDefaultValidityLength();
			java.time.LocalDateTime issueDateTime = this.issueDate.toLocalDateTime();
			// Add years (not months) and subtract 1 day
			java.time.LocalDateTime expiryDateTime = issueDateTime.plusYears(validityLength).minusDays(1);
			this.expiryDate = java.sql.Timestamp.valueOf(expiryDateTime);
		}
	}

	/**
	 * Generate QR code data for the permit
	 */
	public void generateQRCodeData() {
		if (this.coreLicenseApplication != null) {
			String applicantName = "Unknown";
			if (this.coreLicenseApplication.getSysUserAccount() != null) {
				String firstName = this.coreLicenseApplication.getSysUserAccount().getFirstName() != null ? 
					this.coreLicenseApplication.getSysUserAccount().getFirstName() : "";
				String lastName = this.coreLicenseApplication.getSysUserAccount().getLastName() != null ? 
					this.coreLicenseApplication.getSysUserAccount().getLastName() : "";
				applicantName = (firstName + " " + lastName).trim();
				if (applicantName.isEmpty()) {
					applicantName = this.coreLicenseApplication.getSysUserAccount().getUsername();
				}
			}

			java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
			String issueStr = this.issueDate != null ? this.issueDate.toLocalDateTime().format(formatter) : "TBD";
			String expiryStr = this.expiryDate != null ? this.expiryDate.toLocalDateTime().format(formatter) : "TBD";

			this.qrCodeData = String.format("NWRA %s PERMIT\nNumber: %s\nHolder: %s\nValid: %s to %s",
				this.coreLicenseApplication.getCoreLicenseType() != null ? 
					this.coreLicenseApplication.getCoreLicenseType().getName().toUpperCase() : "LICENSE",
				this.permitNumber,
				applicantName,
				issueStr,
				expiryStr
			);
		}
	}

	/**
	 * Set default conditions for the permit
	 */
	public void setDefaultConditions() {
		this.conditionsText = String.join("\n", 
			"Backfill dry boreholes when encountered",
			"Compile and submit all drilling data to NWRA", 
			"Adhere to standard borehole specifications",
			"Report unusual hydrogeological findings",
			"Only drill for clients with valid abstraction permits"
		);
	}

	// Getters and setters for new fields
	public String getQrCodeData() {
		return this.qrCodeData;
	}

	public void setQrCodeData(String qrCodeData) {
		this.qrCodeData = qrCodeData;
	}

	public String getConditionsText() {
		return this.conditionsText;
	}

	public void setConditionsText(String conditionsText) {
		this.conditionsText = conditionsText;
	}

	public String getDirectorName() {
		return this.directorName;
	}

	public void setDirectorName(String directorName) {
		this.directorName = directorName;
	}

	public String getContactPhone() {
		return this.contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getContactEmail() {
		return this.contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

}