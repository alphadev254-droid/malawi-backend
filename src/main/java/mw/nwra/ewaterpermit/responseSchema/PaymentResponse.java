package mw.nwra.ewaterpermit.responseSchema;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class PaymentResponse {
    
    private String paymentReference;
    private String applicationId;
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String description;
    private String customerName;
    private String customerEmail;
    private String customerPhoneNumber;
    private Timestamp createdDate;
    private Timestamp completedDate;
    private String invoiceUrl;
    private String receiptUrl;
    private String errorMessage;
    private String transactionId;
    private String bomaPayReference;
    private String npgReference;
    
    // BOMAPay specific fields
    private String orderId;
    private String formUrl;
    private Integer orderStatus;
    private String paymentStatus;
    private BigDecimal amountDue;
    private Timestamp dueDate;
    private String phoneNumber;
    
    public PaymentResponse() {
    }
    
    public PaymentResponse(String paymentReference, String status, BigDecimal amount, String currency) {
        this.paymentReference = paymentReference;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
    }
    
    public String getPaymentReference() {
        return paymentReference;
    }
    
    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
    
    public String getApplicationId() {
        return applicationId;
    }
    
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public String getCustomerPhoneNumber() {
        return customerPhoneNumber;
    }
    
    public void setCustomerPhoneNumber(String customerPhoneNumber) {
        this.customerPhoneNumber = customerPhoneNumber;
    }
    
    public Timestamp getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }
    
    public Timestamp getCompletedDate() {
        return completedDate;
    }
    
    public void setCompletedDate(Timestamp completedDate) {
        this.completedDate = completedDate;
    }
    
    public String getInvoiceUrl() {
        return invoiceUrl;
    }
    
    public void setInvoiceUrl(String invoiceUrl) {
        this.invoiceUrl = invoiceUrl;
    }
    
    public String getReceiptUrl() {
        return receiptUrl;
    }
    
    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getBomaPayReference() {
        return bomaPayReference;
    }
    
    public void setBomaPayReference(String bomaPayReference) {
        this.bomaPayReference = bomaPayReference;
    }
    
    public String getNpgReference() {
        return npgReference;
    }
    
    public void setNpgReference(String npgReference) {
        this.npgReference = npgReference;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getFormUrl() {
        return formUrl;
    }
    
    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }
    
    public Integer getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public BigDecimal getAmountDue() {
        return amountDue;
    }
    
    public void setAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue;
    }
    
    public Timestamp getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}