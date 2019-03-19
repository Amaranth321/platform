/**
 *	FileStructure.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 *
 * This Thrift IDL defines file structure for file transfer
 */

namespace java com.kaisquare.kaisync.thrift
namespace cpp com.kaisquare.kaisync.thrift

/**
 *	(1) identifier - The unique id of file
 *	(2) length - The length of file
 *	(3)	host - File transfer server host
 *	(4) port - The port of file transfer server
 *	(5) date - The created time of file (epoch unix UTC times)
 */
struct SyncFile
{
	1:string identifier
	2:i64 length
	3:string host
	4:i32 port
	5:i64 createdTime;
	6:string hash
}

/**
 *	Received chunked data from server
 *
 *	(1)	size - total size was read
 *	(2)	data - file binary data
 */
struct FileChunk
{
	1:i32 size
	2:binary data
}