package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysObject;
import mw.nwra.ewaterpermit.repository.SysObjectRepository;
import mw.nwra.ewaterpermit.responseSchema.SearchResponse;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysObjectService")
public class SysObjectServiceImpl implements SysObjectService {
	@Autowired
	SysObjectRepository sysObjectRepository;

	@Override
	public List<SysObject> getAllSysObjects() {
		return this.sysObjectRepository.findAll();
	}

	@Override
	public SearchResponse getAllSysObjects(int page, int limit, String search) {

		Page<SysObject> objs = this.sysObjectRepository.findAll(search,
				AppUtil.getPageRequest(page, limit, "name", "asc"));
		return new SearchResponse(objs.getTotalElements(), objs.getContent());

//		return this.sysObjectRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "asc")).getContent();
	}

	@Override
	public SysObject getSysObjectById(String sysObjectId) {
		return this.sysObjectRepository.findById(sysObjectId).orElse(null);
	}

	@Override
	public void deleteSysObject(String sysObjectId) {
		this.sysObjectRepository.deleteById(sysObjectId);
	}

	@Override
	public SysObject addSysObject(SysObject sysObject) {
		return this.sysObjectRepository.saveAndFlush(sysObject);
	}

	@Override
	public SysObject editSysObject(SysObject sysObject) {
		return this.sysObjectRepository.saveAndFlush(sysObject);
	}

	@Override
	public SysObject getSysObjectByName(String name) {
		return this.sysObjectRepository.findByName(name);
	}

	@Override
	public List<SysObject> getAllSysObjects(int page, int limit) {
		return sysObjectRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "desc")).getContent();
	}
}
