package mw.nwra.ewaterpermit.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mw.nwra.ewaterpermit.model.CoreLicenseWaterUse;
import mw.nwra.ewaterpermit.model.CoreWaterUse;

@Repository
public interface CoreLicenseWaterUseRepository extends JpaRepository<CoreLicenseWaterUse, String> {

	CoreLicenseWaterUse findByCoreWaterUse(CoreWaterUse waterUse);

	@Query("SELECT SUM(lwu.amountPerDayM3) FROM CoreLicenseWaterUse lwu " +
			"JOIN lwu.coreLicenseApplication la " +
			"WHERE (la.sourceWru.id = :unitId OR la.destWru.id = :unitId) " +
			"AND la.coreApplicationStatus.name IN ('APPROVED', 'ACTIVE', 'SUBMITTED')")
	Double getTotalAllAbstractionByWaterResourceUnit(@Param("unitId") String unitId);

	@Query("SELECT COUNT(DISTINCT la.id) FROM CoreLicenseWaterUse lwu " +
			"JOIN lwu.coreLicenseApplication la " +
			"WHERE (la.sourceWru.id = :unitId OR la.destWru.id = :unitId) " +
			"AND la.coreApplicationStatus.name IN ('APPROVED', 'ACTIVE', 'SUBMITTED')")
	Long countActivePermitsByWaterResourceUnit(@Param("unitId") String unitId);

	@Query("SELECT SUM(CAST(JSON_EXTRACT(la.formSpecificData, '$.maxDailyQuantity') AS DOUBLE)) " +
			"FROM CoreLicenseApplication la " +
			"WHERE (la.sourceWru.id = :unitId OR la.destWru.id = :unitId) " +
			"AND la.coreLicenseType.name LIKE '%Effluent%Discharge%' " +
			"AND la.coreApplicationStatus.name IN ('APPROVED', 'ACTIVE', 'SUBMITTED') " +
			"AND la.formSpecificData IS NOT NULL " +
			"AND JSON_EXTRACT(la.formSpecificData, '$.maxDailyQuantity') IS NOT NULL")
	Double getTotalDischargeByWaterResourceUnit(@Param("unitId") String unitId);

	@Query("SELECT COUNT(DISTINCT la.id) " +
			"FROM CoreLicenseApplication la " +
			"WHERE (la.sourceWru.id = :unitId OR la.destWru.id = :unitId) " +
			"AND la.coreLicenseType.name LIKE '%Effluent%Discharge%' " +
			"AND la.coreApplicationStatus.name IN ('APPROVED', 'ACTIVE', 'SUBMITTED')")
	Long countActiveDischargePermitsByWaterResourceUnit(@Param("unitId") String unitId);

	@Query("SELECT SUM(lwu.amountPerDayM3) FROM CoreLicenseWaterUse lwu " +
			"JOIN lwu.coreLicenseApplication la " +
			"WHERE (la.coreLicenseType.name LIKE '%Surface%Water%' OR la.coreLicenseType.name LIKE '%Ground%Water%') " +
			"AND la.coreApplicationStatus.name IN ('APPROVED', 'ACTIVE', 'SUBMITTED')")
	Double getTotalNationalAbstraction();

	@Query("SELECT SUM(lwu.amountPerDayM3) FROM CoreLicenseWaterUse lwu " +
			"JOIN lwu.coreLicenseApplication la " +
			"WHERE (la.sourceWru.id = :unitId OR la.destWru.id = :unitId) " +
			"AND (la.coreLicenseType.name LIKE '%Surface%Water%' OR la.coreLicenseType.name LIKE '%Ground%Water%') " +
			"AND la.coreApplicationStatus.name IN ('APPROVED', 'ACTIVE', 'SUBMITTED')")
	Double getTotalAbstractionByWaterResourceUnit(@Param("unitId") String unitId);

