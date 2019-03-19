package com.kaisquare.kaisync.platform;

import java.net.InetSocketAddress;
import java.util.List;

import com.kaisquare.kaisync.IClientChannel;
import com.kaisquare.kaisync.ISyncFile;
import com.kaisquare.kaisync.file.IServerSyncFile;

/**
 * Server side Platform synchronization handler
 */
public interface IPlatformSyncHandler {
	
	/**
	 * Call this method before processing client's data, so this handler can get client information
	 * before the specific method of this handler be called.
	 * @param channel client channel
	 * @return true if this process should go on, otherwise connection will be closed and reject this process
	 */
	boolean beforeProcess(IClientChannel channel);

	/**
	 * Get software update host
	 * @return
	 */
	InetSocketAddress getSoftwareUpdateHost();
	
	/**
	 * Get commands for specific client
	 * @param identifier get commands from specified identifier
	 * @param macAddress get commands from the particular hardware
	 * @return command list
	 */
	List<Command> getCommands(String identifier, String macAddress);
	
	/**
	 * The commands are sent from client
	 * @param identifier commands that sent from specified identifier
	 * @param macAddress commands that sent from the particular hardware
	 * @param commands all commands
	 */
	void sendCommands(String identifier, String macAddress, List<Command> commands);

	/**
	 * Receives event video file in cloud.
	 * @param eventId The event ID.
	 * @param nodeId The node ID as registered on Cloud.
	 * @param fileName The file name.
	 * @return the {@link ISyncFile} that describes the file information for synchronization
	 */
	IServerSyncFile syncEventVideoFile(String eventId, String nodeId, String fileName);

	/**
	 * add log file to cloud
	 * @param nodeId Node ID
	 * @param fileID the file ID that uploaded to platform via {@link IPlatformSync#uploadFile()}
	 * @return True on success, otherwise False
	 */
	boolean addLogFile(String nodeId, String fileID);
	
	/**
	 * Upload a file to platform
	 * @param the name of upload file
	 * @return the {@link ISyncFile} that describes the file information to upload
	 */
	IServerSyncFile uploadFile(String fileName);
	
	/**
	 * Get the latest version
	 * @param deviceType the device type
	 * @param version specify the version of client
	 * @return version
	 */
	String getLatestVersion(DeviceType deviceType, double version);
	
	/**
	 * Get the latest update file
	 * @param deviceType the device type
	 * @param version specify the version of client
	 * @return update file description
	 */
	IServerSyncFile getLatestUpdateFile(DeviceType deviceType, double version);
	
	/**
	 * Get the update file which is on the server
	 * @param identifier identifier (device id)
	 * @param model the model for the update file
	 * @return
	 */
	IServerUpdateFileInfo getLatestUpdateFile(String identifier, String model);
	
}
