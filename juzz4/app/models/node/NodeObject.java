package models.node;

import com.google.code.morphia.annotations.Entity;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.backwardcompatibility.VcaInstance;
import models.licensing.NodeLicenseInfo;
import platform.CloudLicenseManager;
import platform.analytics.VcaAppInfo;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceLog;
import platform.devices.DeviceModelInfo;
import platform.nodesoftware.NodeSoftwareStatus;
import platform.nodesoftware.SoftwareManager;
import play.Logger;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Entity
public class NodeObject extends Model
{
    private final String cloudPlatformDeviceId;
    private final String cloudCoreDeviceId;
    private String name;
    private List<NodeCamera> cameras;
    private NodeSettings settings;
    private String version;
    private NodeSoftwareStatus softwareStatus;
    private String downloadedVersion; //from OTA
    private List<VcaAppInfo> supportedAppList;

    @Deprecated
    private String vcaVersion;

    @Deprecated
    public List<VcaInstance> analytics;

    /**
     * Creates NodeObject and saves it in db
     *
     * @param name
     * @param cloudPlatformDeviceId
     * @param cloudCoreDeviceId
     */
    public static NodeObject create(String name,
                                    String cloudPlatformDeviceId,
                                    String cloudCoreDeviceId)
    {
        NodeObject nodeObject = new NodeObject(name, cloudPlatformDeviceId, cloudCoreDeviceId);
        return nodeObject.save();
    }

    public static NodeObject findByPlatformId(String platformDeviceId)
    {
        NodeObject nodeObject = NodeObject.q().filter("cloudPlatformDeviceId", platformDeviceId).first();
        if (nodeObject == null)
        {
            Logger.warn(Util.getCallerFn() + "Node object not found (platformDeviceId=%s). Already-deleted node?", platformDeviceId);
            return null;
        }

        if (nodeObject.cameras == null)
        {
            nodeObject.cameras = new ArrayList<>();
        }

        if (nodeObject.analytics == null)
        {
            nodeObject.analytics = new ArrayList<>();
        }

        return nodeObject;
    }

    public static NodeObject findByCoreId(String coreDeviceId)
    {
        NodeObject nodeObject = NodeObject.q().filter("cloudCoreDeviceId", coreDeviceId).first();
        if (nodeObject == null)
        {
            Logger.warn(Util.getCallerFn() + "Node object not found (coreDeviceId=%s). Already-deleted node?",
                        coreDeviceId);
            return null;
        }

        if (nodeObject.cameras == null)
        {
            nodeObject.cameras = new ArrayList<>();
        }

        if (nodeObject.analytics == null)
        {
            nodeObject.analytics = new ArrayList<>();
        }

        return nodeObject;
    }

    private NodeObject(String name, String cloudPlatformDeviceId, String cloudCoreDeviceId)
    {
        this.name = name;
        this.cloudPlatformDeviceId = cloudPlatformDeviceId;
        this.cloudCoreDeviceId = cloudCoreDeviceId;

        cameras = new ArrayList<>();
        version = SoftwareManager.UNSET_NODE_VERSION;
        vcaVersion = "";
    }

    public String getNodeId()
    {
        return cloudPlatformDeviceId;
    }

