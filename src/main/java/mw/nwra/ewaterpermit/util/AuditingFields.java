package mw.nwra.ewaterpermit.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.JoinColumn;
import mw.nwra.ewaterpermit.exception.EntityAuditException;

public class AuditingFields {

	private AuditingFields() {
	}

	public static Map<String, Object> extractFields(Object object) throws EntityAuditException {
		Field[] objectFields = object.getClass().getDeclaredFields();
		Map<String, Object> objectFieldsMap = new HashMap<String, Object>(objectFields.length);
		for (Field objectField : objectFields) {
			objectField.setAccessible(true);

			try {
				if (objectField.isAnnotationPresent(JoinColumn.class)) {
					// if foreign key
					objectFieldsMap.put(objectField.getName(), JpaUtils.getIdentityAsString(objectField.get(object)));
				} else {
					objectFieldsMap.put(objectField.getName(), objectField.get(object));
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new EntityAuditException(e);
			}
		}
		return objectFieldsMap;
	}
}
