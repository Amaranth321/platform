package models.transients;

import models.NodeLogFile;

public class NodeLogDevice {
	
	public String nodeId;
	public String name;
	public String bucketName;
	public String status;
	public String address;
	public String availableLog;
	public String filename;
	public NodeLogFile.PullingStatus pullingStatus;
}
