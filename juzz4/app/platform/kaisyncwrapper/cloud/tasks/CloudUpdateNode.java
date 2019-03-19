package platform.kaisyncwrapper.cloud.tasks;

import lib.util.Util;
import platform.node.KaiNodeAdminService;
import play.jobs.Job;

/**
 * sent from cloud when user click 'Update Now' button, will update software on node side.
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */
public class CloudUpdateNode extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        Util.printImptLog("Cloud started the software update");
        new Job()
        {
            @Override
            public void doJob()
            {
                KaiNodeAdminService.getInstance().updateNodeSoftware();
            }
        }.in(5);

        return true;
    }
}
