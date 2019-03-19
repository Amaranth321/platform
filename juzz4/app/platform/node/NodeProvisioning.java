package platform.node;

import com.kaisquare.core.thrift.DeviceDetails;
import lib.util.ListUtil;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.MongoBucket;
import models.licensing.NodeLicense;
import models.node.NodeInfo;
import models.node.NodeSettings;
import models.transportobjects.NodeCameraTransport;
import models.transportobjects.VcaTransport;
import platform.BucketManager;
import platform.CryptoManager;
import platform.DeviceManager;
import platform.Environment;
import platform.analytics.IVcaInstance;
import platform.analytics.LocalVcaInstance;
import platform.analytics.VcaInfo;
import platform.analytics.VcaManager;
import platform.devices.DeviceChannelPair;
import play.Logger;

import java.util.List;

/**
 * Only for nodes
 */
public class NodeProvisioning
{
    private static NodeProvisioning instance = null;
    private static BucketManager bucketManager = null;

    private NodeProvisioning()
    {
        bucketManager = BucketManager.getInstance();
    }

    public static NodeProvisioning getInstance()
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        if (instance == null)
        {
            instance = new NodeProvisioning();
        }
        return instance;
    }

    public void activateNode() throws ApiException
    {
        //activate running vca
        try
        {
            List<IVcaInstance> allVcaList = VcaManager.getInstance().listVcaInstances(null);
            for (IVcaInstance vcaInstance : allVcaList)
            {
                //TODO: should not activate the instances that were deactivated on purpose by user
                vcaInstance.activate();
            }
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
        }

        NodeManager nodeManager = NodeManager.getInstance();
        MongoBucket nodeBucket = nodeManager.getBucket();
        NodeLicense targetLicense = nodeManager.getLicense();

        //update features
        updateNodeFeatures(targetLicense.featureNameList);

        //activate bucket
        bucketManager.activateBucket(nodeBucket.getBucketId());

        //update node info
        if (nodeManager.isRegisteredOnCloud())
        {
            NodeInfo nodeInfo = nodeManager.getNodeInfo();
            nodeInfo.setSuspended(false);
            nodeInfo.save();
        }

        Logger.info(Util.getCallerFn() + "node activated.");
    }

    public void suspendNode() throws ApiException
    {
        //deactivate all vca
        try
        {
            List<IVcaInstance> allVcaList = VcaManager.getInstance().listVcaInstances(null);
            for (IVcaInstance vcaInstance : allVcaList)
            {
                vcaInstance.deactivate();
            }
        }
        catch (Exception e)
        {
            Logger.error(Util.whichFn() + e.getMessage());
        }

        NodeManager nodeManager = NodeManager.getInstance();

        //suspend bucket
        MongoBucket nodeBucket = nodeManager.getBucket();
        bucketManager.deactivateBucket(nodeBucket.getBucketId());

        //update node info
        if (nodeManager.isRegisteredOnCloud())
        {
            NodeInfo nodeInfo = nodeManager.getNodeInfo();
            nodeInfo.setSuspended(true);
            nodeInfo.save();
        }

        Logger.info(Util.getCallerFn() + "node deactivated.");
    }

    public void factoryResetNode()
    {
        try
        {
            KaiNodeAdminService.getInstance().factoryReset();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    public void updateNodeFeatures(List<String> featureNames) throws ApiException
    {
        //Find bucket and update features
        NodeManager nodeManager = NodeManager.getInstance();
        MongoBucket bucket = nodeManager.getBucket();

        //Check feature diff. Note: must remove first
        List<String> removedFeatureIds = ListUtil.getExtraItems(featureNames, bucket.getFeatureNames());
        List<String> addedFeatureIds = ListUtil.getExtraItems(bucket.getFeatureNames(), featureNames);

        bucketManager.removeFeatures(bucket.getBucketId(), removedFeatureIds);
        bucketManager.addFeatures(bucket.getBucketId(), addedFeatureIds);

        Logger.info("NodeProvisioning: features updated (%s)", featureNames);
    }

    public void restoreData(RestorableNodeData restorableData) throws ApiException
    {
        if (restorableData == null)
        {
            Logger.error("restorableData is null. Skipped restoring data from the old node");
            return;
        }

        List<NodeCameraTransport> cameraList = restorableData.getCameraList();
        if (cameraList != null)
        {
            Logger.info("Restoring %s cameras", cameraList.size());
            MongoBucket bucket = NodeManager.getInstance().getBucket();
            for (NodeCameraTransport nodeCamera : cameraList)
            {
                Logger.info("Restoring camera (%s)", nodeCamera.name);
                try
                {
                    DeviceDetails deviceDetails = new DeviceDetails();
                    deviceDetails.setId(nodeCamera.nodeCoreDeviceId);
                    deviceDetails.setName(nodeCamera.name);
                    deviceDetails.setModelId(nodeCamera.model.getId().toString());
                    deviceDetails.setKey(nodeCamera.deviceKey);
                    deviceDetails.setHost(nodeCamera.host);
                    deviceDetails.setPort(nodeCamera.port);
                    deviceDetails.setAddress(nodeCamera.address);
                    deviceDetails.setLat(nodeCamera.latitude);
                    deviceDetails.setLng(nodeCamera.longitude);
                    deviceDetails.setLogin(nodeCamera.login);
                    deviceDetails.setPassword(CryptoManager.aesDecrypt(nodeCamera.password));
                    deviceDetails.setCloudRecordingEnabled(nodeCamera.cloudRecordingEnabled + "");

                    DeviceManager.getInstance().addDeviceToBucket(bucket.getBucketId(), deviceDetails);
                }
                catch (Exception e)
                {
                    Logger.error(e, "Failed to add node camera (%s).", nodeCamera.name);
                }
            }
        }

        List<VcaTransport> vcaList = restorableData.getVcaList();
        if (vcaList != null)
        {
            Logger.info("Restoring %s vca instances", vcaList.size());
            for (VcaTransport cloudInst : vcaList)
            {
                try
                {
                    DeviceChannelPair cloudCam = new DeviceChannelPair(cloudInst.coreDeviceId, cloudInst.channelId);
                    VcaInfo vcaInfo = VcaInfo.createNew(cloudInst.instanceId,
                                                        cloudInst.appId,
                                                        cloudCam.adjustIdPairForNode(),
                                                        cloudInst.thresholds,
                                                        cloudInst.recurrenceRule,
                                                        cloudInst.enabled);

                    Logger.info("Restoring node Instance: %s", vcaInfo);
                    LocalVcaInstance.addNew(vcaInfo);
                }
                catch (Exception e)
                {
                    Logger.error(e, "Failed to restore node vca (%s:%s)", cloudInst.type, cloudInst.instanceId);
                }
            }
        }

        NodeSettings nodeSettings = restorableData.getNodeSettings();
        if (nodeSettings != null)
        {
            boolean ipInUse = Util.isPingable(nodeSettings.getNetworkSettings().getAddress());
            if (ipInUse)
            {
                Util.printImptLog("Old IP Address is still in use. Update manually");
                return;
            }
            NodeManager.getInstance().setSettings(nodeSettings, true);
        }
    }
}
