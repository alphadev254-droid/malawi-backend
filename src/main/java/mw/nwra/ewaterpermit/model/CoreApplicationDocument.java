package mw.nwra.ewaterpermit.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_application_document")
@NamedQuery(name = "CoreApplicationDocument.findAll", query = "SELECT c FROM CoreApplicationDocument c")
public class CoreApplicationDocument extends BaseEntity {
	@Column(name = "document_url")
	private String documentUrl;

	@Column(name = "status")
	private String status = "AWAITING_APPROVAL";

	// bi-directional many-to-one association to CoreLicenseApplication
	@JsonBackReference
	@ManyToOne
	@JoinColumn(name = "license_application_id")
	private CoreLicenseApplication coreLicenseApplication;

	// bi-directional many-to-one association to CoreDocumentCategory
	@ManyToOne
	@JoinColumn(name = "document_category_id")
	private CoreDocumentCategory coreDocumentCategory;

	public CoreApplicationDocument() {
	}

	public String getDocumentUrl() {
		return this.documentUrl;
	}

	public void setDocumentUrl(String documentUrl) {
		this.documentUrl = documentUrl;
	}

	public CoreLicenseApplication getCoreLicenseApplication() {
		return this.coreLicenseApplication;
	}

	public void setCoreLicenseApplication(CoreLicenseApplication coreLicenseApplication) {
		this.coreLicenseApplication = coreLicenseApplication;
	}

	public CoreDocumentCategory getCoreDocumentCategory() {
		return this.coreDocumentCategory;
	}

	public void setCoreDocumentCategory(CoreDocumentCategory coreDocumentCategory) {
		this.coreDocumentCategory = coreDocumentCategory;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}