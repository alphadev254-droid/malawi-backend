package mw.nwra.ewaterpermit.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
import mw.nwra.ewaterpermit.dto.ApplicationDocumentSummaryDTO;
import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreApplicationDocument;
import mw.nwra.ewaterpermit.model.CoreApplicationPayment;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;
import mw.nwra.ewaterpermit.repository.CoreLicenseApplicationRepository;
import mw.nwra.ewaterpermit.service.CoreApplicationDocumentService;
import mw.nwra.ewaterpermit.service.CoreApplicationPaymentService;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1/application-documents")
public class CoreApplicationDocumentController {
	private final String PREFIX = "doc_";
	private final String OBJ = "application-documents";
	@Autowired
	private CoreApplicationDocumentService documentService;
	@Autowired
	private CoreApplicationPaymentService paymentService;
	@Autowired
	private CoreLicenseApplicationService applicationService;
	@Autowired
	private CoreLicenseApplicationRepository applicationRepository;
	@Autowired
	private Auditor auditor;

	// Import for grouping
	private static final java.util.function.Function<mw.nwra.ewaterpermit.model.CoreApplicationPayment, String> GROUP_BY_APPLICATION =
		payment -> payment.getCoreLicenseApplication() != null ? payment.getCoreLicenseApplication().getId() : "unknown";

	@GetMapping(path = "")
	public SearchResponse getAllCoreApplicationDocuments(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "query", defaultValue = "") String query) {
		SearchResponse res = this.documentService.getAllCoreApplicationDocuments(page, limit, query);
		if (res == null) {
			throw new EntityNotFoundException("Documents not found");
		}
		@SuppressWarnings("unchecked")
		List<CoreApplicationDocument> documents = (List<CoreApplicationDocument>) res.getData();
		res.setData(documents);
		return res;
	}

	@GetMapping(path = "/{id}")
	public CoreApplicationDocument getCoreApplicationDocumentById(@PathVariable(name = "id") String documentId) {
		CoreApplicationDocument document = this.documentService.getCoreApplicationDocumentById(documentId);
		if (document == null) {
			throw new EntityNotFoundException("Document not found");
		}
		return document;
	}

	@GetMapping(path = "/application/{id}")
	public List<CoreApplicationDocument> getCoreApplicationDocumentByApplication(
			@PathVariable(name = "id") String documentId,
			@RequestParam(name = "receiptDocumentId", required = false) String receiptDocumentId) {
		CoreLicenseApplication appl = new CoreLicenseApplication();
		appl.setId(documentId);
		List<CoreApplicationDocument> documents = this.documentService.getCoreApplicationDocumentByApplication(appl);
		if (documents == null) {
			throw new EntityNotFoundException("Documents not found");
		}

		// If receiptDocumentId is provided, filter to return only that document
		if (receiptDocumentId != null && !receiptDocumentId.isEmpty()) {
			return documents.stream()
					.filter(doc -> receiptDocumentId.equals(doc.getId()))
					.toList();
		}

		return documents;
	}

	@PostMapping(path = "")
	public CoreApplicationDocument createCoreApplicationDocument(@RequestBody Map<String, Object> documentRequest,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.CREATE.toString());

		CoreApplicationDocument document = (CoreApplicationDocument) AppUtil
				.objectToClass(CoreApplicationDocument.class, documentRequest);
		if (document == null) {
			throw new ForbiddenException("Could not create the document");
		}
