package mw.nwra.ewaterpermit.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.service.MailingService;

@RestController
@RequestMapping(value = "/v1/applications")
public class InvoiceController {

    @Autowired
    private MailingService mailingService;

    @GetMapping(path = "/{applicationId}/invoice/download")
    public ResponseEntity<Map<String, Object>> downloadInvoice(@PathVariable String applicationId) {
        
        try {
            // Mock invoice data for GET request
            Map<String, Object> invoiceData = Map.of(
                "invoiceId", "INV-" + applicationId,
                "applicationId", applicationId,
                "applicantName", "Sample Applicant",
                "applicantEmail", "sample@example.com",
                "licenseType", "Surface Water Permit",
                "feeAmount", 50000.0,
                "dueDate", "2025-08-09",
                "generatedDate", new Date().toString(),
                "status", "PENDING"
            );

            return ResponseEntity.ok(invoiceData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to download invoice: " + e.getMessage()));
        }
    }

    @GetMapping(path = "/{applicationId}/payment-status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String applicationId) {
        
        try {
            // Mock payment status data
            Map<String, Object> paymentStatus = Map.of(
                "applicationId", applicationId,
                "paymentStatus", "PENDING",
                "invoiceId", "INV-" + applicationId,
                "amountDue", 50000.0,
                "dueDate", "2025-08-09",
                "paymentMethod", "Not specified",
                "lastUpdated", new Date().toString()
            );

            return ResponseEntity.ok(paymentStatus);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get payment status: " + e.getMessage()));
        }
    }

    @PostMapping(path = "/{applicationId}/process-payment")
    public ResponseEntity<Map<String, Object>> processPayment(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> paymentRequest) {
        
        try {
            // Extract payment data from request
            String paymentMethod = (String) paymentRequest.get("paymentMethod");
            // Handle both Integer and Double for amount
            Number amountNumber = (Number) paymentRequest.get("amount");
            Double amount = amountNumber != null ? amountNumber.doubleValue() : null;
            String phoneNumber = (String) paymentRequest.get("phoneNumber");
            
            // Mock payment processing
            String transactionId = "TXN-" + System.currentTimeMillis();
            
            // Simulate payment processing result
            Map<String, Object> paymentResult = Map.of(
                "success", true,
                "transactionId", transactionId,
                "applicationId", applicationId,
                "amount", amount != null ? amount : 50000.0,
                "paymentMethod", paymentMethod != null ? paymentMethod : "Mobile Money",
                "phoneNumber", phoneNumber != null ? phoneNumber : "N/A",
                "status", "COMPLETED",
                "processedDate", new Date().toString(),
                "message", "Payment processed successfully"
            );

            return ResponseEntity.ok(paymentResult);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "error", "Payment processing failed: " + e.getMessage()
                ));
        }
    }

    @PostMapping(path = "/{applicationId}/invoice/download") 
    public ResponseEntity<Map<String, Object>> generateInvoice(
            @PathVariable String applicationId, 
            @RequestBody Map<String, Object> invoiceRequest) {
        
        try {
            // Extract invoice data from request
            String applicantName = (String) invoiceRequest.get("applicantName");
            String applicantEmail = (String) invoiceRequest.get("applicantEmail");
            String licenseType = (String) invoiceRequest.get("licenseType");
            // Handle both Integer and Double for feeAmount
            Number feeAmountNumber = (Number) invoiceRequest.get("feeAmount");
            Double feeAmount = feeAmountNumber != null ? feeAmountNumber.doubleValue() : null;
            String dueDate = (String) invoiceRequest.get("dueDate");

            // Generate invoice data
            Map<String, Object> invoiceData = Map.of(
                "invoiceId", "INV-" + System.currentTimeMillis(),
                "applicationId", applicationId,
                "applicantName", applicantName,
                "applicantEmail", applicantEmail,
                "licenseType", licenseType,
                "feeAmount", feeAmount,
                "dueDate", dueDate,
                "generatedDate", new Date().toString(),
                "status", "PENDING"
            );

            // Generate simple PDF invoice (mock implementation)
            byte[] invoicePdf = generateSimpleInvoicePDF(invoiceData);

            // Send email with invoice
            String subject = "Invoice for " + licenseType + " Application";
            String emailBody = buildInvoiceEmailBody(invoiceData);
            
            mailingService.sendEmailWithAttachment(
                applicantEmail,
                subject,
                emailBody,
                invoicePdf,
                "invoice_" + invoiceData.get("invoiceId") + ".pdf"
            );

            return ResponseEntity.ok(invoiceData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate invoice: " + e.getMessage()));
        }
    }

    private String buildInvoiceEmailBody(Map<String, Object> invoiceData) {
        return String.format("""
            Dear %s,
            
            Thank you for submitting your %s application.
            
            Application Details:
            - Application ID: %s
            - Invoice ID: %s
            - Amount Due: MWK %.2f
            - Due Date: %s
            
            Please find the invoice attached to this email. You can make payment using the following methods:
            
            1. Online Payment: Log into your account and navigate to "My Applications"
            2. Bank Transfer: Use the reference number from the invoice
            3. Mobile Money: Use the payment code provided in the invoice
            
            If you have any questions, please contact our support team.
            
            Best regards,
            NWRA E-Water Permit System
            """,
            invoiceData.get("applicantName"),
            invoiceData.get("licenseType"),
            invoiceData.get("applicationId"),
            invoiceData.get("invoiceId"),
            invoiceData.get("feeAmount"),
            invoiceData.get("dueDate")
        );
    }

    private byte[] generateSimpleInvoicePDF(Map<String, Object> invoiceData) {
        // Create a simple text-based PDF content
        String pdfContent = String.format("""
            INVOICE
            
            Invoice ID: %s
            Application ID: %s
            Date: %s
            
            Bill To:
            %s
            
            Description: %s
            Amount: MWK %.2f
            Due Date: %s
            
            Payment Status: %s
            
            Please make payment using the reference: %s
            
            Thank you for using NWRA E-Water Permit System.
            """,
            invoiceData.get("invoiceId"),
            invoiceData.get("applicationId"),
            invoiceData.get("generatedDate"),
            invoiceData.get("applicantName"),
            invoiceData.get("licenseType"),
            invoiceData.get("feeAmount"),
            invoiceData.get("dueDate"),
            invoiceData.get("status"),
            invoiceData.get("invoiceId")
        );
        
        // Return the content as bytes (in a real implementation, this would be a proper PDF)
        return pdfContent.getBytes();
    }
}