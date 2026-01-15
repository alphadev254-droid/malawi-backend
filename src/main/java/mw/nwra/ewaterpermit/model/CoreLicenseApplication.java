package mw.nwra.ewaterpermit.model;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_license_application")
@NamedQuery(name = "CoreLicenseApplication.findAll", query = "SELECT c FROM CoreLicenseApplication c")
public class CoreLicenseApplication extends BaseEntity {

    // @Column(name = "date_submitted")
    // private Timestamp dateSubmitted;

    @Column(name = "dest_easting")
    private String destEasting;

    @Column(name = "dest_hactarage")
    private String destHectarage;

    @Column(name = "dest_northing")
    private String destNorthing;

    @Column(name = "dest_owner_fullname")
    private String destOwnerFullname;

    @Column(name = "dest_plot_number")
    private String destPlotNumber;

    @Column(name = "dest_ta")
    private String destTa;

    @Column(name = "dest_village")
    private String destVillage;

    @Column(name = "source_easting")
    private String sourceEasting;

    @Column(name = "source_hactarage")
    private String sourceHectarage;

    @Column(name = "source_northing")
    private String sourceNorthing;

    @Column(name = "source_owner_fullname")
    private String sourceOwnerFullname;

    @Column(name = "source_plot_number")
    private String sourcePlotNumber;

    @Column(name = "source_ta")
    private String sourceTa;

    @Column(name = "source_village")
    private String sourceVillage;

    @Column(name = "date_submitted")
    private Timestamp dateSubmitted;

    @Column(name = "existing_borehole_count")
    private Integer existingBoreholeCount;

    @Column(name = "permit_duration")
    private Double permitDuration;

    @Column(name = "nearby_water_utility_board")
    private String nearbyWaterUtilityBoard;

    @Column(name = "alt_water_source")
    private String altWaterSource;

    @Column(name = "alt_other_water")
    private String altOtherWater;

    @Column(name = "board_minutes", columnDefinition = "TEXT")
    private String boardMinutes;

    @Column(name = "board_approval_date", nullable = true)
    private Timestamp boardApprovalDate;

    @Column(name = "board_minutes_document")
    private String boardMinutesDocument;

    // Workflow referral fields for internal refer back functionality
    @Column(name = "referred_to_user_id")
    private String referredToUserId;

    @Column(name = "referred_from_user_id")
    private String referredFromUserId;

    @Column(name = "referral_reason", columnDefinition = "TEXT")
    private String referralReason;

    @Column(name = "last_handled_by_user_id")
    private String lastHandledByUserId;

    @Column(name = "referral_date")
    private Timestamp referralDate;

    // JSON fields for form data storage
    @Column(name = "client_info", columnDefinition = "JSON")
    private String clientInfo;

    @Column(name = "location_info", columnDefinition = "JSON")
    private String locationInfo;

    @Column(name = "application_metadata", columnDefinition = "JSON")
    private String applicationMetadata;

    @Column(name = "form_specific_data", columnDefinition = "JSON")
    private String formSpecificData;

    // Application type and renewal/variation/transfer fields
    @Column(name = "application_type")
    private String applicationType;

    @Column(name = "original_license_id")
    private String originalLicenseId;

    // Transfer fields
    @Column(name = "transfer_to_user_id")
    private String transferToUserId;

    @Column(name = "is_disabled")
    private Boolean isDisabled = false;

    // Emergency application fields
    @Column(name = "application_priority")
    private String applicationPriority = "NORMAL"; // NORMAL or EMERGENCY

    @Column(name = "emergency_justification_file", length = 500)
    private String emergencyJustificationFile;

    @Column(name = "emergency_reason", columnDefinition = "TEXT")
    private String emergencyReason;

    @Column(name = "emergency_submitted_date")
    private Timestamp emergencySubmittedDate;

    // License fee field (set by manager based on assessment, overrides license type default)
    @Column(name = "license_fee")
    private Double licenseFee;

