package platform.mq;

public interface QueueService extends Runnable {
	
	public void setPrefetch(int prefetch);
	
	public void setAutoAck(boolean autoAck);
	
	public void close();

}
