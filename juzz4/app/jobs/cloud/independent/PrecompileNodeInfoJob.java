package jobs.cloud.independent;

import jobs.cloud.CloudCronJob;
import models.MongoDevice;
import models.MongoDeviceModel;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.node.NodeSettings;
import models.node.PrecompiledNodeInfo;
import models.stats.VcaHourlyStats;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.db.cache.CacheClient;
import platform.devices.DeviceStatus;
import play.Logger;
import play.jobs.Every;

import java.util.ArrayList;
import java.util.List;

/**
 * This job is to periodically compile node information for Node browser page
 * in order to speed up the page loading and filtering.
 * <p/>
 * The info will also be used to detect malfunctioning nodes
 *
 * @author Aye Maung
 * @since v4.5
 */
@Every("20mn")
public class PrecompileNodeInfoJob extends CloudCronJob
{
    private static final int VCA_STATUS_AVG_HOURS = 3;
    private static final int VCA_STATUS_COUNT_THRESHOLD = 5;

    private static final CacheClient cacheClient = CacheClient.getInstance();

    @Override
    public void doJob()
    {
        //clear existing
        PrecompiledNodeInfo.q().delete();

        //re-compile data from all nodes
        Iterable<NodeObject> allNodeObjects = NodeObject.q().fetch();
        for (NodeObject nodeObject : allNodeObjects)
        {
            MongoDevice dbDevice = MongoDevice.getByCoreId(nodeObject.getNodeCoreDeviceId());
            if (dbDevice == null)
            {
                Logger.error("Device doesn't exist for NodeObject (%s)", nodeObject.getName());
                continue;
            }

            compileAndSave(nodeObject, dbDevice);
        }
    }

    private void compileAndSave(NodeObject nodeObject, MongoDevice dbDevice)
    {
        String bucketName = cacheClient.getBucket(dbDevice.getBucketId()).getName();

        //network settings
        String ipAddress = "";
        String timezone = "";
        NodeSettings nodeSettings = nodeObject.getSettings();
        if (nodeSettings != null)
        {
            String ip = "";
            try
            {
                ip = nodeObject.getSettings().getNetworkSettings().getAddress();
            }
            catch (Exception e)
            {
            }
            ipAddress = ip;

            String tz = "";
            try
            {
                tz = nodeObject.getSettings().getTimezone();
            }
            catch (Exception e)
            {
            }
            timezone = tz;
        }

        //camera list
        List<PrecompiledNodeInfo.CameraSummary> cameraSummaryList = new ArrayList<>();
        int offlineCameraCount = 0;
        for (NodeCamera nodeCamera : nodeObject.getCameras())
        {
            cameraSummaryList.add(new PrecompiledNodeInfo.CameraSummary(nodeCamera));
            if (nodeCamera.getStatus() == DeviceStatus.DISCONNECTED)
            {
                offlineCameraCount++;
            }
        }

        //vca list
        List<PrecompiledNodeInfo.VcaSummary> vcaSummaryList = new ArrayList<>();
        int unstableVcaCount = 0;
        int noRecentDataCount = 0;
        for (IVcaInstance vcaInstance : VcaManager.getInstance().listVcaInstancesOfDevice(dbDevice))
        {
            //check status threshold
            int avgCount = VcaHourlyStats.getAvgStatusChangeCount(vcaInstance.getVcaInfo().getInstanceId(), VCA_STATUS_AVG_HOURS);
            if (avgCount > VCA_STATUS_COUNT_THRESHOLD)
            {
                unstableVcaCount++;
            }

            //todo: check no recent data
            boolean noRecentData = false;

            vcaSummaryList.add(new PrecompiledNodeInfo.VcaSummary(vcaInstance, avgCount, noRecentData));
        }

        MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(dbDevice.getModelId());
        PrecompiledNodeInfo compiledInfo = new PrecompiledNodeInfo(
                Long.parseLong(dbDevice.getDeviceId()),
                dbDevice.getCoreDeviceId(),
                bucketName,
                dbDevice.getName(),
                nodeObject.getNodeVersion(),
                timezone,
                deviceModel.getName(),
                ipAddress,
                dbDevice.getDeviceKey(),
                nodeObject.getDownloadedVersion(),
                cameraSummaryList,
                vcaSummaryList,
                dbDevice.getStatus(),
                nodeObject.getSoftwareStatus(),
                unstableVcaCount,
                noRecentDataCount,
                offlineCameraCount
        );

        compiledInfo.save();
    }
}
