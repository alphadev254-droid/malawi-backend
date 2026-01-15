package mw.nwra.ewaterpermit.model;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * The persistent class for the sys_config database table.
 * 
 */
@Entity
@Table(name = "sys_config")
@NamedQuery(name = "SysConfig.findAll", query = "SELECT s FROM SysConfig s")
public class SysConfig extends BaseEntity {

	@Column(name = "available_lang")
	private String availableLang;

	@Column(name = "base_lang")
	private String baseLang;

	@Column(name = "contact_email_address")
	private String contactEmailAddress;

	@Column(name = "contact_fax")
	private String contactFax;

	@Column(name = "contact_phone")
	private String contactPhone;

	@Column(name = "contact_physical_address")
	private String contactPhysicalAddress;

	@Column(name = "contact_postal_address")
	private String contactPostalAddress;

	@Lob
	@Column(name = "custom_css")
	private String customCss;

	@Column(name = "date_deactivated")
	private Timestamp dateDeactivated;

	private String favicon;

	@Column(name = "lock_user_maximum_attempts")
	private int lockUserMaximumAttempts;

	@Column(name = "lock_user_time")
	private int lockUserTime;

	@Column(name = "not_found_image")
	private String notFoundImage;

	@Column(name = "register_user_on_email_failure")
	private String registerUserOnEmailFailure;

	@Column(name = "storage_url")
	private String storageUrl;

	@Column(name = "system_description")
	private String systemDescription;

	@Column(name = "system_disclaimer")
	private String systemDisclaimer;

	@Column(name = "system_email_address")
	private String systemEmailAddress;

	@Column(name = "system_email_auth")
	private String systemEmailAuth;

	@Column(name = "system_email_port")
	private int systemEmailPort;

	@Column(name = "system_email_smtp")
	private String systemEmailSmtp;

	@Column(name = "system_email_smtp_security")
	private String systemEmailSmtpSecurity;

	@Column(name = "system_logo_url")
	private String systemLogoUrl;

	@Column(name = "system_name")
	private String systemName;

	@Column(name = "system_name_full")
	private String systemNameFull;

	@Column(name = "system_url")
	private String systemUrl;

	@Column(name = "upload_directory")
	private String uploadDirectory;

	@Column(name = "user_session_timeout")
	private int userSessionTimeout;

	@Column(name = "frontend_url")
	private String frontendUrl;

	@Column(name = "backend_url")
	private String backendUrl;

	@Lob
	@Column(name = "wrmis_allowed_ips", columnDefinition = "TEXT")
	private String wrmisAllowedIps;

	@Column(name = "wrmis_client_id", length = 255)
	private String wrmisClientId;

	@Column(name = "wrmis_client_secret", length = 500)
	private String wrmisClientSecret;

	public SysConfig() {
	}

	public String getAvailableLang() {
		return this.availableLang;
	}

	public void setAvailableLang(String availableLang) {
		this.availableLang = availableLang;
	}

	public String getBaseLang() {
		return this.baseLang;
	}

	public void setBaseLang(String baseLang) {
		this.baseLang = baseLang;
	}

	public String getContactEmailAddress() {
		return this.contactEmailAddress;
	}

	public void setContactEmailAddress(String contactEmailAddress) {
		this.contactEmailAddress = contactEmailAddress;
	}

	public String getContactFax() {
		return this.contactFax;
	}

	public void setContactFax(String contactFax) {
		this.contactFax = contactFax;
	}

