package mw.nwra.ewaterpermit.util;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.Diff;
import org.apache.commons.lang3.builder.DiffResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.constant.Action;
import mw.nwra.ewaterpermit.exception.EntityAuditException;
import mw.nwra.ewaterpermit.model.SysAuditEntry;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.SysAuditEntryService;
import mw.nwra.ewaterpermit.service.SysObjectService;

@Service("auditor")
public class Auditor {
	@Autowired
	SysAuditEntryService sysAuditEntryService;
	@Autowired
	SysObjectService sysObjectService;

	public Auditor() {
	}

	public void audit(SysUserAccount userAccount, Object oldObject, Object newObject, Class<?> entityClass,
			String action) {
		String tableName = JpaUtils.getEntityTableName(entityClass);
		if (Action.UPDATE.toString().equals(action)) {
			String objectKey = JpaUtils.getIdentityAsString(newObject);

			DiffResult<Object> diffResult = null;
			try {
				diffResult = AuditingDiff.diff(oldObject, newObject);
			} catch (EntityAuditException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			List<Diff<?>> diffs = diffResult.getDiffs();
			for (Diff<?> diff : diffs) {
				SysAuditEntry auditEntry = new SysAuditEntry();
				auditEntry.setKeyValue(objectKey);
				auditEntry.setSysUserAccount(userAccount);
				auditEntry.setAction(action);
				auditEntry.setTableName(tableName);

				auditEntry.setFieldName(JpaUtils.getEntityDBFieldName(entityClass, diff.getFieldName()));
				auditEntry.setNewValue(String.valueOf(diff.getRight()));
				auditEntry.setOldValue(String.valueOf(diff.getLeft()));

				/*
				 * if (diff.getLeft() != null){ auditEntry.setOldValue(""); } else { if
				 * (diff.getLeft().toString() != null) {
				 * auditEntry.setOldValue(diff.getLeft().toString()); } else {
				 * storeAuditRecord(entityManager, auditEntry); } }
				 */

				this.storeAuditRecord(auditEntry);
			}
		} else if (Action.CREATE.toString().equals(action) || Action.DELETE.toString().equals(action)) {
			Map<String, Object> entityFields = null;

			if (Action.CREATE.toString().equals(action)) {
				try {
					entityFields = AuditingFields.extractFields(newObject);
				} catch (EntityAuditException e) {
					e.printStackTrace();
				}
			} else {
				try {
					entityFields = AuditingFields.extractFields(oldObject);
				} catch (EntityAuditException e) {
					e.printStackTrace();
				}
			}

			for (Map.Entry<String, Object> entityField : entityFields.entrySet()) {
				SysAuditEntry auditEntry = new SysAuditEntry();
				auditEntry.setAction(action);
				auditEntry.setFieldName(JpaUtils.getEntityDBFieldName(entityClass, entityField.getKey()));

				if (Action.CREATE.toString().equals(action)) {
					auditEntry.setKeyValue(JpaUtils.getIdentityAsString(newObject));
					auditEntry.setNewValue(String.valueOf(entityField.getValue()));
					auditEntry.setOldValue("");
				} else {
					auditEntry.setKeyValue(JpaUtils.getIdentityAsString(oldObject));
					auditEntry.setNewValue("");
					auditEntry.setOldValue(String.valueOf(entityField.getValue()));
				}

				auditEntry.setTableName(tableName);
				auditEntry.setSysUserAccount(userAccount);
				this.storeAuditRecord(auditEntry);
			}
		}
	}

	/**
	 * Simple audit method for logging actions without field-level tracking
	 * Used for operations like VIEW, LOGIN, etc.
	 */
	public void audit(Action action, String tableName, String keyValue, SysUserAccount userAccount, String description) {
		SysAuditEntry auditEntry = new SysAuditEntry();
		auditEntry.setAction(action.toString());
		auditEntry.setTableName(tableName);
		auditEntry.setKeyValue(keyValue);
		auditEntry.setSysUserAccount(userAccount);
		auditEntry.setFieldName(description);
		auditEntry.setNewValue("");
		auditEntry.setOldValue("");
		this.storeAuditRecord(auditEntry);
	}

	private void storeAuditRecord(SysAuditEntry auditEntry) {
		auditEntry.setDateCreated(new Timestamp(new Date().getTime()));
		this.sysAuditEntryService.addSysAuditEntry(auditEntry);
	}
}