    @Column(name = "license_fee_set_by_user_id")
    private String licenseFeeSetByUserId;

    @Column(name = "license_fee_set_date")
    private Timestamp licenseFeeSetDate;

    // Easement-specific fields
    @Column(name = "burdened_land_description", columnDefinition = "TEXT")
    private String burdenedLandDescription;

    @Column(name = "benefitted_land_description", columnDefinition = "TEXT")
    private String benefittedLandDescription;

    @Column(name = "permit_conditions", columnDefinition = "TEXT")
    private String permitConditions;

    @Column(name = "nature_of_burden", columnDefinition = "TEXT")
    private String natureOfBurden;

    // Owner tracking field (required)
    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    // User account ID field (required for database)
    @Column(name = "user_account_id", nullable = false)
    private String userAccountId;

    // bi-directional many-to-one association to CoreLandRegime
    @ManyToOne
    @JoinColumn(name = "water_source_id")
    private CoreWaterSource coreWaterSource;

    // bi-directional many-to-one association to CoreLandRegime
    @ManyToOne
    @JoinColumn(name = "dest_land_regime_id")
    private CoreLandRegime destLandRegime;

    // bi-directional many-to-one association to CoreLandRegime
    @ManyToOne
    @JoinColumn(name = "source_land_regime_id")
    private CoreLandRegime sourceLandRegime;

    // bi-directional many-to-one association to CoreLicenseType
    @ManyToOne
    @JoinColumn(name = "license_type_id")
    private CoreLicenseType coreLicenseType;

    // bi-directional many-to-one association to CoreCustomer
    // @ManyToOne
    // @JoinColumn(name = "customer_id")
    // private CoreCustomer coreCustomer;

    // bi-directional many-to-one association to CoreLicense
    @ManyToOne
    @JoinColumn(name = "current_licence_id")
    private CoreLicense currentLicense;

    // bi-directional many-to-one association to CoreLicense
    @ManyToOne
    @JoinColumn(name = "application_status_id")
    private CoreApplicationStatus coreApplicationStatus;

    // bi-directional many-to-one association to CoreLicense
    @ManyToOne
    @JoinColumn(name = "application_step_id")
    private CoreApplicationStep coreApplicationStep;

    // bi-directional many-to-one association to CoreLicense
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private SysUserAccount sysUserAccount;

    // bi-directional many-to-one association to CoreApplicationDocument
    @JsonIgnore
    @OneToMany(mappedBy = "coreLicenseApplication")
    private List<CoreApplicationDocument> coreApplicationDocuments;

    // bi-directional many-to-one association to CoreApplicationPayment
    @JsonIgnore
    @OneToMany(mappedBy = "coreLicenseApplication")
    private List<CoreApplicationPayment> coreApplicationPayments;

    // bi-directional many-to-one association to CoreLicense
    @JsonIgnore
    @OneToMany(mappedBy = "coreLicenseApplication")
    private List<CoreLicense> coreLicenses;

    // bi-directional many-to-one association to CoreLicenseWaterUse
    @JsonIgnore
    @OneToMany(mappedBy = "coreLicenseApplication")
    private List<CoreLicenseWaterUse> coreLicenseWaterUses;

