package platform.debug;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import com.kaisquare.sync.CommandType;
import platform.Environment;
import play.modules.morphia.Model;

import java.util.List;

/**
 * Author:  Aye Maung
 */
@Entity
@Indexes({
        @Index("time")
})
public class CommandLog extends Model
{
    public final String nodeId;
    public final CommandType commandType;
    public final List<String> params;
    public final CommandLogType logType;
    public final String nodeName;
    public final String msg;
    public final long time;

    public CommandLog(String nodeId,
                      CommandType commandType,
                      List<String> params,
                      CommandLogType logType,
                      String nodeName,
                      String msg)
    {

        this.nodeId = nodeId;
        this.commandType = commandType;
        this.params = params;
        this.logType = logType;
        this.nodeName = nodeName;
        this.msg = msg;
        time = Environment.getInstance().getCurrentUTCTimeMillis();
    }

    @Override
    public String toString()
    {
        String format = "%-30s [%-9s] %s %-20s %s";
        String other = Environment.getInstance().onCloud() ? nodeName : "Cloud";
        String printed = "";
        if (logType == CommandLogType.PROCESSED)
        {
            printed = String.format(format, commandType, logType, "<=", other, msg);
        }
        else
        {
            printed = String.format(format, commandType, logType, "=>", other, msg);
        }
        return printed;
    }
}
