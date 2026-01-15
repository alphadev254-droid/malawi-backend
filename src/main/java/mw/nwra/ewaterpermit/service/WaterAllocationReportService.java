package mw.nwra.ewaterpermit.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreWaterResourceArea;
import mw.nwra.ewaterpermit.model.CoreWaterResourceUnit;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceAreaRepository;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceUnitRepository;
import mw.nwra.ewaterpermit.repository.CoreLicenseWaterUseRepository;
import mw.nwra.ewaterpermit.model.LocationInfo;

@Service
public class WaterAllocationReportService {

    @Autowired
    private CoreWaterResourceAreaRepository waterResourceAreaRepository;

    @Autowired
    private CoreWaterResourceUnitRepository waterResourceUnitRepository;

    @Autowired
    private CoreLicenseWaterUseRepository licenseWaterUseRepository;
    
    @Autowired
    private ReverseGeocodingService reverseGeocodingService;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getWaterDischargeDistributionReport() {
        System.out.println("=== WATER DISCHARGE DISTRIBUTION REPORT DEBUG ===");
        Map<String, Object> report = new HashMap<>();
        
        // Calculate national total discharge
        System.out.println("Step 1: Calculating national total discharge...");
        Double nationalTotalDischarge = licenseWaterUseRepository.getTotalNationalDischarge();
        System.out.println("Raw national total discharge from DB: " + nationalTotalDischarge);
        nationalTotalDischarge = nationalTotalDischarge != null ? nationalTotalDischarge : 0.0;
        System.out.println("Final national total discharge: " + nationalTotalDischarge);
        
        // Get all water resource areas
        System.out.println("Step 2: Getting all water resource areas...");
        List<CoreWaterResourceArea> areas = waterResourceAreaRepository.findAll();
        System.out.println("Found " + areas.size() + " water resource areas");
        
        List<Map<String, Object>> areaReports = new ArrayList<>();
        List<Map<String, Object>> highDischargeAreas = new ArrayList<>();
        
        System.out.println("Step 3: Processing each area...");
        for (int i = 0; i < areas.size(); i++) {
            CoreWaterResourceArea area = areas.get(i);
            System.out.println("Processing area " + (i+1) + "/" + areas.size() + ": ID=" + area.getId());
            
            Map<String, Object> areaData = calculateAreaDischarge(area, nationalTotalDischarge);
            areaReports.add(areaData);
            
            // Log area results
            System.out.println("  Area results: " + areaData);
            
            // Identify high discharge areas (>15% of national total)
            double percentage = (Double) areaData.get("percentageOfNational");
            System.out.println("  Percentage of national: " + percentage + "%");
            if (percentage > 15.0) {
                System.out.println("  -> HIGH DISCHARGE AREA!");
                highDischargeAreas.add(areaData);
            }
        }
        
        System.out.println("Step 4: Sorting areas by discharge percentage...");
        // Sort by discharge percentage (highest first)
        areaReports.sort((a, b) -> Double.compare(
            (Double) b.get("percentageOfNational"), 
            (Double) a.get("percentageOfNational")
        ));
        
        System.out.println("Step 5: Building final report...");
        System.out.println("High discharge areas found: " + highDischargeAreas.size());
        
        report.put("nationalTotalDischarge", nationalTotalDischarge);
        report.put("waterResourceAreas", areaReports);
        report.put("highDischargeAreas", highDischargeAreas);
        report.put("summary", calculateDischargeSummaryStatistics(areaReports, nationalTotalDischarge));
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private Map<String, Object> calculateAreaDischarge(CoreWaterResourceArea area, double nationalTotal) {
        System.out.println("    calculateAreaDischarge for area: " + area.getId());
        Map<String, Object> areaData = new HashMap<>();
        
        areaData.put("areaId", area.getId());
        
        // Use database-persisted area name with fallback to reverse geocoding
        String areaName = getAreaDisplayName(area);
        areaData.put("areaName", areaName);
        areaData.put("geoType", area.getGeoType());
        System.out.println("    Area name: " + areaName);
        
        List<CoreWaterResourceUnit> units = area.getCoreWaterResourceUnits();
        System.out.println("    Units in area: " + units.size());
        
        double totalDischarge = 0.0;
        int activeDischargePermits = 0;
        List<Map<String, Object>> unitDetails = new ArrayList<>();
        
        for (int i = 0; i < units.size(); i++) {
            CoreWaterResourceUnit unit = units.get(i);
            System.out.println("      Processing unit " + (i+1) + "/" + units.size() + ": " + unit.getId());
            
            Double unitDischarge = licenseWaterUseRepository.getTotalDischargeByWaterResourceUnit(unit.getId());
            System.out.println("      Unit discharge from DB: " + unitDischarge);
            if (unitDischarge != null) {
                totalDischarge += unitDischarge;
            }
            
            Long unitPermits = licenseWaterUseRepository.countActiveDischargePermitsByWaterResourceUnit(unit.getId());
            System.out.println("      Unit permits from DB: " + unitPermits);
            if (unitPermits != null) {
                activeDischargePermits += unitPermits.intValue();
            }
            
            Map<String, Object> unitData = new HashMap<>();
            unitData.put("unitId", unit.getId());
            unitData.put("unitName", "Unit " + unit.getId());
            unitData.put("discharge", unitDischarge != null ? unitDischarge : 0.0);
            unitData.put("permits", unitPermits != null ? unitPermits.intValue() : 0);
            unitDetails.add(unitData);
        }
        
        System.out.println("    Total discharge for area: " + totalDischarge);
        System.out.println("    Active discharge permits: " + activeDischargePermits);
        System.out.println("    National total: " + nationalTotal);
        
        double percentageOfNational = nationalTotal > 0 ? (totalDischarge / nationalTotal) * 100 : 0.0;
        System.out.println("    Percentage of national: " + percentageOfNational + "%");
        
        areaData.put("totalDischarge", totalDischarge);
        areaData.put("percentageOfNational", percentageOfNational);
        areaData.put("activeDischargePermits", activeDischargePermits);
        areaData.put("unitsCount", units.size());
        areaData.put("dischargeStatus", determineDischargeStatus(percentageOfNational));
        areaData.put("units", unitDetails);
        
        return areaData;
    }
    
    private String determineDischargeStatus(double percentageOfNational) {
        if (percentageOfNational >= 15.0) {
            return "HIGH";
        } else if (percentageOfNational >= 5.0) {
            return "MEDIUM";
        } else if (percentageOfNational >= 1.0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateDischargeSummaryStatistics(List<Map<String, Object>> areaReports, double nationalTotal) {
        Map<String, Object> summary = new HashMap<>();
        
        if (areaReports.isEmpty()) {
            return summary;
        }
        
        int totalAreas = areaReports.size();
        long highDischargeAreas = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("percentageOfNational"))
            .filter(percentage -> percentage > 15.0)
            .count();
        
        double averageDischargePerArea = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("totalDischarge"))
            .average()
            .orElse(0.0);
        
        int totalDischargePermits = areaReports.stream()
            .mapToInt(area -> (Integer) area.get("activeDischargePermits"))
            .sum();
        
        Map<String, Long> statusCounts = areaReports.stream()
            .collect(Collectors.groupingBy(
                area -> (String) area.get("dischargeStatus"),
                Collectors.counting()
            ));
        
        summary.put("nationalTotalDischarge", nationalTotal);
        summary.put("totalAreas", totalAreas);
        summary.put("highDischargeAreas", highDischargeAreas);
        summary.put("averageDischargePerArea", averageDischargePerArea);
        summary.put("totalDischargePermits", totalDischargePermits);
        summary.put("statusDistribution", statusCounts);
        
        return summary;
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getWaterUseDistributionReport() {
        System.out.println("=== WATER USE DISTRIBUTION REPORT DEBUG ===");
        Map<String, Object> report = new HashMap<>();
        
        // Calculate national total abstraction
        System.out.println("Step 1: Calculating national total abstraction...");
        Double nationalTotalAbstraction = licenseWaterUseRepository.getTotalNationalAbstraction();
        System.out.println("Raw national total abstraction from DB: " + nationalTotalAbstraction);
        nationalTotalAbstraction = nationalTotalAbstraction != null ? nationalTotalAbstraction : 0.0;
        System.out.println("Final national total abstraction: " + nationalTotalAbstraction);
        
        // Get all water resource areas
        System.out.println("Step 2: Getting all water resource areas...");
        List<CoreWaterResourceArea> areas = waterResourceAreaRepository.findAll();
        System.out.println("Found " + areas.size() + " water resource areas");
        
        List<Map<String, Object>> areaReports = new ArrayList<>();
        List<Map<String, Object>> highAbstractionAreas = new ArrayList<>();
        
        System.out.println("Step 3: Processing each area...");
        for (int i = 0; i < areas.size(); i++) {
            CoreWaterResourceArea area = areas.get(i);
            System.out.println("Processing area " + (i+1) + "/" + areas.size() + ": ID=" + area.getId());
            
            Map<String, Object> areaData = calculateAreaAbstraction(area, nationalTotalAbstraction);
            areaReports.add(areaData);
            
            // Log area results
            System.out.println("  Area results: " + areaData);
            
            // Identify high abstraction areas (>15% of national total)
            double percentage = (Double) areaData.get("percentageOfNational");
            System.out.println("  Percentage of national: " + percentage + "%");
            if (percentage > 15.0) {
                System.out.println("  -> HIGH ABSTRACTION AREA!");
                highAbstractionAreas.add(areaData);
            }
        }
        
        System.out.println("Step 4: Sorting areas by abstraction percentage...");
        // Sort by abstraction percentage (highest first)
        areaReports.sort((a, b) -> Double.compare(
            (Double) b.get("percentageOfNational"), 
            (Double) a.get("percentageOfNational")
        ));
        
        System.out.println("Step 5: Building final report...");
        System.out.println("High abstraction areas found: " + highAbstractionAreas.size());
        
        report.put("nationalTotalAbstraction", nationalTotalAbstraction);
        report.put("waterResourceAreas", areaReports);
        report.put("highAbstractionAreas", highAbstractionAreas);
        report.put("summary", calculateAbstractionSummaryStatistics(areaReports, nationalTotalAbstraction));
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private Map<String, Object> calculateAreaAbstraction(CoreWaterResourceArea area, double nationalTotal) {
        System.out.println("    calculateAreaAbstraction for area: " + area.getId());
        Map<String, Object> areaData = new HashMap<>();
        
        areaData.put("areaId", area.getId());
        
        // Use database-persisted area name with fallback to reverse geocoding
        String areaName = getAreaDisplayName(area);
        areaData.put("areaName", areaName);
        areaData.put("geoType", area.getGeoType());
        System.out.println("    Area name: " + areaName);
        
        List<CoreWaterResourceUnit> units = area.getCoreWaterResourceUnits();
        System.out.println("    Units in area: " + units.size());
        
        double totalAbstraction = 0.0;
        int activeAbstractionPermits = 0;
        List<Map<String, Object>> unitDetails = new ArrayList<>();
        
        for (int i = 0; i < units.size(); i++) {
            CoreWaterResourceUnit unit = units.get(i);
            System.out.println("      Processing unit " + (i+1) + "/" + units.size() + ": " + unit.getId());
            
            Double unitAbstraction = licenseWaterUseRepository.getTotalAbstractionByWaterResourceUnit(unit.getId());
            System.out.println("      Unit abstraction from DB: " + unitAbstraction);
            if (unitAbstraction != null) {
                totalAbstraction += unitAbstraction;
            }
            
            Long unitPermits = licenseWaterUseRepository.countActiveAbstractionPermitsByWaterResourceUnit(unit.getId());
            System.out.println("      Unit permits from DB: " + unitPermits);
            if (unitPermits != null) {
                activeAbstractionPermits += unitPermits.intValue();
            }
            
            Map<String, Object> unitData = new HashMap<>();
            unitData.put("unitId", unit.getId());
            unitData.put("unitName", "Unit " + unit.getId());
            unitData.put("abstraction", unitAbstraction != null ? unitAbstraction : 0.0);
            unitData.put("permits", unitPermits != null ? unitPermits.intValue() : 0);
            unitDetails.add(unitData);
        }
        
        System.out.println("    Total abstraction for area: " + totalAbstraction);
        System.out.println("    Active abstraction permits: " + activeAbstractionPermits);
        System.out.println("    National total: " + nationalTotal);
        
        double percentageOfNational = nationalTotal > 0 ? (totalAbstraction / nationalTotal) * 100 : 0.0;
        System.out.println("    Percentage of national: " + percentageOfNational + "%");
        
        areaData.put("totalAbstraction", totalAbstraction);
        areaData.put("percentageOfNational", percentageOfNational);
        areaData.put("activeAbstractionPermits", activeAbstractionPermits);
        areaData.put("unitsCount", units.size());
        areaData.put("abstractionStatus", determineAbstractionStatus(percentageOfNational));
        areaData.put("units", unitDetails);
        
        return areaData;
    }
    
    private String determineAbstractionStatus(double percentageOfNational) {
        if (percentageOfNational >= 15.0) {
            return "HIGH";
        } else if (percentageOfNational >= 5.0) {
            return "MEDIUM";
        } else if (percentageOfNational >= 1.0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateAbstractionSummaryStatistics(List<Map<String, Object>> areaReports, double nationalTotal) {
        Map<String, Object> summary = new HashMap<>();
        
        if (areaReports.isEmpty()) {
            return summary;
        }
        
        int totalAreas = areaReports.size();
        long highAbstractionAreas = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("percentageOfNational"))
            .filter(percentage -> percentage > 15.0)
            .count();
        
        double averageAbstractionPerArea = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("totalAbstraction"))
            .average()
            .orElse(0.0);
        
        int totalAbstractionPermits = areaReports.stream()
            .mapToInt(area -> (Integer) area.get("activeAbstractionPermits"))
            .sum();
        
        Map<String, Long> statusCounts = areaReports.stream()
            .collect(Collectors.groupingBy(
                area -> (String) area.get("abstractionStatus"),
                Collectors.counting()
            ));
        
        summary.put("nationalTotalAbstraction", nationalTotal);
        summary.put("totalAreas", totalAreas);
        summary.put("highAbstractionAreas", highAbstractionAreas);
        summary.put("averageAbstractionPerArea", averageAbstractionPerArea);
        summary.put("totalAbstractionPermits", totalAbstractionPermits);
        summary.put("statusDistribution", statusCounts);
        
        return summary;
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getWaterPermitsDistributionReport() {
        Map<String, Object> report = new HashMap<>();

        Long totalApprovedLicenses = licenseWaterUseRepository.getTotalApprovedLicenses();
        totalApprovedLicenses = totalApprovedLicenses != null ? totalApprovedLicenses : 0L;

        List<CoreWaterResourceArea> areas = waterResourceAreaRepository.findAll();
        List<Map<String, Object>> areaReports = new ArrayList<>();
        List<Map<String, Object>> highDistributionAreas = new ArrayList<>();

        for (int i = 0; i < areas.size(); i++) {
            CoreWaterResourceArea area = areas.get(i);
            Map<String, Object> areaData = calculateAreaPermitsDistribution(area, totalApprovedLicenses);
            areaReports.add(areaData);
            // Identify high distribution areas (>10% of total)
            double percentage = (Double) areaData.get("percentageOfTotal");
            System.out.println("  Percentage of total: " + percentage + "%");
            if (percentage > 10.0) {
                System.out.println("  -> HIGH DISTRIBUTION AREA!");
                highDistributionAreas.add(areaData);
            }
        }
        
        System.out.println("Step 4: Sorting areas by permits count...");
        // Sort by permits count (highest first)
        areaReports.sort((a, b) -> Long.compare(
            (Long) b.get("approvedLicenses"), 
            (Long) a.get("approvedLicenses")
        ));
        
        System.out.println("Step 5: Building final report...");
        System.out.println("High distribution areas found: " + highDistributionAreas.size());
        
        report.put("totalApprovedLicenses", totalApprovedLicenses);
        report.put("waterResourceAreas", areaReports);
        report.put("highDistributionAreas", highDistributionAreas);
        report.put("summary", calculatePermitsDistributionSummaryStatistics(areaReports, totalApprovedLicenses));
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private Map<String, Object> calculateAreaPermitsDistribution(CoreWaterResourceArea area, long totalApproved) {
         Map<String, Object> areaData = new HashMap<>();

        areaData.put("areaId", area.getId());
        
        // Use database-persisted area name with fallback to reverse geocoding
        String areaName = getAreaDisplayName(area);
        areaData.put("areaName", areaName);
        areaData.put("geoType", area.getGeoType());
        System.out.println("    Area name: " + areaName);
        
        List<CoreWaterResourceUnit> units = area.getCoreWaterResourceUnits();
        System.out.println("    Units in area: " + units.size());
        
        long approvedLicenses = 0L;
        List<Map<String, Object>> unitDetails = new ArrayList<>();
        
        for (int i = 0; i < units.size(); i++) {
            CoreWaterResourceUnit unit = units.get(i);
            System.out.println("      Processing unit " + (i+1) + "/" + units.size() + ": " + unit.getId());
            
            Long unitLicenses = licenseWaterUseRepository.getApprovedLicensesByWaterResourceUnit(unit.getId());
            System.out.println("      Unit approved licenses from DB: " + unitLicenses);
            if (unitLicenses != null) {
                approvedLicenses += unitLicenses;
            }
            
            Map<String, Object> unitData = new HashMap<>();
            unitData.put("unitId", unit.getId());
            unitData.put("unitName", "Unit " + unit.getId());
            unitData.put("approvedLicenses", unitLicenses != null ? unitLicenses : 0L);
            unitDetails.add(unitData);
        }
        
        System.out.println("    Total approved licenses for area: " + approvedLicenses);
        System.out.println("    Total approved nationally: " + totalApproved);
        
        double percentageOfTotal = totalApproved > 0 ? ((double) approvedLicenses / totalApproved) * 100 : 0.0;
        System.out.println("    Percentage of total: " + percentageOfTotal + "%");
        
        areaData.put("approvedLicenses", approvedLicenses);
        areaData.put("percentageOfTotal", percentageOfTotal);
        areaData.put("unitsCount", units.size());
        areaData.put("distributionStatus", determineDistributionStatus(percentageOfTotal));
        areaData.put("units", unitDetails);
        
        return areaData;
    }
    
    private String determineDistributionStatus(double percentageOfTotal) {
        if (percentageOfTotal >= 20.0) {
            return "VERY_HIGH";
        } else if (percentageOfTotal >= 10.0) {
            return "HIGH";
        } else if (percentageOfTotal >= 5.0) {
            return "MEDIUM";
        } else if (percentageOfTotal >= 1.0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculatePermitsDistributionSummaryStatistics(List<Map<String, Object>> areaReports, long totalApproved) {
        Map<String, Object> summary = new HashMap<>();
        
        if (areaReports.isEmpty()) {
            return summary;
        }
        
        int totalAreas = areaReports.size();
        long highDistributionAreas = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("percentageOfTotal"))
            .filter(percentage -> percentage > 10.0)
            .count();
        
        double averageLicensesPerArea = areaReports.stream()
            .mapToLong(area -> (Long) area.get("approvedLicenses"))
            .average()
            .orElse(0.0);
        
        Map<String, Long> statusCounts = areaReports.stream()
            .collect(Collectors.groupingBy(
                area -> (String) area.get("distributionStatus"),
                Collectors.counting()
            ));
        
        summary.put("totalApprovedLicenses", totalApproved);
        summary.put("totalAreas", totalAreas);
        summary.put("highDistributionAreas", highDistributionAreas);
        summary.put("averageLicensesPerArea", averageLicensesPerArea);
        summary.put("statusDistribution", statusCounts);
        
        return summary;
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getWaterLicenseDebtDistributionReport() {
        System.out.println("=== WATER LICENSE DEBT DISTRIBUTION REPORT DEBUG ===");
        Map<String, Object> report = new HashMap<>();
        
        // Calculate total unpaid debt
        System.out.println("Step 1: Calculating total unpaid debt...");
        Double totalUnpaidDebt = licenseWaterUseRepository.getTotalUnpaidDebt();
        System.out.println("Total unpaid debt from DB: " + totalUnpaidDebt);
        totalUnpaidDebt = totalUnpaidDebt != null ? totalUnpaidDebt : 0.0;
        System.out.println("Final total unpaid debt: " + totalUnpaidDebt);
        
        // Get all water resource areas
        System.out.println("Step 2: Getting all water resource areas...");
        List<CoreWaterResourceArea> areas = waterResourceAreaRepository.findAll();
        System.out.println("Found " + areas.size() + " water resource areas");
        
        List<Map<String, Object>> areaReports = new ArrayList<>();
        List<Map<String, Object>> highDebtAreas = new ArrayList<>();
        
        System.out.println("Step 3: Processing each area...");
        for (int i = 0; i < areas.size(); i++) {
            CoreWaterResourceArea area = areas.get(i);
            System.out.println("Processing area " + (i+1) + "/" + areas.size() + ": ID=" + area.getId());
            
            Map<String, Object> areaData = calculateAreaDebtDistribution(area, totalUnpaidDebt);
            areaReports.add(areaData);
            
            // Log area results
            System.out.println("  Area results: " + areaData);
            
            // Identify high debt areas (>15% of total debt)
            double percentage = (Double) areaData.get("percentageOfTotalDebt");
            System.out.println("  Percentage of total debt: " + percentage + "%");
            if (percentage > 15.0) {
                System.out.println("  -> HIGH DEBT AREA!");
                highDebtAreas.add(areaData);
            }
        }
        
        System.out.println("Step 4: Sorting areas by debt percentage...");
        // Sort by debt percentage (highest first)
        areaReports.sort((a, b) -> Double.compare(
            (Double) b.get("percentageOfTotalDebt"), 
            (Double) a.get("percentageOfTotalDebt")
        ));
        
        System.out.println("Step 5: Building final report...");
        System.out.println("High debt areas found: " + highDebtAreas.size());
        
        report.put("totalUnpaidDebt", totalUnpaidDebt);
        report.put("waterResourceAreas", areaReports);
        report.put("highDebtAreas", highDebtAreas);
        report.put("summary", calculateDebtDistributionSummaryStatistics(areaReports, totalUnpaidDebt));
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private Map<String, Object> calculateAreaDebtDistribution(CoreWaterResourceArea area, double totalDebt) {
        System.out.println("    calculateAreaDebtDistribution for area: " + area.getId());
        Map<String, Object> areaData = new HashMap<>();
        
        areaData.put("areaId", area.getId());
        
        // Use database-persisted area name with fallback to reverse geocoding
        String areaName = getAreaDisplayName(area);
        areaData.put("areaName", areaName);
        areaData.put("geoType", area.getGeoType());
        System.out.println("    Area name: " + areaName);
        
        List<CoreWaterResourceUnit> units = area.getCoreWaterResourceUnits();
        System.out.println("    Units in area: " + units.size());
        
        double totalAreaDebt = 0.0;
        long unpaidPaymentsCount = 0L;
        List<Map<String, Object>> unitDetails = new ArrayList<>();
        
        for (int i = 0; i < units.size(); i++) {
            CoreWaterResourceUnit unit = units.get(i);
            System.out.println("      Processing unit " + (i+1) + "/" + units.size() + ": " + unit.getId());
            
            Double unitDebt = licenseWaterUseRepository.getUnpaidDebtByWaterResourceUnit(unit.getId());
            System.out.println("      Unit unpaid debt from DB: " + unitDebt);
            if (unitDebt != null) {
                totalAreaDebt += unitDebt;
            }
            
            Long unitPayments = licenseWaterUseRepository.getUnpaidPaymentCountByWaterResourceUnit(unit.getId());
            System.out.println("      Unit unpaid payments from DB: " + unitPayments);
            if (unitPayments != null) {
                unpaidPaymentsCount += unitPayments;
            }
            
            Map<String, Object> unitData = new HashMap<>();
            unitData.put("unitId", unit.getId());
            unitData.put("unitName", "Unit " + unit.getId());
            unitData.put("unpaidDebt", unitDebt != null ? unitDebt : 0.0);
            unitData.put("unpaidPayments", unitPayments != null ? unitPayments : 0L);
            unitDetails.add(unitData);
        }
        
        System.out.println("    Total area debt: " + totalAreaDebt);
        System.out.println("    Unpaid payments count: " + unpaidPaymentsCount);
        System.out.println("    Total debt nationally: " + totalDebt);
        
        double percentageOfTotalDebt = totalDebt > 0 ? (totalAreaDebt / totalDebt) * 100 : 0.0;
        System.out.println("    Percentage of total debt: " + percentageOfTotalDebt + "%");
        
        areaData.put("unpaidDebt", totalAreaDebt);
        areaData.put("percentageOfTotalDebt", percentageOfTotalDebt);
        areaData.put("unpaidPaymentsCount", unpaidPaymentsCount);
        areaData.put("unitsCount", units.size());
        areaData.put("debtStatus", determineDebtStatus(percentageOfTotalDebt));
        areaData.put("units", unitDetails);
        
        return areaData;
    }
    
    private String determineDebtStatus(double percentageOfTotalDebt) {
        if (percentageOfTotalDebt >= 25.0) {
            return "VERY_HIGH";
        } else if (percentageOfTotalDebt >= 15.0) {
            return "HIGH";
        } else if (percentageOfTotalDebt >= 5.0) {
            return "MEDIUM";
        } else if (percentageOfTotalDebt >= 1.0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateDebtDistributionSummaryStatistics(List<Map<String, Object>> areaReports, double totalDebt) {
        Map<String, Object> summary = new HashMap<>();
        
        if (areaReports.isEmpty()) {
            return summary;
        }
        
        int totalAreas = areaReports.size();
        long highDebtAreas = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("percentageOfTotalDebt"))
            .filter(percentage -> percentage > 15.0)
            .count();
        
        double averageDebtPerArea = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("unpaidDebt"))
            .average()
            .orElse(0.0);
        
        long totalUnpaidPayments = areaReports.stream()
            .mapToLong(area -> (Long) area.get("unpaidPaymentsCount"))
            .sum();
        
        Map<String, Long> statusCounts = areaReports.stream()
            .collect(Collectors.groupingBy(
                area -> (String) area.get("debtStatus"),
                Collectors.counting()
            ));
        
        summary.put("totalUnpaidDebt", totalDebt);
        summary.put("totalAreas", totalAreas);
        summary.put("highDebtAreas", highDebtAreas);
        summary.put("averageDebtPerArea", averageDebtPerArea);
        summary.put("totalUnpaidPayments", totalUnpaidPayments);
        summary.put("statusDistribution", statusCounts);
        
        return summary;
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getWaterLicenseRevenueDistributionReport() {
        System.out.println("=== WATER LICENSE REVENUE DISTRIBUTION REPORT DEBUG ===");
        Map<String, Object> report = new HashMap<>();
        
        // Calculate total license revenue
        System.out.println("Step 1: Calculating total license revenue...");
        Double totalLicenseRevenue = licenseWaterUseRepository.getTotalLicenseRevenue();
        System.out.println("Total license revenue from DB: " + totalLicenseRevenue);
        totalLicenseRevenue = totalLicenseRevenue != null ? totalLicenseRevenue : 0.0;
        System.out.println("Final total license revenue: " + totalLicenseRevenue);
        
        // Get all water resource areas
        System.out.println("Step 2: Getting all water resource areas...");
        List<CoreWaterResourceArea> areas = waterResourceAreaRepository.findAll();
        System.out.println("Found " + areas.size() + " water resource areas");
        
        List<Map<String, Object>> areaReports = new ArrayList<>();
        List<Map<String, Object>> highRevenueAreas = new ArrayList<>();
        
        System.out.println("Step 3: Processing each area...");
        for (int i = 0; i < areas.size(); i++) {
            CoreWaterResourceArea area = areas.get(i);
            System.out.println("Processing area " + (i+1) + "/" + areas.size() + ": ID=" + area.getId());
            
            Map<String, Object> areaData = calculateAreaRevenueDistribution(area, totalLicenseRevenue);
            areaReports.add(areaData);
            
            // Log area results
            System.out.println("  Area results: " + areaData);
            
            // Identify high revenue areas (>15% of total revenue)
            double percentage = (Double) areaData.get("percentageOfTotalRevenue");
            System.out.println("  Percentage of total revenue: " + percentage + "%");
            if (percentage > 15.0) {
                System.out.println("  -> HIGH REVENUE AREA!");
                highRevenueAreas.add(areaData);
            }
        }
        
        System.out.println("Step 4: Sorting areas by revenue percentage...");
        // Sort by revenue percentage (highest first)
        areaReports.sort((a, b) -> Double.compare(
            (Double) b.get("percentageOfTotalRevenue"), 
            (Double) a.get("percentageOfTotalRevenue")
        ));
        
        System.out.println("Step 5: Building final report...");
        System.out.println("High revenue areas found: " + highRevenueAreas.size());
        
        report.put("totalLicenseRevenue", totalLicenseRevenue);
        report.put("waterResourceAreas", areaReports);
        report.put("highRevenueAreas", highRevenueAreas);
        report.put("summary", calculateRevenueDistributionSummaryStatistics(areaReports, totalLicenseRevenue));
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private Map<String, Object> calculateAreaRevenueDistribution(CoreWaterResourceArea area, double totalRevenue) {
        System.out.println("    calculateAreaRevenueDistribution for area: " + area.getId());
        Map<String, Object> areaData = new HashMap<>();
        
        areaData.put("areaId", area.getId());
        
        // Use database-persisted area name with fallback to reverse geocoding
        String areaName = getAreaDisplayName(area);
        areaData.put("areaName", areaName);
        areaData.put("geoType", area.getGeoType());
        System.out.println("    Area name: " + areaName);
        
        List<CoreWaterResourceUnit> units = area.getCoreWaterResourceUnits();
        System.out.println("    Units in area: " + units.size());
        
        double totalAreaRevenue = 0.0;
        long paidLicensesCount = 0L;
        List<Map<String, Object>> unitDetails = new ArrayList<>();
        
        for (int i = 0; i < units.size(); i++) {
            CoreWaterResourceUnit unit = units.get(i);
            System.out.println("      Processing unit " + (i+1) + "/" + units.size() + ": " + unit.getId());
            
            Double unitRevenue = licenseWaterUseRepository.getLicenseRevenueByWaterResourceUnit(unit.getId());
            System.out.println("      Unit license revenue from DB: " + unitRevenue);
            if (unitRevenue != null) {
                totalAreaRevenue += unitRevenue;
            }
            
            Long unitPaidLicenses = licenseWaterUseRepository.getPaidLicenseCountByWaterResourceUnit(unit.getId());
            System.out.println("      Unit paid licenses from DB: " + unitPaidLicenses);
            if (unitPaidLicenses != null) {
                paidLicensesCount += unitPaidLicenses;
            }
            
            Map<String, Object> unitData = new HashMap<>();
            unitData.put("unitId", unit.getId());
            unitData.put("unitName", "Unit " + unit.getId());
            unitData.put("licenseRevenue", unitRevenue != null ? unitRevenue : 0.0);
            unitData.put("paidLicenses", unitPaidLicenses != null ? unitPaidLicenses : 0L);
            unitDetails.add(unitData);
        }
        
        System.out.println("    Total area revenue: " + totalAreaRevenue);
        System.out.println("    Paid licenses count: " + paidLicensesCount);
        System.out.println("    Total revenue nationally: " + totalRevenue);
        
        double percentageOfTotalRevenue = totalRevenue > 0 ? (totalAreaRevenue / totalRevenue) * 100 : 0.0;
        System.out.println("    Percentage of total revenue: " + percentageOfTotalRevenue + "%");
        
        areaData.put("licenseRevenue", totalAreaRevenue);
        areaData.put("percentageOfTotalRevenue", percentageOfTotalRevenue);
        areaData.put("paidLicensesCount", paidLicensesCount);
        areaData.put("unitsCount", units.size());
        areaData.put("revenueStatus", determineRevenueStatus(percentageOfTotalRevenue));
        areaData.put("units", unitDetails);
        
        return areaData;
    }
    
    private String determineRevenueStatus(double percentageOfTotalRevenue) {
        if (percentageOfTotalRevenue >= 25.0) {
            return "VERY_HIGH";
        } else if (percentageOfTotalRevenue >= 15.0) {
            return "HIGH";
        } else if (percentageOfTotalRevenue >= 5.0) {
            return "MEDIUM";
        } else if (percentageOfTotalRevenue >= 1.0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateRevenueDistributionSummaryStatistics(List<Map<String, Object>> areaReports, double totalRevenue) {
        Map<String, Object> summary = new HashMap<>();
        
        if (areaReports.isEmpty()) {
            return summary;
        }
        
        int totalAreas = areaReports.size();
        long highRevenueAreas = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("percentageOfTotalRevenue"))
            .filter(percentage -> percentage > 15.0)
            .count();
        
        double averageRevenuePerArea = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("licenseRevenue"))
            .average()
            .orElse(0.0);
        
        long totalPaidLicenses = areaReports.stream()
            .mapToLong(area -> (Long) area.get("paidLicensesCount"))
            .sum();
        
        Map<String, Long> statusCounts = areaReports.stream()
            .collect(Collectors.groupingBy(
                area -> (String) area.get("revenueStatus"),
                Collectors.counting()
            ));
        
        summary.put("totalLicenseRevenue", totalRevenue);
        summary.put("totalAreas", totalAreas);
        summary.put("highRevenueAreas", highRevenueAreas);
        summary.put("averageRevenuePerArea", averageRevenuePerArea);
        summary.put("totalPaidLicenses", totalPaidLicenses);
        summary.put("statusDistribution", statusCounts);
        
        return summary;
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getLargestWaterUsersReport(int limit) {
        System.out.println("=== LARGEST WATER USERS REPORT DEBUG ===");
        System.out.println("Requested limit: " + limit);
        Map<String, Object> report = new HashMap<>();
        
        // Get largest water users from database
        System.out.println("Step 1: Getting largest water users from database...");
        List<Object[]> rawResults = licenseWaterUseRepository.getLargestWaterUsers(limit);
        System.out.println("Found " + rawResults.size() + " water users");
        
        List<Map<String, Object>> largestUsers = new ArrayList<>();
        double totalUsage = 0.0;
        
        System.out.println("Step 2: Processing each user...");
        for (int i = 0; i < rawResults.size(); i++) {
            Object[] row = rawResults.get(i);
            System.out.println("Processing user " + (i+1) + "/" + rawResults.size());
            
            String applicationId = (String) row[0];
            String userAccountId = (String) row[1];
            Double dailyUsage = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            String licenseTypeId = (String) row[3];
            String username = (String) row[5];
            String phoneNumber = (String) row[6];
            String emailAddress = (String) row[7];
            
            System.out.println("  Application ID: " + applicationId);
            System.out.println("  User Account ID: " + userAccountId);
            System.out.println("  Daily Usage: " + dailyUsage + " m³/day");
            System.out.println("  Username: " + username);
            System.out.println("  Phone: " + phoneNumber);
            System.out.println("  Email: " + emailAddress);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("rank", i + 1);
            userInfo.put("applicationId", applicationId);
            userInfo.put("userAccountId", userAccountId);
            userInfo.put("dailyUsage", dailyUsage);
            userInfo.put("licenseTypeId", licenseTypeId);
            
            // User personal details from sys_user_account
            userInfo.put("applicantName", username != null ? username : "Unknown User");
            userInfo.put("organization", username != null ? username + " Organization" : "Unknown Organization");
            userInfo.put("contactInfo", emailAddress != null ? emailAddress : "No email provided");
            userInfo.put("phoneNumber", phoneNumber != null ? phoneNumber : "No phone provided");
            userInfo.put("emailAddress", emailAddress != null ? emailAddress : "No email provided");
            
            // Categorize usage level
            userInfo.put("usageCategory", categorizeUsage(dailyUsage));
            
            largestUsers.add(userInfo);
            totalUsage += dailyUsage;
            
            System.out.println("  User info: " + userInfo);
        }
        
        System.out.println("Step 3: Calculating summary statistics...");
        Map<String, Object> summary = calculateLargestUsersSummary(largestUsers, totalUsage);
        
        report.put("largestUsers", largestUsers);
        report.put("requestedLimit", limit);
        report.put("actualCount", largestUsers.size());
        report.put("totalUsage", totalUsage);
        report.put("summary", summary);
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private String categorizeUsage(double dailyUsage) {
        if (dailyUsage >= 10000) {
            return "VERY_HIGH";
        } else if (dailyUsage >= 5000) {
            return "HIGH";
        } else if (dailyUsage >= 1000) {
            return "MEDIUM";
        } else if (dailyUsage >= 100) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateLargestUsersSummary(List<Map<String, Object>> users, double totalUsage) {
        Map<String, Object> summary = new HashMap<>();
        
        if (users.isEmpty()) {
            return summary;
        }
        
        // Calculate usage statistics
        double averageUsage = totalUsage / users.size();
        double maxUsage = users.stream()
            .mapToDouble(user -> (Double) user.get("dailyUsage"))
            .max()
            .orElse(0.0);
        double minUsage = users.stream()
            .mapToDouble(user -> (Double) user.get("dailyUsage"))
            .min()
            .orElse(0.0);
        
        // Count by usage category
        Map<String, Long> categoryDistribution = users.stream()
            .collect(Collectors.groupingBy(
                user -> (String) user.get("usageCategory"),
                Collectors.counting()
            ));
        
        // Calculate percentages for top users
        for (Map<String, Object> user : users) {
            double usage = (Double) user.get("dailyUsage");
            double percentage = totalUsage > 0 ? (usage / totalUsage) * 100 : 0.0;
            user.put("percentageOfTotal", percentage);
        }
        
        summary.put("totalUsage", totalUsage);
        summary.put("averageUsage", averageUsage);
        summary.put("maxUsage", maxUsage);
        summary.put("minUsage", minUsage);
        summary.put("categoryDistribution", categoryDistribution);
        summary.put("userCount", users.size());
        
        return summary;
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getLargestWaterDischargeReport(int limit) {
        System.out.println("=== LARGEST WATER DISCHARGE REPORT DEBUG ===");
        System.out.println("Requested limit: " + limit);
        Map<String, Object> report = new HashMap<>();
        
        // Get largest water discharge from database
        System.out.println("Step 1: Getting largest water discharge from database...");
        List<Object[]> rawResults = licenseWaterUseRepository.getLargestDischargers(limit);
        System.out.println("Found " + rawResults.size() + " discharge permits");
        
        List<Map<String, Object>> largestDischargers = new ArrayList<>();
        double totalDischarge = 0.0;
        
        System.out.println("Step 2: Processing each discharger...");
        for (int i = 0; i < rawResults.size(); i++) {
            Object[] row = rawResults.get(i);
            System.out.println("Processing discharger " + (i+1) + "/" + rawResults.size());
            
            String applicationId = (String) row[0];
            String userAccountId = (String) row[1];
            Double dailyDischarge = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            String licenseTypeId = (String) row[3];
            String username = (String) row[5];
            String phoneNumber = (String) row[6];
            String emailAddress = (String) row[7];
            
            System.out.println("  Application ID: " + applicationId);
            System.out.println("  User Account ID: " + userAccountId);
            System.out.println("  Daily Discharge: " + dailyDischarge + " m³/day");
            System.out.println("  Username: " + username);
            System.out.println("  Phone: " + phoneNumber);
            System.out.println("  Email: " + emailAddress);
            
            Map<String, Object> dischargerInfo = new HashMap<>();
            dischargerInfo.put("rank", i + 1);
            dischargerInfo.put("applicationId", applicationId);
            dischargerInfo.put("userAccountId", userAccountId);
            dischargerInfo.put("dailyDischarge", dailyDischarge);
            dischargerInfo.put("licenseTypeId", licenseTypeId);
            
            // User personal details from sys_user_account
            dischargerInfo.put("applicantName", username != null ? username : "Unknown User");
            dischargerInfo.put("organization", username != null ? username + " Organization" : "Unknown Organization");
            dischargerInfo.put("contactInfo", emailAddress != null ? emailAddress : "No email provided");
            dischargerInfo.put("phoneNumber", phoneNumber != null ? phoneNumber : "No phone provided");
            dischargerInfo.put("emailAddress", emailAddress != null ? emailAddress : "No email provided");
            
            // Categorize discharge level
            dischargerInfo.put("dischargeCategory", categorizeDischarge(dailyDischarge));
            
            largestDischargers.add(dischargerInfo);
            totalDischarge += dailyDischarge;
            
            System.out.println("  Discharger info: " + dischargerInfo);
        }
        
        System.out.println("Step 3: Calculating summary statistics...");
        Map<String, Object> summary = calculateLargestDischargersSummary(largestDischargers, totalDischarge);
        
        report.put("largestDischargers", largestDischargers);
        report.put("requestedLimit", limit);
        report.put("actualCount", largestDischargers.size());
        report.put("totalDischarge", totalDischarge);
        report.put("summary", summary);
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private String categorizeDischarge(double dailyDischarge) {
        if (dailyDischarge >= 5000) {
            return "VERY_HIGH";
        } else if (dailyDischarge >= 2000) {
            return "HIGH";
        } else if (dailyDischarge >= 500) {
            return "MEDIUM";
        } else if (dailyDischarge >= 100) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateLargestDischargersSummary(List<Map<String, Object>> dischargers, double totalDischarge) {
        Map<String, Object> summary = new HashMap<>();
        
        if (dischargers.isEmpty()) {
            return summary;
        }
        
        // Calculate discharge statistics
        double averageDischarge = totalDischarge / dischargers.size();
        double maxDischarge = dischargers.stream()
            .mapToDouble(discharger -> (Double) discharger.get("dailyDischarge"))
            .max()
            .orElse(0.0);
        double minDischarge = dischargers.stream()
            .mapToDouble(discharger -> (Double) discharger.get("dailyDischarge"))
            .min()
            .orElse(0.0);
        
        // Count by discharge category
        Map<String, Long> categoryDistribution = dischargers.stream()
            .collect(Collectors.groupingBy(
                discharger -> (String) discharger.get("dischargeCategory"),
                Collectors.counting()
            ));
        
        // Calculate percentages for top dischargers
        for (Map<String, Object> discharger : dischargers) {
            double discharge = (Double) discharger.get("dailyDischarge");
            double percentage = totalDischarge > 0 ? (discharge / totalDischarge) * 100 : 0.0;
            discharger.put("percentageOfTotal", percentage);
        }
        
        summary.put("totalDischarge", totalDischarge);
        summary.put("averageDischarge", averageDischarge);
        summary.put("maxDischarge", maxDischarge);
        summary.put("minDischarge", minDischarge);
        summary.put("categoryDistribution", categoryDistribution);
        summary.put("dischargerCount", dischargers.size());
        
        return summary;
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getLargestDebtHoldersReport(int limit) {
        System.out.println("=== LARGEST DEBT HOLDERS REPORT DEBUG ===");
        System.out.println("Requested limit: " + limit);
        Map<String, Object> report = new HashMap<>();
        
        // Get largest debt holders from database
        System.out.println("Step 1: Getting largest debt holders from database...");
        List<Object[]> rawResults = licenseWaterUseRepository.getLargestDebtHolders(limit);
        System.out.println("Found " + rawResults.size() + " debt holders");
        
        List<Map<String, Object>> largestDebtHolders = new ArrayList<>();
        double totalDebt = 0.0;
        
        System.out.println("Step 2: Processing each debt holder...");
        for (int i = 0; i < rawResults.size(); i++) {
            Object[] row = rawResults.get(i);
            System.out.println("Processing debt holder " + (i+1) + "/" + rawResults.size());
            
            String applicationId = (String) row[0];
            String userAccountId = (String) row[1];
            Double debtAmount = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            String licenseTypeId = (String) row[3];
            String username = (String) row[5];
            String phoneNumber = (String) row[6];
            String emailAddress = (String) row[7];
            
            System.out.println("  Application ID: " + applicationId);
            System.out.println("  User Account ID: " + userAccountId);
            System.out.println("  Debt Amount: MWK " + debtAmount);
            System.out.println("  Username: " + username);
            System.out.println("  Phone: " + phoneNumber);
            System.out.println("  Email: " + emailAddress);
            
            Map<String, Object> debtHolderInfo = new HashMap<>();
            debtHolderInfo.put("rank", i + 1);
            debtHolderInfo.put("applicationId", applicationId);
            debtHolderInfo.put("userAccountId", userAccountId);
            debtHolderInfo.put("debtAmount", debtAmount);
            debtHolderInfo.put("licenseTypeId", licenseTypeId);
            
            // User personal details from sys_user_account
            debtHolderInfo.put("applicantName", username != null ? username : "Unknown User");
            debtHolderInfo.put("organization", username != null ? username + " Organization" : "Unknown Organization");
            debtHolderInfo.put("contactInfo", emailAddress != null ? emailAddress : "No email provided");
            debtHolderInfo.put("phoneNumber", phoneNumber != null ? phoneNumber : "No phone provided");
            debtHolderInfo.put("emailAddress", emailAddress != null ? emailAddress : "No email provided");
            
            // Categorize debt level
            debtHolderInfo.put("debtCategory", categorizeDebt(debtAmount));
            
            largestDebtHolders.add(debtHolderInfo);
            totalDebt += debtAmount;
            
            System.out.println("  Debt holder info: " + debtHolderInfo);
        }
        
        System.out.println("Step 3: Calculating summary statistics...");
        Map<String, Object> summary = calculateLargestDebtHoldersSummary(largestDebtHolders, totalDebt);
        
        report.put("largestDebtHolders", largestDebtHolders);
        report.put("requestedLimit", limit);
        report.put("actualCount", largestDebtHolders.size());
        report.put("totalDebt", totalDebt);
        report.put("summary", summary);
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private String categorizeDebt(double debtAmount) {
        if (debtAmount >= 100000) {
            return "VERY_HIGH";
        } else if (debtAmount >= 50000) {
            return "HIGH";
        } else if (debtAmount >= 20000) {
            return "MEDIUM";
        } else if (debtAmount >= 5000) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateLargestDebtHoldersSummary(List<Map<String, Object>> debtHolders, double totalDebt) {
        Map<String, Object> summary = new HashMap<>();
        
        if (debtHolders.isEmpty()) {
            return summary;
        }
        
        // Calculate debt statistics
        double averageDebt = totalDebt / debtHolders.size();
        double maxDebt = debtHolders.stream()
            .mapToDouble(debtHolder -> (Double) debtHolder.get("debtAmount"))
            .max()
            .orElse(0.0);
        double minDebt = debtHolders.stream()
            .mapToDouble(debtHolder -> (Double) debtHolder.get("debtAmount"))
            .min()
            .orElse(0.0);
        
        // Count by debt category
        Map<String, Long> categoryDistribution = debtHolders.stream()
            .collect(Collectors.groupingBy(
                debtHolder -> (String) debtHolder.get("debtCategory"),
                Collectors.counting()
            ));
        
        // Calculate percentages for top debt holders
        for (Map<String, Object> debtHolder : debtHolders) {
            double debt = (Double) debtHolder.get("debtAmount");
            double percentage = totalDebt > 0 ? (debt / totalDebt) * 100 : 0.0;
            debtHolder.put("percentageOfTotal", percentage);
        }
        
        summary.put("totalDebt", totalDebt);
        summary.put("averageDebt", averageDebt);
        summary.put("maxDebt", maxDebt);
        summary.put("minDebt", minDebt);
        summary.put("categoryDistribution", categoryDistribution);
        summary.put("debtHolderCount", debtHolders.size());
        
        return summary;
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getWaterAllocationLevelReport() {
        System.out.println("=== WATER ALLOCATION LEVEL REPORT DEBUG ===");
        Map<String, Object> report = new HashMap<>();
        
        // Get all water resource areas
        System.out.println("Step 1: Getting all water resource areas...");
        List<CoreWaterResourceArea> areas = waterResourceAreaRepository.findAll();
        System.out.println("Found " + areas.size() + " water resource areas");
        
        List<Map<String, Object>> areaReports = new ArrayList<>();
        List<Map<String, Object>> highAllocationAreas = new ArrayList<>();
        
        double nationalTotalAbstraction = 0.0;
        double nationalTotalRunoff = 0.0;
        
        System.out.println("Step 2: Processing each area...");
        for (int i = 0; i < areas.size(); i++) {
            CoreWaterResourceArea area = areas.get(i);
            System.out.println("Processing area " + (i+1) + "/" + areas.size() + ": ID=" + area.getId());
            
            Map<String, Object> areaData = calculateAreaAllocationLevel(area);
            areaReports.add(areaData);
            
            // Log area results
            System.out.println("  Area results: " + areaData);
            
            // Add to national totals
            nationalTotalAbstraction += (Double) areaData.get("abstraction");
            nationalTotalRunoff += (Double) areaData.get("runoff");
            
            // Identify high allocation areas (>80% allocation level)
            double allocationLevel = (Double) areaData.get("level");
            System.out.println("  Allocation level: " + allocationLevel + "%");
            if (allocationLevel >= 80.0) {
                System.out.println("  -> HIGH ALLOCATION AREA!");
                highAllocationAreas.add(areaData);
            }
        }
        
        System.out.println("Step 3: Sorting areas by allocation level...");
        // Sort by allocation level (highest first)
        areaReports.sort((a, b) -> Double.compare(
            (Double) b.get("level"), 
            (Double) a.get("level")
        ));
        
        System.out.println("Step 4: Building final report...");
        System.out.println("High allocation areas found: " + highAllocationAreas.size());
        
        // Calculate national allocation level
        double nationalAllocationLevel = nationalTotalRunoff > 0 ? 
            (nationalTotalAbstraction / nationalTotalRunoff) * 100 : 0.0;
        
        report.put("nationalTotalAbstraction", nationalTotalAbstraction);
        report.put("nationalTotalRunoff", nationalTotalRunoff);
        report.put("nationalAllocationLevel", nationalAllocationLevel);
        report.put("waterResourceAreas", areaReports);
        report.put("highAllocationAreas", highAllocationAreas);
        report.put("summary", calculateAllocationLevelSummaryStatistics(areaReports, nationalTotalAbstraction, nationalTotalRunoff));
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    public Map<String, Object> getWaterAllocationByUnit() {
        return Map.of("message", "Water allocation by unit report not implemented yet");
    }
    
    public List<Map<String, Object>> getHighAllocationAreas() {
        return List.of(Map.of("message", "High allocation areas not implemented yet"));
    }
    
    public List<Map<String, Object>> getHighAllocationUnits() {
        return List.of(Map.of("message", "High allocation units not implemented yet"));
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getLargestRevenueLicencesReport(int limit) {
        System.out.println("=== LARGEST REVENUE LICENCES REPORT DEBUG ===");
        System.out.println("Requested limit: " + limit);
        Map<String, Object> report = new HashMap<>();
        
        // Get largest revenue licences from database
        System.out.println("Step 1: Getting largest revenue licences from database...");
        List<Object[]> rawResults = licenseWaterUseRepository.getLargestRevenueLicences(limit);
        System.out.println("Found " + rawResults.size() + " revenue licences");
        
        List<Map<String, Object>> largestRevenueLicences = new ArrayList<>();
        double totalRevenue = 0.0;
        
        System.out.println("Step 2: Processing each revenue licence...");
        for (int i = 0; i < rawResults.size(); i++) {
            Object[] row = rawResults.get(i);
            System.out.println("Processing revenue licence " + (i+1) + "/" + rawResults.size());
            
            String applicationId = (String) row[0];
            String userAccountId = (String) row[1];
            Double revenueAmount = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            String licenseTypeId = (String) row[3];
            String username = (String) row[5];
            String phoneNumber = (String) row[6];
            String emailAddress = (String) row[7];
            Double debtAmount = row[8] != null ? ((Number) row[8]).doubleValue() : 0.0;
            
            System.out.println("  Application ID: " + applicationId);
            System.out.println("  User Account ID: " + userAccountId);
            System.out.println("  Revenue Amount: MWK " + revenueAmount);
            System.out.println("  Debt Amount: MWK " + debtAmount);
            System.out.println("  Username: " + username);
            System.out.println("  Phone: " + phoneNumber);
            System.out.println("  Email: " + emailAddress);
            
            Map<String, Object> revenueLicenceInfo = new HashMap<>();
            revenueLicenceInfo.put("rank", i + 1);
            revenueLicenceInfo.put("applicationId", applicationId);
            revenueLicenceInfo.put("userAccountId", userAccountId);
            revenueLicenceInfo.put("revenueAmount", revenueAmount);
            revenueLicenceInfo.put("debtAmount", debtAmount);
            revenueLicenceInfo.put("licenseTypeId", licenseTypeId);
            
            // User personal details from sys_user_account
            revenueLicenceInfo.put("applicantName", username != null ? username : "Unknown User");
            revenueLicenceInfo.put("organization", username != null ? username + " Organization" : "Unknown Organization");
            revenueLicenceInfo.put("contactInfo", emailAddress != null ? emailAddress : "No email provided");
            revenueLicenceInfo.put("phoneNumber", phoneNumber != null ? phoneNumber : "No phone provided");
            revenueLicenceInfo.put("emailAddress", emailAddress != null ? emailAddress : "No email provided");
            
            // Categorize revenue level
            revenueLicenceInfo.put("revenueCategory", categorizeRevenue(revenueAmount));
            
            largestRevenueLicences.add(revenueLicenceInfo);
            totalRevenue += revenueAmount;
            
            System.out.println("  Revenue licence info: " + revenueLicenceInfo);
        }
        
        System.out.println("Step 3: Calculating summary statistics...");
        Map<String, Object> summary = calculateLargestRevenueLicencesSummary(largestRevenueLicences, totalRevenue);
        
        report.put("largestRevenueLicences", largestRevenueLicences);
        report.put("requestedLimit", limit);
        report.put("actualCount", largestRevenueLicences.size());
        report.put("totalRevenue", totalRevenue);
        report.put("summary", summary);
        report.put("timestamp", System.currentTimeMillis());
        
        System.out.println("=== REPORT GENERATION COMPLETE ===");
        return report;
    }
    
    private String categorizeRevenue(double revenueAmount) {
        if (revenueAmount >= 100000) {
            return "VERY_HIGH";
        } else if (revenueAmount >= 50000) {
            return "HIGH";
        } else if (revenueAmount >= 20000) {
            return "MEDIUM";
        } else if (revenueAmount >= 5000) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateLargestRevenueLicencesSummary(List<Map<String, Object>> revenueLicences, double totalRevenue) {
        Map<String, Object> summary = new HashMap<>();
        
        if (revenueLicences.isEmpty()) {
            return summary;
        }
        
        // Calculate revenue statistics
        double averageRevenue = totalRevenue / revenueLicences.size();
        double maxRevenue = revenueLicences.stream()
            .mapToDouble(licence -> (Double) licence.get("revenueAmount"))
            .max()
            .orElse(0.0);
        double minRevenue = revenueLicences.stream()
            .mapToDouble(licence -> (Double) licence.get("revenueAmount"))
            .min()
            .orElse(0.0);
        
        // Calculate total debt for these revenue holders
        double totalDebt = revenueLicences.stream()
            .mapToDouble(licence -> (Double) licence.get("debtAmount"))
            .sum();
        
        // Count by revenue category
        Map<String, Long> categoryDistribution = revenueLicences.stream()
            .collect(Collectors.groupingBy(
                licence -> (String) licence.get("revenueCategory"),
                Collectors.counting()
            ));
        
        // Calculate percentages for top revenue licences
        for (Map<String, Object> licence : revenueLicences) {
            double revenue = (Double) licence.get("revenueAmount");
            double percentage = totalRevenue > 0 ? (revenue / totalRevenue) * 100 : 0.0;
            licence.put("percentageOfTotal", percentage);
        }
        
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalDebt", totalDebt);
        summary.put("averageRevenue", averageRevenue);
        summary.put("maxRevenue", maxRevenue);
        summary.put("minRevenue", minRevenue);
        summary.put("categoryDistribution", categoryDistribution);
        summary.put("revenueLicenceCount", revenueLicences.size());
        
        return summary;
    }
    
    private Map<String, Object> calculateAreaAllocationLevel(CoreWaterResourceArea area) {
        System.out.println("    calculateAreaAllocationLevel for area: " + area.getId());
        Map<String, Object> areaData = new HashMap<>();
        
        areaData.put("areaId", area.getId());
        
        // Use database-persisted area name with fallback to reverse geocoding
        String areaName = getAreaDisplayName(area);
        areaData.put("areaName", areaName);
        areaData.put("geoType", area.getGeoType());
        System.out.println("    Area name: " + areaName);
        
        List<CoreWaterResourceUnit> units = area.getCoreWaterResourceUnits();
        System.out.println("    Units in area: " + units.size());
        
        double totalAbstraction = 0.0;
        int activePermits = 0;
        
        for (int i = 0; i < units.size(); i++) {
            CoreWaterResourceUnit unit = units.get(i);
            System.out.println("      Processing unit " + (i+1) + "/" + units.size() + ": " + unit.getId());
            
            Double unitAbstraction = licenseWaterUseRepository.getTotalAbstractionByWaterResourceUnit(unit.getId());
            System.out.println("      Unit abstraction from DB: " + unitAbstraction);
            if (unitAbstraction != null) {
                totalAbstraction += unitAbstraction;
            }
            
            Long unitPermits = licenseWaterUseRepository.countActivePermitsByWaterResourceUnit(unit.getId());
            System.out.println("      Unit permits from DB: " + unitPermits);
            if (unitPermits != null) {
                activePermits += unitPermits.intValue();
            }
        }
        
        // Estimate runoff - for now use a default value, should be replaced with actual data
        double runoff = 1000.0 * units.size(); // Simplified: 1000 m³/day per unit
        double allocationLevel = runoff > 0 ? (totalAbstraction / runoff) * 100 : 0.0;
        
        System.out.println("    Total abstraction for area: " + totalAbstraction);
        System.out.println("    Estimated runoff: " + runoff);
        System.out.println("    Allocation level: " + allocationLevel + "%");
        System.out.println("    Active permits: " + activePermits);
        
        areaData.put("abstraction", totalAbstraction);
        areaData.put("runoff", runoff);
        areaData.put("level", allocationLevel);
        areaData.put("status", determineAllocationStatus(allocationLevel));
        areaData.put("permits", activePermits);
        
        return areaData;
    }
    
    private String determineAllocationStatus(double allocationLevel) {
        if (allocationLevel >= 80.0) {
            return "HIGH";
        } else if (allocationLevel >= 50.0) {
            return "MEDIUM";
        } else if (allocationLevel >= 20.0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private Map<String, Object> calculateAllocationLevelSummaryStatistics(List<Map<String, Object>> areaReports, double nationalAbstraction, double nationalRunoff) {
        Map<String, Object> summary = new HashMap<>();
        
        if (areaReports.isEmpty()) {
            return summary;
        }
        
        int totalAreas = areaReports.size();
        long highAllocationAreas = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("level"))
            .filter(level -> level >= 80.0)
            .count();
        
        double averageAllocationLevel = areaReports.stream()
            .mapToDouble(area -> (Double) area.get("level"))
            .average()
            .orElse(0.0);
        
        int totalPermits = areaReports.stream()
            .mapToInt(area -> (Integer) area.get("permits"))
            .sum();
        
        Map<String, Long> statusCounts = areaReports.stream()
            .collect(Collectors.groupingBy(
                area -> (String) area.get("status"),
                Collectors.counting()
            ));
        
        double nationalAllocationLevel = nationalRunoff > 0 ? 
            (nationalAbstraction / nationalRunoff) * 100 : 0.0;
        
        summary.put("nationalTotalAbstraction", nationalAbstraction);
        summary.put("nationalTotalRunoff", nationalRunoff);
        summary.put("nationalAllocationLevel", nationalAllocationLevel);
        summary.put("totalAreas", totalAreas);
        summary.put("highAllocationAreas", highAllocationAreas);
        summary.put("averageAllocationLevel", averageAllocationLevel);
        summary.put("totalPermits", totalPermits);
        summary.put("statusDistribution", statusCounts);
        
        return summary;
    }
    
    private String getAreaDisplayName(CoreWaterResourceArea area) {
        // Check if display name is already set in database
        if (area.getDisplayName() != null && !area.getDisplayName().trim().isEmpty()) {
            return area.getDisplayName();
        }
        
        // Fall back to reverse geocoding and save the result
        String resolvedName = "Area " + area.getId();
        if (area.getGeoGeometry() != null) {
            try {
                LocationInfo location = reverseGeocodingService.getLocationFromGeometry(area.getGeoGeometry());
                if (location.isSuccess() && location.getAreaName() != null && !location.getAreaName().trim().isEmpty()) {
                    resolvedName = location.getAreaName();
                    
                    // Save the resolved name back to database for future use
                    area.setDisplayName(resolvedName);
                    waterResourceAreaRepository.save(area);
                }
            } catch (Exception e) {
                // If reverse geocoding fails, use fallback name
                System.err.println("Reverse geocoding failed for area " + area.getId() + ": " + e.getMessage());
            }
        }
        
        return resolvedName;
    }
}