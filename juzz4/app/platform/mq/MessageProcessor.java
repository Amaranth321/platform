package platform.mq;

public interface MessageProcessor<T> {
	
	public void processMessage(T message, MessageAck messsageAck);

}
