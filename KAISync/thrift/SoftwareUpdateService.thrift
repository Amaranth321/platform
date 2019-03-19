/**
 *	SoftwareUpdateService.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 *
 * This Thrift IDL defines methods for getting the latest version number for KAI Node
 */

include "FileStructure.thrift"

namespace java com.kaisquare.kaisync.thrift
namespace cpp com.kaisquare.kaisync.thrift

service SoftwareUpdateService
{
	/**
	 *	Get the latest version string
	 *
	 *	return version
	 */
	string getLatestVersion();

	/**
	 *	Get KAI Node update file
	 *
	 *	return SyncFile object
	 */
	FileStructure.SyncFile getLatestUpdateFile();
	/**
	 *	Get the latest version string
     *  (1) deviceType: the device type
	 *
	 *	return version
	 */
	string getLatestVersion0(1:string deviceType);

	/**
	 *	Get KAI Node update file
     *  (1) deviceType: the device type
	 *
	 *	return SyncFile object
	 */
	FileStructure.SyncFile getLatestUpdateFile0(1:string deviceType);
}
