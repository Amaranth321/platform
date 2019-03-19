package com.kaisquare.kaisync.codec;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;

import com.kaisquare.kaisync.file.FileOptions;
import com.kaisquare.kaisync.transport.StringKeyValueHeader;
import com.kaisquare.kaisync.utils.Utils;

public class FileSyncInfo extends StringKeyValueHeader {

	public static final String FIELD_ID = "ID";
	public static final String FIELD_OPTION = "OPTION";
	public static final String FIELD_POSITION = "POSITION";
	public static final String FIELD_LENGTH = "LENGTH";
	public static final String FIELD_STATUS = "STATUS";
	public static final String FIELD_METADATA = "METADATA";
	
	public static final int STATUS_SUCCESS = 0;
	public static final int STATUS_FAILED = 1;
	
	public FileSyncInfo(Map<String, String> headers) {
		super(headers);
	}
	
	public String getID()
	{
		return getString(FIELD_ID);
	}
	
	public FileOptions getOption()
	{
		String option = getString(FIELD_OPTION);
		if (!Utils.isStringEmpty(option))
			return FileOptions.valueOf(option);
		
		return null;
	}

	public long getPosition()
	{
		return getLong(FIELD_POSITION);
	}
	
	public long getLength()
	{
		return getLong(FIELD_LENGTH);
	}
	
	public int getStatus()
	{
		int status = -1;
		try {
			status = Integer.parseInt(getString(FIELD_STATUS));
		} catch (NumberFormatException e) {
		}
		
		return status;
	}
	
	public String getMetadata()
	{
		return getString(FIELD_METADATA);
	}
	
	public static String toMetadataValue(Map<String, String> metadata)
	{
		if (metadata == null)
			return "";
		
		@SuppressWarnings("unchecked")
		Entry<String, String>[] entries = metadata.entrySet().toArray(new Entry[0]);
		
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> e : entries)
		{
			String value = String.format("%s:%s", e.getKey(), e.getValue());
			if (sb.length() > 0)
				sb.append(",");
			sb.append(Base64.encodeBase64URLSafeString(value.getBytes()));
		}
		
		return sb.toString();
	}
	
	public static Map<String, String> fromMetadataValue(String value)
	{
		HashMap<String, String> map = new HashMap<String, String>();
		
		if (!Utils.isStringEmpty(value))
		{
			String[] values = value.split(",");
			for (String v : values)
			{
				String origin = new String(Base64.decodeBase64(v));
				String[] keyvalue = origin.split("\\:", 2);
				if (keyvalue.length == 2)
					map.put(keyvalue[0], keyvalue[1]);
			}
		}
		
		return map;
	}
}
