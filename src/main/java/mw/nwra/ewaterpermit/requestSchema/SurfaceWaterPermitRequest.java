package mw.nwra.ewaterpermit.requestSchema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SurfaceWaterPermitRequest {

    @NotBlank(message = "Source easting is required")
    private String sourceEasting;

    @NotBlank(message = "Source northing is required")
    private String sourceNorthing;

    @NotBlank(message = "Source village is required")
    private String sourceVillage;

    @NotBlank(message = "Source traditional authority is required")
    private String sourceTa;

    @NotBlank(message = "Source plot number is required")
    private String sourcePlotNumber;

    @NotBlank(message = "Source owner full name is required")
    private String sourceOwnerFullname;

    @NotBlank(message = "Source hectarage is required")
    private String sourceHectarage;

    private String destEasting;
    private String destNorthing;
    private String destVillage;
    private String destTa;
    private String destPlotNumber;
    private String destOwnerFullname;
    private String destHectarage;

    @NotNull(message = "Permit duration is required")
    private Double permitDuration;

    private String nearbyWaterUtilityBoard;
    private String altWaterSource;
    private String altOtherWater;
    private Integer existingBoreholeCount;

    @NotBlank(message = "Water source ID is required")
    private String waterSourceId;

    @NotBlank(message = "Water resource area ID is required")
    private String waterResourceAreaId;

    @NotBlank(message = "Water use purpose is required")
    private String waterUsePurpose;

    @NotNull(message = "Estimated water volume is required")
    private Double estimatedWaterVolume;

    public SurfaceWaterPermitRequest() {
    }

    public String getSourceEasting() {
        return sourceEasting;
    }

    public void setSourceEasting(String sourceEasting) {
        this.sourceEasting = sourceEasting;
    }

    public String getSourceNorthing() {
        return sourceNorthing;
    }

    public void setSourceNorthing(String sourceNorthing) {
        this.sourceNorthing = sourceNorthing;
    }

    public String getSourceVillage() {
        return sourceVillage;
    }

    public void setSourceVillage(String sourceVillage) {
        this.sourceVillage = sourceVillage;
    }

    public String getSourceTa() {
        return sourceTa;
    }

    public void setSourceTa(String sourceTa) {
        this.sourceTa = sourceTa;
    }

    public String getSourcePlotNumber() {
        return sourcePlotNumber;
    }

    public void setSourcePlotNumber(String sourcePlotNumber) {
        this.sourcePlotNumber = sourcePlotNumber;
    }

    public String getSourceOwnerFullname() {
        return sourceOwnerFullname;
    }

    public void setSourceOwnerFullname(String sourceOwnerFullname) {
        this.sourceOwnerFullname = sourceOwnerFullname;
    }

    public String getSourceHectarage() {
        return sourceHectarage;
    }

    public void setSourceHectarage(String sourceHectarage) {
        this.sourceHectarage = sourceHectarage;
    }

    public String getDestEasting() {
        return destEasting;
    }

    public void setDestEasting(String destEasting) {
        this.destEasting = destEasting;
    }

    public String getDestNorthing() {
        return destNorthing;
    }

    public void setDestNorthing(String destNorthing) {
        this.destNorthing = destNorthing;
    }

    public String getDestVillage() {
        return destVillage;
    }

    public void setDestVillage(String destVillage) {
        this.destVillage = destVillage;
    }

    public String getDestTa() {
        return destTa;
    }

    public void setDestTa(String destTa) {
        this.destTa = destTa;
    }

    public String getDestPlotNumber() {
        return destPlotNumber;
    }

    public void setDestPlotNumber(String destPlotNumber) {
        this.destPlotNumber = destPlotNumber;
    }

    public String getDestOwnerFullname() {
        return destOwnerFullname;
    }

    public void setDestOwnerFullname(String destOwnerFullname) {
        this.destOwnerFullname = destOwnerFullname;
    }

    public String getDestHectarage() {
        return destHectarage;
    }

    public void setDestHectarage(String destHectarage) {
        this.destHectarage = destHectarage;
    }

    public Double getPermitDuration() {
        return permitDuration;
    }

    public void setPermitDuration(Double permitDuration) {
        this.permitDuration = permitDuration;
    }

    public String getNearbyWaterUtilityBoard() {
        return nearbyWaterUtilityBoard;
    }

    public void setNearbyWaterUtilityBoard(String nearbyWaterUtilityBoard) {
        this.nearbyWaterUtilityBoard = nearbyWaterUtilityBoard;
    }

    public String getAltWaterSource() {
        return altWaterSource;
    }

    public void setAltWaterSource(String altWaterSource) {
        this.altWaterSource = altWaterSource;
    }

    public String getAltOtherWater() {
        return altOtherWater;
    }

    public void setAltOtherWater(String altOtherWater) {
        this.altOtherWater = altOtherWater;
    }

    public Integer getExistingBoreholeCount() {
        return existingBoreholeCount;
    }

    public void setExistingBoreholeCount(Integer existingBoreholeCount) {
        this.existingBoreholeCount = existingBoreholeCount;
    }

    public String getWaterSourceId() {
        return waterSourceId;
    }

    public void setWaterSourceId(String waterSourceId) {
        this.waterSourceId = waterSourceId;
    }

    public String getWaterResourceAreaId() {
        return waterResourceAreaId;
    }

    public void setWaterResourceAreaId(String waterResourceAreaId) {
        this.waterResourceAreaId = waterResourceAreaId;
    }

    public String getWaterUsePurpose() {
        return waterUsePurpose;
    }

    public void setWaterUsePurpose(String waterUsePurpose) {
        this.waterUsePurpose = waterUsePurpose;
    }

    public Double getEstimatedWaterVolume() {
        return estimatedWaterVolume;
    }

    public void setEstimatedWaterVolume(Double estimatedWaterVolume) {
        this.estimatedWaterVolume = estimatedWaterVolume;
    }
}