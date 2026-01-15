package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreFinancialYear;
import mw.nwra.ewaterpermit.model.CoreFinancialYear.FinancialYearStatus;

public interface CoreFinancialYearService {
    List<CoreFinancialYear> getAllFinancialYears();
    CoreFinancialYear getFinancialYearById(String id);
    CoreFinancialYear addFinancialYear(CoreFinancialYear financialYear);
    CoreFinancialYear updateFinancialYear(CoreFinancialYear financialYear);
    void deleteFinancialYear(String id);
    List<CoreFinancialYear> getFinancialYearsByStatus(FinancialYearStatus status);
    List<CoreFinancialYear> getActiveOrPendingFinancialYears();
    CoreFinancialYear getActiveFinancialYear();
}