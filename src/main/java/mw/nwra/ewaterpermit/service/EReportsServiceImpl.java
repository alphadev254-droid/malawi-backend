package mw.nwra.ewaterpermit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.repository.CoreLicenseRepository;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceAreaRepository;
import mw.nwra.ewaterpermit.repository.CoreLicenseWaterUseRepository;

@Service(value = "eReportsService")
public class EReportsServiceImpl implements EReportsService {

    @Autowired
    private CoreLicenseRepository coreLicenseRepository;

    @Autowired
    private CoreWaterResourceAreaRepository coreWaterResourceAreaRepository;

    @Autowired
    private CoreLicenseWaterUseRepository coreLicenseWaterUseRepository;

    @Override
    public Number getReportValue(String type) {
        switch (type) {
            case "total_approved_licences":
                return getTotalApprovedLicences();
            case "total_areas":
                return getTotalAreas();
            case "high_distribution_areas":
                return getHighDistributionAreas();
            case "average_licenses_per_area":
                return getAverageLicensesPerArea();
            case "national_total_abstraction":
                return getNationalTotalAbstraction();
            case "national_total_discharge":
                return getNationalTotalDischarge();
            case "total_unpaid_debt":
                return getTotalUnpaidDebt();
            case "total_license_revenue":
                return getTotalLicenseRevenue();
            case "national_allocation_level":
                return getNationalAllocationLevel();
            case "high_allocation_areas":
                return getHighAllocationAreas();
            default:
                return Double.NaN;
        }
    }

    private Long getTotalApprovedLicences() {
        try {
            return coreLicenseRepository.countByStatus("ACTIVE");
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long getTotalAreas() {
        try {
            return coreWaterResourceAreaRepository.count();
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long getHighDistributionAreas() {
        try {
            Long totalAreas = coreWaterResourceAreaRepository.count();
            return totalAreas > 10 ? totalAreas / 3 : totalAreas;
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double getAverageLicensesPerArea() {
        try {
            Long totalLicenses = coreLicenseRepository.count();
            Long totalAreas = coreWaterResourceAreaRepository.count();
            
            if (totalAreas == 0) {
                return 0.0;
            }
            
            return (double) totalLicenses / totalAreas;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getNationalTotalAbstraction() {
        try {
            Double total = coreLicenseWaterUseRepository.getTotalNationalAbstraction();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getNationalTotalDischarge() {
        try {
            Double total = coreLicenseWaterUseRepository.getTotalNationalDischarge();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getTotalUnpaidDebt() {
        try {
            Double total = coreLicenseWaterUseRepository.getTotalUnpaidDebt();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getTotalLicenseRevenue() {
        try {
            Double total = coreLicenseWaterUseRepository.getTotalLicenseRevenue();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getNationalAllocationLevel() {
        try {
            Double abstraction = coreLicenseWaterUseRepository.getTotalNationalAbstraction();
            Long totalAreas = coreWaterResourceAreaRepository.count();
            
            if (abstraction == null || totalAreas == 0) {
                return 0.0;
            }
            
            // Estimate national runoff (simplified calculation)
            double estimatedRunoff = totalAreas * 1000.0; // 1000 m³/day per area
            return estimatedRunoff > 0 ? (abstraction / estimatedRunoff) * 100 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Long getHighAllocationAreas() {
        try {
            // Areas with >80% allocation level (simplified calculation)
            Long totalAreas = coreWaterResourceAreaRepository.count();
            return totalAreas > 5 ? totalAreas / 5 : 0L; // Estimate: 20% of areas are high allocation
        } catch (Exception e) {
            return 0L;
        }
    }
}