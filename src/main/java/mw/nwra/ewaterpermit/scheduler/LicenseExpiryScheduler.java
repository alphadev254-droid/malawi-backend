package mw.nwra.ewaterpermit.scheduler;

import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.service.CoreLicenseService;
import mw.nwra.ewaterpermit.service.EmailQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler to automatically expire licenses that have reached their expiry date
 * Runs daily at midnight to check and expire licenses
 */
@Component
public class LicenseExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(LicenseExpiryScheduler.class);

    @Autowired
    private CoreLicenseService coreLicenseService;

    @Autowired
    private EmailQueueService emailQueueService;

    /**
     * Run every 2 minutes for testing purposes
     * Cron format: second, minute, hour, day of month, month, day of week
     */
@Scheduled(cron = "0 0 1 * * ?", zone = "Africa/Blantyre")
    public void processLicenseNotifications() {
        log.info("=== LICENSE NOTIFICATION SCHEDULER STARTED (TESTING MODE - EVERY 2 MINUTES) ===");
        
        // Process all notification stages
        send3MonthNotifications();
        send2MonthNotifications();
        send1MonthNotifications();
        send1WeekNotifications();
        
        // Finally, expire licenses
        expireLicenses();
    }

    /**
     * Send 3-month expiry notifications
     */
    private void send3MonthNotifications() {
        log.info("Checking for licenses expiring in 3 months...");
        try {
            List<CoreLicense> licenses = coreLicenseService.findLicensesExpiringIn3Months();
            
            if (licenses.isEmpty()) {
                log.info("No licenses found expiring in 3 months");
                return;
            }
            
            log.info("Found {} licenses expiring in 3 months", licenses.size());
            
            for (CoreLicense license : licenses) {
                try {
                    sendRenewalReminderEmail(license, "3_MONTHS");
                    license.setNotification3MonthsSent(new java.sql.Date(System.currentTimeMillis()));
                    coreLicenseService.editCoreLicense(license);
                    log.info("3-month notification sent for license: {}", license.getLicenseNumber());
                } catch (Exception e) {
                    log.error("Error sending 3-month notification for license {}: {}", 
                            license.getLicenseNumber(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in send3MonthNotifications: {}", e.getMessage());
        }
    }

    /**
     * Send 2-month expiry notifications
     */
    private void send2MonthNotifications() {
        log.info("Checking for licenses expiring in 2 months...");
        try {
            List<CoreLicense> licenses = coreLicenseService.findLicensesExpiringIn2Months();
            
            if (licenses.isEmpty()) {
                log.info("No licenses found expiring in 2 months");
                return;
            }
            
            log.info("Found {} licenses expiring in 2 months", licenses.size());
            
            for (CoreLicense license : licenses) {
                try {
                    sendRenewalReminderEmail(license, "2_MONTHS");
                    license.setNotification2MonthsSent(new java.sql.Date(System.currentTimeMillis()));
                    coreLicenseService.editCoreLicense(license);
                    log.info("2-month notification sent for license: {}", license.getLicenseNumber());
                } catch (Exception e) {
                    log.error("Error sending 2-month notification for license {}: {}", 
                            license.getLicenseNumber(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in send2MonthNotifications: {}", e.getMessage());
        }
    }

    /**
     * Send 1-month expiry notifications
     */
    private void send1MonthNotifications() {
        log.info("Checking for licenses expiring in 1 month...");
        try {
            List<CoreLicense> licenses = coreLicenseService.findLicensesExpiringIn1Month();
            
            if (licenses.isEmpty()) {
                log.info("No licenses found expiring in 1 month");
                return;
            }
            
            log.info("Found {} licenses expiring in 1 month", licenses.size());
            
            for (CoreLicense license : licenses) {
                try {
                    sendRenewalReminderEmail(license, "1_MONTH");
                    license.setNotification1MonthSent(new java.sql.Date(System.currentTimeMillis()));
                    coreLicenseService.editCoreLicense(license);
                    log.info("1-month notification sent for license: {}", license.getLicenseNumber());
                } catch (Exception e) {
                    log.error("Error sending 1-month notification for license {}: {}", 
                            license.getLicenseNumber(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in send1MonthNotifications: {}", e.getMessage());
        }
    }

    /**
     * Send 1-week expiry notifications
     */
    private void send1WeekNotifications() {
        log.info("Checking for licenses expiring in 1 week...");
        try {
            List<CoreLicense> licenses = coreLicenseService.findLicensesExpiringIn1Week();
            
            if (licenses.isEmpty()) {
                log.info("No licenses found expiring in 1 week");
                return;
            }
            
            log.info("Found {} licenses expiring in 1 week", licenses.size());
            
            for (CoreLicense license : licenses) {
                try {
                    sendRenewalReminderEmail(license, "1_WEEK");
                    license.setNotification1WeekSent(new java.sql.Date(System.currentTimeMillis()));
                    coreLicenseService.editCoreLicense(license);
                    log.info("1-week notification sent for license: {}", license.getLicenseNumber());
                } catch (Exception e) {
                    log.error("Error sending 1-week notification for license {}: {}", 
                            license.getLicenseNumber(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in send1WeekNotifications: {}", e.getMessage());
        }
    }

    /**
     * Expire licenses that have reached their expiry date
     */
    private void expireLicenses() {
        log.info("Checking for licenses that need to be expired...");

        try {
            // Get today's date
            LocalDate today = LocalDate.now();
            Date todayDate = Date.valueOf(today);

            log.info("Current date: {}", today);

            // Find all active licenses that have expired
            List<CoreLicense> expiredLicenses = coreLicenseService.findActiveLicensesExpiredByDate(todayDate);

            if (expiredLicenses == null || expiredLicenses.isEmpty()) {
                log.info("No licenses found that need to be expired today");
                return;
            }

            log.info("Found {} licenses that have expired", expiredLicenses.size());

            int successCount = 0;
            int failureCount = 0;

            // Process each expired license
            for (CoreLicense license : expiredLicenses) {
                try {
                    log.info("Processing expired license: {} (Expiry Date: {})",
                            license.getLicenseNumber(), license.getExpirationDate());

                    // Update license status to EXPIRED
                    String previousStatus = license.getStatus();
                    license.setStatus("EXPIRED");
                    license.setDateUpdated(new java.sql.Timestamp(System.currentTimeMillis()));

                    // Save the updated license
                    coreLicenseService.editCoreLicense(license);

                    log.info("License {} status changed from {} to EXPIRED",
                            license.getLicenseNumber(), previousStatus);

                    // Send expiry notification email to license holder
                    try {
                        sendExpiryNotificationEmail(license);
                    } catch (Exception emailError) {
                        log.warn("Failed to send expiry notification for license {}: {}",
                                license.getLicenseNumber(), emailError.getMessage());
                    }

                    successCount++;

                } catch (Exception e) {
                    failureCount++;
                    log.error("Error expiring license {}: {}",
                            license.getLicenseNumber(), e.getMessage(), e);
                }
            }

            log.info("=== LICENSE EXPIRY SCHEDULER COMPLETED ===");
            log.info("Successfully expired: {} licenses", successCount);
            if (failureCount > 0) {
                log.warn("Failed to expire: {} licenses", failureCount);
            }

        } catch (Exception e) {
            log.error("Error in license expiry scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Send renewal reminder email based on notification stage
     */
    private void sendRenewalReminderEmail(CoreLicense license, String stage) {
        try {
            if (license.getCoreLicenseApplication() == null ||
                license.getCoreLicenseApplication().getSysUserAccount() == null ||
                license.getCoreLicenseApplication().getSysUserAccount().getEmailAddress() == null) {
                log.warn("Cannot send renewal reminder - no email address for license: {}",
                        license.getLicenseNumber());
                return;
            }

            String recipientEmail = license.getCoreLicenseApplication().getSysUserAccount().getEmailAddress();
            String recipientName = getFullName(license);
            String licenseType = license.getCoreLicenseApplication().getCoreLicenseType() != null ?
                    license.getCoreLicenseApplication().getCoreLicenseType().getName() : "Water Permit";

            String timeRemaining = getTimeRemainingText(stage);
            String urgencyLevel = getUrgencyLevel(stage);
            String subject = String.format("License Renewal Reminder - %s Remaining: %s", timeRemaining, license.getLicenseNumber());
            
            String emailBody = String.format("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>License Renewal Reminder</title>
                        <style>
                            body {
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                line-height: 1.4;
                                margin: 0;
                                padding: 10px;
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
                                position: relative;
                            }
                            .watermark {
                                position: absolute;
                                right: -30px;
                                top: 30px;
                                opacity: 0.05;
                                font-size: 80px;
                                color: #4a90e2;
                                transform: rotate(-15deg);
                                pointer-events: none;
                                z-index: 1;
                            }
                            .header {
                                background: linear-gradient(135deg, #4a90e2 0%%, #357abd 100%%);
                                color: white;
                                padding: 15px 30px;
                                text-align: center;
                                position: relative;
                                z-index: 2;
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
                                padding: 20px 30px;
                            }
                            .reminder-box {
                                background-color: %s;
                                border: 1px solid %s;
                                border-radius: 6px;
                                padding: 20px;
                                margin: 20px 0;
                                text-align: center;
                            }
                            .reminder-box h2 {
                                margin: 0 0 10px 0;
                                color: %s;
                                font-size: 20px;
                            }
                            .license-details {
                                background-color: #f8fafc;
                                border: 1px solid #e2e8f0;
                                border-radius: 6px;
                                padding: 16px;
                                margin: 16px 0;
                            }
                            .footer {
                                background-color: #f1f5f9;
                                padding: 30px;
                                text-align: center;
                                border-top: 1px solid #e2e8f0;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <div class="watermark">💧</div>
                                <img src="cid:logo" alt="NWRA Logo" class="logo">
                                <p>National Water Resources Authority</p>
                                <h1>License Renewal Reminder</h1>
                            </div>
                            
                            <div class="content">
                                <p>Dear <strong>%s</strong>,</p>
                                
                                <div class="reminder-box">
                                    <h2>%s</h2>
                                    <p>Your %s license expires in <strong>%s</strong></p>
                                </div>
                                
                                <div class="license-details">
                                    <h3>License Details</h3>
                                    <p><strong>License Number:</strong> %s</p>
                                    <p><strong>License Type:</strong> %s</p>
                                    <p><strong>Expiry Date:</strong> %s</p>
                                </div>
                                
                                <p>To avoid any interruption to your operations, please renew your license before the expiry date.</p>
                                
                                <p><strong>How to Renew:</strong></p>
                                <ol>
                                    <li>Log into the NWRA e-Water Permit System</li>
                                    <li>Navigate to "My Licenses"</li>
                                    <li>Select your license and click "Renew"</li>
                                    <li>Complete the renewal application</li>
                                    <li>Pay the renewal fees</li>
                                </ol>
                            </div>
                            
                            <div class="footer">
                                <p><strong>National Water Resources Authority</strong></p>
                                <p>This is an automated reminder. Please do not reply to this email.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """,
                    getHeaderColor(stage), getReminderBoxColor(stage), getReminderBorderColor(stage), getReminderTextColor(stage),
                    recipientName, urgencyLevel, licenseType, timeRemaining,
                    license.getLicenseNumber(), licenseType, license.getExpirationDate()
            );

            String taskId = "license-renewal-reminder-" + stage.toLowerCase() + "-" + license.getId() + "-" + System.currentTimeMillis();
            emailQueueService.sendEmailAsync(taskId, recipientEmail, subject, emailBody);

            log.info("Renewal reminder email ({}) sent to: {}", stage, recipientEmail);

        } catch (Exception e) {
            log.error("Error sending renewal reminder email: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String getTimeRemainingText(String stage) {
        return switch (stage) {
            case "3_MONTHS" -> "3 Months";
            case "2_MONTHS" -> "2 Months";
            case "1_MONTH" -> "1 Month";
            case "1_WEEK" -> "1 Week";
            default -> "Soon";
        };
    }

    private String getUrgencyLevel(String stage) {
        return switch (stage) {
            case "3_MONTHS" -> "Renewal Reminder";
            case "2_MONTHS" -> "Important Renewal Notice";
            case "1_MONTH" -> "Urgent Renewal Required";
            case "1_WEEK" -> "FINAL RENEWAL NOTICE";
            default -> "Renewal Notice";
        };
    }

    private String getHeaderColor(String stage) {
        return "#4a90e2"; // Blue water theme for all stages
    }

    private String getReminderBoxColor(String stage) {
        return switch (stage) {
            case "3_MONTHS" -> "#d1fae5";
            case "2_MONTHS" -> "#fef3c7";
            case "1_MONTH" -> "#fee2e2";
            case "1_WEEK" -> "#fecaca";
            default -> "#f3f4f6";
        };
    }

    private String getReminderBorderColor(String stage) {
        return switch (stage) {
            case "3_MONTHS" -> "#10b981";
            case "2_MONTHS" -> "#f59e0b";
            case "1_MONTH" -> "#ef4444";
            case "1_WEEK" -> "#dc2626";
            default -> "#6b7280";
        };
    }

    private String getReminderTextColor(String stage) {
        return switch (stage) {
            case "3_MONTHS" -> "#065f46";
            case "2_MONTHS" -> "#92400e";
            case "1_MONTH" -> "#991b1b";
            case "1_WEEK" -> "#991b1b";
            default -> "#374151";
        };
    }

    /**
     * Send email notification to license holder about license expiry
     */
    private void sendExpiryNotificationEmail(CoreLicense license) {
        try {
            if (license.getCoreLicenseApplication() == null ||
                license.getCoreLicenseApplication().getSysUserAccount() == null ||
                license.getCoreLicenseApplication().getSysUserAccount().getEmailAddress() == null) {
                log.warn("Cannot send expiry notification - no email address for license: {}",
                        license.getLicenseNumber());
                return;
            }

            String recipientEmail = license.getCoreLicenseApplication().getSysUserAccount().getEmailAddress();
            String recipientName = getFullName(license);
            String licenseType = license.getCoreLicenseApplication().getCoreLicenseType() != null ?
                    license.getCoreLicenseApplication().getCoreLicenseType().getName() : "Water Permit";

            String subject = "License Expired - Renewal Required: " + license.getLicenseNumber();
            String emailBody = String.format("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>License Expired Notice</title>
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
                                color: #dc2626;
                                transform: rotate(-15deg);
                                pointer-events: none;
                                z-index: 1;
                            }
                            .notice-header {
                                background: linear-gradient(135deg, #4a90e2 0%%, #357abd 100%%);
                                border-bottom: 3px solid #357abd;
                                padding: 25px;
                                text-align: center;
                                position: relative;
                                z-index: 2;
                            }
                            .notice-title {
                                margin: 0;
                                font-size: 28px;
                                font-weight: bold;
                                color: white;
                                letter-spacing: 1px;
                                text-transform: uppercase;
                            }
                            .authority-name {
                                margin: 8px 0 0 0;
                                font-size: 16px;
                                color: #5a5a5a;
                                font-weight: normal;
                            }
                            .license-number {
                                background-color: rgba(255,255,255,0.2);
                                color: white;
                                padding: 8px 16px;
                                margin: 15px auto 0;
                                display: inline-block;
                                font-family: 'Courier New', monospace;
                                font-weight: bold;
                                border: 1px solid rgba(255,255,255,0.3);
                                border-radius: 4px;
                            }
                            .notice-body {
                                padding: 30px;
                                position: relative;
                                z-index: 2;
                            }
                            .notice-section {
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
                            .status-box {
                                background-color: #fee2e2;
                                border: 2px solid #dc2626;
                                padding: 15px;
                                text-align: center;
                                margin: 20px 0;
                                font-weight: bold;
                                color: #991b1b;
                                font-size: 16px;
                            }
                            .warning-box {
                                background-color: #fef3c7;
                                border: 1px solid #f59e0b;
                                border-left: 4px solid #d97706;
                                padding: 15px;
                                margin: 20px 0;
                                font-size: 13px;
                                color: #78350f;
                            }
                            .important-notice {
                                background-color: #f5f5f5;
                                border: 2px solid #c0c0c0;
                                padding: 15px;
                                margin: 20px 0;
                                font-size: 12px;
                                color: #2c2c2c;
                            }
                            .notice-footer {
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

                            <div class="notice-header">
                                <div class="watermark">💧</div>
                                <img src="cid:logo" alt="NWRA Logo" style="width: 100px; height: 100px; margin-bottom: 10px; background-color: white; padding: 8px; border-radius: 6px;">
                                <p class="authority-name">National Water Resources Authority</p>
                                <h1 class="notice-title">⚠ License Expired</h1>
                                <div class="license-number">License No: %s</div>
                            </div>

                            <div class="notice-body">
                                <div class="notice-section">
                                    <div class="section-title">License Holder Information</div>
                                    <table class="detail-table">
                                        <tr class="detail-row">
                                            <td class="detail-label">License Holder:</td>
                                            <td class="detail-value">%s</td>
                                        </tr>
                                    </table>
                                </div>

                                <div class="notice-section">
                                    <div class="section-title">License Details</div>
                                    <table class="detail-table">
                                        <tr class="detail-row">
                                            <td class="detail-label">License Number:</td>
                                            <td class="detail-value">%s</td>
                                        </tr>
                                        <tr class="detail-row">
                                            <td class="detail-label">License Type:</td>
                                            <td class="detail-value">%s</td>
                                        </tr>
                                        <tr class="detail-row">
                                            <td class="detail-label">Expiry Date:</td>
                                            <td class="detail-value">%s</td>
                                        </tr>
                                    </table>
                                </div>

                                <div class="status-box">
                                    STATUS: EXPIRED - RENEWAL REQUIRED
                                </div>

                                <div class="warning-box">
                                    <strong>⚠ IMPORTANT:</strong><br>
                                    Your license has expired and is no longer valid. You must cease all activities covered by this license immediately. Operating without a valid license is a violation of the Water Resources Act and may result in legal penalties.
                                </div>

                                <div class="important-notice">
                                    <strong>What This Means:</strong><br>
                                    <ul style="margin: 10px 0; padding-left: 20px;">
                                        <li>Your license is no longer valid for any water-related activities</li>
                                        <li>All operations under this license must cease immediately</li>
                                        <li>You must apply for renewal to continue operations legally</li>
                                        <li>Continued operation may result in enforcement action</li>
                                    </ul>
                                </div>

                                <div class="notice-section">
                                    <div class="section-title">How to Renew Your License</div>
                                    <ol style="color: #2c2c2c; line-height: 1.6;">
                                        <li>Log into the NWRA e-Water Permit System</li>
                                        <li>Navigate to "Home" section then to renewal tab and choose from their license you wanna renew</li>
                            
                                        <li>Complete the renewal application form</li>
                                        <li>Pay the required renewal fees</li>
                                        <li>Submit supporting documentation</li>
                                        <li>Await review and approval from NWRA</li>
                                    </ol>
                                </div>

                                <div class="warning-box">
                                    <strong>Need Assistance?</strong><br>
                                    If you have questions about the renewal process or need assistance, please contact our licensing office immediately at the details provided below.
                                </div>
                            </div>

                            <div class="notice-footer">
                                <p style="margin: 0; font-weight: bold;">NATIONAL WATER RESOURCES AUTHORITY</p>
                                <p style="margin: 5px 0;">Republic of Malawi</p>
                                <p style="margin: 10px 0;">
                                    Phone: +265 (0) 995 511 963<br>
                                    Email: ceo@nwra.mw
                                </p>
                                <div class="authority-seal">
                                    Official Notice - License Management System<br>
                                    This is an automated notification. Please retain for your records.
                                </div>
                            </div>
                        </div>
                    </body>
                    </html>
                    """,
                    license.getLicenseNumber(),
                    recipientName,
                    license.getLicenseNumber(),
                    licenseType,
                    license.getExpirationDate()
            );

            String taskId = "license-expired-" + license.getId() + "-" + System.currentTimeMillis();
            emailQueueService.sendEmailAsync(taskId, recipientEmail, subject, emailBody);

            log.info("Expiry notification email sent to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Error sending expiry notification email: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get full name of license holder
     */
    private String getFullName(CoreLicense license) {
        try {
            if (license.getCoreLicenseApplication() != null &&
                license.getCoreLicenseApplication().getSysUserAccount() != null) {

                String firstName = license.getCoreLicenseApplication().getSysUserAccount().getFirstName();
                String lastName = license.getCoreLicenseApplication().getSysUserAccount().getLastName();

                if (firstName != null && lastName != null) {
                    return firstName + " " + lastName;
                } else if (firstName != null) {
                    return firstName;
                } else if (lastName != null) {
                    return lastName;
                }
            }
        } catch (Exception e) {
            log.warn("Error getting full name: {}", e.getMessage());
        }
        return "License Holder";
    }

    /**
     * Manual trigger for testing - can be called via admin endpoint if needed
     */
    public void runManually() {
        log.info("Manual license expiry check triggered");
        expireLicenses();
    }
}
