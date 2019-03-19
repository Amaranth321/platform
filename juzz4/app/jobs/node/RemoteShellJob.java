package jobs.node;

import models.node.NodeRemoteShell;
import platform.node.NodeManager;
import play.Logger;
import play.jobs.Job;

public class RemoteShellJob extends Job
{

    //class variables

    //instance variables
    private String host;
    private String port;
    private String user;
    private boolean open;

    public RemoteShellJob()
    {
        NodeRemoteShell nrs = new NodeRemoteShell().find().first();
        if (nrs != null)
        {
            this.host = nrs.host;
            this.port = Integer.toString(nrs.port);
            this.user = nrs.username;
            this.open = nrs.open;
        }
    }

    public RemoteShellJob(String host, String port, String user)
    {
        this.host = host;
        this.port = port;
        this.user = user;
    }

    @Override
    public void doJob()
    {
        try
        {
            //run command to start autossh
            if (open)
            {
                NodeManager.getInstance().startRemoteShell(host, port, user);
            }

        }
        catch (Exception exp)
        {
            Logger.warn("%s:%s", "Error in RemoteShellJob", exp.toString());
            Logger.error(lib.util.Util.getStackTraceString(exp));
        }
        finally
        {
            Logger.info("RemoteShellJob: exited");
        }
    }
}

