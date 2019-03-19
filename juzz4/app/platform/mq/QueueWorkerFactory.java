package platform.mq;

import java.io.IOException;

public interface QueueWorkerFactory<T> {
	
	/**
	 * Create queue consumer
	 * @param queueName queue to pull the messages
	 * @param processor the processor for the message of the queue
	 * @param autoAck set whether automatically send acknowledgment
	 * @param prefetch prefetch numbers of messages
	 * @return the consumer worker
	 * @throws IOException
	 */
	Worker createConsumer(String queueName, MessageProcessor<T> processor, boolean autoAck, int prefetch) throws IOException;

}
