package mw.nwra.ewaterpermit.dto;

import java.util.Date;

public class LicenseTransferInfoDto {
    private String firstName;
    private String lastName;
    private String emailAddress;
    private String phoneNumber;
    private String newLicenseNumber;
    private Date transferDate;
    
    public LicenseTransferInfoDto() {
    }
    
    public LicenseTransferInfoDto(String firstName, String lastName, String emailAddress, 
                                  String phoneNumber, String newLicenseNumber, Date transferDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.newLicenseNumber = newLicenseNumber;
        this.transferDate = transferDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNewLicenseNumber() {
        return newLicenseNumber;
    }

    public void setNewLicenseNumber(String newLicenseNumber) {
        this.newLicenseNumber = newLicenseNumber;
    }

    public Date getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(Date transferDate) {
        this.transferDate = transferDate;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}