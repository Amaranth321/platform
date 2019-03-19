package platform.kaisyncwrapper.node.tasks;

import com.google.gson.Gson;
import lib.util.Util;
import models.NodeCommand;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.devices.DeviceChannelPair;
import play.Logger;

import java.util.List;

/**
 * Sent from nodes when a device is updated
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */

public class NodeDeviceUpdated extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonCamera = parameters.get(0);
        NodeCamera updatedCamera = new Gson().fromJson(jsonCamera, NodeCamera.class);
        NodeObject nodeObject = getNodeObject();

        //find
        int index = nodeObject.getCameras().indexOf(updatedCamera);
        if (index < 0)
        {
            Logger.error(Util.whichFn() + "node camera not found (%s)", updatedCamera);
            return false;
        }

        //replace
        nodeObject.getCameras().set(index, updatedCamera);
        nodeObject.save();

        //remove cache from all node cameras
        CacheClient cacheClient = CacheClient.getInstance();
        for (NodeCamera nodeCamera : nodeObject.getCameras())
        {
            DeviceChannelPair idPair = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(),
                                                             nodeCamera.nodeCoreDeviceId);
            CachedNodeCamera cachedObject = cacheClient.getNodeCamera(idPair);
            cacheClient.remove(cachedObject);
        }

        return true;
    }
}
