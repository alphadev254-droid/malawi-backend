package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "boma_pay_transaction_history")
@NamedQuery(name = "BomaPayTransactionHistory.findAll", query = "SELECT b FROM BomaPayTransactionHistory b")
public class BomaPayTransactionHistory extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "payment_type", nullable = false)
    private String paymentType; // APPLICATION_FEE or LICENSE_FEE

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus; // REGISTERED, PROCESSING, COMPLETED, DECLINED, REVERSED, etc.

    @Column(name = "boma_pay_status")
    private String bomaPayStatus; // Raw status from BomaPay API

    @Column(name = "form_url", length = 500)
    private String formUrl;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(name = "initiated_by_user_id", nullable = false)
    private String initiatedByUserId;

    @Column(name = "initiated_date", nullable = false)
    private java.sql.Timestamp initiatedDate;

    @Column(name = "completed_date")
    private java.sql.Timestamp completedDate;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "webhook_received")
    private boolean webhookReceived = false;

    @Column(name = "webhook_data", length = 2000)
    private String webhookData;

    @Column(name = "return_url_visited")
    private boolean returnUrlVisited = false;

    @Column(name = "notes", length = 1000)
    private String notes;

    // bi-directional many-to-one association to CoreLicenseApplication
    @ManyToOne
    @JoinColumn(name = "license_application_id", nullable = false)
    private CoreLicenseApplication coreLicenseApplication;

    // bi-directional many-to-one association to SysUserAccount
    @ManyToOne
    @JoinColumn(name = "initiated_by_user_id", insertable = false, updatable = false)
    private SysUserAccount initiatedByUser;

    public BomaPayTransactionHistory() {
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getBomaPayStatus() {
        return bomaPayStatus;
    }

    public void setBomaPayStatus(String bomaPayStatus) {
        this.bomaPayStatus = bomaPayStatus;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getInitiatedByUserId() {
        return initiatedByUserId;
    }

    public void setInitiatedByUserId(String initiatedByUserId) {
        this.initiatedByUserId = initiatedByUserId;
    }

    public java.sql.Timestamp getInitiatedDate() {
        return initiatedDate;
    }

    public void setInitiatedDate(java.sql.Timestamp initiatedDate) {
        this.initiatedDate = initiatedDate;
    }

    public java.sql.Timestamp getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(java.sql.Timestamp completedDate) {
        this.completedDate = completedDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isWebhookReceived() {
        return webhookReceived;
    }

    public void setWebhookReceived(boolean webhookReceived) {
        this.webhookReceived = webhookReceived;
    }

    public String getWebhookData() {
        return webhookData;
    }

    public void setWebhookData(String webhookData) {
        this.webhookData = webhookData;
    }

    public boolean isReturnUrlVisited() {
        return returnUrlVisited;
    }

    public void setReturnUrlVisited(boolean returnUrlVisited) {
        this.returnUrlVisited = returnUrlVisited;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public CoreLicenseApplication getCoreLicenseApplication() {
        return coreLicenseApplication;
    }

    public void setCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
        this.coreLicenseApplication = coreLicenseApplication;
    }

    public SysUserAccount getInitiatedByUser() {
        return initiatedByUser;
    }

    public void setInitiatedByUser(SysUserAccount initiatedByUser) {
        this.initiatedByUser = initiatedByUser;
    }
}