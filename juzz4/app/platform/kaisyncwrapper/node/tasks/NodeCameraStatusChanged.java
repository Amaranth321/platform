package platform.kaisyncwrapper.node.tasks;

import models.NodeCommand;
import models.node.NodeObject;
import platform.DeviceManager;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceStatus;

/**
 * Sent from nodes to update camera status on cloud
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeCameraStatusChanged extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        String cameraCoreDeviceId = command.getParameters().get(0);
        String status = command.getParameters().get(1);

        //cloud objects
        NodeObject nodeObject = getNodeObject();
        DeviceChannelPair camera = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), cameraCoreDeviceId);
        DeviceStatus deviceStatus = DeviceStatus.valueOf(status);

        DeviceManager.getInstance().updateNodeCameraStatus(camera, deviceStatus);
        return true;
    }
}
