package mw.nwra.ewaterpermit.repository;

import mw.nwra.ewaterpermit.model.CoreLicenseAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoreLicenseAssessmentRepository extends JpaRepository<CoreLicenseAssessment, String> {

    @Query("SELECT a FROM CoreLicenseAssessment a WHERE a.licenseApplicationId = :applicationId")
    CoreLicenseAssessment findByLicenseApplicationId(@Param("applicationId") String applicationId);

    @Query("SELECT a FROM CoreLicenseAssessment a WHERE a.licenseApplicationId IN :applicationIds")
    List<CoreLicenseAssessment> findByLicenseApplicationIdIn(@Param("applicationIds") List<String> applicationIds);
}