package mw.nwra.ewaterpermit.controller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import mw.nwra.ewaterpermit.model.CoreWaterResourceArea;
import mw.nwra.ewaterpermit.model.CoreWaterResourceUnit;
import mw.nwra.ewaterpermit.model.LocationInfo;
import mw.nwra.ewaterpermit.repository.CoreLicenseWaterUseRepository;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceAreaRepository;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceUnitRepository;
import mw.nwra.ewaterpermit.service.ReverseGeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.service.EReportsService;
import mw.nwra.ewaterpermit.service.WaterAllocationReportService;

@RestController
@RequestMapping(value = "/v1/e-reports", produces = "application/json")
public class EReportsController {

    // Simple cache for reverse geocoding results
    private final Map<String, String> areaNameCache = new ConcurrentHashMap<>();

    @Autowired
    private CoreWaterResourceAreaRepository waterResourceAreaRepository;


    @Autowired
    private CoreLicenseWaterUseRepository licenseWaterUseRepository;

    @Autowired
    private ReverseGeocodingService reverseGeocodingService;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "total_approved_licences",
            "total_areas",
            "high_distribution_areas",
            "average_licenses_per_area",
            "national_total_abstraction",
            "national_total_discharge",
            "total_unpaid_debt",
            "total_license_revenue",
            "national_allocation_level",
            "high_allocation_areas"
    );

    @Autowired
    private EReportsService eReportsService;

    @Autowired
    private WaterAllocationReportService waterAllocationReportService;

    @GetMapping
    public ResponseEntity<Object> getReportValue(@RequestParam("key") String type) {
        if (!ALLOWED_TYPES.contains(type)) {
            return ResponseEntity.badRequest()
                    .body("Invalid type. Allowed types: " + String.join(", ", ALLOWED_TYPES));
        }

        try {
            Number value = eReportsService.getReportValue(type);
            return ResponseEntity.ok(value);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving report data: " + e.getMessage());
        }
    }

    @GetMapping("/water-resource-areas")
    public ResponseEntity<Object> getWaterResourceAreas(@RequestParam(required = false) String reportType) {
        try {
            // If no reportType provided, return permits distribution areas (default behavior)
            if (reportType == null || reportType.trim().isEmpty()) {
                return getPermitsDistributionAreas();
            }

            switch (reportType.toLowerCase()) {
                case "permits":
                    return getPermitsDistributionAreas();
                case "abstraction":
                    return getAbstractionDistributionAreas();
                case "discharge":
                    return getDischargeDistributionAreas();
                case "debt":
                    return getDebtDistributionAreas();
                case "revenue":
                    return getRevenueDistributionAreas();
                case "allocation":
                    return getAllocationLevelAreas();
                default:
                    return ResponseEntity.badRequest()
                            .body("Invalid report type. Allowed types: permits, abstraction, discharge, debt, revenue, allocation");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving water resource areas data: " + e.getMessage());
        }
    }

    @GetMapping("/high-distribution-areas")
    public ResponseEntity<Object> getHighDistributionAreas(@RequestParam(required = false) String reportType) {
        try {
            // If no reportType provided, return permits high distribution areas (default behavior)
            if (reportType == null || reportType.trim().isEmpty()) {
                return getPermitsHighDistributionAreas();
            }

            switch (reportType.toLowerCase()) {
                case "permits":
                    return getPermitsHighDistributionAreas();
                case "abstraction":
                    return getAbstractionHighDistributionAreas();
                case "discharge":
                    return getDischargeHighDistributionAreas();
                case "debt":
                    return getDebtHighDistributionAreas();
                case "revenue":
                    return getRevenueHighDistributionAreas();
                case "allocation":
                    return getAllocationHighDistributionAreas();
                default:
                    return ResponseEntity.badRequest()
                            .body("Invalid report type. Allowed types: permits, abstraction, discharge, debt, revenue, allocation");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving high distribution areas data: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getFullPermitsDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterPermitsDistributionReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving full permits distribution report: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getPermitsDistributionAreas() {
//        try {
//            List<CoreWaterResourceArea> areas = waterResourceAreaRepository.findAll();
//            Long totalApprovedLicenses = licenseWaterUseRepository.getTotalApprovedLicenses();
//
//            // Get all area license counts in one query instead of N queries
//            List<Object[]> areaLicenseCounts = licenseWaterUseRepository.getApprovedLicensesByAllAreas();
//            Map<String, Long> areaLicenseMap = new HashMap<>();
//            for (Object[] row : areaLicenseCounts) {
//                String areaId = (String) row[0];
//                Long count = ((Number) row[1]).longValue();
//                areaLicenseMap.put(areaId, count);
//            }
//
//            List<Map<String, Object>> areaReports = areas.parallelStream()
//                .map(area -> calculateAreaPermitsDistribution(area, totalApprovedLicenses, areaLicenseMap))
//                .collect(Collectors.toList());
//
//            return ResponseEntity.ok(areaReports);
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError()
//                .body("Error retrieving permits distribution areas: " + e.getMessage());
//        }
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterPermitsDistributionReport();
            Object waterResourceAreas = report.get("waterResourceAreas");
            return ResponseEntity.ok(waterResourceAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving permits distribution areas: " + e.getMessage());
        }
    }

    private Map<String, Object> calculateAreaPermitsDistribution(CoreWaterResourceArea area, long totalApproved, Map<String, Long> areaLicenseMap) {
        Map<String, Object> areaData = new HashMap<>();
        areaData.put("areaId", area.getId());

        // Use database-persisted area name with fallback to reverse geocoding
        String areaName = getAreaDisplayName(area);
        areaData.put("areaName", areaName);
        areaData.put("geoType", area.getGeoType());

        List<CoreWaterResourceUnit> units = area.getCoreWaterResourceUnits();

        // Get approved licenses from batch query result instead of individual queries
        long approvedLicenses = areaLicenseMap.getOrDefault(area.getId(), 0L);

        // Only get unit details if there are units and we need detailed breakdown
        List<Map<String, Object>> unitDetails = new ArrayList<>();
        if (!units.isEmpty()) {
            // Get unit license counts in batch for this area
            List<Object[]> unitLicenseCounts = licenseWaterUseRepository.getApprovedLicensesByUnitsInArea(area.getId());
            Map<String, Long> unitLicenseMap = new HashMap<>();
            for (Object[] row : unitLicenseCounts) {
                String unitId = (String) row[0];
                Long count = ((Number) row[1]).longValue();
                unitLicenseMap.put(unitId, count);
            }

            for (CoreWaterResourceUnit unit : units) {
                Long unitLicenses = unitLicenseMap.getOrDefault(unit.getId(), 0L);
                Map<String, Object> unitData = new HashMap<>();
                unitData.put("unitId", unit.getId());
                unitData.put("unitName", "Unit " + unit.getId());
                unitData.put("approvedLicenses", unitLicenses);
                unitDetails.add(unitData);
            }
        }

        double percentageOfTotal = totalApproved > 0 ? ((double) approvedLicenses / totalApproved) * 100 : 0.0;

        areaData.put("approvedLicenses", approvedLicenses);
        areaData.put("percentageOfTotal", percentageOfTotal);
        areaData.put("unitsCount", units.size());
        areaData.put("distributionStatus", determineDistributionStatus(percentageOfTotal));
        areaData.put("units", unitDetails);

        return areaData;
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

    private ResponseEntity<Object> getAbstractionDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterUseDistributionReport();
            Object waterResourceAreas = report.get("waterResourceAreas");
            return ResponseEntity.ok(waterResourceAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving abstraction distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getDischargeDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterDischargeDistributionReport();
            Object waterResourceAreas = report.get("waterResourceAreas");
            return ResponseEntity.ok(waterResourceAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving discharge distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getDebtDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseDebtDistributionReport();
            Object waterResourceAreas = report.get("waterResourceAreas");
            return ResponseEntity.ok(waterResourceAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving debt distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getRevenueDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseRevenueDistributionReport();
            Object waterResourceAreas = report.get("waterResourceAreas");
            return ResponseEntity.ok(waterResourceAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving revenue distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getAllocationLevelAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterAllocationLevelReport();
            Object waterResourceAreas = report.get("waterResourceAreas");
            return ResponseEntity.ok(waterResourceAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving allocation level areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getPermitsHighDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterPermitsDistributionReport();
            Object highDistributionAreas = report.get("highDistributionAreas");
            return ResponseEntity.ok(highDistributionAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving permits high distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getAbstractionHighDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterUseDistributionReport();
            Object highAbstractionAreas = report.get("highAbstractionAreas");
            return ResponseEntity.ok(highAbstractionAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving abstraction high distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getDischargeHighDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterDischargeDistributionReport();
            Object highDischargeAreas = report.get("highDischargeAreas");
            return ResponseEntity.ok(highDischargeAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving discharge high distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getDebtHighDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseDebtDistributionReport();
            Object highDebtAreas = report.get("highDebtAreas");
            return ResponseEntity.ok(highDebtAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving debt high distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getRevenueHighDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseRevenueDistributionReport();
            Object highRevenueAreas = report.get("highRevenueAreas");
            return ResponseEntity.ok(highRevenueAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving revenue high distribution areas: " + e.getMessage());
        }
    }

    private ResponseEntity<Object> getAllocationHighDistributionAreas() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterAllocationLevelReport();
            Object highAllocationAreas = report.get("highAllocationAreas");
            return ResponseEntity.ok(highAllocationAreas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving allocation high distribution areas: " + e.getMessage());
        }
    }

    @PostMapping("/admin/populate-area-names")
    public ResponseEntity<Object> populateAreaNames() {
        try {
            List<CoreWaterResourceArea> areas = waterResourceAreaRepository.findAll();
            int updated = 0;
            int failed = 0;
            
            for (CoreWaterResourceArea area : areas) {
                // Only populate if display name is null or empty
                if (area.getDisplayName() == null || area.getDisplayName().trim().isEmpty()) {
                    try {
                        String resolvedName = getAreaDisplayName(area);
                        if (!resolvedName.startsWith("Area ")) {  // Only count non-fallback names
                            updated++;
                        }
                    } catch (Exception e) {
                        failed++;
                        System.err.println("Failed to populate area " + area.getId() + ": " + e.getMessage());
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalAreas", areas.size());
            result.put("updated", updated);
            result.put("areas", areas);
            result.put("failed", failed);
            result.put("message", "Bulk population completed. Updated: " + updated + ", Failed: " + failed);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error during bulk population: " + e.getMessage());
        }
    }

    @PostMapping("/admin/update-area-name")
    public ResponseEntity<Object> updateAreaName(@RequestBody Map<String, String> request) {
        try {
            String areaId = request.get("areaId");
            String displayName = request.get("displayName");
            
            if (areaId == null || displayName == null) {
                return ResponseEntity.badRequest()
                    .body("Both areaId and displayName are required");
            }
            
            CoreWaterResourceArea area = waterResourceAreaRepository.findById(areaId).orElse(null);
            if (area == null) {
                return ResponseEntity.notFound().build();
            }
            
            area.setDisplayName(displayName.trim());
            waterResourceAreaRepository.save(area);
            
            return ResponseEntity.ok(Map.of("message", "Area name updated successfully", "areaId", areaId, "displayName", displayName));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error updating area name: " + e.getMessage());
        }
    }

    @GetMapping("/admin/areas-without-names")
    public ResponseEntity<Object> getAreasWithoutNames() {
        try {
            List<CoreWaterResourceArea> allAreas = waterResourceAreaRepository.findAll();
            List<Map<String, Object>> areasWithoutNames = new ArrayList<>();
            
            for (CoreWaterResourceArea area : allAreas) {
                if (area.getDisplayName() == null || area.getDisplayName().trim().isEmpty()) {
                    Map<String, Object> areaInfo = new HashMap<>();
                    areaInfo.put("areaId", area.getId());
                    areaInfo.put("geoType", area.getGeoType());
                    areaInfo.put("hasGeometry", area.getGeoGeometry() != null);
                    areasWithoutNames.add(areaInfo);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "areasWithoutNames", areasWithoutNames,
                "count", areasWithoutNames.size(),
                "totalAreas", allAreas.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error retrieving areas without names: " + e.getMessage());
        }
    }
}