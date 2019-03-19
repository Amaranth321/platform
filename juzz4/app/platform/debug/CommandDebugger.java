package platform.debug;

import models.NodeCommand;
import models.command.QueuedPlatformCommand;
import platform.Environment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * @author Aye Maung
 */
public class CommandDebugger
{
    private static CommandDebugger instance = new CommandDebugger();

    private CommandDebugger()
    {
    }

    public static CommandDebugger getInstance()
    {
        return instance;
    }

    public void logCloudCommand(NodeCommand command,
                                CommandLogType type,
                                String targetNodeName,
                                String logMsg)
    {
        if (!Environment.getInstance().onCloud())
        {
            return;
        }

        CommandLog commandLog = new CommandLog(command.getNodeId(), command.getCommand(), command.getParameters(),
                                               type, targetNodeName, logMsg);
        queue(commandLog);
    }

    public void logCloudCommand(QueuedPlatformCommand command,
                                CommandLogType type,
                                String targetNodeName,
                                String logMsg)
    {
        if (!Environment.getInstance().onCloud())
        {
            return;
        }

        CommandLog commandLog = new CommandLog(command.getNodeId(), command.getCommandType(), command.getParams(), type, targetNodeName, logMsg);
        queue(commandLog);
    }

    public void logNodeCommand(NodeCommand command,
                               CommandLogType type,
                               String logMsg)
    {
        if (!Environment.getInstance().onKaiNode())
        {
            return;
        }

        CommandLog commandLog = new CommandLog(command.getNodeId(), command.getCommand(), command.getParameters(), type, "Node", logMsg);
        queue(commandLog);
    }

    public void logNodeCommand(QueuedPlatformCommand command,
                               CommandLogType type,
                               String logMsg)
    {
        if (!Environment.getInstance().onKaiNode())
        {
            return;
        }

        CommandLog commandLog = new CommandLog(command.getNodeId(), command.getCommandType(), command.getParams(), type, "Node", logMsg);
        queue(commandLog);
    }

    public String getPrintedLogs()
    {
        int recentCount = 40;
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        try
        {
            out.println(String.format("(Last %s)", recentCount));
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            Iterator<CommandLog> iterator = CommandLog.q().order("-time").limit(recentCount).iterator();
            while (iterator.hasNext())
            {
                CommandLog log = iterator.next();
                out.println(String.format("%s %s", PlatformDebugger.timestamp(log.time), log.toString()));
            }
        }
        catch (Exception e)
        {
            out.println(e.getMessage());
        }
        finally
        {
            return sw.toString();
        }
    }

    private void queue(CommandLog commandLog)
    {
        commandLog.save();
    }
}
