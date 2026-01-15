package mw.nwra.ewaterpermit.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_document_category")
@NamedQuery(name = "CoreDocumentCategory.findAll", query = "SELECT c FROM CoreDocumentCategory c")
public class CoreDocumentCategory extends BaseEntity {

	private String description;

	private String name;

	// bi-directional many-to-one association to CoreApplicationDocument
	@JsonIgnore
	@OneToMany(mappedBy = "coreDocumentCategory")
	private List<CoreApplicationDocument> coreApplicationDocuments;

	public CoreDocumentCategory() {
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<CoreApplicationDocument> getCoreApplicationDocuments() {
		return this.coreApplicationDocuments;
	}

	public void setCoreApplicationDocuments(List<CoreApplicationDocument> coreApplicationDocuments) {
		this.coreApplicationDocuments = coreApplicationDocuments;
	}

	public CoreApplicationDocument addCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument) {
		getCoreApplicationDocuments().add(coreApplicationDocument);
		coreApplicationDocument.setCoreDocumentCategory(this);

		return coreApplicationDocument;
	}

	public CoreApplicationDocument removeCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument) {
		getCoreApplicationDocuments().remove(coreApplicationDocument);
		coreApplicationDocument.setCoreDocumentCategory(null);

		return coreApplicationDocument;
	}

}