package mw.nwra.ewaterpermit.util;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

public class JpaUtils {

	private JpaUtils() {
	}

	public static Object getIdentity(Object entity) {
		Class<?> currentClass = entity.getClass();

		// Check fields in current class and all superclasses
		while (currentClass != null && currentClass != Object.class) {
			Field[] fields = currentClass.getDeclaredFields();

			for (Field field : fields) {
				field.setAccessible(true);

				if (field.isAnnotationPresent(Id.class)) {
					try {
						return field.get(entity);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}

			currentClass = currentClass.getSuperclass();
		}

		return null;
	}

	public static String getIdentityName(Serializable entity) {
		Class<?> currentClass = entity.getClass();

		// Check fields in current class and all superclasses
		while (currentClass != null && currentClass != Object.class) {
			Field[] fields = currentClass.getDeclaredFields();

			for (Field field : fields) {
				field.setAccessible(true);

				if (field.isAnnotationPresent(Id.class)) {
					return field.getName();
				}
			}

			currentClass = currentClass.getSuperclass();
		}

		return null;
	}

	public static Class<?> getIdentityType(Class<?> clazz) {
		Class<?> currentClass = clazz;

		// Check fields in current class and all superclasses
		while (currentClass != null && currentClass != Object.class) {
			Field[] fields = currentClass.getDeclaredFields();

			for (Field field : fields) {
				field.setAccessible(true);

				if (field.isAnnotationPresent(Id.class)) {
					try {
						return field.getType();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}

			currentClass = currentClass.getSuperclass();
		}

		return null;
	}

	public static String getIdentityAsString(Object entity) {
		Object id = JpaUtils.getIdentity(entity);

		if (id != null) {

			if (id.getClass() == String.class) {
				return (String) id;
			} else if (id.getClass() == long.class) {
				return Long.toString((long) id);
			} else if (id.getClass() == int.class) {
				return Integer.toString((int) id);
			} else {
				return String.valueOf(id);
			}
		}

		return null;
	}

	public static String getEntityTableName(Class<?> clazz) {
		if (clazz.isAnnotationPresent(Table.class)) {
			Table table = clazz.getAnnotation(Table.class);

			return table.name();
		}

		return null;
	}

	public static String getEntityDBFieldName(Class<?> clazz, String field) {
		try {
			Field fld = clazz.getDeclaredField(field);
			if (fld.isAnnotationPresent(Column.class)) {
				Annotation annotation = fld.getAnnotation(Column.class);
				if (annotation instanceof Column) {
					Column col = (Column) annotation;
					field = col.name();
				}
			} else if (fld.isAnnotationPresent(JoinColumn.class)) {
				Annotation annotation = fld.getAnnotation(JoinColumn.class);
				if (annotation instanceof JoinColumn) {
					JoinColumn col = (JoinColumn) annotation;
					field = col.name();
				}

			}
		} catch (Exception e) {
		}
		return field;
	}

	// For mapping result set to list of objects
	public static List<Object> formartResultSet(ResultSet rs) throws SQLException {
		List<Object> data = new ArrayList<>();
		// collect column names
		List<String> columnNames = new ArrayList<>();
		ResultSetMetaData rsmd = rs.getMetaData();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			columnNames.add(rsmd.getColumnLabel(i));
		}

		while (rs.next()) {
			Map<String, Object> rowData = new LinkedHashMap<String, Object>();
			for (String col : columnNames) {
				rowData.put(col, rs.getObject(col));
			}
			data.add(rowData); // add row
		}
		return data;
	}
}
