package mw.nwra.ewaterpermit.model;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_user_account_activation")
@NamedQuery(name = "SysUserAccountActivation.findAll", query = "SELECT s FROM SysUserAccountActivation s")
public class SysUserAccountActivation extends BaseEntity {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "date_activated")
	private Timestamp dateActivated;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "date_expired")
	private Timestamp dateExpired;

	@Column(name = "email_address")
	private String emailAddress;

	@Column(name = "registration_host")
	private String registrationHost;

	private String token;

	// bi-directional many-to-one association to SysUserAccount
	@ManyToOne
	@JoinColumn(name = "user_account_id")
	private SysUserAccount sysUserAccount;

	public SysUserAccountActivation() {
	}

	public Timestamp getDateActivated() {
		return this.dateActivated;
	}

	public void setDateActivated(Timestamp dateActivated) {
		this.dateActivated = dateActivated;
	}

	public Timestamp getDateExpired() {
		return this.dateExpired;
	}

	public void setDateExpired(Timestamp dateExpired) {
		this.dateExpired = dateExpired;
	}

	public String getEmailAddress() {
		return this.emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getRegistrationHost() {
		return this.registrationHost;
	}

	public void setRegistrationHost(String registrationHost) {
		this.registrationHost = registrationHost;
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public SysUserAccount getSysUserAccount() {
		return this.sysUserAccount;
	}

	public void setSysUserAccount(SysUserAccount sysUserAccount) {
		this.sysUserAccount = sysUserAccount;
	}

}