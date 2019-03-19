package platform.kaisyncwrapper.node.tasks;

import models.backwardcompatibility.Device;
import models.NodeCommand;
import platform.DeviceManager;

/**
 * Sent when node has been factory-reset
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeFactoryReset extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        // TODO: use MongoDevice
        Device nodeDevice = getNodeDevice();
        DeviceManager.getInstance().removeDeviceFromBucket(nodeDevice.bucket.getId().toString(), nodeDevice.getId().toString());
        return true;
    }
}
