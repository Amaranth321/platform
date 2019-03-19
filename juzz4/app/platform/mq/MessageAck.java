package platform.mq;

/**
 * This is the interface that acknowledges the message whether it is properly processed or not
 */
public interface MessageAck {
	
	/**
	 * Get id of the message
	 * @return
	 */
	public String getMessageId();
	
	/**
	 * Acknowledge the message, we determine that the message is properly handled 
	 */
	public void ack();
	
	/**
	 * Something came up during the process, do not acknowledge the message, so the message will be re-sent again
	 */
	public void notAck();

}
