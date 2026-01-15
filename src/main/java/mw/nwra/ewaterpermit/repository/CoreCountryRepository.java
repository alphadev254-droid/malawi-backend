package mw.nwra.ewaterpermit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mw.nwra.ewaterpermit.model.CoreCountry;

@Repository
public interface CoreCountryRepository extends JpaRepository<CoreCountry, String> {
    List<CoreCountry> findAllByOrderByNameAsc();
}