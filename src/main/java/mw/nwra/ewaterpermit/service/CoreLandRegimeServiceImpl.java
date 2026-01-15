package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreLandRegime;
import mw.nwra.ewaterpermit.repository.CoreLandRegimeRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreLandRegimeService")
public class CoreLandRegimeServiceImpl implements CoreLandRegimeService {
	@Autowired
	CoreLandRegimeRepository repo;

	@Override
	public List<CoreLandRegime> getAllCoreLandRegimes() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreLandRegime> getAllCoreLandRegimes(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreLandRegime getCoreLandRegimeById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreLandRegime getCoreLandRegimeByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreLandRegime(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreLandRegime addCoreLandRegime(CoreLandRegime coreLandRegime) {
		return this.repo.saveAndFlush(coreLandRegime);
	}

	@Override
	public CoreLandRegime editCoreLandRegime(CoreLandRegime coreLandRegime) {
		return this.repo.saveAndFlush(coreLandRegime);
	}
}
