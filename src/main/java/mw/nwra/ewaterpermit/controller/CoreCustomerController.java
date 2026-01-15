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
import mw.nwra.ewaterpermit.model.CoreCustomer;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreCustomerService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/customers")
public class CoreCustomerController {

	@Autowired
	private CoreCustomerService coreCustomerService;
	
	@Autowired
	private Auditor auditor;

	@GetMapping(path = "")
	public List<CoreCustomer> getCoreCustomers(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<CoreCustomer> newsCategories = this.coreCustomerService.getAllCoreCustomers(page, limit);
		if (newsCategories.isEmpty()) {
			throw new EntityNotFoundException("Object not found");
		}
		return newsCategories;
	}

	@GetMapping(path = "/{id}")
	public CoreCustomer getCoreCustomerById(@PathVariable(name = "id") String id) {
		CoreCustomer coreCustomer = this.coreCustomerService.getCoreCustomerById(id);
		if (coreCustomer == null) {
			throw new EntityNotFoundException("Object not found");
		}
		return coreCustomer;
	}

	@PostMapping(path = "")
	public CoreCustomer createCoreCustomer(@RequestBody Map<String, Object> coreCustomerRequest,
			@RequestHeader(name = "Authorization") String token) {
		CoreCustomer coreCustomer = (CoreCustomer) AppUtil.objectToClass(CoreCustomer.class, coreCustomerRequest);
		if (coreCustomer == null) {
			throw new ForbiddenException("Could not create the coreCustomer");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		coreCustomer = this.coreCustomerService.addCoreCustomer(coreCustomer);
		
		// Audit log
		auditor.audit(user, null, coreCustomer, CoreCustomer.class, Action.CREATE.toString());
		
		return coreCustomer;
	}

	@PutMapping(path = "/{id}")
	public CoreCustomer updateCoreCustomer(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> coreCustomerRequest,
			@RequestHeader(name = "Authorization") String token) {
		CoreCustomer coreCustomer = this.coreCustomerService.getCoreCustomerById(id);
		if (coreCustomer == null) {
			throw new EntityNotFoundException("Role not found");
		}
		
		// Clone for audit
		CoreCustomer oldCustomer = new CoreCustomer();
		BeanUtils.copyProperties(coreCustomer, oldCustomer);
		
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreCustomer.class, coreCustomerRequest);
		CoreCustomer coreCustomerFromObj = (CoreCustomer) AppUtil.objectToClass(CoreCustomer.class,
				coreCustomerRequest);
		if (coreCustomerFromObj == null) {
			throw new ForbiddenException("Could not update the Object");
		}
		SysUserAccount user = AppUtil.getLoggedInUser(token);
		BeanUtils.copyProperties(coreCustomerFromObj, coreCustomer, propertiesToIgnore);
		this.coreCustomerService.editCoreCustomer(coreCustomer);
		
		// Audit log
		auditor.audit(user, oldCustomer, coreCustomer, CoreCustomer.class, Action.UPDATE.toString());
		
		return coreCustomer;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteCoreCustomer(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		CoreCustomer categ = this.coreCustomerService.getCoreCustomerById(id);
		if (categ == null) {
			throw new EntityNotFoundException("User group not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysUserGroup() != null && ua.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
			this.coreCustomerService.deleteCoreCustomer(id);
			
			// Audit log
			auditor.audit(ua, categ, null, CoreCustomer.class, Action.DELETE.toString());
		} else {
			throw new EntityNotFoundException("Action denied");
		}
	}
}
