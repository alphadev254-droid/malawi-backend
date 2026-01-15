package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.constant.SysAccountStatusValue;
import mw.nwra.ewaterpermit.constant.SysTemplateName;
import mw.nwra.ewaterpermit.constant.TemplateName;
import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.exception.UnauthorizedException;
import mw.nwra.ewaterpermit.model.SysAccountStatus;
import mw.nwra.ewaterpermit.model.SysDisposableDomain;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.model.SysUserAccountActivation;
import mw.nwra.ewaterpermit.model.SysUserGroup;
import mw.nwra.ewaterpermit.model.UserNotification;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationCategory;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationPriority;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationType;
import mw.nwra.ewaterpermit.requestSchema.AuthenticationRequest;
import mw.nwra.ewaterpermit.requestSchema.OTPRequest;
import mw.nwra.ewaterpermit.requestSchema.OTPVerifyRequest;
import mw.nwra.ewaterpermit.requestSchema.ResetPasswordRequest;
import mw.nwra.ewaterpermit.requestSchema.SysUserAccountCreateRequest;
import mw.nwra.ewaterpermit.responseSchema.AuthenticationResponse;
import mw.nwra.ewaterpermit.service.CustomUserDetailsService;
import mw.nwra.ewaterpermit.service.MailingService;
import mw.nwra.ewaterpermit.service.SysAccountStatusService;
import mw.nwra.ewaterpermit.service.NotificationService;
import mw.nwra.ewaterpermit.service.SysDisposableDomainService;
import mw.nwra.ewaterpermit.service.SysUserAccountActivationService;
import mw.nwra.ewaterpermit.service.SysUserAccountService;
import mw.nwra.ewaterpermit.service.SysUserGroupService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.JwtUtil;
import mw.nwra.ewaterpermit.util.PasswordHash;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;

@RestController
@RequestMapping(value = "/v1/auth")
public class UserAuthController {

	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	CustomUserDetailsService userDetailsService;

	@Autowired
	private SysUserAccountService userAccountService;

	@Autowired
	private SysAccountStatusService accountStatusService;

	@Autowired
	private SysUserAccountActivationService userAccountActivationService;

	@Autowired
	private MailingService mailService;

	@Autowired
	JwtUtil jwtUtil;

//	@Autowired
//	MessagingService messagingService;

	@Autowired
	SysDisposableDomainService disposableDomainService;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private SysUserGroupService sysUserGroupService;

	@Autowired
	private Auditor auditor;

	@PostMapping(path = "/sign-in")
	public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) throws Exception {
		String exceptionFound = null;

		SysUserAccount userAccount = this.userAccountService.getSysUserAccountByUsername(request.getUsername());
		if (userAccount != null && userAccount.getSysAccountStatus() != null) {
			if (userAccount.getSysAccountStatus().getName()
					.equalsIgnoreCase(SysAccountStatusValue.CONFIRM_ACCOUNT.toString())) {
				exceptionFound = "Unconfirmed account. Check your email for activation link.";
			} else if (userAccount.getSysAccountStatus().getName()
					.equalsIgnoreCase(SysAccountStatusValue.TEMPORARILY_DISABLED.toString())) {
				if (userAccount.getCanLoginAfter() != null
						&& new Timestamp(new Date().getTime()).before(userAccount.getCanLoginAfter())) {

					exceptionFound = "Your account has temporarily been disabled for security reasons. Try again later.";
				}
			} else if (userAccount.getSysAccountStatus().getName()
					.equalsIgnoreCase(SysAccountStatusValue.DEACTIVATED.toString())) {
				exceptionFound = "Your account has been suspended. Contact system administrator for help.";
			}
		}

		if (exceptionFound != null) {
			throw new UnauthorizedException(exceptionFound);
		}
		// authenticate
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		} catch (Exception e) {
			throw new UnauthorizedException("Invalid credentials (Username or password incorrect)");
		}

		// copy old user details
//		SysUserAccount theUser = SerializationUtils.clone(userAccount);
//		userAccount.setLastLogin(new Timestamp(new Date().getTime()));
//		userAccount.setCanLoginAfter(null);
//		userAccount.setLastPasswordAttempt(new Timestamp(new Date().getTime()));
//		userAccount.setPasswordAttemptCount(0);
//		this.userAccountService.updateSysUserAccount(userAccount);

		final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
		final String token = jwtUtil.generateToken(userDetails, request.isRememberMe());
