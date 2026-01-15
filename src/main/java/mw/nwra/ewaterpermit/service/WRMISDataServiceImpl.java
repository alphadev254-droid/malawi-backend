package mw.nwra.ewaterpermit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mw.nwra.ewaterpermit.dto.WRMISApprovedPermitDTO;
import mw.nwra.ewaterpermit.dto.WRMISPermitApplicationDTO;
import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.CoreLicenseAssessment;
import mw.nwra.ewaterpermit.repository.CoreLicenseApplicationRepository;
import mw.nwra.ewaterpermit.repository.CoreLicenseAssessmentRepository;
import mw.nwra.ewaterpermit.repository.CoreLicenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of WRMIS Data Service
 * Aggregates and transforms permit application and approved permit data for WRMIS
 */
@Service
public class WRMISDataServiceImpl implements WRMISDataService {

    private static final Logger log = LoggerFactory.getLogger(WRMISDataServiceImpl.class);

    @Autowired
    private CoreLicenseApplicationRepository applicationRepository;

    @Autowired
    private CoreLicenseRepository licenseRepository;

    @Autowired
    private CoreLicenseAssessmentRepository assessmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<WRMISPermitApplicationDTO> getPermitApplications(Date dateFrom, Date dateTo) {
        log.info("📊 WRMIS: Fetching permit applications from {} to {}", dateFrom, dateTo);

        List<CoreLicenseApplication> applications;

        if (dateFrom != null && dateTo != null) {
            // Query with date range
            applications = applicationRepository.findByDateSubmittedBetween(dateFrom, dateTo);
        } else if (dateFrom != null) {
            // Query from date onwards
            applications = applicationRepository.findByDateSubmittedAfter(dateFrom);
        } else if (dateTo != null) {
            // Query up to date
            applications = applicationRepository.findByDateSubmittedBefore(dateTo);
        } else {
            // Get all applications
            applications = applicationRepository.findAll();
        }

        log.info("📋 Found {} permit applications", applications.size());
        return applications.stream()
                .map(this::mapToPermitApplicationDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WRMISPermitApplicationDTO> getPermitApplicationsByEmail(String email, Date dateFrom, Date dateTo) {
        log.info("📧 WRMIS: Fetching permit applications for email: {}", email);

        // Use optimized query instead of findAll() to avoid N+1 problem
        List<CoreLicenseApplication> applications = applicationRepository.findAllWithRelations();

        // Filter by email and date range
        return applications.stream()
                .filter(app -> matchesEmail(app, email))
                .filter(app -> matchesDateRange(app.getDateSubmitted(), dateFrom, dateTo))
                .map(this::mapToPermitApplicationDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WRMISPermitApplicationDTO> getPermitApplicationsByDate(Date specificDate) {
        log.info("📅 WRMIS: Fetching permit applications for date: {}", specificDate);

        if (specificDate == null) {
            return Collections.emptyList();
        }

        LocalDate targetDate = specificDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // Use optimized query instead of findAll() to avoid N+1 problem
        List<CoreLicenseApplication> applications = applicationRepository.findAllWithRelations();

        return applications.stream()
                .filter(app -> {
                    if (app.getDateSubmitted() == null) return false;
                    LocalDate appDate = app.getDateSubmitted().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                    return appDate.equals(targetDate);
                })
                .map(this::mapToPermitApplicationDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WRMISApprovedPermitDTO> getApprovedPermits(Date dateFrom, Date dateTo) {
        log.info("📊 WRMIS: Fetching approved permits from {} to {}", dateFrom, dateTo);

        List<CoreLicense> licenses;

        if (dateFrom != null && dateTo != null) {
            licenses = licenseRepository.findByDateIssuedBetween(dateFrom, dateTo);
        } else if (dateFrom != null) {
            licenses = licenseRepository.findByDateIssuedAfter(dateFrom);
        } else if (dateTo != null) {
            licenses = licenseRepository.findByDateIssuedBefore(dateTo);
        } else {
            licenses = licenseRepository.findAll();
        }

        log.info("📋 Found {} approved permits", licenses.size());
        return mapLicensesToDTOsWithAssessments(licenses);
    }

    @Override
    public List<WRMISApprovedPermitDTO> getApprovedPermitsByEmail(String email, Date dateFrom, Date dateTo) {
        log.info("📧 WRMIS: Fetching approved permits for email: {}", email);

        List<CoreLicense> licenses = licenseRepository.findAll();
        if (licenses == null) {
            return Collections.emptyList();
        }

        List<CoreLicense> filteredLicenses = licenses.stream()
                .filter(license -> matchesLicenseEmail(license, email))
                .filter(license -> matchesDateRange(license.getDateIssued(), dateFrom, dateTo))
                .collect(Collectors.toList());

        return mapLicensesToDTOsWithAssessments(filteredLicenses);
    }

    @Override
    public WRMISApprovedPermitDTO getApprovedPermitByLicenseNumber(String licenseNumber) {
        log.info("🔍 WRMIS: Fetching approved permit by license number: {}", licenseNumber);

        List<CoreLicense> licenses = licenseRepository.findByLicenseNumber(licenseNumber);

        if (licenses.isEmpty()) {
            log.warn("⚠️ No license found with number: {}", licenseNumber);
            return null;
        }

        // Return the most recent version if multiple exist
        CoreLicense license = licenses.stream()
                .max(Comparator.comparing(l -> l.getDateIssued() != null ? l.getDateIssued() : new Date(0)))
                .orElse(licenses.get(0));

        // Use batch method even for single license to reuse logic
        List<WRMISApprovedPermitDTO> results = mapLicensesToDTOsWithAssessments(Collections.singletonList(license));
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<WRMISApprovedPermitDTO> getApprovedPermitsByDate(Date specificDate) {
        log.info("📅 WRMIS: Fetching approved permits for date: {}", specificDate);

        if (specificDate == null) {
            return Collections.emptyList();
        }

        LocalDate targetDate = specificDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        List<CoreLicense> licenses = licenseRepository.findAll();
        if (licenses == null) {
            return Collections.emptyList();
        }

        List<CoreLicense> filteredLicenses = licenses.stream()
                .filter(license -> {
                    if (license.getDateIssued() == null) return false;
                    LocalDate licenseDate = license.getDateIssued().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                    return licenseDate.equals(targetDate);
                })
                .collect(Collectors.toList());

        return mapLicensesToDTOsWithAssessments(filteredLicenses);
    }

    // ========== Private Helper Methods ==========

    /**
     * Map CoreLicenseApplication to WRMISPermitApplicationDTO
     */
    private WRMISPermitApplicationDTO mapToPermitApplicationDTO(CoreLicenseApplication app) {
        WRMISPermitApplicationDTO dto = new WRMISPermitApplicationDTO();

        try {
            // Basic application info
            dto.setApplicationId(app.getId());
            dto.setApplicationNumber(generateApplicationNumber(app));
            dto.setApplicationType(app.getApplicationType() != null ? app.getApplicationType() : "NEW");
            dto.setLicenseType(app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : null);
            dto.setApplicationStatus(app.getCoreApplicationStatus() != null ?
                    app.getCoreApplicationStatus().getName() : null);
            dto.setDateSubmitted(app.getDateSubmitted());
            dto.setDateUpdated(app.getDateUpdated());

            // Parse JSON fields
            parseClientInfo(app.getClientInfo(), dto);
            parseLocationInfo(app.getLocationInfo(), dto);
            parseFormSpecificData(app.getFormSpecificData(), dto);

            // Direct fields
            dto.setPermitDuration(app.getPermitDuration());
            dto.setSourceVillage(app.getSourceVillage());
            dto.setSourceTA(app.getSourceTa());
            dto.setSourceLatitude(app.getSourceEasting());
            dto.setSourceLongitude(app.getSourceNorthing());

        } catch (Exception e) {
            log.error("Error mapping application {}: {}", app.getId(), e.getMessage());
        }

        return dto;
    }

    /**
     * Batch map licenses to DTOs with assessments (optimized to avoid N+1 queries)
     */
    private List<WRMISApprovedPermitDTO> mapLicensesToDTOsWithAssessments(List<CoreLicense> licenses) {
        if (licenses == null || licenses.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract all application IDs
        List<String> applicationIds = licenses.stream()
                .map(CoreLicense::getCoreLicenseApplication)
                .filter(Objects::nonNull)
                .map(CoreLicenseApplication::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Fetch all assessments in ONE query (instead of N queries)
        Map<String, CoreLicenseAssessment> assessmentMap = new HashMap<>();
        if (!applicationIds.isEmpty()) {
            List<CoreLicenseAssessment> assessments = assessmentRepository.findByLicenseApplicationIdIn(applicationIds);
            if (assessments != null) {
                assessments.forEach(assessment -> {
                    if (assessment != null && assessment.getLicenseApplicationId() != null) {
                        assessmentMap.put(assessment.getLicenseApplicationId(), assessment);
                    }
                });
            }
        }

        // Map each license to DTO using the pre-fetched assessments
        return licenses.stream()
                .map(license -> mapToApprovedPermitDTO(license, assessmentMap))
                .collect(Collectors.toList());
    }

    /**
     * Map CoreLicense to WRMISApprovedPermitDTO with pre-fetched assessment
     */
    private WRMISApprovedPermitDTO mapToApprovedPermitDTO(CoreLicense license, Map<String, CoreLicenseAssessment> assessmentMap) {
        WRMISApprovedPermitDTO dto = new WRMISApprovedPermitDTO();

        try {
            // Basic license info
            dto.setLicenseId(license.getId());
            dto.setPermitNumber(license.getLicenseNumber());
            dto.setLicenseStatus(license.getStatus() != null ? license.getStatus() : "ACTIVE");
            dto.setDateIssued(license.getDateIssued());
            dto.setExpirationDate(license.getExpirationDate());
            dto.setLicenseVersion(license.getLicenseVersion());

            // Volume unit is always m³ (cubic meters)
            dto.setVolumeUnit("m³");

            // Get related application data
            CoreLicenseApplication app = license.getCoreLicenseApplication();
            if (app != null) {
                dto.setApplicationId(app.getId());
                dto.setLicenseType(app.getCoreLicenseType() != null ? app.getCoreLicenseType().getName() : null);

                // Holder details from application client_info
                parseClientInfoForLicense(app.getClientInfo(), dto);

                // Get approved volume from pre-fetched assessment (calculated_annual_rental)
                CoreLicenseAssessment assessment = assessmentMap.get(app.getId());
                if (assessment != null && assessment.getCalculatedAnnualRental() != null) {
                    dto.setApprovedVolume(assessment.getCalculatedAnnualRental());
                }
            }

            // Calculate validity period
            if (license.getDateIssued() != null && license.getExpirationDate() != null) {
                long diffInMillis = license.getExpirationDate().getTime() - license.getDateIssued().getTime();
                long years = diffInMillis / (365L * 24 * 60 * 60 * 1000);
                dto.setValidityPeriod(years + " years");
            }

        } catch (Exception e) {
            log.error("Error mapping license {}: {}", license.getId(), e.getMessage());
        }

        return dto;
    }

    /**
     * Parse CLIENT_INFO JSON field
     */
    private void parseClientInfo(String clientInfoJson, WRMISPermitApplicationDTO dto) {
        if (clientInfoJson == null || clientInfoJson.trim().isEmpty()) return;

        try {
            Map<String, Object> clientInfo = objectMapper.readValue(clientInfoJson, Map.class);
            dto.setApplicantName(getString(clientInfo, "clientName", "organizationName", "applicantName"));
            dto.setApplicantEmail(getString(clientInfo, "email", "emailAddress"));
            dto.setApplicantPhone(getString(clientInfo, "phone", "telephone"));
            dto.setApplicantMobile(getString(clientInfo, "mobile", "mobileNumber"));
            dto.setApplicantAddress(getString(clientInfo, "address", "organizationAddress"));
            dto.setApplicantDistrict(getString(clientInfo, "district"));
            dto.setApplicantTA(getString(clientInfo, "traditionalAuthority", "ta"));
        } catch (Exception e) {
            log.error("Error parsing CLIENT_INFO: {}", e.getMessage());
        }
    }

    /**
     * Parse LOCATION_INFO JSON field
     */
    private void parseLocationInfo(String locationInfoJson, WRMISPermitApplicationDTO dto) {
        if (locationInfoJson == null || locationInfoJson.trim().isEmpty()) return;

        try {
            Map<String, Object> locationInfo = objectMapper.readValue(locationInfoJson, Map.class);

            // Parse coordinates
            String coords = getString(locationInfo, "gpsCoordinates", "coordinates", "locationCoordinates");
            if (coords != null && coords.contains(",")) {
                String[] parts = coords.split(",");
                if (parts.length >= 2) {
                    dto.setSourceLatitude(parts[0].trim());
                    dto.setSourceLongitude(parts[1].trim());
                }
            }

            dto.setSourceVillage(getString(locationInfo, "village", "sourceVillage"));
            dto.setSourceDistrict(getString(locationInfo, "district", "sourceDistrict"));
            dto.setSourceTA(getString(locationInfo, "traditionalAuthority", "ta", "sourceTA"));
        } catch (Exception e) {
            log.error("Error parsing LOCATION_INFO: {}", e.getMessage());
        }
    }

    /**
     * Parse FORM_SPECIFIC_DATA JSON field
     */
    private void parseFormSpecificData(String formDataJson, WRMISPermitApplicationDTO dto) {
        if (formDataJson == null || formDataJson.trim().isEmpty()) return;

        try {
            Map<String, Object> formData = objectMapper.readValue(formDataJson, Map.class);

            // Requested volume - check multiple possible field names from different forms
            // Surface/Ground water: quantity, totalVolume, dailyPumpingQuantity, amountPerDayM3
            // Bore completion: aquiferYield, stepYield, averageDischarge
            // Effluent discharge: maxDailyQuantity, maxFlow, wetWellVolume
            String volumeStr = getString(formData,
                "estimatedQuantity", "requestedVolume", "quantity", "totalVolume",
                "dailyPumpingQuantity", "amountPerDayM3",
                "aquiferYield", "stepYield", "averageDischarge",
                "maxDailyQuantity", "maxFlow", "wetWellVolume");
            if (volumeStr != null) {
                try {
                    dto.setRequestedVolume(new BigDecimal(volumeStr));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse volume: {}", volumeStr);
                }
            }

            // Volume unit is always m³ (cubic meters)
            dto.setVolumeUnit("m³");

        } catch (Exception e) {
            log.error("Error parsing FORM_SPECIFIC_DATA: {}", e.getMessage());
        }
    }

    /**
     * Parse CLIENT_INFO for license DTO
     */
    private void parseClientInfoForLicense(String clientInfoJson, WRMISApprovedPermitDTO dto) {
        if (clientInfoJson == null || clientInfoJson.trim().isEmpty()) return;

        try {
            Map<String, Object> clientInfo = objectMapper.readValue(clientInfoJson, Map.class);
            dto.setHolderEmail(getString(clientInfo, "email", "emailAddress"));
            dto.setHolderPhone(getString(clientInfo, "phone", "telephone", "mobile"));
            dto.setHolderAddress(getString(clientInfo, "address", "organizationAddress"));
            dto.setHolderDistrict(getString(clientInfo, "district"));
        } catch (Exception e) {
            log.error("Error parsing CLIENT_INFO for license: {}", e.getMessage());
        }
    }

    /**
     * Parse LOCATION_INFO for license DTO
     */
    private void parseLocationInfoForLicense(String locationInfoJson, WRMISApprovedPermitDTO dto) {
        if (locationInfoJson == null || locationInfoJson.trim().isEmpty()) return;

        try {
            Map<String, Object> locationInfo = objectMapper.readValue(locationInfoJson, Map.class);

            String coords = getString(locationInfo, "gpsCoordinates", "coordinates");
            if (coords != null && coords.contains(",")) {
                String[] parts = coords.split(",");
                if (parts.length >= 2) {
                    dto.setSourceLatitude(parts[0].trim());
                    dto.setSourceLongitude(parts[1].trim());
                }
            }

            dto.setSourceVillage(getString(locationInfo, "village"));
            dto.setSourceDistrict(getString(locationInfo, "district"));
            dto.setSourceTA(getString(locationInfo, "traditionalAuthority", "ta"));
            dto.setCatchmentArea(getString(locationInfo, "catchment", "catchmentArea"));
        } catch (Exception e) {
            log.error("Error parsing LOCATION_INFO for license: {}", e.getMessage());
        }
    }

    /**
     * Parse FORM_SPECIFIC_DATA for license DTO
     */
    private void parseFormSpecificDataForLicense(String formDataJson, WRMISApprovedPermitDTO dto) {
        if (formDataJson == null || formDataJson.trim().isEmpty()) return;

        try {
            Map<String, Object> formData = objectMapper.readValue(formDataJson, Map.class);

            String volumeStr = getString(formData, "estimatedQuantity", "approvedVolume");
            if (volumeStr != null) {
                try {
                    dto.setApprovedVolume(new BigDecimal(volumeStr));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse approved volume: {}", volumeStr);
                }
            }

            dto.setVolumeUnit(getString(formData, "unit", "volumeUnit"));
        } catch (Exception e) {
            log.error("Error parsing FORM_SPECIFIC_DATA for license: {}", e.getMessage());
        }
    }

    /**
     * Get string value from map with fallback keys
     */
    private String getString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * Check if application matches email
     */
    private boolean matchesEmail(CoreLicenseApplication app, String email) {
        if (email == null) return true;

        // Check user account email
        if (app.getSysUserAccount() != null &&
            email.equalsIgnoreCase(app.getSysUserAccount().getEmailAddress())) {
            return true;
        }

        // Check CLIENT_INFO JSON
        if (app.getClientInfo() != null) {
            try {
                Map<String, Object> clientInfo = objectMapper.readValue(app.getClientInfo(), Map.class);
                String jsonEmail = getString(clientInfo, "email", "emailAddress");
                if (email.equalsIgnoreCase(jsonEmail)) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Error parsing CLIENT_INFO for email match: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     * Check if license matches email
     */
    private boolean matchesLicenseEmail(CoreLicense license, String email) {
        if (email == null) return true;

        CoreLicenseApplication app = license.getCoreLicenseApplication();
        if (app != null) {
            return matchesEmail(app, email);
        }

        return false;
    }

    /**
     * Check if date falls within range
     */
    private boolean matchesDateRange(Date date, Date dateFrom, Date dateTo) {
        if (date == null) return false;
        if (dateFrom != null && date.before(dateFrom)) return false;
        if (dateTo != null && date.after(dateTo)) return false;
        return true;
    }

    /**
     * Generate application number for display
     */
    private String generateApplicationNumber(CoreLicenseApplication app) {
        if (app.getId() != null) {
            return "APP-" + app.getId().substring(0, Math.min(8, app.getId().length())).toUpperCase();
        }
        return null;
    }
}
