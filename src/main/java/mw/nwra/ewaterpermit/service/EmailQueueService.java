package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.model.SysConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import mw.nwra.ewaterpermit.model.UserNotification;
import mw.nwra.ewaterpermit.service.NotificationService;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailQueueService {

    @Autowired
    private SysConfigService configService;
    
    @Autowired
    private NotificationService notificationService;

    private final ConcurrentHashMap<String, String> emailTasks = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<String> sendEmailAsync(String taskId, String to, String subject, String htmlBody) {
        try {
            emailTasks.put(taskId, "SENDING");

            // Load email configuration from database
            SysConfig config = configService.getSystemConfig();

            // Create mail sender with database configuration
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(config.getSystemEmailSmtp());
            mailSender.setPort(config.getSystemEmailPort());
            mailSender.setUsername(config.getSystemEmailAddress());
            mailSender.setPassword(config.getSystemEmailAuth());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            // Dynamically set SSL or STARTTLS based on port
            if (config.getSystemEmailPort() == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "false");
                props.put("mail.smtp.starttls.required", "false");
                props.put("mail.smtp.ssl.trust", config.getSystemEmailSmtp());
            } else {
                props.put("mail.smtp.ssl.enable", "false");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            }
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");
            props.put("mail.debug", "false");

            System.out.println("=== EMAIL QUEUE SERVICE - DB CONFIG ===");
            System.out.println("SMTP Host: " + config.getSystemEmailSmtp());
            System.out.println("SMTP Port: " + config.getSystemEmailPort());
            System.out.println("Email From: " + config.getSystemEmailAddress());
            System.out.println("========================================");

            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(config.getSystemEmailAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML content

            // Add logo as inline attachment
            try {
                var logoResource = new org.springframework.core.io.ClassPathResource("static/images/logo_docs.png");
                if (logoResource.exists()) {
                    helper.addInline("logo", logoResource);
                }
            } catch (Exception logoEx) {
                // Continue without logo if not found
                System.out.println("Logo not found: " + logoEx.getMessage());
            }

            mailSender.send(mimeMessage);

            // Email tracking notifications removed - they caused DB errors due to null user_id
            // Officer notifications are properly saved in OfficerNotificationServiceImpl with user_id

            emailTasks.put(taskId, "SENT");
            return CompletableFuture.completedFuture("SENT");

        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();

            // Email tracking notifications removed - they caused DB errors due to null user_id

            emailTasks.put(taskId, "FAILED");
            return CompletableFuture.completedFuture("FAILED");
        }
    }

    public String getEmailStatus(String taskId) {
        return emailTasks.getOrDefault(taskId, "NOT_FOUND");
    }

    public void removeEmailTask(String taskId) {
        emailTasks.remove(taskId);
    }

    public String queueInvoiceEmail(String applicationId, String applicantName, String applicantEmail, String licenseType, double applicationFees) {
        String taskId = "email-" + applicationId + "-" + System.currentTimeMillis();

        String subject = "Invoice for " + licenseType + " Application";
        String htmlBody = createInvoiceEmailTemplate(applicantName, applicationId, licenseType, applicationFees);

        sendEmailAsync(taskId, applicantEmail, subject, htmlBody);

        // Application submission notifications removed - caused DB errors due to null user_id
        // User-specific notifications should be created separately with proper user association

        return taskId;
    }

    public String queueReceiptConfirmationEmail(String applicationId, String applicantName, String applicantEmail, String licenseType, Double amount, String paymentMethod) {
        String taskId = "receipt-" + applicationId + "-" + System.currentTimeMillis();

        String subject = "Payment Receipt Confirmation - " + licenseType + " Application";
        String htmlBody = createReceiptConfirmationEmailTemplate(applicantName, applicationId, licenseType, amount, paymentMethod);

        sendEmailAsync(taskId, applicantEmail, subject, htmlBody);

        // Payment notifications removed - caused DB errors due to null user_id
        // User-specific notifications should be created separately with proper user association

        return taskId;
    }

    public String queuePaymentApprovalEmail(String applicationId, String applicantName, String applicantEmail, String licenseType, String feeType) {
        String taskId = "approval-" + applicationId + "-" + System.currentTimeMillis();

        String subject = "Payment Approved - " + licenseType + " Application";
        String htmlBody = createPaymentApprovalEmailTemplate(applicantName, applicationId, licenseType, feeType);

        sendEmailAsync(taskId, applicantEmail, subject, htmlBody);

        // Payment approval notifications removed - caused DB errors due to null user_id
        // User-specific notifications should be created separately with proper user association

        return taskId;
    }

    private String createInvoiceEmailTemplate(String applicantName, String applicationId, String licenseType, double applicationFees) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Application Confirmation</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        margin: 0;
                        padding: 20px;
                        background-color: #f8f9fa;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .header {
                        background-color: #0ea5e9;
                        color: white;
                        padding: 15px 30px;
                        text-align: center;
                    }
                    .header .logo {
                        width: 100px;
                        height: 100px;
                        margin-bottom: 10px;
                        background-color: white;
                        padding: 8px;
                        border-radius: 6px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    .header p {
                        margin: 8px 0 0 0;
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .greeting {
                        font-size: 16px;
                        margin-bottom: 24px;
                    }
                    .status {
                        background-color: #dbeafe;
                        color: #1e40af;
                        padding: 12px 20px;
                        border-radius: 6px;
                        text-align: center;
                        font-weight: 600;
                        margin: 24px 0;
                    }
                    .details {
                        background-color: #f8fafc;
                        border: 1px solid #e2e8f0;
                        border-radius: 6px;
                        padding: 24px;
                        margin: 24px 0;
                    }
                    .details h3 {
                        margin: 0 0 20px 0;
                        font-size: 18px;
                        color: #1e293b;
                    }
                    .detail-table {
                        width: 100%%;
                        border-collapse: collapse;
                    }
                    .detail-row {
                        border-bottom: 1px solid #e2e8f0;
                    }
                    .detail-row:last-child {
                        border-bottom: none;
                        font-weight: 600;
                        color: #2563eb;
                    }
                    .detail-row td {
                        padding: 12px 8px;
                        vertical-align: top;
                    }
                    .detail-label {
                        color: #64748b;
                        width: 45%%;
                    }
                    .detail-value {
                        color: #1e293b;
                        font-weight: 500;
                        text-align: right;
                        width: 55%%;
                    }
                    .payment-notice {
                        background-color: #fef3c7;
                        border: 1px solid #f59e0b;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 24px 0;
                        text-align: center;
                    }
                    .payment-notice p {
                        margin: 0;
                        color: #92400e;
                        font-weight: 500;
                    }
                    .footer {
                        background-color: #f1f5f9;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e2e8f0;
                    }
                    .footer p {
                        margin: 0;
                        color: #64748b;
                        font-size: 14px;
                    }
                    .footer .org-name {
                        font-weight: 600;
                        color: #1e293b;
                        margin-bottom: 4px;
                    }
                    .disclaimer {
                        font-size: 12px;
                        color: #94a3b8;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <img src="cid:logo" alt="NWRA Logo" style="width: 100px; height: 100px; margin-bottom: 10px; background-color: white; padding: 8px; border-radius: 6px; display: block; margin-left: auto; margin-right: auto;">
                        <p>National Water Resources Authority</p>
                        <h1>Application Received</h1>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">
                            Dear <strong>%s</strong>,
                        </div>
                        
                        <p>Thank you for submitting your water permit application. Please complete your payment of the application through the system to proceed with application processing.</p>
                        
                        <div class="status">
                            Application Status: SUBMITTED
                        </div>
                        
                        <div class="details">
                            <h3>Application Summary</h3>
                            <table class="detail-table">
                                <tr class="detail-row">
                                    <td class="detail-label">Application ID</td>
                                    <td class="detail-value">%s</td>
                                </tr>
                                <tr class="detail-row">
                                    <td class="detail-label">License Type</td>
                                    <td class="detail-value">%s</td>
                                </tr>
                                <tr class="detail-row">
                                    <td class="detail-label">Applicant Name</td>
                                    <td class="detail-value">%s</td>
                                </tr>
                                <tr class="detail-row">
                                    <td class="detail-label">Application Fee</td>
                                    <td class="detail-value">MWK %.2f</td>
                                </tr>
                            </table>
                        </div>
                        
                        <div class="payment-notice">
                            <p>Please complete your payment to proceed with application processing.</p>
                        </div>
                        
                        <p>Our team will review your application and contact you if any additional information is required. You will receive email notifications as your application progresses through each stage of the review process.</p>
                        
                        <p>For any inquiries regarding your application, please reference your Application ID when contacting our support team.</p>
                    </div>
                    
                    <div class="footer">
                        <p class="org-name">National Water Resources Authority</p>
                        <p>License Application System</p>
                        <p class="disclaimer">
                            This is an automated message. Please do not reply to this email.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
                applicantName, applicationId, licenseType, applicantName, applicationFees);
    }

    private String createReceiptConfirmationEmailTemplate(String applicantName, String applicationId, String licenseType, Double amount, String paymentMethod) {
        String amountText = amount != null ? String.format("MWK %.2f", amount) : "Amount not specified";

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Official Payment Receipt</title>
                <style>
                    body {
                        font-family: 'Times New Roman', 'Georgia', serif;
                        line-height: 1.4;
                        margin: 0;
                        padding: 20px;
                        background-color: #f5f5f5;
                        color: #2c2c2c;
                    }
                    .container {
                        max-width: 650px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border: 2px solid #d0d0d0;
                        position: relative;
                        overflow: hidden;
                    }
                    .watermark {
                        position: absolute;
                        right: -50px;
                        top: 50px;
                        opacity: 0.03;
                        font-size: 120px;
                        color: #4a90e2;
                        transform: rotate(-15deg);
                        pointer-events: none;
                        z-index: 1;
                    }
                    .receipt-header {
                        background: linear-gradient(135deg, #e8e8e8 0%%, #f0f0f0 100%%);
                        border-bottom: 3px solid #b8b8b8;
                        padding: 25px;
                        text-align: center;
                        position: relative;
                        z-index: 2;
                    }
                    .receipt-title {
                        margin: 0;
                        font-size: 28px;
                        font-weight: bold;
                        color: #2c2c2c;
                        letter-spacing: 1px;
                        text-transform: uppercase;
                    }
                    .authority-name {
                        margin: 8px 0 0 0;
                        font-size: 16px;
                        color: #5a5a5a;
                        font-weight: normal;
                    }
                    .receipt-number {
                        background-color: #d8d8d8;
                        color: #2c2c2c;
                        padding: 8px 16px;
                        margin: 15px auto 0;
                        display: inline-block;
                        font-family: 'Courier New', monospace;
                        font-weight: bold;
                        border: 1px solid #a8a8a8;
                    }
                    .receipt-body {
                        padding: 30px;
                        position: relative;
                        z-index: 2;
                    }
                    .receipt-section {
                        margin-bottom: 25px;
                    }
                    .section-title {
                        font-size: 14px;
                        font-weight: bold;
                        color: #2c2c2c;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 15px;
                        border-bottom: 1px solid #d0d0d0;
                        padding-bottom: 5px;
                    }
                    .detail-table {
                        width: 100%%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                    }
                    .detail-row {
                        border-bottom: 1px dotted #c0c0c0;
                    }
                    .detail-row td {
                        padding: 12px 8px;
                        vertical-align: top;
                    }
                    .detail-label {
                        color: #5a5a5a;
                        font-weight: normal;
                        width: 40%%;
                    }
                    .detail-value {
                        color: #2c2c2c;
                        font-weight: bold;
                        text-align: right;
                    }
                    .amount-row {
                        border-bottom: 2px solid #888;
                        font-size: 16px;
                    }
                    .amount-row .detail-value {
                        font-size: 18px;
                        color: #2c2c2c;
                    }
                    .status-box {
                        background-color: #ececec;
                        border: 1px solid #b8b8b8;
                        padding: 15px;
                        text-align: center;
                        margin: 20px 0;
                        font-weight: bold;
                        color: #2c2c2c;
                    }
                    .notice-box {
                        background-color: #f8f8f8;
                        border: 1px solid #d0d0d0;
                        border-left: 4px solid #888;
                        padding: 15px;
                        margin: 20px 0;
                        font-size: 13px;
                        color: #4a4a4a;
                    }
                    .important-notice {
                        background-color: #f5f5f5;
                        border: 2px solid #c0c0c0;
                        padding: 15px;
                        margin: 20px 0;
                        font-size: 12px;
                        color: #2c2c2c;
                    }
                    .receipt-footer {
                        background-color: #ececec;
                        border-top: 2px solid #b8b8b8;
                        padding: 20px;
                        text-align: center;
                        font-size: 12px;
                        color: #5a5a5a;
                    }
                    .authority-seal {
                        margin-top: 15px;
                        font-size: 11px;
                        color: #888;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="watermark">💧</div>

                    <div class="receipt-header">
                        <img src="cid:logo" alt="NWRA Logo" style="width: 100px; height: 100px; margin-bottom: 10px; background-color: white; padding: 8px; border-radius: 6px;">
                        <p class="authority-name">National Water Resources Authority</p>
                        <h1 class="receipt-title">Official Receipt</h1>
                        <div class="receipt-number">Receipt No: RCP-%s</div>
                    </div>

                    <div class="receipt-body">
                        <div class="receipt-section">
                            <div class="section-title">Recipient Information </div>
                            <table class="detail-table">
                                <tr class="detail-row">
                                    <td class="detail-label">Received From:</td>
                                    <td class="detail-value">%s</td>
                                </tr>
                            </table>
                        </div>

                        <div class="receipt-section">
                            <div class="section-title">Payment Details </div>
                            <table class="detail-table">
                                <tr class="detail-row">
                                    <td class="detail-label">Application ID: </td>
                                    <td class="detail-value">%s</td>
                                </tr>
                                <tr class="detail-row">
                                    <td class="detail-label">License Type: </td>
                                    <td class="detail-value">%s</td>
                                </tr>
                                <tr class="detail-row">
                                    <td class="detail-label">Payment Method: </td>
                                    <td class="detail-value">%s</td>
                                </tr>
                                <tr class="detail-row amount-row">
                                    <td class="detail-label">Expected Application Fee : </td>
                                    <td class="detail-value">%s</td>
                                </tr>
                            </table>
                        </div>

                        <div class="status-box">
                            STATUS: PAYMENT RECEIPT RECEIVED - UNDER VERIFICATION
                        </div>

                        <div class="notice-box">
                            <strong>Processing Information:</strong><br>
                            Your payment receipt has been received and is currently under verification by our team.
                            You will receive email notification once verification is complete.
                        </div>

                        <div class="important-notice">
                            <strong>IMPORTANT NOTICE:</strong> This receipt confirms that payment documentation has been received.
                            Payment of fees does not guarantee approval of your application. All applications are subject to
                            technical review and must meet regulatory requirements and standards set by the National Water
                            Resources Authority before approval can be granted.
                        </div>

                        <div class="receipt-section">
                            <div class="section-title">Next Steps</div>
                            <ol style="color: #2c2c2c; line-height: 1.6;">
                                <li>Payment verification by accounts team</li>
                                <li>Email confirmation upon verification completion</li>
                                <li>Application proceeds to technical review</li>
                                <li>Notification of any additional requirements</li>
                            </ol>
                        </div>
                    </div>

                    <div class="receipt-footer">
                        <p style="margin: 0; font-weight: bold;">NATIONAL WATER RESOURCES AUTHORITY</p>
                        <p style="margin: 5px 0;">Republic of Malawi</p>
                        <div class="authority-seal">
                            Official Document - License Application System<br>
                            This is an automated receipt. Please retain for your records.
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                applicationId.substring(0, Math.min(8, applicationId.length())),
                applicantName, applicationId, licenseType, paymentMethod, amountText);
    }

    private String createPaymentApprovalEmailTemplate(String applicantName, String applicationId, String licenseType, String feeType) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Payment Approved</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        margin: 0;
                        padding: 20px;
                        background-color: #f8f9fa;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .header {
                        background-color: #0ea5e9;
                        color: white;
                        padding: 15px 30px;
                        text-align: center;
                    }
                    .header .logo {
                        width: 80px;
                        height: 80px;
                        margin-bottom: 10px;
                        background-color: white;
                        padding: 8px;
                        border-radius: 6px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    .header p {
                        margin: 8px 0 0 0;
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .greeting {
                        font-size: 16px;
                        margin-bottom: 24px;
                    }
                    .status {
                        background-color: #d1fae5;
                        color: #065f46;
                        padding: 12px 20px;
                        border-radius: 6px;
                        text-align: center;
                        font-weight: 600;
                        margin: 24px 0;
                    }
                    .details {
                        background-color: #f8fafc;
                        border: 1px solid #e2e8f0;
                        border-radius: 6px;
                        padding: 24px;
                        margin: 24px 0;
                    }
                    .details h3 {
                        margin: 0 0 20px 0;
                        font-size: 18px;
                        color: #1e293b;
                    }
                    .detail-table {
                        width: 100%%;
                        border-collapse: collapse;
                    }
                    .detail-row {
                        border-bottom: 1px solid #e2e8f0;
                    }
                    .detail-row:last-child {
                        border-bottom: none;
                    }
                    .detail-row td {
                        padding: 12px 8px;
                        vertical-align: top;
                    }
                    .detail-label {
                        color: #64748b;
                        width: 45%%;
                    }
                    .detail-value {
                        color: #1e293b;
                        font-weight: 500;
                        text-align: right;
                        width: 55%%;
                    }
                    .success-notice {
                        background-color: #d1fae5;
                        border: 1px solid #10b981;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 24px 0;
                    }
                    .success-notice p {
                        margin: 0;
                        color: #065f46;
                    }
                    .footer {
                        background-color: #f1f5f9;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e2e8f0;
                    }
                    .footer p {
                        margin: 0;
                        color: #64748b;
                        font-size: 14px;
                    }
                    .footer .org-name {
                        font-weight: 600;
                        color: #1e293b;
                        margin-bottom: 4px;
                    }
                    .disclaimer {
                        font-size: 12px;
                        color: #94a3b8;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <img src="cid:logo" alt="NWRA Logo" style="width: 80px; height: 80px; margin-bottom: 10px; background-color: white; padding: 8px; border-radius: 6px; display: block; margin-left: auto; margin-right: auto;">
                        <p>National Water Resources Authority</p>
                        <h1>✓ Payment Approved</h1>
                    </div>

                    <div class="content">
                        <div class="greeting">
                            Dear <strong>%s</strong>,
                        </div>

                        <p>We are pleased to inform you that your payment has been verified and approved by our accounts department.</p>

                        <div class="status">
                            Payment Status: APPROVED
                        </div>

                        <div class="details">
                            <h3>Application Summary</h3>
                            <table class="detail-table">
                                <tr class="detail-row">
                                    <td class="detail-label">Application ID</td>
                                    <td class="detail-value">%s</td>
                                </tr>
                                <tr class="detail-row">
                                    <td class="detail-label">License Type</td>
                                    <td class="detail-value">%s</td>
                                </tr>
                                <tr class="detail-row">
                                    <td class="detail-label">Fee Type</td>
                                    <td class="detail-value">%s</td>
                                </tr>
                            </table>
                        </div>

                        <div class="success-notice">
                            <p><strong>Next Steps:</strong></p>
                            <p>Your application will now proceed to the next stage of processing. Our technical team will review your application and you will receive notifications as your application progresses through each stage.</p>
                        </div>

                        <p>You can track your application status by logging into the system with your application ID.</p>

                        <p>Thank you for your patience during the verification process. If you have any questions, please contact our support team.</p>
                    </div>

                    <div class="footer">
                        <p class="org-name">National Water Resources Authority</p>
                        <p>License Application System</p>
                        <p class="disclaimer">
                            This is an automated message. Please do not reply to this email.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
                applicantName, applicationId, licenseType, feeType);
    }

    /**
     * Save email notification to database
     */
    private void saveEmailNotification(String to, String subject, String htmlBody, String status) {
        try {
            UserNotification notification = new UserNotification();
            
            notification.setTitle("Email: " + subject);
            notification.setMessage(extractTextFromHtml(htmlBody));
            notification.setType("SENT".equals(status) ? UserNotification.NotificationType.INFO : UserNotification.NotificationType.ERROR);
            notification.setCategory(UserNotification.NotificationCategory.SYSTEM);
            notification.setPriority(UserNotification.NotificationPriority.MEDIUM);
            
            notificationService.createNotification(notification);
            System.out.println("Email notification saved for: " + to);
            
        } catch (Exception e) {
            System.err.println("Failed to save email notification: " + e.getMessage());
        }
    }

    /**
     * Save application submission notification
     */
    private void saveApplicationSubmissionNotification(String applicationId, String applicantName, String applicantEmail, String licenseType) {
        try {
            UserNotification notification = new UserNotification();
            notification.setTitle("Application Submitted - " + licenseType);
            notification.setMessage(String.format("Your %s application (ID: %s) has been submitted successfully. You will receive updates as it progresses through the review process.", 
                    licenseType, applicationId));
            notification.setType(UserNotification.NotificationType.SUCCESS);
            notification.setCategory(UserNotification.NotificationCategory.LICENSE);
            notification.setPriority(UserNotification.NotificationPriority.HIGH);
            notification.setActionUrl("/applications/" + applicationId);
            notification.setActionLabel("View Application");
            notification.setReferenceId(applicationId);
            
            notificationService.createNotification(notification);
            System.out.println("Application submission notification saved for: " + applicantEmail);
            
        } catch (Exception e) {
            System.err.println("Failed to save application submission notification: " + e.getMessage());
        }
    }

    /**
     * Save payment notification
     */
    private void savePaymentNotification(String applicationId, String applicantName, String applicantEmail, Double amount, String paymentMethod) {
        try {
            UserNotification notification = new UserNotification();
            notification.setTitle("Payment Received - MWK " + String.format("%.2f", amount));
            notification.setMessage(String.format("Your payment of MWK %.2f via %s has been received for application %s. Payment is being processed.", 
                    amount, paymentMethod, applicationId));
            notification.setType(UserNotification.NotificationType.SUCCESS);
            notification.setCategory(UserNotification.NotificationCategory.PAYMENT);
            notification.setPriority(UserNotification.NotificationPriority.HIGH);
            notification.setActionUrl("/applications/" + applicationId);
            notification.setActionLabel("View Application");
            notification.setReferenceId(applicationId);
            
            notificationService.createNotification(notification);
            System.out.println("Payment notification saved for: " + applicantEmail);
            
        } catch (Exception e) {
            System.err.println("Failed to save payment notification: " + e.getMessage());
        }
    }

    /**
     * Save payment approval notification
     */
    private void savePaymentApprovalNotification(String applicationId, String applicantName, String applicantEmail, String licenseType, String feeType) {
        try {
            UserNotification notification = new UserNotification();
            notification.setTitle("Payment Approved - " + feeType);
            notification.setMessage(String.format("Your %s payment for %s application (ID: %s) has been approved. Your application will now proceed to the next stage.", 
                    feeType, licenseType, applicationId));
            notification.setType(UserNotification.NotificationType.SUCCESS);
            notification.setCategory(UserNotification.NotificationCategory.PAYMENT);
            notification.setPriority(UserNotification.NotificationPriority.HIGH);
            notification.setActionUrl("/applications/" + applicationId);
            notification.setActionLabel("View Application");
            notification.setReferenceId(applicationId);
            
            notificationService.createNotification(notification);
            System.out.println("Payment approval notification saved for: " + applicantEmail);
            
        } catch (Exception e) {
            System.err.println("Failed to save payment approval notification: " + e.getMessage());
        }
    }

    /**
     * Extract plain text from HTML content for notification message
     */
    private String extractTextFromHtml(String htmlContent) {
        if (htmlContent == null) return "";
        
        // Simple HTML tag removal - replace with proper HTML parser if needed
        String text = htmlContent.replaceAll("<[^>]+>", "");
        text = text.replaceAll("\\s+", " ").trim();
        
        // Limit length for notification
        if (text.length() > 200) {
            text = text.substring(0, 197) + "...";
        }
        
        return text;
    }
}