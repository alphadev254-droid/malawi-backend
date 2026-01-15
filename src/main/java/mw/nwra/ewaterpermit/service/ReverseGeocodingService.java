package mw.nwra.ewaterpermit.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mw.nwra.ewaterpermit.model.LocationInfo;

@Service
public class ReverseGeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ReverseGeocodingService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public LocationInfo getLocationDetails(double latitude, double longitude) {
        System.out.println("[REVERSE GEOCODING] Getting location for: " + latitude + ", " + longitude);
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String url = String.format("%s?format=json&lat=%f&lon=%f&addressdetails=1", 
                    NOMINATIM_URL, latitude, longitude);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "NWRA-WaterPermit/1.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    LocationInfo location = parseLocationResponse(response.body());
                    System.out.println("[REVERSE GEOCODING] Success: " + location.getAreaName());
                    return location;
                } else {
                    System.out.println("[REVERSE GEOCODING] HTTP error " + response.statusCode() + " on attempt " + attempt);
                }
                
            } catch (Exception e) {
                System.out.println("[REVERSE GEOCODING] Error on attempt " + attempt + ": " + e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        LocationInfo errorLocation = new LocationInfo();
        errorLocation.setErrorMessage("Failed to get location after " + MAX_RETRIES + " attempts");
        System.out.println("[REVERSE GEOCODING] Failed to get location for: " + latitude + ", " + longitude);
        return errorLocation;
    }

    private LocationInfo parseLocationResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode address = root.get("address");
            
            if (address == null) {
                LocationInfo errorLocation = new LocationInfo();
                errorLocation.setErrorMessage("No address data in response");
                return errorLocation;
            }

            LocationInfo location = new LocationInfo();
            location.setSuccess(true);
            location.setDisplayName(getStringValue(root, "display_name"));
            
            // Extract address components
            location.setCountry(getStringValue(address, "country"));
            location.setRegion(getStringValue(address, "state"));
            location.setDistrict(getStringValue(address, "state_district"));
            location.setCity(getStringValue(address, "city"));
            location.setTown(getStringValue(address, "town"));
            location.setVillage(getStringValue(address, "village"));
            location.setPostcode(getStringValue(address, "postcode"));
            
            // Fallback for district if state_district is null
            if (location.getDistrict() == null) {
                location.setDistrict(getStringValue(address, "county"));
            }
            
            return location;
            
        } catch (Exception e) {
            LocationInfo errorLocation = new LocationInfo();
            errorLocation.setErrorMessage("Error parsing response: " + e.getMessage());
            return errorLocation;
        }
    }

    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText() : null;
    }

    public LocationInfo getLocationFromGeometry(byte[] geoGeometry) {
        System.out.println("[REVERSE GEOCODING] Processing geometry data...");
        
        if (geoGeometry == null) {
            System.out.println("[REVERSE GEOCODING] ERROR: No geometry data provided");
            LocationInfo errorLocation = new LocationInfo();
            errorLocation.setErrorMessage("No geometry data provided");
            return errorLocation;
        }

        try {
            String geoJson = new String(geoGeometry, java.nio.charset.StandardCharsets.UTF_8);
            
            JsonNode geoNode = objectMapper.readTree(geoJson);
            
            // Extract centroid from polygon coordinates
            double[] centroid = calculateCentroid(geoNode);
            if (centroid != null) {
                return getLocationDetails(centroid[1], centroid[0]); // lat, lon
            } else {
                System.out.println("[REVERSE GEOCODING] ERROR: Could not calculate centroid from geometry");
            }
            
        } catch (Exception e) {
            System.out.println("[REVERSE GEOCODING] ERROR processing geometry: " + e.getMessage());
            e.printStackTrace();
        }
        
        LocationInfo errorLocation = new LocationInfo();
        errorLocation.setErrorMessage("Could not extract coordinates from geometry");
        return errorLocation;
    }

    private double[] calculateCentroid(JsonNode geoNode) {
        System.out.println("[REVERSE GEOCODING] Calculating centroid from geometry...");
        
        try {
            JsonNode coordinates = geoNode.get("coordinates");
            System.out.println("[REVERSE GEOCODING] Coordinates node exists: " + (coordinates != null));
            
            if (coordinates != null && coordinates.isArray() && coordinates.size() > 0) {
                System.out.println("[REVERSE GEOCODING] Coordinates array size: " + coordinates.size());
                
                JsonNode polygon = coordinates.get(0);
                if (polygon.isArray() && polygon.size() > 0) {
                    System.out.println("[REVERSE GEOCODING] Polygon rings: " + polygon.size());
                    
                    JsonNode ring = polygon.get(0);
                    if (ring.isArray()) {
                        System.out.println("[REVERSE GEOCODING] Ring points: " + ring.size());
                        
                        double sumLon = 0, sumLat = 0;
                        int count = 0;
                        
                        for (JsonNode coord : ring) {
                            if (coord.isArray() && coord.size() >= 2) {
                                double lon = coord.get(0).asDouble();
                                double lat = coord.get(1).asDouble();
                                sumLon += lon;
                                sumLat += lat;
                                count++;
                                
                                if (count <= 3) { // Log first few coordinates
                                    System.out.println("[REVERSE GEOCODING] Point " + count + ": [" + lon + ", " + lat + "]");
                                }
                            }
                        }
                        
                        if (count > 0) {
                            double centroidLon = sumLon / count;
                            double centroidLat = sumLat / count;
                            System.out.println("[REVERSE GEOCODING] Calculated centroid from " + count + " points: [" + centroidLon + ", " + centroidLat + "]");
                            return new double[]{centroidLon, centroidLat};
                        } else {
                            System.out.println("[REVERSE GEOCODING] ERROR: No valid coordinate points found");
                        }
                    } else {
                        System.out.println("[REVERSE GEOCODING] ERROR: Ring is not an array");
                    }
                } else {
                    System.out.println("[REVERSE GEOCODING] ERROR: Polygon is not an array or is empty");
                }
            } else {
                System.out.println("[REVERSE GEOCODING] ERROR: Coordinates is not an array or is empty");
            }
        } catch (Exception e) {
            System.out.println("[REVERSE GEOCODING] ERROR calculating centroid: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}