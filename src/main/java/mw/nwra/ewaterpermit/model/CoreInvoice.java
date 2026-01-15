package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_invoice")
@NamedQuery(name = "CoreInvoice.findAll", query = "SELECT c FROM CoreInvoice c")
public class CoreInvoice extends BaseEntity {

	@Column(name = "invoice_number")
	private String invoiceNumber;

	@Column(name = "invoice_type")
	private String invoiceType; // APPLICATION_FEE, LICENSE_FEE

	@Column(name = "invoice_status")
	private String invoiceStatus = "PENDING"; // PENDING, PAID, OVERDUE, CANCELLED

	@Column(name = "amount")
	private double amount;

	@Column(name = "currency")
	private String currency = "MWK";

	@Column(name = "issue_date")
	private java.sql.Timestamp issueDate;

	@Column(name = "due_date")
	private java.sql.Timestamp dueDate;

	@Column(name = "paid_date")
	private java.sql.Timestamp paidDate;

	@Column(name = "description")
	private String description;

	@Column(name = "bank_account")
	private String bankAccount = "1234567890";

	@Column(name = "bank_name")
	private String bankName = "National Reserve Bank";

	@Column(name = "branch_code")
	private String branchCode = "987654";

	@Column(name = "swift_code")
	private String swiftCode = "NRWAMWMW";

	@Column(name = "payment_instructions", columnDefinition = "TEXT")
	private String paymentInstructions;

	// bi-directional many-to-one association to CoreLicenseApplication
	@ManyToOne
	@JoinColumn(name = "license_application_id")
	private CoreLicenseApplication coreLicenseApplication;

	// bi-directional many-to-one association to CoreApplicationPayment (nullable)
	@ManyToOne
	@JoinColumn(name = "payment_id")
	private CoreApplicationPayment coreApplicationPayment;

	public CoreInvoice() {
	}

	public String getInvoiceNumber() {
		return this.invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public String getInvoiceType() {
		return this.invoiceType;
	}

	public void setInvoiceType(String invoiceType) {
		this.invoiceType = invoiceType;
	}

	public String getInvoiceStatus() {
		return this.invoiceStatus;
	}

	public void setInvoiceStatus(String invoiceStatus) {
		this.invoiceStatus = invoiceStatus;
	}

	public double getAmount() {
		return this.amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return this.currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public java.sql.Timestamp getIssueDate() {
		return this.issueDate;
	}

	public void setIssueDate(java.sql.Timestamp issueDate) {
		this.issueDate = issueDate;
	}

	public java.sql.Timestamp getDueDate() {
		return this.dueDate;
	}

	public void setDueDate(java.sql.Timestamp dueDate) {
		this.dueDate = dueDate;
	}

	public java.sql.Timestamp getPaidDate() {
		return this.paidDate;
	}

	public void setPaidDate(java.sql.Timestamp paidDate) {
		this.paidDate = paidDate;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getBankAccount() {
		return this.bankAccount;
	}

	public void setBankAccount(String bankAccount) {
		this.bankAccount = bankAccount;
	}

	public String getBankName() {
		return this.bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getBranchCode() {
		return this.branchCode;
	}

	public void setBranchCode(String branchCode) {
		this.branchCode = branchCode;
	}

	public String getSwiftCode() {
		return this.swiftCode;
	}

	public void setSwiftCode(String swiftCode) {
		this.swiftCode = swiftCode;
	}

	public String getPaymentInstructions() {
		return this.paymentInstructions;
	}

	public void setPaymentInstructions(String paymentInstructions) {
		this.paymentInstructions = paymentInstructions;
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
	 * Generate invoice number based on type and application
	 */
	public void generateInvoiceNumber() {
		if (this.coreLicenseApplication != null) {
			String prefix = "APPLICATION_FEE".equals(this.invoiceType) ? "INV-APP" : "INV-LIC";
			String year = String.valueOf(java.time.LocalDate.now().getYear());
			String appId = this.coreLicenseApplication.getId().substring(0, 8).toUpperCase();
			this.invoiceNumber = prefix + "-" + year + "-" + appId;
		}
	}

	/**
	 * Set default payment instructions
	 */
	public void setDefaultPaymentInstructions() {
		this.paymentInstructions = String.join("\n",
			"Payment can be made through the following methods:",
			"1. Online Payment: Use our secure online payment portal",
			"2. Bank Transfer: Transfer to account details provided above",
			"3. Cash Payment: Visit our offices during business hours",
			"",
			"Please quote your invoice number when making payment.",
			"Contact us at +265 (0) 995 511 963 for payment assistance."
		);
	}

	/**
	 * Calculate due date (typically 30 days from issue date)
	 */
	public void calculateDueDate() {
		if (this.issueDate != null) {
			java.time.LocalDateTime issueDT = this.issueDate.toLocalDateTime();
			java.time.LocalDateTime dueDT = issueDT.plusDays(30);
			this.dueDate = java.sql.Timestamp.valueOf(dueDT);
		}
	}
}