	@Query("SELECT COUNT(DISTINCT la.id) FROM CoreLicenseWaterUse lwu " +
			"JOIN lwu.coreLicenseApplication la " +
			"WHERE (la.sourceWru.id = :unitId OR la.destWru.id = :unitId) " +
			"AND (la.coreLicenseType.name LIKE '%Surface%Water%' OR la.coreLicenseType.name LIKE '%Ground%Water%') " +
			"AND la.coreApplicationStatus.name IN ('APPROVED', 'ACTIVE', 'SUBMITTED')")
	Long countActiveAbstractionPermitsByWaterResourceUnit(@Param("unitId") String unitId);

	@Query("SELECT SUM(CAST(JSON_EXTRACT(la.formSpecificData, '$.maxDailyQuantity') AS DOUBLE)) " +
			"FROM CoreLicenseApplication la " +
			"WHERE la.coreLicenseType.name LIKE '%Effluent%Discharge%' " +
			"AND la.coreApplicationStatus.name IN ('APPROVED', 'ACTIVE', 'SUBMITTED') " +
			"AND la.formSpecificData IS NOT NULL " +
			"AND JSON_EXTRACT(la.formSpecificData, '$.maxDailyQuantity') IS NOT NULL")
	Double getTotalNationalDischarge();

	@Query("SELECT COUNT(DISTINCT la.id) FROM CoreLicenseWaterUse lwu " +
			"JOIN lwu.coreLicenseApplication la " +
			"WHERE la.coreApplicationStatus.name = 'SUBMITTED'")
	Long getTotalApprovedLicenses();

	@Query("SELECT COUNT(DISTINCT la.id) FROM CoreLicenseWaterUse lwu " +
			"JOIN lwu.coreLicenseApplication la " +
			"WHERE (la.sourceWru.id = :unitId OR la.destWru.id = :unitId) " +
			"AND la.coreApplicationStatus.name = 'SUBMITTED'")
	Long getApprovedLicensesByWaterResourceUnit(@Param("unitId") String unitId);

	@Query(value = "SELECT SUM(clt.license_fees) FROM core_license cl " +
			"JOIN core_license_application la ON la.id = cl.license_application_id " +
			"JOIN core_license_type clt ON clt.id = la.license_type_id " +
			"JOIN core_application_payment cp ON cp.license_application_id = cl.license_application_id " +
			"JOIN core_fees_type cft ON cft.id = cp.fees_type_id " +
			"WHERE cl.status = 'ACTIVE' " +
			"AND cp.payment_status != 'PAID' " +
			"AND cft.name = 'License fees'", nativeQuery = true)
	Double getTotalUnpaidDebt();

	@Query(value = "SELECT SUM(clt.license_fees) FROM core_license cl " +
			"JOIN core_license_application la ON la.id = cl.license_application_id " +
			"JOIN core_license_type clt ON clt.id = la.license_type_id " +
			"JOIN core_application_payment cp ON cp.license_application_id = cl.license_application_id " +
			"JOIN core_fees_type cft ON cft.id = cp.fees_type_id " +
			"WHERE cl.status = 'ACTIVE' " +
			"AND cp.payment_status != 'PAID' " +
			"AND cft.name = 'License fees' " +
			"AND (la.source_wru = :unitId OR la.dest_wru = :unitId)", nativeQuery = true)
	Double getUnpaidDebtByWaterResourceUnit(@Param("unitId") String unitId);

	@Query(value = "SELECT COUNT(DISTINCT cl.id) FROM core_license cl " +
			"JOIN core_license_application la ON la.id = cl.license_application_id " +
			"JOIN core_application_payment cp ON cp.license_application_id = cl.license_application_id " +
			"JOIN core_fees_type cft ON cft.id = cp.fees_type_id " +
			"WHERE cl.status = 'ACTIVE' " +
			"AND cp.payment_status != 'PAID' " +
			"AND cft.name = 'License fees' " +
			"AND (la.source_wru = :unitId OR la.dest_wru = :unitId)", nativeQuery = true)
	Long getUnpaidPaymentCountByWaterResourceUnit(@Param("unitId") String unitId);

