package platform;

import com.google.code.morphia.query.Query;
import com.kaisquare.core.thrift.CoreException;
import com.kaisquare.core.thrift.DeviceDetails;
import core.CoreClient;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.Analytics.NodeVcaInstance;
import models.MongoDevice;
import models.MongoDeviceModel;
import models.MongoUser;
import models.licensing.LicenseStatus;
import models.licensing.NodeLicense;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.notification.SentNotification;
import platform.access.DefaultUser;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceLog;
import platform.devices.DeviceStatus;
import platform.events.EventType;
import platform.label.LabelManager;
import platform.node.CloudConnector;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventType;
import platform.reports.DecorativeQuery;
import platform.reports.EventReport;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;

public class DeviceManager
{
    private static final DeviceManager instance = new DeviceManager();

    private DeviceManager()
    {
    }

    public static DeviceManager getInstance()
    {
        return instance;
    }

    public List<MongoDeviceModel> getDeviceModels() throws ApiException
    {
        try
        {
            return MongoDeviceModel.q().fetchAll();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    public MongoDevice addDeviceToBucket(String bucketId, DeviceDetails deviceDetails)
            throws ApiException, CoreException
    {
        // verify device model
        MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(deviceDetails.getModelId());
        if (deviceModel == null)
        {
            throw new IllegalArgumentException("Invalid device model ID");
        }

        // add device to core
        String coreDeviceId = CoreClient.getInstance().deviceManagementClient.addDevice(deviceDetails);
        if (Util.isNullOrEmpty(coreDeviceId))
        {
            throw new ApiException("Failed to add device to core");
        }

        // add device to platform db
        MongoDevice platformDevice;
        try
        {
            platformDevice = new MongoDevice();

            platformDevice.setDeviceId(MongoDevice.generateNewId());
            platformDevice.setCoreDeviceId(coreDeviceId);
            platformDevice.setName(deviceDetails.getName());
            platformDevice.setDeviceKey(deviceDetails.getKey());
            platformDevice.setModelId(deviceDetails.getModelId());
            platformDevice.setHost(deviceDetails.getHost());
            platformDevice.setPort(deviceDetails.getPort());
            platformDevice.setLogin(deviceDetails.getLogin());
            platformDevice.setPassword(CryptoManager.aesEncrypt(deviceDetails.getPassword()));
            platformDevice.setLatitude(Double.parseDouble(deviceDetails.getLat()));
            platformDevice.setLongitude(Double.parseDouble(deviceDetails.getLng()));
            platformDevice.setAddress(deviceDetails.getAddress());
            platformDevice.setCloudRecordingEnabled(deviceDetails.getCloudRecordingEnabled().equalsIgnoreCase("true"));
            platformDevice.setBucketId(bucketId);

            platformDevice.save();
        }
        catch (Exception e)
        {
            Logger.error(e, "");

            Logger.info("Removing already-added core device");
            DeviceDetails dd = new DeviceDetails();
            dd.setId(coreDeviceId);
            CoreClient.getInstance().deviceManagementClient.deleteDevice(dd);

            throw new ApiException("Failed to add device to platform");
        }

        // node device: create a NodeObject object in DB
        if (platformDevice.isKaiNode())
        {
            NodeObject node = NodeObject.create(platformDevice.getName(),
                                                platformDevice.getDeviceId(),
                                                platformDevice.getCoreDeviceId());
            node.save();
        }
        else // camera device
        {
            StorageManager.getInstance().setRecordingRetention(platformDevice);
        }

        try
        {
            // auto-assign devices to default accounts
            for (DefaultUser defaultUser : DefaultUser.values())
            {
                MongoUser user = MongoUser.q()
                        .filter("bucketId", bucketId)
                        .filter("login", defaultUser.getUsername())
                        .get();

                if (user != null && defaultUser.autoAssignDevices())
                {
                    addUserToDevice(user.getUserId(), platformDevice.getDeviceId());
                }
            }

            // if application type is "node", push the local device information to cloud server
            if (Environment.getInstance().onKaiNode())
            {
                CloudConnector.getInstance().getKaiSyncCommandClient().nodeDeviceAdded(platformDevice);
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        return platformDevice;
    }

    public void updateDevice(MongoDevice platformDevice) throws ApiException, CoreException
    {
        platformDevice.setPassword(CryptoManager.aesDecrypt(platformDevice.getPassword()));

        boolean result = CoreClient.getInstance().deviceManagementClient.updateDevice(platformDevice.toDeviceDetails());
        if (result == false)
        {
            throw new ApiException("Core failed to update device");
        }
        platformDevice.setPassword(CryptoManager.aesEncrypt(platformDevice.getPassword()));
        platformDevice.save();

        //If device is a Node, update it's NodeObject object in DB. If device
        //is no longer a Node, delete it's NodeObject object from DB.
        if (platformDevice.isKaiNode())
        {
            NodeObject node = NodeObject.findByPlatformId(platformDevice.getDeviceId());
            if (node != null)
            {
                node.setName(platformDevice.getName());
                node.save();
            }
            else
            {
                NodeObject.create(platformDevice.getName(),
                                  platformDevice.getDeviceId(),
                                  platformDevice.getCoreDeviceId());
            }
        }
        else
        {
            StorageManager.getInstance().setRecordingRetention(platformDevice);
        }

        //if application type is "node", push the local device information to cloud server
        if (Environment.getInstance().onKaiNode())
        {
            CloudConnector.getInstance().getKaiSyncCommandClient().nodeDeviceUpdated(platformDevice);
        }

        //broadcast change
        PlatformEventMonitor.getInstance().broadcast(PlatformEventType.DEVICE_UPDATED, platformDevice);

        //remove cache
        removeDeviceCache(platformDevice);
    }

    public boolean removeDeviceFromBucket(String bucketId, String platformDeviceId) throws ApiException
    {
        /*
         * Assumptions:
		 * 1) bucket object and deviceId are valid
		 */
        try
        {
            //if the device doesn't belong to this bucket, throw exception
            MongoDevice device = MongoDevice.getByPlatformId(platformDeviceId);
            if (device == null)
            {
                throw new ApiException("No such device");
            }

            if (!device.getBucketId().equals(bucketId))
            {
                throw new ApiException("Device is not in this bucket.");
            }

            //remove device from CoreClient
            DeviceDetails deviceDetails = new DeviceDetails();
            deviceDetails.setId(device.getCoreDeviceId());
            boolean coreResult = CoreClient.getInstance().deviceManagementClient.deleteDevice(deviceDetails);
            if (!coreResult)
            {
                throw new ApiException("Core failed to delete device");
            }

            if (device.isKaiNode())
            {
                // inform license
                CloudLicenseManager.getInstance().nodeDeleted(device.getDeviceId());

                // remove node vca in db
                NodeVcaInstance.find("vcaInfo.camera.coreDeviceId", device.getCoreDeviceId()).delete();

                NodeObject nodeObject = NodeObject.findByPlatformId(device.getDeviceId());
                if (nodeObject != null)
                {
                    // inform labels
                    for (NodeCamera nodeCamera : nodeObject.getCameras())
                    {
                        DeviceChannelPair cameraIdPair = new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(),
                                                                               nodeCamera.nodeCoreDeviceId);
                        LabelManager.getInstance().cameraDeleted(cameraIdPair);
                    }

                    // delete its NodeObject object from DB.
                    nodeObject.delete();
                }
            }

            device.delete();

            //Notify Others
            PlatformEventMonitor.getInstance().broadcast(PlatformEventType.DEVICE_DELETED, device);

            if (Environment.getInstance().onKaiNode())
            {
                //if application type is "node", push the local device information to cloud server
                CloudConnector.getInstance().getKaiSyncCommandClient().nodeDeviceDeleted(device);
            }

            //remove reportQueryHistory for the deleted device
            ReportManager.getInstance().removeReportQueryHistory(platformDeviceId, null, "");

            //remove connection logs
            DeviceLog.q().filter("platformDeviceId", Long.parseLong(platformDeviceId)).delete();

            //remove sentnotifications
            Logger.info("Deleting sent notifications");
            SentNotification.q().filter("notificationInfo.camera.coreDeviceId", device.getCoreDeviceId()).delete();

            //remove reports
            //note: deviceChannelPair same for both camera and node
            Logger.info("Deleting event reports");
            DeviceChannelPair deviceChannelPair = new DeviceChannelPair(device.getCoreDeviceId(), "");
            for (EventType eventType : EventReport.getSupportedEventTypes())
            {
                try
                {
                    Logger.info("Deleting report: %s, device: %s, channel: %s",
                                eventType,
                                deviceChannelPair.getCoreDeviceId(),
                                deviceChannelPair.getChannelId());

                    Query query = EventReport.getReport(eventType)
                            .query(null, null)
                            .addDevice(deviceChannelPair)
                            .getQuery();

                    // for crowddensity
                    if (query instanceof DecorativeQuery)
                    {
                        query = ((DecorativeQuery) query).getRawQuery();
                    }

                    Model.ds().delete(query);
                }
                catch (Exception e)
                {
                    Logger.error(e.getMessage());
                }
            }

            return true;
        }
        catch (ApiException apie)
        {
            throw apie;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public boolean addUserToDevice(String userId, String deviceId)
    {
        try
        {
            // verify common bucket
            MongoDevice device = MongoDevice.getByPlatformId(deviceId);
            if (device == null)
            {
                throw new ApiException("No such device");
            }
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("No such user");
            }
            if (!device.getBucketId().equals(user.getBucketId()))
            {
                throw new ApiException("User does not belong to device's bucket");
            }

            device.getUserIds().add(userId);
            device.save();

            //remove cache
            removeDeviceCache(device);

            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public boolean removeUserFromDevice(String userId, String deviceId) throws ApiException
    {
        try
        {
            // If the target user has access to this device, revoke the access
            MongoDevice device = MongoDevice.getByPlatformId(deviceId);
            if (device == null)
            {
                throw new ApiException("No such device");
            }
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("No such user");
            }

            if (!device.getBucketId().equals(user.getBucketId()))
            {
                throw new ApiException("Device is not in User's bucket");
            }

            device.getUserIds().remove(userId);
            device.save();

            //remove cache
            removeDeviceCache(device);

            return true;
        }
        catch (ApiException apie)
        {
            throw apie;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public List<MongoDevice> getDevicesOfBucket(String bucketId)
    {
        try
        {
            // get list of all devices in the bucket
            List<MongoDevice> devices = MongoDevice.q().filter("bucketId", bucketId)
                    .order("name")
                    .fetchAll();

            if (devices == null)
            {
                return new ArrayList<>();
            }

            // check license if cloud
            List<MongoDevice> retList = new ArrayList<>();
            if (Environment.getInstance().onCloud())
            {
                for (MongoDevice dvc : devices)
                {
                    if (dvc.isKaiNode())
                    {
                        NodeLicense license = CloudLicenseManager.getInstance().getLicenseByNode(dvc.getDeviceId());
                        if (license != null && license.status.equals(LicenseStatus.ACTIVE))
                        {
                            retList.add(dvc);
                            continue;
                        }
                    }

                    retList.add(dvc);
                }
            }
            else
            {
                retList = devices;
            }

            return retList;

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return new ArrayList<>();
        }
    }

    public List<MongoDevice> getDevicesOfUser(String userId)
    {
        List<MongoDevice> results = new ArrayList<>();

        MongoUser user = MongoUser.getById(userId);

        // get list of all devices in bucket
        List<MongoDevice> devices = MongoDevice.q()
                .filter("bucketId", user.getBucketId())
                .order("name")
                .fetchAll();

        for (MongoDevice device : devices)
        {
            if (device.getUserIds().contains(userId))
            {
                results.add(device);
            }
        }

        return results;
    }

    public boolean checkUserDeviceAccess(String deviceId, String userId) throws ApiException
    {
        try
        {
            MongoUser user = MongoUser.getById(userId);
            if (user == null)
            {
                throw new ApiException("invalid-user-id");
            }

            MongoDevice platformDevice = MongoDevice.getByPlatformId(deviceId);
            if (platformDevice == null)
            {
                throw new ApiException("invalid-device-id");
            }

            return platformDevice.getUserIds().contains(userId);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    /**
     * @param coreDeviceId
     * @param channelId
     *
     * @return channelId for local devices, camera name for nodes
     */
    public String getDeviceChannelName(String coreDeviceId, String channelId) throws ApiException
    {
        if (Util.isNullOrEmpty(channelId))
        {
            return "";
        }

        try
        {
            MongoDevice cloudDevice = MongoDevice.getByCoreId(coreDeviceId);
            if (cloudDevice == null)
            {
                throw new ApiException("event-device-not-found");
            }

            if (cloudDevice.isKaiNode())
            {
                List<NodeObject> allNodes = DeviceManager.getInstance().getAllNodeObjects();
                for (NodeObject node : allNodes)
                {
                    if (node.getNodeCoreDeviceId().equals(coreDeviceId))
                    {
                        for (NodeCamera nCam : node.getCameras())
                        {
                            if (nCam.nodeCoreDeviceId.equals(channelId))
                            {
                                return nCam.name;
                            }
                        }
                    }
                }

                throw new ApiException(String.format("event-node-camera-not-found : CoreDeviceId:%s ChannelId:%s",
                                                     coreDeviceId,
                                                     channelId));
            }

            return Integer.parseInt(channelId) + 1 + "";
        }
        catch (Exception e)
        {
            Logger.error(e.getMessage());
            return "";
        }
    }

    public void editNodeCamera(String nodePlatformId, NodeCamera updatedCamera) throws ApiException
    {
        //don't save the changes on cloud. Node will notify cloud when the changes are completed
        CloudActionMonitor.getInstance().cloudEditedNodeDevice(nodePlatformId, updatedCamera);
    }

    public List<NodeObject> getAllNodeObjects()
    {
        List<NodeObject> retList = new ArrayList<>();
        List<MongoDevice> devices = MongoDevice.q().fetchAll();
        for (MongoDevice device : devices)
        {
            if (!device.isKaiNode())
            {
                continue;
            }

            try
            {
                NodeObject nodeObject = NodeObject.findByCoreId(device.getCoreDeviceId());
                if (nodeObject != null)
                {
                    retList.add(nodeObject);
                }
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
        return retList;
    }

    public boolean nodeObjectExists(String platformDeviceId)
    {
        NodeObject nodeObject = NodeObject.findByPlatformId(platformDeviceId);
        return nodeObject != null;
    }

    public List<NodeObject> getNodeObjects(String bucketId)
    {
        List<NodeObject> nodeObjects = new ArrayList<>();

        try
        {
            List<MongoDevice> bucketDevices = getDevicesOfBucket(bucketId);
            for (MongoDevice bucketDevice : bucketDevices)
            {
                if (bucketDevice.isKaiNode())
                {
                    nodeObjects.add(NodeObject.findByPlatformId(bucketDevice.getDeviceId()));
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
        }
        return nodeObjects;
    }

    /**
     * @param deviceId  core device Id
     * @param channelId channel Id
     */
    public String getEventCameraIdentifier(String deviceId, String channelId)
    {
        return String.format("%s_%s", deviceId, channelId);
    }

    /**
     * Core device id is used because connection status comes from events
     *
     * @param deviceChannelPair
     * @param deviceStatus
     */
    public void updateDeviceStatus(DeviceChannelPair deviceChannelPair, DeviceStatus deviceStatus)
    {
        MongoDevice device = MongoDevice.getByCoreId(deviceChannelPair.getCoreDeviceId());
        if (device == null)
        {
            return;
        }

        device.setStatus(deviceStatus);
        device.save();

        // log
        DeviceLog.createLog(Long.parseLong(device.getDeviceId()), deviceStatus.toString());
    }

    public void updateNodeCameraStatus(DeviceChannelPair camera, DeviceStatus status)
    {
        CachedDevice nodeDevice = CacheClient.getInstance().getDeviceByCoreId(camera.getCoreDeviceId());
        NodeObject nodeObject = NodeObject.findByPlatformId(String.valueOf(nodeDevice.getPlatformDeviceId()));

        for (NodeCamera cam : nodeObject.getCameras())
        {
            if (cam.nodeCoreDeviceId.equals(camera.getChannelId()))
            {
                cam.setStatus(status);
                nodeObject.save();

                //log
                DeviceLog.createLog(Long.parseLong(nodeDevice.getPlatformDeviceId()), cam, status.toString());
                return;
            }
        }

        Logger.error("Node camera not found %s", camera);
    }

    public void removeDeviceCache(MongoDevice device)
    {
        CacheClient cacheClient = CacheClient.getInstance();
        CachedDevice platformDeviceCache = cacheClient.getDeviceByPlatformId(device.getDeviceId());
        CachedDevice coreDeviceCache = cacheClient.getDeviceByCoreId(device.getCoreDeviceId());

        // remove both
        cacheClient.remove(platformDeviceCache);
        cacheClient.remove(coreDeviceCache);

        if (device.isKaiNode())
        {
            cacheClient.remove(cacheClient.getNodeObject(platformDeviceCache));
        }
    }
}
