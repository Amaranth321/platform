package com.kaisquare.kaisync;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public interface ISyncReadFile extends ISyncFile
{
	
	/**
	 * The {@link InputStream} for reading file from server
	 * @return
	 * @throws IOException 
	 */
	InputStream getInputStream() throws IOException;
	
	/**
	 * The {@link InputStream} for reading file from server
	 * @param position the position where file will be started for read
	 * @return
	 * @throws IOException 
	 */
	InputStream getInputStream(long position) throws IOException;
    
    /**
     * Set file read timeout, this should be set before opening the file
     * @param seconds the period of timeout in seconds
     */
    public void setReadTimeout(int seconds);
	
	/**
	 * Get size of the file, the positive number if it's able to get size of the file, otherwise it returns -1 for the unknown file size
	 * @return file size
	 */
	long getSize();
	
	/**
	 * Get the created date of the file 
	 * @return created date
	 */
	Date getCreatedDate();
	
	/**
	 * Get file hash string
	 * @return hash string
	 */
	String getHash();
}
