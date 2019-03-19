package com.kaisquare.kaisync.transport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.kaisquare.kaisync.utils.Utils;

public class StringKeyValueHeader {
	
	protected Map<String, String> mKeyValues;
	
	public StringKeyValueHeader(Map<String, String> headers)
	{
		mKeyValues = new HashMap<String, String>();
		mKeyValues.putAll(headers);
	}
	
	public String get(String key)
	{
		return mKeyValues.get(key);
	}

	public String getString(String key)
	{
		return get(key);
	}
	
	public long getLong(String key)
	{
		String value = getString(key);
		if (!Utils.isStringEmpty(value) && Pattern.matches("^[\\-]?\\d+$", value))
			return Long.parseLong(value);
		else
			return 0;
	}
	
	public int getInt(String key)
	{
		String value = getString(key);
		if (!Utils.isStringEmpty(value) && Pattern.matches("^[\\-]?\\d+$", value))
			return Integer.parseInt(value);
		else
			return 0;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, String>> iterator = mKeyValues.entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry<String, String> entry = iterator.next();
			String key = entry.getKey();
			String value = entry.getValue();
			
			sb.append(String.format("%s=%s\n", key, value));
		}
		sb.append("\n");
		
		return sb.toString();
	}
}
