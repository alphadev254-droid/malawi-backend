package mw.nwra.ewaterpermit.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.CoreCountry;
import mw.nwra.ewaterpermit.repository.CoreCountryRepository;

@Service
public class CoreCountryServiceImpl implements CoreCountryService {

    @Autowired
    private CoreCountryRepository countryRepository;

    @Override
    public List<CoreCountry> getAllCoreCountries() {
        return countryRepository.findAllByOrderByNameAsc();
    }
}