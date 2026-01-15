package mw.nwra.ewaterpermit.service;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.dto.LicenseDataDto;
import mw.nwra.ewaterpermit.dto.LicenseListDto;
import mw.nwra.ewaterpermit.dto.LicenseTransferInfoDto;
import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.repository.CoreLicenseRepository;
import mw.nwra.ewaterpermit.service.CoreApplicationPaymentService;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreLicenseService")
public class CoreLicenseServiceImpl implements CoreLicenseService {
	@Autowired
	CoreLicenseRepository repo;
	
	@Autowired
	CoreApplicationPaymentService paymentService;

	@Override
	public List<CoreLicense> getAllCoreLicenses() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreLicense> getAllCoreLicenses(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreLicense getCoreLicenseById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public List<CoreLicense> getCoreLicensesByLicenseNumber(String number) {
		return this.repo.findByLicenseNumberOrderByDateCreatedDesc(number);
	}

	@Override
	public List<CoreLicense> getActiveLicensesByLicenseNumber(String number) {
		return this.repo.findByLicenseNumberAndStatusOrderByDateCreatedDesc(number, "ACTIVE");
	}

	@Override
	public void deleteCoreLicense(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLicense addCoreLicense(CoreLicense coreLicense) {
		return this.repo.saveAndFlush(coreLicense);
	}

	@Override
	public CoreLicense editCoreLicense(CoreLicense coreLicense) {
		return this.repo.saveAndFlush(coreLicense);
	}

	@Override
	public Long count() {
		return this.repo.count();
	}

	@Override
	public Long countByStatus(String status) {
		return this.repo.countByStatus(status);
	}

	@Override
	public Long countLicensesExpiringInDays(int days) {
		return this.repo.countLicensesExpiringInDays(days);
	}

	@Override
	public List<CoreLicense> getActiveLicensesByUserId(String userId) {
		// Temporary implementation - will be fixed once database relationships are confirmed
		return List.of();
	}

	@Override
	public List<CoreLicense> getExpiringLicensesByUserId(String userId, int daysAhead) {
		// Temporary implementation - will be fixed once database relationships are confirmed
		return List.of();
	}

	@Override
	public List<CoreLicense> getExpiredLicensesByUserId(String userId) {
		// Temporary implementation - will be fixed once database relationships are confirmed
		return List.of();
	}

	@Override
	public Long countActiveLicensesByUserId(String userId) {
		return this.repo.countActiveLicensesByUserId(userId);
	}

	@Override
	public List<LicenseListDto> getAllLicensesForManager() {
		List<Object[]> results = this.repo.findAllLicensesForManager();
		return results.stream().map(this::mapToLicenseListDto).collect(Collectors.toList());
	}

	@Override
	public List<LicenseListDto> getActiveLicensesByOwnerId(String ownerId) {
		List<Object[]> results = this.repo.findActiveLicensesByOwnerId(ownerId);
		return results.stream().map(this::mapToLicenseListDtoForApplicant).collect(Collectors.toList());
	}

	@Override
	public List<LicenseListDto> getAllLicensesForManagerByLicenseType(String licenseTypeId) {
		List<Object[]> results = this.repo.findAllLicensesForManagerByLicenseType(licenseTypeId);
		return results.stream().map(this::mapToLicenseListDto).collect(Collectors.toList());
	}

	@Override
	public List<LicenseListDto> getActiveLicensesByOwnerIdAndLicenseType(String ownerId, String licenseTypeId) {
		System.out.println("=== DEBUGGING LICENSE QUERY ===");
		System.out.println("Original licenseTypeId: " + licenseTypeId);

		// Check if this is a renewal type and get all related license type IDs
		List<String> searchLicenseTypeIds = findAllRelatedLicenseTypeIds(licenseTypeId);
		System.out.println("Searching for license types: " + searchLicenseTypeIds);

		System.out.println("Searching for licenses with ownerId: " + ownerId + " and licenseTypeIds: " + searchLicenseTypeIds);
		List<Object[]> results = this.repo.findActiveLicensesByOwnerIdAndLicenseTypes(ownerId, searchLicenseTypeIds);
		System.out.println("Found " + results.size() + " results");

		if (results.isEmpty()) {
			System.out.println("NO RESULTS FOUND - Check database for:");
			System.out.println("1. Applications with owner_id = " + ownerId);
			System.out.println("2. Applications with license_type_id in " + searchLicenseTypeIds);
			System.out.println("3. Licenses with status = 'ACTIVE'");
		} else {
			System.out.println("Results found:");
			for (int i = 0; i < results.size(); i++) {
				Object[] row = results.get(i);
				System.out.println("Result " + (i+1) + ": License ID = " + row[0] + ", License Number = " + row[1] + ", Status = " + row[4] + ", Type = " + row[8]);
			}
		}
		System.out.println("=== END DEBUGGING ===");

		return results.stream().map(this::mapToLicenseListDtoForApplicant).collect(Collectors.toList());
	}

	private LicenseListDto mapToLicenseListDto(Object[] row) {
		LicenseListDto dto = new LicenseListDto();
		dto.setId((String) row[0]);
		dto.setLicenseNumber((String) row[1]);
		dto.setDateIssued((Date) row[2]);
		dto.setExpirationDate((Date) row[3]);
		dto.setStatus((String) row[4]);
		dto.setDateUpdated((Date) row[5]);
		// Handle both Integer and BigInteger for license_version
		if (row[6] != null) {
			if (row[6] instanceof Integer) {
				dto.setLicenseVersion((Integer) row[6]);
			} else if (row[6] instanceof BigInteger) {
				dto.setLicenseVersion(((BigInteger) row[6]).intValue());
			} else {
				dto.setLicenseVersion(1);
			}
		} else {
			dto.setLicenseVersion(1);
		}
		dto.setParentLicenseId((String) row[7]);
		dto.setLicenseTypeName((String) row[8]);
		dto.setApplicationStatusName((String) row[9]);
		dto.setFirstName((String) row[10]);
		dto.setLastName((String) row[11]);
		dto.setEmailAddress((String) row[12]);

		// Map assessment data
		dto.setCalculatedAnnualRental(row[13] != null ? (java.math.BigDecimal) row[13] : null);
		dto.setRentalQuantity(row[14] != null ? (java.math.BigDecimal) row[14] : null);
		dto.setRentalRate(row[15] != null ? (java.math.BigDecimal) row[15] : null);
		dto.setRecommendedScheduleDate(row[16] != null ? (Date) row[16] : null);
		dto.setAssessmentNotes((String) row[17]);
		dto.setLicenseOfficerId((String) row[18]);
		dto.setAssessmentStatus((String) row[19]);
		dto.setAssessmentFilesUpload((String) row[20]);

		// Map application data for license display
		dto.setLocationInfo((String) row[21]);
		dto.setApplicationMetadata((String) row[22]);
		dto.setFormSpecificData((String) row[23]);

		return dto;
	}

	private LicenseListDto mapToLicenseListDtoForApplicant(Object[] row) {
		LicenseListDto dto = new LicenseListDto();
		dto.setId((String) row[0]);
		dto.setLicenseNumber((String) row[1]);
		dto.setDateIssued((Date) row[2]);
		dto.setExpirationDate((Date) row[3]);
		dto.setStatus((String) row[4]);
		dto.setDateUpdated((Date) row[5]);
		// Handle both Integer and BigInteger for license_version
		if (row[6] != null) {
			if (row[6] instanceof Integer) {
				dto.setLicenseVersion((Integer) row[6]);
			} else if (row[6] instanceof BigInteger) {
				dto.setLicenseVersion(((BigInteger) row[6]).intValue());
			} else {
				dto.setLicenseVersion(1);
			}
		} else {
			dto.setLicenseVersion(1);
		}
		dto.setParentLicenseId((String) row[7]);
		dto.setLicenseTypeName((String) row[8]);
		dto.setApplicationStatusName((String) row[9]);
		return dto;
	}

	@Override
	public LicenseDataDto getLicenseDataById(String licenseId) {
		try {
			System.out.println("Getting license data for license: " + licenseId);
			
			// Get license data directly without payment validation for now
			List<Object[]> results = this.repo.findLicenseDataById(licenseId);
			System.out.println("Query result: " + (results != null ? results.toString() : "null"));
			
			if (results == null || results.isEmpty()) {
				return new LicenseDataDto("License not found", false, false);
			}
			
			Object[] result = results.get(0);
			System.out.println("First result array: " + java.util.Arrays.toString(result));
			
			LicenseDataDto dto = mapToLicenseDataDto(result);
			System.out.println("Mapped DTO: permitNumber=" + dto.getPermitNumber() + ", applicantName=" + dto.getApplicantName());
			return dto;
		} catch (Exception e) {
			System.out.println("Error in getLicenseDataById: " + e.getMessage());
			return new LicenseDataDto("Error loading license data: " + e.getMessage(), false, false);
		}
	}

	private LicenseDataDto mapToLicenseDataDto(Object[] row) {
		try {
			LicenseDataDto dto = new LicenseDataDto();
			dto.setPermitNumber(row[0] != null ? row[0].toString() : null);
			dto.setApplicantName(row[1] != null ? row[1].toString() : null);
			dto.setSourceOwnerFullname(row[2] != null ? row[2].toString() : null);
			dto.setIssueDate(row[3] != null ? (Date) row[3] : null);
			dto.setExpiryDate(row[4] != null ? (Date) row[4] : null);
			dto.setDirectorName(row[5] != null ? row[5].toString() : null);
			dto.setContactPhone(row[6] != null ? row[6].toString() : null);
			dto.setContactEmail(row[7] != null ? row[7].toString() : null);
			dto.setConditionsText(row[8] != null ? row[8].toString() : null);
			
			// Check if this is a transfer license (assuming license type is in row[13])
			String licenseType = row[13] != null ? row[13].toString() : null;
			boolean isTransfer = licenseType != null && licenseType.toUpperCase().contains("TRANSFER");
			
			// Handle payment and verification status safely
			boolean paymentRequired = false;
			boolean verificationPending = false;
			
			System.out.println("Row[9] (payment_required): " + row[9] + " (type: " + (row[9] != null ? row[9].getClass().getSimpleName() : "null") + ")");
			System.out.println("Row[10] (verification_pending): " + row[10] + " (type: " + (row[10] != null ? row[10].getClass().getSimpleName() : "null") + ")");
			System.out.println("License type: " + licenseType + ", isTransfer: " + isTransfer);
			
			// For transfer licenses, treat as paid (no payment required)
			if (isTransfer) {
				paymentRequired = false;
				verificationPending = false;
				System.out.println("Transfer license detected - bypassing payment requirements");
			} else {
				if (row[9] != null) {
					if (row[9] instanceof BigInteger) {
						paymentRequired = ((BigInteger) row[9]).intValue() == 1;
					} else if (row[9] instanceof Integer) {
						paymentRequired = ((Integer) row[9]) == 1;
					} else if (row[9] instanceof Long) {
						paymentRequired = ((Long) row[9]).intValue() == 1;
					}
				}
				
				if (row[10] != null) {
					if (row[10] instanceof BigInteger) {
						verificationPending = ((BigInteger) row[10]).intValue() == 1;
					} else if (row[10] instanceof Integer) {
						verificationPending = ((Integer) row[10]) == 1;
					} else if (row[10] instanceof Long) {
						verificationPending = ((Long) row[10]).intValue() == 1;
					}
				}
			}
			
			System.out.println("Calculated paymentRequired: " + paymentRequired);
			System.out.println("Calculated verificationPending: " + verificationPending);
			
			// Set payment information from query results
			dto.setPaymentStatus(row[11] != null ? row[11].toString() : null);
			if (row[12] != null) {
				if (row[12] instanceof Double) {
					dto.setAmountPaid((Double) row[12]);
				} else if (row[12] instanceof Number) {
					dto.setAmountPaid(((Number) row[12]).doubleValue());
				}
			}
			
			if (paymentRequired) {
				dto.setLicenseAvailable(false);
				dto.setPaymentRequired(true);
				dto.setMessage("Payment required for license access");
			} else if (verificationPending) {
				dto.setLicenseAvailable(false);
				dto.setVerificationPending(true);
				dto.setMessage("Awaiting accountant verification for license access");
			} else {
				dto.setLicenseAvailable(true);
				dto.setPaymentRequired(false);
				dto.setVerificationPending(false);
				if (isTransfer) {
					dto.setMessage("Transfer license - no payment required");
				}
			}
			
			return dto;
		} catch (Exception e) {
			LicenseDataDto errorDto = new LicenseDataDto();
			errorDto.setMessage("Error mapping license data: " + e.getMessage());
			errorDto.setLicenseAvailable(false);
			return errorDto;
		}
	}

	public LicenseTransferInfoDto getLicenseTransferInfo(String originalLicenseId) {
		try {
			List<Object[]> results = this.repo.findTransferInfoByOriginalLicenseId(originalLicenseId);
			
			if (results == null || results.isEmpty()) {
				return null; // No transfer found for this license
			}
			
			Object[] result = results.get(0);
			return mapToLicenseTransferInfoDto(result);
		} catch (Exception e) {
			System.out.println("Error in getLicenseTransferInfo: " + e.getMessage());
			return null;
		}
	}

	private LicenseTransferInfoDto mapToLicenseTransferInfoDto(Object[] row) {
		// Query returns: first_name, last_name, email_address, phone_number, new_license_number, transfer_date
		LicenseTransferInfoDto dto = new LicenseTransferInfoDto();
		dto.setFirstName(row[0] != null ? row[0].toString() : null);
		dto.setLastName(row[1] != null ? row[1].toString() : null);
		dto.setEmailAddress(row[2] != null ? row[2].toString() : null);
		dto.setPhoneNumber(row[3] != null ? row[3].toString() : null);
		dto.setNewLicenseNumber(row[4] != null ? row[4].toString() : null);
		dto.setTransferDate(row[5] != null ? (Date) row[5] : null);
		return dto;
	}

	@Override
	public String findOriginalLicenseTypeIdForRenewal(String licenseTypeId) {
		try {
			// First get the license type name
			System.out.println("Looking up license type name for ID: " + licenseTypeId);
			String licenseTypeName = this.repo.findLicenseTypeNameById(licenseTypeId);

			if (licenseTypeName == null) {
				System.out.println("License type not found for ID: " + licenseTypeId);
				return licenseTypeId;
			}

			System.out.println("License type name: " + licenseTypeName);

			// Check if this is a renewal type
			if (licenseTypeName.startsWith("Renewal ")) {
				String originalTypeId = this.repo.findOriginalLicenseTypeIdForRenewal(licenseTypeName);
				if (originalTypeId != null) {
					System.out.println("Found original license type ID: " + originalTypeId);
					return originalTypeId;
				} else {
					System.out.println("No matching original license type found for: " + licenseTypeName);
				}
			} else {
				System.out.println("Not a renewal type, using original ID");
			}

			return licenseTypeId; // Return original if not a renewal type or mapping fails
		} catch (Exception e) {
			System.out.println("Error finding original license type: " + e.getMessage());
			return licenseTypeId; // Return original if mapping fails
		}
	}

	private List<String> findAllRelatedLicenseTypeIds(String licenseTypeId) {
		try {
			// Get the license type name for the provided ID
			String licenseTypeName = this.repo.findLicenseTypeNameById(licenseTypeId);
			if (licenseTypeName == null) {
				return List.of(licenseTypeId);
			}

			System.out.println("Finding all related types for: " + licenseTypeName);

			// Extract the base type (e.g., "Borehole Construction Permit" from both "New" and "Renewal" variants)
			String baseType = extractBaseType(licenseTypeName);
			System.out.println("Base type: " + baseType);

			// Find all license types that match this base type
			List<String> relatedTypeIds = this.repo.findLicenseTypesByBaseType(baseType);
			System.out.println("Found related license types: " + relatedTypeIds);

			return relatedTypeIds.isEmpty() ? List.of(licenseTypeId) : relatedTypeIds;

		} catch (Exception e) {
			System.out.println("Error finding related license types: " + e.getMessage());
			return List.of(licenseTypeId);
		}
	}

	private String extractBaseType(String licenseTypeName) {
		System.out.println("=== EXTRACT BASE TYPE ===");
		System.out.println("Input license type name: " + licenseTypeName);

		String baseType = licenseTypeName;

		// Handle prefixes first - remove action type prefixes to get the base type
		if (licenseTypeName.startsWith("New ")) {
			baseType = licenseTypeName.substring(4); // "New Surface Water License" → "Surface Water License"
			System.out.println("Detected 'New' prefix, base type: " + baseType);
		} else if (licenseTypeName.startsWith("Renewal of ")) {
			baseType = licenseTypeName.substring(11); // "Renewal of Surface Water License" → "Surface Water License"
			System.out.println("Detected 'Renewal of' prefix, base type: " + baseType);
		} else if (licenseTypeName.startsWith("Renewal ")) {
			baseType = licenseTypeName.substring(8); // "Renewal Surface Water License" → "Surface Water License"
			System.out.println("Detected 'Renewal' prefix, base type: " + baseType);
		} else if (licenseTypeName.startsWith("Transfer ")) {
			baseType = licenseTypeName.substring(9); // "Transfer Surface Water License" → "Surface Water License"
			System.out.println("Detected 'Transfer' prefix, base type: " + baseType);
		}
		// Handle suffixes - remove action type suffixes to get the base type
		else if (licenseTypeName.endsWith(" variation")) {
			baseType = licenseTypeName.substring(0, licenseTypeName.length() - 10); // "Surface Water License variation" → "Surface Water License"
			System.out.println("Detected 'variation' suffix, base type: " + baseType);
		} else {
			System.out.println("No known prefix/suffix detected, using original: " + baseType);
		}

		System.out.println("Final base type: " + baseType);
		System.out.println("=== END EXTRACT BASE TYPE ===");
		return baseType;
	}

	@Override
	public List<CoreLicense> findActiveLicensesExpiredByDate(java.sql.Date expiryDate) {
		return this.repo.findActiveLicensesExpiredByDate(expiryDate);
	}

	@Override
	public List<CoreLicense> findLicensesExpiringIn3Months() {
		return this.repo.findLicensesExpiringIn3Months();
	}

	@Override
	public List<CoreLicense> findLicensesExpiringIn2Months() {
		return this.repo.findLicensesExpiringIn2Months();
	}

	@Override
	public List<CoreLicense> findLicensesExpiringIn1Month() {
		return this.repo.findLicensesExpiringIn1Month();
	}

	@Override
	public List<CoreLicense> findLicensesExpiringIn1Week() {
		return this.repo.findLicensesExpiringIn1Week();
	}
}
