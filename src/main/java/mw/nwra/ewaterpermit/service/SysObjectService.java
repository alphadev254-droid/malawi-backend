package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysObject;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;

public interface SysObjectService {
	public List<SysObject> getAllSysObjects();

	public SearchResponse getAllSysObjects(int page, int limit, String search);

	public SysObject getSysObjectById(String sysObjectId);

	public SysObject getSysObjectByName(String name);

	public void deleteSysObject(String sysObjectId);

	public SysObject addSysObject(SysObject sysObject);

	public SysObject editSysObject(SysObject sysObject);

	public List<SysObject> getAllSysObjects(int page, int limit);
}
