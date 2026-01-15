package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreTraditionalAuthority;
import mw.nwra.ewaterpermit.repository.CoreTraditionalAuthorityRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreTraditionalAuthorityService")
public class CoreTraditionalAuthorityServiceImpl implements CoreTraditionalAuthorityService {
	@Autowired
	CoreTraditionalAuthorityRepository repo;

	@Override
	public List<CoreTraditionalAuthority> getAllCoreTraditionalAuthoritys() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreTraditionalAuthority> getAllCoreTraditionalAuthoritys(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreTraditionalAuthority getCoreTraditionalAuthorityById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreTraditionalAuthority getCoreTraditionalAuthorityByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreTraditionalAuthority(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreTraditionalAuthority addCoreTraditionalAuthority(CoreTraditionalAuthority coreTraditionalAuthority) {
		return this.repo.saveAndFlush(coreTraditionalAuthority);
	}

	@Override
	public CoreTraditionalAuthority editCoreTraditionalAuthority(CoreTraditionalAuthority coreTraditionalAuthority) {
		return this.repo.saveAndFlush(coreTraditionalAuthority);
	}
}
