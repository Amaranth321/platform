package com.kaisquare.kaisync.platform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Command implements Serializable {
	
	/**
	 * V1
	 */
	private static final long serialVersionUID = 1035982284430215440L;
	private String mId;
	private String mCommand;
	private List<String> mParameters;
	private String mOriginalId;
	
	public Command(String id, String command, String originalId)
	{
		mId = id;
		mCommand = command;
		mOriginalId = originalId;
		mParameters = new ArrayList<String>();
	}
	
	public String getId()
	{
		return mId;
	}
	
	public String getCommand()
	{
		return mCommand;
	}
	
	public List<String> getParameters()
	{
		return mParameters;
	}
	
	public String getOriginalId()
	{
		return mOriginalId;
	}
	
	/**
	 * Acknowledge this command 
	 */
	public void ack()
	{
	}
	
	/**
	 * Negative acknowledge this command, command will be requeued back into the queue system
	 */
	public void nack()
	{
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("id=%s, command=%s, originalId=%s, parameters=[", mId, mCommand, mOriginalId));
		for (int i = 0; i < mParameters.size(); i++)
		{
			if (i > 0)
				sb.append(", ");
			String p = mParameters.get(i);
			sb.append(p);
		}
		sb.append("]");
		
		return sb.toString();
	}
}
