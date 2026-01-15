package mw.nwra.ewaterpermit.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mw.nwra.ewaterpermit.model.CoreInvoice;
import mw.nwra.ewaterpermit.repository.CoreInvoiceRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreInvoiceService")
public class CoreInvoiceServiceImpl implements CoreInvoiceService {
	private static final Logger log = LoggerFactory.getLogger(CoreInvoiceServiceImpl.class);
	
	@Autowired
	CoreInvoiceRepository repo;

	@Override
	public List<CoreInvoice> getAllCoreInvoices() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreInvoice> getAllCoreInvoices(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "issueDate", "desc")).getContent();
	}

	@Override
	public CoreInvoice getCoreInvoiceById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public void deleteCoreInvoice(String id) {
		this.repo.deleteById(id);
	}

	@Override
	@Transactional
	public CoreInvoice addCoreInvoice(CoreInvoice coreInvoice) {
		try {
			return this.repo.save(coreInvoice);
		} catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
			// Retry once with a fresh entity
			log.warn("Optimistic locking failure, retrying save for invoice: {}", coreInvoice.getId());
			try {
				Thread.sleep(100); // Brief pause
				return this.repo.save(coreInvoice);
			} catch (Exception retryException) {
				log.error("Retry failed for invoice save: {}", retryException.getMessage());
				throw e; // Re-throw original exception
			}
		}
	}

	@Override
	@Transactional
	public void editCoreInvoice(CoreInvoice coreInvoice) {
		try {
			this.repo.save(coreInvoice);
		} catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
			// Retry once with a fresh entity
			log.warn("Optimistic locking failure, retrying edit for invoice: {}", coreInvoice.getId());
			try {
				Thread.sleep(100); // Brief pause
				this.repo.save(coreInvoice);
			} catch (Exception retryException) {
				log.error("Retry failed for invoice edit: {}", retryException.getMessage());
				throw e; // Re-throw original exception
			}
		}
    }

	@Override
	public List<CoreInvoice> getInvoicesByApplicationId(String applicationId) {
		return this.repo.findByApplicationId(applicationId);
	}

	@Override
	public Optional<CoreInvoice> getInvoiceByApplicationIdAndType(String applicationId, String invoiceType) {
		return this.repo.findByApplicationIdAndType(applicationId, invoiceType);
	}

	@Override
	public Optional<CoreInvoice> getInvoiceByInvoiceNumber(String invoiceNumber) {
		return this.repo.findByInvoiceNumber(invoiceNumber);
	}

	@Override
	public List<CoreInvoice> getInvoicesByStatus(String status) {
		return this.repo.findByStatus(status);
	}
}