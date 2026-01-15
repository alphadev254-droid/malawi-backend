package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
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

import mw.nwra.ewaterpermit.exception.DataFormatException;
import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreCustomerType;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreCustomerTypeService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/customer-types")
public class CoreCustomerTypeController {

	@Autowired
	private CoreCustomerTypeService coreCustomerService;

	@GetMapping(path = "")
	public List<CoreCustomerType> getAllGroups(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreCustomerType> newsCategories = this.coreCustomerService.getAllCoreCustomerTypes(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Customer Type not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreCustomerType getCoreCustomerTypeById(@PathVariable(name = "id") String groupId) {
		CoreCustomerType sysGroup = this.coreCustomerService.getCoreCustomerTypeById(groupId);
		if (sysGroup == null) {
			throw new EntityNotFoundException("Customer Type not found");
		}
		return sysGroup;
	}

	@PostMapping(path = "")
	public CoreCustomerType createCoreCustomerType(@RequestBody Map<String, Object> coreCustomerTypeRequest) {
		CoreCustomerType coreCustomerType = null;
		try {
			coreCustomerType = (CoreCustomerType) AppUtil.objectToClass(CoreCustomerType.class,
					coreCustomerTypeRequest);
		} catch (DataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (coreCustomerType == null) {
			throw new ForbiddenException("Could not create the sys user group");
		}
		coreCustomerType.setDateCreated(new Timestamp(new Date().getTime()));
		return this.coreCustomerService.addCoreCustomerType(coreCustomerType);
	}

	@PutMapping(path = "/{id}")
	public CoreCustomerType updateCoreCustomerType(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreCustomerTypeRequest) {
		CoreCustomerType coreCustomerType = this.coreCustomerService.getCoreCustomerTypeById(id);
		if (coreCustomerType == null) {
			throw new EntityNotFoundException("Role not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreCustomerType.class, coreCustomerTypeRequest);
		CoreCustomerType coreCustomerTypeFromObj = null;
		try {
			coreCustomerTypeFromObj = (CoreCustomerType) AppUtil.objectToClass(CoreCustomerType.class,
					coreCustomerTypeRequest);
		} catch (DataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (coreCustomerTypeFromObj == null) {
			throw new ForbiddenException("Could not update the coreCustomerType coreCustomerType");
		}
		BeanUtils.copyProperties(coreCustomerTypeFromObj, coreCustomerType, propertiesToIgnore);
		coreCustomerType.setDateUpdated(new Timestamp(new Date().getTime()));
		this.coreCustomerService.editCoreCustomerType(coreCustomerType);
		return coreCustomerType;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreCustomerType(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreCustomerType categ = this.coreCustomerService.getCoreCustomerTypeById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.coreCustomerService.deleteCoreCustomerType(id);
	}
}