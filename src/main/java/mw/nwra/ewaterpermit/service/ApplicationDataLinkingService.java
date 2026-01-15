package mw.nwra.ewaterpermit.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.CoreLicenseWaterUse;
import mw.nwra.ewaterpermit.model.CoreWaterResourceUnit;
import mw.nwra.ewaterpermit.model.CoreWaterUse;
import mw.nwra.ewaterpermit.repository.CoreWaterResourceUnitRepository;
import mw.nwra.ewaterpermit.repository.CoreWaterUseRepository;
import mw.nwra.ewaterpermit.repository.CoreLicenseWaterUseRepository;

@Service
public class ApplicationDataLinkingService {

    @Autowired
    private CoreWaterResourceUnitRepository waterResourceUnitRepository;

    @Autowired
    private CoreWaterUseRepository waterUseRepository;

    @Autowired
    private CoreLicenseWaterUseRepository licenseWaterUseRepository;

    @Autowired
    private mw.nwra.ewaterpermit.service.CoreLicenseApplicationService licenseApplicationService;

    private static final Pattern UTM_PATTERN = Pattern.compile(
        "^\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*\\(Zone\\s+(\\d+)([NS]?)\\)\\s*$"
    );
    
    private static final Pattern LATLONG_PATTERN = Pattern.compile(
        "^\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*$"
    );
    
    private static final double EARTH_RADIUS = 6378137.0;
    private static final double FLATTENING = 1.0 / 298.257223563;
    private static final double E2 = 2 * FLATTENING - FLATTENING * FLATTENING;
    private static final double K0 = 0.9996;
    
    private static class Coordinate {
        public final double longitude;
        public final double latitude;
        
        public Coordinate(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }
    
    public void linkWaterResourceUnit(CoreLicenseApplication application, Map<String, Object> request) {
        System.out.println("[WRU LINKING] Starting Water Resource Unit linking for application: " + application.getId());
        
        try {
            Object gpsObj = request.get("gpsCoordinates");
            Object formatObj = request.get("coordinateFormat");
            
            // Check for surface water coordinates field
            if (gpsObj == null) {
                gpsObj = request.get("waterSourceCoordinates");
                System.out.println("[WRU LINKING] Using waterSourceCoordinates field");
            }
            
            // Check for effluent discharge coordinates field
            if (gpsObj == null) {
                gpsObj = request.get("dischargePointCoordinates");
                System.out.println("[WRU LINKING] Using dischargePointCoordinates field");
            }
            
            System.out.println("[WRU LINKING] GPS Coordinates: " + gpsObj);
            System.out.println("[WRU LINKING] Coordinate Format: " + formatObj);
            
            if (gpsObj == null) {
                System.out.println("[WRU LINKING] No GPS coordinates provided, using fallback");
                List<CoreWaterResourceUnit> units = waterResourceUnitRepository.findAll();
                System.out.println("[WRU LINKING] Found " + units.size() + " water resource units in database");
                
                if (!units.isEmpty()) {
                    CoreWaterResourceUnit firstUnit = units.get(0);
                    application.setSourceWru(firstUnit);
                    application.setDestWru(firstUnit);
                    
                    // Save the application to persist WRU changes
                    licenseApplicationService.editCoreLicenseApplication(application);
                    System.out.println("[WRU LINKING] Assigned first available unit: " + firstUnit.getId() + " and saved to database");
                } else {
                    System.out.println("[WRU LINKING] WARNING: No water resource units found in database");
                }
                return;
            }
            
            String gpsCoordinates = gpsObj.toString().trim();
            String coordinateFormat = formatObj != null ? formatObj.toString() : "LATLONG";
            
            System.out.println("[WRU LINKING] Parsing coordinates: " + gpsCoordinates + " (Format: " + coordinateFormat + ")");
            
            Coordinate targetPoint = parseCoordinates(gpsCoordinates, coordinateFormat);
            System.out.println("[WRU LINKING] Parsed coordinates - Lat: " + targetPoint.latitude + ", Lng: " + targetPoint.longitude);
            
            List<CoreWaterResourceUnit> units = waterResourceUnitRepository.findAll();
            System.out.println("[WRU LINKING] Searching through " + units.size() + " water resource units for spatial match");
            
            CoreWaterResourceUnit matchingUnit = findContainingWRU(targetPoint, units);
            
            if (matchingUnit != null) {
                System.out.println("[WRU LINKING] Before setting - Source WRU: " + application.getSourceWru());
                System.out.println("[WRU LINKING] Before setting - Dest WRU: " + application.getDestWru());
                
                application.setSourceWru(matchingUnit);
                application.setDestWru(matchingUnit);
                
                System.out.println("[WRU LINKING] After setting - Source WRU: " + application.getSourceWru().getId());
                System.out.println("[WRU LINKING] After setting - Dest WRU: " + application.getDestWru().getId());
                
                // Save the application to persist WRU changes
                licenseApplicationService.editCoreLicenseApplication(application);
                System.out.println("[WRU LINKING] SUCCESS: Found spatial match with unit: " + matchingUnit.getId() + " and saved to database");
            } else if (!units.isEmpty()) {
                CoreWaterResourceUnit firstUnit = units.get(0);
                
                System.out.println("[WRU LINKING] Before fallback - Source WRU: " + application.getSourceWru());
                System.out.println("[WRU LINKING] Before fallback - Dest WRU: " + application.getDestWru());
                
                application.setSourceWru(firstUnit);
                application.setDestWru(firstUnit);
                
                System.out.println("[WRU LINKING] After fallback - Source WRU: " + application.getSourceWru().getId());
                System.out.println("[WRU LINKING] After fallback - Dest WRU: " + application.getDestWru().getId());
                
                // Save the application to persist WRU changes
                licenseApplicationService.editCoreLicenseApplication(application);
                System.out.println("[WRU LINKING] No spatial match found, using fallback unit: " + firstUnit.getId() + " and saved to database");
            } else {
                System.out.println("[WRU LINKING] ERROR: No water resource units available");
            }
            
        } catch (Exception e) {
            System.out.println("[WRU LINKING] ERROR during linking: " + e.getMessage());
            e.printStackTrace();
            
            try {
                List<CoreWaterResourceUnit> units = waterResourceUnitRepository.findAll();
                if (!units.isEmpty()) {
                    CoreWaterResourceUnit firstUnit = units.get(0);
                    application.setSourceWru(firstUnit);
                    application.setDestWru(firstUnit);
                    
                    // Save the application to persist WRU changes
                    licenseApplicationService.editCoreLicenseApplication(application);
                    System.out.println("[WRU LINKING] Fallback successful, assigned unit: " + firstUnit.getId() + " and saved to database");
                }
            } catch (Exception fallbackError) {
                System.out.println("[WRU LINKING] CRITICAL ERROR: Fallback also failed - " + fallbackError.getMessage());
            }
        }
        
        System.out.println("[WRU LINKING] Completed WRU linking process");
    }
    
