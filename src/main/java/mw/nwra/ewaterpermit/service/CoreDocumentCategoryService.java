package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreDocumentCategory;

public interface CoreDocumentCategoryService {
	public List<CoreDocumentCategory> getAllCoreDocumentCategorys();

	public List<CoreDocumentCategory> getAllCoreDocumentCategorys(int page, int limit);

	public CoreDocumentCategory getCoreDocumentCategoryById(String id);

	public CoreDocumentCategory getCoreDocumentCategoryByName(String name);

	public void deleteCoreDocumentCategory(String id);

	public CoreDocumentCategory addCoreDocumentCategory(CoreDocumentCategory coreDocumentCategory);

	public CoreDocumentCategory editCoreDocumentCategory(CoreDocumentCategory coreDocumentCategory);

	// Alias methods for consistency
	default CoreDocumentCategory createCoreDocumentCategory(CoreDocumentCategory coreDocumentCategory) {
		return addCoreDocumentCategory(coreDocumentCategory);
	}

	default List<CoreDocumentCategory> getCoreDocumentCategories(int page, int limit) {
		return getAllCoreDocumentCategorys(page, limit);
	}
}
