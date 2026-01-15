package mw.nwra.ewaterpermit.requestSchema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EffluentDischargePermitRequest {

    // Initial application type question
    @NotBlank(message = "Application type is required")
    private String applicationType; // "New" or "Variation"

    // Client details
    @NotBlank(message = "Client name is required")
    private String clientName;

    @NotBlank(message = "Client address is required")
    private String clientAddress;

    @NotBlank(message = "District is required")
    private String district;

    // Land ownership details
    @NotBlank(message = "Landowner name is required")
    private String landownerName;

    @NotBlank(message = "Landowner address is required")
    private String landownerAddress;

    @NotBlank(message = "Land location is required")
    private String landLocation;

    @NotBlank(message = "Land district is required")
    private String landDistrict;

    @NotBlank(message = "Land area is required")
    private String landArea;

    // Plot number (either lease or freehold)
    private String leasePlotNo;
    private String freeholdPlotNo;

    // Discharge point details
    @NotBlank(message = "Discharge point easting is required")
    private String dischargePointEasting;

    @NotBlank(message = "Discharge point northing is required")
    private String dischargePointNorthing;

    @NotBlank(message = "Discharge point village is required")
    private String dischargePointVillage;

    @NotBlank(message = "Discharge point traditional authority is required")
    private String dischargePointTa;

    private String dischargePointHectarage;
    private String dischargePointOwnerFullname;
    private String dischargePointPlotNumber;

    // Treatment facility details
    private String treatmentFacilityEasting;
    private String treatmentFacilityNorthing;
    private String treatmentFacilityVillage;
    private String treatmentFacilityTa;
    private String treatmentFacilityHectarage;
    private String treatmentFacilityOwnerFullname;
    private String treatmentFacilityPlotNumber;

    // Discharge nature and means
    @NotBlank(message = "Discharge type is required")
    private String dischargeType;

    @NotBlank(message = "Discharge means is required")
    private String dischargeMeans;

    @NotNull(message = "Maximum daily quantity is required")
    private Integer maxDailyQuantity;

    private String highestDischargeRate;
    private String dischargePeriods;

    // Receiving water details
    @NotBlank(message = "Receiving water is required")
    private String receivingWater;

    private String watercourseName;

    // Technical details
    private Integer diameter;
    private Double depth;

    // Population data
    private Integer allYearPopulation;
    private Integer wetSeasonPopulation;
    private Integer drySeasonPopulation;

    // Permit details
    @NotNull(message = "Permit duration is required")
    private Double permitDuration;

    private String anticipatedStartDate;
    private String limitedPeriod;

    // Effluent details
    @NotBlank(message = "Effluent type is required")
    private String effluentType;

    @NotBlank(message = "Treatment method is required")
    private String treatmentMethod;

    private String dischargeFrequency;
    private String nearbyWaterUtilityBoard;

    // Default constructor
    public EffluentDischargePermitRequest() {
    }

    // Getters and Setters
    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getLandownerName() {
        return landownerName;
    }

    public void setLandownerName(String landownerName) {
        this.landownerName = landownerName;
    }

    public String getLandownerAddress() {
        return landownerAddress;
    }

    public void setLandownerAddress(String landownerAddress) {
        this.landownerAddress = landownerAddress;
    }

    public String getLandLocation() {
        return landLocation;
    }

    public void setLandLocation(String landLocation) {
        this.landLocation = landLocation;
    }

    public String getLandDistrict() {
        return landDistrict;
    }

    public void setLandDistrict(String landDistrict) {
        this.landDistrict = landDistrict;
    }

    public String getLandArea() {
        return landArea;
    }

    public void setLandArea(String landArea) {
        this.landArea = landArea;
    }

    public String getLeasePlotNo() {
        return leasePlotNo;
    }

    public void setLeasePlotNo(String leasePlotNo) {
        this.leasePlotNo = leasePlotNo;
    }

    public String getFreeholdPlotNo() {
        return freeholdPlotNo;
    }

    public void setFreeholdPlotNo(String freeholdPlotNo) {
        this.freeholdPlotNo = freeholdPlotNo;
    }

    public String getDischargePointEasting() {
        return dischargePointEasting;
    }

    public void setDischargePointEasting(String dischargePointEasting) {
        this.dischargePointEasting = dischargePointEasting;
    }

    public String getDischargePointNorthing() {
        return dischargePointNorthing;
    }

    public void setDischargePointNorthing(String dischargePointNorthing) {
        this.dischargePointNorthing = dischargePointNorthing;
    }

    public String getDischargePointVillage() {
        return dischargePointVillage;
    }

    public void setDischargePointVillage(String dischargePointVillage) {
        this.dischargePointVillage = dischargePointVillage;
    }

    public String getDischargePointTa() {
        return dischargePointTa;
    }

    public void setDischargePointTa(String dischargePointTa) {
        this.dischargePointTa = dischargePointTa;
    }

    public String getDischargePointHectarage() {
        return dischargePointHectarage;
    }

    public void setDischargePointHectarage(String dischargePointHectarage) {
        this.dischargePointHectarage = dischargePointHectarage;
    }

    public String getDischargePointOwnerFullname() {
        return dischargePointOwnerFullname;
    }

    public void setDischargePointOwnerFullname(String dischargePointOwnerFullname) {
        this.dischargePointOwnerFullname = dischargePointOwnerFullname;
    }

    public String getDischargePointPlotNumber() {
        return dischargePointPlotNumber;
    }

    public void setDischargePointPlotNumber(String dischargePointPlotNumber) {
        this.dischargePointPlotNumber = dischargePointPlotNumber;
    }

    public String getTreatmentFacilityEasting() {
        return treatmentFacilityEasting;
    }

    public void setTreatmentFacilityEasting(String treatmentFacilityEasting) {
        this.treatmentFacilityEasting = treatmentFacilityEasting;
    }

    public String getTreatmentFacilityNorthing() {
        return treatmentFacilityNorthing;
    }

    public void setTreatmentFacilityNorthing(String treatmentFacilityNorthing) {
        this.treatmentFacilityNorthing = treatmentFacilityNorthing;
    }

    public String getTreatmentFacilityVillage() {
        return treatmentFacilityVillage;
    }

    public void setTreatmentFacilityVillage(String treatmentFacilityVillage) {
        this.treatmentFacilityVillage = treatmentFacilityVillage;
    }

    public String getTreatmentFacilityTa() {
        return treatmentFacilityTa;
    }

    public void setTreatmentFacilityTa(String treatmentFacilityTa) {
        this.treatmentFacilityTa = treatmentFacilityTa;
    }

    public String getTreatmentFacilityHectarage() {
        return treatmentFacilityHectarage;
    }

    public void setTreatmentFacilityHectarage(String treatmentFacilityHectarage) {
        this.treatmentFacilityHectarage = treatmentFacilityHectarage;
    }

    public String getTreatmentFacilityOwnerFullname() {
        return treatmentFacilityOwnerFullname;
    }

    public void setTreatmentFacilityOwnerFullname(String treatmentFacilityOwnerFullname) {
        this.treatmentFacilityOwnerFullname = treatmentFacilityOwnerFullname;
    }

    public String getTreatmentFacilityPlotNumber() {
        return treatmentFacilityPlotNumber;
    }

    public void setTreatmentFacilityPlotNumber(String treatmentFacilityPlotNumber) {
        this.treatmentFacilityPlotNumber = treatmentFacilityPlotNumber;
    }

    public String getDischargeType() {
        return dischargeType;
    }

    public void setDischargeType(String dischargeType) {
        this.dischargeType = dischargeType;
    }

    public String getDischargeMeans() {
        return dischargeMeans;
    }

    public void setDischargeMeans(String dischargeMeans) {
        this.dischargeMeans = dischargeMeans;
    }

    public Integer getMaxDailyQuantity() {
        return maxDailyQuantity;
    }

    public void setMaxDailyQuantity(Integer maxDailyQuantity) {
        this.maxDailyQuantity = maxDailyQuantity;
    }

    public String getHighestDischargeRate() {
        return highestDischargeRate;
    }

    public void setHighestDischargeRate(String highestDischargeRate) {
        this.highestDischargeRate = highestDischargeRate;
    }

    public String getDischargePeriods() {
        return dischargePeriods;
    }

    public void setDischargePeriods(String dischargePeriods) {
        this.dischargePeriods = dischargePeriods;
    }

    public String getReceivingWater() {
        return receivingWater;
    }

    public void setReceivingWater(String receivingWater) {
        this.receivingWater = receivingWater;
    }

    public String getWatercourseName() {
        return watercourseName;
    }

    public void setWatercourseName(String watercourseName) {
        this.watercourseName = watercourseName;
    }

    public Integer getDiameter() {
        return diameter;
    }

    public void setDiameter(Integer diameter) {
        this.diameter = diameter;
    }

    public Double getDepth() {
        return depth;
    }

    public void setDepth(Double depth) {
        this.depth = depth;
    }

    public Integer getAllYearPopulation() {
        return allYearPopulation;
    }

    public void setAllYearPopulation(Integer allYearPopulation) {
        this.allYearPopulation = allYearPopulation;
    }

    public Integer getWetSeasonPopulation() {
        return wetSeasonPopulation;
    }

    public void setWetSeasonPopulation(Integer wetSeasonPopulation) {
        this.wetSeasonPopulation = wetSeasonPopulation;
    }

    public Integer getDrySeasonPopulation() {
        return drySeasonPopulation;
    }

    public void setDrySeasonPopulation(Integer drySeasonPopulation) {
        this.drySeasonPopulation = drySeasonPopulation;
    }

    public Double getPermitDuration() {
        return permitDuration;
    }

    public void setPermitDuration(Double permitDuration) {
        this.permitDuration = permitDuration;
    }

    public String getAnticipatedStartDate() {
        return anticipatedStartDate;
    }

    public void setAnticipatedStartDate(String anticipatedStartDate) {
        this.anticipatedStartDate = anticipatedStartDate;
    }

    public String getLimitedPeriod() {
        return limitedPeriod;
    }

    public void setLimitedPeriod(String limitedPeriod) {
        this.limitedPeriod = limitedPeriod;
    }

    public String getEffluentType() {
        return effluentType;
    }

    public void setEffluentType(String effluentType) {
        this.effluentType = effluentType;
    }

    public String getTreatmentMethod() {
        return treatmentMethod;
    }

    public void setTreatmentMethod(String treatmentMethod) {
        this.treatmentMethod = treatmentMethod;
    }

    public String getDischargeFrequency() {
        return dischargeFrequency;
    }

    public void setDischargeFrequency(String dischargeFrequency) {
        this.dischargeFrequency = dischargeFrequency;
    }

    public String getNearbyWaterUtilityBoard() {
        return nearbyWaterUtilityBoard;
    }

    public void setNearbyWaterUtilityBoard(String nearbyWaterUtilityBoard) {
        this.nearbyWaterUtilityBoard = nearbyWaterUtilityBoard;
    }
}