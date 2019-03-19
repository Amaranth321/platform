package controllers.api;

import com.google.gson.Gson;
import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.backwardcompatibility.Feature;
import models.MongoBucket;
import models.MongoFeature;
import models.licensing.LicenseLog;
import models.licensing.LicenseStatus;
import models.licensing.NodeLicense;
import models.licensing.NodeLicenseInfo;
import platform.CloudLicenseManager;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.With;

import java.util.*;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle License Management
 * @sectiondesc APIs for managing node licenses.
 */

@With(APIInterceptor.class)
public class LicenseProvisioning extends APIController
{
    /**
     * Checks if the action is allowed for the buckets involved
     */
    private static void checkAccess(MongoBucket... bucketsInvolved)
    {
        try
        {
            MongoBucket callerBucket = MongoBucket.getById(getCallerBucketId());

            for (MongoBucket bkt : bucketsInvolved)
            {
                if (bkt != null && !callerBucket.hasControlOver(bkt))
                {
                    throw new ApiException("msg-no-rights-to-buckets");
                }
            }
        }
        catch (ApiException apiE)
        {
            Logger.error(apiE.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apiE.getMessage());
            renderJSON(map);
        }
    }

    /**
     * @param license-number License number whose status is to be checked. Mandatory
     *
     * @servtitle Returns status of license
     * @httpmethod POST
     * @uri /api/{bucket}/checklicensestatus
     * @responsejson {
     * "result": "ok",
     * "status": UNUSED/ ACTIVE/ SUSPENDED / EXPIRED
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void checklicensestatus()
    {
        try
        {
            String licenseNumber = readApiParameter("license-number", true);
            licenseNumber = Util.removeNonAlphanumeric(licenseNumber);

            NodeLicenseInfo nodeLicenseInfo = CloudLicenseManager.getInstance().getNodeLicenseInfo(licenseNumber);
            if (nodeLicenseInfo == null)
            {
                throw new ApiException("license-not-found");
            }

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("status", nodeLicenseInfo.status.toString());
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Returns licenses of current bucket
     * @httpmethod POST
     * @uri /api/{bucket}/getnodelicenses
     * @responsejson {
     * "result": "ok",
     * "node-licenses": [
     * {
     * "cloudBucketId" : NumberLong(2),
     * "licenseNumber" : "ITCXAJ9EKX1CVZK",
     * "status" : "UNUSED",
     * "featureIdList" : [ "28",   "29",   "30",   "31",   "32", "33",   "34" ],
     * "maxVcaCount" : 5,
     * "cloudStorageGb" : 6,
     * "durationMonths" : 12,
     * "created" : NumberLong("1402624865292"),
     * "enabled" : true,
     * "_created" : NumberLong("1402645565292"),
     * "_modified" : NumberLong("1402645565292")
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getnodelicenses()
    {
        try
        {
            String targetBucketId = readApiParameter("bucket-id", false);
            List<NodeLicenseInfo> resultList = CloudLicenseManager.getInstance().getAccessibleLicenses(targetBucketId);
            sortByBucketIgnoreCase(resultList);

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("node-licenses", resultList);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id        Id of bucket to issue license e.g 2. Mandatory
     * @param duration-months  Validity peroid of license in month e.g 12/ 15. -1 refer as perpetual. Mandatory
     * @param cloud-storage-gb Total cloud storage in GB (gigabyte). e.g 5. Mandatory
     * @param max-vca-count    Maximum number of VCA that a node with license can register e.g 4. Mandatory
     * @param features         Feature Ids that the node with license have access to.
     *                         e.g ["100", "200", "250"]. Mandatory
     *
     * @servtitle Adds license for node
     * @httpmethod POST
     * @uri /api/{bucket}/addnodelicense
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void addnodelicense()
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            String durationMonths = readApiParameter("duration-months", true);
            String cloudStorageGb = readApiParameter("cloud-storage-gb", true);
            String maxCameraLimit = readApiParameter("max-camera-limit", true);
            String maxVcaCount = readApiParameter("max-vca-count", true);
            String jsonFeatureList = readApiParameter("features", true);

            //Validate params
            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }
            if (!Util.isInteger(durationMonths))
            {
                throw new ApiException("invalid-validity-period");
            }
            if (!Util.isInteger(cloudStorageGb))
            {
                throw new ApiException("invalid-cloud-storage");
            }
            if (!Util.isInteger(maxCameraLimit))
            {
                throw new ApiException("invalid-max-camera-limit");
            }
            if (!Util.isInteger(maxVcaCount))
            {
                throw new ApiException("invalid-max-vca-count");
            }

            //parse values
            int iDurationMonths = Integer.parseInt(durationMonths);
            int iCloudStorageGb = Integer.parseInt(cloudStorageGb);
            int iMaxCameraLimit = Integer.parseInt(maxCameraLimit);
            int iMaxVcaCount = Integer.parseInt(maxVcaCount);
            List<String> featureNameList = new ArrayList<>();
            featureNameList = new Gson().fromJson(jsonFeatureList, featureNameList.getClass());

            //create license
            CloudLicenseManager.getInstance().addNodeLicense(bucketId, iDurationMonths, iCloudStorageGb, iMaxCameraLimit, iMaxVcaCount, featureNameList);

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param license-number   License number to update e.g ITCXAJ9EKX1CVZK. Mandatory
     * @param duration-months  Validity peroid of license in month e.g 12/ 15. -1 refer as perpetual. Mandatory
     * @param cloud-storage-gb Total cloud storage in GB (gigabyte). e.g 5. Mandatory
     * @param max-vca-count    Maximum number of VCA that a node with license can register e.g 4. Mandatory
     * @param features         Feature Ids that the node with license have access to.
     *                         e.g ["100", "200", "250"]. Mandatory
     *
     * @servtitle Updates license for node
     * @httpmethod POST
     * @uri /api/{bucket}/updatenodelicense
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatenodelicense()
    {
        try
        {
            String licenseNumber = readApiParameter("license-number", true);
            String durationMonths = readApiParameter("duration-months", true);
            String cloudStorageGb = readApiParameter("cloud-storage-gb", true);
            String maxCameraLimit = readApiParameter("max-camera-limit", true);
            String maxVcaCount = readApiParameter("max-vca-count", true);
            String jsonFeatureList = readApiParameter("features", true);

            //Validate params
            licenseNumber = Util.removeNonAlphanumeric(licenseNumber);
            NodeLicense originalLicense = NodeLicense.find("licenseNumber", licenseNumber).first();
            if (originalLicense == null)
            {
                throw new ApiException("invalid-license-number");
            }
            if (!Util.isInteger(durationMonths))
            {
                throw new ApiException("invalid-validity-period");
            }
            if (!Util.isInteger(cloudStorageGb))
            {
                throw new ApiException("invalid-cloud-storage");
            }
            if (!Util.isInteger(maxCameraLimit))
            {
                throw new ApiException("invalid-max-camera-limit");
            }
            if (!Util.isInteger(maxVcaCount))
            {
                throw new ApiException("invalid-max-vca-count");
            }

            //check access
            MongoBucket targetBucket = MongoBucket.getById(originalLicense.cloudBucketId.toString());
            checkAccess(targetBucket);

            //parse values
            int iDurationMonths = Integer.parseInt(durationMonths);
            int iCloudStorageGb = Integer.parseInt(cloudStorageGb);
            int iMaxCameraLimit = Integer.parseInt(maxCameraLimit);
            int iMaxVcaCount = Integer.parseInt(maxVcaCount);
            List<String> featureNameList = new ArrayList<>();
            featureNameList = new Gson().fromJson(jsonFeatureList, featureNameList.getClass());

            //update license
            CloudLicenseManager.getInstance().updateNodeLicense(licenseNumber, iDurationMonths, iCloudStorageGb, iMaxCameraLimit, iMaxVcaCount, featureNameList);

            /**
             *
             *  Log changes to the license
             *
             */
            try
            {
                List<String> changelog = new ArrayList<>();
                if (originalLicense.durationMonths != iDurationMonths)
                {
                    changelog.add(String.format("Updated validity period to %s months", iDurationMonths));
                }
                if (originalLicense.cloudStorageGb != iCloudStorageGb)
                {
                    changelog.add(String.format("Updated cloud storage to %s GB", iCloudStorageGb));
                }
                if (originalLicense.maxVcaCount != iMaxVcaCount)
                {
                    changelog.add(String.format("Updated max vca count to %s", iMaxVcaCount));
                }
                for (String fName : originalLicense.featureNameList)
                {
                    if (!featureNameList.contains(fName))
                    {
                        MongoFeature fItem = MongoFeature.getByName(fName);
                        changelog.add(String.format("Removed feature: %s > %s", Messages.get(fItem.getType()), Messages.get(fItem.getName())));
                    }
                }
                for (String fName : featureNameList)
                {
                    if (!originalLicense.featureNameList.contains(fName))
                    {
                        MongoFeature fItem = MongoFeature.getByName(fName);
                        changelog.add(String.format("Added feature: %s > %s", Messages.get(fItem.getType()), Messages.get(fItem.getName())));
                    }
                }

                LicenseLog licenseLog = new LicenseLog();
                licenseLog.licenseNumber = originalLicense.licenseNumber;
                licenseLog.username = renderArgs.get("username").toString();
                licenseLog.remoteIp = request.remoteAddress;
                licenseLog.changes = changelog;
                licenseLog.save();
            }
            catch (Exception e)
            {
                Logger.error("Failed to log: " + e.getMessage());
            }

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param license-number License number to suspend e.g ITCXAJ9EKX1CVZK. Mandatory
     *
     * @servtitle Suspend license for node
     * @httpmethod POST
     * @uri /api/{bucket}/suspendnodelicense
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void suspendnodelicense()
    {
        try
        {
            String licenseNumber = readApiParameter("license-number", true);
            licenseNumber = Util.removeNonAlphanumeric(licenseNumber);

            CloudLicenseManager licenseManager = CloudLicenseManager.getInstance();

            //check access
            NodeLicenseInfo licenseInfo = licenseManager.getNodeLicenseInfo(licenseNumber);
            MongoBucket targetBucket = MongoBucket.getById(licenseInfo.cloudBucketId.toString());
            checkAccess(targetBucket);

            //suspend license
            Logger.info("Processing license suspension request (%s)", licenseNumber);
            licenseManager.suspendNodeLicense(licenseNumber);

            /**
             *
             *  Log changes to the license
             *
             */
            try
            {
                LicenseLog licenseLog = new LicenseLog();
                licenseLog.licenseNumber = licenseNumber;
                licenseLog.username = renderArgs.get("username").toString();
                licenseLog.remoteIp = request.remoteAddress;
                licenseLog.changes.add("Suspended license");
                licenseLog.save();
            }
            catch (Exception e)
            {
                Logger.error("Failed to log: " + e.getMessage());
            }

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param license-number License number to unsuspend e.g ITCXAJ9EKX1CVZK. Mandatory
     *
     * @servtitle Unsuspend the sespended license for node
     * @httpmethod POST
     * @uri /api/{bucket}/unsuspendnodelicense
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void unsuspendnodelicense()
    {
        try
        {
            String licenseNumber = readApiParameter("license-number", true);
            licenseNumber = Util.removeNonAlphanumeric(licenseNumber);

            CloudLicenseManager licenseManager = CloudLicenseManager.getInstance();

            //check access
            NodeLicenseInfo licenseInfo = licenseManager.getNodeLicenseInfo(licenseNumber);
            MongoBucket targetBucket = MongoBucket.getById(licenseInfo.cloudBucketId.toString());
            checkAccess(targetBucket);

            //unsuspend license
            Logger.info("Processing license un-suspension request (%s)", licenseNumber);
            licenseManager.unsuspendNodeLicense(licenseNumber);

            /**
             *
             *  Log changes to the license
             *
             */
            try
            {
                LicenseLog licenseLog = new LicenseLog();
                licenseLog.licenseNumber = licenseNumber;
                licenseLog.username = renderArgs.get("username").toString();
                licenseLog.remoteIp = request.remoteAddress;
                licenseLog.changes.add("Unsuspended license");
                licenseLog.save();
            }
            catch (Exception e)
            {
                Logger.error("Failed to log: " + e.getMessage());
            }

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param license-number License number to delete e.g ITCXAJ9EKX1CVZK. Mandatory
     *
     * @servtitle Delete license for node
     * @httpmethod POST
     * @uri /api/{bucket}/deletenodelicense
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void deletenodelicense()
    {
        try
        {
            String licenseNumber = readApiParameter("license-number", true);
            licenseNumber = Util.removeNonAlphanumeric(licenseNumber);

            CloudLicenseManager licenseManager = CloudLicenseManager.getInstance();

            //check access
            NodeLicenseInfo licenseInfo = licenseManager.getNodeLicenseInfo(licenseNumber);
            MongoBucket targetBucket = MongoBucket.findById(licenseInfo.cloudBucketId);
            checkAccess(targetBucket);

            //delete license
            licenseManager.deleteNodeLicense(licenseNumber);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param license-number License number to delete. Mandatory
     *
     * @servtitle Returns license module log
     * @httpmethod POST
     * @uri /api/{bucket}/getnodelicenselogs
     * @responsejson {
     * "result": "ok",
     * "logs": [
     * {
     * "time" : NumberLong("1402625904463"),
     * "licenseNumber" : "ITCXAJ9EKX1CVZK",
     * "username" : "root",
     * "remoteIp" : "127.0.0.1",
     * "changes" : [  "Updated max vca count to 3" ],
     * "_created" : NumberLong("1402646604464"),
     * "_modified" : NumberLong("1402646604464")
     * },
     * {
     * "time" : NumberLong("1402625904463"),
     * "licenseNumber" : "ITCXAJ9EKX1CVZK",
     * "username" : "root",
     * "remoteIp" : "127.0.0.1",
     * "changes" : [  "Updated validity period to 3 months" ],
     * "_created" : NumberLong("1402646604464"),
     * "_modified" : NumberLong("1402646604464")
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getnodelicenselogs()
    {
        try
        {
            String licenseNumber = readApiParameter("license-number", true);
            licenseNumber = Util.removeNonAlphanumeric(licenseNumber);

            //check access
            NodeLicenseInfo licenseInfo = CloudLicenseManager.getInstance().getNodeLicenseInfo(licenseNumber);
            MongoBucket targetBucket = MongoBucket.getById(licenseInfo.cloudBucketId.toString());
            checkAccess(targetBucket);

            List<LicenseLog> logs = LicenseLog.q().filter("licenseNumber", licenseNumber).order("-time").asList();

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("logs", logs);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param file-format         "xls" or "pdf".
     * @param bucket-id           Bucket ID
     * @param status              License Status
     * @param registration-number registration number
     * @param device-name         device name
     *
     * @servtitle Exports license list as a file in the specified format i.e PDF or XLS
     * @httpmethod GET
     * @uri /api/{bucket}/exportlicenselist
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportlicenselist() throws ApiException
    {
        try
        {
        	String timeZoneOffset = readApiParameter("time-zone-offset", false);
            FileFormat fileFormat = FileFormat.parse(readApiParameter("file-format", true));
            String bucketId = readApiParameter("bucket-id", false);
            String status = readApiParameter("status", false);
            String registrationNumber = readApiParameter("registration-number", false);
            String deviceName = readApiParameter("device-name", false);

            //get target bucket
            String callerBucketId = getCallerBucketId();
            String targetBucketId = Util.isNullOrEmpty(bucketId) ? callerBucketId : bucketId;

            //get node license info
            List<NodeLicenseInfo> accessibleList = CloudLicenseManager.getInstance().getAccessibleLicenses(targetBucketId);
            List<NodeLicenseInfo> filteredList = new ArrayList<>();

            //do filtering
            for (NodeLicenseInfo licenseInfo : accessibleList)
            {
                try
                {
                    if (!status.isEmpty() && !licenseInfo.status.equals(LicenseStatus.valueOf(status.toUpperCase())))
                    {
                        continue;
                    }
                    if (!registrationNumber.isEmpty() &&
                        !licenseInfo.registrationNumber.toLowerCase().contains(registrationNumber.toLowerCase()))
                    {
                        continue;
                    }
                    if (!deviceName.isEmpty() &&
                        licenseInfo.deviceName != null &&
                        (licenseInfo.deviceName.equalsIgnoreCase("N/A") ||
                         licenseInfo.deviceName.toLowerCase().indexOf(deviceName.toLowerCase()) < 0))
                    {
                        continue;
                    }

                    filteredList.add(licenseInfo);

                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                }
            }

            sortByBucketIgnoreCase(filteredList);

            ReportBuilder reportBuilder = CloudLicenseManager.getInstance().exportNodeLicenses(filteredList, fileFormat,timeZoneOffset);
            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    private static void sortByBucketIgnoreCase(List<NodeLicenseInfo> licenseList)
    {
        Collections.sort(licenseList, new Comparator<NodeLicenseInfo>()
        {
            @Override
            public int compare(NodeLicenseInfo o1, NodeLicenseInfo o2)
            {
                return !o1.bucketName.equals(o2.bucketName) ?
                       o1.bucketName.compareToIgnoreCase(o2.bucketName) :
                       o1.status.name().compareTo(o2.status.name());
            }
        });
    }

    /**
     * @param bucket-id bucket id of the target bucket. Mandatory
     *
     * @servtitle Returns license module log
     * @httpmethod POST
     * @uri /api/{bucket}/getassignablenodefeatures
     * @responsejson {
     * "result": "ok",
     * "features": [ {@link Feature} ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getassignablenodefeatures()
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);

            //check bucket
            if (!Util.isLong(bucketId))
            {
                throw new ApiException("invalid-bucket-id");
            }
            MongoBucket bucket = MongoBucket.getById(bucketId);
            if (bucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            // for compatibility
            List<Feature> sqlFeatures = new ArrayList<>();
            for (String featureName : bucket.getFeatureNames())
            {
                MongoFeature mongoFeature = MongoFeature.getByName(featureName);
                if (mongoFeature != null && mongoFeature.isAssignableToNodes())
                {
                    sqlFeatures.add(new Feature(mongoFeature));
                }
            }

            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("features", sqlFeatures);
            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
