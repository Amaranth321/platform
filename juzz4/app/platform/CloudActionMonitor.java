package platform;

import com.google.gson.Gson;
import com.kaisquare.sync.CommandType;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.command.cloud.CloudNodeCommand;
import models.licensing.NodeLicense;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.analytics.VcaInfo;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.kaisyncwrapper.KaiSyncHelper;
import play.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 */
public class CloudActionMonitor
{
    private static final CloudActionMonitor instance = new CloudActionMonitor();

    private CloudActionMonitor()
    {
    }

    private String getMacAddress(String nodeId) throws ApiException
    {
        //on cloud, use deviceKey to get mac address
        CachedDevice cachedDevice = CacheClient.getInstance().getDeviceByPlatformId(nodeId);
        if (cachedDevice == null)
        {
            throw new ApiException(String.format("Device (%s) not found", nodeId));
        }

        String macAddress = cachedDevice.getDeviceKey();
        if (Util.isNullOrEmpty(macAddress))
        {
            throw new ApiException(String.format("Device (%s) does not have deviceKey", nodeId));
        }

        return macAddress;
    }

    public static CloudActionMonitor getInstance()
    {
        return instance;
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     * @param vcaInfo
     */
    public void cloudAddedNodeVca(String cloudPlatformDeviceId, VcaInfo vcaInfo) throws ApiException
    {
        String infoToSend = KaiSyncHelper.stringifyVcaInfoForSendingToNode(vcaInfo);
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_ADD_NODE_VCA,
                                                    Arrays.asList(infoToSend));
        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     * @param vcaInfo
     */
    public void cloudUpdatedNodeVca(String cloudPlatformDeviceId, VcaInfo vcaInfo) throws ApiException
    {
        String infoToSend = KaiSyncHelper.stringifyVcaInfoForSendingToNode(vcaInfo);
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_UPDATE_NODE_VCA,
                                                    Arrays.asList(infoToSend));
        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     * @param vcaInstanceId
     */
    public void cloudRemovedNodeVca(String cloudPlatformDeviceId, String vcaInstanceId) throws ApiException
    {
        String nodeId = String.valueOf(cloudPlatformDeviceId);
        CloudNodeCommand cmd = new CloudNodeCommand(nodeId,
                                                    getMacAddress(nodeId),
                                                    CommandType.CLOUD_REMOVE_NODE_VCA,
                                                    Arrays.asList(vcaInstanceId));

        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     * @param vcaInstanceId
     */
    public void cloudActivatedNodeVca(String cloudPlatformDeviceId, String vcaInstanceId) throws ApiException
    {
        String nodeId = String.valueOf(cloudPlatformDeviceId);
        CloudNodeCommand cmd = new CloudNodeCommand(nodeId,
                                                    getMacAddress(nodeId),
                                                    CommandType.CLOUD_ACTIVATE_NODE_VCA,
                                                    Arrays.asList(vcaInstanceId));

        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     * @param vcaInstanceId
     */
    public void cloudDeactivatedNodeVca(String cloudPlatformDeviceId, String vcaInstanceId) throws ApiException
    {
        String nodeId = String.valueOf(cloudPlatformDeviceId);
        CloudNodeCommand cmd = new CloudNodeCommand(nodeId,
                                                    getMacAddress(nodeId),
                                                    CommandType.CLOUD_DEACTIVATE_NODE_VCA,
                                                    Arrays.asList(vcaInstanceId));

        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     * @param nodeCam               Node Camera
     */
    public void cloudEditedNodeDevice(String cloudPlatformDeviceId, NodeCamera nodeCam) throws ApiException
    {
        String jsonNodeCam = new Gson().toJson(nodeCam);
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_EDIT_NODE_DEVICE,
                                                    Arrays.asList(jsonNodeCam));

        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     */
    public void cloudSuspendedNodeLicense(String cloudPlatformDeviceId) throws ApiException
    {
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_SUSPEND_LICENSE);

        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     */
    public void cloudUnsuspendedNodeLicense(String cloudPlatformDeviceId) throws ApiException
    {
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_UNSUSPEND_LICENSE);

        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     * @param updatedLicense        updated license
     */
    public void cloudUpdatedNodeLicense(String cloudPlatformDeviceId, NodeLicense updatedLicense) throws ApiException
    {
        //adjust license based on node's release number
        NodeObject node = NodeObject.findByPlatformId(cloudPlatformDeviceId);
        NodeLicense adjustedLicense = CloudLicenseManager.getInstance().getCompatibleLicense(updatedLicense,
                                                                                             node.getReleaseNumber());

        String jsonLicense = new Gson().toJson(adjustedLicense);
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_UPDATE_LICENSE,
                                                    Arrays.asList(jsonLicense));

        cmd.save();
    }

    /**
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     */
    public void cloudDeletedNodeLicense(String cloudPlatformDeviceId) throws ApiException
    {
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_DELETE_LICENSE);

        cmd.save();
    }


    /**
     * Sends the start remote shell command to the specified node.
     *
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     * @param host                  Hostname of the server with which the node should open SSH session
     * @param port                  Port of the server with which the node should open SSH session
     * @param username              Username to use for the ssh session with the server
     */
    public void cloudStartedRemoteShell(final String cloudPlatformDeviceId,
                                        String host,
                                        Integer port,
                                        String username) throws ApiException
    {

        Map<String, String> params = new HashMap<>();
        params.put("host", host);
        params.put("port", String.valueOf(port));
        params.put("user", username);
        String jsonParams = new Gson().toJson(params);

        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_START_REMOTE_SHELL,
                                                    Arrays.asList(jsonParams));

        cmd.save();
    }

    /**
     * Sends the start remote shell command to the specified node.
     *
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     */
    public void cloudStoppedRemoteShell(final String cloudPlatformDeviceId) throws ApiException
    {
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_STOP_REMOTE_SHELL);

        cmd.save();
    }

