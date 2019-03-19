package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.utils.IndexDirection;
import com.kaisquare.sync.CommandType;
import com.kaisquare.sync.NodeCommandState;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

@Entity
@Indexes({
	@Index("nodeId, macAddress, state, createdTime"),
	@Index("nodeId, state, createdTime"),
	@Index("nodeId, command"),
	@Index("createdTime, state")
})
public class NodeCommand extends Model {
	
	private String nodeId;
	private String macAddress;
	private String sourceId = "";
	private CommandType command;
	private List<String> parameters;
	@Indexed(value=IndexDirection.ASC)
	private long createdTime;
	private long sendTime;
	private long processTime;
	@Indexed
	private NodeCommandState state;
	private boolean result;
	private boolean isReceivedCommand;

    public static MorphiaQuery queryByMac(String macAddress)
    {
        return q().filter("macAddress", macAddress);
    }

	public NodeCommand(String nodeId, String macAddress, CommandType command)
	{
		this(nodeId, macAddress, command, false);
	}
	
	public NodeCommand(String nodeId, String macAddress, CommandType command, boolean receivedCommand)
	{
		this.nodeId = nodeId;
		this.macAddress = macAddress.toLowerCase().intern();
		this.command = command;
		isReceivedCommand = receivedCommand;
		if (receivedCommand)
		{
			state = NodeCommandState.Processing;
			processTime = getUTCTime();
		}
		else
			state = NodeCommandState.Pending; 
		parameters = new ArrayList<String>();
		createdTime = getUTCTime();
	}
	
	public void setSourceId(String id)
	{
		sourceId = id;
	}
	
	public String getSourceId()
	{
		return sourceId;
	}
	
	public String getNodeId()
	{
		return nodeId;
	}
	
	public String getMacAddress()
	{
		return macAddress;
	}
	
	public CommandType getCommand()
	{
		return command;
	}
	
	public boolean isReceivedCommand()
	{
		return isReceivedCommand;
	}
	
	public List<String> getParameters()
	{
		return parameters;
	}
	
	public void setState(NodeCommandState s)
	{
		switch (s)
		{
		case Sending:
			sending();
			break;
		case Processing:
			processing();
			break;
		case Failed:
			failed();
			break;
		case Success:
			success();
			break;
		case Cancel:
			cancel();
		}
	}

	public boolean getResult() {
		return result;
	}
	
	public long getSendTime()
	{
		return sendTime;
	}

	public long getProcessTime()
	{
		return processTime;
	}
	
	public void sending()
	{
		if (state == NodeCommandState.Pending)
		{
			state = NodeCommandState.Sending;
			sendTime = getUTCTime();
		}
	}

	public void processing() {
		if (processTime == 0)
		{
			state = NodeCommandState.Processing;
			processTime = getUTCTime();
		}
	}
	
	public void respond(boolean result)
	{
		if (isReceivedCommand() && state != NodeCommandState.Responding)
		{
			state = NodeCommandState.Responding;
			this.result = result;
		}
	}
	
	public void failed()
	{
		if (state == NodeCommandState.Responding || 
			state == NodeCommandState.Processing || state == NodeCommandState.Sending)
			state = NodeCommandState.Failed;
	}
	
	public void success()
	{
		if (state == NodeCommandState.Responding || 
			state == NodeCommandState.Processing || state == NodeCommandState.Sending)
			state = NodeCommandState.Success;
	}
	
	public void cancel()
	{
		if (state == NodeCommandState.Pending)
			state = NodeCommandState.Cancel;
	}
	
	public NodeCommandState getState()
	{
		return state;
	}
	
	public long getCreatedTime()
	{
		return createdTime;
	}
	
	private long getUTCTime()
	{
		return DateTime.now(DateTimeZone.UTC).getMillis();
	}
}
