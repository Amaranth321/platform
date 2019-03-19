/**
 *	PlatformSynchronizationService.thrift
 *
 * Copyright (C) KAI Square Pte Ltd
 *
 * This Thrift IDL defines synchronization interfaces between cloud server and KAI Node
 */

include "FileStructure.thrift"

namespace java com.kaisquare.kaisync.thrift
namespace cpp com.kaisquare.kaisync.thrift

struct NetAddress
{
	1:string host,
	2:i32 port
}

struct Command
{
	1:string id,
	2:string command,
	3:list<string> parameters,
	4:string originalId
}

service PlatformSynchronizationService
{
	/**
	 *	Get synchronized file for database
	 *
	 *	(1) identifier - the identifier which already be synchronized before
	 *
	 *	return the SyncFile object
	 */
	FileStructure.SyncFile getDbSyncFile(1:string identifier);

	/**
	 *	Get software update host
	 *
	 *	return NetAddress which includes host and port
	 *
	 */
	NetAddress getSoftwareUpdateHost();
	
	/*
	 * Get commands from server
	 *
	 * (1) identifier - the commands form specified identifier
	 *
	 *	return key/value commands, the value should be parameters of the 'key' command
	 */
	list<Command> getCommands(1:string identifier);
	
	/*
	 * Get commands from server
	 *
	 * (1) identifier - the commands form specified identifier
	 * (2) macAddress - Device MAC address
	 *
	 *	return key/value commands, the value should be parameters of the 'key' command
	 */
	list<Command> getCommands1(1:string identifier
					2:string macAddress);
	
	/*
	 * Send commands to server
	 *
	 * (1) identifier - the target for receiving commands
	 * (2) commands - wrapped command in a map object, key for command, value for command parameters
	 */
	bool sendCommands(1:string identifier,
					2:list<Command> commands);
					
	/*
	 * Send commands to server
	 *
	 * (1) identifier - the target for receiving commands
	 * (2) macAddress - Device MAC address
	 * (3) commands - wrapped command in a map object, key for command, value for command parameters
	 */
	bool sendCommands1(1:string identifier,
					2:string macAddress,
					3:list<Command> commands);

	/**
	 *	Syncs event video file from node to cloud.
	 *
	 *	(1) eventId - the event identifier
	 *	(2) nodeId - the node identifier, as registered on Cloud
	 *	(3) fileName - the file name
	 *
	 *	return the SyncFile object
	 */
	FileStructure.SyncFile syncEventVideoFile(1:string eventId,
						  2:string nodeId,
						  3:string fileName);

	/**
	 *	add log file to cloud platform
	 *
	 *	(1) nodeId - node id
	 *	(2) fileID - the file ID that uploaded to cloud
	 *
	 *	return TRUE on success adding file, FALSE otherwise
	 */
	bool addLogFile(1:string nodeId, 2:string fileID);
	
	/**
	 *	Upload a file to cloud
	 *
	 *	(2) fileName - the name of upload file
	 */
	FileStructure.SyncFile uploadFile(1:string fileName);
}
