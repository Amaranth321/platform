package com.kaisquare.kaisync.transport;

/**
 * A factory for creating {@link ITransportListener} to handle received data from a connection
 * A {@link ITransportListener} will be created for each connections, which means, 
 * a {@link ITransportListener} only serve one connection
 */
public interface ITransportHandlerFactory {
	
	/**
	 * Get handler for handling connection data
	 * @return
	 */
	ITransportListener getTransportHandler();

}
