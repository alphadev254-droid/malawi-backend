package mw.nwra.ewaterpermit.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreFinancialYear;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.CoreFinancialYearService;
import mw.nwra.ewaterpermit.util.AppUtil;

@RestController
@RequestMapping(value = "/v1/financial-years")
public class CoreFinancialYearController {
    
    @Autowired
    private CoreFinancialYearService financialYearService;
    
    @GetMapping
    public List<CoreFinancialYear> getAllFinancialYears() {
        return financialYearService.getAllFinancialYears();
    }
    
    @GetMapping("/{id}")
    public CoreFinancialYear getFinancialYearById(@PathVariable String id) {
        CoreFinancialYear financialYear = financialYearService.getFinancialYearById(id);
        if (financialYear == null) {
            throw new EntityNotFoundException("Financial year not found");
        }
        return financialYear;
    }
    
    @GetMapping("/active")
    public CoreFinancialYear getActiveFinancialYear() {
        return financialYearService.getActiveFinancialYear();
    }
    
    @GetMapping("/pending")
    public List<CoreFinancialYear> getPendingFinancialYears(@RequestHeader("Authorization") String token) {
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null || !user.getSysUserGroup().getName().equalsIgnoreCase("licensing_manager")) {
            throw new ForbiddenException("Only licensing managers can view pending financial years");
        }
        return financialYearService.getFinancialYearsByStatus(CoreFinancialYear.FinancialYearStatus.PENDING);
    }
    
    @PostMapping
    public CoreFinancialYear createFinancialYear(@RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        
        // Check if user is accountant or admin
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null || (!user.getSysUserGroup().getName().equalsIgnoreCase("accountant") 
                && !user.getSysUserGroup().getName().equalsIgnoreCase("admin"))) {
            throw new ForbiddenException("Only accountants can create financial years");
        }
        
        // Check if there's already an active or pending financial year
        List<CoreFinancialYear> activeOrPending = financialYearService.getActiveOrPendingFinancialYears();
        if (!activeOrPending.isEmpty()) {
            throw new ForbiddenException("Cannot create financial year. There is already an active or pending financial year.");
        }
        
        CoreFinancialYear financialYear = (CoreFinancialYear) AppUtil.objectToClass(CoreFinancialYear.class, request);
        if (financialYear == null) {
            throw new ForbiddenException("Could not create financial year");
        }
        
        financialYear.setCreatedBy(user.getId());
        return financialYearService.addFinancialYear(financialYear);
    }
    
    @PutMapping("/{id}")
    public CoreFinancialYear updateFinancialYear(@PathVariable String id, 
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        
        CoreFinancialYear existingYear = financialYearService.getFinancialYearById(id);
        if (existingYear == null) {
            throw new EntityNotFoundException("Financial year not found");
        }
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        String userRole = user.getSysUserGroup().getName().toLowerCase();
        
        // Check permissions based on action
        if (request.containsKey("status")) {
            String newStatus = (String) request.get("status");
            if ("PENDING".equals(newStatus) && !"accountant".equals(userRole)) {
                throw new ForbiddenException("Only accountants can request to close financial years");
            }
            if ("CLOSED".equals(newStatus) && !"licensing_manager".equals(userRole)) {
                throw new ForbiddenException("Only licensing managers can close financial years");
            }
        }
        
        String[] propertiesToIgnore = AppUtil.getIgnoredProperties(CoreFinancialYear.class, request);
        CoreFinancialYear updatedYear = (CoreFinancialYear) AppUtil.objectToClass(CoreFinancialYear.class, request);
        
        BeanUtils.copyProperties(updatedYear, existingYear, propertiesToIgnore);
        return financialYearService.updateFinancialYear(existingYear);
    }
    
    @DeleteMapping("/{id}")
    public void deleteFinancialYear(@PathVariable String id, @RequestHeader("Authorization") String token) {
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null || !user.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
            throw new ForbiddenException("Only admins can delete financial years");
        }
        
        CoreFinancialYear financialYear = financialYearService.getFinancialYearById(id);
        if (financialYear == null) {
            throw new EntityNotFoundException("Financial year not found");
        }
        
        financialYearService.deleteFinancialYear(id);
    }
}