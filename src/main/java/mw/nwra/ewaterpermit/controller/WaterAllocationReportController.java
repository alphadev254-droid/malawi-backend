package mw.nwra.ewaterpermit.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import mw.nwra.ewaterpermit.service.WaterAllocationReportService;
import mw.nwra.ewaterpermit.service.PDFReportService;

@RestController
@RequestMapping(value = "/v1/reports/water-allocation", produces = "application/json")
public class WaterAllocationReportController {

    @Autowired
    private WaterAllocationReportService waterAllocationReportService;

    @Autowired
    private PDFReportService pdfReportService;

    @GetMapping("/level")
    public ResponseEntity<Map<String, Object>> getWaterAllocationLevelReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterAllocationLevelReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate water allocation level report", 
                           "message", e.getMessage()));
        }
    }

    @GetMapping("/units")
    public ResponseEntity<Map<String, Object>> getWaterAllocationByUnit() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterAllocationByUnit();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate water allocation by unit report", 
                           "message", e.getMessage()));
        }
    }

    @GetMapping("/high-allocation-areas")
    public ResponseEntity<List<Map<String, Object>>> getHighAllocationAreas() {
        try {
            List<Map<String, Object>> areas = waterAllocationReportService.getHighAllocationAreas();
            return ResponseEntity.ok(areas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(List.of(Map.of("error", "Failed to get high allocation areas", 
                                   "message", e.getMessage())));
        }
    }

    @GetMapping("/high-allocation-units")
    public ResponseEntity<List<Map<String, Object>>> getHighAllocationUnits() {
        try {
            List<Map<String, Object>> units = waterAllocationReportService.getHighAllocationUnits();
            return ResponseEntity.ok(units);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(List.of(Map.of("error", "Failed to get high allocation units", 
                                   "message", e.getMessage())));
        }
    }

    @GetMapping("/discharge-distribution")
    public ResponseEntity<Map<String, Object>> getWaterDischargeDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterDischargeDistributionReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate water discharge distribution report", 
                           "message", e.getMessage()));
        }
    }
    
    @GetMapping("/use-distribution")
    public ResponseEntity<Map<String, Object>> getWaterUseDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterUseDistributionReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate water use distribution report", 
                           "message", e.getMessage()));
        }
    }
    
    @GetMapping("/permits-distribution")
    public ResponseEntity<Map<String, Object>> getWaterPermitsDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterPermitsDistributionReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate water permits distribution report", 
                           "message", e.getMessage()));
        }
    }
    
    @GetMapping("/debt-distribution")
    public ResponseEntity<Map<String, Object>> getWaterLicenseDebtDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseDebtDistributionReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate water license debt distribution report", 
                           "message", e.getMessage()));
        }
    }
    
    @GetMapping("/revenue-distribution")
    public ResponseEntity<Map<String, Object>> getWaterLicenseRevenueDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseRevenueDistributionReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate water license revenue distribution report", 
                           "message", e.getMessage()));
        }
    }
    
    @GetMapping("/largest-users")
    public ResponseEntity<Map<String, Object>> getLargestWaterUsersReport(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestWaterUsersReport(limit);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate largest water users report", 
                           "message", e.getMessage()));
        }
    }
    
    @GetMapping("/largest-discharge")
    public ResponseEntity<Map<String, Object>> getLargestWaterDischargeReport(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestWaterDischargeReport(limit);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate largest water discharge report", 
                           "message", e.getMessage()));
        }
    }
    
    @GetMapping("/largest-debt-holders")
    public ResponseEntity<Map<String, Object>> getLargestDebtHoldersReport(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestDebtHoldersReport(limit);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate largest debt holders report", 
                           "message", e.getMessage()));
        }
    }
    
    @GetMapping("/largest-revenue-licences")
    public ResponseEntity<Map<String, Object>> getLargestRevenueLicencesReport(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestRevenueLicencesReport(limit);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate largest revenue licences report", 
                           "message", e.getMessage()));
        }
    }
    
    // PDF Download endpoints for all reports
    
    @GetMapping("/level/download-pdf")
    public ResponseEntity<byte[]> downloadWaterAllocationLevelReportPDF() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterAllocationLevelReport();
            byte[] pdfBytes = pdfReportService.generateWaterAllocationLevelReport(report);
            return createPdfDownloadResponse(pdfBytes, "water-allocation-level-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/discharge-distribution/download-pdf")
    public ResponseEntity<byte[]> downloadWaterDischargeDistributionReportPDF() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterDischargeDistributionReport();
            byte[] pdfBytes = pdfReportService.generateWaterDischargeDistributionReport(report);
            return createPdfDownloadResponse(pdfBytes, "water-discharge-distribution-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/use-distribution/download-pdf")
    public ResponseEntity<byte[]> downloadWaterUseDistributionReportPDF() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterUseDistributionReport();
            byte[] pdfBytes = pdfReportService.generateWaterUseDistributionReport(report);
            return createPdfDownloadResponse(pdfBytes, "water-use-distribution-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/permits-distribution/download-pdf")
    public ResponseEntity<byte[]> downloadWaterPermitsDistributionReportPDF() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterPermitsDistributionReport();
            byte[] pdfBytes = pdfReportService.generateWaterPermitsDistributionReport(report);
            return createPdfDownloadResponse(pdfBytes, "water-permits-distribution-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/debt-distribution/download-pdf")
    public ResponseEntity<byte[]> downloadWaterLicenseDebtDistributionReportPDF() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseDebtDistributionReport();
            byte[] pdfBytes = pdfReportService.generateWaterLicenseDebtDistributionReport(report);
            return createPdfDownloadResponse(pdfBytes, "water-license-debt-distribution-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/revenue-distribution/download-pdf")
    public ResponseEntity<byte[]> downloadWaterLicenseRevenueDistributionReportPDF() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseRevenueDistributionReport();
            byte[] pdfBytes = pdfReportService.generateWaterLicenseRevenueDistributionReport(report);
            return createPdfDownloadResponse(pdfBytes, "water-license-revenue-distribution-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/largest-users/download-pdf")
    public ResponseEntity<byte[]> downloadLargestWaterUsersReportPDF(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestWaterUsersReport(limit);
            byte[] pdfBytes = pdfReportService.generateLargestWaterUsersReport(report);
            return createPdfDownloadResponse(pdfBytes, "largest-water-users-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/largest-discharge/download-pdf")
    public ResponseEntity<byte[]> downloadLargestWaterDischargeReportPDF(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestWaterDischargeReport(limit);
            byte[] pdfBytes = pdfReportService.generateLargestDischargeReport(report);
            return createPdfDownloadResponse(pdfBytes, "largest-water-discharge-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/largest-debt-holders/download-pdf")
    public ResponseEntity<byte[]> downloadLargestDebtHoldersReportPDF(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestDebtHoldersReport(limit);
            byte[] pdfBytes = pdfReportService.generateLargestDebtHoldersReport(report);
            return createPdfDownloadResponse(pdfBytes, "largest-debt-holders-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/largest-revenue-licences/download-pdf")
    public ResponseEntity<byte[]> downloadLargestRevenueLicencesReportPDF(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestRevenueLicencesReport(limit);
            byte[] pdfBytes = pdfReportService.generateLargestRevenueLicencesReport(report);
            return createPdfDownloadResponse(pdfBytes, "largest-revenue-licences-report.pdf");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // JSON Download endpoints for all reports
    
    @GetMapping("/discharge-distribution/download")
    public ResponseEntity<byte[]> downloadWaterDischargeDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterDischargeDistributionReport();
            return createJsonDownloadResponse(report, "water-discharge-distribution-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/level/download")
    public ResponseEntity<byte[]> downloadWaterAllocationLevelReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterAllocationLevelReport();
            return createJsonDownloadResponse(report, "water-allocation-level-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/use-distribution/download")
    public ResponseEntity<byte[]> downloadWaterUseDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterUseDistributionReport();
            return createJsonDownloadResponse(report, "water-use-distribution-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/permits-distribution/download")
    public ResponseEntity<byte[]> downloadWaterPermitsDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterPermitsDistributionReport();
            return createJsonDownloadResponse(report, "water-permits-distribution-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/debt-distribution/download")
    public ResponseEntity<byte[]> downloadWaterLicenseDebtDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseDebtDistributionReport();
            return createJsonDownloadResponse(report, "water-license-debt-distribution-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/revenue-distribution/download")
    public ResponseEntity<byte[]> downloadWaterLicenseRevenueDistributionReport() {
        try {
            Map<String, Object> report = waterAllocationReportService.getWaterLicenseRevenueDistributionReport();
            return createJsonDownloadResponse(report, "water-license-revenue-distribution-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/largest-users/download")
    public ResponseEntity<byte[]> downloadLargestWaterUsersReport(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestWaterUsersReport(limit);
            return createJsonDownloadResponse(report, "largest-water-users-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/largest-discharge/download")
    public ResponseEntity<byte[]> downloadLargestWaterDischargeReport(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestWaterDischargeReport(limit);
            return createJsonDownloadResponse(report, "largest-water-discharge-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/largest-debt-holders/download")
    public ResponseEntity<byte[]> downloadLargestDebtHoldersReport(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestDebtHoldersReport(limit);
            return createJsonDownloadResponse(report, "largest-debt-holders-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/largest-revenue-licences/download")
    public ResponseEntity<byte[]> downloadLargestRevenueLicencesReport(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> report = waterAllocationReportService.getLargestRevenueLicencesReport(limit);
            return createJsonDownloadResponse(report, "largest-revenue-licences-report.json");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private ResponseEntity<byte[]> createJsonDownloadResponse(Map<String, Object> report, String filename) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] jsonBytes = objectMapper.writeValueAsBytes(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(jsonBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(jsonBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private ResponseEntity<byte[]> createPdfDownloadResponse(byte[] pdfBytes, String filename) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}