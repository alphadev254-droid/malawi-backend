package mw.nwra.ewaterpermit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_audit_entry")
@NamedQuery(name = "SysAuditEntry.findAll", query = "SELECT s FROM SysAuditEntry s")
public class SysAuditEntry extends BaseEntity {

	private String action;

	@Column(name = "field_name")
	private String fieldName;

	@Column(name = "key_value")
	private String keyValue;

	@Lob
	@Column(name = "new_value")
	private String newValue;

	@Lob
	@Column(name = "old_value")
	private String oldValue;

	@Column(name = "table_name")
	private String tableName;

	// bi-directional many-to-one association to SysUserAccount
	@ManyToOne
	@JoinColumn(name = "user_account_id")
	private SysUserAccount sysUserAccount;

	public SysAuditEntry() {
	}

	public String getAction() {
		return this.action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getFieldName() {
		return this.fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getKeyValue() {
		return this.keyValue;
	}

	public void setKeyValue(String keyValue) {
		this.keyValue = keyValue;
	}

	public String getNewValue() {
		return this.newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public String getOldValue() {
		return this.oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getTableName() {
		return this.tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public SysUserAccount getSysUserAccount() {
		return this.sysUserAccount;
	}

	public void setSysUserAccount(SysUserAccount sysUserAccount) {
		this.sysUserAccount = sysUserAccount;
	}

}