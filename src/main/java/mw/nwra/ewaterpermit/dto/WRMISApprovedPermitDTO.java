package mw.nwra.ewaterpermit.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for WRMIS Approved Permit Data Export
 * Contains all approved permit/license information for WRMIS integration
 */
public class WRMISApprovedPermitDTO {

    // License identifiers
    private String licenseId;
    private String permitNumber;
    private String licenseType;
    private String licenseStatus;
    private Integer licenseVersion;

    // Permit holder details (from application client_info)
    private String holderEmail;
    private String holderPhone;
    private String holderAddress;
    private String holderDistrict;

    // Approved permit details
    private BigDecimal approvedVolume; // From assessment.rental_quantity
    private String volumeUnit; // Always "m³"
    private Date dateIssued;
    private Date expirationDate;
    private String validityPeriod; // e.g., "5 years"

    // Related application
    private String applicationId;

    // Location details
    private String sourceLatitude;
    private String sourceLongitude;
    private String sourceVillage;
    private String sourceDistrict;
    private String sourceTA;
    private String catchmentArea;

    // Constructor
    public WRMISApprovedPermitDTO() {}

    // Getters and Setters
    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getPermitNumber() {
        return permitNumber;
    }

    public void setPermitNumber(String permitNumber) {
        this.permitNumber = permitNumber;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getLicenseStatus() {
        return licenseStatus;
    }

    public void setLicenseStatus(String licenseStatus) {
        this.licenseStatus = licenseStatus;
    }

    public Integer getLicenseVersion() {
        return licenseVersion;
    }

    public void setLicenseVersion(Integer licenseVersion) {
        this.licenseVersion = licenseVersion;
    }

    public String getHolderEmail() {
        return holderEmail;
    }

    public void setHolderEmail(String holderEmail) {
        this.holderEmail = holderEmail;
    }

    public String getHolderPhone() {
        return holderPhone;
    }

    public void setHolderPhone(String holderPhone) {
        this.holderPhone = holderPhone;
    }

    public String getHolderAddress() {
        return holderAddress;
    }

    public void setHolderAddress(String holderAddress) {
        this.holderAddress = holderAddress;
    }

    public String getHolderDistrict() {
        return holderDistrict;
    }

    public void setHolderDistrict(String holderDistrict) {
        this.holderDistrict = holderDistrict;
    }

    public BigDecimal getApprovedVolume() {
        return approvedVolume;
    }

    public void setApprovedVolume(BigDecimal approvedVolume) {
        this.approvedVolume = approvedVolume;
    }

    public String getVolumeUnit() {
        return volumeUnit;
    }

    public void setVolumeUnit(String volumeUnit) {
        this.volumeUnit = volumeUnit;
    }

    public Date getDateIssued() {
        return dateIssued;
    }

    public void setDateIssued(Date dateIssued) {
        this.dateIssued = dateIssued;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(String validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getSourceLatitude() {
        return sourceLatitude;
    }

    public void setSourceLatitude(String sourceLatitude) {
        this.sourceLatitude = sourceLatitude;
    }

    public String getSourceLongitude() {
        return sourceLongitude;
    }

    public void setSourceLongitude(String sourceLongitude) {
        this.sourceLongitude = sourceLongitude;
    }

    public String getSourceVillage() {
        return sourceVillage;
    }

    public void setSourceVillage(String sourceVillage) {
        this.sourceVillage = sourceVillage;
    }

    public String getSourceDistrict() {
        return sourceDistrict;
    }

    public void setSourceDistrict(String sourceDistrict) {
        this.sourceDistrict = sourceDistrict;
    }

    public String getSourceTA() {
        return sourceTA;
    }

    public void setSourceTA(String sourceTA) {
        this.sourceTA = sourceTA;
    }

    public String getCatchmentArea() {
        return catchmentArea;
    }

    public void setCatchmentArea(String catchmentArea) {
        this.catchmentArea = catchmentArea;
    }
}
