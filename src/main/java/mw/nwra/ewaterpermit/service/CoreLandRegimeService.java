package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreLandRegime;

public interface CoreLandRegimeService {
	public List<CoreLandRegime> getAllCoreLandRegimes();

	public List<CoreLandRegime> getAllCoreLandRegimes(int page, int limit);

	public CoreLandRegime getCoreLandRegimeById(String id);

	public CoreLandRegime getCoreLandRegimeByName(String name);

	public void deleteCoreLandRegime(String id);

	public CoreLandRegime addCoreLandRegime(CoreLandRegime coreLandRegime);

	public CoreLandRegime editCoreLandRegime(CoreLandRegime coreLandRegime);
}
