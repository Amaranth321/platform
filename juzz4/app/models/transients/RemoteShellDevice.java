package models.transients;

import models.RemoteShellState.ConnectionState;

public class RemoteShellDevice {
	
	public Long nodeId;
	public String status;
	public String name;
	public String bucketName;
	public String address;
	public String host;
	public int port;
	public String username;
	public ConnectionState connectionState;
}
