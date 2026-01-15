package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreTraditionalAuthority;

public interface CoreTraditionalAuthorityService {
	public List<CoreTraditionalAuthority> getAllCoreTraditionalAuthoritys();

	public List<CoreTraditionalAuthority> getAllCoreTraditionalAuthoritys(int page, int limit);

	public CoreTraditionalAuthority getCoreTraditionalAuthorityById(String id);

	public CoreTraditionalAuthority getCoreTraditionalAuthorityByName(String name);

	public void deleteCoreTraditionalAuthority(String id);

	public CoreTraditionalAuthority addCoreTraditionalAuthority(CoreTraditionalAuthority coreTraditionalAuthority);

	public CoreTraditionalAuthority editCoreTraditionalAuthority(CoreTraditionalAuthority coreTraditionalAuthority);
}
