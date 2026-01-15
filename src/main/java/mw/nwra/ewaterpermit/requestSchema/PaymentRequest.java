package mw.nwra.ewaterpermit.requestSchema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class PaymentRequest {
    
    @NotBlank(message = "Application ID is required")
    private String applicationId;
    
    @NotBlank(message = "Payment type is required")
    private String paymentType; // APPLICATION_FEE, PERMIT_FEE, PENALTY, etc.
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency = "MWK";
    
    @NotBlank(message = "Customer phone number is required")
    private String customerPhoneNumber;
    
    @NotBlank(message = "Customer email is required")
    private String customerEmail;
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    private String description;
    
    // BOMAPay specific fields
    private String paymentMethod; // MOBILE_MONEY, BANK_TRANSFER, etc.
    private String orderNumber;
    private String orderId;
    private String returnUrl;
    private String failUrl;
    private String phoneNumber;
    
    // NPG Interface fields
    private String companyCode = "V130"; // BUKRS - Company Code
    private String documentType = "DZ"; // BLART - Document Type
    private String period; // MONAT - Financial Period
    private String postingKeyDebit = "40"; // NEWBS - Posting Key Debit
    private String glAccountDebit = "321220002"; // NEWKO - Cash Control GL Account
    private String postingKeyCredit = "50"; // NEWBS - Posting Key Credit
    private String revenueGlAccount = "141110001"; // NEWKO - Revenue GL Account
    private String taxCode = "A0"; // MWSKZ - Tax Code
    private String businessArea = "V130"; // GSBER - Business Area
    private String fund = "101"; // GEBER - Fund
    private String grant = "G001"; // GRANT_NBR - Grant
    private String functionalArea = "10"; // FKBER - Functional Area
    private String commitmentItem = "142110039"; // FIPEX - Commitment Item
    
    public PaymentRequest() {
    }
    
    public String getApplicationId() {
        return applicationId;
    }
    
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    
    public String getPaymentType() {
        return paymentType;
    }
    
    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
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
    
    public String getCustomerPhoneNumber() {
        return customerPhoneNumber;
    }
    
    public void setCustomerPhoneNumber(String customerPhoneNumber) {
        this.customerPhoneNumber = customerPhoneNumber;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getCompanyCode() {
        return companyCode;
    }
    
    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
    
    public String getPostingKeyDebit() {
        return postingKeyDebit;
    }
    
    public void setPostingKeyDebit(String postingKeyDebit) {
        this.postingKeyDebit = postingKeyDebit;
    }
    
    public String getGlAccountDebit() {
        return glAccountDebit;
    }
    
    public void setGlAccountDebit(String glAccountDebit) {
        this.glAccountDebit = glAccountDebit;
    }
    
    public String getPostingKeyCredit() {
        return postingKeyCredit;
    }
    
    public void setPostingKeyCredit(String postingKeyCredit) {
        this.postingKeyCredit = postingKeyCredit;
    }
    
    public String getRevenueGlAccount() {
        return revenueGlAccount;
    }
    
    public void setRevenueGlAccount(String revenueGlAccount) {
        this.revenueGlAccount = revenueGlAccount;
    }
    
    public String getTaxCode() {
        return taxCode;
    }
    
    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }
    
    public String getBusinessArea() {
        return businessArea;
    }
    
    public void setBusinessArea(String businessArea) {
        this.businessArea = businessArea;
    }
    
    public String getFund() {
        return fund;
    }
    
    public void setFund(String fund) {
        this.fund = fund;
    }
    
    public String getGrant() {
        return grant;
    }
    
    public void setGrant(String grant) {
        this.grant = grant;
    }
    
    public String getFunctionalArea() {
        return functionalArea;
    }
    
    public void setFunctionalArea(String functionalArea) {
        this.functionalArea = functionalArea;
    }
    
    public String getCommitmentItem() {
        return commitmentItem;
    }
    
    public void setCommitmentItem(String commitmentItem) {
        this.commitmentItem = commitmentItem;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getReturnUrl() {
        return returnUrl;
    }
    
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
    
    public String getFailUrl() {
        return failUrl;
    }
    
    public void setFailUrl(String failUrl) {
        this.failUrl = failUrl;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}