    public String getNodeCoreDeviceId()
    {
        return cloudCoreDeviceId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public MongoDevice getDbDevice()
    {
        MongoDevice dbDevice = MongoDevice.getByPlatformId(cloudPlatformDeviceId);
        if (dbDevice == null)
        {
            throw new IllegalStateException(Util.whichFn() + "Missing db device for node : " + name);
        }
        return dbDevice;
    }

    public List<NodeCamera> getCameras()
    {
        if (cameras == null)
        {
            cameras = new ArrayList<>();
        }
        return cameras;
    }

    public NodeSettings getSettings()
    {
        //timezone offset zero probably means info is not sent by the node
        if (settings != null && settings.getTzOffsetMins() == 0)
        {
            TimeZone timeZone = TimeZone.getTimeZone(settings.getTimezone());
            settings.setTimezone(timeZone);
            save();
        }

        return settings;
    }

    public TimeZone getTimeZone()
    {
        if (settings == null || Util.isNullOrEmpty(settings.getTimezone()))
        {
            return null;
        }

        try
        {
            return TimeZone.getTimeZone(settings.getTimezone());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    public void setSettings(NodeSettings settings)
    {
        this.settings = settings;
    }

    public String getNodeVersion()
    {
        //this happens when node is just registered and version is not updated to Cloud yet
        if (Util.isNullOrEmpty(version))
        {
            return SoftwareManager.UNSET_NODE_VERSION;
        }

        //previously, version's format is not validated, so it might have incorrect formats
        if (!SoftwareManager.getInstance().isValidNodeVersion(version))
        {
            return SoftwareManager.UNSET_NODE_VERSION;
        }

        return version;
    }

    public void setNodeVersion(String nodeVersion)
    {
        if (!getNodeVersion().equals(nodeVersion))
        {
            //update software status
            MongoDevice nodeDevice = getDbDevice();
            boolean isLatest = SoftwareManager.getInstance().isLatest(Long.parseLong(nodeDevice.getModelId()), nodeVersion);
            NodeSoftwareStatus newStatus = isLatest ? NodeSoftwareStatus.LATEST : NodeSoftwareStatus.NOT_DOWNLOADED;
            setSoftwareStatus(newStatus, null);

            //log
            String log = String.format("version set to %s", nodeVersion);
            DeviceLog.createLog(Long.parseLong(cloudPlatformDeviceId), log);
        }

        version = nodeVersion;
    }

    public double getReleaseNumber()
    {
        return SoftwareManager.getInstance().getReleaseNumber(getNodeVersion());
    }

    public NodeLicenseInfo getLicense()
    {
        MongoDevice nodeDevice = getDbDevice();
        CloudLicenseManager licenseMgr = CloudLicenseManager.getInstance();
        try
        {
            return licenseMgr.convertToNodeLicenseInfo(licenseMgr.getLicenseByNode(nodeDevice.getDeviceId()));
        }
        catch (ApiException e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    public NodeSoftwareStatus getSoftwareStatus()
    {
        if (softwareStatus == NodeSoftwareStatus.UPDATING ||
            softwareStatus == NodeSoftwareStatus.UPDATE_AVAILABLE)
        {
            return softwareStatus;
        }

        MongoDevice nodeDevice = getDbDevice();
        boolean isLatest = SoftwareManager.getInstance().isLatest(Long.parseLong(nodeDevice.getModelId()), getNodeVersion());
        softwareStatus = isLatest ? NodeSoftwareStatus.LATEST : NodeSoftwareStatus.NOT_DOWNLOADED;
        save();
        return softwareStatus;
    }

    public void setSoftwareStatus(NodeSoftwareStatus newStatus, String downloadedVersion)
    {
        if (newStatus == NodeSoftwareStatus.UPDATE_AVAILABLE)
        {
            Logger.info("[%s] Update available. Downloaded version: %s", name, downloadedVersion);
            this.downloadedVersion = downloadedVersion;
        }
        else if (newStatus == NodeSoftwareStatus.LATEST)
        {
            this.downloadedVersion = "";
        }

        softwareStatus = newStatus;
    }

    public String getDownloadedVersion()
    {
        return downloadedVersion;
    }

    /**
     * @return storage limit in MB
     */
    public int getCameraStorageLimit(String cameraCoreDeviceId)
    {
        List<NodeCamera> camerasWithRecording = new ArrayList<>();
        NodeCamera targetCam = null;
        for (NodeCamera nodeCamera : cameras)
        {
            if (nodeCamera.cloudRecordingEnabled)
            {
                camerasWithRecording.add(nodeCamera);
            }

            if (nodeCamera.nodeCoreDeviceId.equals(cameraCoreDeviceId))
            {
                targetCam = nodeCamera;
            }
        }

        if (targetCam == null)
        {
            Logger.error(Util.whichFn() + "node camera does not exist (coreDeviceId = %s)", cameraCoreDeviceId);
            return 0;
        }

        if (!targetCam.cloudRecordingEnabled)
        {
            return 0;
        }

        NodeLicenseInfo licenseInfo = getLicense();
        return (licenseInfo.cloudStorageGb * 1024) / camerasWithRecording.size();
    }

    public void removeAllRelatedCache()
    {
        CacheClient cacheClient = CacheClient.getInstance();

        //node itself
        CachedDevice cachedDevice = cacheClient.getDeviceByCoreId(cloudCoreDeviceId);
        cacheClient.remove(cacheClient.getNodeObject(cachedDevice));

        //node cameras
        for (NodeCamera camera : cameras)
        {
            DeviceChannelPair idPair = new DeviceChannelPair(cloudCoreDeviceId, camera.nodeCoreDeviceId);
            cacheClient.remove(cacheClient.getNodeCamera(idPair));
        }
    }

    public List<VcaAppInfo> getSupportedAppList()
    {
        if (supportedAppList == null)
        {
            //null means version is < 4.5, hence just assign old apps
            supportedAppList = VcaAppInfo.kaiX1Apps(vcaVersion);
            save();
        }
        return supportedAppList;
    }

    public void setSupportedAppList(List<VcaAppInfo> supportedAppList)
    {
        this.supportedAppList = supportedAppList;
    }

    public DeviceModelInfo getModelInfo()
    {
        return new DeviceModelInfo(getDbDevice().getModelId());
    }
}
