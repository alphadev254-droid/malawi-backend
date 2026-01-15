package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreApplicationDocument;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.repository.CoreApplicationDocumentRepository;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreApplicationDocumentService")
public class CoreApplicationDocumentServiceImpl implements CoreApplicationDocumentService {
	@Autowired
	CoreApplicationDocumentRepository repo;

	@Override
	public List<CoreApplicationDocument> getAllCoreApplicationDocuments() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreApplicationDocument> getAllCoreApplicationDocuments(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public SearchResponse getAllCoreApplicationDocuments(int page, int limit, String query) {
		Page<CoreApplicationDocument> res = this.repo.findAll(query,
				AppUtil.getPageRequest(page, limit, "dateCreated", "asc"));
		return res.getContent().isEmpty() ? null : new SearchResponse(res.getTotalElements(), res.getContent());
	}

	@Override
	public CoreApplicationDocument getCoreApplicationDocumentById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public void deleteCoreApplicationDocument(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreApplicationDocument addCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument) {
		return this.repo.saveAndFlush(coreApplicationDocument);
	}

	@Override
	public CoreApplicationDocument editCoreApplicationDocument(CoreApplicationDocument coreApplicationDocument) {
		return this.repo.saveAndFlush(coreApplicationDocument);
	}

	@Override
	public List<CoreApplicationDocument> getCoreApplicationDocumentByApplication(CoreLicenseApplication appl) {
		return repo.findByCoreLicenseApplication(appl);
	}

	@Override
	public SearchResponse getAllCoreApplicationDocumentsWithDetails(int page, int limit, String query) {
		Page<CoreApplicationDocument> res = this.repo.findAllWithDetails(query,
				AppUtil.getPageRequest(page, limit, "dateCreated", "asc"));
		return res.getContent().isEmpty() ? null : new SearchResponse(res.getTotalElements(), res.getContent());
	}
}
