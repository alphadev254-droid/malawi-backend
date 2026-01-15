package mw.nwra.ewaterpermit.dto;

public class PaymentStatusDTO {
    private Double amount;
    private String status;
    private String message;

    public PaymentStatusDTO() {}

    public PaymentStatusDTO(Double amount, String status, String message) {
        this.amount = amount;
        this.status = status;
        this.message = message;
    }

    // Getters and Setters
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}