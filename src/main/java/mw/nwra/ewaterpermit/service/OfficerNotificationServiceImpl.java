package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.model.UserNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class OfficerNotificationServiceImpl implements OfficerNotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(OfficerNotificationServiceImpl.class);
    
    @Autowired
    private SysUserAccountService userAccountService;
    
    @Autowired
    private EmailQueueService emailQueueService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public void notifyOfficersAboutNewApplication(String roleOrGroupName, CoreLicenseApplication application) {
        try {
            log.info("=== STARTING OFFICER NOTIFICATION ===");
            log.info("Role/Group: {}", roleOrGroupName);
            log.info("Application ID: {}", application.getId());
            log.info("Application Status: {}", application.getCoreApplicationStatus() != null ? application.getCoreApplicationStatus().getName() : "NULL");
            log.info("Application Step: {}", application.getCoreApplicationStep() != null ? application.getCoreApplicationStep().getName() : "NULL");
            
            List<SysUserAccount> officers = getOfficersByRole(roleOrGroupName);
            
            if (officers.isEmpty()) {
                log.warn("❌ NO OFFICERS FOUND FOR ROLE: {}", roleOrGroupName);
                log.warn("⚠️  This means no notifications will be sent!");
                return;
            }
            
            log.info("✅ Found {} officers for role '{}': ", officers.size(), roleOrGroupName);
            for (int i = 0; i < officers.size(); i++) {
                SysUserAccount officer = officers.get(i);
                log.info("  {}. Officer: {} {} (Email: {}, Group: {})", 
                    i + 1,
                    officer.getFirstName() != null ? officer.getFirstName() : "",
                    officer.getLastName() != null ? officer.getLastName() : "",
                    officer.getEmailAddress(),
                    officer.getSysUserGroup() != null ? officer.getSysUserGroup().getName() : "NULL"
                );
            }
            
            log.info("🔄 SENDING NOTIFICATIONS TO {} OFFICERS...", officers.size());
            int successCount = 0;
            for (SysUserAccount officer : officers) {
                try {
                    sendNotificationToOfficer(officer, application, "NEW_APPLICATION");
                    successCount++;
                    log.info("✅ Email queued successfully for officer: {}", officer.getEmailAddress());
                } catch (Exception emailError) {
                    log.error("❌ Failed to send email to officer {}: {}", officer.getEmailAddress(), emailError.getMessage());
                }
            }
            
            log.info("=== NOTIFICATION SUMMARY ===");
            log.info("Total officers found: {}", officers.size());
            log.info("Emails successfully queued: {}", successCount);
            log.info("Application: {} | Role: {} | Step: {}", 
                application.getId(), 
                roleOrGroupName,
                application.getCoreApplicationStep() != null ? application.getCoreApplicationStep().getName() : "NULL"
            );
            log.info("=== END OFFICER NOTIFICATION ===");
            
        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR in officer notification for application {}: {}", application.getId(), e.getMessage(), e);
        }
    }
    
    @Override
    public void notifyOfficersAboutStatusChange(String roleOrGroupName, CoreLicenseApplication application, String previousStatus, String newStatus) {
        try {
            log.info("Notifying officers in role '{}' about status change for application: {} ({} -> {})", 
                     roleOrGroupName, application.getId(), previousStatus, newStatus);
            
            List<SysUserAccount> officers = getOfficersByRole(roleOrGroupName);
            
            if (officers.isEmpty()) {
                log.warn("No officers found for role: {}", roleOrGroupName);
                return;
            }
            
            for (SysUserAccount officer : officers) {
                sendNotificationToOfficer(officer, application, "STATUS_CHANGE");
            }
            
            log.info("Notified {} officers about status change for application: {}", officers.size(), application.getId());
            
        } catch (Exception e) {
            log.error("Error notifying officers about status change for application {}: {}", application.getId(), e.getMessage(), e);
        }
    }
    
    @Override
    public List<SysUserAccount> getOfficersByRole(String roleOrGroupName) {
        try {
            log.info("🔍 SEARCHING FOR OFFICERS BY ROLE: '{}' (OPTIMIZED)", roleOrGroupName);
            
            // Use optimized query that loads officers directly with all associations
            List<SysUserAccount> matchingUsers = userAccountService.getOfficersByRoleOptimized(roleOrGroupName);
            log.info("✅ Found {} users matching role '{}' with valid email addresses (single query)", matchingUsers.size(), roleOrGroupName);
            
            if (matchingUsers.isEmpty()) {
                log.warn("❌ NO USERS found for role '{}'. Getting available roles for debugging...", roleOrGroupName);
                // Only load available groups when we need them for debugging
                List<String> allGroups = userAccountService.getAllUserGroupNames();
                log.warn("📋 Available user groups in system: {}", allGroups);
            } else {
                log.info("📊 Officers found for role '{}': ", roleOrGroupName);
                for (int i = 0; i < matchingUsers.size(); i++) {
                    SysUserAccount officer = matchingUsers.get(i);
                    log.info("  {}. {} {} ({})", 
                        i + 1,
                        officer.getFirstName() != null ? officer.getFirstName() : "",
                        officer.getLastName() != null ? officer.getLastName() : "",
                        officer.getEmailAddress()
                    );
                }
            }
            
            return matchingUsers;
        } catch (Exception e) {
            log.error("❌ Error getting officers by role '{}': {}", roleOrGroupName, e.getMessage(), e);
            return List.of();
        }
    }
    
    public void notifyAccountantsAboutReceiptUpload(CoreLicenseApplication application) {
        try {
            log.info("Notifying accountants about receipt upload for application: {}", application.getId());
            
            List<SysUserAccount> accountants = getOfficersByRole("accountant");
            
            if (accountants.isEmpty()) {
                log.warn("No accountants found for receipt upload notification");
                return;
            }
            
            for (SysUserAccount accountant : accountants) {
                sendNotificationToOfficer(accountant, application, "RECEIPT_UPLOAD");
            }
            
            log.info("Notified {} accountants about receipt upload for application: {}", accountants.size(), application.getId());
            
        } catch (Exception e) {
            log.error("Error notifying accountants about receipt upload for application {}: {}", application.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void sendNotificationToOfficer(SysUserAccount officer, CoreLicenseApplication application, String notificationType) {
        try {
            log.info("📧 PREPARING EMAIL FOR OFFICER: {}", officer.getEmailAddress());
            
            if (officer.getEmailAddress() == null || officer.getEmailAddress().isEmpty()) {
                log.warn("❌ Officer {} has no email address, skipping notification", officer.getId());
                return;
            }
            
            log.info("📝 Generating email content...");
            String subject = generateEmailSubject(application, notificationType);
            String emailBody = generateEmailBody(officer, application, notificationType);
            
            String taskId = String.format("officer-notification-%s-%s-%d", 
                                        notificationType.toLowerCase(), 
                                        application.getId(), 
                                        System.currentTimeMillis());
            
            log.info("📮 Email Details:");
            log.info("  - Task ID: {}", taskId);
            log.info("  - Recipient: {}", officer.getEmailAddress());
            log.info("  - Subject: {}", subject);
            log.info("  - Notification Type: {}", notificationType);
            log.info("  - Application ID: {}", application.getId());
            
            log.info("🚀 CALLING EmailQueueService.sendEmailAsync()...");
            emailQueueService.sendEmailAsync(taskId, officer.getEmailAddress(), subject, emailBody);
            log.info("✅ EmailQueueService.sendEmailAsync() completed successfully!");
            
            log.info("📬 Officer notification email queued for: {} ({})", officer.getEmailAddress(), notificationType);
            
            // Save notification to database
            saveNotificationToDatabase(officer, application, notificationType, subject);
            
        } catch (Exception e) {
            log.error("❌ FAILED to send notification to officer {}: {}", officer.getId(), e.getMessage(), e);
            log.error("❌ Error details: ", e);
        }
    }
    
    private String generateEmailSubject(CoreLicenseApplication application, String notificationType) {
        String licenseType = application.getCoreLicenseType() != null ? 
                           application.getCoreLicenseType().getName() : "Water Permit";
        
        return switch (notificationType) {
            case "NEW_APPLICATION" -> String.format("New %s Application Requires Review - %s", licenseType, application.getId());
            case "STATUS_CHANGE" -> String.format("%s Application Status Updated - %s", licenseType, application.getId());
            case "RECEIPT_UPLOAD" -> String.format("Payment Receipt Uploaded - Verification Required: %s", application.getId());
            default -> String.format("Application Notification - %s", application.getId());
        };
    }
    
    private String generateEmailBody(SysUserAccount officer, CoreLicenseApplication application, String notificationType) {
        String officerName = getOfficerFullName(officer);
        String applicantName = getApplicantFullName(application.getSysUserAccount());
        String licenseType = application.getCoreLicenseType() != null ? 
                           application.getCoreLicenseType().getName() : "Water Permit";
        String applicationId = application.getId();
        
        String message = switch (notificationType) {
            case "NEW_APPLICATION" -> String.format(
                "A new %s application has been submitted and requires your review.", licenseType);
            case "STATUS_CHANGE" -> String.format(
                "The status of %s application has been updated and may require your attention.", licenseType);
            case "RECEIPT_UPLOAD" -> String.format(
                "A payment receipt has been uploaded for %s application and requires verification.", licenseType);
            default -> String.format(
                "An application notification has been generated for your attention.");
        };
        
        String actionTitle = switch (notificationType) {
            case "NEW_APPLICATION" -> "New Application Review";
            case "STATUS_CHANGE" -> "Status Update";
            case "RECEIPT_UPLOAD" -> "Payment Verification";
            default -> "Staff Notification";
        };
        
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Staff Notification</title>
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
            padding: 20px 30px;
            }
            .greeting {
            font-size: 16px;
            margin-bottom: 16px;
            }
            .status {
            background-color: #dbeafe;
            color: #1e40af;
            padding: 10px 15px;
            border-radius: 6px;
            text-align: center;
            font-weight: 600;
            margin: 16px 0;
            }
            .details {
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 6px;
            padding: 16px;
            margin: 16px 0;
            }
            .details h3 {
            margin: 0 0 12px 0;
            font-size: 18px;
            color: #1e293b;
            }
            .detail-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid #e2e8f0;
            }
            .detail-row:last-child {
            border-bottom: none;
            }
            .detail-label {
            color: #64748b;
            }
            .detail-value {
            color: #1e293b;
            font-weight: 500;
            }
            .action-notice {
            background-color: #fef3c7;
            border: 1px solid #f59e0b;
            border-radius: 6px;
            padding: 20px;
            margin: 24px 0;
            text-align: center;
            }
            .action-notice p {
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
            <img src="cid:logo" alt="NWRA Logo" class="logo">
            <p>National Water Resources Authority</p>
            <h1>%s</h1>
            </div>

                <div class="content">
                    <div class="greeting">
                        Dear <strong>%s</strong>,
                    </div>
                    
                    <p>%s</p>
                    
                    <div class="status">
                        Application ID: %s
                    </div>
                    
                    <div class="details">
                        <h3>Application Details</h3>
                        <div class="detail-row">
                            <span class="detail-label">Applicant Name : </span>
                            <span class="detail-value">%s</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">License Type : </span>
                            <span class="detail-value">%s</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Current Status : </span>
                            <span class="detail-value">%s</span>
                        </div>
                    </div>
                    
                    <div class="action-notice">
                        <p>Please log into the NWRA system to review this application and take the necessary action.</p>
                    </div>
                    
                    <p>For any questions regarding this application, please reference the Application ID when contacting the relevant department.</p>
                </div>
                
                <div class="footer">
                    <p class="org-name">National Water Resources Authority</p>
                    <p>License Application System</p>
                    <p class="disclaimer">
                        This is an automated notification. Please do not reply to this email.
                    </p>
                </div>
            </div>

            </body>
            </html>
            """,
            actionTitle, officerName, message, applicationId, applicantName, licenseType, 
            application.getCoreApplicationStatus() != null ? application.getCoreApplicationStatus().getName() : "PENDING"
        );
    }
    
    private String getOfficerFullName(SysUserAccount officer) {
        if (officer == null) return "Officer";
        
        StringBuilder name = new StringBuilder();
        if (officer.getFirstName() != null) name.append(officer.getFirstName());
        if (officer.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(officer.getLastName());
        }
        
        return name.length() > 0 ? name.toString() : "Officer";
    }
    
    private String getApplicantFullName(SysUserAccount applicant) {
        if (applicant == null) return "Unknown";
        
        StringBuilder name = new StringBuilder();
        if (applicant.getFirstName() != null) name.append(applicant.getFirstName());
        if (applicant.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(applicant.getLastName());
        }
        
        return name.length() > 0 ? name.toString() : "Unknown";
    }
    
    /**
     * Helper function to save notification to database
     */
    private void saveNotificationToDatabase(SysUserAccount officer, CoreLicenseApplication application, String notificationType, String emailSubject) {
        try {
            UserNotification notification = new UserNotification();
            notification.setId("NOTIF_" + System.currentTimeMillis() + "_" + officer.getId().substring(0, 8));
            notification.setUserId(officer.getId());
            notification.setTitle(emailSubject);
            notification.setMessage(generateNotificationMessage(application, notificationType));
            notification.setType(UserNotification.NotificationType.INFO);
            notification.setCategory(UserNotification.NotificationCategory.SYSTEM);
            notification.setReferenceId(application.getId());
            notification.setReferenceType("LICENSE_APPLICATION");
            notification.setPriority(UserNotification.NotificationPriority.HIGH);
            notification.setActionUrl("/e-services/applications");
            notification.setActionLabel("Review Application");
            notification.setExpiresAt(java.time.LocalDateTime.now().plusDays(30));
            
            notificationService.createNotification(notification);
            log.info("💾 Notification saved to database for officer: {}", officer.getEmailAddress());
        } catch (Exception e) {
            log.error("❌ Failed to save notification to database: {}", e.getMessage());
        }
    }
    
    private String generateNotificationMessage(CoreLicenseApplication application, String notificationType) {
        String licenseType = application.getCoreLicenseType() != null ? application.getCoreLicenseType().getName() : "Water Permit";
        String applicantName = getApplicantFullName(application.getSysUserAccount());
        
        return switch (notificationType) {
            case "NEW_APPLICATION" -> String.format("New %s application from %s requires your review.", licenseType, applicantName);
            case "STATUS_CHANGE" -> String.format("%s application from %s has been updated.", licenseType, applicantName);
            case "RECEIPT_UPLOAD" -> String.format("Payment receipt for %s application from %s requires verification.", licenseType, applicantName);
            default -> "Application notification requires your attention.";
        };
    }
}