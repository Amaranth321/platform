package com.kaisquare.kaisync.platform;

import java.util.List;

/**
 * A listener callback receiving commands from server
 */
public interface ICommandReceivedListener {
	
	/**
	 * Called when commands are received
	 * @param identifier the bound identifier for commands
	 * @param macAddress the bound macAddress for commands
	 * @param commands received commands
	 * 
	 * @return true on success processing commands, false otherwise
	 */
	boolean onCommandReceived(String identifier, String macAddress, List<Command> commands);

}
