package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreApplicationDocument;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;

public interface CoreApplicationDocumentService {
	public List<CoreApplicationDocument> getAllCoreApplicationDocuments();

	public SearchResponse getAllCoreApplicationDocuments(int page, int limit, String query);

	public List<CoreApplicationDocument> getAllCoreApplicationDocuments(int page, int limit);

	public CoreApplicationDocument getCoreApplicationDocumentById(String id);

	public void deleteCoreApplicationDocument(String id);

	public CoreApplicationDocument addCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument);

	public CoreApplicationDocument editCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument);

	public List<CoreApplicationDocument> getCoreApplicationDocumentByApplication(CoreLicenseApplication appl);

	public SearchResponse getAllCoreApplicationDocumentsWithDetails(int page, int limit, String query);

	// Alias methods for consistency
	default CoreApplicationDocument createCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument) {
		return addCoreApplicationDocument(coreApplicationDocument);
	}

	default List<CoreApplicationDocument> getCoreApplicationDocuments(int page, int limit) {
		return getAllCoreApplicationDocuments(page, limit);
	}
}
