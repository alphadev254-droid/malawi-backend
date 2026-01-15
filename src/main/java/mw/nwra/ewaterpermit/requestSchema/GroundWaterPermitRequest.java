package mw.nwra.ewaterpermit.requestSchema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GroundWaterPermitRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Telephone is required")
    private String telephone;

    private String mobilePhone;
    private String email;

    @NotBlank(message = "Works landowner is required")
    private String worksLandowner;

    @NotBlank(message = "Water use landowner is required")
    private String waterUseLandowner;

    private String landownerAddress;

    @NotBlank(message = "Property regime is required")
    private String propertyRegime;

    private String plotNo;

    @NotBlank(message = "Works location is required")
    private String worksLocation;

    @NotBlank(message = "Water use location is required")
    private String waterUseLocation;

    @NotBlank(message = "GPS coordinates are required")
    private String gpsCoordinates;

    @NotBlank(message = "Village is required")
    private String village;

    @NotBlank(message = "Traditional authority is required")
    private String traditionalAuthority;

    @NotNull(message = "Land area is required")
    private Double landArea;

    @NotBlank(message = "Water source is required")
    private String waterSource;

    private String otherWaterSource;

    // Borehole details
    private String dateDrilled;
    private String driller;
    private Double diameter;
    private Double depth;
    private String linningCasings;
    private String testYield;

    // Water use details
    @NotNull(message = "Daily water quantity is required")
    private Double dailyWaterQuantity;

    @NotBlank(message = "Water use purpose is required")
    private String waterUsePurpose;

    // Construction details
    private String pumpType;
    private String otherPumpType;
    private String drivingMachine;
    private Double brakeHorsepower;
    private Double pumpElevation;
    private String pumpConnection;
    private Double suctionMainDiameter;
    private Double maxSuctionHeight;
    private Double waterLiftHeight;
    private Double deliveryPipeLength;
    private Double pumpingHours;
    private Double dailyPumpingQuantity;
    private String measurementMethod;

    // Other information
    private String alternativeSources;
    private String otherAlternativeSource;
    private String existingBoreholes;
    private Integer existingBoreholesCount;
    private String permitDuration;
    private String waterUtilityArea;
    private String waterUtilityName;

    // Declaration
    private String applicantSignature;
    private String applicantName;
    private String declarationDate;

    public GroundWaterPermitRequest() {
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWorksLandowner() {
        return worksLandowner;
    }

    public void setWorksLandowner(String worksLandowner) {
        this.worksLandowner = worksLandowner;
    }

    public String getWaterUseLandowner() {
        return waterUseLandowner;
    }

    public void setWaterUseLandowner(String waterUseLandowner) {
        this.waterUseLandowner = waterUseLandowner;
    }

    public String getLandownerAddress() {
        return landownerAddress;
    }

    public void setLandownerAddress(String landownerAddress) {
        this.landownerAddress = landownerAddress;
    }

    public String getPropertyRegime() {
        return propertyRegime;
    }

    public void setPropertyRegime(String propertyRegime) {
        this.propertyRegime = propertyRegime;
    }

    public String getPlotNo() {
        return plotNo;
    }

    public void setPlotNo(String plotNo) {
        this.plotNo = plotNo;
    }

    public String getWorksLocation() {
        return worksLocation;
    }

    public void setWorksLocation(String worksLocation) {
        this.worksLocation = worksLocation;
    }

    public String getWaterUseLocation() {
        return waterUseLocation;
    }

    public void setWaterUseLocation(String waterUseLocation) {
        this.waterUseLocation = waterUseLocation;
    }

    public String getGpsCoordinates() {
        return gpsCoordinates;
    }

    public void setGpsCoordinates(String gpsCoordinates) {
        this.gpsCoordinates = gpsCoordinates;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getTraditionalAuthority() {
        return traditionalAuthority;
    }

    public void setTraditionalAuthority(String traditionalAuthority) {
        this.traditionalAuthority = traditionalAuthority;
    }

    public Double getLandArea() {
        return landArea;
    }

    public void setLandArea(Double landArea) {
        this.landArea = landArea;
    }

    public String getWaterSource() {
        return waterSource;
    }

    public void setWaterSource(String waterSource) {
        this.waterSource = waterSource;
    }

    public String getOtherWaterSource() {
        return otherWaterSource;
    }

    public void setOtherWaterSource(String otherWaterSource) {
        this.otherWaterSource = otherWaterSource;
    }

    public String getDateDrilled() {
        return dateDrilled;
    }

    public void setDateDrilled(String dateDrilled) {
        this.dateDrilled = dateDrilled;
    }

    public String getDriller() {
        return driller;
    }

    public void setDriller(String driller) {
        this.driller = driller;
    }

    public Double getDiameter() {
        return diameter;
    }

    public void setDiameter(Double diameter) {
        this.diameter = diameter;
    }

    public Double getDepth() {
        return depth;
    }

    public void setDepth(Double depth) {
        this.depth = depth;
    }

    public String getLinningCasings() {
        return linningCasings;
    }

    public void setLinningCasings(String linningCasings) {
        this.linningCasings = linningCasings;
    }

    public String getTestYield() {
        return testYield;
    }

    public void setTestYield(String testYield) {
        this.testYield = testYield;
    }

    public Double getDailyWaterQuantity() {
        return dailyWaterQuantity;
    }

    public void setDailyWaterQuantity(Double dailyWaterQuantity) {
        this.dailyWaterQuantity = dailyWaterQuantity;
    }

    public String getWaterUsePurpose() {
        return waterUsePurpose;
    }

    public void setWaterUsePurpose(String waterUsePurpose) {
        this.waterUsePurpose = waterUsePurpose;
    }

    public String getPumpType() {
        return pumpType;
    }

    public void setPumpType(String pumpType) {
        this.pumpType = pumpType;
    }

    public String getOtherPumpType() {
        return otherPumpType;
    }

    public void setOtherPumpType(String otherPumpType) {
        this.otherPumpType = otherPumpType;
    }

    public String getDrivingMachine() {
        return drivingMachine;
    }

    public void setDrivingMachine(String drivingMachine) {
        this.drivingMachine = drivingMachine;
    }

    public Double getBrakeHorsepower() {
        return brakeHorsepower;
    }

    public void setBrakeHorsepower(Double brakeHorsepower) {
        this.brakeHorsepower = brakeHorsepower;
    }

    public Double getPumpElevation() {
        return pumpElevation;
    }

    public void setPumpElevation(Double pumpElevation) {
        this.pumpElevation = pumpElevation;
    }

    public String getPumpConnection() {
        return pumpConnection;
    }

    public void setPumpConnection(String pumpConnection) {
        this.pumpConnection = pumpConnection;
    }

    public Double getSuctionMainDiameter() {
        return suctionMainDiameter;
    }

    public void setSuctionMainDiameter(Double suctionMainDiameter) {
        this.suctionMainDiameter = suctionMainDiameter;
    }

    public Double getMaxSuctionHeight() {
        return maxSuctionHeight;
    }

    public void setMaxSuctionHeight(Double maxSuctionHeight) {
        this.maxSuctionHeight = maxSuctionHeight;
    }

    public Double getWaterLiftHeight() {
        return waterLiftHeight;
    }

    public void setWaterLiftHeight(Double waterLiftHeight) {
        this.waterLiftHeight = waterLiftHeight;
    }

    public Double getDeliveryPipeLength() {
        return deliveryPipeLength;
    }

    public void setDeliveryPipeLength(Double deliveryPipeLength) {
        this.deliveryPipeLength = deliveryPipeLength;
    }

    public Double getPumpingHours() {
        return pumpingHours;
    }

    public void setPumpingHours(Double pumpingHours) {
        this.pumpingHours = pumpingHours;
    }

    public Double getDailyPumpingQuantity() {
        return dailyPumpingQuantity;
    }

    public void setDailyPumpingQuantity(Double dailyPumpingQuantity) {
        this.dailyPumpingQuantity = dailyPumpingQuantity;
    }

    public String getMeasurementMethod() {
        return measurementMethod;
    }

    public void setMeasurementMethod(String measurementMethod) {
        this.measurementMethod = measurementMethod;
    }

    public String getAlternativeSources() {
        return alternativeSources;
    }

    public void setAlternativeSources(String alternativeSources) {
        this.alternativeSources = alternativeSources;
    }

    public String getOtherAlternativeSource() {
        return otherAlternativeSource;
    }

    public void setOtherAlternativeSource(String otherAlternativeSource) {
        this.otherAlternativeSource = otherAlternativeSource;
    }

    public String getExistingBoreholes() {
        return existingBoreholes;
    }

    public void setExistingBoreholes(String existingBoreholes) {
        this.existingBoreholes = existingBoreholes;
    }

    public Integer getExistingBoreholesCount() {
        return existingBoreholesCount;
    }

    public void setExistingBoreholesCount(Integer existingBoreholesCount) {
        this.existingBoreholesCount = existingBoreholesCount;
    }

    public String getPermitDuration() {
        return permitDuration;
    }

    public void setPermitDuration(String permitDuration) {
        this.permitDuration = permitDuration;
    }

    public String getWaterUtilityArea() {
        return waterUtilityArea;
    }

    public void setWaterUtilityArea(String waterUtilityArea) {
        this.waterUtilityArea = waterUtilityArea;
    }

    public String getWaterUtilityName() {
        return waterUtilityName;
    }

    public void setWaterUtilityName(String waterUtilityName) {
        this.waterUtilityName = waterUtilityName;
    }

    public String getApplicantSignature() {
        return applicantSignature;
    }

    public void setApplicantSignature(String applicantSignature) {
        this.applicantSignature = applicantSignature;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getDeclarationDate() {
        return declarationDate;
    }

    public void setDeclarationDate(String declarationDate) {
        this.declarationDate = declarationDate;
    }
}