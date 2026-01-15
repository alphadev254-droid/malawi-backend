package mw.nwra.ewaterpermit.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.model.CoreApplicationPayment;
import mw.nwra.ewaterpermit.model.CoreFinancialYear;
import mw.nwra.ewaterpermit.service.CoreApplicationPaymentService;
import mw.nwra.ewaterpermit.service.CoreFinancialYearService;

@RestController
@RequestMapping("/v1/licensing-manager")

public class LicensingManagerController {

    @Autowired
    private CoreFinancialYearService coreFinancialYearService;

    @Autowired
    private CoreApplicationPaymentService coreApplicationPaymentService;

    /**
     * Get all financial years for dropdown filter
     */
    @GetMapping("/financial-years")
    public ResponseEntity<List<CoreFinancialYear>> getFinancialYears() {
        List<CoreFinancialYear> financialYears = coreFinancialYearService.getAllFinancialYears();
        return ResponseEntity.ok(financialYears);
    }

    /**
     * Get financial transactions filtered by financial year
     */
    @GetMapping("/financial-transactions")
    public ResponseEntity<List<CoreApplicationPayment>> getFinancialTransactions(
            @RequestParam String financialYearId) {
        List<CoreApplicationPayment> payments = coreApplicationPaymentService.findByFinancialYearId(financialYearId);
        return ResponseEntity.ok(payments);
    }
}
