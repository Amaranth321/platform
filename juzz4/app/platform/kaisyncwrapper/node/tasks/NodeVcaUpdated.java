package platform.kaisyncwrapper.node.tasks;

import lib.util.Util;
import models.Analytics.NodeVcaInstance;
import models.NodeCommand;
import models.node.NodeObject;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaInfo;
import platform.analytics.VcaManager;
import platform.kaisyncwrapper.KaiSyncHelper;
import play.Logger;

import java.util.List;

/**
 * Sent from nodes when vca is updated
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */

public class NodeVcaUpdated extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonInfo = parameters.get(0);

        NodeObject nodeObject = getNodeObject();
        VcaInfo updatedInfo = KaiSyncHelper.parseVcaInfoReceivedOnCloud(nodeObject, jsonInfo);

        //search instance
        IVcaInstance dbInstance = VcaManager.getInstance().getVcaInstance(updatedInfo.getInstanceId());
        if (dbInstance == null)
        {
            Logger.error(Util.whichFn() + "vca not found (%s : %s)", getNodeName(), updatedInfo.getInstanceId());
            return false;
        }

        //update
        NodeVcaInstance nodeInstance = (NodeVcaInstance) dbInstance;
        nodeInstance.getVcaInfo().setSettings(updatedInfo.getSettings());
        nodeInstance.getVcaInfo().setRecurrenceRule(updatedInfo.getRecurrenceRule());
        nodeInstance.setPendingRequest(false);

        //check migration
        nodeInstance = VcaManager.getInstance().checkAndMigrate(nodeObject.getReleaseNumber(), nodeInstance);
        nodeInstance.save();

        return true;
    }
}
