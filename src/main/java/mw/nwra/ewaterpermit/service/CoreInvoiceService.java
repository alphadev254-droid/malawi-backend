package mw.nwra.ewaterpermit.service;

import java.util.List;
import java.util.Optional;

import mw.nwra.ewaterpermit.model.CoreInvoice;

public interface CoreInvoiceService {
	List<CoreInvoice> getAllCoreInvoices();

	List<CoreInvoice> getAllCoreInvoices(int page, int limit);

	CoreInvoice getCoreInvoiceById(String id);

	void deleteCoreInvoice(String id);

	CoreInvoice addCoreInvoice(CoreInvoice coreInvoice);

	void editCoreInvoice(CoreInvoice coreInvoice);

	// Additional methods specific to invoices
	List<CoreInvoice> getInvoicesByApplicationId(String applicationId);
	
	Optional<CoreInvoice> getInvoiceByApplicationIdAndType(String applicationId, String invoiceType);
	
	Optional<CoreInvoice> getInvoiceByInvoiceNumber(String invoiceNumber);
	
	List<CoreInvoice> getInvoicesByStatus(String status);

	// Alias methods for consistency
	default CoreInvoice createCoreInvoice(CoreInvoice coreInvoice) {
		return addCoreInvoice(coreInvoice);
	}

	default List<CoreInvoice> getCoreInvoices(int page, int limit) {
		return getAllCoreInvoices(page, limit);
	}
}