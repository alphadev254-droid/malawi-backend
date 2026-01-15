package mw.nwra.ewaterpermit.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Service
public class PDFReportService {

    private static final Logger log = LoggerFactory.getLogger(PDFReportService.class);
    
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
    
    private final DecimalFormat numberFormat = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public byte[] generateWaterAllocationLevelReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Water Allocation Level Report", reportData, this::addWaterAllocationLevelContent);
    }

    public byte[] generateWaterDischargeDistributionReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Water Discharge Distribution Report", reportData, this::addDistributionReportContent);
    }

    public byte[] generateWaterUseDistributionReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Water Use Distribution Report", reportData, this::addDistributionReportContent);
    }

    public byte[] generateWaterPermitsDistributionReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Water Permits Distribution Report", reportData, this::addDistributionReportContent);
    }

    public byte[] generateWaterLicenseDebtDistributionReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Water License Debt Distribution Report", reportData, this::addDistributionReportContent);
    }

    public byte[] generateWaterLicenseRevenueDistributionReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Water License Revenue Distribution Report", reportData, this::addDistributionReportContent);
    }

    public byte[] generateLargestWaterUsersReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Largest Water Users Report", reportData, this::addLargestUsersContent);
    }

    public byte[] generateLargestDischargeReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Largest Water Discharge Report", reportData, this::addLargestDischargeContent);
    }

    public byte[] generateLargestDebtHoldersReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Largest Debt Holders Report", reportData, this::addLargestDebtHoldersContent);
    }

    public byte[] generateLargestRevenueLicencesReport(Map<String, Object> reportData) throws Exception {
        return generatePDFReport("Largest Revenue Licences Report", reportData, this::addLargestRevenueLicencesContent);
    }

    private byte[] generatePDFReport(String title, Map<String, Object> reportData, ContentAdder contentAdder) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            // Add header and footer
            writer.setPageEvent(new HeaderFooterPageEvent(title));
            
            document.open();
            
            // Add title
            Paragraph titlePara = new Paragraph(title, TITLE_FONT);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);
            
            // Add generation date
            Paragraph datePara = new Paragraph("Generated on: " + dateFormat.format(new Date()), SMALL_FONT);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            datePara.setSpacingAfter(20);
            document.add(datePara);
            
            // Add content using the provided content adder
            contentAdder.addContent(document, reportData);
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF report: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void addWaterAllocationLevelContent(Document document, Map<String, Object> reportData) throws DocumentException {
        // Summary section
        if (reportData.containsKey("summary")) {
            Map<String, Object> summary = (Map<String, Object>) reportData.get("summary");
            
            Paragraph summaryTitle = new Paragraph("Summary", HEADER_FONT);
            summaryTitle.setSpacingBefore(10);
            summaryTitle.setSpacingAfter(10);
            document.add(summaryTitle);
            
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);
            
            addTableRow(summaryTable, "Total Water Resource Areas", String.valueOf(summary.getOrDefault("totalAreas", 0)));
            addTableRow(summaryTable, "Total Water Resource Units", String.valueOf(summary.getOrDefault("totalUnits", 0)));
            addTableRow(summaryTable, "Total Water Abstraction (ML/year)", formatNumber(summary.get("totalAbstraction")));
            
            document.add(summaryTable);
        }
        
        // Allocation levels by area
        if (reportData.containsKey("allocationByArea")) {
            List<Map<String, Object>> areas = (List<Map<String, Object>>) reportData.get("allocationByArea");
            
            Paragraph areasTitle = new Paragraph("Water Allocation by Area", HEADER_FONT);
            areasTitle.setSpacingBefore(10);
            areasTitle.setSpacingAfter(10);
            document.add(areasTitle);
            
            PdfPTable areasTable = new PdfPTable(4);
            areasTable.setWidthPercentage(100);
            areasTable.setWidths(new float[]{3, 2, 2, 2});
            areasTable.setSpacingAfter(20);
            
            // Headers
            addTableHeader(areasTable, "Area Name");
            addTableHeader(areasTable, "Abstraction (ML/year)");
            addTableHeader(areasTable, "Allocation Level");
            addTableHeader(areasTable, "Status");
            
            // Data rows
            for (Map<String, Object> area : areas) {
                addTableCell(areasTable, String.valueOf(area.getOrDefault("areaName", "Unknown")));
                addTableCell(areasTable, formatNumber(area.get("totalAbstraction")));
                addTableCell(areasTable, formatNumber(area.get("allocationLevel")) + "%");
                addTableCell(areasTable, String.valueOf(area.getOrDefault("status", "Unknown")));
            }
            
            document.add(areasTable);
        }
    }

    private void addDistributionReportContent(Document document, Map<String, Object> reportData) throws DocumentException {
        // Summary section
        if (reportData.containsKey("summary")) {
            Map<String, Object> summary = (Map<String, Object>) reportData.get("summary");
            
            Paragraph summaryTitle = new Paragraph("Summary", HEADER_FONT);
            summaryTitle.setSpacingBefore(10);
            summaryTitle.setSpacingAfter(10);
            document.add(summaryTitle);
            
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);
            
            for (Map.Entry<String, Object> entry : summary.entrySet()) {
                String key = formatKey(entry.getKey());
                String value = formatValue(entry.getValue());
                addTableRow(summaryTable, key, value);
            }
            
            document.add(summaryTable);
        }
        
        // Distribution data
        if (reportData.containsKey("distribution")) {
            List<Map<String, Object>> distribution = (List<Map<String, Object>>) reportData.get("distribution");
            
            Paragraph distributionTitle = new Paragraph("Distribution by Area", HEADER_FONT);
            distributionTitle.setSpacingBefore(10);
            distributionTitle.setSpacingAfter(10);
            document.add(distributionTitle);
            
            PdfPTable distributionTable = new PdfPTable(4);
            distributionTable.setWidthPercentage(100);
            distributionTable.setWidths(new float[]{3, 2, 2, 1});
            distributionTable.setSpacingAfter(20);
            
            // Headers (dynamically determine based on first row)
            if (!distribution.isEmpty()) {
                Map<String, Object> firstRow = distribution.get(0);
                addTableHeader(distributionTable, "Area Name");
                addTableHeader(distributionTable, "Count");
                addTableHeader(distributionTable, "Total Value");
                addTableHeader(distributionTable, "Percentage");
                
                // Data rows
                for (Map<String, Object> row : distribution) {
                    addTableCell(distributionTable, String.valueOf(row.getOrDefault("areaName", "Unknown")));
                    addTableCell(distributionTable, String.valueOf(row.getOrDefault("count", 0)));
                    addTableCell(distributionTable, formatNumber(row.get("totalValue")));
                    addTableCell(distributionTable, formatNumber(row.get("percentage")) + "%");
                }
            }
            
            document.add(distributionTable);
        }
    }

    private void addLargestUsersContent(Document document, Map<String, Object> reportData) throws DocumentException {
        if (reportData.containsKey("largestUsers")) {
            List<Map<String, Object>> users = (List<Map<String, Object>>) reportData.get("largestUsers");
            
            Paragraph usersTitle = new Paragraph("Largest Water Users", HEADER_FONT);
            usersTitle.setSpacingBefore(10);
            usersTitle.setSpacingAfter(10);
            document.add(usersTitle);
            
            PdfPTable usersTable = new PdfPTable(5);
            usersTable.setWidthPercentage(100);
            usersTable.setWidths(new float[]{1, 3, 2, 2, 2});
            usersTable.setSpacingAfter(20);
            
            // Headers
            addTableHeader(usersTable, "Rank");
            addTableHeader(usersTable, "License Holder");
            addTableHeader(usersTable, "License Type");
            addTableHeader(usersTable, "Abstraction (ML/year)");
            addTableHeader(usersTable, "Area");
            
            // Data rows
            int rank = 1;
            for (Map<String, Object> user : users) {
                addTableCell(usersTable, String.valueOf(rank++));
                addTableCell(usersTable, String.valueOf(user.getOrDefault("licenseHolder", "Unknown")));
                addTableCell(usersTable, String.valueOf(user.getOrDefault("licenseType", "Unknown")));
                addTableCell(usersTable, formatNumber(user.get("totalAbstraction")));
                addTableCell(usersTable, String.valueOf(user.getOrDefault("areaName", "Unknown")));
            }
            
            document.add(usersTable);
        }
    }

    private void addLargestDischargeContent(Document document, Map<String, Object> reportData) throws DocumentException {
        if (reportData.containsKey("largestDischarge")) {
            List<Map<String, Object>> discharge = (List<Map<String, Object>>) reportData.get("largestDischarge");
            
            Paragraph dischargeTitle = new Paragraph("Largest Water Discharge", HEADER_FONT);
            dischargeTitle.setSpacingBefore(10);
            dischargeTitle.setSpacingAfter(10);
            document.add(dischargeTitle);
            
            PdfPTable dischargeTable = new PdfPTable(5);
            dischargeTable.setWidthPercentage(100);
            dischargeTable.setWidths(new float[]{1, 3, 2, 2, 2});
            dischargeTable.setSpacingAfter(20);
            
            // Headers
            addTableHeader(dischargeTable, "Rank");
            addTableHeader(dischargeTable, "License Holder");
            addTableHeader(dischargeTable, "License Type");
            addTableHeader(dischargeTable, "Discharge Volume");
            addTableHeader(dischargeTable, "Area");
            
            // Data rows
            int rank = 1;
            for (Map<String, Object> item : discharge) {
                addTableCell(dischargeTable, String.valueOf(rank++));
                addTableCell(dischargeTable, String.valueOf(item.getOrDefault("licenseHolder", "Unknown")));
                addTableCell(dischargeTable, String.valueOf(item.getOrDefault("licenseType", "Unknown")));
                addTableCell(dischargeTable, formatNumber(item.get("dischargeVolume")));
                addTableCell(dischargeTable, String.valueOf(item.getOrDefault("areaName", "Unknown")));
            }
            
            document.add(dischargeTable);
        }
    }

    private void addLargestDebtHoldersContent(Document document, Map<String, Object> reportData) throws DocumentException {
        if (reportData.containsKey("largestDebtHolders")) {
            List<Map<String, Object>> debtHolders = (List<Map<String, Object>>) reportData.get("largestDebtHolders");
            
            Paragraph debtTitle = new Paragraph("Largest Debt Holders", HEADER_FONT);
            debtTitle.setSpacingBefore(10);
            debtTitle.setSpacingAfter(10);
            document.add(debtTitle);
            
            PdfPTable debtTable = new PdfPTable(5);
            debtTable.setWidthPercentage(100);
            debtTable.setWidths(new float[]{1, 3, 2, 2, 2});
            debtTable.setSpacingAfter(20);
            
            // Headers
            addTableHeader(debtTable, "Rank");
            addTableHeader(debtTable, "License Holder");
            addTableHeader(debtTable, "License Type");
            addTableHeader(debtTable, "Outstanding Debt");
            addTableHeader(debtTable, "Area");
            
            // Data rows
            int rank = 1;
            for (Map<String, Object> holder : debtHolders) {
                addTableCell(debtTable, String.valueOf(rank++));
                addTableCell(debtTable, String.valueOf(holder.getOrDefault("licenseHolder", "Unknown")));
                addTableCell(debtTable, String.valueOf(holder.getOrDefault("licenseType", "Unknown")));
                addTableCell(debtTable, "MWK " + formatNumber(holder.get("outstandingDebt")));
                addTableCell(debtTable, String.valueOf(holder.getOrDefault("areaName", "Unknown")));
            }
            
            document.add(debtTable);
        }
    }

    private void addLargestRevenueLicencesContent(Document document, Map<String, Object> reportData) throws DocumentException {
        if (reportData.containsKey("largestRevenueLicences")) {
            List<Map<String, Object>> revenues = (List<Map<String, Object>>) reportData.get("largestRevenueLicences");
            
            Paragraph revenueTitle = new Paragraph("Largest Revenue Licences", HEADER_FONT);
            revenueTitle.setSpacingBefore(10);
            revenueTitle.setSpacingAfter(10);
            document.add(revenueTitle);
            
            PdfPTable revenueTable = new PdfPTable(5);
            revenueTable.setWidthPercentage(100);
            revenueTable.setWidths(new float[]{1, 3, 2, 2, 2});
            revenueTable.setSpacingAfter(20);
            
            // Headers
            addTableHeader(revenueTable, "Rank");
            addTableHeader(revenueTable, "License Holder");
            addTableHeader(revenueTable, "License Type");
            addTableHeader(revenueTable, "Total Revenue");
            addTableHeader(revenueTable, "Area");
            
            // Data rows
            int rank = 1;
            for (Map<String, Object> revenue : revenues) {
                addTableCell(revenueTable, String.valueOf(rank++));
                addTableCell(revenueTable, String.valueOf(revenue.getOrDefault("licenseHolder", "Unknown")));
                addTableCell(revenueTable, String.valueOf(revenue.getOrDefault("licenseType", "Unknown")));
                addTableCell(revenueTable, "MWK " + formatNumber(revenue.get("totalRevenue")));
                addTableCell(revenueTable, String.valueOf(revenue.getOrDefault("areaName", "Unknown")));
            }
            
            document.add(revenueTable);
        }
    }

    // Helper methods
    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String key, String value) {
        addTableCell(table, key);
        addTableCell(table, value);
    }

    private String formatNumber(Object value) {
        if (value == null) return "0.00";
        if (value instanceof Number) {
            return numberFormat.format(((Number) value).doubleValue());
        }
        try {
            return numberFormat.format(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }

    private String formatValue(Object value) {
        if (value == null) return "N/A";
        if (value instanceof Number) {
            return formatNumber(value);
        }
        return value.toString();
    }

    private String formatKey(String key) {
        return key.replaceAll("([a-z])([A-Z])", "$1 $2")
                  .replaceAll("_", " ")
                  .substring(0, 1).toUpperCase() + key.substring(1);
    }

    @FunctionalInterface
    private interface ContentAdder {
        void addContent(Document document, Map<String, Object> reportData) throws DocumentException;
    }

    // Header and Footer page event
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final String reportTitle;

        public HeaderFooterPageEvent(String reportTitle) {
            this.reportTitle = reportTitle;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            // Footer
            PdfPTable footer = new PdfPTable(3);
            footer.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
            footer.setLockedWidth(true);

            // Left: Organization name
            PdfPCell leftCell = new PdfPCell(new Phrase("National Water Resources Authority (NWRA)", SMALL_FONT));
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            footer.addCell(leftCell);

            // Center: Report title
            PdfPCell centerCell = new PdfPCell(new Phrase(reportTitle, SMALL_FONT));
            centerCell.setBorder(Rectangle.NO_BORDER);
            centerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footer.addCell(centerCell);

            // Right: Page number
            PdfPCell rightCell = new PdfPCell(new Phrase("Page " + writer.getPageNumber(), SMALL_FONT));
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            footer.addCell(rightCell);

            footer.writeSelectedRows(0, -1, 
                document.leftMargin(), 
                document.bottomMargin() - 10, 
                writer.getDirectContent());
        }
    }
}