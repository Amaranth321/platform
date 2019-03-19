package platform.content.export;

import models.MongoBucket;
import models.MongoUser;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.DeviceManager;
import platform.devices.DeviceChannelPair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * todo: This class is no longer needed due to the use of Memcached. Replace it with CacheClient
 *
 * This class contain bucket-related data for quick lookup to reduce db queries
 * Create an instance of this for that task at hand
 * Do not store it permanently as bucket info might be modified by users
 *
 * @author Aye Maung
 * @since v4.3
 */
public class BucketInfoProxy
{
    private final int userCount;
    private final Map<String, String> nodeNameMap;
    private final Map<String, String> cameraNameMap;
    private final List<DeviceChannelPair> cameraList;

    public BucketInfoProxy(MongoBucket bucket)
    {
        userCount = (int) MongoUser.q().filter("bucketId", bucket.getBucketId()).count();
        nodeNameMap = new LinkedHashMap<>();
        cameraNameMap = new LinkedHashMap<>();

        //compile node and camera info for lookup
        List<NodeObject> bucketNodes = DeviceManager.getInstance().getNodeObjects(bucket.getBucketId());
        List<DeviceChannelPair> cameraList = new ArrayList<>();
        for (NodeObject nodeObject : bucketNodes)
        {
            nodeNameMap.put(nodeObject.getNodeCoreDeviceId(), nodeObject.getName());
            for (NodeCamera nodeCamera : nodeObject.getCameras())
            {
                String key = DeviceManager.getInstance().getEventCameraIdentifier(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId);

                cameraNameMap.put(key, nodeCamera.name);
                cameraList.add(new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), nodeCamera.nodeCoreDeviceId));
            }
        }
        this.cameraList = cameraList;
    }

    /**
     * @return total number of bucket users
     */
    public int getUserCount()
    {
        return userCount;
    }

    /**
     * @return the total number of bucket nodes
     */
    public int getDeviceCount()
    {
        return nodeNameMap.keySet().size();
    }

    public List<DeviceChannelPair> getCameraList()
    {
        return cameraList;
    }

    /**
     * @param deviceId coreDeviceId
     */
    public String getNodeName(String deviceId)
    {
        if (!nodeNameMap.containsKey(deviceId))
        {
            return "Nil";
        }

        return nodeNameMap.get(deviceId);
    }

    /**
     * @param deviceId  coreDeviceId
     * @param channelId channel Id
     */
    public String getCameraName(String deviceId, String channelId)
    {
        String key = DeviceManager.getInstance().getEventCameraIdentifier(deviceId, channelId);
        if (!cameraNameMap.containsKey(key))
        {
            return "Nil";
        }

        return cameraNameMap.get(key);
    }
}
