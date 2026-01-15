package mw.nwra.ewaterpermit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import mw.nwra.ewaterpermit.model.CoreFinancialYear;
import mw.nwra.ewaterpermit.model.CoreFinancialYear.FinancialYearStatus;

@Repository
public interface CoreFinancialYearRepository extends JpaRepository<CoreFinancialYear, String> {
    
    @Query("SELECT f FROM CoreFinancialYear f WHERE f.status = :status")
    List<CoreFinancialYear> findByStatus(FinancialYearStatus status);
    
    @Query("SELECT f FROM CoreFinancialYear f WHERE f.status IN ('ACTIVE', 'PENDING')")
    List<CoreFinancialYear> findActiveOrPending();
    
    Optional<CoreFinancialYear> findByName(String name);
}