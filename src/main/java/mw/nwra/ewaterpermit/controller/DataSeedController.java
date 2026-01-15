package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.model.CoreApplicationStatus;
import mw.nwra.ewaterpermit.model.CoreApplicationStep;
import mw.nwra.ewaterpermit.model.CoreLicenseRequirement;
import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.model.CoreWaterResourceArea;
import mw.nwra.ewaterpermit.model.CoreWaterSource;
import mw.nwra.ewaterpermit.model.CoreWaterUse;
import mw.nwra.ewaterpermit.service.CoreApplicationStatusService;
import mw.nwra.ewaterpermit.service.CoreApplicationStepService;
import mw.nwra.ewaterpermit.service.CoreLicenseRequirementService;
import mw.nwra.ewaterpermit.service.CoreLicenseTypeService;
import mw.nwra.ewaterpermit.service.CoreWaterResourceAreaService;
import mw.nwra.ewaterpermit.service.CoreWaterSourceService;
import mw.nwra.ewaterpermit.service.CoreWaterUseService;

@RestController
@RequestMapping(value = "/v1/data-seed")
public class DataSeedController {

    @Autowired
    private CoreLicenseTypeService licenseTypeService;

    @Autowired
    private CoreLicenseRequirementService licenseRequirementService;

    @Autowired
    private CoreApplicationStatusService applicationStatusService;

    @Autowired
    private CoreApplicationStepService applicationStepService;

    @Autowired
    private CoreWaterResourceAreaService waterResourceAreaService;

    @Autowired
    private CoreWaterSourceService waterSourceService;

    @Autowired
    private CoreWaterUseService waterUseService;

    @GetMapping(path = "/check-data")
    public ResponseEntity<Map<String, Object>> checkData() {
        List<CoreLicenseType> licenseTypes = licenseTypeService.getAllCoreLicenseTypes();
        List<CoreLicenseRequirement> licenseRequirements = licenseRequirementService.getAllCoreLicenseRequirements();
        List<CoreApplicationStatus> applicationStatuses = applicationStatusService.getAllCoreApplicationStatuses(0, 1000);
        List<CoreApplicationStep> applicationSteps = applicationStepService.getAllCoreApplicationSteps(0, 1000);
        List<CoreWaterResourceArea> waterResourceAreas = waterResourceAreaService.getAllCoreWaterResourceAreas(0, 1000);
        List<CoreWaterSource> waterSources = waterSourceService.getAllCoreWaterSources(0, 1000);
        List<CoreWaterUse> waterUses = waterUseService.getAllCoreWaterUses(0, 1000);

        Map<String, Object> data = Map.of(
            "licenseTypes", licenseTypes.size(),
            "licenseRequirements", licenseRequirements.size(),
            "applicationStatuses", applicationStatuses.size(),
            "applicationSteps", applicationSteps.size(),
            "waterResourceAreas", waterResourceAreas.size(),
            "waterSources", waterSources.size(),
            "waterUses", waterUses.size()
        );

        return ResponseEntity.ok(data);
    }

