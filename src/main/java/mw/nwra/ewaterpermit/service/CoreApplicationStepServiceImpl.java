package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreApplicationStep;
import mw.nwra.ewaterpermit.model.CoreLicenseType;
import mw.nwra.ewaterpermit.repository.CoreApplicationStepRepository;
import mw.nwra.ewaterpermit.util.AppUtil;

@Service(value = "coreApplicationStepService")
public class CoreApplicationStepServiceImpl implements CoreApplicationStepService {
	@Autowired
	CoreApplicationStepRepository repo;

	@Override
	public List<CoreApplicationStep> getAllCoreApplicationSteps() {
		return this.repo.findAll();
	}

	@Override
	public List<CoreApplicationStep> getAllCoreApplicationSteps(int page, int limit) {
		return this.repo.findAll(AppUtil.getPageRequest(page, limit, "dateCreated", "desc")).getContent();
	}

	@Override
	public CoreApplicationStep getCoreApplicationStepById(String id) {
		return this.repo.findById(id).orElse(null);
	}

	@Override
	public CoreApplicationStep getCoreApplicationStepByName(String name) {
		return this.repo.findByName(name);
	}

	@Override
	public void deleteCoreApplicationStep(String id) {
		this.repo.deleteById(id);
	}

	@Override
	public CoreApplicationStep addCoreApplicationStep(CoreApplicationStep coreApplicationStep) {
		return this.repo.saveAndFlush(coreApplicationStep);
	}

	@Override
	public CoreApplicationStep editCoreApplicationStep(CoreApplicationStep coreApplicationStep) {
		return this.repo.saveAndFlush(coreApplicationStep);
	}

	@Override
	public List<CoreApplicationStep> getCoreApplicationStepByLicenseType(CoreLicenseType type) {
		return this.repo.findByCoreLicenseType(type);
	}
	
	@Override
	public CoreApplicationStep getFirstStepByLicenseType(CoreLicenseType type) {
		return this.repo.findFirstByCoreLicenseTypeOrderBySequenceNumberAsc(type);
	}
	
	@Override
	public CoreApplicationStep getNextStep(CoreApplicationStep currentStep) {
		return this.repo.findFirstByCoreLicenseTypeAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
				currentStep.getCoreLicenseType(), currentStep.getSequenceNumber());
	}
	
	@Override
	public CoreApplicationStep getPreviousStep(CoreApplicationStep currentStep) {
		return this.repo.findFirstByCoreLicenseTypeAndSequenceNumberLessThanOrderBySequenceNumberDesc(
				currentStep.getCoreLicenseType(), currentStep.getSequenceNumber());
	}
	
	@Override
	public CoreApplicationStep getStepByLicenseTypeAndSequenceNumber(CoreLicenseType licenseId, byte sequenceNumber) {
		return this.repo.findFirstByCoreLicenseTypeAndSequenceNumber(licenseId, sequenceNumber);
	}

	@Override
	public String getNextStepIdForApplication(String applicationId) {
		try {
			return repo.getNextStepIdForApplication(applicationId);
		} catch (Exception e) {
			return null;
		}
	}
}