    private Coordinate parseCoordinates(String coordinates, String format) {
        if ("UTM".equalsIgnoreCase(format)) {
            Matcher matcher = UTM_PATTERN.matcher(coordinates);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid UTM coordinate format");
            }
            
            double easting = Double.parseDouble(matcher.group(1));
            double northing = Double.parseDouble(matcher.group(2));
            int zone = Integer.parseInt(matcher.group(3));
            String hemisphere = matcher.group(4);
            
            boolean isNorthern = hemisphere == null || hemisphere.isEmpty() || "N".equalsIgnoreCase(hemisphere);
            
            if (zone != 36) {
                throw new IllegalArgumentException("Only UTM Zone 36 is supported");
            }
            
            return convertUtmToLatLon(easting, northing, zone, isNorthern);
        } else {
            Matcher matcher = LATLONG_PATTERN.matcher(coordinates);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid Lat/Long coordinate format");
            }
            
            double latitude = Double.parseDouble(matcher.group(1));
            double longitude = Double.parseDouble(matcher.group(2));
            
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Invalid coordinate values");
            }
            
            return new Coordinate(longitude, latitude);
        }
    }
    
    private Coordinate convertUtmToLatLon(double easting, double northing, int zone, boolean isNorthern) {
        double falseEasting = 500000.0;
        double falseNorthing = isNorthern ? 0.0 : 10000000.0;
        
        double centralMeridian = Math.toRadians((zone - 1) * 6 - 180 + 3);
        
        double x = easting - falseEasting;
        double y = northing - falseNorthing;
        
        double M = y / K0;
        double mu = M / (EARTH_RADIUS * (1 - E2/4 - 3*E2*E2/64 - 5*E2*E2*E2/256));
        
        double e1 = (1 - Math.sqrt(1 - E2)) / (1 + Math.sqrt(1 - E2));
        
        double J1 = (3 * e1 / 2 - 27 * e1*e1*e1 / 32);
        double J2 = (21 * e1*e1 / 16 - 55 * e1*e1*e1*e1 / 32);
        double J3 = (151 * e1*e1*e1 / 96);
        double J4 = (1097 * e1*e1*e1*e1 / 512);
        
        double fp = mu + J1 * Math.sin(2 * mu) + J2 * Math.sin(4 * mu) + J3 * Math.sin(6 * mu) + J4 * Math.sin(8 * mu);
        
        double e2 = E2;
        double C1 = e2 * Math.cos(fp) * Math.cos(fp);
        double T1 = Math.tan(fp) * Math.tan(fp);
        double R1 = EARTH_RADIUS * (1 - e2) / Math.pow(1 - e2 * Math.sin(fp) * Math.sin(fp), 1.5);
        double N1 = EARTH_RADIUS / Math.sqrt(1 - e2 * Math.sin(fp) * Math.sin(fp));
        double D = x / (N1 * K0);
        
        double Q1 = N1 * Math.tan(fp) / R1;
        double Q2 = (D * D / 2);
        double Q3 = (5 + 3 * T1 + 10 * C1 - 4 * C1 * C1 - 9 * e2) * D * D * D * D / 24;
        double Q4 = (61 + 90 * T1 + 298 * C1 + 45 * T1 * T1 - 1.6 * e2 - 37 * e2 * C1) * D * D * D * D * D * D / 720;
        
        double lat = fp - Q1 * (Q2 - Q3 + Q4);
        
        double Q6 = (1 + 2 * T1 + C1) * D * D * D / 6;
        double Q7 = (5 - 2 * C1 + 28 * T1 - 3 * C1 * C1 + 8 * e2 + 24 * T1 * T1) * D * D * D * D * D / 120;
        
        double lon = centralMeridian + (D - Q6 + Q7) / Math.cos(fp);
        
        return new Coordinate(Math.toDegrees(lon), Math.toDegrees(lat));
    }
    
    private CoreWaterResourceUnit findContainingWRU(Coordinate point, List<CoreWaterResourceUnit> units) {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("[SPATIAL MATCH] Starting spatial matching for point: " + point.latitude + ", " + point.longitude);
        
        for (int i = 0; i < units.size(); i++) {
            CoreWaterResourceUnit unit = units.get(i);
            System.out.println("[SPATIAL MATCH] Checking unit " + (i+1) + "/" + units.size() + ": " + unit.getId());
            
            try {
                if (unit.getGeoGeometry() != null) {
                    String geoJson = new String(unit.getGeoGeometry(), StandardCharsets.UTF_8);
                    System.out.println("[SPATIAL MATCH] GeoJSON length: " + geoJson.length() + " characters");
                    System.out.println("[SPATIAL MATCH] GeoJSON preview: " + geoJson.substring(0, Math.min(100, geoJson.length())) + "...");
                    
                    JsonNode geoNode = mapper.readTree(geoJson);
                    
                    if (isPointInGeoJsonPolygon(point, geoNode)) {
                        System.out.println("[SPATIAL MATCH] MATCH FOUND! Unit " + unit.getId() + " contains the point");
                        return unit;
                    } else {
                        System.out.println("[SPATIAL MATCH] Point not in unit " + unit.getId());
                    }
                } else {
                    System.out.println("[SPATIAL MATCH] Unit " + unit.getId() + " has no geometry data");
                }
            } catch (Exception e) {
                System.out.println("[SPATIAL MATCH] Error processing unit " + unit.getId() + ": " + e.getMessage());
            }
        }
        
        System.out.println("[SPATIAL MATCH] No spatial match found in any unit");
        return null;
    }
    
    private boolean isPointInGeoJsonPolygon(Coordinate point, JsonNode geoNode) {
        try {
            JsonNode coordinates = geoNode.get("coordinates");
            if (coordinates == null || !coordinates.isArray()) {
                return false;
            }
            
            for (JsonNode polygon : coordinates) {
                if (polygon.isArray() && !polygon.isEmpty()) {
                    JsonNode exteriorRing = polygon.get(0);
                    if (exteriorRing.isArray()) {
                        double[][] polygonCoords = new double[exteriorRing.size()][2];
                        for (int i = 0; i < exteriorRing.size(); i++) {
                            JsonNode coord = exteriorRing.get(i);
                            if (coord.isArray() && coord.size() >= 2) {
                                polygonCoords[i][0] = coord.get(0).asDouble();
                                polygonCoords[i][1] = coord.get(1).asDouble();
                            }
                        }
                        
                        if (isPointInPolygon(point.longitude, point.latitude, polygonCoords)) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isPointInPolygon(double x, double y, double[][] polygon) {
        if (polygon.length < 3) {
            return false;
        }
        
        boolean inside = false;
        int j = polygon.length - 1;
        
        for (int i = 0; i < polygon.length; i++) {
            double xi = polygon[i][0], yi = polygon[i][1];
            double xj = polygon[j][0], yj = polygon[j][1];
            
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
            j = i;
        }
        
        return inside;
    }

    public void linkWaterUse(CoreLicenseApplication application, Map<String, Object> request) {
        System.out.println("[WATER USE LINKING] Starting water use linking for application: " + application.getId());
        
        Object waterUsesObj = request.get("waterUses");
        System.out.println("[WATER USE LINKING] Water uses data: " + waterUsesObj);
        
        if (waterUsesObj != null) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> waterUses = (List<Map<String, Object>>) waterUsesObj;
                System.out.println("[WATER USE LINKING] Found " + waterUses.size() + " water use entries to process");
                
                for (int i = 0; i < waterUses.size(); i++) {
                    Map<String, Object> waterUseData = waterUses.get(i);
                    System.out.println("[WATER USE LINKING] Processing water use entry " + (i+1) + "/" + waterUses.size() + ": " + waterUseData);
                    
                    // Handle 'name', 'use', and 'useType' fields
                    Object nameObj = waterUseData.get("name");
                    Object useObj = waterUseData.get("use");
                    Object useTypeObj = waterUseData.get("useType");
                    Object quantityObj = waterUseData.get("quantity");
                    
                    System.out.println("[WATER USE LINKING] Name: " + nameObj + ", Use: " + useObj + ", UseType: " + useTypeObj + ", Quantity: " + quantityObj);
                    
                    // Get the water use names (could be single string or array)
                    Object waterUseNames = nameObj != null ? nameObj : (useObj != null ? useObj : useTypeObj);
                    
                    if (waterUseNames != null && quantityObj != null) {
                        String quantityStr = quantityObj.toString();
                        double totalQuantity = 0.0;
                        
                        try {
                            totalQuantity = Double.parseDouble(quantityStr);
                        } catch (NumberFormatException e) {
                            System.out.println("[WATER USE LINKING] Invalid quantity format: " + quantityStr);
                            continue;
                        }
                        
                        // Handle array of water use names
                        if (waterUseNames instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> useNamesList = (List<String>) waterUseNames;
                            System.out.println("[WATER USE LINKING] Processing " + useNamesList.size() + " water use types");
                            
                            // Distribute quantity equally among all uses
                            double quantityPerUse = totalQuantity / useNamesList.size();
                            
                            for (String waterUseName : useNamesList) {
                                processWaterUse(application, waterUseName.trim(), quantityPerUse);
                            }
                        } else {
                            // Handle single water use name
                            String waterUseName = waterUseNames.toString().trim();
                            processWaterUse(application, waterUseName, totalQuantity);
                        }
                    } else {
                        System.out.println("[WATER USE LINKING] Skipping water use - missing name/use or quantity");
                    }
                }
            } catch (Exception e) {
                System.out.println("[WATER USE LINKING] ERROR during water use linking: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[WATER USE LINKING] No water uses data provided in request");
        }
        
        System.out.println("[WATER USE LINKING] Completed water use linking process");
    }
    
    private void processWaterUse(CoreLicenseApplication application, String waterUseName, double quantity) {
        System.out.println("[WATER USE LINKING] Looking up water use by name: " + waterUseName);
        CoreWaterUse waterUse = waterUseRepository.findByName(waterUseName);
        
        if (waterUse != null) {
            System.out.println("[WATER USE LINKING] Found water use in database: " + waterUse.getId());
            
            CoreLicenseWaterUse licenseWaterUse = new CoreLicenseWaterUse();
            licenseWaterUse.setCoreLicenseApplication(application);
            licenseWaterUse.setCoreWaterUse(waterUse);
            licenseWaterUse.setAmountPerDayM3(quantity);
            
            CoreLicenseWaterUse saved = licenseWaterUseRepository.save(licenseWaterUse);
            System.out.println("[WATER USE LINKING] SUCCESS: Saved " + waterUseName + " with " + quantity + " m³/day, ID: " + saved.getId());
        } else {
            System.out.println("[WATER USE LINKING] WARNING: Water use '" + waterUseName + "' not found in database");
        }
    }
}