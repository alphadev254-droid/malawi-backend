package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysAuditEntry;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;

public interface SysAuditEntryService {
	public List<SysAuditEntry> getAllSysAuditEntrys();

	public SearchResponse getAllSysAuditEntrys(int page, int limit, String query);

	public SysAuditEntry getSysAuditEntryById(String sysAuditEntryId);

	public void deleteSysAuditEntry(String sysAuditEntryId);

	public SysAuditEntry addSysAuditEntry(SysAuditEntry sysAuditEntry);

	public SysAuditEntry editSysAuditEntry(SysAuditEntry sysAuditEntry);
}
