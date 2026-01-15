package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_country")
@NamedQuery(name = "CoreCountry.findAll", query = "SELECT c FROM CoreCountry c")
public class CoreCountry extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "iso")
    private String iso;

    @Column(name = "nicename")
    private String nicename;

    @Column(name = "iso3")
    private String iso3;

    @Column(name = "numcode")
    private String numcode;

    @Column(name = "phonecode")
    private String phonecode;

    public CoreCountry() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIso() {
        return this.iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getNicename() {
        return this.nicename;
    }

    public void setNicename(String nicename) {
        this.nicename = nicename;
    }

    public String getIso3() {
        return this.iso3;
    }

    public void setIso3(String iso3) {
        this.iso3 = iso3;
    }

    public String getNumcode() {
        return this.numcode;
    }

    public void setNumcode(String numcode) {
        this.numcode = numcode;
    }

    public String getPhonecode() {
        return this.phonecode;
    }

    public void setPhonecode(String phonecode) {
        this.phonecode = phonecode;
    }


}