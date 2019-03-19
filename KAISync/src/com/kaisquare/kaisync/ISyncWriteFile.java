package com.kaisquare.kaisync;

import java.io.IOException;
import java.io.OutputStream;

public interface ISyncWriteFile extends ISyncFile
{
	/**
	 * Set the information of the file, this is only 
	 * @param key
	 * @param value
	 */
	void setMetadata(String key, String value);
	
	/**
	 * The {@link OutputStream} for writing file on server
	 * @return
	 * @throws IOException 
	 */
	OutputStream getOutputStream() throws IOException;
    
    /**
     * Set file write timeout, this should be set before opening the file
     * @param seconds the period of timeout in seconds
     */
    public void setWriteTimeout(int seconds);

}
