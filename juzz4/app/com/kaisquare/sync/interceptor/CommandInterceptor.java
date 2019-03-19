package com.kaisquare.sync.interceptor;

import models.NodeCommand;

/**
 * A command that will be sent in a different way
 */
public interface CommandInterceptor {
	
	/**
	 * Handle the command in its own way 
	 * @param command
	 * @return true if it handles by itself, otherwise the command will be sent by the original way
	 */
	boolean intercept(NodeCommand command);

}
