package platform.kaisyncwrapper.node.tasks;

import models.NodeCommand;
import models.node.NodeObject;
import platform.nodesoftware.NodeSoftwareStatus;
import play.Logger;

import java.util.List;

/**
 * Sent from node to inform that node have new version update file
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeUpdateFileReady extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        NodeObject nodeObject = getNodeObject();

        //already the latest
        if (nodeObject.getSoftwareStatus() == NodeSoftwareStatus.LATEST)
        {
            Logger.info("[%s] node version is already the latest. skipped.", getNodeName());
            return true;
        }

        String downloadedVersion = "";
        List<String> parameters = command.getParameters();
        if (parameters == null || parameters.isEmpty())
        {
            Logger.debug("[%s:%s] no downloaded version sent.", getNodeName(), nodeObject.getNodeVersion());
        }
        else
        {
            downloadedVersion = parameters.get(0);
        }

        nodeObject.setSoftwareStatus(NodeSoftwareStatus.UPDATE_AVAILABLE, downloadedVersion);
        nodeObject.save();
        return true;
    }
}
