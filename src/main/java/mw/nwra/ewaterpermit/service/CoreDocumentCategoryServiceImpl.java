package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreDocumentCategory;
import mw.nwra.ewaterpermit.repository.CoreDocumentCategoryRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreDocumentCategoryService")
public class CoreDocumentCategoryServiceImpl implements CoreDocumentCategoryService {
	@Autowired
	CoreDocumentCategoryRepository repo;

	@Override
	public List<CoreDocumentCategory> getAllCoreDocumentCategorys() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreDocumentCategory> getAllCoreDocumentCategorys(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreDocumentCategory getCoreDocumentCategoryById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreDocumentCategory getCoreDocumentCategoryByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreDocumentCategory(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreDocumentCategory addCoreDocumentCategory(CoreDocumentCategory coreDocumentCategory) {
		return this.repo.saveAndFlush(coreDocumentCategory);
	}

	@Override
	public CoreDocumentCategory editCoreDocumentCategory(CoreDocumentCategory coreDocumentCategory) {
		return this.repo.saveAndFlush(coreDocumentCategory);
	}
}
