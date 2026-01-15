package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.model.CoreLicense;
import mw.nwra.ewaterpermit.model.CoreLicenseApplication;

public interface PDFGenerationService {
    
    byte[] generateSurfaceWaterPermitPDF(CoreLicense license);
    
    byte[] generateEffluentDischargePermitPDF(CoreLicense license);
    
    byte[] generateBoreholePermitPDF(CoreLicense license);
    
    byte[] generateInvoicePDF(CoreLicenseApplication application, String feeType);
    
    byte[] generateReceiptPDF(String paymentReference);
    
    String generatePermitPDFAndSave(CoreLicense license);
    
    String generateInvoicePDFAndSave(CoreLicenseApplication application, String feeType);
    
    String generateReceiptPDFAndSave(String paymentReference);
}