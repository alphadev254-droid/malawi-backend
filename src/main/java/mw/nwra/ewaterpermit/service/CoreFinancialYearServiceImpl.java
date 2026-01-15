package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreFinancialYear;
import mw.nwra.ewaterpermit.model.CoreFinancialYear.FinancialYearStatus;
import mw.nwra.ewaterpermit.repository.CoreFinancialYearRepository;

@Service
public class CoreFinancialYearServiceImpl implements CoreFinancialYearService {
    
    @Autowired
    private CoreFinancialYearRepository repository;
    
    @Override
    public List<CoreFinancialYear> getAllFinancialYears() {
        return repository.findAll();
    }
    
    @Override
    public CoreFinancialYear getFinancialYearById(String id) {
        return repository.findById(id).orElse(null);
    }
    
    @Override
    public CoreFinancialYear addFinancialYear(CoreFinancialYear financialYear) {
        return repository.saveAndFlush(financialYear);
    }
    
    @Override
    public CoreFinancialYear updateFinancialYear(CoreFinancialYear financialYear) {
        return repository.saveAndFlush(financialYear);
    }
    
    @Override
    public void deleteFinancialYear(String id) {
        repository.deleteById(id);
    }
    
    @Override
    public List<CoreFinancialYear> getFinancialYearsByStatus(FinancialYearStatus status) {
        return repository.findByStatus(status);
    }
    
    @Override
    public List<CoreFinancialYear> getActiveOrPendingFinancialYears() {
        return repository.findActiveOrPending();
    }
    
    @Override
    public CoreFinancialYear getActiveFinancialYear() {
        List<CoreFinancialYear> activeYears = repository.findByStatus(FinancialYearStatus.ACTIVE);
        return activeYears.isEmpty() ? null : activeYears.get(0);
    }
}