//		if (document.getDocumentUrl() != null && AppUtil.isBase64File(document.getDocumentUrl())) {
//			String category = document.getCoreApplicationDocumentCategory().getName() + "_";
//			document.setDocumentUrl(AppUtil.uploadFile(this.PREFIX + category, document.getDocumentUrl()));
//		}
//		document.setSysUserAccount(ua);
		document.setDateCreated(new Timestamp(new Date().getTime()));
		CoreApplicationDocument result = this.documentService.addCoreApplicationDocument(document);
		
		// Audit log
		auditor.audit(Action.CREATE, "ApplicationDocument", result.getId(), ua, "Created application document");
		
		return result;
	}

	@PutMapping(path = "/{id}")
	public CoreApplicationDocument updateCoreApplicationDocument(@PathVariable(name = "id") String id,
			@RequestBody Map<String, Object> documentRequest, @RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.UPDATE.toString());

		CoreApplicationDocument document = this.documentService.getCoreApplicationDocumentById(id);
		if (document == null) {
			throw new EntityNotFoundException("Document not found");
		}
		String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreApplicationDocument.class, documentRequest);
		CoreApplicationDocument documentFromObj = (CoreApplicationDocument) AppUtil
				.objectToClass(CoreApplicationDocument.class, documentRequest);
		if (documentFromObj == null) {
			throw new ForbiddenException("Could not update the document document");
		}
		if (documentFromObj.getDocumentUrl() != null && AppUtil.isBase64File(documentFromObj.getDocumentUrl())) {
			if (document.getDocumentUrl() != null) {
				AppUtil.deleteUploadedFile(document.getDocumentUrl());
			}
			String category = document.getCoreDocumentCategory().getName() + "_";
			documentFromObj
					.setDocumentUrl(AppUtil.uploadFile(this.PREFIX + category, documentFromObj.getDocumentUrl()));
		}
		if (documentFromObj.getDocumentUrl() == null && document.getDocumentUrl() != null) {
			AppUtil.deleteUploadedFile(document.getDocumentUrl());
		}
		BeanUtils.copyProperties(documentFromObj, document, propertiesToIgnore);
		document.setDateUpdated(new Timestamp(new Date().getTime()));
		this.documentService.editCoreApplicationDocument(document);
		
		// Audit log
		auditor.audit(Action.UPDATE, "ApplicationDocument", document.getId(), ua, "Updated application document");
		
		return document;
	}

	@DeleteMapping(path = "/{id}")
	public CoreApplicationDocument deleteCoreApplicationDocument(@PathVariable(name = "id") String id,
			@RequestHeader(name = "Authorization") String token) {
		SysUserAccount ua = AppUtil.getLoggedInUser(token);
		AppUtil.can(AppUtil.getUserPermissions(ua), this.OBJ, Action.DELETE.toString());

		CoreApplicationDocument document = this.documentService.getCoreApplicationDocumentById(id);
		if (document == null) {
			throw new EntityNotFoundException("Document with id '" + id + "' not found");
		}
		this.documentService.deleteCoreApplicationDocument(document.getId());

		// delete from the file system
		AppUtil.deleteUploadedFile(document.getDocumentUrl());
		
		// Audit log
		auditor.audit(Action.DELETE, "ApplicationDocument", document.getId(), ua, "Deleted application document");
		
		return document;
	}

	@PostMapping(path = "/upload")
	public ResponseEntity<?> uploadCoreApplicationDocument(@RequestBody Map<String, Object> data) {
		String url = null;
		if (AppUtil.isBase64File(data.get("data").toString())) {
			url = AppUtil.uploadFile("document_", data.get("data").toString());
		}
		return ResponseEntity.ok(url);
	}

	@GetMapping(path = "/summary")
	public SearchResponse getDocumentsSummary(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "query", defaultValue = "") String query,
			@RequestHeader(name = "Authorization") String token) {

		SysUserAccount loggedInUser = AppUtil.getLoggedInUser(token);
		String userRole = loggedInUser.getSysUserGroup().getName().toLowerCase();

		// Get all payments with optimized query (eager fetching from payment table)
		SearchResponse response = this.paymentService.getAllPaymentsWithDetails(page, limit, query);

		if (response == null || response.getData() == null) {
			return new SearchResponse(0L, new ArrayList<>());
		}

		@SuppressWarnings("unchecked")
		List<CoreApplicationPayment> payments = (List<CoreApplicationPayment>) response.getData();

		// Filter: If applicant, show only their payments; others see all
		List<CoreApplicationPayment> filteredPayments = payments.stream()
			.filter(payment -> {
				if ("applicant".equals(userRole)) {
					return payment.getCoreLicenseApplication() != null &&
						   payment.getCoreLicenseApplication().getSysUserAccount() != null &&
						   payment.getCoreLicenseApplication().getSysUserAccount().getId().equals(loggedInUser.getId());
				}
				return true; // Non-applicants see all
			})
			.collect(Collectors.toList());

		// Group payments by application and convert to summary DTOs
		java.util.Map<String, List<CoreApplicationPayment>> paymentsByApplication = filteredPayments.stream()
			.collect(Collectors.groupingBy(GROUP_BY_APPLICATION));

		List<ApplicationDocumentSummaryDTO> summaries = paymentsByApplication.values().stream()
			.map(appPayments -> convertPaymentsToSummaryDTO(appPayments))
			.collect(Collectors.toList());

		return new SearchResponse((long) summaries.size(), summaries);
	}

	@GetMapping(path = "/full-application/{applicationId}")
	public ResponseEntity<?> getFullApplicationDetails(
			@PathVariable(name = "applicationId") String applicationId,
			@RequestHeader(name = "Authorization") String token) {

		SysUserAccount loggedInUser = AppUtil.getLoggedInUser(token);
		String userRole = loggedInUser.getSysUserGroup().getName().toLowerCase();

		// Fetch application using simple query without loading relationships
		CoreLicenseApplication application = applicationRepository.findByIdSimple(applicationId)
			.orElseThrow(() -> new EntityNotFoundException("Application not found"));

		// If applicant, only allow viewing their own application
		if ("applicant".equals(userRole)) {
			// Need to check owner - but this will trigger lazy load, so let's use a query
			String ownerId = application.getSysUserAccount() != null ? application.getSysUserAccount().getId() : null;
			if (ownerId == null || !ownerId.equals(loggedInUser.getId())) {
				throw new ForbiddenException("You can only view your own applications");
			}
		}

		// Return the application entity - contains all form data fields
		return ResponseEntity.ok(application);
	}

	private ApplicationDocumentSummaryDTO convertPaymentsToSummaryDTO(List<CoreApplicationPayment> payments) {
		if (payments == null || payments.isEmpty()) {
			return null;
		}

		// Get the first payment to access application details (all payments have same application)
		CoreApplicationPayment firstPayment = payments.get(0);
		CoreLicenseApplication application = firstPayment.getCoreLicenseApplication();

		if (application == null) {
			return null;
		}

		ApplicationDocumentSummaryDTO dto = new ApplicationDocumentSummaryDTO();

		// Set document info to null or use first payment's receipt if needed
		dto.setDocumentId(firstPayment.getReceiptDocumentId());
		dto.setDocumentUrl(null); // We'll fetch this only if needed
		dto.setStatus(firstPayment.getPaymentStatus());
		dto.setDateCreated(firstPayment.getDateCreated() != null ? firstPayment.getDateCreated().toString() : null);
		dto.setDocumentCategory("Payment Receipt");

		// Applicant info (NO password)
		if (application.getSysUserAccount() != null) {
			SysUserAccount user = application.getSysUserAccount();
			ApplicationDocumentSummaryDTO.ApplicantInfoDTO applicant =
				new ApplicationDocumentSummaryDTO.ApplicantInfoDTO(
					user.getId(),
					user.getFirstName(),
					user.getLastName(),
					user.getEmailAddress()
				);
			dto.setApplicant(applicant);
		}

		// Application basic info
		ApplicationDocumentSummaryDTO.ApplicationBasicInfoDTO appInfo =
			new ApplicationDocumentSummaryDTO.ApplicationBasicInfoDTO();
		appInfo.setId(application.getId());
		appInfo.setDateSubmitted(application.getDateSubmitted() != null ?
			application.getDateSubmitted().toString() : null);
		appInfo.setApplicationType(application.getApplicationType());
		appInfo.setBoardMinutes(application.getBoardMinutes());
		appInfo.setBoardApprovalDate(application.getBoardApprovalDate() != null ?
			application.getBoardApprovalDate().toString() : null);
		appInfo.setBoardMinutesDocument(application.getBoardMinutesDocument());
		dto.setApplication(appInfo);

		// License type info
		if (application.getCoreLicenseType() != null) {
			ApplicationDocumentSummaryDTO.LicenseTypeInfoDTO licenseType =
				new ApplicationDocumentSummaryDTO.LicenseTypeInfoDTO();
			licenseType.setId(application.getCoreLicenseType().getId());
			licenseType.setName(application.getCoreLicenseType().getName());
			licenseType.setApplicationFees(application.getCoreLicenseType().getApplicationFees());
			licenseType.setLicenseFees(application.getCoreLicenseType().getLicenseFees());
			dto.setLicenseType(licenseType);
		}

		// Application status
		if (application.getCoreApplicationStatus() != null) {
			dto.setApplicationStatus(new ApplicationDocumentSummaryDTO.ApplicationStatusInfoDTO(
				application.getCoreApplicationStatus().getName()
			));
		}

		// Application step
		if (application.getCoreApplicationStep() != null) {
			ApplicationDocumentSummaryDTO.ApplicationStepInfoDTO step =
				new ApplicationDocumentSummaryDTO.ApplicationStepInfoDTO();
			step.setName(application.getCoreApplicationStep().getName());
			Byte seqNum = application.getCoreApplicationStep().getSequenceNumber();
			step.setSequenceNumber(seqNum != null ? seqNum.intValue() : null);
			dto.setApplicationStep(step);
		}

		// Convert all payments for this application
		List<ApplicationDocumentSummaryDTO.PaymentDocumentDTO> paymentDocs = payments.stream()
			.map(payment -> {
				ApplicationDocumentSummaryDTO.PaymentDocumentDTO paymentDoc =
					new ApplicationDocumentSummaryDTO.PaymentDocumentDTO();
				paymentDoc.setPaymentId(payment.getId());
				paymentDoc.setFeeType(payment.getCoreFeesType() != null ?
					payment.getCoreFeesType().getName() : null);
				paymentDoc.setAmount(payment.getAmountPaid());
				paymentDoc.setPaymentStatus(payment.getPaymentStatus());
				paymentDoc.setPaymentMethod(payment.getPaymentMethod());
				paymentDoc.setReceiptDocumentId(payment.getReceiptDocumentId());
				paymentDoc.setDateCreated(payment.getDateCreated() != null ?
					payment.getDateCreated().toString() : null);
				// Note: We don't fetch receiptDocumentUrl here to avoid N+1, frontend uses receipt document ID
				return paymentDoc;
			})
			.collect(Collectors.toList());
		dto.setPaymentDocuments(paymentDocs);

		return dto;
	}

	private ApplicationDocumentSummaryDTO convertToSummaryDTO(CoreApplicationDocument document) {
		ApplicationDocumentSummaryDTO dto = new ApplicationDocumentSummaryDTO();

		// Document info
		dto.setDocumentId(document.getId());
		dto.setDocumentUrl(document.getDocumentUrl());
		dto.setStatus(document.getStatus());
		dto.setDateCreated(document.getDateCreated() != null ? document.getDateCreated().toString() : null);
		dto.setDocumentCategory(document.getCoreDocumentCategory() != null ?
			document.getCoreDocumentCategory().getName() : null);

		CoreLicenseApplication application = document.getCoreLicenseApplication();
		if (application != null) {
			// Applicant info (NO password)
			if (application.getSysUserAccount() != null) {
				SysUserAccount user = application.getSysUserAccount();
				ApplicationDocumentSummaryDTO.ApplicantInfoDTO applicant =
					new ApplicationDocumentSummaryDTO.ApplicantInfoDTO(
						user.getId(),
						user.getFirstName(),
						user.getLastName(),
						user.getEmailAddress()
					);
				dto.setApplicant(applicant);
			}

			// Application basic info
			ApplicationDocumentSummaryDTO.ApplicationBasicInfoDTO appInfo =
				new ApplicationDocumentSummaryDTO.ApplicationBasicInfoDTO();
			appInfo.setId(application.getId());
			appInfo.setDateSubmitted(application.getDateSubmitted() != null ?
				application.getDateSubmitted().toString() : null);
			appInfo.setApplicationType(application.getApplicationType());
			// appInfo.setBoardMinutes(application.getBoardl());
			appInfo.setBoardApprovalDate(application.getBoardApprovalDate() != null ?
				application.getBoardApprovalDate().toString() : null);
			appInfo.setBoardMinutesDocument(application.getBoardMinutesDocument());
			dto.setApplication(appInfo);

			// License type info
			if (application.getCoreLicenseType() != null) {
				ApplicationDocumentSummaryDTO.LicenseTypeInfoDTO licenseType =
					new ApplicationDocumentSummaryDTO.LicenseTypeInfoDTO();
				licenseType.setId(application.getCoreLicenseType().getId());
				licenseType.setName(application.getCoreLicenseType().getName());
				licenseType.setApplicationFees(application.getCoreLicenseType().getApplicationFees());
				licenseType.setLicenseFees(application.getCoreLicenseType().getLicenseFees());
				dto.setLicenseType(licenseType);
			}

			// Application status
			if (application.getCoreApplicationStatus() != null) {
				dto.setApplicationStatus(new ApplicationDocumentSummaryDTO.ApplicationStatusInfoDTO(
					application.getCoreApplicationStatus().getName()
				));
			}

			// Application step
			if (application.getCoreApplicationStep() != null) {
				ApplicationDocumentSummaryDTO.ApplicationStepInfoDTO step =
					new ApplicationDocumentSummaryDTO.ApplicationStepInfoDTO();
				step.setName(application.getCoreApplicationStep().getName());
				// Convert byte to Integer
				Byte seqNum = application.getCoreApplicationStep().getSequenceNumber();
				step.setSequenceNumber(seqNum != null ? seqNum.intValue() : null);
				dto.setApplicationStep(step);
			}

			// Payment documents
			List<CoreApplicationPayment> payments = paymentService.getByLicence(application);
			if (payments != null && !payments.isEmpty()) {
				List<ApplicationDocumentSummaryDTO.PaymentDocumentDTO> paymentDocs = payments.stream()
					.map(payment -> {
						ApplicationDocumentSummaryDTO.PaymentDocumentDTO paymentDoc =
							new ApplicationDocumentSummaryDTO.PaymentDocumentDTO();
						paymentDoc.setPaymentId(payment.getId());
						paymentDoc.setFeeType(payment.getCoreFeesType() != null ?
							payment.getCoreFeesType().getName() : null);
						paymentDoc.setAmount(payment.getAmountPaid());
						paymentDoc.setPaymentStatus(payment.getPaymentStatus());
						paymentDoc.setPaymentMethod(payment.getPaymentMethod());
						paymentDoc.setReceiptDocumentId(payment.getReceiptDocumentId());
						paymentDoc.setDateCreated(payment.getDateCreated() != null ?
							payment.getDateCreated().toString() : null);

						// Get receipt document URL
						if (payment.getReceiptDocumentId() != null) {
							try {
								CoreApplicationDocument receiptDoc =
									documentService.getCoreApplicationDocumentById(payment.getReceiptDocumentId());
								if (receiptDoc != null) {
									paymentDoc.setReceiptDocumentUrl(receiptDoc.getDocumentUrl());
								}
							} catch (Exception e) {
								// Skip if document not found
							}
						}

						return paymentDoc;
					})
					.collect(Collectors.toList());
				dto.setPaymentDocuments(paymentDocs);
			}
		}

		return dto;
	}
}
