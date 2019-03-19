package com.kaisquare.sync;

/**
 * State of command
 */
public enum NodeCommandState {
	/**
	 * The command is added and ready to send
	 */
	Pending,
	/**
	 * The command is already sent to remote host (It may be sent in the queue) 
	 */
	Sending,
	/**
	 * The command is being processed by remote host.
	 */
	Processing,
	/**
	 * The command has been processed by local, and it's ready to send this response to remote host (Remote has not received yet).
	 * Just wait for remote host to retrieve this response
	 */
	Responding,
	/**
	 * The command has been processed, but remote host was failed to process the command.
	 */
	Failed,
	/**
	 * The command has been processed, and remote host successfully processed the command. 
	 */
	Success,
	/**
	 * The command is cancelled.
	 */
	Cancel
}