	@Query(value = "SELECT SUM(clt.license_fees) FROM core_license cl " +
			"JOIN core_license_application la ON la.id = cl.license_application_id " +
			"JOIN core_license_type clt ON clt.id = la.license_type_id " +
			"JOIN core_application_payment cp ON cp.license_application_id = cl.license_application_id " +
			"JOIN core_fees_type cft ON cft.id = cp.fees_type_id " +
			"WHERE cl.status = 'ACTIVE' " +
			"AND cp.payment_status = 'PAID' " +
			"AND cft.name = 'License fees'", nativeQuery = true)
	Double getTotalLicenseRevenue();

	@Query(value = "SELECT SUM(clt.license_fees) FROM core_license cl " +
			"JOIN core_license_application la ON la.id = cl.license_application_id " +
			"JOIN core_license_type clt ON clt.id = la.license_type_id " +
			"JOIN core_application_payment cp ON cp.license_application_id = cl.license_application_id " +
			"JOIN core_fees_type cft ON cft.id = cp.fees_type_id " +
			"WHERE cl.status = 'ACTIVE' " +
			"AND cp.payment_status = 'PAID' " +
			"AND cft.name = 'License fees' " +
			"AND (la.source_wru = :unitId OR la.dest_wru = :unitId)", nativeQuery = true)
	Double getLicenseRevenueByWaterResourceUnit(@Param("unitId") String unitId);

	@Query(value = "SELECT COUNT(DISTINCT cl.id) FROM core_license cl " +
			"JOIN core_license_application la ON la.id = cl.license_application_id " +
			"JOIN core_application_payment cp ON cp.license_application_id = cl.license_application_id " +
			"JOIN core_fees_type cft ON cft.id = cp.fees_type_id " +
			"WHERE cl.status = 'ACTIVE' " +
			"AND cp.payment_status = 'PAID' " +
			"AND cft.name = 'License fees' " +
			"AND (la.source_wru = :unitId OR la.dest_wru = :unitId)", nativeQuery = true)
	Long getPaidLicenseCountByWaterResourceUnit(@Param("unitId") String unitId);

	@Query(value = "SELECT la.id as applicationId, la.user_account_id as userAccountId, lwu.amount_per_day_m3 as dailyUsage, " +
			"la.license_type_id as licenseTypeId, la.date_created as applicationDate, " +
			"sua.username, sua.phone_number, sua.email_address " +
			"FROM core_license_application la " +
			"JOIN core_license_water_use lwu ON lwu.license_application_id = la.id " +
			"JOIN sys_user_account sua ON sua.id = la.user_account_id " +
			"WHERE la.application_status_id IN (SELECT id FROM core_application_status WHERE name IN ('APPROVED', 'ACTIVE')) " +
			"ORDER BY lwu.amount_per_day_m3 DESC " +
			"LIMIT :limit", nativeQuery = true)
	List<Object[]> getLargestWaterUsers(@Param("limit") int limit);

	@Query(value = "SELECT la.id as applicationId, la.user_account_id as userAccountId, " +
			"CAST(JSON_EXTRACT(la.form_specific_data, '$.maxDailyQuantity') AS DECIMAL(10,2)) as dailyDischarge, " +
			"la.license_type_id as licenseTypeId, la.date_created as applicationDate, " +
			"sua.username, sua.phone_number, sua.email_address " +
			"FROM core_license_application la " +
			"JOIN sys_user_account sua ON sua.id = la.user_account_id " +
			"JOIN core_license_type clt ON clt.id = la.license_type_id " +
			"WHERE clt.name LIKE '%Effluent%Discharge%' " +
			"AND la.application_status_id IN (SELECT id FROM core_application_status WHERE name IN ('APPROVED', 'ACTIVE', 'SUBMITTED')) " +
			"AND la.form_specific_data IS NOT NULL " +
			"AND JSON_EXTRACT(la.form_specific_data, '$.maxDailyQuantity') IS NOT NULL " +
			"ORDER BY CAST(JSON_EXTRACT(la.form_specific_data, '$.maxDailyQuantity') AS DECIMAL(10,2)) DESC " +
			"LIMIT :limit", nativeQuery = true)
	List<Object[]> getLargestDischargers(@Param("limit") int limit);

