package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.dto.WRMISApprovedPermitDTO;
import mw.nwra.ewaterpermit.dto.WRMISPermitApplicationDTO;

import java.util.Date;
import java.util.List;

public interface LegacyFileDataService {

    List<WRMISApprovedPermitDTO> getApprovedPermits(Date dateFrom, Date dateTo);

    List<WRMISApprovedPermitDTO> getApprovedPermitsByDate(Date specificDate);

    List<WRMISApprovedPermitDTO> getApprovedPermitsByEmail(String email, Date dateFrom, Date dateTo);

    WRMISApprovedPermitDTO getApprovedPermitByLicenseNumber(String licenseNumber);

    List<WRMISPermitApplicationDTO> getPermitApplications(Date dateFrom, Date dateTo);

    List<WRMISPermitApplicationDTO> getPermitApplicationsByDate(Date specificDate);

    List<WRMISPermitApplicationDTO> getPermitApplicationsByEmail(String email, Date dateFrom, Date dateTo);

    int getTotalApprovedPermitsCount();

    int getTotalPermitApplicationsCount();
}
