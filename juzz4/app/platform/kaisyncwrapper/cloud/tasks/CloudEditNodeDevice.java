package platform.kaisyncwrapper.cloud.tasks;

import com.google.gson.Gson;
import models.MongoDevice;
import models.node.NodeCamera;
import platform.DeviceManager;

import java.util.List;

/**
 * Sent from Cloud when node device is edited by cloud
 * processCommand will be executed on node
 *
 * @author Aye Maung
 */

public class CloudEditNodeDevice extends CloudToNodeCommandTask
{
    @Override
    protected boolean processCommand() throws Exception
    {
        String jsonNodeCamera = getParameter(0);
        NodeCamera editedCamera = new Gson().fromJson(jsonNodeCamera, NodeCamera.class);

        //search camera in local device list
        MongoDevice nodeCamera = MongoDevice.getByPlatformId(editedCamera.nodePlatformDeviceId);
        if (nodeCamera == null)
        {
            throw new Exception("Node camera not found");
        }

        //copy new details
        nodeCamera = editedCamera.updateAndConvert(nodeCamera);
        DeviceManager.getInstance().updateDevice(nodeCamera);

        return true;
    }
}
