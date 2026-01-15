package mw.nwra.ewaterpermit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mw.nwra.ewaterpermit.model.CoreLicensePermit;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoreLicensePermitRepository extends JpaRepository<CoreLicensePermit, String> {

	@Query("SELECT p FROM CoreLicensePermit p WHERE "
			+ " p.permitNumber LIKE %:search% OR "
			+ " p.coreLicenseApplication.destOwnerFullname LIKE %:search% OR "
			+ " p.coreLicenseApplication.sourceOwnerFullname LIKE %:search%")
	Page<CoreLicensePermit> findAll(@Param("search") String search, Pageable pageable);

	@Query("SELECT p FROM CoreLicensePermit p WHERE p.coreLicenseApplication.id = :applicationId")
	Optional<CoreLicensePermit> findByApplicationId(@Param("applicationId") String applicationId);

	@Query("SELECT p FROM CoreLicensePermit p WHERE p.permitNumber = :permitNumber")
	Optional<CoreLicensePermit> findByPermitNumber(@Param("permitNumber") String permitNumber);

	// Dashboard queries
	@Query("SELECT COALESCE(wru.id, 'Unknown'), COUNT(p) FROM CoreLicensePermit p " +
		   "LEFT JOIN p.coreLicenseApplication app " +
		   "LEFT JOIN app.destWru wru " +
		   "GROUP BY wru.id")
	List<Object[]> getPermitsByRegion();

	@Query("SELECT COALESCE(wst.name, 'Unknown'), COUNT(p) FROM CoreLicensePermit p " +
		   "LEFT JOIN p.coreLicenseApplication app " +
		   "LEFT JOIN app.coreWaterSource ws " +
		   "LEFT JOIN ws.coreWaterSourceType wst " +
		   "GROUP BY wst.name")
	List<Object[]> getPermitsByWaterSourceType();

	@Query("SELECT p.permitStatus, COUNT(p) FROM CoreLicensePermit p GROUP BY p.permitStatus")
	List<Object[]> getPermitsByStatus();
	
	// Role-specific dashboard queries
	@Query("SELECT COUNT(p) FROM CoreLicensePermit p WHERE p.coreLicenseApplication.sysUserAccount.id = :userId")
	Long countByUserId(@Param("userId") String userId);
}