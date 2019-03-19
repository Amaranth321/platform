package platform.kaisyncwrapper.node.tasks;

import com.google.gson.Gson;
import models.Analytics.NodeVcaInstance;
import models.NodeCommand;
import models.node.NodeObject;
import models.stats.VcaHourlyStats;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.analytics.VcaStatus;
import platform.rt.RTFeedManager;
import play.Logger;

import java.util.List;
import java.util.Map;

/**
 * Sent from nodes to update vca states on cloud
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeUpdateVcaStates extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonMap = parameters.get(0);
        NodeObject nodeObject = getNodeObject();

        Map<String, String> statesMap = new Gson().fromJson(jsonMap, Map.class);
        for (String instanceId : statesMap.keySet())
        {
            IVcaInstance dbInstance = VcaManager.getInstance().getVcaInstance(instanceId);
            if (dbInstance == null)
            {
                continue;
            }

            NodeVcaInstance nodeInst = (NodeVcaInstance) dbInstance;
            VcaStatus newStatus;
            String statusString = statesMap.get(instanceId);
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
        }

        nodeObject.save();

        return true;
    }

}
