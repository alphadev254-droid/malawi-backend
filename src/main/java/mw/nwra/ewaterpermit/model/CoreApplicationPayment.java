package mw.nwra.ewaterpermit.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_application_payment")
@NamedQuery(name = "CoreApplicationPayment.findAll", query = "SELECT c FROM CoreApplicationPayment c")
public class CoreApplicationPayment extends BaseEntity {

	@Column(name = "amount_paid")
	private double amountPaid;

	@Column(name = "payment_status")
	private String paymentStatus = "PENDING";

	@Column(name = "payment_method")
	private String paymentMethod;

	@Column(name = "receipt_document_id")
	private String receiptDocumentId;

	@Column(name = "needs_verification")
	private boolean needsVerification = false;

	@Column(name = "verification_notes")
	private String verificationNotes;

	@Column(name = "verified_by_user_id")
	private String verifiedByUserId;

	@Column(name = "verification_date")
	private java.sql.Timestamp verificationDate;

	// bi-directional many-to-one association to CoreLicenseApplication
	@JsonBackReference
	@ManyToOne
	@JoinColumn(name = "license_application_id")
	private CoreLicenseApplication coreLicenseApplication;

	// bi-directional many-to-one association to CoreFeesType
	@ManyToOne
	@JoinColumn(name = "fees_type_id")
	private CoreFeesType coreFeesType;

	// bi-directional many-to-one association to CoreLicense
	@ManyToOne
	@JoinColumn(name = "license_id")
	private CoreLicense coreLicense;

	// bi-directional many-to-one association to CoreFinancialYear
	@ManyToOne
	@JoinColumn(name = "financial_year_id")
	private CoreFinancialYear coreFinancialYear;

	public CoreApplicationPayment() {
	}

	public double getAmountPaid() {
		return this.amountPaid;
	}

	public void setAmountPaid(double amountPaid) {
		this.amountPaid = amountPaid;
	}

	public CoreLicenseApplication getCoreLicenseApplication() {
		return this.coreLicenseApplication;
	}

	public void setCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		this.coreLicenseApplication = coreLicenseApplication;
	}

	public CoreFeesType getCoreFeesType() {
		return this.coreFeesType;
	}

	public void setCoreFeesType(CoreFeesType coreFeesType) {
		this.coreFeesType = coreFeesType;
	}

	public String getPaymentStatus() {
		return this.paymentStatus;
	}

	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public String getPaymentMethod() {
		return this.paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getReceiptDocumentId() {
		return this.receiptDocumentId;
	}

	public void setReceiptDocumentId(String receiptDocumentId) {
		this.receiptDocumentId = receiptDocumentId;
	}

	public boolean isNeedsVerification() {
		return this.needsVerification;
	}

	public void setNeedsVerification(boolean needsVerification) {
		this.needsVerification = needsVerification;
	}

	public String getVerificationNotes() {
		return this.verificationNotes;
	}

	public void setVerificationNotes(String verificationNotes) {
		this.verificationNotes = verificationNotes;
	}

	public String getVerifiedByUserId() {
		return this.verifiedByUserId;
	}

	public void setVerifiedByUserId(String verifiedByUserId) {
		this.verifiedByUserId = verifiedByUserId;
	}

	public java.sql.Timestamp getVerificationDate() {
		return this.verificationDate;
	}

	public void setVerificationDate(java.sql.Timestamp verificationDate) {
		this.verificationDate = verificationDate;
	}

	public CoreLicense getCoreLicense() {
		return this.coreLicense;
	}

	public void setCoreLicense(CoreLicense coreLicense) {
		this.coreLicense = coreLicense;
	}

	public CoreFinancialYear getCoreFinancialYear() {
		return this.coreFinancialYear;
	}

	public void setCoreFinancialYear(CoreFinancialYear coreFinancialYear) {
		this.coreFinancialYear = coreFinancialYear;
	}

}