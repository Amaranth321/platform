package platform.kaisyncwrapper.node.tasks;

import models.Analytics.NodeTmpVcaInstance;
import models.Analytics.NodeVcaInstance;
import models.NodeCommand;
import models.stats.VcaHourlyStats;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.rt.RTFeedManager;

import java.util.List;

/**
 * Sent from nodes when vca is removed
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeVcaRemoved extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String vcaInstanceId = parameters.get(0);

        IVcaInstance dbInstance = VcaManager.getInstance().getVcaInstance(vcaInstanceId);
        if (dbInstance != null)
        {
            //notify UI
            RTFeedManager.getInstance().vcaInstanceChanged(dbInstance.getVcaInfo().getInstanceId(),
                                                           dbInstance.getVcaInfo().getCamera());

            ((NodeVcaInstance) dbInstance).delete();
        }

        //check tmp collection in case
        NodeTmpVcaInstance.find("vcaInfo.instanceId", vcaInstanceId).delete();

        //clear stats
        VcaHourlyStats.remove(vcaInstanceId);

        return true;
    }
}
