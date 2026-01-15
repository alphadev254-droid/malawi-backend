package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.SysSalutation;

public interface SysSalutationService {
	public List<SysSalutation> getAllSysSalutations();

	public List<SysSalutation> getAllSysSalutations(int page, int limit);

	public SysSalutation getSysSalutationById(String sysSalutationId);

	public void deleteSysSalutation(String sysSalutationId);

	public SysSalutation addSysSalutation(SysSalutation sysSalutation);

	public SysSalutation editSysSalutation(SysSalutation sysSalutation);
}