	public String getContactPhone() {
		return this.contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getContactPhysicalAddress() {
		return this.contactPhysicalAddress;
	}

	public void setContactPhysicalAddress(String contactPhysicalAddress) {
		this.contactPhysicalAddress = contactPhysicalAddress;
	}

	public String getContactPostalAddress() {
		return this.contactPostalAddress;
	}

	public void setContactPostalAddress(String contactPostalAddress) {
		this.contactPostalAddress = contactPostalAddress;
	}

	public String getCustomCss() {
		return this.customCss;
	}

	public void setCustomCss(String customCss) {
		this.customCss = customCss;
	}

	public Timestamp getDateDeactivated() {
		return this.dateDeactivated;
	}

	public void setDateDeactivated(Timestamp dateDeactivated) {
		this.dateDeactivated = dateDeactivated;
	}

	public String getFavicon() {
		return this.favicon;
	}

	public void setFavicon(String favicon) {
		this.favicon = favicon;
	}

	public int getLockUserMaximumAttempts() {
		return this.lockUserMaximumAttempts;
	}

	public void setLockUserMaximumAttempts(int lockUserMaximumAttempts) {
		this.lockUserMaximumAttempts = lockUserMaximumAttempts;
	}

	public int getLockUserTime() {
		return this.lockUserTime;
	}

	public void setLockUserTime(int lockUserTime) {
		this.lockUserTime = lockUserTime;
	}

	public String getNotFoundImage() {
		return this.notFoundImage;
	}

	public void setNotFoundImage(String notFoundImage) {
		this.notFoundImage = notFoundImage;
	}

	public String getRegisterUserOnEmailFailure() {
		return this.registerUserOnEmailFailure;
	}

	public void setRegisterUserOnEmailFailure(String registerUserOnEmailFailure) {
		this.registerUserOnEmailFailure = registerUserOnEmailFailure;
	}

	public String getStorageUrl() {
		return this.storageUrl;
	}

	public void setStorageUrl(String storageUrl) {
		this.storageUrl = storageUrl;
	}

	public String getSystemDescription() {
		return this.systemDescription;
	}

	public void setSystemDescription(String systemDescription) {
		this.systemDescription = systemDescription;
	}

	public String getSystemDisclaimer() {
		return this.systemDisclaimer;
	}

	public void setSystemDisclaimer(String systemDisclaimer) {
		this.systemDisclaimer = systemDisclaimer;
	}

	public String getSystemEmailAddress() {
		return this.systemEmailAddress;
	}

	public void setSystemEmailAddress(String systemEmailAddress) {
		this.systemEmailAddress = systemEmailAddress;
	}

	public String getSystemEmailAuth() {
		return this.systemEmailAuth;
	}

	public void setSystemEmailAuth(String systemEmailAuth) {
		this.systemEmailAuth = systemEmailAuth;
	}

	public int getSystemEmailPort() {
		return this.systemEmailPort;
	}

	public void setSystemEmailPort(int systemEmailPort) {
		this.systemEmailPort = systemEmailPort;
	}

	public String getSystemEmailSmtp() {
		return this.systemEmailSmtp;
	}

	public void setSystemEmailSmtp(String systemEmailSmtp) {
		this.systemEmailSmtp = systemEmailSmtp;
	}

	public String getSystemEmailSmtpSecurity() {
		return this.systemEmailSmtpSecurity;
	}

	public void setSystemEmailSmtpSecurity(String systemEmailSmtpSecurity) {
		this.systemEmailSmtpSecurity = systemEmailSmtpSecurity;
	}

	public String getSystemLogoUrl() {
		return this.systemLogoUrl;
	}

	public void setSystemLogoUrl(String systemLogoUrl) {
		this.systemLogoUrl = systemLogoUrl;
	}

	public String getSystemName() {
		return this.systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public String getSystemNameFull() {
		return this.systemNameFull;
	}

	public void setSystemNameFull(String systemNameFull) {
		this.systemNameFull = systemNameFull;
	}

	public String getSystemUrl() {
		return this.systemUrl;
	}

	public void setSystemUrl(String systemUrl) {
		this.systemUrl = systemUrl;
	}

	public String getUploadDirectory() {
		return this.uploadDirectory;
	}

	public void setUploadDirectory(String uploadDirectory) {
		this.uploadDirectory = uploadDirectory;
	}

	public int getUserSessionTimeout() {
		return this.userSessionTimeout;
	}

	public void setUserSessionTimeout(int userSessionTimeout) {
		this.userSessionTimeout = userSessionTimeout;
	}

	public String getFrontendUrl() {
		return this.frontendUrl;
	}

	public void setFrontendUrl(String frontendUrl) {
		this.frontendUrl = frontendUrl;
	}

	public String getBackendUrl() {
		return this.backendUrl;
	}

	public void setBackendUrl(String backendUrl) {
		this.backendUrl = backendUrl;
	}

	public String getWrmisAllowedIps() {
		return this.wrmisAllowedIps;
	}

	public void setWrmisAllowedIps(String wrmisAllowedIps) {
		this.wrmisAllowedIps = wrmisAllowedIps;
	}

	public String getWrmisClientId() {
		return this.wrmisClientId;
	}

	public void setWrmisClientId(String wrmisClientId) {
		this.wrmisClientId = wrmisClientId;
	}

	public String getWrmisClientSecret() {
		return this.wrmisClientSecret;
	}

	public void setWrmisClientSecret(String wrmisClientSecret) {
		this.wrmisClientSecret = wrmisClientSecret;
	}

}