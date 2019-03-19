/*
 * ObjectUtil.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.util;

import java.math.BigDecimal;

/**
 * Utility methods for transforming objects of one type to another.
 *
 * @author Tan Yee Fan
 */
public class ObjectUtil {
	/**
	 * Private constructor.
	 */
	private ObjectUtil() {
	}

	/**
	 * Transforms an object into a {@code String}. If this cannot be done,
	 * then {@code null} is returned.
	 *
	 * @param obj Object.
	 */
	public static String asString(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof String) {
			return (String)obj;
		}
		// TODO: Figure whether to restrict this to Number objects only,
		// or any object that is not null.
		if (obj instanceof Number) {
			return obj.toString();
		}
		return null;
	}

	/**
	 * Transforms an object into a {@code Boolean}. If this cannot be done,
	 * then {@code null} is returned.
	 *
	 * @param obj Object.
	 */
	public static Boolean asBoolean(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Boolean) {
			return (Boolean)obj;
		}
		if (obj instanceof Number) {
			Number num = (Number)obj;
			if (num instanceof Float || num instanceof Double || num instanceof BigDecimal) {
				return Boolean.valueOf(num.doubleValue() != 0.0);
			}
			else {
				return Boolean.valueOf(num.intValue() != 0);
			}
		}
		if (obj instanceof String) {
			String str = (String)obj;
			try {
				return Boolean.valueOf(str);
			}
			catch (NumberFormatException e) {
			}
			try {
				int v = Integer.parseInt(str);
				return (v != 0);
			}
			catch (NumberFormatException e) {
			}
		}
		return null;
	}

	/**
	 * Transforms an object into a {@code Integer}. If this cannot be done,
	 * then {@code null} is returned.
	 *
	 * @param obj Object.
	 */
	public static Integer asInteger(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Integer) {
			return (Integer)obj;
		}
		if (obj instanceof Number) {
			Number num = (Number)obj;
			return Integer.valueOf(num.intValue());
		}
		if (obj instanceof String) {
			String str = (String)obj;
			try {
				return Integer.valueOf(str);
			}
			catch (NumberFormatException e) {
			}
		}
		return null;
	}

	/**
	 * Transforms an object into a {@code Long}. If this cannot be done,
	 * then {@code null} is returned.
	 *
	 * @param obj Object.
	 */
	public static Long asLong(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Long) {
			return (Long)obj;
		}
		if (obj instanceof Number) {
			Number num = (Number)obj;
			return Long.valueOf(num.longValue());
		}
		if (obj instanceof String) {
			String str = (String)obj;
			try {
				return Long.valueOf(str);
			}
			catch (NumberFormatException e) {
			}
		}
		return null;
	}

	/**
	 * Transforms an object into a {@code Float}. If this cannot be done,
	 * then {@code null} is returned.
	 *
	 * @param obj Object.
	 */
	public static Float asFloat(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Float) {
			return (Float)obj;
		}
		if (obj instanceof Number) {
			Number num = (Number)obj;
			return Float.valueOf(num.floatValue());
		}
		if (obj instanceof String) {
			String str = (String)obj;
			try {
				return Float.valueOf(str);
			}
			catch (NumberFormatException e) {
			}
		}
		return null;
	}

	/**
	 * Transforms an object into a {@code Double}. If this cannot be done,
	 * then {@code null} is returned.
	 *
	 * @param obj Object.
	 */
	public static Double asDouble(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Double) {
			return (Double)obj;
		}
		if (obj instanceof Number) {
			Number num = (Number)obj;
			return Double.valueOf(num.doubleValue());
		}
		if (obj instanceof String) {
			String str = (String)obj;
			try {
				return Double.valueOf(str);
			}
			catch (NumberFormatException e) {
			}
		}
		return null;
	}
}

