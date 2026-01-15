package mw.nwra.ewaterpermit.dto;

public class PaymentSummaryDTO {
    private String id;
    private String applicationId;
    private String paymentStatus;
    private String feesTypeName;
    private Double amountPaid;
    private String paymentMethod;

    public PaymentSummaryDTO() {}

    public PaymentSummaryDTO(String id, String applicationId, String paymentStatus, 
                            String feesTypeName, Double amountPaid, String paymentMethod) {
        this.id = id;
        this.applicationId = applicationId;
        this.paymentStatus = paymentStatus;
        this.feesTypeName = feesTypeName;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getFeesTypeName() {
        return feesTypeName;
    }

    public void setFeesTypeName(String feesTypeName) {
        this.feesTypeName = feesTypeName;
    }

    public Double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}