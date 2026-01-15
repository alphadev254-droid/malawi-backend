package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import mw.nwra.ewaterpermit.constant.Action;
import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;
import mw.nwra.ewaterpermit.service.SysUserAccountService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

import mw.nwra.ewaterpermit.util.PasswordHash;
import org.springframework.dao.DataIntegrityViolationException;

@RestController
@RequestMapping(value = "/v1/sys-user-accounts")
public class SysUserAccountController {
	private final String OBJ = "user-accounts";
	@Autowired
	private SysUserAccountService userAccountService;
	@Autowired
	private Auditor auditor;

	@GetMapping
	public SearchResponse getAllSysUserAccounts(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "20") int limit,
			@RequestParam(value = "query", defaultValue = "") String query) {
		SearchResponse userAccounts = this.userAccountService.getAllSysUserAccounts(page, limit, query);
		if (userAccounts == null) {
			throw new EntityNotFoundException("User accounts not found");
		}
		@SuppressWarnings("unchecked")
		List<SysUserAccount> accounts = this.sanitizeSysUserAccounts((List<SysUserAccount>) userAccounts.getData());
		userAccounts.setData(accounts);
		return userAccounts;
	}

	@GetMapping(path = "/{id}")
	public SysUserAccount getSysUserAccountById(@PathVariable(name = "id") String id) {
		SysUserAccount userAccount = this.userAccountService.getSysUserAccountById(id);
		if (userAccount == null) {
			throw new EntityNotFoundException("User account with id '" + id + "' not found");
		}
		return AppUtil.sanitizeSysUserAccount(userAccount);
	}

	@PostMapping
	public SysUserAccount createSysUserAccount(@RequestBody Map<String, Object> userAccountRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.CREATE.toString());

		SysUserAccount userAccount = (SysUserAccount) AppUtil.objectToClass(SysUserAccount.class, userAccountRequest);
		if (userAccount == null) {
			throw new ForbiddenException("Could not create the user account");
		}
		if (userAccount.getPassword() != null && !userAccount.getPassword().trim().isEmpty()) {
			// hash password supplied
			userAccount.setPassword(PasswordHash.hashPassword(userAccount.getPassword()));
		} else {
			throw new ForbiddenException("Could not create the user account. Password unavailable.");
		}
		userAccount.setDateCreated(new Timestamp(new Date().getTime()));
		this.userAccountService.createSysUserAccount(userAccount);

		// Audit log
		auditor.audit(Action.CREATE, "SysUserAccount", userAccount.getId(), ua, "Created user account");

		return AppUtil.sanitizeSysUserAccount(userAccount);
	}

	@PutMapping(path = "/{id}")
	public SysUserAccount updateSysUserAccount(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> userAccountRequest, @RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.UPDATE.toString());

		SysUserAccount userAccount = this.userAccountService.getSysUserAccountById(id);
		if (userAccount == null) {
			throw new EntityNotFoundException("User account not found");
		}

		// Clone old user account for audit
		SysUserAccount oldUserAccount = new SysUserAccount();
		BeanUtils.copyProperties(userAccount, oldUserAccount);

		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(SysUserAccount.class, userAccountRequest);

		SysUserAccount userAccountFromObj = (SysUserAccount) AppUtil.objectToClass(SysUserAccount.class,
				userAccountRequest);
		if (userAccountFromObj == null) {
			throw new ForbiddenException("Could not update the user account");
		}
		SysUserAccount loggedUser = AppUtil.getLoggedInUser(token);
		if (loggedUser != null && loggedUser.getSysUserGroup() != null
				&& loggedUser.getSysUserGroup().getName().equalsIgnoreCase("admin")) {

		} else if (!loggedUser.getId().equals(userAccount.getId())) {
			throw new EntityNotFoundException("Action denied. Not your acccount");
		}
		if (userAccountFromObj.getPassword() != null && !userAccountFromObj.getPassword().trim().isEmpty()) {
			// if new password supplied, verify current password first
			String currentPassword = (String) userAccountRequest.get("currentPassword");
			if (currentPassword != null && !PasswordHash.checkPassword(currentPassword, userAccount.getPassword())) {
				throw new ForbiddenException("Current password is incorrect");
			}
			userAccountFromObj.setPassword(PasswordHash.hashPassword(userAccountFromObj.getPassword()));
		} else {
			userAccountFromObj.setPassword(userAccount.getPassword());
		}
		if (userAccountFromObj.getEmailAddress() == null) {
			userAccountFromObj.setEmailAddress(userAccount.getEmailAddress());
		}
		BeanUtils.copyProperties(userAccountFromObj, userAccount, propertiesToIgnore);
		userAccount.setDateUpdated(new Timestamp(new Date().getTime()));
		this.userAccountService.updateSysUserAccount(userAccount);

		// Audit log
		auditor.audit(Action.UPDATE, "SysUserAccount", userAccount.getId(), ua, "Updated user account");

		return AppUtil.sanitizeSysUserAccount(userAccount);
	}

	@PutMapping(path = "/me/change-password")
	public SysUserAccount changeMyPassword(@RequestBody Map<String, Object> passwordRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount loggedUser = AppUtil.getLoggedInUser(token);
		if (loggedUser == null) {
			throw new EntityNotFoundException("User not found");
		}

		String currentPassword = (String) passwordRequest.get("currentPassword");
		String newPassword = (String) passwordRequest.get("password");

		if (currentPassword == null || currentPassword.trim().isEmpty()) {
			throw new ForbiddenException("Current password is required");
		}
		if (newPassword == null || newPassword.trim().isEmpty()) {
			throw new ForbiddenException("New password is required");
		}

		// Verify current password
		if (!PasswordHash.checkPassword(currentPassword, loggedUser.getPassword())) {
			throw new ForbiddenException("Current password is incorrect");
		}

		// Clone for audit
		SysUserAccount oldUserAccount = new SysUserAccount();
		BeanUtils.copyProperties(loggedUser, oldUserAccount);

		// Update password
		loggedUser.setPassword(PasswordHash.hashPassword(newPassword));
		loggedUser.setDateUpdated(new Timestamp(new Date().getTime()));
		this.userAccountService.updateSysUserAccount(loggedUser);

		// Audit log
		auditor.audit(Action.UPDATE, "SysUserAccount", loggedUser.getId(), loggedUser, "Changed password");

		return AppUtil.sanitizeSysUserAccount(loggedUser);
	}

	@PutMapping(path = "/me/update-profile")
	public SysUserAccount updateMyProfile(@RequestBody Map<String, Object> profileRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount loggedUser = AppUtil.getLoggedInUser(token);
		if (loggedUser == null) {
			throw new EntityNotFoundException("User not found");
		}

		// Clone for audit
		SysUserAccount oldUserAccount = new SysUserAccount();
		BeanUtils.copyProperties(loggedUser, oldUserAccount);

		String username = (String) profileRequest.get("username");
		String emailAddress = (String) profileRequest.get("emailAddress");

		// Update username if provided
		if (username != null && !username.trim().isEmpty()) {
			// Check if username already exists (excluding current user)
			SysUserAccount existingUser = this.userAccountService.getSysUserAccountByUsername(username);
			if (existingUser != null && !existingUser.getId().equals(loggedUser.getId())) {
				throw new ForbiddenException("Username already exists");
			}
			loggedUser.setUsername(username);
		}

		// Update email if provided
		if (emailAddress != null && !emailAddress.trim().isEmpty()) {
			// Check if email already exists (excluding current user)
			SysUserAccount existingUser = this.userAccountService.getSysUserAccountByEmailAddress(emailAddress);
			if (existingUser != null && !existingUser.getId().equals(loggedUser.getId())) {
				throw new ForbiddenException("Email address already exists");
			}
			loggedUser.setEmailAddress(emailAddress);
		}

		loggedUser.setDateUpdated(new Timestamp(new Date().getTime()));
		this.userAccountService.updateSysUserAccount(loggedUser);

		// Audit log
		auditor.audit(Action.UPDATE, "SysUserAccount", loggedUser.getId(), loggedUser, "Updated profile");

		return AppUtil.sanitizeSysUserAccount(loggedUser);
	}

	@DeleteMapping(path = "/{id}")
	public void deleteSysUserAccount(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.DELETE.toString());

		SysUserAccount userAccount = this.userAccountService.getSysUserAccountById(id);
		if (userAccount == null) {
			throw new EntityNotFoundException("User account with id '" + id + "' not found");
		}

		String userName = (userAccount.getFirstName() != null ? userAccount.getFirstName() + " " : "") +
						  (userAccount.getLastName() != null ? userAccount.getLastName() : "");
		userName = userName.trim().isEmpty() ? userAccount.getUsername() : userName.trim();

		try {
			this.userAccountService.deleteSysUserAccount(userAccount.getId());

			// Audit log
			auditor.audit(Action.DELETE, "SysUserAccount", userAccount.getId(), ua, "Deleted user account");
		} catch (DataIntegrityViolationException e) {
			throw new ForbiddenException("Cannot delete user '" + userName + "' because they have associated records in the system (activations, applications, etc.). Please deactivate the user instead of deleting.");
		}
	}

	@GetMapping(path = "/count")
	public Map<String, Long> countAll() {
		long count = this.userAccountService.countAll();
		Map<String, Long> response = new HashMap<>();
		response.put("count", count);
		return response;
	}

	private List<SysUserAccount> sanitizeSysUserAccounts(List<SysUserAccount> userAccounts) {
		return userAccounts.stream().map(userAccount -> {
			return AppUtil.sanitizeSysUserAccount(userAccount);
		}).collect(Collectors.toList());
	}
}
