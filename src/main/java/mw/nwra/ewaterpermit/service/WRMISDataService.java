package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.dto.WRMISApprovedPermitDTO;
import mw.nwra.ewaterpermit.dto.WRMISPermitApplicationDTO;

import java.util.Date;
import java.util.List;

/**
 * Service interface for WRMIS Data Integration
 * Provides methods to retrieve permit application and approved permit data for WRMIS
 */
public interface WRMISDataService {

    /**
     * Get all permit applications within date range
     * @param dateFrom Start date (nullable)
     * @param dateTo End date (nullable)
     * @return List of permit application DTOs
     */
    List<WRMISPermitApplicationDTO> getPermitApplications(Date dateFrom, Date dateTo);

    /**
     * Get permit applications by user email
     * @param email Applicant email address
     * @param dateFrom Start date (nullable)
     * @param dateTo End date (nullable)
     * @return List of permit application DTOs
     */
    List<WRMISPermitApplicationDTO> getPermitApplicationsByEmail(String email, Date dateFrom, Date dateTo);

    /**
     * Get permit application by specific date
     * @param specificDate Specific date to query
     * @return List of permit application DTOs
     */
    List<WRMISPermitApplicationDTO> getPermitApplicationsByDate(Date specificDate);

    /**
     * Get all approved permits within date range
     * @param dateFrom Start date (nullable)
     * @param dateTo End date (nullable)
     * @return List of approved permit DTOs
     */
    List<WRMISApprovedPermitDTO> getApprovedPermits(Date dateFrom, Date dateTo);

    /**
     * Get approved permits by user email
     * @param email Permit holder email address
     * @param dateFrom Start date (nullable)
     * @param dateTo End date (nullable)
     * @return List of approved permit DTOs
     */
    List<WRMISApprovedPermitDTO> getApprovedPermitsByEmail(String email, Date dateFrom, Date dateTo);

    /**
     * Get approved permit by license number
     * @param licenseNumber License number
     * @return Approved permit DTO
     */
    WRMISApprovedPermitDTO getApprovedPermitByLicenseNumber(String licenseNumber);

    /**
     * Get approved permits by specific date
     * @param specificDate Specific date to query
     * @return List of approved permit DTOs
     */
    List<WRMISApprovedPermitDTO> getApprovedPermitsByDate(Date specificDate);
}
