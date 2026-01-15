package mw.nwra.ewaterpermit.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.model.CoreCountry;
import mw.nwra.ewaterpermit.service.CoreCountryService;

@RestController
@RequestMapping(value = "/v1/countries")
public class CoreCountryController {

    @Autowired
    private CoreCountryService countryService;

    @GetMapping
    public List<CoreCountry> getAllCountries() {
        try {
            return this.countryService.getAllCoreCountries();
        } catch (Exception e) {
            System.err.println("Error fetching countries: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}