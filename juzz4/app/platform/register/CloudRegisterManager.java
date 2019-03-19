package platform.register;

import com.kaisquare.core.thrift.CoreException;
import com.kaisquare.core.thrift.DeviceDetails;
import core.CoreClient;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InternalException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.*;
import models.backwardcompatibility.DeviceModel;
import models.command.cloud.CloudNodeCommand;
import models.licensing.NodeLicense;
import models.licensing.NodeLicenseInfo;
import models.node.NodeObject;
import platform.*;
import platform.nodesoftware.SoftwareManager;
import play.Logger;

public class CloudRegisterManager
{
    private static final CloudRegisterManager instance = new CloudRegisterManager();
    private static final CloudLicenseManager licenseMgr = CloudLicenseManager.getInstance();

    private CloudRegisterManager()
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }
    }

    private void verifyRequestInfo(NodeSetupRequest request) throws ApiException
    {
        Logger.info("Verifying Bucket");
        MongoBucket targetBucket = MongoBucket.getById(request.getBucketId().toString());
        if (targetBucket == null)
        {
            throw new ApiException("invalid-bucket-id");
        }

        Logger.info("Verifying License");
        NodeLicenseInfo nodeLicenseInfo = licenseMgr.getNodeLicenseInfo(request.getLicenseNumber());
        if (nodeLicenseInfo == null)
        {
            throw new ApiException("incorrect-license-number");
        }
        if (!nodeLicenseInfo.cloudBucketId.toString().equals(targetBucket.getBucketId()))
        {
            throw new ApiException("msg-accesskey-license-mismatch");
        }

        Logger.info("Verifying inventory");
        MongoInventoryItem invItem = MongoInventoryItem.getByRegistrationNumber(request.getRegistrationNumber());
        if (invItem.isActivated())
        {
            throw new ApiException("reg-number-in-use");
        }
        if (!invItem.isKaiNode())
        {
            throw new ApiException("error-reg-number-not-node");
        }

        Logger.info("Verifying MAC address");
        String mac = request.getMacAddress();
        if (!Util.isNullOrEmpty(mac) && !invItem.getMacAddress().equalsIgnoreCase(mac))
        {
            throw new ApiException("error-inventory-not-match-node-mac");
        }

        Logger.info("Verifying version");
        String version = request.getVersion();
        if (!Util.isNullOrEmpty(version))
        {
            if (!SoftwareManager.getInstance().isValidNodeVersion(version))
            {
                throw new ApiException("invalid-version");
            }

            double releaseNo = SoftwareManager.getInstance().getReleaseNumber(version);
            double cloudReleaseNo = VersionManager.getInstance().getPlatformVersion();
            if (releaseNo > cloudReleaseNo)
            {
                throw new ApiException("node-release-higher-than-cloud");
            }
        }
    }

    public static CloudRegisterManager getInstance()
    {
        return instance;
    }

    public NodeSetupInfo registerNode(NodeSetupRequest request) throws ApiException, InternalException, CoreException
    {
        verifyRequestInfo(request);

        //Verification passed
        MongoBucket targetBucket = MongoBucket.getById(request.getBucketId().toString());
        MongoInventoryItem invItem = MongoInventoryItem.getByRegistrationNumber(request.getRegistrationNumber());
        MongoDeviceModel mongoDeviceModel = invItem.getDeviceModel();

        Logger.info("Adding device to core engine");
        DeviceDetails deviceDetails = new DeviceDetails();
        deviceDetails.setName(request.getDeviceName());
        deviceDetails.setAddress(request.getAddress());
        deviceDetails.setLat(request.getLatitude());
        deviceDetails.setLng(request.getLongitude());
        deviceDetails.setModelId(invItem.getDeviceModelId());
        deviceDetails.setKey(invItem.getMacAddress());
        deviceDetails.setHost("");
        deviceDetails.setPort("");
        deviceDetails.setCloudRecordingEnabled("false");

        MongoDevice nodeDevice = DeviceManager.getInstance()
                .addDeviceToBucket(targetBucket.getBucketId(), deviceDetails);
        if (nodeDevice == null)
        {
            throw new ApiException("unknown");
        }

        Logger.info("Setting version");
        NodeObject nodeObject = NodeObject.findByPlatformId(nodeDevice.getDeviceId());
        nodeObject.setNodeVersion(request.getVersion());

        Logger.info("Activating inventory");
        InventoryManager.getInstance().activateInventory(request.getRegistrationNumber());

        Logger.info("Activating node license");
        licenseMgr.markLicenseAsUsed(request.getLicenseNumber(),
                                     nodeDevice.getDeviceId(),
                                     request.getRegistrationNumber());

        Logger.info("Assigning node to the caller");
        try
        {
            DeviceManager.getInstance().addUserToDevice(request.getUserId().toString(), nodeDevice.getDeviceId());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        NodeLicense dbLicense = licenseMgr.getDbNodeLicense(request.getLicenseNumber());

        Logger.info("Creating response");
        NodeSetupInfo setupInfo = new NodeSetupInfo();
        setupInfo.setBucketName(targetBucket.getName());
        setupInfo.setNodeName(nodeDevice.getName());
        setupInfo.setCloudPlatformDeviceId(Long.parseLong(nodeDevice.getDeviceId()));
        setupInfo.setCloudCoreDeviceId(nodeDevice.getCoreDeviceId());
        setupInfo.setNodeLicense(licenseMgr.getCompatibleLicense(dbLicense, request.getReleaseNumber()));
        setupInfo.setBrandingAssets(targetBucket.getBrandingAssets());
        setupInfo.setNotificationSettings(targetBucket.getNotificationSettings());
        setupInfo.setDeviceModel(new DeviceModel(mongoDeviceModel));
        Logger.info("Node registration completed (%s)", nodeDevice.getName());

        return setupInfo;
    }

    public NodeSetupInfo replaceNode(NodeSetupRequest request) throws ApiException, CoreException
    {
        verifyRequestInfo(request);

        //Verification passed
        MongoBucket targetBucket = MongoBucket.getById(request.getBucketId().toString());
        MongoInventoryItem invItem = MongoInventoryItem.getByRegistrationNumber(request.getRegistrationNumber());
        MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(invItem.getDeviceModelId());
        NodeLicenseInfo licenseInfo = licenseMgr.getNodeLicenseInfo(request.getLicenseNumber());

        Logger.info("Find the old node");
        MongoDevice oldNode = MongoDevice.getByPlatformId(licenseInfo.nodeCloudPlatormId.toString());
        if (oldNode == null)
        {
            throw new ApiException("Cannot find the node to be replaced");
        }

        Logger.info("Unactivated old inventory");
        InventoryManager.getInstance().deactivateInventory(oldNode.getDeviceKey());

        Logger.info("Remove unsent commands by old mac address");
        removeUnsentCommands(oldNode.getDeviceKey());

        Logger.info("Sending command to terminate the old node");
        CloudActionMonitor.getInstance().cloudDeletedNodeLicense(oldNode.getDeviceId());

        Logger.info("Updating device");
        oldNode.setDeviceKey(invItem.getMacAddress());
        oldNode.setModelId(invItem.getDeviceModelId());
        CoreClient.getInstance().deviceManagementClient.updateDevice(oldNode.toDeviceDetails());
        oldNode.save();

        Logger.info("Clearing node device cache");
        DeviceManager.getInstance().removeDeviceCache(oldNode);

        Logger.info("Activating inventory");
        InventoryManager.getInstance().activateInventory(request.getRegistrationNumber());

        Logger.info("Activating node license");
        licenseMgr.markLicenseAsUsed(request.getLicenseNumber(),
                                     oldNode.getDeviceId(),
                                     request.getRegistrationNumber());

        NodeLicense dbLicense = licenseMgr.getDbNodeLicense(request.getLicenseNumber());

        Logger.info("Cloud-side completed (%s:%s)", targetBucket, oldNode.getName());
        Logger.info("Creating response");
        NodeSetupInfo setupInfo = new NodeSetupInfo();
        setupInfo.setBucketName(targetBucket.getName());
        setupInfo.setNodeName(oldNode.getName());
        setupInfo.setCloudPlatformDeviceId(Long.parseLong(oldNode.getDeviceId()));
        setupInfo.setCloudCoreDeviceId(oldNode.getCoreDeviceId());
        setupInfo.setNodeLicense(licenseMgr.getCompatibleLicense(dbLicense, request.getReleaseNumber()));
        setupInfo.setBrandingAssets(targetBucket.getBrandingAssets());
        setupInfo.setNotificationSettings(targetBucket.getNotificationSettings());
        setupInfo.setDeviceModel(new DeviceModel(deviceModel));
        Logger.info("Node replacement completed (%s)", oldNode.getName());

        return setupInfo;
    }

    private void removeUnsentCommands(String oldMacAddress)
    {
        CloudNodeCommand.queryByMac(oldMacAddress).delete();
        NodeCommand.queryByMac(oldMacAddress).delete();
    }
}

