package com.kaisquare.kaisync.platform;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import com.kaisquare.kaisync.ISyncReadFile;
import com.kaisquare.kaisync.ISyncWriteFile;

/**
 * This interface defines what things the client can synchronize with cloud server
 * The client only needs to use this interface to communicate with server, and implements its own logic for synchronization,
 * it gets data or parameters via this interface. it doesn't need to consider how it connects or communicate with server. 
 */
public interface IPlatformSync extends Closeable {
	
//	public static final int FUNC_DB_FILE = 1;
	public static final int FUNC_SOFTWARE_HOST = 2;
	public static final int FUNC_GET_COMMAND = 3;
	public static final int FUNC_SEND_COMMAND = 4;
	public static final int FUNC_SYNC_VIDEO = 5;
	public static final int FUNC_ADD_LOG = 6;
	public static final int FUNC_UPLOAD_FILE = 7;
	public static final int FUNC_CHECK_LATEST_VERSION = 8;
	public static final int FUNC_GET_UPDATE_FILE = 9;
	public static final int FUNC_VALIDATE_CLIENT = 10;
	public static final int FUNC_REGISTER_REMOTE_SHELL = 11;
	public static final int FUNC_GET_LATEST_UPDATE_FILE = 12;
	
	/**
	 * Set connection timeout (default 60000ms)
	 * @param timeout timeout in milliseconds
	 */
	void setTimeout(int timeout);
	
	/**
	 * Get software update host
	 * @return
	 */
	InetSocketAddress getSoftwareUpdateHost();
	
	/**
	 * Get commands from server immediately
	 * @param identifier specify identifier to get commands
	 * @param macAddress the commands for the particular hardware
	 * @return list of commands
	 */
	List<Command> getCommands(String identifier, String macAddress);
	
	/**
	 * Bind a command received listener that receives command by callback
	 * @param identifier specify identifier to get commands
	 * @param macAddress the commands for the particular hardware
	 * @param listener a listener for command receiving 
	 * @return {@link CommandClient}
	 * @throws IOException 
	 */
	CommandClient bindCommands(String identifier, String macAddress, ICommandReceivedListener listener) throws IOException;
	
	/**
	 * Send commands to server
	 * @param identifier send commands to specific identifier
	 * @param macAddress the commands for the particular hardware
	 * @param commands all commands
	 * 
	 * return true on success, false otherwise
	 */
	boolean sendCommands(String identifier, String macAddress, List<Command> commands);
	
	/**
	 * Send commands to the client
	 * @param identifier send commands to specific identifier
	 * @param macAddress the commands for the particular hardware
	 * @param commands all commands
	 * return true on success, false otherwise
	 */
	boolean sendClientCommands(String identifier, String macAddress, List<Command> commands);

	/**
	 * Sends event video file to cloud.
	 * @param eventId The event ID.
	 * @param nodeId The node ID as registered on Cloud.
	 * @param fileName The file name.
	 * @return the {@link ISyncWriteFile} that describes the file information for synchronization
	 */
	ISyncWriteFile syncEventVideoFile(String eventId, String nodeId, String fileName);
	
	/**
	 * add log file to cloud
	 * @param nodeId Node ID
	 * @param fileID the file ID that uploaded to platform via {@link IPlatformSync#uploadFile()}
	 * @return True on success, otherwise False
	 */
	boolean addLogFile(String nodeId, String fileID);
	
	/**
	 * Upload a file to platform
	 * @param fileName the name of upload file
	 * @return the {@link ISyncWriteFile} that describes the file information to upload
	 */
	ISyncWriteFile uploadFile(String fileName);
	
	/**
	 * Get the latest version
	 * @return version
	 */
	String getLatestVersion(DeviceType type);

	/**
	 * Get the latest update file
	 * @return update file
	 */
	ISyncReadFile getLatestUpdateFile(DeviceType type);
	
	/**
	 * Get the update file info by giving device id or model id
	 * @param identifier the identifier (device id)
	 * @param modelId the model id for the update file (Optional)
	 * @return
	 */
	UpdateFileInfo getLatestUpdateFile(String identifier, String modelId);
	
	/**
	 * Push an event to cloud
	 * @param message the event message
	 * @return true if event is successfully pushed to cloud
	 */
	boolean pushEvent(byte[] message);
	
	/**
	 * Push an event to cloud according to specified target 
	 * @param target the target name
	 * @param key the message routing key
	 * @param message the event message
	 * @return true if event is successfully pushed to cloud
	 */
	boolean pushEvent(String target, String key, byte[] message);
	
	/**
	 * Set credentials if needed for this connection
	 * @param username
	 * @param password
	 */
	public void setCredentials(String username, String password);
	
	/**
	 * Ask cloud to create a command queue for the node client,
	 * cloud will validate that the node id and the MAC address is authorized to create command queue on the server
	 * @param identifier identifier
	 * @param macAddress client's MAC address
	 * @return true on success, false otherwise
	 * 
	 * @throws IOException if connection or server doesn't respond proper data or status, the IOException will be thrown,
	 * instead of just returning false
	 */
	public boolean validate(String identifier, String macAddress) throws IOException;
	
	/**
	 * Send a message to the specific exchange 
	 * @param target the target name
	 * @param type the type of the message
	 * @param message the message
	 * @return true on success, false otherwise
	 */
	public boolean sendMessage(String target, String type, MessagePacket message);
	
	/**
	 * Close the connection
	 */
	void close();
}
