package com.power.common.util;

import com.power.common.model.EnumDictionary;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Enum helpers.
 */
public final class EnumUtil {

	private EnumUtil() {
		throw new IllegalStateException("Utility class");
	}

	@SuppressWarnings("unchecked")
	public static <T extends EnumDictionary> List<T> getEnumInformation(Class<?> enumClass, String codeField,
			String descField) {
		if (Objects.isNull(enumClass)) {
			throw new RuntimeException("Enum class can't be null.");
		}
		if (!enumClass.isEnum()) {
			throw new RuntimeException(enumClass.getCanonicalName() + " is not an enum class.");
		}
		if (StringUtil.isEmpty(codeField) || StringUtil.isEmpty(descField)) {
			throw new RuntimeException(enumClass.getCanonicalName()
					+ ":Please specify the code field name of the dictionary enumeration class and the field name that describes the dictionary code information");
		}

		String codeAccessor = toAccessorName(codeField);
		String descAccessor = toAccessorName(descField);
		List<T> dictionaries = new ArrayList<>();

		for (Object enumConstant : enumClass.getEnumConstants()) {
			Enum<?> enumValue = (Enum<?>) enumConstant;
			Object code = readAccessor(enumClass, enumValue, codeAccessor);
			Object desc = readAccessor(enumClass, enumValue, descAccessor);

			EnumDictionary item = new EnumDictionary();
			item.setType(resolveSimpleTypeName(code));
			item.setDesc(String.valueOf(desc));
			item.setValue(String.valueOf(code));
			item.setName(enumValue.name());
			item.setOrdinal(enumValue.ordinal());
			dictionaries.add((T) item);
		}

		return dictionaries;
	}

	public static Object getFieldValueByMethod(Class<?> enumClass, String methodName) {
		return getFieldValueByMethod(enumClass, methodName, null);
	}

	public static Object getFieldValueByMethod(Class<?> enumClass, String methodName, String enumName) {
		Enum<?> enumConstant = resolveEnumConstant(enumClass, enumName);
		if (enumConstant == null || StringUtil.isEmpty(methodName)) {
			return null;
		}

		String normalized = methodName.endsWith("()") ? methodName.substring(0, methodName.length() - 2) : methodName;
		Object value = invokeMethod(enumClass, enumConstant, normalized);
		if (value != null || normalized.startsWith("get") || normalized.startsWith("is")) {
			return value;
		}
		String getterName = "get" + StringUtil.firstToUpperCase(normalized);
		return invokeMethod(enumClass, enumConstant, getterName);
	}

	public static Object getFieldValue(Class<?> enumClass, String fieldName) {
		return getFieldValue(enumClass, fieldName, null);
	}

	public static Object getFieldValue(Class<?> enumClass, String fieldName, String enumName) {
		Enum<?> enumConstant = resolveEnumConstant(enumClass, enumName);
		if (enumConstant == null || StringUtil.isEmpty(fieldName)) {
			return null;
		}

		try {
			Field field = enumClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(enumConstant);
		}
		catch (ReflectiveOperationException ignored) {
			return getFieldValueByMethod(enumClass, fieldName, enumName);
		}
	}

	private static String toAccessorName(String fieldName) {
		String normalized = fieldName.trim();
		if (normalized.endsWith("()")) {
			return normalized.substring(0, normalized.length() - 2);
		}
		if (normalized.startsWith("get") || normalized.startsWith("is")) {
			return normalized;
		}
		return "get" + StringUtil.firstToUpperCase(normalized);
	}

	private static Object readAccessor(Class<?> enumClass, Enum<?> enumConstant, String accessorName) {
		Object value = invokeMethod(enumClass, enumConstant, accessorName);
		if (value != null) {
			return value;
		}
		String fallbackName = StringUtil.firstToLowerCase(accessorName.replaceFirst("^(get|is)", StringUtil.EMPTY));
		if (StringUtil.isNotEmpty(fallbackName)) {
			try {
				Field field = enumClass.getDeclaredField(fallbackName);
				field.setAccessible(true);
				return field.get(enumConstant);
			}
			catch (ReflectiveOperationException ignored) {
				return null;
			}
		}
		return null;
	}

	private static Object invokeMethod(Class<?> enumClass, Enum<?> enumConstant, String methodName) {
		try {
			Method method = enumClass.getDeclaredMethod(methodName);
			method.setAccessible(true);
			return method.invoke(enumConstant);
		}
		catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	private static Enum<?> resolveEnumConstant(Class<?> enumClass, String enumName) {
		if (enumClass == null || !enumClass.isEnum()) {
			return null;
		}
		Object[] constants = enumClass.getEnumConstants();
		if (constants == null || constants.length == 0) {
			return null;
		}
		if (StringUtil.isEmpty(enumName)) {
			return (Enum<?>) constants[0];
		}
		for (Object constant : constants) {
			Enum<?> enumConstant = (Enum<?>) constant;
			if (Objects.equals(enumName, enumConstant.name())) {
				return enumConstant;
			}
		}
		return null;
	}

	private static String resolveSimpleTypeName(Object value) {
		if (value == null) {
			return "string";
		}
		return value.getClass().getSimpleName().toLowerCase(Locale.ROOT);
	}

}
