package platform.kaisyncwrapper.node.tasks;

import models.Analytics.NodeVcaInstance;
import models.NodeCommand;
import models.node.NodeObject;
import platform.analytics.VcaInfo;
import platform.analytics.VcaManager;
import platform.analytics.VcaStatus;
import platform.kaisyncwrapper.KaiSyncHelper;
import platform.rt.RTFeedManager;

import java.util.List;

/**
 * Sent from nodes when vca is added
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeVcaAdded extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonInfo = parameters.get(0);

        NodeObject nodeObject = getNodeObject();
        VcaInfo newVcaInfo = KaiSyncHelper.parseVcaInfoReceivedOnCloud(nodeObject, jsonInfo);

        //save
        NodeVcaInstance nodeInst = NodeVcaInstance.saveAsCloudCopy(newVcaInfo);
        nodeInst.setStatus(VcaStatus.WAITING);
        nodeInst.save();

        //check migration
        nodeInst = VcaManager.getInstance().checkAndMigrate(nodeObject.getReleaseNumber(), nodeInst);
        nodeInst.save();

        //notify UI
        RTFeedManager.getInstance().vcaInstanceChanged(nodeInst.getVcaInfo().getInstanceId(),
                                                       nodeInst.getVcaInfo().getCamera());

        return true;
    }
}
