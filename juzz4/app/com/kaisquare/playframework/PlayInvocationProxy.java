package com.kaisquare.playframework;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;

/**
 * Create a proxy that invokes methods of specific object with Play features (especially JPA)
 * @param <T> The type of object, it can be an interface
 */
public class PlayInvocationProxy<T> extends Invoker.Invocation implements InvocationHandler {
	
	private Object[] arguments;
	private Class<? extends T> targetClass;
	private T sourceObj;

	protected PlayInvocationProxy(Class<? extends T> classOfObj, Object...args) {
		arguments = args;
		targetClass = classOfObj;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object result = null;
		
		try {
			synchronized (targetClass)
			{
				if (sourceObj == null)
				{
					Class<?>[] params;
					if (arguments != null)
					{
						params = new Class<?>[arguments.length];
						for (int i = 0; i < arguments.length; i++)
							params[i] = arguments[i].getClass();
					}
					else
						params = new Class<?>[0];
					
					Constructor<? extends T> ctor = targetClass.getDeclaredConstructor(params);
					sourceObj = ctor.newInstance(arguments);
					arguments = null;
				}
			}
			try {
				if (init())
				{
					before();
					result = method.invoke(sourceObj, args);
				}
				after();
			} catch (Throwable e) {
				onException(e);
			} finally {
				_finally();
			}
		} catch (Exception e) {
			Logger.error(e, "failed to invoke from proxy");
		}
		
		return result;
	}

	@Override
	public void execute() throws Exception {
	}

	@Override
	public InvocationContext getInvocationContext() {
		return new InvocationContext("PlayProxy", targetClass.getAnnotations());
	}
	
	public static <T> T newProxyInstance(Class<T> interfaceOfClass, Class<? extends T> classOfObj, Object...args)
	{
		return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class[]{ interfaceOfClass },
				new PlayInvocationProxy<T>(classOfObj, args));
	}
}
