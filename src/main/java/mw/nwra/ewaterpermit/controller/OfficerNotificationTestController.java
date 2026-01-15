package mw.nwra.ewaterpermit.controller;

import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.service.OfficerNotificationService;
import mw.nwra.ewaterpermit.service.OfficerNotificationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/test/notifications")

public class OfficerNotificationTestController {

    private static final Logger log = LoggerFactory.getLogger(OfficerNotificationTestController.class);

    @Autowired
    private OfficerNotificationService officerNotificationService;

    @Autowired
    private CoreLicenseApplicationService applicationService;

    /**
     * Test endpoint to check which officers are in a specific role
     */
    @GetMapping("/officers/{roleName}")
    public ResponseEntity<?> getOfficersByRole(@PathVariable String roleName) {
        try {
            log.info("Testing officer lookup for role: {}", roleName);
            
            List<SysUserAccount> officers = officerNotificationService.getOfficersByRole(roleName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("role", roleName);
            response.put("officerCount", officers.size());
            response.put("officers", officers.stream().map(officer -> {
                Map<String, Object> officerInfo = new HashMap<>();
                officerInfo.put("id", officer.getId());
                officerInfo.put("name", getOfficerFullName(officer));
                officerInfo.put("email", officer.getEmailAddress());
                officerInfo.put("group", officer.getSysUserGroup() != null ? officer.getSysUserGroup().getName() : null);
                return officerInfo;
            }).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing officer lookup for role {}: {}", roleName, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test endpoint to simulate receipt upload notification
     */
    @PostMapping("/test-receipt-upload/{applicationId}")
    public ResponseEntity<?> testReceiptUploadNotification(@PathVariable String applicationId) {
        try {
            log.info("Testing receipt upload notification for application: {}", applicationId);
            
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<SysUserAccount> accountants = officerNotificationService.getOfficersByRole("accountant");
            
            if (accountants.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "No accountants found in the system",
                    "applicationId", applicationId,
                    "accountantCount", 0
                ));
            }
            
            // Test the receipt upload notification
            ((OfficerNotificationServiceImpl) officerNotificationService).notifyAccountantsAboutReceiptUpload(application);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Receipt upload notifications sent successfully");
            response.put("applicationId", applicationId);
            response.put("accountantCount", accountants.size());
            response.put("notifiedAccountants", accountants.stream().map(accountant -> {
                Map<String, Object> accountantInfo = new HashMap<>();
                accountantInfo.put("name", getOfficerFullName(accountant));
                accountantInfo.put("email", accountant.getEmailAddress());
                return accountantInfo;
            }).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing receipt upload notification for application {}: {}", applicationId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test endpoint to send notification to officers for a specific application
     */
    @PostMapping("/send/{applicationId}/{roleName}")
    public ResponseEntity<?> testSendNotification(@PathVariable String applicationId, @PathVariable String roleName) {
        try {
            log.info("Testing notification send for application: {} to role: {}", applicationId, roleName);
            
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<SysUserAccount> officers = officerNotificationService.getOfficersByRole(roleName);
            
            if (officers.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "No officers found for role: " + roleName,
                    "applicationId", applicationId,
                    "role", roleName,
                    "officerCount", 0
                ));
            }
            
            // Send notifications
            officerNotificationService.notifyOfficersAboutNewApplication(roleName, application);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Notifications sent successfully");
            response.put("applicationId", applicationId);
            response.put("role", roleName);
            response.put("officerCount", officers.size());
            response.put("notifiedOfficers", officers.stream().map(officer -> {
                Map<String, Object> officerInfo = new HashMap<>();
                officerInfo.put("name", getOfficerFullName(officer));
                officerInfo.put("email", officer.getEmailAddress());
                return officerInfo;
            }).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing notification for application {} to role {}: {}", applicationId, roleName, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List all available user groups/roles
     */
    @GetMapping("/roles")
    public ResponseEntity<?> listAllRoles() {
        try {
            log.info("Listing all available user roles/groups");
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Use specific role names to test notifications");
            response.put("commonRoles", List.of(
                "license_officer",
                "licensing_manager", 
                "ceo",
                "accountant",
                "technical_officer"
            ));
            response.put("testEndpoints", Map.of(
                "checkOfficers", "GET /v1/test/notifications/officers/{roleName}",
                "testGenericNotification", "POST /v1/test/notifications/send/{applicationId}/{roleName}",
                "testReceiptUpload", "POST /v1/test/notifications/test-receipt-upload/{applicationId}"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error listing roles: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test workflow step to role mapping
     */
    @GetMapping("/map-step/{stepName}")
    public ResponseEntity<?> testStepMapping(@PathVariable String stepName) {
        try {
            log.info("Testing step to role mapping for: {}", stepName);
            
            // We'll use reflection or a simple mapping simulation since mapStepToOfficerRole is private
            String mappedRole = simulateStepMapping(stepName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("stepName", stepName);
            response.put("mappedRole", mappedRole);
            response.put("message", "Step '" + stepName + "' maps to role '" + mappedRole + "'");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing step mapping: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Simulate the step mapping logic for testing
     */
    private String simulateStepMapping(String stepName) {
        if (stepName == null) return null;
        
        String stepLower = stepName.toLowerCase();
        
        if (stepLower.contains("payment") || stepLower.contains("accountant") || stepLower.contains("receipt") || stepLower.contains("financial")) {
            return "accountant";
        } else if (stepLower.contains("license officer") || stepLower.contains("review") || stepLower.contains("technical")) {
            return "license_officer";
        } else if (stepLower.contains("manager") || stepLower.contains("approval") || stepLower.contains("decision")) {
            return "licensing_manager";
        } else if (stepLower.contains("ceo") || stepLower.contains("final")) {
            return "ceo";
        } else if (stepLower.contains("assessment") || stepLower.contains("evaluation")) {
            return "technical_officer";
        }
        
        return "license_officer"; // default
    }

    private String getOfficerFullName(SysUserAccount officer) {
        if (officer == null) return "Unknown";
        
        StringBuilder name = new StringBuilder();
        if (officer.getFirstName() != null) name.append(officer.getFirstName());
        if (officer.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(officer.getLastName());
        }
        
        return name.length() > 0 ? name.toString() : "Unknown";
    }
}
