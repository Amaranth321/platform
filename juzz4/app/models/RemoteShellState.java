package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;

import play.modules.morphia.Model;

@Entity
public class RemoteShellState extends Model {
	
	@Indexed
	public String cloudPlatformDeviceId;
	public String macAddress;
	public String host;
	public int port;
	public String username;
	@Deprecated
	public boolean open; //Not been used in v4.5 onwards
	@Deprecated
	public boolean stopped; //Not been used in v4.5 onwards
	public boolean registered; //registered by the individual process, instead of platform
	public ConnectionState connectionState;

	public enum ConnectionState {
		CLOUD_REQUESTED_START,
		CLOUD_REQUESTED_STOP,
		NODE_CONNECTED,
		NODE_DISCONNECTED
	}
}
