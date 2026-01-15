package mw.nwra.ewaterpermit.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.model.SysSalutation;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.SysSalutationService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/sys-salutations")
public class SysSalutationController {

	@Autowired
	private SysSalutationService salutationService;

	@GetMapping(path = "")
	public List<SysSalutation> getAllSysSalutations(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		List<SysSalutation> salutations = this.salutationService.getAllSysSalutations(page, limit);
		if (salutations.isEmpty()) {
			throw new EntityNotFoundException("SysSalutations not found");
		}
		return salutations;
	}

	@GetMapping(path = "/{id}")
	public SysSalutation getSysSalutationById(@PathVariable(name = "id") String salutationId) {
		SysSalutation salutation = this.salutationService.getSysSalutationById(salutationId);
		if (salutation == null) {
			throw new EntityNotFoundException("SysSalutation not found");
		}
		return salutation;
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysSalutation(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysSalutation salutation = this.salutationService.getSysSalutationById(id);
		if (salutation == null) {
			throw new EntityNotFoundException("Salutation not found");
		}
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		if (ua != null && ua.getSysSalutation() != null && ua.getSysSalutation().getName().equalsIgnoreCase("admin")) {
		} else {
			throw new EntityNotFoundException("Action denied");
		}
		this.salutationService.deleteSysSalutation(id);
	}
}
