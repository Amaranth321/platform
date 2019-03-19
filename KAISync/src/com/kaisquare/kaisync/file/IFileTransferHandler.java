package com.kaisquare.kaisync.file;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Server side File Transfer handler
 */
public interface IFileTransferHandler {
	
	/**
	 * Open file for reading
	 * @param identifier The file id that will be opened
	 * @param options the file action, refer to {@link FileOptions}
	 * @param position the position where file will be started for read/write
	 * @param metadata the metadata of the file (only available for write file)
	 * @return the unique id for opened file, the id will be passed by 
	 * {@link IFileTransferHandler#readFile(String, byte[], int, int)}, {@link IFileTransferHandler#writeFile(String, byte[], int, int)}, {@link IFileTransferHandler#closeFile(String)}
	 */
	String openFile(String identifier, FileOptions option, long position, Map<String, String> metadata);
	
	/**
	 * Read file data from specific identifier
	 * @param identifier The opened file id
	 * @param b the buffer into which the data is read.
	 * @param offset the start offset in array b at which the data is written.
	 * @param length the maximum number of bytes to read
	 * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
	 */
	int readFile(String identifier, byte[] b, int offset, int length);
	
	/**
	 * Write data to file
	 * @param identifier the opened file id which returned by {@link IFileTransferHandler#openFile)}
	 * @param src the data
	 * @param offset the start offset in the data
	 * @param length the number of bytes to write
	 */
	void writeFile(String identifier, byte[] src, int offset, int length);

	/**
	 * Close the opened file
	 * @param identifier the file opened by {@link IFileTransferHandler#openInputFile(String)}
	 * @return true on success, false otherwise
	 */
	boolean closeFile(String identifier);

	/**
	 * Delete an existing file
	 * @param identifier the unique id of existing file
	 * @return
	 */
	boolean deleteFile(String identifier);
	
	/**
	 * Get file length
	 * @param identifier the identifier of file
	 * @return the length of file
	 */
	long getFileLength(String identifier);
	
	/**
	 * This handler is no longer needed
	 */
	void close();

	InputStream getInputStream(String identifier);
	
	OutputStream getOutputStream(String identifier);
}
