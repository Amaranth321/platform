package platform.kaisyncwrapper.node;

import com.kaisquare.sync.NodeCommandState;
import com.kaisquare.sync.TaskManager;
import lib.util.Util;
import models.command.node.PrioritizedNodeCommand;
import org.apache.commons.lang.StringUtils;
import platform.common.StatusPrinter;
import platform.debug.CommandLogType;
import platform.debug.PlatformDebugger;
import platform.kaisyncwrapper.KaiSyncHelper;
import platform.kaisyncwrapper.PlatformCommandQueue;
import play.Logger;
import play.jobs.Job;
import play.modules.morphia.Model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: Aye Maung
 */
public class PrioritizedCommandQueue extends PlatformCommandQueue<PrioritizedNodeCommand> implements StatusPrinter
{
    private static final PrioritizedCommandQueue instance = new PrioritizedCommandQueue();
    private static final AtomicBoolean queueProcessing = new AtomicBoolean(false);
    private static final AtomicInteger unknownQueueRestartCount = new AtomicInteger(0);

    public static PrioritizedCommandQueue getInstance()
    {
        return instance;
    }

    private PrioritizedCommandQueue()
    {
        startQueueChecker();
    }

    /**
     * Add the command to the queue that will be executed based on the priority.
     * Use this for updating information to cloud.
     * <p/>
     * Note: pending commands will be cancelled if a new one  of the same type is added ()
     *
     * @param command
     */
    @Override
    public void queueCommand(PrioritizedNodeCommand command)
    {
        cancelPendingIfAny(command);
        command.save();

        if (!queueProcessing.get())
        {
            processNextInQueue();
        }
    }

    /**
     * @return printed list of commands in the prioritized queue.
     */
    @Override
    public String getPrintedStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        try
        {
            Model.MorphiaQuery query = PrioritizedNodeCommand.q().order("-priority, -time");
            out.println(String.format("Prioritized Commands Queue (%s : %s total : %s unknown restarts)",
                                      queueProcessing.get() ?
                                      "Waiting" :
                                      "Idle", query.count(), unknownQueueRestartCount.get()));
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            Iterable<PrioritizedNodeCommand> commands = query.fetch();
            for (PrioritizedNodeCommand command : commands)
            {
                out.println(String.format("(P=%s) %s %s",
                                          command.getPriority(),
                                          PlatformDebugger.timestamp(command.getCreatedTime()),
                                          command.getCommandType()));
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
        PrioritizedNodeCommand.q().delete();
    }

    @Override
    protected void processNextInQueue()
    {
        PrioritizedNodeCommand priCmd = getNext();
        if (priCmd == null)
        {
            queueProcessing.getAndSet(false);
            return;
        }

        try
        {
            queueProcessing.getAndSet(true);
            sendToKaiSync(priCmd);

            //log
            String logMsg = Util.cutIfLong(StringUtils.join(priCmd.getParams(), " "), 100);
            debugger.logNodeCommand(priCmd, CommandLogType.SENT, logMsg);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            debugger.logNodeCommand(priCmd, CommandLogType.ERROR, e.getMessage());
        }

        dequeue(priCmd);
    }

    @Override
    protected synchronized PrioritizedNodeCommand getNext()
    {
        return PrioritizedNodeCommand.q()
                .order("priority, time")
                .first();
    }

    @Override
    protected synchronized void dequeue(PrioritizedNodeCommand command)
    {
        command.delete();
    }

    @Override
    protected void onCommandCompleted(PrioritizedNodeCommand command, NodeCommandState state)
    {
        debugger.logNodeCommand(command, CommandLogType.COMPLETED, state.toString());
        processNextInQueue();
    }

    private void cancelPendingIfAny(PrioritizedNodeCommand command)
    {
        try
        {
            TaskManager.getInstance().removeCommandsByType(command.getNodeId(), command.getCommandType());
            PrioritizedNodeCommand.q().filter("commandType", command.getCommandType()).delete();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void startQueueChecker()
    {
        //Monitor to make sure queue does not stop prematurely for whatever reasons
        new Job<Void>()
        {
            private boolean errorInQueue = false;

            @Override
            public void doJob()
            {
                Model.MorphiaQuery uncompleted = KaiSyncHelper.queryAllCommandsInProgress();

                if (getNext() != null && uncompleted.count() == 0)
                {
                    //happened twice in a row
                    if (errorInQueue)
                    {
                        Logger.info("Prioritized Queue has stopped. Restarting.");
                        unknownQueueRestartCount.getAndIncrement();
                        processNextInQueue();
                        errorInQueue = false;
                    }
                    else
                    {
                        errorInQueue = true;
                    }
                }
                else
                {
                    errorInQueue = false;
                }
            }
        }.every(15);
    }
}
