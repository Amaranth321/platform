package platform.services;

public interface ResultStatistics {
	
	public int getTotalCount();
	
	public int getSuccessCount();
	
	public int getFailCount();
	
	public double getAverage();

	public long getLastProcessTime();
}
