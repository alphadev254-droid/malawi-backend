package mw.nwra.ewaterpermit.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.repository.CoreWaterResourceAreaRepository;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceUnitRepository;
import mw.nwra.ewaterpermit.repository.CoreLicenseWaterUseRepository;
import mw.nwra.ewaterpermit.model.LocationInfo;

@Service
public class WaterAllocationService {

    @Autowired
    private CoreWaterResourceAreaRepository waterResourceAreaRepository;

    @Autowired
    private CoreWaterResourceUnitRepository waterResourceUnitRepository;

    @Autowired
    private CoreLicenseWaterUseRepository licenseWaterUseRepository;

    @Autowired
    private ReverseGeocodingService reverseGeocodingService;

    public List<Map<String, Object>> getWaterAllocationData() {
        System.out.println("[WATER ALLOCATION REPORT] ========== STARTING WATER ALLOCATION REPORT GENERATION ==========");
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // Get all water resource areas
            System.out.println("[WATER ALLOCATION REPORT] Step 1: Fetching all water resource areas from database");
            var areas = waterResourceAreaRepository.findAll();
            System.out.println("[WATER ALLOCATION REPORT] Found " + areas.size() + " water resource areas in database");
            
            if (areas.isEmpty()) {
                System.out.println("[WATER ALLOCATION REPORT] WARNING: No water resource areas found, returning empty result");
                Map<String, Object> sample = new HashMap<>();
                sample.put("area", "No water resource areas found");
                sample.put("abstraction", 0.0);
                sample.put("runoff", 0.0);
                sample.put("level", 0.0);
                sample.put("status", "MINIMAL");
                sample.put("permits", 0);
                result.add(sample);
                return result;
            }
            
            System.out.println("[WATER ALLOCATION REPORT] Step 2: Processing each water resource area for allocation calculations");
            for (int i = 0; i < areas.size(); i++) {
                var area = areas.get(i);
                System.out.println("[WATER ALLOCATION REPORT] Processing area " + (i+1) + "/" + areas.size() + ": " + area.getId());
                
                Map<String, Object> data = new HashMap<>();
                
                // Get area name from reverse geocoding
                String areaName = "Area-" + (area.getId() != null ? area.getId().substring(0, 8) : "Unknown");
                try {
                    System.out.println("[WATER ALLOCATION REPORT] Getting location name for area: " + area.getId());
                    
                    // Check if geometry data exists
                    if (area.getGeoGeometry() != null) {
                        System.out.println("[WATER ALLOCATION REPORT] Geometry data size: " + area.getGeoGeometry().length + " bytes");
                        
                        // Try to peek at the geometry data
                        String geoPreview = new String(area.getGeoGeometry(), java.nio.charset.StandardCharsets.UTF_8);
                        System.out.println("[WATER ALLOCATION REPORT] Geometry preview: " + geoPreview.substring(0, Math.min(200, geoPreview.length())) + "...");
                        
                        LocationInfo location = reverseGeocodingService.getLocationFromGeometry(area.getGeoGeometry());
                        if (location.isSuccess()) {
                            areaName = location.getAreaName();
                            System.out.println("[WATER ALLOCATION REPORT] SUCCESS: Area name resolved to: " + areaName);
                        } else {
                            System.out.println("[WATER ALLOCATION REPORT] FAILED: Could not resolve area name: " + location.getErrorMessage());
                            System.out.println("[WATER ALLOCATION REPORT] Using fallback name: " + areaName);
                        }
                    } else {
                        System.out.println("[WATER ALLOCATION REPORT] WARNING: No geometry data available for area " + area.getId());
                        System.out.println("[WATER ALLOCATION REPORT] Using fallback name: " + areaName);
                    }
                } catch (Exception e) {
                    System.out.println("[WATER ALLOCATION REPORT] ERROR getting area name: " + e.getMessage());
                    e.printStackTrace();
                    System.out.println("[WATER ALLOCATION REPORT] Using fallback name: " + areaName);
                }
                
                // Calculate total abstraction for this area
                double totalAbstraction = 0.0;
                int permitCount = 0;
                
                try {
                    // Query units directly to avoid lazy loading issues
                    System.out.println("[WATER ALLOCATION REPORT] Querying water resource units for area: " + area.getId());
                    var units = waterResourceUnitRepository.findByCoreWaterResourceAreaId(area.getId());
                    System.out.println("[WATER ALLOCATION REPORT] Area " + areaName + " has " + units.size() + " water resource units");
                    
                    for (int j = 0; j < units.size(); j++) {
                        var unit = units.get(j);
                        System.out.println("[WATER ALLOCATION REPORT] Processing unit " + (j+1) + "/" + units.size() + ": " + unit.getId());
                        
                        if (unit != null && unit.getId() != null) {
                            System.out.println("[WATER ALLOCATION REPORT] Querying total abstraction for unit: " + unit.getId());
                            Double unitAbstraction = licenseWaterUseRepository.getTotalAbstractionByWaterResourceUnit(unit.getId());
                            double abstractionValue = unitAbstraction != null ? unitAbstraction : 0.0;
                            System.out.println("[WATER ALLOCATION REPORT] Unit " + unit.getId() + " abstraction: " + abstractionValue + " m³/day");
                            
                            totalAbstraction += abstractionValue;
                            
                            System.out.println("[WATER ALLOCATION REPORT] Querying permit count for unit: " + unit.getId());
                            Long unitPermits = licenseWaterUseRepository.countActivePermitsByWaterResourceUnit(unit.getId());
                            int permitsValue = unitPermits != null ? unitPermits.intValue() : 0;
                            System.out.println("[WATER ALLOCATION REPORT] Unit " + unit.getId() + " permits: " + permitsValue);
                            
                            permitCount += permitsValue;
                        } else {
                            System.out.println("[WATER ALLOCATION REPORT] Skipping null or invalid unit");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[WATER ALLOCATION REPORT] ERROR calculating abstraction for area " + areaName + ": " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Simple runoff estimation (1000 m3/day base)
                double estimatedRunoff = 1000.0;
                System.out.println("[WATER ALLOCATION REPORT] Area " + areaName + " - Total abstraction: " + totalAbstraction + " m³/day, Estimated runoff: " + estimatedRunoff + " m³/day");
                
                // Calculate allocation level
                double allocationLevel = estimatedRunoff > 0 ? (totalAbstraction / estimatedRunoff) * 100 : 0.0;
                
                // Determine status
                String status = "MINIMAL";
                if (allocationLevel >= 80) status = "HIGH";
                else if (allocationLevel >= 50) status = "MEDIUM";
                else if (allocationLevel >= 20) status = "LOW";
                
                System.out.println("[WATER ALLOCATION REPORT] Area " + areaName + " - Allocation level: " + allocationLevel + "%, Status: " + status + ", Permits: " + permitCount);
                
                data.put("area", areaName);
                data.put("abstraction", Math.round(totalAbstraction * 100.0) / 100.0);
                data.put("runoff", estimatedRunoff);
                data.put("level", Math.round(allocationLevel * 10.0) / 10.0);
                data.put("status", status);
                data.put("permits", permitCount);
                
                result.add(data);
                System.out.println("[WATER ALLOCATION REPORT] Completed processing area " + areaName);
            }
            
            System.out.println("[WATER ALLOCATION REPORT] Step 3: Report generation completed successfully");
            System.out.println("[WATER ALLOCATION REPORT] Total areas processed: " + result.size());
            
        } catch (Exception e) {
            System.out.println("[WATER ALLOCATION REPORT] CRITICAL ERROR during report generation: " + e.getMessage());
            e.printStackTrace();
            
            // Return error data if service fails
            Map<String, Object> error = new HashMap<>();
            error.put("area", "Error loading data: " + e.getMessage());
            error.put("abstraction", 0.0);
            error.put("runoff", 0.0);
            error.put("level", 0.0);
            error.put("status", "MINIMAL");
            error.put("permits", 0);
            result.add(error);
        }
        
        System.out.println("[WATER ALLOCATION REPORT] ========== WATER ALLOCATION REPORT GENERATION COMPLETED ==========");
        System.out.println("[WATER ALLOCATION REPORT] Final result: " + result);
        return result;
    }
}