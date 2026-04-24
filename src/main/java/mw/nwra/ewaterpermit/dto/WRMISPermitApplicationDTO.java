package mw.nwra.ewaterpermit.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for WRMIS Permit Application Data Export
 * Contains all permit application information for WRMIS integration
 */
public class WRMISPermitApplicationDTO {

    // Application identifiers
    private String applicationId;
    private String applicationNumber;
    private String applicationType; // NEW, RENEWAL, TRANSFER, VARIATION
    private String licenseType;     // GROUND_WATER, SURFACE_WATER, IRRIGATION, DOMESTIC, DRILLING, etc.
    private String category;        // Raw category from CSV: "Ground Water", "Surface Water", etc.
    private String applicationStatus;
    private Date dateSubmitted;
    private Date dateUpdated;
    private Date expiryDate;

    // Applicant details
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantMobile;
    private String applicantAddress;
    private String applicantPhysicalAddress;
    private String applicantContactPerson;
    private String applicantDistrict;
    private String applicantTA;

    // Requested permit details
    private BigDecimal requestedVolume;
    private String volumeUnit;
    private Double permitDuration;
    private String waterSource;   // e.g. "Borehole", "Lake Malawi", "Shire River"
    private String waterUse;      // e.g. "Irrigation", "Domestic", "Livestock"
    private String granterName;

    // Location information
    private String sourceLatitude;
    private String sourceLongitude;
    private String sourceVillage;
    private String sourceTA;
    private String sourceDistrict;

    public WRMISPermitApplicationDTO() {}

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }

    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }

    public Date getDateSubmitted() { return dateSubmitted; }
    public void setDateSubmitted(Date dateSubmitted) { this.dateSubmitted = dateSubmitted; }

    public Date getDateUpdated() { return dateUpdated; }
    public void setDateUpdated(Date dateUpdated) { this.dateUpdated = dateUpdated; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }

    public String getApplicantPhone() { return applicantPhone; }
    public void setApplicantPhone(String applicantPhone) { this.applicantPhone = applicantPhone; }

    public String getApplicantMobile() { return applicantMobile; }
    public void setApplicantMobile(String applicantMobile) { this.applicantMobile = applicantMobile; }

    public String getApplicantAddress() { return applicantAddress; }
    public void setApplicantAddress(String applicantAddress) { this.applicantAddress = applicantAddress; }

    public String getApplicantPhysicalAddress() { return applicantPhysicalAddress; }
    public void setApplicantPhysicalAddress(String applicantPhysicalAddress) { this.applicantPhysicalAddress = applicantPhysicalAddress; }

    public String getApplicantContactPerson() { return applicantContactPerson; }
    public void setApplicantContactPerson(String applicantContactPerson) { this.applicantContactPerson = applicantContactPerson; }

    public String getApplicantDistrict() { return applicantDistrict; }
    public void setApplicantDistrict(String applicantDistrict) { this.applicantDistrict = applicantDistrict; }

    public String getApplicantTA() { return applicantTA; }
    public void setApplicantTA(String applicantTA) { this.applicantTA = applicantTA; }

    public BigDecimal getRequestedVolume() { return requestedVolume; }
    public void setRequestedVolume(BigDecimal requestedVolume) { this.requestedVolume = requestedVolume; }

    public String getVolumeUnit() { return volumeUnit; }
    public void setVolumeUnit(String volumeUnit) { this.volumeUnit = volumeUnit; }

    public Double getPermitDuration() { return permitDuration; }
    public void setPermitDuration(Double permitDuration) { this.permitDuration = permitDuration; }

    public String getWaterSource() { return waterSource; }
    public void setWaterSource(String waterSource) { this.waterSource = waterSource; }

    public String getWaterUse() { return waterUse; }
    public void setWaterUse(String waterUse) { this.waterUse = waterUse; }

    public String getGranterName() { return granterName; }
    public void setGranterName(String granterName) { this.granterName = granterName; }

    public String getSourceLatitude() { return sourceLatitude; }
    public void setSourceLatitude(String sourceLatitude) { this.sourceLatitude = sourceLatitude; }

    public String getSourceLongitude() { return sourceLongitude; }
    public void setSourceLongitude(String sourceLongitude) { this.sourceLongitude = sourceLongitude; }

    public String getSourceVillage() { return sourceVillage; }
    public void setSourceVillage(String sourceVillage) { this.sourceVillage = sourceVillage; }

    public String getSourceTA() { return sourceTA; }
    public void setSourceTA(String sourceTA) { this.sourceTA = sourceTA; }

    public String getSourceDistrict() { return sourceDistrict; }
    public void setSourceDistrict(String sourceDistrict) { this.sourceDistrict = sourceDistrict; }
}
