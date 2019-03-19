package com.kaisquare.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * This class is used to pass full generics type information, and avoid problems
 * with type erasure (that basically removes most usable type references from
 * runtime Class objects).
 * <p>
 * It is taken from Jackson JSON Processor, with additions based on:
 * http://gafter.blogspot.com/2006/12/super-type-tokens.html
 * <p>
 * Usage is by subclassing: here is one way to instantiate a reference to
 * generic type <code>List&lt;Integer&gt;</code>:
 * <pre>
 * TypeReference ref = new TypeReference<List<Integer>>() { };
 * </pre>
 */
public abstract class TypeReference<T> implements Comparable<TypeReference<T>> {

	private final Type type;

	protected TypeReference() {
		Type superClass = getClass().getGenericSuperclass();
		if (superClass instanceof Class<?>) { // sanity check, should never happen
			throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
		}
		type = ((ParameterizedType)superClass).getActualTypeArguments()[0];
	}

	/**
	 * Returns the type.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Instantiates a new instance of {@code T} using the constructor with
	 * no arguments.
	 */
	@SuppressWarnings("unchecked")
	public T newInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Class<?> rawType = type instanceof Class<?> ? (Class<?>)type : (Class<?>)((ParameterizedType)type).getRawType();
		Constructor<?> constructor = rawType.getConstructor();
		return (T)constructor.newInstance();
	}

	/**
	 * Instantiates a new instance of {@code T} using the constructor with
	 * the given parameter types.
	 */
	@SuppressWarnings("unchecked")
	public T newInstance(Class[] parameterTypes, Object[] parameterValues) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Class<?> rawType = type instanceof Class<?> ? (Class<?>)type : (Class<?>)((ParameterizedType)type).getRawType();
		Constructor<?> constructor = rawType.getConstructor(parameterTypes);
		return (T)constructor.newInstance(parameterValues);
	}

	/**
	 * The only reason we define this method (and require implementation
	 * of <code>Comparable</code>) is to prevent constructing a
	 * reference without type information.
	 */
	public int compareTo(TypeReference<T> o) {
		// just need an implementation, not a good one... hence:
		return 0;
	}
}
