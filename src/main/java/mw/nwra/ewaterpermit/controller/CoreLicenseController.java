package mw.nwra.ewaterpermit.controller;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.constant.Action;
import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreLicenseService;
import mw.nwra.ewaterpermit.dto.LicenseTransferInfoDto;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;


@RestController
@RequestMapping(value = "/v1/licenses")
public class CoreLicenseController {

    private static final Logger log = LoggerFactory.getLogger(CoreLicenseController.class);

    @Autowired
    private CoreLicenseService coreLicenseService;
    @Autowired
    private AppUtil appUtil;
    @Autowired
    private CoreLicenseApplicationService coreLicenseApplicationService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private mw.nwra.ewaterpermit.service.SysUserAccountService sysUserAccountService;
    @Autowired
    private Auditor auditor;
    @GetMapping(path = "")
    public List<CoreLicense> getCoreLicenses(@RequestParam(value = "page", defaultValue = "0") int page,
                                             @RequestParam(value = "limit", defaultValue = "50") int limit) throws JsonProcessingException {
        List<CoreLicense> newsCategories = this.coreLicenseService.getAllCoreLicenses(page, limit);
        if (newsCategories.isEmpty()) {
            throw new EntityNotFoundException("Object not found");
        }
        return newsCategories;
    }

    @GetMapping(path = "/{id}")
    public CoreLicense getCoreLicenseById(@PathVariable(name = "id") String id) {
        CoreLicense coreLicense = this.coreLicenseService.getCoreLicenseById(id);
        if (coreLicense == null) {
            throw new EntityNotFoundException("Object not found");
        }
        return coreLicense;
    }





