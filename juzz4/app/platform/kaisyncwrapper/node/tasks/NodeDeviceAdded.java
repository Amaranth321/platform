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
 * Sent from nodes when a device is added
 * <p/>
 * processCommand will be executed on cloud
 *
 * @author Aye Maung
 */
public class NodeDeviceAdded extends NodeToCloudCommandTask
{
    @Override
    protected boolean processCommand(NodeCommand command) throws Exception
    {
        List<String> parameters = command.getParameters();
        String jsonCamera = parameters.get(0);
        NodeCamera newCamera = new Gson().fromJson(jsonCamera, NodeCamera.class);

        NodeObject nodeObject = getNodeObject();

        //add or replace
        int index = nodeObject.getCameras().indexOf(newCamera);
        if (index < 0)
        {
            nodeObject.getCameras().add(newCamera);
        }
        else
        {
            Logger.warn(Util.whichClass() + "same-ID camera exists. Replaced.");
            nodeObject.getCameras().set(index, newCamera);
        }

        nodeObject.save();

        //remove cache from all node cameras
        CacheClient cacheClient = CacheClient.getInstance();
        for (NodeCamera nodeCamera : nodeObject.getCameras())
        {
            DeviceChannelPair idPair = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);
            CachedNodeCamera cachedObject = cacheClient.getNodeCamera(idPair);
            cacheClient.remove(cachedObject);
        }

        return true;
    }
}
