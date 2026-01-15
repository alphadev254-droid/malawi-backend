package mw.nwra.ewaterpermit.model;

public class LocationInfo {
    private String country;
    private String region;
    private String district;
    private String city;
    private String town;
    private String village;
    private String postcode;
    private String displayName;
    private boolean success;
    private String errorMessage;

    public LocationInfo() {
        this.success = false;
    }

    public LocationInfo(String country, String region, String district, String city, String displayName) {
        this.country = country;
        this.region = region;
        this.district = district;
        this.city = city;
        this.displayName = displayName;
        this.success = true;
    }

    // Getters and Setters
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getAreaName() {
        if (district != null && !district.isEmpty()) {
            return district + " District";
        }
        if (city != null && !city.isEmpty()) {
            return city;
        }
        if (town != null && !town.isEmpty()) {
            return town;
        }
        if (region != null && !region.isEmpty()) {
            return region + " Region";
        }
        return "Unknown Area";
    }

    @Override
    public String toString() {
        return "LocationInfo{" +
                "country='" + country + '\'' +
                ", region='" + region + '\'' +
                ", district='" + district + '\'' +
                ", city='" + city + '\'' +
                ", displayName='" + displayName + '\'' +
                ", success=" + success +
                '}';
    }
}