    @GetMapping(path = "/my-licenses")
    public List<Map<String, Object>> getMyLicenses(@RequestHeader(name = "Authorization") String token,
                                                   @RequestParam(value = "licenseTypeId", required = false) String licenseTypeId,
                                                   @RequestParam(value = "dateFrom", required = false) String dateFrom,
                                                   @RequestParam(value = "dateTo", required = false) String dateTo,
                                                   @RequestParam(value = "expiryYears", required = false) Integer expiryYears,
                                                   @RequestParam(value = "expiryMonths", required = false) Integer expiryMonths,
                                                   @RequestParam(value = "expiryDays", required = false) Integer expiryDays,
                                                   @RequestParam(value = "excludeWithPendingTransfer", required = false, defaultValue = "false") boolean excludeWithPendingTransfer) {
        SysUserAccount currentUser = AppUtil.getLoggedInUser(token);
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        log.info("Fetching licenses for user: {} with filters - licenseTypeId: {}, dateFrom: {}, dateTo: {}, expiryYears: {}, expiryMonths: {}, expiryDays: {}, excludeWithPendingTransfer: {}",
                 currentUser.getUsername(), licenseTypeId, dateFrom, dateTo, expiryYears, expiryMonths, expiryDays, excludeWithPendingTransfer);
        log.info("Current user ID: {}", currentUser.getId());

        // Check if user has management/administrative role that can view all licenses
        String userRole = currentUser.getSysUserGroup() != null ? currentUser.getSysUserGroup().getName().toLowerCase() : "";
        boolean isManager = "licensing_manager".equals(userRole)
                         || "license_manager".equals(userRole)
                         || "licensing manager".equals(userRole)
                         || "admin".equals(userRole)
                         || "drs".equals(userRole)
                         || "ceo".equals(userRole)
                         || "senior_licensing_officer".equals(userRole)
                         || "senior licensing officer".equals(userRole)
                         || "senior_license_officer".equals(userRole)
                         || "accountant".equals(userRole);

        if (isManager) {
            // Manager view: use optimized method with lightweight DTO
            List<mw.nwra.ewaterpermit.dto.LicenseListDto> licenses;
            if (licenseTypeId != null && !licenseTypeId.trim().isEmpty()) {
                licenses = this.coreLicenseService.getAllLicensesForManagerByLicenseType(licenseTypeId);
                log.info("Found {} licenses for manager role: {} filtered by licenseTypeId: {}", licenses.size(), userRole, licenseTypeId);
            } else {
                licenses = this.coreLicenseService.getAllLicensesForManager();
                log.info("Found {} licenses for manager role: {}", licenses.size(), userRole);
            }

            // Apply date and expiry filtering
            licenses = filterLicensesByDate(licenses, dateFrom, dateTo);
            licenses = filterLicensesByExpiry(licenses, expiryYears, expiryMonths, expiryDays);

            // Exclude licenses with pending or approved transfers if requested
            if (excludeWithPendingTransfer) {
                licenses = licenses.stream()
                    .filter(license -> !coreLicenseApplicationService.hasPendingOrApprovedTransfer(license.getId()))
                    .collect(java.util.stream.Collectors.toList());
                log.info("After excluding licenses with pending transfers: {} licenses remaining", licenses.size());
            }

            log.info("After all filtering: {} licenses remaining", licenses.size());

            return licenses.stream()
                    .sorted((a, b) -> {
                        if (a.getDateIssued() == null && b.getDateIssued() == null) return 0;
                        if (a.getDateIssued() == null) return 1;
                        if (b.getDateIssued() == null) return -1;
                        return b.getDateIssued().compareTo(a.getDateIssued());
                    })
                    .map(license -> {
                        Map<String, Object> licenseData = new java.util.HashMap<>();
                        licenseData.put("id", license.getId());
                        licenseData.put("licenseNumber", license.getLicenseNumber());
                        licenseData.put("status", license.getStatus() != null ? license.getStatus() : "ACTIVE");
                        licenseData.put("dateIssued", license.getDateIssued());
                        licenseData.put("expirationDate", license.getExpirationDate());
                        licenseData.put("dateUpdated", license.getDateUpdated());
                        licenseData.put("licenseVersion", license.getLicenseVersion());
                        licenseData.put("parentLicenseId", license.getParentLicenseId());
                        licenseData.put("firstName", license.getFirstName());
                        licenseData.put("lastName", license.getLastName());
                        licenseData.put("emailAddress", license.getEmailAddress());

                        // C/v1/licenses/my-licensesreate minimal coreLicenseApplication object for frontend compatibility
                        Map<String, Object> applicationData = new java.util.HashMap<>();
                        Map<String, Object> licenseType = new java.util.HashMap<>();
                        licenseType.put("name", license.getLicenseTypeName());
                        applicationData.put("coreLicenseType", licenseType);

                        Map<String, Object> applicationStatus = new java.util.HashMap<>();
                        applicationStatus.put("name", license.getApplicationStatusName());
                        applicationData.put("coreApplicationStatus", applicationStatus);

                        // Add assessment data
                        Map<String, Object> assessmentData = new java.util.HashMap<>();
                        assessmentData.put("calculatedAnnualRental", license.getCalculatedAnnualRental());
                        assessmentData.put("rentalQuantity", license.getRentalQuantity());
                        assessmentData.put("rentalRate", license.getRentalRate());
                        assessmentData.put("recommendedScheduleDate", license.getRecommendedScheduleDate());
                        assessmentData.put("assessmentNotes", license.getAssessmentNotes());
                        assessmentData.put("licenseOfficerId", license.getLicenseOfficerId());
                        assessmentData.put("assessmentStatus", license.getAssessmentStatus());
                        assessmentData.put("assessmentFilesUpload", license.getAssessmentFilesUpload());
                        applicationData.put("coreLicenseAssessment", assessmentData);

                        // Add location and application metadata for license display
                        applicationData.put("locationInfo", license.getLocationInfo());
                        applicationData.put("applicationMetadata", license.getApplicationMetadata());
                        applicationData.put("formSpecificData", license.getFormSpecificData());

                        licenseData.put("coreLicenseApplication", applicationData);

                        return licenseData;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } else {
            // Applicant view: use optimized method with SQL-level filtering
            List<mw.nwra.ewaterpermit.dto.LicenseListDto> licenses;
            if (licenseTypeId != null && !licenseTypeId.trim().isEmpty()) {
                licenses = this.coreLicenseService.getActiveLicensesByOwnerIdAndLicenseType(currentUser.getId(), licenseTypeId);
                log.info("Found {} active licenses for applicant: {} filtered by licenseTypeId: {}", licenses.size(), currentUser.getUsername(), licenseTypeId);
            } else {
                licenses = this.coreLicenseService.getActiveLicensesByOwnerId(currentUser.getId());
                log.info("Found {} active licenses for applicant: {}", licenses.size(), currentUser.getUsername());
            }

            // Apply date and expiry filtering
            licenses = filterLicensesByDate(licenses, dateFrom, dateTo);
            licenses = filterLicensesByExpiry(licenses, expiryYears, expiryMonths, expiryDays);

            // Exclude licenses with pending or approved transfers if requested
            if (excludeWithPendingTransfer) {
                licenses = licenses.stream()
                    .filter(license -> !coreLicenseApplicationService.hasPendingOrApprovedTransfer(license.getId()))
                    .collect(java.util.stream.Collectors.toList());
                log.info("After excluding licenses with pending transfers: {} licenses remaining", licenses.size());
            }

            log.info("After all filtering: {} licenses remaining", licenses.size());

            return licenses.stream()
                    .sorted((a, b) -> {
                        if (a.getDateIssued() == null && b.getDateIssued() == null) return 0;
                        if (a.getDateIssued() == null) return 1;
                        if (b.getDateIssued() == null) return -1;
                        return b.getDateIssued().compareTo(a.getDateIssued());
                    })
                    .map(license -> {
                        Map<String, Object> licenseData = new java.util.HashMap<>();
                        licenseData.put("id", license.getId());
                        licenseData.put("licenseNumber", license.getLicenseNumber());
                        licenseData.put("status", license.getStatus() != null ? license.getStatus() : "ACTIVE");
                        licenseData.put("dateIssued", license.getDateIssued());
                        licenseData.put("expirationDate", license.getExpirationDate());
                        licenseData.put("dateUpdated", license.getDateUpdated());

                        // Create minimal coreLicenseApplication object for frontend compatibility
                        Map<String, Object> applicationData = new java.util.HashMap<>();
                        Map<String, Object> licenseType = new java.util.HashMap<>();
                        licenseType.put("name", license.getLicenseTypeName());
                        applicationData.put("coreLicenseType", licenseType);

                        Map<String, Object> applicationStatus = new java.util.HashMap<>();
                        applicationStatus.put("name", license.getApplicationStatusName());
                        applicationData.put("coreApplicationStatus", applicationStatus);

                        licenseData.put("coreLicenseApplication", applicationData);

                        return licenseData;
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    private List<mw.nwra.ewaterpermit.dto.LicenseListDto> filterLicensesByDate(
            List<mw.nwra.ewaterpermit.dto.LicenseListDto> licenses,
            String dateFrom,
            String dateTo) {

        if (licenses == null || (dateFrom == null && dateTo == null)) {
            return licenses != null ? licenses : new java.util.ArrayList<>();
        }

        java.time.LocalDate fromDate = null;
        java.time.LocalDate toDate = null;

        try {
            if (dateFrom != null && !dateFrom.trim().isEmpty()) {
                fromDate = java.time.LocalDate.parse(dateFrom);
                log.info("Parsed dateFrom: {}", fromDate);
            }
            if (dateTo != null && !dateTo.trim().isEmpty()) {
                toDate = java.time.LocalDate.parse(dateTo);
                log.info("Parsed dateTo: {}", toDate);
            }
        } catch (Exception e) {
            log.error("Invalid date format - dateFrom: {}, dateTo: {}, error: {}", dateFrom, dateTo, e.getMessage(), e);
            return licenses;
        }

        final java.time.LocalDate finalFromDate = fromDate;
        final java.time.LocalDate finalToDate = toDate;

        try {
            return licenses.stream()
                    .filter(license -> {
                        try {
                            Object dateObj = license.getDateIssued();
                            if (dateObj == null) {
                                log.debug("License {} has null dateIssued", license.getLicenseNumber());
                                return false;
                            }

                            java.time.LocalDate licenseDate = null;
                            if (dateObj instanceof java.sql.Date) {
                                licenseDate = ((java.sql.Date) dateObj).toLocalDate();
                            } else if (dateObj instanceof java.util.Date) {
                                licenseDate = new java.sql.Date(((java.util.Date) dateObj).getTime()).toLocalDate();
                            } else if (dateObj instanceof java.time.LocalDate) {
                                licenseDate = (java.time.LocalDate) dateObj;
                            } else {
                                log.warn("Unexpected date type for license {}: {} ({})",
                                        license.getLicenseNumber(), dateObj, dateObj.getClass().getName());
                                return false;
                            }

                            if (licenseDate == null) return false;

                            boolean fromMatch = finalFromDate == null || !licenseDate.isBefore(finalFromDate);
                            boolean toMatch = finalToDate == null || !licenseDate.isAfter(finalToDate);
                            boolean matches = fromMatch && toMatch;

                            log.debug("License {}: date={}, fromMatch={}, toMatch={}, matches={}",
                                     license.getLicenseNumber(), licenseDate, fromMatch, toMatch, matches);

                            return matches;
                        } catch (Exception e) {
                            log.error("Error filtering license {}: {}", license.getLicenseNumber(), e.getMessage(), e);
                            return false;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("Error during date filtering: {}", e.getMessage(), e);
            return licenses;
        }
    }

    private List<mw.nwra.ewaterpermit.dto.LicenseListDto> filterLicensesByExpiry(
            List<mw.nwra.ewaterpermit.dto.LicenseListDto> licenses,
            Integer expiryYears,
            Integer expiryMonths,
            Integer expiryDays) {

        if (licenses == null || (expiryYears == null && expiryMonths == null && expiryDays == null)) {
            return licenses != null ? licenses : new java.util.ArrayList<>();
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate targetDate = today;

        if (expiryYears != null && expiryYears > 0) {
            targetDate = targetDate.plusYears(expiryYears);
        }
        if (expiryMonths != null && expiryMonths > 0) {
            targetDate = targetDate.plusMonths(expiryMonths);
        }
        if (expiryDays != null && expiryDays > 0) {
            targetDate = targetDate.plusDays(expiryDays);
        }

        final java.time.LocalDate finalTargetDate = targetDate;
        log.info("Filtering licenses expiring by: {}", finalTargetDate);

        return licenses.stream()
                .filter(license -> {
                    try {
                        Object expiryObj = license.getExpirationDate();
                        if (expiryObj == null) {
                            return false;
                        }

                        java.time.LocalDate expiryDate = null;
                        if (expiryObj instanceof java.sql.Date) {
                            expiryDate = ((java.sql.Date) expiryObj).toLocalDate();
                        } else if (expiryObj instanceof java.util.Date) {
                            expiryDate = new java.sql.Date(((java.util.Date) expiryObj).getTime()).toLocalDate();
                        } else if (expiryObj instanceof java.time.LocalDate) {
                            expiryDate = (java.time.LocalDate) expiryObj;
                        }

                        return expiryDate != null && !expiryDate.isAfter(finalTargetDate);
                    } catch (Exception e) {
                        log.error("Error filtering license by expiry {}: {}", license.getLicenseNumber(), e.getMessage());
                        return false;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping(path = "/{id}/transfer-info")
    public LicenseTransferInfoDto getLicenseTransferInfo(@PathVariable(name = "id") String originalLicenseId) {
        LicenseTransferInfoDto transferInfo = this.coreLicenseService.getLicenseTransferInfo(originalLicenseId);
        if (transferInfo == null) {
            throw new EntityNotFoundException("No transfer information found for this license");
        }
        return transferInfo;
    }

    @PostMapping(path = "")
    public CoreLicense createCoreLicense(@RequestBody Map<String, Object> coreLicenseRequest,
                                         @RequestHeader(name = "Authorization") String token) {
        CoreLicense coreLicense = (CoreLicense) AppUtil.objectToClass(CoreLicense.class, coreLicenseRequest);
        if (coreLicense == null) {
            throw new ForbiddenException("Could not create the coreLicense");
        }
        CoreLicense saved = this.coreLicenseService.addCoreLicense(coreLicense);
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        auditor.audit(Action.CREATE, "CoreLicense", saved.getId(), user, "Created license");
        return saved;
    }

    @PutMapping(path = "/{id}")
    public CoreLicense updateCoreLicense(@PathVariable(name = "id") String id,
                                         @RequestBody Map<String, Object> coreLicenseRequest,
                                         @RequestHeader(name = "Authorization") String token) {
        CoreLicense oldLicense = this.coreLicenseService.getCoreLicenseById(id);
        if (oldLicense == null) {
            throw new EntityNotFoundException("Role not found");
        }

        // Clone old license for audit
        CoreLicense oldLicenseClone = new CoreLicense();
        BeanUtils.copyProperties(oldLicense, oldLicenseClone);

        String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreLicense.class, coreLicenseRequest);
        CoreLicense coreLicenseFromObj = (CoreLicense) AppUtil.objectToClass(CoreLicense.class, coreLicenseRequest);
        if (coreLicenseFromObj == null) {
            throw new ForbiddenException("Could not update the Object");
        }
        BeanUtils.copyProperties(coreLicenseFromObj, oldLicense, propertiesToIgnore);
        CoreLicense updated = this.coreLicenseService.editCoreLicense(oldLicense);
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        auditor.audit(Action.UPDATE, "CoreLicense", id, user, "Updated license");
        return updated;
    }

    @DeleteMapping(path = "/{id}")
    public void deleteCoreLicense(@PathVariable(name = "id") String id,
                                  @RequestHeader(name = "Authorization") String token) {
        CoreLicense categ = this.coreLicenseService.getCoreLicenseById(id);
        if (categ == null) {
            throw new EntityNotFoundException("User group not found");
        }
        SysUserAccount ua = AppUtil.getLoggedInUser(token);
        if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
        } else {
            throw new EntityNotFoundException("Action denied");
        }

        this.coreLicenseService.deleteCoreLicense(id);
        auditor.audit(Action.DELETE, "CoreLicense", id, ua, "Deleted license");
    }
}