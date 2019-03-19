package platform.kaisyncwrapper.cloud;

import com.kaisquare.sync.NodeCommandState;
import lib.util.Util;
import models.command.cloud.CloudNodeCommand;
import org.apache.commons.lang.StringUtils;
import platform.common.StatusPrinter;
import platform.debug.CommandLogType;
import platform.kaisyncwrapper.KaiSyncHelper;
import platform.kaisyncwrapper.PlatformCommandQueue;
import play.Logger;
import play.modules.morphia.Model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author:  Aye Maung
 * <p/>
 * For sending commands from cloud to each node in the order commands are called
 * <p/>
 * Not singleton like {@link platform.kaisyncwrapper.node.SequencedCommandQueue}
 * At runtime, each node will have its own instance of PerNodeQueue
 */
public class PerNodeQueue extends PlatformCommandQueue<CloudNodeCommand> implements StatusPrinter
{
    private final String nodeName;
    private final String nodeId;
    private final AtomicBoolean queueProcessing = new AtomicBoolean(false);
    private boolean stopIssued = false;

    public PerNodeQueue(String nodeName, String nodeId)
    {
        this.nodeName = nodeName;
        this.nodeId = nodeId;
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public String getNodeName()
    {
        return nodeName;
    }

    public void process()
    {
        if (!isProcessing())
        {
            processNextInQueue();
        }
    }

    public void stopAfterCurrentTask()
    {
        stopIssued = true;
    }

    public boolean isEmpty()
    {
        return query().count() == 0;
    }

    public boolean isProcessing()
    {
        return queueProcessing.get();
    }

    public boolean pendingInKaiSync()
    {
        return KaiSyncHelper.queryCommandsInProgress(nodeId).count() > 0;
    }

    public void reset()
    {
        queueProcessing.getAndSet(false);
    }

    public Model.MorphiaQuery query()
    {
        return CloudNodeCommand.q().filter("nodeId", nodeId).order("time");
    }

    @Override
    public void queueCommand(CloudNodeCommand command)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPrintedStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        try
        {
            Model.MorphiaQuery query = CloudNodeCommand.q().filter("nodeId", nodeId).order("time");
            long count = query.count();
            if (count > 0)
            {
                out.println(String.format("[%s] %s in queue (%s)", queueProcessing.get() ? "Waiting" : "Idle",
                                          count, nodeName));
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

    @Override
    public void clearAll()
    {
        CloudNodeCommand.q().filter("nodeId", nodeId).delete();
    }

    @Override
    protected void processNextInQueue()
    {
        CloudNodeCommand nodeCmd = getNext();
        if (nodeCmd == null)
        {
            return;
        }

        try
        {
            queueProcessing.getAndSet(true);
            sendToKaiSync(nodeCmd);

            //log
            String logMsg = Util.cutIfLong(StringUtils.join(nodeCmd.getParams(), " "), 100);
            debugger.logCloudCommand(nodeCmd, CommandLogType.SENT, nodeName, logMsg);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            debugger.logCloudCommand(nodeCmd, CommandLogType.ERROR, nodeName, e.getMessage());
        }

        dequeue(nodeCmd);
    }

    @Override
    protected CloudNodeCommand getNext()
    {
        return query().first();
    }

    @Override
    protected void dequeue(CloudNodeCommand command)
    {
        command.delete();
    }

    @Override
    protected void onCommandCompleted(CloudNodeCommand command, NodeCommandState state)
    {
        debugger.logCloudCommand(command, CommandLogType.COMPLETED, nodeName, state.toString());
        queueProcessing.getAndSet(false);

        if (!stopIssued)
        {
            processNextInQueue();
        }
    }
}
