package mw.nwra.ewaterpermit.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;

import mw.nwra.ewaterpermit.exception.EntityAuditException;

public class AuditingDiff {

	private AuditingDiff() {
	}

	public static DiffResult<Object> diff(Object object1, Object object2) throws EntityAuditException {
		Map<String, Object> object1FieldsMap = AuditingFields.extractFields(object1);
		Map<String, Object> object2FieldsMap = AuditingFields.extractFields(object2);

		DiffBuilder<Object> diffBuilder = new DiffBuilder<Object>(object1, object2, new AuditingToStringStyle());

		Set<String> object1FieldsMapKeys = object1FieldsMap.keySet();
		List<String> object1FieldNames = new ArrayList<String>(object1FieldsMapKeys.size());
		Iterator<String> object1FieldsMapKeysIterator = object1FieldsMapKeys.iterator();

		while (object1FieldsMapKeysIterator.hasNext()) {
			object1FieldNames.add(object1FieldsMapKeysIterator.next());
		}

		for (String object1FieldName : object1FieldNames) {
			diffBuilder.append(object1FieldName, object1FieldsMap.get(object1FieldName),
					object2FieldsMap.get(object1FieldName));
		}

		return diffBuilder.build();
	}
}
