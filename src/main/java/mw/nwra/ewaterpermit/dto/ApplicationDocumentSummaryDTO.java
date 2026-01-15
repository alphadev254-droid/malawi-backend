package mw.nwra.ewaterpermit.dto;

import java.util.List;

public class ApplicationDocumentSummaryDTO {
    private String documentId;
    private String documentUrl;
    private String status;
    private String dateCreated;
    private String documentCategory;

    private ApplicantInfoDTO applicant;
    private ApplicationBasicInfoDTO application;
    private LicenseTypeInfoDTO licenseType;
    private ApplicationStatusInfoDTO applicationStatus;
    private ApplicationStepInfoDTO applicationStep;
    private List<PaymentDocumentDTO> paymentDocuments;

    // Nested DTOs
    public static class ApplicantInfoDTO {
        private String id;
        private String firstName;
        private String lastName;
        private String email;

        public ApplicantInfoDTO() {}

        public ApplicantInfoDTO(String id, String firstName, String lastName, String email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ApplicationBasicInfoDTO {
        private String id;
        private String dateSubmitted;
        private String applicationType;
        private String boardMinutes;
        private String boardApprovalDate;
        private String boardMinutesDocument;

        public ApplicationBasicInfoDTO() {}

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDateSubmitted() { return dateSubmitted; }
        public void setDateSubmitted(String dateSubmitted) { this.dateSubmitted = dateSubmitted; }
        public String getApplicationType() { return applicationType; }
        public void setApplicationType(String applicationType) { this.applicationType = applicationType; }
        public String getBoardMinutes() { return boardMinutes; }
        public void setBoardMinutes(String boardMinutes) { this.boardMinutes = boardMinutes; }
        public String getBoardApprovalDate() { return boardApprovalDate; }
        public void setBoardApprovalDate(String boardApprovalDate) { this.boardApprovalDate = boardApprovalDate; }
        public String getBoardMinutesDocument() { return boardMinutesDocument; }
        public void setBoardMinutesDocument(String boardMinutesDocument) { this.boardMinutesDocument = boardMinutesDocument; }
    }

    public static class LicenseTypeInfoDTO {
        private String id;
        private String name;
        private Double applicationFees;
        private Double licenseFees;

        public LicenseTypeInfoDTO() {}

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getApplicationFees() { return applicationFees; }
        public void setApplicationFees(Double applicationFees) { this.applicationFees = applicationFees; }
        public Double getLicenseFees() { return licenseFees; }
        public void setLicenseFees(Double licenseFees) { this.licenseFees = licenseFees; }
    }

    public static class ApplicationStatusInfoDTO {
        private String name;

        public ApplicationStatusInfoDTO() {}

        public ApplicationStatusInfoDTO(String name) {
            this.name = name;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class ApplicationStepInfoDTO {
        private String name;
        private Integer sequenceNumber;

        public ApplicationStepInfoDTO() {}

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getSequenceNumber() { return sequenceNumber; }
        public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    }

    public static class PaymentDocumentDTO {
        private String paymentId;
        private String feeType;
        private Double amount;
        private String paymentStatus;
        private String paymentMethod;
        private String receiptDocumentId;
        private String receiptDocumentUrl;
        private String dateCreated;

        public PaymentDocumentDTO() {}

        // Getters and Setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getFeeType() { return feeType; }
        public void setFeeType(String feeType) { this.feeType = feeType; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getReceiptDocumentId() { return receiptDocumentId; }
        public void setReceiptDocumentId(String receiptDocumentId) { this.receiptDocumentId = receiptDocumentId; }
        public String getReceiptDocumentUrl() { return receiptDocumentUrl; }
        public void setReceiptDocumentUrl(String receiptDocumentUrl) { this.receiptDocumentUrl = receiptDocumentUrl; }
        public String getDateCreated() { return dateCreated; }
        public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }
    }

    // Main DTO Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }
    public String getDocumentCategory() { return documentCategory; }
    public void setDocumentCategory(String documentCategory) { this.documentCategory = documentCategory; }
    public ApplicantInfoDTO getApplicant() { return applicant; }
    public void setApplicant(ApplicantInfoDTO applicant) { this.applicant = applicant; }
    public ApplicationBasicInfoDTO getApplication() { return application; }
    public void setApplication(ApplicationBasicInfoDTO application) { this.application = application; }
    public LicenseTypeInfoDTO getLicenseType() { return licenseType; }
    public void setLicenseType(LicenseTypeInfoDTO licenseType) { this.licenseType = licenseType; }
    public ApplicationStatusInfoDTO getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(ApplicationStatusInfoDTO applicationStatus) { this.applicationStatus = applicationStatus; }
    public ApplicationStepInfoDTO getApplicationStep() { return applicationStep; }
    public void setApplicationStep(ApplicationStepInfoDTO applicationStep) { this.applicationStep = applicationStep; }
    public List<PaymentDocumentDTO> getPaymentDocuments() { return paymentDocuments; }
    public void setPaymentDocuments(List<PaymentDocumentDTO> paymentDocuments) { this.paymentDocuments = paymentDocuments; }
}