    @PostMapping(path = "/seed-basic-data")
    public ResponseEntity<Map<String, Object>> seedBasicData() {
        int seededCount = 0;

        // Seed License Types
        if (licenseTypeService.getAllCoreLicenseTypes().isEmpty()) {
            String[][] licenseTypeData = {
                {"Surface Water Permit", "5000.0"},
                {"Groundwater Permit", "7500.0"}, 
                {"Effluent Discharge Permit", "10000.0"},
                {"Borehole Drilling Permit", "15000.0"},
                {"Borehole Construction Permit", "12000.0"}
            };

            for (String[] data : licenseTypeData) {
                CoreLicenseType licenseType = new CoreLicenseType();
                licenseType.setName(data[0]);
                licenseType.setDescription("License type for " + data[0]);
                licenseType.setApplicationFees(Double.parseDouble(data[1]));
                licenseType.setDateCreated(new Timestamp(new Date().getTime()));
                licenseTypeService.addCoreLicenseType(licenseType);
                seededCount++;
            }
        }

        // Seed Application Statuses
        if (applicationStatusService.getAllCoreApplicationStatuses(0, 1000).isEmpty()) {
            String[] statusNames = {
                "Submitted",
                "Under Review",
                "Pending Payment",
                "Approved",
                "Rejected",
                "Referred Back"
            };

            for (String name : statusNames) {
                CoreApplicationStatus status = new CoreApplicationStatus();
                status.setName(name);
                status.setDescription("Application status: " + name);
                status.setDateCreated(new Timestamp(new Date().getTime()));
                applicationStatusService.addCoreApplicationStatus(status);
                seededCount++;
            }
        }

        // Seed Water Uses
        if (waterUseService.getAllCoreWaterUses(0, 1000).isEmpty()) {
            String[] waterUseNames = {
                "Domestic Use",
                "Agricultural Irrigation",
                "Industrial Use",
                "Commercial Use",
                "Livestock Watering",
                "Aquaculture",
                "Municipal Supply"
            };

            for (String name : waterUseNames) {
                CoreWaterUse waterUse = new CoreWaterUse();
                waterUse.setName(name);
                waterUse.setDescription("Water use for " + name);
                waterUse.setDateCreated(new Timestamp(new Date().getTime()));
                waterUseService.addCoreWaterUse(waterUse);
                seededCount++;
            }
        }

        // Seed Water Resource Areas
        if (waterResourceAreaService.getAllCoreWaterResourceAreas(0, 1000).isEmpty()) {
            String[] areaTypes = {
                "RIVER_BASIN",
                "CATCHMENT_AREA", 
                "WATERSHED",
                "AQUIFER_ZONE",
                "WETLAND_AREA"
            };

            for (String geoType : areaTypes) {
                CoreWaterResourceArea area = new CoreWaterResourceArea();
                area.setGeoType(geoType);
                area.setDateCreated(new Timestamp(new Date().getTime()));
                waterResourceAreaService.addCoreWaterResourceArea(area);
                seededCount++;
            }
        }

        // Seed Water Sources
        if (waterSourceService.getAllCoreWaterSources(0, 1000).isEmpty()) {
            String[] sourceNames = {
                "Lilongwe River",
                "Shire River",
                "Bua River",
                "Dwangwa River",
                "Ruo River",
                "Lake Malawi",
                "Lake Chilwa",
                "Groundwater - Shallow",
                "Groundwater - Deep"
            };

            for (String name : sourceNames) {
                CoreWaterSource source = new CoreWaterSource();
                source.setName(name);
                source.setDescription("Water source: " + name);
                source.setDateCreated(new Timestamp(new Date().getTime()));
                waterSourceService.addCoreWaterSource(source);
                seededCount++;
            }
        }

        return ResponseEntity.ok(Map.of(
            "message", "Basic data seeded successfully",
            "seededCount", seededCount
        ));
    }

    @PostMapping(path = "/seed-license-requirements")
    public ResponseEntity<Map<String, Object>> seedLicenseRequirements() {
        List<CoreLicenseType> licenseTypes = licenseTypeService.getAllCoreLicenseTypes();
        
        if (licenseTypes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No license types found. Please seed basic data first."
            ));
        }

        int seededCount = 0;

        // Seed requirements for each license type
        for (CoreLicenseType licenseType : licenseTypes) {
            String[] requirements = getRequirementsForLicenseType(licenseType.getName());
            
            for (String reqName : requirements) {
                // Check if requirement already exists for this license type
                boolean exists = licenseRequirementService.getCoreLicenseRequirementByCoreLicenseType(licenseType)
                    .stream()
                    .anyMatch(req -> req.getName().equals(reqName));
                
                if (!exists) {
                    CoreLicenseRequirement requirement = new CoreLicenseRequirement();
                    requirement.setName(reqName);
                    requirement.setDescription("Required for " + licenseType.getName());
                    requirement.setCoreLicenseType(licenseType);
                    requirement.setDateCreated(new Timestamp(new Date().getTime()));
                    licenseRequirementService.addCoreLicenseRequirement(requirement);
                    seededCount++;
                }
            }
        }

        return ResponseEntity.ok(Map.of(
            "message", "License requirements seeded successfully",
            "seededCount", seededCount
        ));
    }

    private String[] getRequirementsForLicenseType(String licenseTypeName) {
        switch (licenseTypeName) {
            case "Surface Water Permit":
                return new String[]{
                    "Completed Application Form A1",
                    "Proof of Land Ownership",
                    "Technical Specifications",
                    "Environmental Impact Assessment",
                    "Hydrological Study Report",
                    "Water Use Plan",
                    "Application Fee Payment"
                };
            case "Groundwater Permit":
                return new String[]{
                    "Completed Application Form B1",
                    "Borehole Construction Details",
                    "Hydrogeological Report",
                    "Water Quality Test Results",
                    "Proof of Land Access",
                    "Application Fee Payment"
                };
            case "Effluent Discharge Permit":
                return new String[]{
                    "Completed Application Form H",
                    "Effluent Discharge Management Plan",
                    "Environmental Impact Assessment",
                    "Water Quality Treatment Plan",
                    "Monitoring Programme",
                    "Application Fee Payment"
                };
            case "Borehole Drilling Permit":
                return new String[]{
                    "Completed Application Form F1",
                    "Company Registration Certificate",
                    "Equipment List and Specifications",
                    "Personnel Qualifications",
                    "Insurance Coverage",
                    "Application Fee Payment"
                };
            default:
                return new String[]{
                    "Completed Application Form",
                    "Supporting Documents",
                    "Application Fee Payment"
                };
        }
    }
}