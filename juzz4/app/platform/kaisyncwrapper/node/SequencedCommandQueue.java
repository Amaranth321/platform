package platform.kaisyncwrapper.node;

import com.kaisquare.sync.CommandType;
import com.kaisquare.sync.NodeCommandState;
import com.kaisquare.sync.TaskManager;
import lib.util.Util;
import models.command.node.SequencedNodeCommand;
import org.apache.commons.lang.StringUtils;
import platform.common.StatusPrinter;
import platform.debug.CommandLogType;
import platform.debug.PlatformDebugger;
import platform.kaisyncwrapper.KaiSyncHelper;
import platform.kaisyncwrapper.PlatformCommandQueue;
import platform.node.KaiSyncCommandClient;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventSubscriber;
import platform.pubsub.PlatformEventTask;
import platform.pubsub.PlatformEventType;
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
public class SequencedCommandQueue extends PlatformCommandQueue<SequencedNodeCommand>
        implements PlatformEventSubscriber, StatusPrinter
{

    private static final SequencedCommandQueue instance = new SequencedCommandQueue();
    private static final AtomicBoolean queueProcessing = new AtomicBoolean(false);
    private static final AtomicInteger unknownQueueRestartCount = new AtomicInteger(0);

    public static SequencedCommandQueue getInstance()
    {
        return instance;
    }

    private SequencedCommandQueue()
    {
        startQueueChecker();
    }

    /**
     * These commands will be executed in the order they are added.
     * Use this mainly for user-triggered actions.
     *
     * @param command
     */
    @Override
    public void queueCommand(SequencedNodeCommand command)
    {
        cancelPendingIfAny(command);
        command.save();

        if (!KaiSyncCommandClient.isStartupSyncDone())
        {
            return;
        }

        if (!queueProcessing.get())
        {
            processNextInQueue();
        }
    }

    /**
     * @return printed list of commands in the sequenced queue.
     */
    @Override
    public String getPrintedStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        try
        {
            Model.MorphiaQuery query = SequencedNodeCommand.q().order("-time");
            out.println(String.format("Sequenced Commands Queue (%s : %s total : %s unknown restarts)",
                                      queueProcessing.get() ?
                                      "Waiting" :
                                      "Idle", query.count(), unknownQueueRestartCount.get()));
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            Iterable<SequencedNodeCommand> commands = query.fetch();
            for (SequencedNodeCommand command : commands)
            {
                out.println(String.format("%s %s",
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
        SequencedNodeCommand.q().delete();
    }

    @Override
    protected void processNextInQueue()
    {
        SequencedNodeCommand seqCmd = getNext();
        if (seqCmd == null)
        {
            queueProcessing.getAndSet(false);
            return;
        }

        try
        {
            queueProcessing.getAndSet(true);
            sendToKaiSync(seqCmd);

            //log
            String logMsg = Util.cutIfLong(StringUtils.join(seqCmd.getParams(), " "), 100);
            debugger.logNodeCommand(seqCmd, CommandLogType.SENT, logMsg);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            debugger.logNodeCommand(seqCmd, CommandLogType.ERROR, e.getMessage());
        }

        dequeue(seqCmd);
    }

    @Override
    protected synchronized SequencedNodeCommand getNext()
    {
        return SequencedNodeCommand.q()
                .order("time")
                .first();
    }

    @Override
    protected synchronized void dequeue(SequencedNodeCommand command)
    {
        command.delete();
    }

    @Override
    protected void onCommandCompleted(SequencedNodeCommand command, NodeCommandState state)
    {
        debugger.logNodeCommand(command, CommandLogType.COMPLETED, state.toString());
        processNextInQueue();
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
                        Logger.info("Sequenced Queue has stopped. Restarting.");
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

    @Override
    public void subscribePlatformEvents()
    {
        /**
         * Start the sequenced queue if startup sync are completed
         */
        PlatformEventMonitor.getInstance().subscribe(PlatformEventType.STARTUP_SYNC_COMPLETED,
                                                     new PlatformEventTask()
                                                     {
                                                         @Override
                                                         public void run(Object... params)
                                                         {
                                                             if (!queueProcessing.get())
                                                             {
                                                                 processNextInQueue();
                                                             }
                                                         }
                                                     });
    }

    private void cancelPendingIfAny(SequencedNodeCommand command)
    {
        try
        {
            //todo: add a more generic cancellation by type
            if (!command.getCommandType().equals(CommandType.NODE_UPDATE_VCA_STATES))
            {
                return;
            }

            TaskManager.getInstance().removeCommandsByType(command.getNodeId(), command.getCommandType());
            SequencedNodeCommand.q().filter("commandType", command.getCommandType()).delete();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }
}
