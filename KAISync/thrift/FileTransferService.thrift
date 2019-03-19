/**
 *	FileTransferService.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 *
 * This Thrift IDL defines methods for file transfer
 */

include "FileStructure.thrift"

namespace java com.kaisquare.kaisync.thrift
namespace cpp com.kaisquare.kaisync.thrift

enum FileAction {
	READ = 1,
	WRITE = 2
}

service FileTransferService
{
	/**
	 *	To tell server which file will be downloaded
	 *
	 *	(1) identifier - the identifier of the file that will be downloaded
	 *	(2)	action - The opened file action
	 *	(3) position - the position where the file will be started for read/write
	 *
	 *	return the unique id for the download request, to use the id for download later
	 */
	string openFile(1:string identifier, 
					2:FileAction action,
					3:i64 position),

	/**
	 *	Read file from server
	 *
	 *	(1) id - the unique id which returned by {@link FileTransferService#openFile}
	 *	(2)	chunkedSize - the chunked size of sending the data
	 *
	 *	return the FileChunk object
	 */
	FileStructure.FileChunk readFile(1:string id,
									2:i32 chunkedSize),

	/**
	 *	Write file to server
	 *
	 *	(1) id - the unique id which returned by {@link FileTransferService#openFile}
	 *	(2) binary - file binary data
	 *	(3) length - the length of data
	 *
	 */
	void writeFile(1:string id,
				2:binary data,
				3:i32 length),

	/**
	 *	Close the opened file, this is only for cancelling download file, server should close the opened file if client
	 *	has already finished downloading file. For file upload, it must call this method to tell server to finish receiving
	 *	data.
	 *
	 *	(1) id - the unique id which returned by {@link FileTransferService#openFile}
	 */
	bool closeFile(1:string id),

	/**
	 *	Delete an existing file
	 *
	 *	(1)	identifier - the identifier of existing file
	 *
	 *	return true on success deleting file, false otherwise
	 */
	bool deleteFile(1:string identifier)
}