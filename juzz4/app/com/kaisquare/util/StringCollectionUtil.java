/*
 * StringCollectionUtil.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.util;

import java.util.Collection;

/**
 * Utility method for converting collections to strings.
 *
 * @author Tan Yee Fan
 */
public class StringCollectionUtil {
	private StringCollectionUtil() {
	}

	/**
	 * Splits a string into a collection of strings using the specified
	 * delimiter.
	 */
	public static <T extends Collection<String>> T split(String string, String regex, TypeReference<T> collectionTypeRef) {
		try {
			T collection = collectionTypeRef.newInstance();
			if (string != null && regex != null) {
				String[] tokens = string.split(regex);
				for (String token: tokens) {
					if (token != null) {
						collection.add(token);
					}
				}
			}
			return collection;
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Splits a string into a collection of objects using the specified
	 * delimiter. Here, it is assumed the class of the collection elements
	 * contains a constructor that accepts a single string argument.
	 */
	public static <T extends Collection<? super U>, U> T split(String string, String regex, TypeReference<T> collectionTypeRef, TypeReference<? extends U> elementTypeRef) {
		try {
			T collection = collectionTypeRef.newInstance();
			if (string != null && regex != null) {
				String[] tokens = string.split(regex);
				for (String token: tokens) {
					if (token != null) {
						try {
							Class[] classes = {String.class};
							Object[] values = {token};
							U element = elementTypeRef.newInstance(classes, values);
							collection.add(element);
						}
						catch (Exception e) {
						}
					}
				}
				return collection;
			}
			return collection;
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Join a collection of objects into a string using the specified
	 * delimiter.
	 */
	public static <T> String join(Collection<T> collection, String delimiter) {
		StringBuilder builder = new StringBuilder();
		if (collection != null) {
			boolean first = true;
			for (T element: collection) {
				if (first)
					first = false;
				else
					builder.append(delimiter);
				builder.append(element);
			}
		}
		return builder.toString();
	}
	
	public static boolean isEmpty(String s)
	{
		return s == null || "".equals(s.trim());
	}
}

