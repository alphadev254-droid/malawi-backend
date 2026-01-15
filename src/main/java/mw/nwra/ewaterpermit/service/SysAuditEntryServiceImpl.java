package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysAuditEntry;
import mw.nwra.ewaterpermit.repository.SysAuditEntryRepository;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysAuditEntryService")
public class SysAuditEntryServiceImpl implements SysAuditEntryService {
	@Autowired
	SysAuditEntryRepository sysAuditEntryRepository;

	@Override
	public List<SysAuditEntry> getAllSysAuditEntrys() {
		return this.sysAuditEntryRepository.findAll();
	}

	@Override
	public SearchResponse getAllSysAuditEntrys(int page, int limit, String query) {
		Page<SysAuditEntry> auditList = this.sysAuditEntryRepository.findAll(query,
				AppUtil.getPageRequest(page, limit, "fieldName", "asc"));
		return new SearchResponse(auditList.getTotalElements(), auditList.getContent());
	}

	@Override
	public SysAuditEntry getSysAuditEntryById(String sysAuditEntryId) {
		return this.sysAuditEntryRepository.findById(sysAuditEntryId).orElse(null);
	}

	@Override
	public void deleteSysAuditEntry(String sysAuditEntryId) {
		this.sysAuditEntryRepository.deleteById(sysAuditEntryId);
	}

	@Override
	public SysAuditEntry addSysAuditEntry(SysAuditEntry sysAuditEntry) {
		return this.sysAuditEntryRepository.saveAndFlush(sysAuditEntry);
	}

	@Override
	public SysAuditEntry editSysAuditEntry(SysAuditEntry sysAuditEntry) {
		return this.sysAuditEntryRepository.saveAndFlush(sysAuditEntry);
	}
}