    /**
     * @param bucketId Bucket that was modified
     * @param infoToUpdate Map of updated fields
     */
    public void cloudUpdateBucketSettings(String bucketId, Map infoToUpdate)
    {
        try
        {
            //update all nodes registerd under this bucket
            List<MongoDevice> bucketDevices = DeviceManager.getInstance().getDevicesOfBucket(bucketId);
            for (MongoDevice bucketDevice : bucketDevices)
            {
                if (!bucketDevice.isKaiNode())
                {
                    continue;
                }

                String cloudPlatformDeviceId = bucketDevice.getDeviceId();
                String jsonInfo = new Gson().toJson(infoToUpdate);

                CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId, getMacAddress(cloudPlatformDeviceId), CommandType.CLOUD_UPDATE_BUCKET_SETTINGS, Arrays.asList(jsonInfo));
                cmd.save();
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    /**
     * Send command to start the node software update
     *
     * @param cloudPlatformDeviceId Node's platform ID on cloud
     */
    public void cloudUpdateNodeSoftware(final String cloudPlatformDeviceId) throws ApiException
    {
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_UPDATE_NODE);
        cmd.save();
    }

    /**
     * Broadcast command to nodeIdList
     */
    public void broadcastCommand(List<Long> nodeIdList, CommandType commandType, String jsonObject) throws ApiException
    {
        for (Long nodeId : nodeIdList)
        {
            CloudNodeCommand cmd = new CloudNodeCommand(nodeId.toString(),
                                                        getMacAddress(nodeId.toString()),
                                                        commandType, Arrays.asList(jsonObject));
            cmd.save();
        }
    }

    public void pullLogFromNode(String cloudPlatformDeviceId) throws Exception
    {
        CloudNodeCommand cmd = new CloudNodeCommand(cloudPlatformDeviceId,
                                                    getMacAddress(cloudPlatformDeviceId),
                                                    CommandType.CLOUD_PULL_LOG);
        cmd.save();
    }
}
