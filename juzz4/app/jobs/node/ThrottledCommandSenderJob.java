package jobs.node;

import jobs.SingletonJob;
import models.command.ThrottledCommand;
import platform.kaisyncwrapper.node.SequencedCommandQueue;
import play.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Aye Maung
 */
public class ThrottledCommandSenderJob extends NodeCronJob implements SingletonJob
{
    private static final ThrottledCommandSenderJob instance = new ThrottledCommandSenderJob();

    private boolean started = false;

    public static ThrottledCommandSenderJob getInstance()
    {
        return instance;
    }

    @Override
    public void doJob()
    {
        Iterable<ThrottledCommand> commands = ThrottledCommand.queryReadyToTrigger().fetch();
        for (ThrottledCommand command : commands)
        {
            if (command.isNew()) //db problem
            {
                return;
            }

            Logger.debug("[ThrottledCommandSenderJob] Sending out command (%s)", command.toString());
            SequencedCommandQueue.getInstance().queueCommand(command.asSequencedCommand());
            command.delete();
        }
    }

    @Override
    public void start()
    {
        if (!started)
        {
            every(getFreqSeconds());
            started = true;
        }
    }

    @Override
    public int getFreqSeconds()
    {
        //this is the job frequency, NOT the throttling time
        return 2;
    }

    @Override
    public String getPrintedStatus()
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println(String.format("queue count   : %s", ThrottledCommand.q().count()));
        return sw.toString();
    }
}
