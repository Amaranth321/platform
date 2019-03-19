package platform.kaisyncwrapper.node.tasks;

import lib.util.Util;
import models.Analytics.NodeVcaInstance;
import models.NodeCommand;
import models.stats.VcaHourlyStats;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.analytics.VcaStatus;
import platform.rt.RTFeedManager;
import play.Logger;

import java.util.List;

/**
 * Sent from nodes to update vca state on cloud
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeVcaStateChanged extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String vcaInstanceId = parameters.get(0);
        String statusString = parameters.get(1);

        IVcaInstance dbInstance = VcaManager.getInstance().getVcaInstance(vcaInstanceId);
        if (dbInstance == null)
        {
            Logger.error(Util.whichFn() + "vca not found (%s : %s)", getNodeName(), vcaInstanceId);
            return false;
        }

        NodeVcaInstance nodeInst = (NodeVcaInstance) dbInstance;
        VcaStatus newStatus;
        if (VcaStatus.isOldStatus(statusString))
        {
            /**
             * v4.4 and below
             */
            newStatus = VcaStatus.migrate(statusString, dbInstance.getVcaInfo().isEnabled());
            Logger.info("[%s] migrated %s status (%s to %s)",
                        getClass().getSimpleName(),
                        dbInstance.getVcaInfo(),
                        statusString,
                        newStatus);
        }
        else
        {
            newStatus = VcaStatus.parse(statusString);
        }

        nodeInst.setStatus(newStatus);
        nodeInst.save();

        //notify UI
        RTFeedManager.getInstance().vcaInstanceChanged(nodeInst.getVcaInfo().getInstanceId(),
                                                       nodeInst.getVcaInfo().getCamera());
        return true;
    }
}
