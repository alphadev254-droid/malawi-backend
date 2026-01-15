package mw.nwra.ewaterpermit.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreDocumentCategory;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreDocumentCategoryService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/document-categories")
public class CoreDocumentCategoryController {

	@Autowired
	private CoreDocumentCategoryService coreDocumentCategoryService;

	@GetMapping(path = "")
	public List<CoreDocumentCategory> getCoreDocumentCategorys(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreDocumentCategory> newsCategories = this.coreDocumentCategoryService.getAllCoreDocumentCategorys(page,
				limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreDocumentCategory getCoreDocumentCategoryById(@PathVariable(name = "id") String id) {
		CoreDocumentCategory coreDocumentCategory = this.coreDocumentCategoryService.getCoreDocumentCategoryById(id);
		if (coreDocumentCategory == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreDocumentCategory;
	}

	@PostMapping(path = "")
	public CoreDocumentCategory createCoreDocumentCategory(
			@RequestBody Map<String, Object> coreDocumentCategoryRequest) {
		CoreDocumentCategory coreDocumentCategory = (CoreDocumentCategory) AppUtil
				.objectToClass(CoreDocumentCategory.class, coreDocumentCategoryRequest);
		if (coreDocumentCategory == null) {
			throw new ForbiddenException("Could not create the coreDocumentCategory");
		}
		return this.coreDocumentCategoryService.addCoreDocumentCategory(coreDocumentCategory);
	}

	@PutMapping(path = "/{id}")
	public CoreDocumentCategory updateCoreDocumentCategory(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreDocumentCategoryRequest) {
		CoreDocumentCategory coreDocumentCategory = this.coreDocumentCategoryService.getCoreDocumentCategoryById(id);
		if (coreDocumentCategory == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreDocumentCategory.class,
				coreDocumentCategoryRequest);
		CoreDocumentCategory coreDocumentCategoryFromObj = (CoreDocumentCategory) AppUtil
				.objectToClass(CoreDocumentCategory.class, coreDocumentCategoryRequest);
		if (coreDocumentCategoryFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		BeanUtils.copyProperties(coreDocumentCategoryFromObj, coreDocumentCategory, propertiesToIgnore);
		this.coreDocumentCategoryService.editCoreDocumentCategory(coreDocumentCategory);
		return coreDocumentCategory;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreDocumentCategory(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreDocumentCategory categ = this.coreDocumentCategoryService.getCoreDocumentCategoryById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreDocumentCategoryService.deleteCoreDocumentCategory(id);
	}
}