//		theUser = AppUtil.sanitizeSysUserAccount(theUser);

		// Login notification disabled

		// Audit login for non-applicant users
		String userRole = userAccount.getSysUserGroup() != null ? userAccount.getSysUserGroup().getName() : "UNKNOWN";
		if (!"APPLICANT".equalsIgnoreCase(userRole)) {
			auditor.audit(Action.VIEW, "UserLogin", userAccount.getId(), userAccount, "User logged in - Role: " + userRole);
		}

		return ResponseEntity.ok(new AuthenticationResponse(token, AppUtil.sanitizeSysUserAccount(userAccount),
				AppUtil.getUserPermissions(userAccount)));
	}

	@Transactional
	@PostMapping(path = "/sign-up", consumes = {"multipart/form-data", "application/json"})
	public SysUserAccount createSysUserAccount(
			@org.springframework.web.bind.annotation.RequestParam(value = "userData", required = false) String userDataJson,
			@org.springframework.web.bind.annotation.RequestParam(value = "profilePicture", required = false) org.springframework.web.multipart.MultipartFile profilePicture,
			jakarta.servlet.http.HttpServletRequest request) {
		// Handle multipart request
		if (userDataJson == null) {
			throw new RuntimeException("No user data provided");
		}
		
		System.out.println("=== MULTIPART REQUEST FROM FRONTEND ===");
		System.out.println("UserData JSON: " + userDataJson);
		System.out.println("Profile Picture: " + (profilePicture != null ? profilePicture.getOriginalFilename() : "None"));
		
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		java.util.Map<String, Object> requestData;
		try {
			requestData = mapper.readValue(userDataJson, java.util.Map.class);
		} catch (Exception e) {
			System.out.println("Error parsing userData JSON: " + e.getMessage());
			throw new RuntimeException("Invalid userData format");
		}
		
		System.out.println("=== PARSED REQUEST DATA ===");
		for (java.util.Map.Entry<String, Object> entry : requestData.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}
		System.out.println("==============================");
		
		// Convert to request object
		SysUserAccountCreateRequest userAccountCreateRequest;
		try {
			userAccountCreateRequest = mapper.convertValue(requestData, SysUserAccountCreateRequest.class);
		} catch (Exception e) {
			System.out.println("Error converting request: " + e.getMessage());
			throw new RuntimeException("Invalid request format");
		}

		System.out.println("=== USER REGISTRATION DATA FROM FRONTEND ===");
		System.out.println("Username: " + userAccountCreateRequest.getUsername());
		System.out.println("First Name: " + userAccountCreateRequest.getFirstName());
		System.out.println("Last Name: " + userAccountCreateRequest.getLastName());
		System.out.println("Email: " + userAccountCreateRequest.getEmailAddress());
		System.out.println("District ID: " + userAccountCreateRequest.getDistrictId());
		System.out.println("Phone Number: " + userAccountCreateRequest.getPhoneNumber());
		System.out.println("Postal Address: " + userAccountCreateRequest.getPostalAddress());
		System.out.println("Designation: " + userAccountCreateRequest.getDesignation());
		System.out.println("Company Reg Number: " + userAccountCreateRequest.getCompanyRegistrationNumber());
		System.out.println("Company Registered Name: " + userAccountCreateRequest.getCompanyRegisteredName());
		System.out.println("Company Trading Name: " + userAccountCreateRequest.getCompanyTradingName());
		System.out.println("National ID: " + userAccountCreateRequest.getNationalId());
		System.out.println("Passport Number: " + userAccountCreateRequest.getPassportNumber());
		System.out.println("Salutation ID: " + userAccountCreateRequest.getSalutationId());
		System.out.println("Customer Type ID: " + userAccountCreateRequest.getCustomerTypeId());
		System.out.println("Passport Country ID: " + userAccountCreateRequest.getPassportCountryId());
		System.out.println("Password provided: " + (userAccountCreateRequest.getPassword() != null && !userAccountCreateRequest.getPassword().isEmpty()));
		System.out.println("===============================================");
		
		try {
			SysUserAccount userAccount = new SysUserAccount();
			BeanUtils.copyProperties(userAccountCreateRequest, userAccount);
			Timestamp currentTime = new Timestamp(new Date().getTime());
			userAccount.setDateCreated(currentTime);
			
			// Set district if provided
			if (userAccountCreateRequest.getDistrictId() != null && !userAccountCreateRequest.getDistrictId().trim().isEmpty()) {
				mw.nwra.ewaterpermit.model.CoreDistrict district = new mw.nwra.ewaterpermit.model.CoreDistrict();
				district.setId(userAccountCreateRequest.getDistrictId());
				userAccount.setCoreDistrict(district);
			}
			
			// Set salutation if provided
			if (userAccountCreateRequest.getSalutationId() != null && !userAccountCreateRequest.getSalutationId().trim().isEmpty()) {
				mw.nwra.ewaterpermit.model.SysSalutation salutation = new mw.nwra.ewaterpermit.model.SysSalutation();
				salutation.setId(userAccountCreateRequest.getSalutationId());
				userAccount.setSysSalutation(salutation);
			}
			
			// Set additional fields
			userAccount.setPhoneNumber(userAccountCreateRequest.getPhoneNumber());
			userAccount.setPostalAddress(userAccountCreateRequest.getPostalAddress());
			userAccount.setDesignation(userAccountCreateRequest.getDesignation());
			userAccount.setCompanyRegistrationNumber(userAccountCreateRequest.getCompanyRegistrationNumber());
			userAccount.setCompanyRegisteredName(userAccountCreateRequest.getCompanyRegisteredName());
			userAccount.setCompanyTradingName(userAccountCreateRequest.getCompanyTradingName());
			userAccount.setNationalId(userAccountCreateRequest.getNationalId());
			userAccount.setPassportNumber(userAccountCreateRequest.getPassportNumber());
			
			// Handle profile picture upload
			System.out.println("=== PROFILE PICTURE PROCESSING ===");
			System.out.println("Profile picture received: " + (profilePicture != null));
			if (profilePicture != null) {
				System.out.println("Profile picture empty: " + profilePicture.isEmpty());
				System.out.println("Profile picture size: " + profilePicture.getSize());
				System.out.println("Profile picture name: " + profilePicture.getOriginalFilename());
			}
			
			if (profilePicture != null && !profilePicture.isEmpty()) {
				try {
					// Create uploads directory in project root
					String projectRoot = System.getProperty("user.dir");
					String uploadDirPath = projectRoot + "/uploads/profile-pictures";
					java.nio.file.Path uploadDir = java.nio.file.Paths.get(uploadDirPath);
					if (!java.nio.file.Files.exists(uploadDir)) {
						java.nio.file.Files.createDirectories(uploadDir);
						System.out.println("Created upload directory: " + uploadDir.toString());
					}
					
					// Generate unique filename
					String originalFilename = profilePicture.getOriginalFilename();
					String fileExtension = originalFilename != null && originalFilename.contains(".") 
						? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
					String filename = "profile_" + java.util.UUID.randomUUID().toString() + fileExtension;
					
					// Save file
					java.nio.file.Path filePath = uploadDir.resolve(filename);
					profilePicture.transferTo(filePath.toFile());
					
					// Set profile picture path in user account
					String profilePhotoPath = "uploads/profile-pictures/" + filename;
					userAccount.setProfilePhoto(profilePhotoPath);
					System.out.println("Profile picture saved to: " + filePath.toString());
					System.out.println("Profile photo path set to: " + profilePhotoPath);
				} catch (Exception e) {
					System.err.println("Failed to save profile picture: " + e.getMessage());
					e.printStackTrace();
					// Continue registration without profile picture
				}
			} else {
				System.out.println("No profile picture to process");
			}
			System.out.println("Profile photo field before save: " + userAccount.getProfilePhoto());
			System.out.println("===================================");
			
			// Handle customer type and passport country from request data
			if (requestData.containsKey("coreCustomerType")) {
				Object customerTypeObj = requestData.get("coreCustomerType");
				if (customerTypeObj instanceof String) {
					System.out.println("Setting customer type ID: " + customerTypeObj);
					// Note: customer_type_id field needs to be added to SysUserAccount model
				} else if (customerTypeObj instanceof java.util.Map) {
					@SuppressWarnings("unchecked")
					java.util.Map<String, Object> customerType = (java.util.Map<String, Object>) customerTypeObj;
					if (customerType.get("id") != null) {
						System.out.println("Setting customer type ID: " + customerType.get("id"));
					}
				}
			}
			
			if (requestData.containsKey("passportCountry")) {
				Object passportCountryObj = requestData.get("passportCountry");
				if (passportCountryObj instanceof String) {
					System.out.println("Setting passport country ID: " + passportCountryObj);
				} else if (passportCountryObj instanceof java.util.Map) {
					@SuppressWarnings("unchecked")
					java.util.Map<String, Object> passportCountry = (java.util.Map<String, Object>) passportCountryObj;
					if (passportCountry.get("id") != null) {
						System.out.println("Setting passport country ID: " + passportCountry.get("id"));
					}
				}
			}
			
			if (userAccount.getPassword() != null && !userAccount.getPassword().trim().isEmpty()) {
				// hash password supplied
				userAccount.setPassword(PasswordHash.hashPassword(userAccount.getPassword()));
			} else {
				throw new ForbiddenException("Could not create the user account. Password unavailable.");
			}
			
			SysAccountStatus status = this.accountStatusService
					.getSysAccountStatusByName(SysAccountStatusValue.CONFIRM_ACCOUNT.toString());
			if (status == null) {
				throw new EntityNotFoundException("Could not find the proper account status");
			}
			userAccount.setSysAccountStatus(status);

			// Fetch Applicant user group dynamically by name (case-insensitive)
			SysUserGroup applicantGroup = this.sysUserGroupService.getSysUserGroupByNameIgnoreCase("applicant");
			if (applicantGroup == null) {
				throw new EntityNotFoundException("Could not find 'Applicant'");
			}
			userAccount.setSysUserGroup(applicantGroup);
			
			// Ensure date_created is set before saving
			userAccount.setDateCreated(currentTime);

			System.out.println("=== SAVING USER ACCOUNT ===");
			System.out.println("Profile photo before save: " + userAccount.getProfilePhoto());
			SysUserAccount createdSysUserAccount = this.userAccountService.createSysUserAccount(userAccount);
			auditor.audit(Action.CREATE, "UserAccount", createdSysUserAccount.getId(), createdSysUserAccount, "User registered");
			System.out.println("Profile photo after save: " + createdSysUserAccount.getProfilePhoto());
			System.out.println("============================");

			// Get frontend URL dynamically from request headers
			String registrationHost = request.getHeader("Origin");
			if (registrationHost == null || registrationHost.trim().isEmpty()) {
				registrationHost = request.getHeader("Referer");
				if (registrationHost != null && registrationHost.contains("/")) {
					// Extract base URL from Referer (e.g., http://localhost:4200/auth/signup -> http://localhost:4200)
					int thirdSlash = registrationHost.indexOf("/", 8);
					if (thirdSlash > 0) {
						registrationHost = registrationHost.substring(0, thirdSlash);
					}
				}
			}

			System.out.println("=== FRONTEND URL DETECTION ===");
			System.out.println("Origin Header: " + request.getHeader("Origin"));
			System.out.println("Referer Header: " + request.getHeader("Referer"));
			System.out.println("Detected Frontend URL: " + registrationHost);
			System.out.println("==============================");
			
			System.out.println("=== REQUEST HEADERS ===");
			System.out.println("Origin: " + request.getHeader("Origin"));
			System.out.println("Referer: " + request.getHeader("Referer"));
			System.out.println("Host: " + request.getHeader("Host"));
			System.out.println("Final Registration Host: " + registrationHost);
			System.out.println("========================");
			
			SysUserAccountActivation userActivation = AppUtil.createSysUserAccountActivation(createdSysUserAccount, registrationHost);
			userActivation = this.userAccountActivationService.addSysUserAccountActivation(userActivation);

			// Send the confirmation mail - if email fails, rollback entire transaction
			System.out.println("=== ATTEMPTING TO SEND EMAIL ===");
			System.out.println("Email: " + userActivation.getSysUserAccount().getEmailAddress());
			System.out.println("Token: " + userActivation.getToken());
			System.out.println("Registration Host: " + registrationHost);

			boolean emailSent = this.mailService.send(SysTemplateName.ACCOUNT_CONFIRMATION.toString(), userActivation.getToken(),
					userActivation.getSysUserAccount(), registrationHost);
			System.out.println("Email sent successfully: " + emailSent);
			System.out.println("=================================");
			
			return AppUtil.sanitizeSysUserAccount(createdSysUserAccount);
			
		} catch (Exception e) {
			System.err.println("=== REGISTRATION FAILED - ROLLING BACK ===");
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.err.println("==========================================");

			// Check if it's an email-related failure
			if (e.getMessage() != null && (e.getMessage().contains("email") || e.getMessage().contains("mail"))) {
				throw new RuntimeException("Registration failed: Unable to send confirmation email. Please check email configuration.", e);
			}

			// Re-throw to trigger transaction rollback
			throw new RuntimeException("Registration failed: " + e.getMessage(), e);
		}
	}

	@GetMapping(path = "/{token}/confirm-account")
	public org.springframework.web.servlet.ModelAndView confirmAccount(@PathVariable(name = "token") String token,
			jakarta.servlet.http.HttpServletRequest request) {
		try {
			SysUserAccountActivation userActivation = this.verifyToken(token);
			SysAccountStatus status = this.accountStatusService
					.getSysAccountStatusByName(SysAccountStatusValue.ACTIVE.toString());
			if (status == null) {
				throw new EntityNotFoundException("Could not find the proper account status");
			}
			// get the user again since the user from token verification is sanitized
			SysUserAccount userAccount = userActivation.getSysUserAccount();
			// Fetch the complete user account to preserve date_created
			SysUserAccount fullUserAccount = this.userAccountService.getSysUserAccountById(userAccount.getId());
			if (fullUserAccount != null) {
				fullUserAccount.setSysAccountStatus(status);
				fullUserAccount.setDateUpdated(new Timestamp(new Date().getTime()));
				userAccount = this.userAccountService.updateSysUserAccount(fullUserAccount);
			} else {
				// Fallback: ensure date_created is preserved
				if (userAccount.getDateCreated() == null) {
					userAccount.setDateCreated(new Timestamp(new Date().getTime()));
				}
				userAccount.setSysAccountStatus(status);
				userAccount.setDateUpdated(new Timestamp(new Date().getTime()));
				userAccount = this.userAccountService.updateSysUserAccount(userAccount);
			}

			// update userAccount Activation
			userActivation.setSysUserAccount(userAccount);
			userActivation.setDateActivated(userAccount.getDateUpdated());

			this.userAccountActivationService.editSysUserAccountActivation(userActivation);
			
			// Account confirmation notification disabled
			
			// Redirect to the frontend verification success page using the registration host
			String registrationHost = userActivation.getRegistrationHost();
			if (registrationHost != null && !registrationHost.trim().isEmpty()) {
				// Redirect to the host where user registered
				try {
					String encodedEmail = java.net.URLEncoder.encode(userAccount.getEmailAddress(), "UTF-8");
					String redirectUrl = registrationHost + "/verification-success?status=success&email=" + encodedEmail;
					return new org.springframework.web.servlet.ModelAndView("redirect:" + redirectUrl);
				} catch (java.io.UnsupportedEncodingException e) {
					// Fallback without encoding
					String redirectUrl = registrationHost + "/verification-success?status=success";
					return new org.springframework.web.servlet.ModelAndView("redirect:" + redirectUrl);
				}
			} else {
				// Fallback to default success page
				return new org.springframework.web.servlet.ModelAndView("redirect:/verification-success?status=success");
			}
		} catch (Exception e) {
			// Get registration host for error redirect
			String errorRedirectHost = "https://nwra-frontend-srws.onrender.com"; // Production fallback
			try {
				SysUserAccountActivation userActivation = this.userAccountActivationService
						.getSysUserAccountActivationByToken(token);
				if (userActivation != null && userActivation.getRegistrationHost() != null) {
					errorRedirectHost = userActivation.getRegistrationHost();
				}
			} catch (Exception ignored) {
				// Use default fallback
			}
			
			// Redirect to error page with proper host
			try {
				String errorMessage = java.net.URLEncoder.encode(e.getMessage(), "UTF-8");
				String redirectUrl = errorRedirectHost + "/verification-error?status=error&message=" + errorMessage;
				return new org.springframework.web.servlet.ModelAndView("redirect:" + redirectUrl);
			} catch (java.io.UnsupportedEncodingException ex) {
				String redirectUrl = errorRedirectHost + "/verification-error?status=error";
				return new org.springframework.web.servlet.ModelAndView("redirect:" + redirectUrl);
			}
		}
	}

	@PostMapping(path = "/reset-password")
	public SysUserAccount resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
		SysUserAccountActivation userActivation = this.verifyToken(resetPasswordRequest.getToken());

		// in case the user resort to reset password after forgetting credentials before
		// activating the account or after deactivating account, set active again
		SysAccountStatus status = this.accountStatusService
				.getSysAccountStatusByName(SysAccountStatusValue.ACTIVE.toString());
		if (status == null) {
			throw new EntityNotFoundException("Could not find the proper account status");
		}
		// get the user again since the user from token verification is sanitized
		SysUserAccount userAccount = userActivation.getSysUserAccount();
		// Fetch the complete user account to preserve date_created
		SysUserAccount fullUserAccount = this.userAccountService.getSysUserAccountById(userAccount.getId());
		if (fullUserAccount != null) {
			fullUserAccount.setSysAccountStatus(status);
			if (resetPasswordRequest.getPassword() != null) {
				// if new password supplied
				fullUserAccount.setPassword(PasswordHash.hashPassword(resetPasswordRequest.getPassword()));
			}
			fullUserAccount.setDateUpdated(new Timestamp(new Date().getTime()));
			userAccount = this.userAccountService.updateSysUserAccount(fullUserAccount);
		} else {
			// Fallback: ensure date_created is preserved
			if (userAccount.getDateCreated() == null) {
				userAccount.setDateCreated(new Timestamp(new Date().getTime()));
			}
			userAccount.setSysAccountStatus(status);
			if (resetPasswordRequest.getPassword() != null) {
				// if new password supplied
				userAccount.setPassword(PasswordHash.hashPassword(resetPasswordRequest.getPassword()));
			}
			userAccount.setDateUpdated(new Timestamp(new Date().getTime()));
			userAccount = this.userAccountService.updateSysUserAccount(userAccount);
			auditor.audit(Action.UPDATE, "UserAccount", userAccount.getId(), userAccount, "Password reset");
		}

		// update userAccount Activation
		userActivation.setSysUserAccount(userAccount);
		userActivation.setDateActivated(userAccount.getDateUpdated());

		this.userAccountActivationService.editSysUserAccountActivation(userActivation);
		
		// Create password reset notification
		createPasswordResetNotification(userAccount);
		
		return AppUtil.sanitizeSysUserAccount(userAccount);
	}

	@GetMapping(path = "/{emailAddress}/reset-password")
	public SysUserAccount processPasswordResetRequest(@PathVariable(name = "emailAddress") String emailAddress,
			jakarta.servlet.http.HttpServletRequest request) {
		SysUserAccount userAccount = this.userAccountService.getSysUserAccountByEmailAddress(emailAddress);
		if (userAccount == null) {
			throw new EntityNotFoundException("User account with email '" + emailAddress + "' does not exist");
		}
		SysUserAccountActivation userActivation = AppUtil.createSysUserAccountActivation(userAccount);
		userActivation = this.userAccountActivationService.addSysUserAccountActivation(userActivation);

		// Get frontend URL dynamically from request headers (same logic as registration)
		String frontendUrl = request.getHeader("Origin");
		if (frontendUrl == null || frontendUrl.trim().isEmpty()) {
			frontendUrl = request.getHeader("Referer");
			if (frontendUrl != null && frontendUrl.contains("/")) {
				// Extract base URL from Referer
				int thirdSlash = frontendUrl.indexOf("/", 8);
				if (thirdSlash > 0) {
					frontendUrl = frontendUrl.substring(0, thirdSlash);
				}
			}
		}

		System.out.println("=== PASSWORD RESET REQUEST ===");
		System.out.println("Email: " + emailAddress);
		System.out.println("Token: " + userActivation.getToken());
		System.out.println("Frontend URL: " + frontendUrl);
		System.out.println("==============================");

		// Send the reset password mail with frontend URL
		try {
			boolean emailSent = this.mailService.send(SysTemplateName.PASSWORD_RESET.toString(), userActivation.getToken(),
					userActivation.getSysUserAccount(), frontendUrl);
			System.out.println("Password reset email sent: " + emailSent);
		} catch (Exception e) {
			System.err.println("Failed to send password reset email: " + e.getMessage());
			e.printStackTrace();
		}
		return AppUtil.sanitizeSysUserAccount(userAccount);
	}

	@GetMapping(path = "/verify-reset-token/{token}")
	public Map<String, Object> verifyResetToken(@PathVariable(name = "token") String token) {
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			SysUserAccountActivation userActivation = this.verifyToken(token);
			result.put("valid", true);
			result.put("message", "Token is valid");
			// Include user info for the reset page
			if (userActivation.getSysUserAccount() != null) {
				result.put("email", userActivation.getSysUserAccount().getEmailAddress());
				result.put("firstName", userActivation.getSysUserAccount().getFirstName());
			}
			return result;
		} catch (EntityNotFoundException e) {
			result.put("valid", false);
			result.put("message", e.getMessage());
			return result;
		} catch (Exception e) {
			result.put("valid", false);
			result.put("message", "Token validation failed");
			return result;
		}
	}

	@GetMapping(path = "/{username}/username")
	public Map<String, Object> usernameExists(@PathVariable(name = "username") String username) {
		SysUserAccount userAccount = this.userAccountService.getSysUserAccountByUsername(username);
		if (userAccount != null) {
			throw new DataIntegrityViolationException("User account with username '" + username + "' already exists");
		}
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("username", username);
		results.put("exists", false);
		return results;
	}

	@GetMapping(path = "/{emailAddress}/email-address")
	public Map<String, Object> remailExists(@PathVariable(name = "emailAddress") String emailAddress) {
		// Clean email address - remove mailto: prefix if present
		if (emailAddress.startsWith("mailto:")) {
			emailAddress = emailAddress.substring(7);
		}
		
		// check temporary mail
		SysDisposableDomain disposableDomain = this.disposableDomainService
				.getSysDisposableDomainByName(emailAddress.split("@")[1]);
		if (disposableDomain != null) {
			throw new ForbiddenException("Disposable email address not allowed");
		}

		SysUserAccount userAccount = this.userAccountService.getSysUserAccountByEmailAddress(emailAddress);
		if (userAccount != null) {
			throw new ForbiddenException("User account with email '" + emailAddress + "' already exists");
		}
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("emailAddress", emailAddress);
		results.put("exists", false);
		return results;
	}

	@GetMapping(path = "/{emailAddress}/temp-mail")
	public Map<String, Object> isTempMail(@PathVariable(name = "emailAddress") String emailAddress) {
		// check temporary mail
		SysDisposableDomain disposableDomain = this.disposableDomainService
				.getSysDisposableDomainByName(emailAddress.split("@")[1]);
		if (disposableDomain != null) {
			throw new ForbiddenException("Disposable email address not allowed");
		}
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("emailAddress", emailAddress);
		results.put("exists", false);
		return results;
	}

	@GetMapping(path = "/{phoneNumber}/phone-number")
	public Map<String, Object> mobileNumberExists(@PathVariable(name = "phoneNumber") String phoneNumber) {
		SysUserAccount userAccount = this.userAccountService.getSysUserAccountByPhoneNumber(phoneNumber);
		if (userAccount != null) {
			throw new DataIntegrityViolationException(
					"User account with phone number '" + phoneNumber + "' already exists");
		}
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("phoneNumber", phoneNumber);
		results.put("exists", false);
		return results;
	}

	private SysUserAccountActivation verifyToken(String token) {
		SysUserAccountActivation userActivation = this.userAccountActivationService
				.getSysUserAccountActivationByTokenAndDate(token, new Timestamp(new Date().getTime()));
		if (userActivation == null) {
			throw new EntityNotFoundException("Invalid request or the verification period has expired.");
		}
		return userActivation;
	}

	@PostMapping(path = "/send-otp")
	public Map<String, Object> sendVerificationMessage(@RequestBody OTPRequest request) {
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("status", this.sendOTP(request, null));
		return results;
	}

	@PostMapping(path = "/verify-otp")
	public Map<String, Object> verifyUser(@RequestBody OTPVerifyRequest request) {
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("status", this.verifyOTP(request));
		return results;
	}

	public String sendOTP(OTPRequest request, SysUserAccount userAccount) {
		if (userAccount == null) {
			userAccount = new SysUserAccount();
			userAccount.setFirstName("Customer");
			userAccount.setLastName("");
			userAccount.setEmailAddress(request.getPayload());
		}

		SysUserAccountActivation userActivation = AppUtil.createSysUserAccountActivation(userAccount);
		SysUserAccountActivation tmpActivation = userActivation;
		tmpActivation.setSysUserAccount(null);
		tmpActivation.setEmailAddress(userAccount.getEmailAddress());
		tmpActivation = this.userAccountActivationService.addSysUserAccountActivation(tmpActivation);
		// lastly send the reset password mail
		boolean msgSent = this.mailService.send(TemplateName.TOKEN_VERIFICATION.toString(), userActivation.getToken(),
				userAccount);
		return msgSent ? "pending" : "failed"; // failed
	}

	public String verifyOTP(OTPVerifyRequest request) {
		SysUserAccountActivation userActivation = this.verifyToken(request.getOtp(), request.getPayload());
		if (userActivation != null) {
			userActivation.setDateActivated(new Timestamp(new Date().getTime()));
			this.userAccountActivationService.editSysUserAccountActivation(userActivation);
		}
		return userActivation != null ? "approved" : "failed";
	}

	public SysUserAccountActivation verifyToken(String token, String emailAddress) {
		SysUserAccountActivation userActivation = this.userAccountActivationService
				.getUserAccountActivationByTokenAndDateAndEmail(token, new Timestamp(new Date().getTime()),
						emailAddress);
		if (userActivation == null) {
			throw new EntityNotFoundException("Invalid request or the verification period has expired.");
		}
		if (userActivation.getSysUserAccount() != null)
			userActivation.setSysUserAccount(AppUtil.sanitizeSysUserAccount(userActivation.getSysUserAccount()));
		return userActivation;
	}

	private void createLoginNotification(SysUserAccount userAccount) {
		try {
			UserNotification notification = new UserNotification();
			notification.setUserId(userAccount.getId());
			notification.setTitle("Successful Login");
			notification.setMessage("You have successfully logged into your account from a new session.");
			notification.setType(NotificationType.SUCCESS);
			notification.setCategory(NotificationCategory.SYSTEM);
			notification.setPriority(NotificationPriority.LOW);
			notification.setActionUrl("/e-services");
			notification.setActionLabel("Go to Dashboard");
			
			notificationService.createNotification(notification);
		} catch (Exception e) {
			System.err.println("Failed to create login notification: " + e.getMessage());
		}
	}

	private void createPasswordResetNotification(SysUserAccount userAccount) {
		try {
			UserNotification notification = new UserNotification();
			notification.setUserId(userAccount.getId());
			notification.setTitle("Password Reset Successful");
			notification.setMessage("Your password has been successfully reset. If this wasn't you, please contact support immediately.");
			notification.setType(NotificationType.SUCCESS);
			notification.setCategory(NotificationCategory.SYSTEM);
			notification.setPriority(NotificationPriority.HIGH);
			notification.setActionUrl("/profile/security");
			notification.setActionLabel("View Security Settings");
			
			notificationService.createNotification(notification);
		} catch (Exception e) {
			System.err.println("Failed to create password reset notification: " + e.getMessage());
		}
	}

	private void createAccountConfirmationNotification(SysUserAccount userAccount) {
		try {
			UserNotification notification = new UserNotification();
			notification.setUserId(userAccount.getId());
			notification.setTitle("Welcome! Account Activated");
			notification.setMessage("Your account has been successfully activated. You can now access all features of the NWRA system.");
			notification.setType(NotificationType.SUCCESS);
			notification.setCategory(NotificationCategory.SYSTEM);
			notification.setPriority(NotificationPriority.MEDIUM);
			notification.setActionUrl("/e-services");
			notification.setActionLabel("Explore Dashboard");
			
			notificationService.createNotification(notification);
		} catch (Exception e) {
			System.err.println("Failed to create account confirmation notification: " + e.getMessage());
		}
	}
}