    // bi-directional many-to-one association to CoreWru
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "dest_wru")
    private CoreWaterResourceUnit destWru;

    // bi-directional many-to-one association to CoreWru
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "source_wru")
    private CoreWaterResourceUnit sourceWru;

    public CoreLicenseApplication() {
    }

    // public Timestamp getDateSubmitted() {
    //	return this.dateSubmitted;
    // }

    // public void setDateSubmitted(Timestamp dateSubmitted) {
    //	this.dateSubmitted = dateSubmitted;
    // }

    public String getDestEasting() {
        return this.destEasting;
    }

    public void setDestEasting(String destEasting) {
        this.destEasting = destEasting;
    }

    public String getDestHectarage() {
        return this.destHectarage;
    }

    public void setDestHectarage(String destHectarage) {
        this.destHectarage = destHectarage;
    }

    public String getDestNorthing() {
        return this.destNorthing;
    }

    public void setDestNorthing(String destNorthing) {
        this.destNorthing = destNorthing;
    }

    public String getDestOwnerFullname() {
        return this.destOwnerFullname;
    }

    public void setDestOwnerFullname(String destOwnerFullname) {
        this.destOwnerFullname = destOwnerFullname;
    }

    public String getDestPlotNumber() {
        return this.destPlotNumber;
    }

    public void setDestPlotNumber(String destPlotNumber) {
        this.destPlotNumber = destPlotNumber;
    }

    public String getDestTa() {
        return this.destTa;
    }

    public void setDestTa(String destTa) {
        this.destTa = destTa;
    }

    public String getDestVillage() {
        return this.destVillage;
    }

    public void setDestVillage(String destVillage) {
        this.destVillage = destVillage;
    }

    public String getSourceEasting() {
        return this.sourceEasting;
    }

    public void setSourceEasting(String sourceEasting) {
        this.sourceEasting = sourceEasting;
    }

    public String getSourceHectarage() {
        return this.sourceHectarage;
    }

    public void setSourceHectarage(String sourceHectarage) {
        this.sourceHectarage = sourceHectarage;
    }

    public String getSourceNorthing() {
        return this.sourceNorthing;
    }

    public void setSourceNorthing(String sourceNorthing) {
        this.sourceNorthing = sourceNorthing;
    }

    public String getSourceOwnerFullname() {
        return this.sourceOwnerFullname;
    }

    public void setSourceOwnerFullname(String sourceOwnerFullname) {
        this.sourceOwnerFullname = sourceOwnerFullname;
    }

    public String getSourcePlotNumber() {
        return this.sourcePlotNumber;
    }

    public void setSourcePlotNumber(String sourcePlotNumber) {
        this.sourcePlotNumber = sourcePlotNumber;
    }

    public String getSourceTa() {
        return this.sourceTa;
    }

    public void setSourceTa(String sourceTa) {
        this.sourceTa = sourceTa;
    }

    public String getSourceVillage() {
        return this.sourceVillage;
    }

    public void setSourceVillage(String sourceVillage) {
        this.sourceVillage = sourceVillage;
    }

    public CoreLandRegime getSourceLandRegime() {
        return this.sourceLandRegime;
    }

    public void setSourceLandRegime(CoreLandRegime sourceLandRegime) {
        this.sourceLandRegime = sourceLandRegime;
    }

    public CoreLandRegime getDestLandRegime() {
        return this.destLandRegime;
    }

    public void setDestLandRegime(CoreLandRegime destLandRegime) {
        this.destLandRegime = destLandRegime;
    }

    public List<CoreApplicationDocument> getCoreApplicationDocuments() {
        return this.coreApplicationDocuments;
    }

    public void setCoreApplicationDocuments(List<CoreApplicationDocument> coreApplicationDocuments) {
        this.coreApplicationDocuments = coreApplicationDocuments;
    }

    public CoreApplicationDocument addCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument) {
        getCoreApplicationDocuments().add(coreApplicationDocument);
        coreApplicationDocument.setCoreLicenseApplication(this);

        return coreApplicationDocument;
    }

    public CoreApplicationDocument removeCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument) {
        getCoreApplicationDocuments().remove(coreApplicationDocument);
        coreApplicationDocument.setCoreLicenseApplication(null);

        return coreApplicationDocument;
    }

    public List<CoreApplicationPayment> getCoreApplicationPayments() {
        return this.coreApplicationPayments;
    }

    public void setCoreApplicationPayments(List<CoreApplicationPayment> coreApplicationPayments) {
        this.coreApplicationPayments = coreApplicationPayments;
    }

    public CoreApplicationPayment addCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
        getCoreApplicationPayments().add(coreApplicationPayment);
        coreApplicationPayment.setCoreLicenseApplication(this);

        return coreApplicationPayment;
    }

    public CoreApplicationPayment removeCoreApplicationPayment(CoreApplicationPayment coreApplicationPayment) {
        getCoreApplicationPayments().remove(coreApplicationPayment);
        coreApplicationPayment.setCoreLicenseApplication(null);

        return coreApplicationPayment;
    }

    public List<CoreLicense> getCoreLicenses() {
        return this.coreLicenses;
    }

    public void setCoreLicenses(List<CoreLicense> coreLicenses) {
        this.coreLicenses = coreLicenses;
    }

    public CoreLicense addCoreLicens(CoreLicense coreLicens) {
        getCoreLicenses().add(coreLicens);
        coreLicens.setCoreLicenseApplication(this);

        return coreLicens;
    }

    public CoreLicense removeCoreLicens(CoreLicense coreLicens) {
        getCoreLicenses().remove(coreLicens);
        coreLicens.setCoreLicenseApplication(null);

        return coreLicens;
    }

    public CoreLicenseType getCoreLicenseType() {
        return this.coreLicenseType;
    }

    public void setCoreLicenseType(CoreLicenseType coreLicenseType) {
        this.coreLicenseType = coreLicenseType;
    }

    // public CoreCustomer getCoreCustomer() {
    //	return this.coreCustomer;
    // }

    // public void setCoreCustomer(CoreCustomer coreCustomer) {
    //	this.coreCustomer = coreCustomer;
    // }

    public CoreLicense getCurrentLicense() {
        return currentLicense;
    }

    public void setCurrentLicense(CoreLicense currentLicense) {
        this.currentLicense = currentLicense;
    }

    public List<CoreLicenseWaterUse> getCoreLicenseWaterUses() {
        return this.coreLicenseWaterUses;
    }

    public void setCoreLicenseWaterUses(List<CoreLicenseWaterUse> coreLicenseWaterUses) {
        this.coreLicenseWaterUses = coreLicenseWaterUses;
    }

    public CoreLicenseWaterUse addCoreLicenseWaterUs(CoreLicenseWaterUse coreLicenseWaterUs) {
        getCoreLicenseWaterUses().add(coreLicenseWaterUs);
        coreLicenseWaterUs.setCoreLicenseApplication(this);

        return coreLicenseWaterUs;
    }

    public CoreLicenseWaterUse removeCoreLicenseWaterUs(CoreLicenseWaterUse coreLicenseWaterUs) {
        getCoreLicenseWaterUses().remove(coreLicenseWaterUs);
        coreLicenseWaterUs.setCoreLicenseApplication(null);

        return coreLicenseWaterUs;
    }

    public CoreWaterSource getCoreWaterSource() {
        return coreWaterSource;
    }

    public void setCoreWaterSource(CoreWaterSource coreWaterSource) {
        this.coreWaterSource = coreWaterSource;
    }

    public CoreApplicationStatus getCoreApplicationStatus() {
        return coreApplicationStatus;
    }

    public void setCoreApplicationStatus(CoreApplicationStatus coreApplicationStatus) {
        this.coreApplicationStatus = coreApplicationStatus;
    }

    public CoreApplicationStep getCoreApplicationStep() {
        return coreApplicationStep;
    }

    public void setCoreApplicationStep(CoreApplicationStep coreApplicationStep) {
        this.coreApplicationStep = coreApplicationStep;
    }

    public SysUserAccount getSysUserAccount() {
        return sysUserAccount;
    }

    public void setSysUserAccount(SysUserAccount sysUserAccount) {
        this.sysUserAccount = sysUserAccount;
    }

    public CoreWaterResourceUnit getDestWru() {
        return destWru;
    }

    public void setDestWru(CoreWaterResourceUnit destWru) {
        this.destWru = destWru;
    }

    public CoreWaterResourceUnit getSourceWru() {
        return sourceWru;
    }

    public void setSourceWru(CoreWaterResourceUnit sourceWru) {
        this.sourceWru = sourceWru;
    }

    public Timestamp getDateSubmitted() {
        return this.dateSubmitted;
    }

    public void setDateSubmitted(Timestamp dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
    }

    public Integer getExistingBoreholeCount() {
        return this.existingBoreholeCount;
    }

    public void setExistingBoreholeCount(Integer existingBoreholeCount) {
        this.existingBoreholeCount = existingBoreholeCount;
    }

    public Double getPermitDuration() {
        return this.permitDuration;
    }

    public void setPermitDuration(Double permitDuration) {
        this.permitDuration = permitDuration;
    }

    public String getNearbyWaterUtilityBoard() {
        return this.nearbyWaterUtilityBoard;
    }

    public void setNearbyWaterUtilityBoard(String nearbyWaterUtilityBoard) {
        this.nearbyWaterUtilityBoard = nearbyWaterUtilityBoard;
    }

    public String getAltWaterSource() {
        return this.altWaterSource;
    }

    public void setAltWaterSource(String altWaterSource) {
        this.altWaterSource = altWaterSource;
    }

    public String getAltOtherWater() {
        return this.altOtherWater;
    }

    public void setAltOtherWater(String altOtherWater) {
        this.altOtherWater = altOtherWater;
    }

    public String getBoardMinutes() {
        return this.boardMinutes;
    }

    public void setBoardMinutes(String boardMinutes) {
        this.boardMinutes = boardMinutes;
    }

    public Timestamp getBoardApprovalDate() {
        return this.boardApprovalDate;
    }

    public void setBoardApprovalDate(Timestamp boardApprovalDate) {
        this.boardApprovalDate = boardApprovalDate;
    }

    // Getters and setters for workflow referral fields
    public String getReferredToUserId() {
        return referredToUserId;
    }

    public void setReferredToUserId(String referredToUserId) {
        this.referredToUserId = referredToUserId;
    }

    public String getReferredFromUserId() {
        return referredFromUserId;
    }

    public void setReferredFromUserId(String referredFromUserId) {
        this.referredFromUserId = referredFromUserId;
    }

    public String getReferralReason() {
        return referralReason;
    }

    public void setReferralReason(String referralReason) {
        this.referralReason = referralReason;
    }

    public String getLastHandledByUserId() {
        return lastHandledByUserId;
    }

    public void setLastHandledByUserId(String lastHandledByUserId) {
        this.lastHandledByUserId = lastHandledByUserId;
    }

    public Timestamp getReferralDate() {
        return referralDate;
    }

    public void setReferralDate(Timestamp referralDate) {
        this.referralDate = referralDate;
    }

    // Helper methods for workflow logic
    public boolean isReferredToUser(String userId) {
        return referredToUserId != null && referredToUserId.equals(userId);
    }

    public boolean hasActiveReferral() {
        return referredToUserId != null;
    }

    public void clearReferral() {
        this.referredToUserId = null;
        this.referredFromUserId = null;
        this.referralReason = null;
        this.referralDate = null;
    }

    public void setStatus(CoreApplicationStatus status) {
        this.coreApplicationStatus = status;
    }

    // Getters and setters for JSON fields
    public String getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }

    public String getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(String locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String getApplicationMetadata() {
        return applicationMetadata;
    }

    public void setApplicationMetadata(String applicationMetadata) {
        this.applicationMetadata = applicationMetadata;
    }

    public String getFormSpecificData() {
        return formSpecificData;
    }

    public void setFormSpecificData(String formSpecificData) {
        this.formSpecificData = formSpecificData;
    }

    // Getters and setters for application type fields
    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getOriginalLicenseId() {
        return originalLicenseId;
    }

    public void setOriginalLicenseId(String originalLicenseId) {
        this.originalLicenseId = originalLicenseId;
    }

    // Getters and setters for transfer fields
    public String getTransferToUserId() {
        return transferToUserId;
    }

    public void setTransferToUserId(String transferToUserId) {
        this.transferToUserId = transferToUserId;
    }

    public Boolean getIsDisabled() {
        return isDisabled;
    }

    public void setIsDisabled(Boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    // Helper methods for transfer logic
    public boolean hasTransferPending() {
        return transferToUserId != null;
    }

    public boolean isDisabledLicense() {
        return isDisabled != null && isDisabled;
    }

    public void clearTransfer() {
        this.transferToUserId = null;
    }

    public void approveTransfer() {
        if (transferToUserId != null) {
            this.isDisabled = true;
        }
    }

    // Getters and setters for emergency application fields
    public String getApplicationPriority() {
        return applicationPriority;
    }

    public void setApplicationPriority(String applicationPriority) {
        this.applicationPriority = applicationPriority;
    }

    public String getEmergencyJustificationFile() {
        return emergencyJustificationFile;
    }

    public void setEmergencyJustificationFile(String emergencyJustificationFile) {
        this.emergencyJustificationFile = emergencyJustificationFile;
    }

    public String getEmergencyReason() {
        return emergencyReason;
    }

    public void setEmergencyReason(String emergencyReason) {
        this.emergencyReason = emergencyReason;
    }

    public Timestamp getEmergencySubmittedDate() {
        return emergencySubmittedDate;
    }

    public void setEmergencySubmittedDate(Timestamp emergencySubmittedDate) {
        this.emergencySubmittedDate = emergencySubmittedDate;
    }

    // License fee getters and setters
    public Double getLicenseFee() {
        return licenseFee;
    }

    public void setLicenseFee(Double licenseFee) {
        this.licenseFee = licenseFee;
    }

    public String getLicenseFeeSetByUserId() {
        return licenseFeeSetByUserId;
    }

    public void setLicenseFeeSetByUserId(String licenseFeeSetByUserId) {
        this.licenseFeeSetByUserId = licenseFeeSetByUserId;
    }

    public Timestamp getLicenseFeeSetDate() {
        return licenseFeeSetDate;
    }

    public void setLicenseFeeSetDate(Timestamp licenseFeeSetDate) {
        this.licenseFeeSetDate = licenseFeeSetDate;
    }



    // Helper method for emergency applications
    public boolean isEmergencyApplication() {
        return "EMERGENCY".equalsIgnoreCase(applicationPriority);
    }

    public boolean hasEmergencyDocument() {
        return emergencyJustificationFile != null && !emergencyJustificationFile.isEmpty();
    }

    // Getters and setters for owner tracking
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    // Getters and setters for user account ID
    public String getUserAccountId() {
        return userAccountId;
    }

    public void setUserAccountId(String userAccountId) {
        this.userAccountId = userAccountId;
    }

    // Helper methods for owner management
    public boolean isOwnedBy(String userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public void transferOwnership(String newOwnerId) {
        if (newOwnerId == null) {
            throw new IllegalArgumentException("Owner ID cannot be null");
        }
        this.ownerId = newOwnerId;
    }

    public void setOwnerFromUser(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        this.ownerId = userId;
    }

    public String getBoardMinutesDocument() {
        return boardMinutesDocument;
    }

    public void setBoardMinutesDocument(String boardMinutesDocument) {
        this.boardMinutesDocument = boardMinutesDocument;
    }

    // Getters and setters for easement-specific fields
    public String getBurdenedLandDescription() {
        return burdenedLandDescription;
    }

    public void setBurdenedLandDescription(String burdenedLandDescription) {
        this.burdenedLandDescription = burdenedLandDescription;
    }

    public String getBenefittedLandDescription() {
        return benefittedLandDescription;
    }

    public void setBenefittedLandDescription(String benefittedLandDescription) {
        this.benefittedLandDescription = benefittedLandDescription;
    }

    public String getPermitConditions() {
        return permitConditions;
    }

    public void setPermitConditions(String permitConditions) {
        this.permitConditions = permitConditions;
    }

    public String getNatureOfBurden() {
        return natureOfBurden;
    }

    public void setNatureOfBurden(String natureOfBurden) {
        this.natureOfBurden = natureOfBurden;
    }
}