	@Query(value = "SELECT la.id as applicationId, la.user_account_id as userAccountId, " +
			"clt.license_fees as debtAmount, la.license_type_id as licenseTypeId, " +
			"la.date_created as applicationDate, sua.username, sua.phone_number, sua.email_address " +
			"FROM core_license cl " +
			"JOIN core_license_application la ON la.id = cl.license_application_id " +
			"JOIN core_license_type clt ON clt.id = la.license_type_id " +
			"JOIN core_application_payment cp ON cp.license_application_id = cl.license_application_id " +
			"JOIN core_fees_type cft ON cft.id = cp.fees_type_id " +
			"JOIN sys_user_account sua ON sua.id = la.user_account_id " +
			"WHERE cl.status = 'ACTIVE' " +
			"AND cp.payment_status != 'PAID' " +
			"AND cft.name = 'License fees' " +
			"ORDER BY clt.license_fees DESC " +
			"LIMIT :limit", nativeQuery = true)
	List<Object[]> getLargestDebtHolders(@Param("limit") int limit);

	@Query(value = "SELECT la.id as applicationId, la.user_account_id as userAccountId, " +
			"clt.license_fees as revenueAmount, la.license_type_id as licenseTypeId, " +
			"la.date_created as applicationDate, sua.username, sua.phone_number, sua.email_address, " +
			"clt.license_fees as debtAmount " +
			"FROM core_license cl " +
			"JOIN core_license_application la ON la.id = cl.license_application_id " +
			"JOIN core_license_type clt ON clt.id = la.license_type_id " +
			"JOIN core_application_payment cp ON cp.license_application_id = cl.license_application_id " +
			"JOIN core_fees_type cft ON cft.id = cp.fees_type_id " +
			"JOIN sys_user_account sua ON sua.id = la.user_account_id " +
			"WHERE cl.status = 'ACTIVE' " +
			"AND cp.payment_status = 'PAID' " +
			"AND cft.name = 'License fees' " +
			"ORDER BY clt.license_fees DESC " +
			"LIMIT :limit", nativeQuery = true)
	List<Object[]> getLargestRevenueLicences(@Param("limit") int limit);

	@Query(value = "SELECT area.id as areaId, COUNT(DISTINCT la.id) as approvedLicenses " +
			"FROM core_water_resource_area area " +
			"LEFT JOIN core_water_resource_unit unit ON unit.water_resource_area_id = area.id " +
			"LEFT JOIN core_license_application la ON (la.source_wru = unit.id OR la.dest_wru = unit.id) " +
			"WHERE la.status = 'SUBMITTED' OR la.id IS NULL " +
			"GROUP BY area.id", nativeQuery = true)
	List<Object[]> getApprovedLicensesByAllAreas();

	@Query(value = "SELECT unit.id as unitId, COUNT(DISTINCT la.id) as approvedLicenses " +
			"FROM core_water_resource_unit unit " +
			"LEFT JOIN core_license_application la ON (la.source_wru = unit.id OR la.dest_wru = unit.id) " +
			"WHERE unit.water_resource_area_id = :areaId AND (la.status = 'SUBMITTED' OR la.id IS NULL) " +
			"GROUP BY unit.id", nativeQuery = true)
	List<Object[]> getApprovedLicensesByUnitsInArea(@Param("areaId") String areaId);

}