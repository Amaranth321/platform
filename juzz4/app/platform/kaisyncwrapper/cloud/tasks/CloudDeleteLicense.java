package platform.kaisyncwrapper.cloud.tasks;

import lib.util.Util;
import platform.node.NodeProvisioning;
import play.Logger;
import play.jobs.Job;

/**
 * Sent from Cloud when node license is deleted by cloud
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */

public class CloudDeleteLicense extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        Util.printImptLog("%s received", getCommandType());

        //run in another thread to let the command respond
        Logger.info("Running factory reset in 5 seconds");
        new Job()
        {
            @Override
            public void doJob() throws Exception
            {
                NodeProvisioning.getInstance().factoryResetNode();
            }
        }.in(5);

        return true;
    }
}
