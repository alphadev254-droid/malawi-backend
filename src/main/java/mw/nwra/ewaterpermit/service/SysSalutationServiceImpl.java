package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysSalutation;
import mw.nwra.ewaterpermit.repository.SysSalutationRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "sysSalutationService")
public class SysSalutationServiceImpl implements SysSalutationService {

	@Autowired
	private SysSalutationRepository sysSalutationRepository;

	@Override
	public List<SysSalutation> getAllSysSalutations() {

		return this.sysSalutationRepository.findAll();
	}

	@Override
	public List<SysSalutation> getAllSysSalutations(int page, int limit) {
		return this.sysSalutationRepository.findAll(AppUtil.getPageRequest(page, limit, "name", "asc")).getContent();
	}

	@Override
	public SysSalutation getSysSalutationById(String sysSalutationId) {

		return this.sysSalutationRepository.findById(sysSalutationId).orElse(null);
	}

	@Override
	public void deleteSysSalutation(String sysSalutationId) {
		this.sysSalutationRepository.deleteById(sysSalutationId);
	}

	@Override
	public SysSalutation addSysSalutation(SysSalutation sysSalutation) {
		return this.sysSalutationRepository.saveAndFlush(sysSalutation);
	}

	@Override
	public SysSalutation editSysSalutation(SysSalutation sysSalutation) {
		return this.sysSalutationRepository.saveAndFlush(sysSalutation);
	}

}
