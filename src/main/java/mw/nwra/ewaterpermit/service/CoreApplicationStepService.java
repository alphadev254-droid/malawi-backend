package mw.nwra.ewaterpermit.service;

import java.util.List;

import mw.nwra.ewaterpermit.model.CoreApplicationStep;
import mw.nwra.ewaterpermit.model.CoreLicenseType;

public interface CoreApplicationStepService {
	List<CoreApplicationStep> getAllCoreApplicationSteps();

	public List<CoreApplicationStep> getAllCoreApplicationSteps(int page, int limit);

	public CoreApplicationStep getCoreApplicationStepById(String id);

	public CoreApplicationStep getCoreApplicationStepByName(String name);

	public void deleteCoreApplicationStep(String id);

	public CoreApplicationStep addCoreApplicationStep(CoreApplicationStep coreApplicationStep);

	public CoreApplicationStep editCoreApplicationStep(CoreApplicationStep coreApplicationStep);

	public List<CoreApplicationStep> getCoreApplicationStepByLicenseType(CoreLicenseType type);
	
	public CoreApplicationStep getFirstStepByLicenseType(CoreLicenseType type);
	
	public CoreApplicationStep getNextStep(CoreApplicationStep currentStep);
	
	public CoreApplicationStep getPreviousStep(CoreApplicationStep currentStep);
	
	public CoreApplicationStep getStepByLicenseTypeAndSequenceNumber(CoreLicenseType licenseId, byte sequenceNumber);

	/**
	 * Get next step ID using optimized query without loading full entities
	 * @param applicationId The application ID
	 * @return The next step ID or null if not found
	 */
	String getNextStepIdForApplication(String applicationId);
}
