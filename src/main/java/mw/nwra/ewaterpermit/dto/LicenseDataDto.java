package mw.nwra.ewaterpermit.dto;

import java.util.Date;

/**
 * Lightweight DTO for license data display
 * Contains only fields needed by license wrapper component
 */
public class LicenseDataDto {
    private String permitNumber;
    private String applicantName;
    private String sourceOwnerFullname;
    private Date issueDate;
    private Date expiryDate;
    private String directorName;
    private String contactPhone;
    private String contactEmail;
    private String qrCodeData;
    private String conditionsText;
    
    // Status fields
    private boolean licenseAvailable;
    private boolean paymentRequired;
    private boolean verificationPending;
    private String message;
    
    // Payment fields
    private String paymentStatus;
    private Double amountPaid;
    
    public LicenseDataDto() {}
    
    // Constructor for successful license data
    public LicenseDataDto(String permitNumber, String applicantName, String sourceOwnerFullname,
                         Date issueDate, Date expiryDate, String directorName, String contactPhone,
                         String contactEmail, String conditionsText) {
        this.permitNumber = permitNumber;
        this.applicantName = applicantName;
        this.sourceOwnerFullname = sourceOwnerFullname;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.directorName = directorName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.conditionsText = conditionsText;
        this.licenseAvailable = true;
        this.paymentRequired = false;
        this.verificationPending = false;
    }
    
    // Constructor for license with restrictions
    public LicenseDataDto(String message, boolean paymentRequired, boolean verificationPending) {
        this.licenseAvailable = false;
        this.paymentRequired = paymentRequired;
        this.verificationPending = verificationPending;
        this.message = message;
    }
    
    // Getters and setters
    public String getPermitNumber() { return permitNumber; }
    public void setPermitNumber(String permitNumber) { this.permitNumber = permitNumber; }
    
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    
    public String getSourceOwnerFullname() { return sourceOwnerFullname; }
    public void setSourceOwnerFullname(String sourceOwnerFullname) { this.sourceOwnerFullname = sourceOwnerFullname; }
    
    public Date getIssueDate() { return issueDate; }
    public void setIssueDate(Date issueDate) { this.issueDate = issueDate; }
    
    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
    
    public String getDirectorName() { return directorName; }
    public void setDirectorName(String directorName) { this.directorName = directorName; }
    
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    
    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }
    
    public String getConditionsText() { return conditionsText; }
    public void setConditionsText(String conditionsText) { this.conditionsText = conditionsText; }
    
    public boolean isLicenseAvailable() { return licenseAvailable; }
    public void setLicenseAvailable(boolean licenseAvailable) { this.licenseAvailable = licenseAvailable; }
    
    public boolean isPaymentRequired() { return paymentRequired; }
    public void setPaymentRequired(boolean paymentRequired) { this.paymentRequired = paymentRequired; }
    
    public boolean isVerificationPending() { return verificationPending; }
    public void setVerificationPending(boolean verificationPending) { this.verificationPending = verificationPending; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public Double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(Double amountPaid) { this.amountPaid = amountPaid; }
}