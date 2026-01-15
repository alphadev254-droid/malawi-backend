package mw.nwra.ewaterpermit.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "core_license_assessment")
@NamedQuery(name = "CoreLicenseAssessment.findAll", query = "SELECT c FROM CoreLicenseAssessment c")
public class CoreLicenseAssessment extends BaseEntity {

    @Column(name = "license_application_id", length = 36, nullable = false)
    public String licenseApplicationId;

    @Column(name = "assessment_status", length = 20, nullable = false)
    public String assessmentStatus; // e.g., Pending, Scheduled, Approved, Completed

    @Column(name = "assessment_files_upload", columnDefinition = "TEXT")
    public String assessmentFilesUpload; // JSON array of file paths

    @Column(name = "calculated_annual_rental", precision = 15, scale = 2)
    public BigDecimal calculatedAnnualRental;

    @Column(name = "rental_quantity", precision = 10, scale = 2)
    public BigDecimal rentalQuantity;

    @Column(name = "rental_rate", precision = 10, scale = 2)
    public BigDecimal rentalRate;

    @Column(name = "recommended_schedule_date")
    @Temporal(TemporalType.DATE)
    public Date recommendedScheduleDate;

    @Column(name = "assessment_notes", columnDefinition = "TEXT")
    public String assessmentNotes;

    @Column(name = "license_officer_id", length = 36)
    public String licenseOfficerId;

    @Column(name = "license_manager_id", length = 36)
    public String licenseManagerId;

    @Column(name = "senior_license_officer_id", length = 36)
    public String seniorLicenseOfficerId;

    @Column(name = "drs_id", length = 36)
    public String drsId;

    @Column(name = "accountant_id", length = 36)
    public String accountantId;


//    @Column(name = "approval_date")
//    @Temporal(TemporalType.TIMESTAMP)
//    public Date approvalDate;

//    @Column(name = "completion_date")
//    @Temporal(TemporalType.TIMESTAMP)
//    public Date completionDate; // When assessment is finalized

    @Column(name = "date_created")
    private Timestamp dateCreated;

    @Column(name = "date_updated")
    private Timestamp dateUpdated;
    
    @PrePersist
    protected void onCreate() {
        dateCreated = new Timestamp(System.currentTimeMillis());
        dateUpdated = new Timestamp(System.currentTimeMillis());
    }
    
    @PreUpdate
    protected void onUpdate() {
        dateUpdated = new Timestamp(System.currentTimeMillis());
    }

    public CoreLicenseAssessment() {
    }

    @Override
    public Timestamp getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public Timestamp getDateUpdated() {
        return dateUpdated;
    }

    @Override
    public void setDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    // Getters and setters for public fields (optional, but good practice)
    public String getLicenseApplicationId() {
        return licenseApplicationId;
    }

    public void setLicenseApplicationId(String licenseApplicationId) {
        this.licenseApplicationId = licenseApplicationId;
    }

    public String getAssessmentStatus() {
        return assessmentStatus;
    }

    public void setAssessmentStatus(String assessmentStatus) {
        this.assessmentStatus = assessmentStatus;
    }

    public String getAssessmentFilesUpload() {
        return assessmentFilesUpload;
    }

    public void setAssessmentFilesUpload(String assessmentFilesUpload) {
        this.assessmentFilesUpload = assessmentFilesUpload;
    }

    public BigDecimal getCalculatedAnnualRental() {
        return calculatedAnnualRental;
    }

    public void setCalculatedAnnualRental(BigDecimal calculatedAnnualRental) {
        this.calculatedAnnualRental = calculatedAnnualRental;
    }

    public BigDecimal getRentalQuantity() {
        return rentalQuantity;
    }

    public void setRentalQuantity(BigDecimal rentalQuantity) {
        this.rentalQuantity = rentalQuantity;
    }

    public BigDecimal getRentalRate() {
        return rentalRate;
    }

    public void setRentalRate(BigDecimal rentalRate) {
        this.rentalRate = rentalRate;
    }

    public Date getRecommendedScheduleDate() {
        return recommendedScheduleDate;
    }

    public void setRecommendedScheduleDate(Date recommendedScheduleDate) {
        this.recommendedScheduleDate = recommendedScheduleDate;
    }

    public String getAssessmentNotes() {
        return assessmentNotes;
    }

    public void setAssessmentNotes(String assessmentNotes) {
        this.assessmentNotes = assessmentNotes;
    }

    public String getLicenseOfficerId() {
        return licenseOfficerId;
    }

    public void setLicenseOfficerId(String licenseOfficerId) {
        this.licenseOfficerId = licenseOfficerId;
    }

    public String getLicenseManagerId() {
        return licenseManagerId;
    }

    public void setLicenseManagerId(String licenseManagerId) {
        this.licenseManagerId = licenseManagerId;
    }

    public String getSeniorLicenseOfficerId() {
        return seniorLicenseOfficerId;
    }

    public void setSeniorLicenseOfficerId(String seniorLicenseOfficerId) {
        this.seniorLicenseOfficerId = seniorLicenseOfficerId;
    }

    public String getDrsId() {
        return drsId;
    }

    public void setDrsId(String drsId) {
        this.drsId = drsId;
    }

    public String getAccountantId() {
        return accountantId;
    }

    public void setAccountantId(String accountantId) {
        this.accountantId = accountantId;
    }

//    public String getApprovalManagerId() {
//        return approvalManagerId;
//    }
//
//    public void setApprovalManagerId(String approvalManagerId) {
//        this.approvalManagerId = approvalManagerId;
//    }
//
//    public Date getApprovalDate() {
//        return approvalDate;
//    }
//
//    public void setApprovalDate(Date approvalDate) {
//        this.approvalDate = approvalDate;
//    }
//
//    public Date getCompletionDate() {
//        return completionDate;
//    }
//
//    public void setCompletionDate(Date completionDate) {
//        this.completionDate = completionDate;
//    }
}