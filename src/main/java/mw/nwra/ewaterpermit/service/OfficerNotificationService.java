package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import java.util.List;

public interface OfficerNotificationService {
    
    /**
     * Notify all officers in a specific role/group about a new application at their stage
     */
    void notifyOfficersAboutNewApplication(String roleOrGroupName, CoreLicenseApplication application);
    
    /**
     * Notify officers about application status change
     */
    void notifyOfficersAboutStatusChange(String roleOrGroupName, CoreLicenseApplication application, String previousStatus, String newStatus);
    
    /**
     * Get officers by role/group name
     */
    List<SysUserAccount> getOfficersByRole(String roleOrGroupName);
    
    /**
     * Send individual notification to an officer
     */
    void sendNotificationToOfficer(SysUserAccount officer, CoreLicenseApplication application, String notificationType);
    
    /**
     * Notify accountants about payment receipt upload
     */
    void notifyAccountantsAboutReceiptUpload(CoreLicenseApplication application);
}