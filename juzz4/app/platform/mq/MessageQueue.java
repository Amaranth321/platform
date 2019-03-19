package platform.mq;

import java.io.IOException;

/**
 * A meesage queue interface
 * @param <T> data type in this message queue
 */
/**
 * @author rs0939
 *
 * @param <T>
 */
public interface MessageQueue<T> {
	
	/**
	 * Open the connection of message queue
	 * @throws IOException
	 */
	public void open() throws IOException;
	
	/**
	 * Determine whether the message queue connection is open
	 * @return true if the connection is open, false otherwise
	 */
	public boolean isOpen();
	
	/**
	 * Create a queue for messages
	 * @param queueName name of the queue
	 * @throws IOException 
	 */
	public void createQueue(String queueName) throws IOException;

	/**
	 * Get total amount of messages in the queue
	 * @param queueName name of the queue
	 * @return amount of messages in the queue, return -1 if the queue doesn't exist
	 */
	public int queueMessages(String queueName);

	/**
	 * Get total amount of consumers connected to the queue
	 * @param queueName name of the queue
	 * @return amount of consumers connected to the queue, return -1 if the queue doesn't exist
	 */
	public int consumers(String queueName);
	
	/**
	 * Delete a queue
	 * @param queueName queue to delete
	 * @param force force deleting the queue even there're messages in the queue
	 * @return true on success, false otherwise
	 */
	public boolean deleteQueue(String queueName, boolean force) throws IOException;
	
	/**
	 * Publish a message to the queue
	 * @param message message
	 * @throws IOException
	 */
	public void publish(String queueName, T message) throws IOException;
	
	/**
	 * Close the message queue
	 */
	public void close();

}
