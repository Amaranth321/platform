package com.kaisquare.kaisync.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import com.kaisquare.kaisync.thrift.FileAction;

/**
 * The actual transport protocol interface
 */
public interface IFileTransport {
	
	/**
	 * Open remote file
	 * @param identifier the file ID to open
	 * @param action the option of file ({@link FileOptions#READ} or {@link FileOptions#WRITE}) 
	 * @param position start position to read/write
	 * @param metadata the metadata to this file
	 */
	 public void openFile(String identifier, FileOptions action, long position, Map<String, String> metadata) throws IOException;
	 
	/**
	 * Set file read timeout
	 * @param seconds the period of timeout in seconds
	 */
	public void setReadTimeout(int seconds);
	
	/**
	 * Set file write timeout
	 * @param seconds the period of timeout in seconds
	 */
	public void setWriteTimeout(int seconds);
	
	/**
	 * Read file from opened file by {@link #openFile(String, FileAction, long)}
	 * @param id the opened file id that given by {@link #openFile(String, FileAction, long)}
	 * @param chunkedSize read chunked size
	 * @return {@link ChunkedData} object
	 */
	public ChunkedData readFile(int chunkedSize) throws IOException;
	
	/**
	 * Write data to remote file, which is opened by {@link #openFile(String, FileAction, long)}
	 * @param id the opened file id that given by {@link #openFile(String, FileAction, long)}
	 * @param data data to write
	 * @param length the length of data that will be written
	 */
	public void writeFile(ByteBuffer data, int length) throws IOException;
	
	/**
	 * Write an {@link InputStream}, the transport will chunk data automatically
	 * @param in an InputStream to write
	 * @throws IOException 
	 */
	public void writeFile(InputStream in) throws IOException;
	
	/**
	 * Get total length of file for reading
	 * @return length of file
	 */
	public long getLength();
	
	/**
	 * Close file 
	 * @param close specific file by ID
	 */
	public void close();

}
