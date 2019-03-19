package models;

import java.util.Date;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;

import play.modules.morphia.Model;

@Entity
public class NodeLogFile extends Model {
	
	@Indexed
	public String nodeId;
	@Indexed
	public String filename;
	public PullingStatus status;
	public Date uploadedDate;
	
	public enum PullingStatus {
		Standby,
		Pulling
	}
	
	public NodeLogFile(String nodeId)
	{
		this.nodeId = nodeId;
		status = PullingStatus.Standby;
	}

}
