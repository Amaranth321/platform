package platform.kaisyncwrapper.node.tasks;

import models.NodeCommand;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.coreengine.RecordingManager;
import platform.devices.DeviceChannelPair;
import play.Logger;

/**
 * Sent from nodes when hard drive expansion is initiated
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */

public class NodeStorageExpanded extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        NodeObject nodeObject = getNodeObject();
        Logger.info("%s received from %s", getClass().getSimpleName(), nodeObject.getName());

        //reset all cloud recordings of this node
        for (NodeCamera camera : nodeObject.getCameras())
        {
            DeviceChannelPair idPair = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), camera.nodeCoreDeviceId);
            RecordingManager.getInstance().resetAllRecordings(idPair);
        }

        return true;
    }
}
