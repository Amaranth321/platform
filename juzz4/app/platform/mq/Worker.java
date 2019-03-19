package platform.mq;

public interface Worker {
	
	public void loop() throws Exception;
	
	public void close();

}
