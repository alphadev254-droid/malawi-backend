package mw.nwra.ewaterpermit.dto;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CoreLicenseApplicationOptimizedDTO {
    private String id;
    private String applicantName;
    private String applicantEmail;
    private String licenseType;
    private String licenseTypeId;
    private String status;
    private String applicationType;
    private LocalDateTime applicationDate;
    private LocalDateTime dateSubmitted;
    private Double applicationFees;
    private Double licenseFees;
    private String clientInfo;
    private String locationInfo;
    private String applicationMetadata;
    private String formSpecificData;
    private PaymentStatusDTO paymentStatus;
    private PaymentStatusDTO licenseFeeStatus;
    
    // Step information
    private String stepId;
    private String stepName;
    private Integer stepSequence;
    
    // Parsed metadata fields for frontend convenience
    private JsonNode metadata;
    private JsonNode clientInfoJson;
    private JsonNode locationInfoJson;
    private JsonNode formSpecificDataJson;

    public CoreLicenseApplicationOptimizedDTO() {
        this.paymentStatus = new PaymentStatusDTO();
        this.licenseFeeStatus = new PaymentStatusDTO();
    }

    public CoreLicenseApplicationOptimizedDTO(Object[] data) {
        this();
        
        if (data.length >= 22) {
            this.id = (String) data[0];
            this.applicantName = (String) data[1];
            this.applicantEmail = (String) data[2];
            this.licenseType = (String) data[3];
            this.licenseTypeId = (String) data[4];
            this.status = (String) data[5];
            this.applicationType = (String) data[6];
            this.applicationDate = data[7] instanceof Timestamp ? ((Timestamp) data[7]).toLocalDateTime() : null;
            this.dateSubmitted = data[8] instanceof Timestamp ? ((Timestamp) data[8]).toLocalDateTime() : null;
            
            // Handle BigDecimal to Double conversion for MySQL
            this.applicationFees = data[9] instanceof java.math.BigDecimal ? ((java.math.BigDecimal) data[9]).doubleValue() : (Double) data[9];
            this.licenseFees = data[10] instanceof java.math.BigDecimal ? ((java.math.BigDecimal) data[10]).doubleValue() : (Double) data[10];
            
            this.clientInfo = (String) data[11];
            this.locationInfo = (String) data[12];
            this.applicationMetadata = (String) data[13];
            this.formSpecificData = (String) data[14];
            
            // Payment status for application fees - handle BigDecimal
            Object appPaymentAmountObj = data[15];
            Double appPaymentAmount = appPaymentAmountObj instanceof java.math.BigDecimal ? 
                ((java.math.BigDecimal) appPaymentAmountObj).doubleValue() : (Double) appPaymentAmountObj;
            String appPaymentStatus = (String) data[16];
            this.paymentStatus.setAmount(appPaymentAmount != null ? appPaymentAmount : 0.0);
            this.paymentStatus.setStatus(appPaymentStatus != null ? appPaymentStatus : "PENDING");
            this.paymentStatus.setMessage("Application fee status: " + this.paymentStatus.getStatus());
            
            // Payment status for license fees - handle BigDecimal
            Object licensePaymentAmountObj = data[17];
            Double licensePaymentAmount = licensePaymentAmountObj instanceof java.math.BigDecimal ? 
                ((java.math.BigDecimal) licensePaymentAmountObj).doubleValue() : (Double) licensePaymentAmountObj;
            String licensePaymentStatus = (String) data[18];
            this.licenseFeeStatus.setAmount(licensePaymentAmount != null ? licensePaymentAmount : 0.0);
            this.licenseFeeStatus.setStatus(licensePaymentStatus != null ? licensePaymentStatus : "PENDING");
            this.licenseFeeStatus.setMessage("License fee status: " + this.licenseFeeStatus.getStatus());
            
            // Step information
            this.stepId = (String) data[19];
            this.stepName = (String) data[20];
            this.stepSequence = data[21] instanceof Number ? ((Number) data[21]).intValue() : null;
        }
        
        // Pre-parse JSON fields
        parseJsonFields();
    }

    private void parseJsonFields() {
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            if (this.applicationMetadata != null && !this.applicationMetadata.isEmpty()) {
                this.metadata = mapper.readTree(this.applicationMetadata);
            }
        } catch (Exception e) {
            // Log but don't fail - frontend can still use raw string
        }
        
        try {
            if (this.clientInfo != null && !this.clientInfo.isEmpty()) {
                this.clientInfoJson = mapper.readTree(this.clientInfo);
            }
        } catch (Exception e) {
            // Log but don't fail
        }
        
        try {
            if (this.locationInfo != null && !this.locationInfo.isEmpty()) {
                this.locationInfoJson = mapper.readTree(this.locationInfo);
            }
        } catch (Exception e) {
            // Log but don't fail
        }
        
        try {
            if (this.formSpecificData != null && !this.formSpecificData.isEmpty()) {
                this.formSpecificDataJson = mapper.readTree(this.formSpecificData);
            }
        } catch (Exception e) {
            // Log but don't fail
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public String getLicenseTypeId() { return licenseTypeId; }
    public void setLicenseTypeId(String licenseTypeId) { this.licenseTypeId = licenseTypeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; }

    public LocalDateTime getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDateTime applicationDate) { this.applicationDate = applicationDate; }

    public LocalDateTime getDateSubmitted() { return dateSubmitted; }
    public void setDateSubmitted(LocalDateTime dateSubmitted) { this.dateSubmitted = dateSubmitted; }

    public Double getApplicationFees() { return applicationFees; }
    public void setApplicationFees(Double applicationFees) { this.applicationFees = applicationFees; }

    public Double getLicenseFees() { return licenseFees; }
    public void setLicenseFees(Double licenseFees) { this.licenseFees = licenseFees; }

    public String getClientInfo() { return clientInfo; }
    public void setClientInfo(String clientInfo) { this.clientInfo = clientInfo; }

    public String getLocationInfo() { return locationInfo; }
    public void setLocationInfo(String locationInfo) { this.locationInfo = locationInfo; }

    public String getApplicationMetadata() { return applicationMetadata; }
    public void setApplicationMetadata(String applicationMetadata) { this.applicationMetadata = applicationMetadata; }

    public String getFormSpecificData() { return formSpecificData; }
    public void setFormSpecificData(String formSpecificData) { this.formSpecificData = formSpecificData; }

    public PaymentStatusDTO getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatusDTO paymentStatus) { this.paymentStatus = paymentStatus; }

    public PaymentStatusDTO getLicenseFeeStatus() { return licenseFeeStatus; }
    public void setLicenseFeeStatus(PaymentStatusDTO licenseFeeStatus) { this.licenseFeeStatus = licenseFeeStatus; }

    public JsonNode getMetadata() { return metadata; }
    public void setMetadata(JsonNode metadata) { this.metadata = metadata; }

    public JsonNode getClientInfoJson() { return clientInfoJson; }
    public void setClientInfoJson(JsonNode clientInfoJson) { this.clientInfoJson = clientInfoJson; }

    public JsonNode getLocationInfoJson() { return locationInfoJson; }
    public void setLocationInfoJson(JsonNode locationInfoJson) { this.locationInfoJson = locationInfoJson; }

    public JsonNode getFormSpecificDataJson() { return formSpecificDataJson; }
    public void setFormSpecificDataJson(JsonNode formSpecificDataJson) { this.formSpecificDataJson = formSpecificDataJson; }

    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public Integer getStepSequence() { return stepSequence; }
    public void setStepSequence(Integer stepSequence) { this.stepSequence = stepSequence; }
}