package mw.nwra.ewaterpermit.dto;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CoreLicenseApplicationSummaryDTO {
    private String id;
    private String applicantName;
    private String applicantEmail;
    private String licenseTypeName;
    private String statusName;
    private String applicationType;
    private LocalDateTime dateCreated;
    private LocalDateTime dateSubmitted;
    private List<PaymentSummaryDTO> payments;

    public CoreLicenseApplicationSummaryDTO() {
        this.payments = new ArrayList<>();
    }

    public CoreLicenseApplicationSummaryDTO(String id, String applicantName, String applicantEmail,
                                           String licenseTypeName, String statusName, String applicationType,
                                           Timestamp dateCreated, Timestamp dateSubmitted) {
        this.id = id;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.licenseTypeName = licenseTypeName;
        this.statusName = statusName;
        this.applicationType = applicationType;
        this.dateCreated = dateCreated != null ? dateCreated.toLocalDateTime() : null;
        this.dateSubmitted = dateSubmitted != null ? dateSubmitted.toLocalDateTime() : null;
        this.payments = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getLicenseTypeName() {
        return licenseTypeName;
    }

    public void setLicenseTypeName(String licenseTypeName) {
        this.licenseTypeName = licenseTypeName;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getDateSubmitted() {
        return dateSubmitted;
    }

    public void setDateSubmitted(LocalDateTime dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
    }

    public List<PaymentSummaryDTO> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentSummaryDTO> payments) {
        this.payments = payments;
    }

    public void addPayment(PaymentSummaryDTO payment) {
        this.payments.add(payment);
    }
}