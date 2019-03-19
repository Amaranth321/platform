package com.kaisquare.kaisync.codec;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.kaisquare.kaisync.transport.StringKeyValueHeader;
import com.kaisquare.kaisync.utils.AppLogger;
import com.kaisquare.kaisync.utils.Utils;

public class PlatformCommand extends StringKeyValueHeader {
	
	public static final String FIELD_COMMAND_ID = "command_id";
	public static final String FIELD_IDENTIFIER = "identifier";
	public static final String FIELD_MAC_ADDRESS = "mac_addr";
	public static final String FIELD_ACTION = "action";
	public static final String FIELD_COMMAND = "command";
	public static final String FIELD_PARAMETERS = "parameters";
	public static final String FIELD_ORIGIN_ID = "origin_id";
	
	private List<String> mParams;

	public PlatformCommand(Map<String, String> headers) {
		super(headers);
	}
	
	public String getID()
	{
		return getString(FIELD_COMMAND_ID);
	}

	public String getIdentifier()
	{
		return getString(FIELD_IDENTIFIER);
	}
	
	public String getMacAddress()
	{
		return getString(FIELD_MAC_ADDRESS);
	}
	
	public String getAction()
	{
		return getString(FIELD_ACTION);
	}
	
	public String getCommand()
	{
		return getString(FIELD_COMMAND);
	}
	
	public List<String> getParameters()
	{
		if (mParams == null)
		{
			mParams = new ArrayList<String>();
			String s = getString(FIELD_PARAMETERS);
			if (!Utils.isStringEmpty(s))
			{
				String[] params = s.split(" ");
				for (String p : params)
				{
					if (!Utils.isStringEmpty(p))
					{
						try {
							mParams.add(URLDecoder.decode(p, "utf8"));
						} catch (UnsupportedEncodingException e) {
							AppLogger.e(this, e, "");
							mParams.add(p);
						}
					}
				}
			}
		}
		
		return new ArrayList<String>(mParams);//clone a new list
	}

	public String getParameterString() {
		return getString(FIELD_PARAMETERS);
	}
	
	public String getOriginID()
	{
		return getString(FIELD_ORIGIN_ID);
	}
}
