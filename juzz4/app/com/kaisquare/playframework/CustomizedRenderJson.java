package com.kaisquare.playframework;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.ivy.core.module.descriptor.ExcludeRule;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;

import play.Logger;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.RenderJson;

public class CustomizedRenderJson extends RenderJson {

	private String json;

	public CustomizedRenderJson(Object o) {
		super(o);
		json = getGson().toJson(o);
	}

	public CustomizedRenderJson(String jsonString) {
		super(jsonString);
		json = jsonString;
	}

	public CustomizedRenderJson(Object o, Type type) {
		super(o, type);
		json = getGson().toJson(o, type);
	}

	public CustomizedRenderJson(Object o, JsonSerializer<?>... adapters) {
		super(adapters);
		json = getGson(adapters).toJson(o);
	}
	
	@Override
	public void apply(Request request, Response response) {
        try {
            String encoding = getEncoding();
            setContentTypeIfNotSet(response, "application/json; charset="+encoding);
            response.out.write(json.getBytes(encoding));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

	protected Gson getGson(JsonSerializer<?>... adapters)
	{
		GsonBuilder gson = new GsonBuilder();
        for (Object adapter : adapters) {
            Type t = getMethod(adapter.getClass(), "serialize").getParameterTypes()[0];
            gson.registerTypeAdapter(t, adapter);
        }
        gson.setExclusionStrategies(ExposeExclusionStrategy.INSTANCE);
        return gson.create();
	}
	
	static Method getMethod(Class clazz, String methodName) {
        Method bestMatch = null;
        for(Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && !m.isBridge()) {
                if (bestMatch == null || !Object.class.equals(m.getParameterTypes()[0])) {
                    bestMatch = m;
                }
            }
        }
        return bestMatch;
    }
	
	static class ExposeExclusionStrategy implements ExclusionStrategy
	{
		public static final ExposeExclusionStrategy INSTANCE = new ExposeExclusionStrategy();
		
		private ExposeExclusionStrategy() {}

		@Override
		public boolean shouldSkipField(FieldAttributes f) {
			Expose anno = f.getAnnotation(Expose.class);
			return anno != null && !anno.serialize();
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
		
	}
}
