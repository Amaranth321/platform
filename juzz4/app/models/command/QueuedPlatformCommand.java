package models.command;

import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.utils.IndexDirection;
import com.google.gson.Gson;
import com.kaisquare.sync.CommandType;
import platform.Environment;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

/*
 * @author Aye Maung
 */
public abstract class QueuedPlatformCommand extends Model
{
    private final String nodeId;
    private final String macAddress;
    private final CommandType commandType;
    private final List<String> params;
    @Indexed(value = IndexDirection.ASC)
    private final long time;

    protected QueuedPlatformCommand(String nodeId,
                                    String macAddress,
                                    CommandType commandType,
                                    List<String> params)
    {
        this.nodeId = nodeId;
        this.macAddress = macAddress;
        this.commandType = commandType;
        this.params = params;
        time = Environment.getInstance().getCurrentUTCTimeMillis();
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", commandType, new Gson().toJson(params));
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public String getMacAddress()
    {
        return macAddress;
    }

    public CommandType getCommandType()
    {
        return commandType;
    }

    public List<String> getParams()
    {
        return params == null ? new ArrayList<String>() : params;
    }

    public String[] getParamsAsArray()
    {
        if (params == null || params.size() == 0)
        {
            return new String[0];
        }

        String[] array = new String[params.size()];
        array = params.toArray(array);
        return array;
    }

    public long getCreatedTime()
    {
        return time;
    }

}
