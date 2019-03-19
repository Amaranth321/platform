package platform;

import lib.util.ListUtil;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.MongoBucket;
import models.MongoDevice;
import models.MongoFeature;
import models.licensing.*;
import models.node.NodeObject;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import platform.content.export.manual.NodeLicensesPdf;
import platform.content.export.manual.NodeLicensesXls;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventSubscriber;
import platform.pubsub.PlatformEventTask;
import platform.pubsub.PlatformEventType;
import play.Logger;
import play.i18n.Lang;

import java.util.ArrayList;
import java.util.List;

public class CloudLicenseManager implements PlatformEventSubscriber
{
    private static CloudLicenseManager instance = new CloudLicenseManager();
    private static CloudActionMonitor cloudActionMonitor = CloudActionMonitor.getInstance();

    private CloudLicenseManager()
    {
    }

    public static CloudLicenseManager getInstance()
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        return instance;
    }

    public void addNodeLicense(String bucketId, int durationMonths, int cloudStorageGb, int maxCameraLimit, int maxVcaCount, List<String> featureNames) throws ApiException
    {
        // check feature list
        for (String featureName : featureNames)
        {
            MongoFeature feature = MongoFeature.getByName(featureName);
            if (feature == null)
            {
                throw new ApiException("unknown feature name: %s", featureName);
            }
            if (!feature.isAssignableToNodes())
            {
                throw new ApiException("feature not assignable to nodes: %s", featureName);
            }
        }

        NodeLicense nodeLicense = new NodeLicense(Long.parseLong(bucketId), durationMonths, cloudStorageGb, maxCameraLimit, maxVcaCount, featureNames);
        nodeLicense.save();
    }

    public void updateNodeLicense(String licenseNumber, int durationMonths, int cloudStorageGb, int maxCameraLimit, int maxVcaCount, List<String> featureNames) throws ApiException
    {
        // check feature list
        for (String featureName : featureNames)
        {
            MongoFeature feature = MongoFeature.getByName(featureName);
            if (feature == null)
            {
                throw new ApiException("unknown feature name: " + featureName);
            }
        }

        // find license and update
        NodeLicense nodeLicense = NodeLicense.q().filter("licenseNumber", licenseNumber).get();
        if (nodeLicense == null)
        {
            throw new ApiException("invalid-license-number");
        }

        // check if license has expired and is not extended
        if (nodeLicense.hasExpired() && nodeLicense.durationMonths >= durationMonths)
        {
            throw new ApiException("license-has-expired");
        }

        nodeLicense.durationMonths = durationMonths;
        nodeLicense.cloudStorageGb = cloudStorageGb;
        nodeLicense.maxCameraLimit = maxCameraLimit;
        nodeLicense.maxVcaCount = maxVcaCount;
        nodeLicense.featureNameList = featureNames;

        // if license is activated, inform the node
        if (!nodeLicense.status.equals(LicenseStatus.UNUSED))
        {
            cloudActionMonitor.cloudUpdatedNodeLicense(nodeLicense.nodeCloudPlatormId.toString(), nodeLicense);
            nodeLicense.status = LicenseStatus.ACTIVE;
        }

        nodeLicense.save();

        // remove cache
        if (nodeLicense.nodeCloudPlatormId != null)
        {
            NodeObject nodeObject = NodeObject.findByPlatformId(nodeLicense.nodeCloudPlatormId.toString());
            nodeObject.removeAllRelatedCache();
        }
    }

    public void deleteNodeLicense(String licenseNumber) throws ApiException
    {
        NodeLicense nodeLicense = NodeLicense.find("licenseNumber", licenseNumber).first();

        //Remove licensed node if not unused
        if (!nodeLicense.status.equals(LicenseStatus.UNUSED))
        {
            MongoBucket nodeBucket = MongoBucket.getById(nodeLicense.cloudBucketId.toString());
            String nodeId = nodeLicense.nodeCloudPlatormId.toString();
            DeviceManager.getInstance().removeDeviceFromBucket(nodeBucket.getBucketId(), nodeId); // will call nodeDeleted()
        }

        LicenseLog.find("licenseNumber", licenseNumber).delete();
        nodeLicense.delete();
    }

    public void suspendNodeLicense(String licenseNumber) throws ApiException
    {
        NodeLicense nodeLicense = NodeLicense.find("licenseNumber", licenseNumber).first();

        if (nodeLicense.hasExpired())
        {
            throw new ApiException("license-has-expired");
        }

        //inform node if active
        if (nodeLicense.status.equals(LicenseStatus.ACTIVE))
        {
            Logger.info("Notifying node (%s) about license suspension: %s", nodeLicense.nodeCloudPlatormId, licenseNumber);
            cloudActionMonitor.cloudSuspendedNodeLicense(nodeLicense.nodeCloudPlatormId.toString());
        }

        nodeLicense.status = LicenseStatus.SUSPENDED;
        nodeLicense.save();
    }

    public void unsuspendNodeLicense(String licenseNumber) throws ApiException
    {
        NodeLicense nodeLicense = NodeLicense.find("licenseNumber", licenseNumber).first();

        if (nodeLicense.hasExpired())
        {
            throw new ApiException("license-has-expired");
        }

        //inform node if any
        if (nodeLicense.nodeCloudPlatormId != null)
        {
            Logger.info("Notifying node (%s) about license un-suspension: %s", nodeLicense.nodeCloudPlatormId, licenseNumber);
            cloudActionMonitor.cloudUnsuspendedNodeLicense(nodeLicense.nodeCloudPlatormId.toString());
            nodeLicense.status = LicenseStatus.ACTIVE;
        }
        else
        {
            nodeLicense.status = LicenseStatus.UNUSED;
        }

        nodeLicense.save();
    }

    public NodeLicenseInfo getNodeLicenseInfo(String licenseNumber) throws ApiException
    {
        //Retrieve Data
        NodeLicense nodeLicense = NodeLicense.find("licenseNumber", licenseNumber).first();
        if (nodeLicense == null)
        {
            throw new ApiException("license-not-found");
        }

        return convertToNodeLicenseInfo(nodeLicense);
    }

    public NodeLicenseInfo convertToNodeLicenseInfo(NodeLicense nodeLicense) throws ApiException
    {
        MongoBucket bucket = MongoBucket.getById(nodeLicense.cloudBucketId.toString());
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        NodeLicenseInfo licenseInfo = new NodeLicenseInfo();
        licenseInfo.cloudBucketId = nodeLicense.cloudBucketId;
        licenseInfo.bucketName = bucket.getName();
        licenseInfo.licenseNumber = nodeLicense.licenseNumber;
        licenseInfo.cloudStorageGb = nodeLicense.cloudStorageGb;
        licenseInfo.durationMonths = nodeLicense.durationMonths;
        licenseInfo.maxCameraLimit = nodeLicense.maxCameraLimit;
        licenseInfo.maxVcaCount = nodeLicense.maxVcaCount;
        licenseInfo.featureNameList = nodeLicense.featureNameList;
        licenseInfo.created = nodeLicense.created;

        //Only for activated licenses
        if (nodeLicense.isAssignedToNode())
        {
            MongoDevice nodeDevice = MongoDevice.getByPlatformId(nodeLicense.nodeCloudPlatormId.toString());
            if (nodeDevice == null)
            {
                throw new ApiException("node-device-not-found");
            }

            licenseInfo.registrationNumber = nodeLicense.registrationNumber;
            licenseInfo.deviceName = nodeDevice.getName();
            licenseInfo.nodeCloudPlatormId = Long.parseLong(nodeDevice.getDeviceId());
            licenseInfo.activated = nodeLicense.activated;
            licenseInfo.expiryDate = nodeLicense.getExpiryDate();
        }
        else
        {
            licenseInfo.registrationNumber = "N/A";
            licenseInfo.deviceName = "N/A";
        }

        //for UI display
        licenseInfo.status = nodeLicense.hasExpired() ? LicenseStatus.EXPIRED : nodeLicense.status;
        return licenseInfo;
    }

    public NodeLicense getDbNodeLicense(String licenseNumber) throws ApiException
    {
        return NodeLicense.find("licenseNumber", licenseNumber).first();
    }

    //activated nodes are exempted
    public void bucketFeaturesRemoved(String bucketId, List<String> removeFeatureNames) throws ApiException
    {
        // populate removed feature names
        List<NodeLicense> nodeLicenses = NodeLicense.q().filter("cloudBucketId", Long.parseLong(bucketId)).fetchAll();
        for (NodeLicense nodeLicense : nodeLicenses)
        {
            List<String> removeListForLicense = ListUtil.hasInCommon(nodeLicense.featureNameList, removeFeatureNames);
            if (removeListForLicense.isEmpty())
            {
                continue;
            }

            Logger.info("(NodeLicense:%s) Removing features %s", nodeLicense.licenseNumber, removeListForLicense);
            List<String> newFeatureList = new ArrayList<>(nodeLicense.featureNameList);
            newFeatureList.removeAll(removeListForLicense);

            updateNodeLicense(nodeLicense.licenseNumber, nodeLicense.durationMonths, nodeLicense.cloudStorageGb, nodeLicense.maxCameraLimit, nodeLicense.maxVcaCount, newFeatureList);
        }
    }

    public NodeLicense getLicenseByNode(String deviceId)
    {
        MongoDevice device = MongoDevice.getByPlatformId(deviceId);
        if (!device.isKaiNode())
        {
            return null;
        }

        return NodeLicense.q().filter("nodeCloudPlatormId", Long.parseLong(deviceId)).get();
    }

    public List<NodeLicenseInfo> getAccessibleLicenses(String bucketId) throws ApiException
    {
        List<NodeLicenseInfo> nodeLicenseInfos = new ArrayList<>();

        // query all accessible buckets
        List<MongoBucket> accessibleBuckets = BucketManager.getInstance().getThisAndDescendants(bucketId);
        for (MongoBucket accessibleBucket : accessibleBuckets)
        {
            if (accessibleBucket.isDeleted())
            {
                continue;
            }

            List<NodeLicense> nodeLicenses = NodeLicense.q().filter("cloudBucketId", Long.parseLong(accessibleBucket.getBucketId())).fetchAll();
            for (NodeLicense nodeLicense : nodeLicenses)
            {
                nodeLicenseInfos.add(getNodeLicenseInfo(nodeLicense.licenseNumber));
            }
        }

        return nodeLicenseInfos;
    }

    public void markLicenseAsUsed(String licenseNumber, String deviceId, String registrationNumber) throws ApiException
    {
        NodeLicense nodeLicense = NodeLicense.q().filter("licenseNumber", licenseNumber).get();
        if (nodeLicense == null)
        {
            return;
        }

        nodeLicense.registrationNumber = registrationNumber;
		nodeLicense.nodeCloudPlatormId = Long.parseLong(deviceId);
		/*
		 * nodeLicense.activated = Environment.getInstance().getCurrentUTCTimeMillis();
		 * nodeLicense.status = LicenseStatus.ACTIVE;
		 */

		if (nodeLicense.status.equals(LicenseStatus.UNUSED)) {// first time register
			nodeLicense.activated = Environment.getInstance().getCurrentUTCTimeMillis();
			nodeLicense.status = LicenseStatus.ACTIVE;
		} else {// when replacement
			nodeLicense.status = LicenseStatus.ACTIVE;
		}
		nodeLicense.save();
    }

    public void nodeDeleted(String deviceId) throws ApiException
    {
        MongoDevice device = MongoDevice.getByPlatformId(deviceId);
        if (device == null || !device.isKaiNode())
        {
            return;
        }

        // send command to reset node
        cloudActionMonitor.cloudDeletedNodeLicense(deviceId);

        // unassigned license if license itself is not deleted
        NodeLicense nodeLicense = NodeLicense.q().filter("nodeCloudPlatormId", Long.parseLong(deviceId)).get();
        if (nodeLicense != null)
        {
            nodeLicense.status = LicenseStatus.UNUSED;
            nodeLicense.nodeCloudPlatormId = null;
            nodeLicense.registrationNumber = null;
            nodeLicense.save();
        }
    }

    public void subscribePlatformEvents()
    {

        PlatformEventMonitor evtMon = PlatformEventMonitor.getInstance();

        /**
         *
         *  BUCKET_SUSPENDED
         *
         */
        evtMon.subscribe(PlatformEventType.BUCKET_SUSPENDED, new PlatformEventTask()
        {
            @Override
            public void run(Object... params)
            {

                String bucketId = params[0].toString();

                List<NodeLicense> licenses = NodeLicense.q().filter("cloudBucketId", Long.parseLong(bucketId)).fetchAll();
                for (NodeLicense nl : licenses)
                {
                    try
                    {
                        //save current information
                        SuspendedLicenseRecord slr = new SuspendedLicenseRecord(convertToNodeLicenseInfo(nl));
                        slr.save();

                        suspendNodeLicense(nl.licenseNumber);
                    }
                    catch (Exception e)
                    {
                        Logger.error(e, "LicenseNumber : %s", nl.licenseNumber);
                    }
                }
            }
        });


        /**
         *
         *  BUCKET_SUSPENDED
         *
         */
        evtMon.subscribe(PlatformEventType.BUCKET_ACTIVATED, new PlatformEventTask()
        {
            @Override
            public void run(Object... params)
            {

                String bucketId = params[0].toString();

                List<NodeLicense> licenses = NodeLicense.q().filter("cloudBucketId", Long.parseLong(bucketId)).fetchAll();
                for (NodeLicense nl : licenses)
                {
                    try
                    {
                        //if previous license status was suspended, no need to activate it.
                        SuspendedLicenseRecord prevRecord = SuspendedLicenseRecord.q()
                                .filter("licenseInfo.licenseNumber", nl.licenseNumber)
                                .first();
                        if (prevRecord != null)
                        {
                            LicenseStatus prevStatus = prevRecord.getLicenseInfo().status;
                            prevRecord.delete();

                            if (prevStatus.equals(LicenseStatus.SUSPENDED))
                            {
                                Logger.info("License (%s) was in suspended status. Skipped.", nl.licenseNumber);
                                continue;
                            }
                        }

                        unsuspendNodeLicense(nl.licenseNumber);
                    }
                    catch (Exception e)
                    {
                        Logger.error(e, "LicenseNumber : %s", nl.licenseNumber);
                    }
                }
            }
        });

    }

    public ReportBuilder exportNodeLicenses(List<NodeLicenseInfo> nodeLicenseInfoList, FileFormat fileFormat, String timeZoneOffset) throws ApiException
    {
    	
    	 int offsetMinutes = 0;
         if (!timeZoneOffset.isEmpty())
         {
             try
             {
                 offsetMinutes = Integer.parseInt(timeZoneOffset);
             }
             catch (NumberFormatException e)
             {
                 Logger.error(lib.util.Util.getStackTraceString(e));
                 throw new ApiException("invalid-time-zone-offset");
             }
         }
         
        switch (fileFormat)
        {
            case PDF:
                return new NodeLicensesPdf(nodeLicenseInfoList, Lang.get(),offsetMinutes);

            case XLS:
                return new NodeLicensesXls(nodeLicenseInfoList, Lang.get());

            default:
                throw new ApiException("file-format-not-supported");
        }
    }

    /**
     * Adjust the license info to make it compatible with the node with targetRelease
     *
     * @param dbLicense
     * @param targetRelease
     */
    public NodeLicense getCompatibleLicense(NodeLicense dbLicense, Double targetRelease)
    {
        if (targetRelease < 4.4)
        {
            int index = dbLicense.featureNameList.indexOf("node-playback");
            if (index >= 0)
            {
                dbLicense.featureNameList.set(index, "cloud-recording");
            }
        }

        return dbLicense;
    }

}

