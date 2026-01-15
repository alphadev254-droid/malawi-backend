package mw.nwra.ewaterpermit.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Lightweight DTO for license list display
 * Contains only fields needed by frontend table
 */
public class LicenseListDto {
    private String id;
    private String licenseNumber;
    private String licenseTypeName;
    private Date dateIssued;
    private Date expirationDate;
    private String status;
    private Date dateUpdated;
    private String applicationStatusName;
    private Integer licenseVersion;
    private String parentLicenseId;

    // Owner info for managers
    private String firstName;
    private String lastName;
    private String emailAddress;

    // Assessment data for managers
    private BigDecimal calculatedAnnualRental;
    private BigDecimal rentalQuantity;
    private BigDecimal rentalRate;
    private Date recommendedScheduleDate;
    private String assessmentNotes;
    private String licenseOfficerId;
    private String assessmentStatus;
    private String assessmentFilesUpload;

    // Application data for license display
    private String locationInfo;  // JSON field for coordinates, village, district
    private String applicationMetadata;  // JSON field for purpose/water uses
    private String formSpecificData;  // JSON field for form-specific data (depth, waterUses, etc.)
    
    public LicenseListDto() {}
    
    public LicenseListDto(String id, String licenseNumber, String licenseTypeName,
                         Date dateIssued, Date expirationDate, String status, Date dateUpdated,
                         String applicationStatusName, Integer licenseVersion, String parentLicenseId) {
        this.id = id;
        this.licenseNumber = licenseNumber;
        this.licenseTypeName = licenseTypeName;
        this.dateIssued = dateIssued;
        this.expirationDate = expirationDate;
        this.status = status;
        this.dateUpdated = dateUpdated;
        this.applicationStatusName = applicationStatusName;
        this.licenseVersion = licenseVersion;
        this.parentLicenseId = parentLicenseId;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    
    public String getLicenseTypeName() { return licenseTypeName; }
    public void setLicenseTypeName(String licenseTypeName) { this.licenseTypeName = licenseTypeName; }
    
    public Date getDateIssued() { return dateIssued; }
    public void setDateIssued(Date dateIssued) { this.dateIssued = dateIssued; }
    
    public Date getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Date getDateUpdated() { return dateUpdated; }
    public void setDateUpdated(Date dateUpdated) { this.dateUpdated = dateUpdated; }
    
    public String getApplicationStatusName() { return applicationStatusName; }
    public void setApplicationStatusName(String applicationStatusName) { this.applicationStatusName = applicationStatusName; }
    
    public Integer getLicenseVersion() { return licenseVersion; }
    public void setLicenseVersion(Integer licenseVersion) { this.licenseVersion = licenseVersion; }
    
    public String getParentLicenseId() { return parentLicenseId; }
    public void setParentLicenseId(String parentLicenseId) { this.parentLicenseId = parentLicenseId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    // Assessment data getters and setters
    public BigDecimal getCalculatedAnnualRental() { return calculatedAnnualRental; }
    public void setCalculatedAnnualRental(BigDecimal calculatedAnnualRental) { this.calculatedAnnualRental = calculatedAnnualRental; }

    public BigDecimal getRentalQuantity() { return rentalQuantity; }
    public void setRentalQuantity(BigDecimal rentalQuantity) { this.rentalQuantity = rentalQuantity; }

    public BigDecimal getRentalRate() { return rentalRate; }
    public void setRentalRate(BigDecimal rentalRate) { this.rentalRate = rentalRate; }

    public Date getRecommendedScheduleDate() { return recommendedScheduleDate; }
    public void setRecommendedScheduleDate(Date recommendedScheduleDate) { this.recommendedScheduleDate = recommendedScheduleDate; }

    public String getAssessmentNotes() { return assessmentNotes; }
    public void setAssessmentNotes(String assessmentNotes) { this.assessmentNotes = assessmentNotes; }

    public String getLicenseOfficerId() { return licenseOfficerId; }
    public void setLicenseOfficerId(String licenseOfficerId) { this.licenseOfficerId = licenseOfficerId; }

    public String getAssessmentStatus() { return assessmentStatus; }
    public void setAssessmentStatus(String assessmentStatus) { this.assessmentStatus = assessmentStatus; }

    public String getAssessmentFilesUpload() { return assessmentFilesUpload; }
    public void setAssessmentFilesUpload(String assessmentFilesUpload) { this.assessmentFilesUpload = assessmentFilesUpload; }

    public String getLocationInfo() { return locationInfo; }
    public void setLocationInfo(String locationInfo) { this.locationInfo = locationInfo; }

    public String getApplicationMetadata() { return applicationMetadata; }
    public void setApplicationMetadata(String applicationMetadata) { this.applicationMetadata = applicationMetadata; }

    public String getFormSpecificData() { return formSpecificData; }
    public void setFormSpecificData(String formSpecificData) { this.formSpecificData = formSpecificData; }
}