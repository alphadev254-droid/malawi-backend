package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.model.CoreLicenseAssessment;
import mw.nwra.ewaterpermit.repository.CoreLicenseAssessmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CoreLicenseAssessmentService {
    
    @Autowired
    private CoreLicenseAssessmentRepository repository;
    
    public CoreLicenseAssessment save(CoreLicenseAssessment assessment) {
        return repository.save(assessment);
    }
    
    public CoreLicenseAssessment findByApplicationId(String applicationId) {
        return repository.findByLicenseApplicationId(applicationId);
    }
}