package mw.nwra.ewaterpermit.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.CoreApplicationPayment;
import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.model.SysConfig;

@Service
public class PDFGenerationServiceImpl implements PDFGenerationService {

    @Autowired
    private SysConfigService configService;

    @Autowired
    private CoreApplicationPaymentService paymentService;

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);

    @Override
    public byte[] generateSurfaceWaterPermitPDF(CoreLicense license) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add header
            addHeader(document, "SURFACE WATER ABSTRACTION PERMIT");
            
            // Add permit details
            addPermitDetails(document, license);
            
            // Add security features
            addSecurityFeatures(document, writer, license);
            
            // Add footer
            addFooter(document);
            
            document.close();
            return baos.toByteArray();
            
        } catch (DocumentException | IOException e) {
            throw new ForbiddenException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Override
    public byte[] generateEffluentDischargePermitPDF(CoreLicense license) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add header
            addHeader(document, "EFFLUENT DISCHARGE PERMIT");
            
            // Add permit details
            addPermitDetails(document, license);
            
            // Add security features
            addSecurityFeatures(document, writer, license);
            
            // Add footer
            addFooter(document);
            
            document.close();
            return baos.toByteArray();
            
        } catch (DocumentException | IOException e) {
            throw new ForbiddenException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Override
    public byte[] generateBoreholePermitPDF(CoreLicense license) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add header
            addHeader(document, "BOREHOLE DRILLING PERMIT");
            
            // Add permit details
            addPermitDetails(document, license);
            
            // Add security features
            addSecurityFeatures(document, writer, license);
            
            // Add footer
            addFooter(document);
            
            document.close();
            return baos.toByteArray();
            
        } catch (DocumentException | IOException e) {
            throw new ForbiddenException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Override
    public byte[] generateInvoicePDF(CoreLicenseApplication application, String feeType) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add header
            addHeader(document, "INVOICE");
            
            // Add invoice details
            addInvoiceDetails(document, application, feeType);
            
            // Add footer
            addFooter(document);
            
            document.close();
            return baos.toByteArray();
            
        } catch (DocumentException | IOException e) {
            throw new ForbiddenException("Failed to generate invoice PDF: " + e.getMessage());
        }
    }

    @Override
    public byte[] generateReceiptPDF(String paymentReference) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add header
            addHeader(document, "OFFICIAL RECEIPT");
            
            // Add receipt details
            addReceiptDetails(document, paymentReference);
            
            // Add footer
            addFooter(document);
            
            document.close();
            return baos.toByteArray();
            
        } catch (DocumentException | IOException e) {
            throw new ForbiddenException("Failed to generate receipt PDF: " + e.getMessage());
        }
    }

    @Override
    public String generatePermitPDFAndSave(CoreLicense license) {
        byte[] pdfBytes = generateSurfaceWaterPermitPDF(license);
        return savePDFToFile(pdfBytes, "permit_" + license.getId() + ".pdf");
    }

    @Override
    public String generateInvoicePDFAndSave(CoreLicenseApplication application, String feeType) {
        byte[] pdfBytes = generateInvoicePDF(application, feeType);
        return savePDFToFile(pdfBytes, "invoice_" + application.getId() + "_" + feeType + ".pdf");
    }

    @Override
    public String generateReceiptPDFAndSave(String paymentReference) {
        byte[] pdfBytes = generateReceiptPDF(paymentReference);
        return savePDFToFile(pdfBytes, "receipt_" + paymentReference + ".pdf");
    }

    private void addHeader(Document document, String title) throws DocumentException, IOException {
        // Add logo if available
        SysConfig config = configService.getAllSysConfigurations().get(0);
        
        // Organization header
        Paragraph orgName = new Paragraph("NATIONAL WATER RESOURCES AUTHORITY", TITLE_FONT);
        orgName.setAlignment(Element.ALIGN_CENTER);
        document.add(orgName);
        
        Paragraph address = new Paragraph("Area 47, P.O. Box 30103, Lilongwe\nTel: +265 884 561 026", NORMAL_FONT);
        address.setAlignment(Element.ALIGN_CENTER);
        document.add(address);
        
        // Add some space
        document.add(new Paragraph(" "));
        
        // Document title
        Paragraph docTitle = new Paragraph(title, TITLE_FONT);
        docTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(docTitle);
        
        document.add(new Paragraph(" "));
    }

    private void addPermitDetails(Document document, CoreLicense license) throws DocumentException {
        // Create table for permit details
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        
        // Add permit information
        addTableRow(table, "Permit Number:", license.getLicenseNumber());
        addTableRow(table, "Permit Type:", license.getCoreLicenseApplication().getCoreLicenseType().getName());
        addTableRow(table, "Permit Holder:", license.getCoreLicenseApplication().getSysUserAccount().getFirstName() + 
                   " " + license.getCoreLicenseApplication().getSysUserAccount().getLastName());
        addTableRow(table, "Issue Date:", license.getDateIssued() != null ? license.getDateIssued().toString() : license.getDateCreated().toString());
        addTableRow(table, "Expiry Date:", license.getExpirationDate().toString());
        addTableRow(table, "Water Source:", license.getCoreLicenseApplication().getCoreWaterSource().getName());
        addTableRow(table, "Purpose:", getWaterUsePurpose(license));
        addTableRow(table, "Location:", getLocationDetails(license));
        
        document.add(table);
        document.add(new Paragraph(" "));
        
        // Add conditions
        Paragraph conditionsTitle = new Paragraph("CONDITIONS:", HEADER_FONT);
        document.add(conditionsTitle);
        
        Paragraph conditions = new Paragraph(
            "1. This permit is valid for the period specified above.\n" +
            "2. The permit holder must comply with all applicable water resources regulations.\n" +
            "3. Regular monitoring and reporting is required.\n" +
            "4. The permit is non-transferable without prior approval.\n" +
            "5. Any violation may result in permit suspension or revocation.",
            NORMAL_FONT
        );
        document.add(conditions);
        
        document.add(new Paragraph(" "));
    }

    private void addInvoiceDetails(Document document, CoreLicenseApplication application, String feeType) throws DocumentException {
        // Invoice details table
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        
        addTableRow(table, "Invoice Number:", "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        addTableRow(table, "Invoice Date:", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        addTableRow(table, "Application ID:", application.getId());
        addTableRow(table, "Applicant:", application.getSysUserAccount().getFirstName() + " " + 
                   application.getSysUserAccount().getLastName());
        addTableRow(table, "Email:", application.getSysUserAccount().getEmailAddress());
        addTableRow(table, "Phone:", application.getSysUserAccount().getPhoneNumber());
        
        document.add(table);
        document.add(new Paragraph(" "));
        
        // Fee details
        Paragraph feeTitle = new Paragraph("FEE DETAILS:", HEADER_FONT);
        document.add(feeTitle);
        
        PdfPTable feeTable = new PdfPTable(3);
        feeTable.setWidthPercentage(100);
        feeTable.setWidths(new float[]{2, 1, 1});
        
        addTableRow3Column(feeTable, "Description", "Quantity", "Amount (MWK)");
        addTableRow3Column(feeTable, feeType + " Fee", "1", "5,000.00");
        addTableRow3Column(feeTable, "Total Amount", "", "5,000.00");
        
        document.add(feeTable);
    }

    private void addReceiptDetails(Document document, String paymentReference) throws DocumentException {
        // Receipt details
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        
        addTableRow(table, "Receipt Number:", "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        addTableRow(table, "Payment Reference:", paymentReference);
        addTableRow(table, "Payment Date:", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        addTableRow(table, "Amount Paid:", "MWK 5,000.00");
        addTableRow(table, "Payment Status:", "COMPLETED");
        
        document.add(table);
        
        document.add(new Paragraph(" "));
        
        Paragraph thankYou = new Paragraph("Thank you for your payment.", HEADER_FONT);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        document.add(thankYou);
    }

    private void addSecurityFeatures(Document document, PdfWriter writer, CoreLicense license) throws DocumentException {
        // Add watermark
        addWatermark(document, "OFFICIAL PERMIT");
        
        // Add QR code
        addQRCode(document, license.getId());
        
        // Add digital signature placeholder
        Paragraph signature = new Paragraph("Digitally signed by: CEO, National Water Resources Authority", SMALL_FONT);
        signature.setAlignment(Element.ALIGN_RIGHT);
        document.add(signature);
        
        Paragraph timestamp = new Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), SMALL_FONT);
        timestamp.setAlignment(Element.ALIGN_RIGHT);
        document.add(timestamp);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        
        Paragraph footer = new Paragraph(
            "This is a computer-generated document. No signature is required.\n" +
            "For verification, visit: https://permits.nwra.mw/verify",
            SMALL_FONT
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", NORMAL_FONT));
        valueCell.setBorder(0);
        valueCell.setPaddingBottom(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addTableRow3Column(PdfPTable table, String column1, String column2, String column3) {
        PdfPCell cell1 = new PdfPCell(new Phrase(column1 != null ? column1 : "", HEADER_FONT));
        cell1.setBorder(0);
        cell1.setPaddingBottom(5);
        
        PdfPCell cell2 = new PdfPCell(new Phrase(column2 != null ? column2 : "", NORMAL_FONT));
        cell2.setBorder(0);
        cell2.setPaddingBottom(5);
        
        PdfPCell cell3 = new PdfPCell(new Phrase(column3 != null ? column3 : "", NORMAL_FONT));
        cell3.setBorder(0);
        cell3.setPaddingBottom(5);
        
        table.addCell(cell1);
        table.addCell(cell2);
        table.addCell(cell3);
    }

    private void addWatermark(Document document, String watermarkText) {
        // Implementation would add watermark to PDF
        // This is a placeholder for watermark functionality
    }

    private void addQRCode(Document document, String licenseId) throws DocumentException {
        // Generate QR code for license verification
        Barcode128 code128 = new Barcode128();
        code128.setCode(licenseId);
        code128.setCodeType(Barcode128.CODE128);
        
        Image code128Image = code128.createImageWithBarcode(null, null, null);
        code128Image.setAlignment(Element.ALIGN_RIGHT);
        code128Image.scalePercent(50);
        
        document.add(code128Image);
    }

    private String getWaterUsePurpose(CoreLicense license) {
        if (license.getCoreLicenseApplication().getCoreLicenseWaterUses() != null && 
            !license.getCoreLicenseApplication().getCoreLicenseWaterUses().isEmpty()) {
            return license.getCoreLicenseApplication().getCoreLicenseWaterUses().get(0).getCoreWaterUse().getName();
        }
        return "Not specified";
    }

    private String getLocationDetails(CoreLicense license) {
        CoreLicenseApplication app = license.getCoreLicenseApplication();
        return String.format("Village: %s, T/A: %s, Coordinates: %s, %s",
            app.getSourceVillage() != null ? app.getSourceVillage() : "N/A",
            app.getSourceTa() != null ? app.getSourceTa() : "N/A",
            app.getSourceEasting() != null ? app.getSourceEasting() : "N/A",
            app.getSourceNorthing() != null ? app.getSourceNorthing() : "N/A"
        );
    }

    private String savePDFToFile(byte[] pdfBytes, String filename) {
        try {
            SysConfig config = configService.getAllSysConfigurations().get(0);
            String uploadPath = config.getUploadDirectory();
            
            File pdfDirectory = new File(uploadPath + "/pdfs");
            if (!pdfDirectory.exists()) {
                pdfDirectory.mkdirs();
            }
            
            File pdfFile = new File(pdfDirectory, filename);
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                fos.write(pdfBytes);
            }
            
            return config.getStorageUrl() + "/pdfs/" + filename;
            
        } catch (IOException e) {
            throw new ForbiddenException("Failed to save PDF file: " + e.getMessage());
        }
    }
}