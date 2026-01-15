package mw.nwra.ewaterpermit.model;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "core_ceo_decision")
public class CoreCEODecision {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "license_application_id", length = 36, nullable = false)
    private String licenseApplicationId;
    
    @Column(name = "decision", length = 20, nullable = false)
    private String decision; // 'APPROVE' or 'REFER_BACK'
    
    @Column(name = "board_minutes", columnDefinition = "TEXT")
    private String boardMinutes;
    
    @Column(name = "board_approval_date")
    private Timestamp boardApprovalDate;
    
    @Column(name = "ceo_notes", columnDefinition = "TEXT")
    private String ceoNotes;
    
    @Column(name = "ceo_documents", columnDefinition = "TEXT")
    private String ceoDocuments; // JSON array of file paths
    
    @Column(name = "ceo_user_id", length = 36)
    private String ceoUserId;
    
    @Column(name = "date_created")
    private Timestamp dateCreated;
    
    // Constructors
    public CoreCEODecision() {}
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getLicenseApplicationId() { return licenseApplicationId; }
    public void setLicenseApplicationId(String licenseApplicationId) { this.licenseApplicationId = licenseApplicationId; }
    
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    
    public String getBoardMinutes() { return boardMinutes; }
    public void setBoardMinutes(String boardMinutes) { this.boardMinutes = boardMinutes; }
    
    public Timestamp getBoardApprovalDate() { return boardApprovalDate; }
    public void setBoardApprovalDate(Timestamp boardApprovalDate) { this.boardApprovalDate = boardApprovalDate; }
    
    public String getCeoNotes() { return ceoNotes; }
    public void setCeoNotes(String ceoNotes) { this.ceoNotes = ceoNotes; }
    
    public String getCeoDocuments() { return ceoDocuments; }
    public void setCeoDocuments(String ceoDocuments) { this.ceoDocuments = ceoDocuments; }
    
    public String getCeoUserId() { return ceoUserId; }
    public void setCeoUserId(String ceoUserId) { this.ceoUserId = ceoUserId; }
    
    public Timestamp getDateCreated() { return dateCreated; }
    public void setDateCreated(Timestamp dateCreated) { this.dateCreated = dateCreated; }
}