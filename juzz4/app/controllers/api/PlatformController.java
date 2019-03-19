package controllers.api;

import com.google.gson.Gson;
import controllers.interceptors.APIInterceptor;
import lib.util.ListUtil;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InternalException;
import models.*;
import models.backwardcompatibility.Bucket;
import models.backwardcompatibility.Feature;
import models.transients.UserInfo;
import models.transportobjects.PlatformInfoTransport;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import platform.BucketManager;
import platform.Environment;
import platform.VersionManager;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.With;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * @author KAI Square
 * @sectiontitle Multi-tenancy Management
 * @sectiondesc APIs to manage buckets
 * @publicapi
 */

@With(APIInterceptor.class)
public class PlatformController extends APIController
{
    private static void checkAccess(MongoBucket... bucketsInvolved)
    {
        try
        {
            String callerBucketId = getCallerBucketId();
            MongoBucket callerBucket = MongoBucket.getById(callerBucketId);

            for (MongoBucket bkt : bucketsInvolved)
            {
                if (!callerBucket.hasControlOver(bkt))
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
     * @servtitle Returns list of available bucket
     * @httpmethod GET
     * @uri /api/{bucket}/getbuckets
     * @responsejson {
     * "result": "ok",
     * "buckets": [
     * {
     * "name": "kaisquare",
     * "path": "kaisquare",
     * "description": "Kaisquare admin account",
     * "activated": true,
     * "features": [
     * {
     * "name": "common",
     * "type": "common",
     * "services": [
     * {
     * "name": "login",
     * "version": "1",
     * "id": 1
     * },
     * {
     * "name": "logout",
     * "version": "1",
     * "id": 2
     * }
     * ],
     * "id": 1
     * }
     * ],
     * "services": [
     * {
     * "name": "login",
     * "version": "1",
     * "id": 1
     * },
     * {
     * "name": "logout",
     * "version": "1",
     * "id": 2
     * }
     * ],
     * "users": [
     * {
     * "name": "Admin",
     * "login": "*******",
     * "password": "******",
     * "email": "",
     * "two_factor_mode": 0,
     * "session_timeout": 300,
     * "activated": true,
     * "creationTimestamp": "15:05:14",
     * "phone": "",
     * "language": "en",
     * "bucketId": 2,
     * "roles": [
     * {
     * "name": "Administrator",
     * "description": "Admin account with access to all features",
     * "features": [
     * "name": "common",
     * "type": "common",
     * "services": [
     * {
     * "name": "login",
     * "version": "1",
     * "id": 1
     * },
     * {
     * "name": "logout",
     * "version": "1",
     * "id": 2
     * }
     * ],
     * "id": 1
     * }
     * ],
     * "id": 2
     * }
     * ],
     * "services": [
     * {
     * "name": "login",
     * "version": "1",
     * "id": 1
     * },
     * {
     * "name": "logout",
     * "version": "1",
     * "id": 2
     * }
     * ],
     * "id": 2
     * }
     * ],
     * "roles": [
     * {
     * "name": "Administrator",
     * "description": "Admin account with access to all features",
     * "features": [
     * {
     * "name": "remote-shell",
     * "type": "customer-support",
     * "services": [
     * {
     * "name": "startremoteshell",
     * "version": "1",
     * "id": 302
     * }
     * ],
     * "id": 63
     * }
     * ],
     * "id": 2
     * },
     * {
     * "name": "Supervisor",
     * "description": "Access to all features except admin settings",
     * "features": [],
     * "id": 3
     * }
     * ],
     * "id": 2
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbuckets() throws ApiException
    {
        try
        {
            //optional
            String showDeleted = readApiParameter("show-deleted", false);

            String callerBucketId = getCallerBucketId();
            List<MongoBucket> visibleBuckets = BucketManager.getInstance().getThisAndDescendants(callerBucketId);
            List<MongoBucket> filteredList = new ArrayList<>();

            //whether or not to include deleted buckets
            if (Util.isBoolean(showDeleted) && Boolean.parseBoolean(showDeleted))
            {
                filteredList = visibleBuckets;
            }
            else
            {
                for (MongoBucket bkt : visibleBuckets)
                {
                    if (!bkt.isDeleted())
                    {
                        filteredList.add(bkt);
                    }
                }
            }

            // for compatibility
            List<Bucket> sqlBuckets = new ArrayList<>();
            for (MongoBucket mongoBucket : filteredList)
            {
                sqlBuckets.add(new Bucket(mongoBucket));
            }

            // sort by name
            Collections.sort(sqlBuckets, new Comparator<Bucket>()
            {
                @Override
                public int compare(Bucket o1, Bucket o2)
                {
                    return o1.name.compareToIgnoreCase(o2.name);
                }
            });

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("buckets", sqlBuckets);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-name        The name of bucket e.g kaisquare. Mandatory
     * @param parent-bucket-id   The id of parent. Mandatory
     * @param bucket-path        The path of bucket e.g kaisquare. Mandatory
     * @param bucket-description Description of bucket
     *
     * @servtitle Adds bucket
     * @httpmethod POST
     * @uri /api/{bucket}/addbucket
     * @responsejson {
     * "result": "ok",
     * "bucket-id": <i>Id of currently added bucket</i>
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void addbucket() throws ApiException
    {
        try
        {
            String name = readApiParameter("bucket-name", true).toLowerCase();
            String parentId = readApiParameter("parent-bucket-id", true);
            String path = readApiParameter("bucket-path", true);
            String description = readApiParameter("bucket-description", true);

            if (!Util.isLong(parentId))
            {
                throw new ApiException("invalid-parent-id");
            }

            MongoBucket parentBucket = MongoBucket.getById(parentId);
            if (parentBucket == null)
            {
                throw new ApiException("parent-bucket-not-found");
            }

            //check if the caller have rights for this action
            checkAccess(parentBucket);

            //create new
            MongoBucket newBucket = BucketManager.getInstance().createNewBucket(name, parentId, path, description);

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(newBucket.getBucketId());
                bLog.bucketName = newBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add("Added " + newBucket.getName());
                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("bucket-id", newBucket.getBucketId());
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id          The id of bucket to update. Mandatory
     * @param parent-bucket-id   The id of parent. Mandatory
     * @param bucket-name        The name of bucket e.g kaisquare. Mandatory
     * @param bucket-path        The path of bucket e.g kaisquare. Mandatory
     * @param bucket-description Description of bucket
     *
     * @servtitle Updates a bucket
     * @httpmethod POST
     * @uri /api/{bucket}/updatebucket
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatebucket() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            String parentId = readApiParameter("parent-bucket-id", true);
            String name = readApiParameter("bucket-name", true);
            String path = readApiParameter("bucket-path", true);
            String description = readApiParameter("bucket-description", true);

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("bucket-not-found");
            }

            Long parsedParentId = Long.parseLong(parentId);
            if (parsedParentId == 0L)
            {
                throw new ApiException("parent-bucket-not-specified");
            }

            MongoBucket parentBucket = MongoBucket.getById(parentId);
            if (parentBucket == null)
            {
                throw new ApiException("parent-bucket-not-found");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket, parentBucket);

            //update
            BucketManager.getInstance().updateBucket(bucketId, parentId, name, path, description);

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add("Updated info");

                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id The id of bucket to delete. Mandatory
     *
     * @servtitle Deletes a bucket
     * @httpmethod POST
     * @uri /api/{bucket}/removebucket
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void removebucket() throws ApiException
    {
        try
        {

            String bucketId = readApiParameter("bucket-id", true);

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("bucket-not-found");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket);

            //make sure child buckets are already deleted
            List<MongoBucket> children = BucketManager.getInstance().getDescendants(bucketId);
            for (MongoBucket child : children)
            {
                if (!child.isDeleted())
                {
                    throw new ApiException("error-delete-children-first");
                }
            }

            //remove
            BucketManager.getInstance().markAsDeleted(bucketId);

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add("Deleted the bucket");
                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id The id of bucket to delete. Mandatory
     *
     * @servtitle Restores a deleted bucket
     * @httpmethod POST
     * @uri /api/{bucket}/restorebucket
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void restorebucket() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("bucket-not-found");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket);

            //make sure parents are not in deleted state
            List<MongoBucket> ancestors = BucketManager.getInstance().getAncestors(bucketId);
            for (MongoBucket ancestor : ancestors)
            {
                if (ancestor.isDeleted())
                {
                    throw new ApiException("error-restore-parent-first");
                }
            }

            //restore
            BucketManager.getInstance().restoreDeletedBucket(bucketId);

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add("Restored the bucket");
                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id The id of bucket. Mandatory
     *
     * @servtitle Retrieves list of features available in a bucket
     * @httpmethod POST
     * @uri /api/{bucket}/getbucketfeatures
     * @responsejson {
     * "result": "ok",
     * "features": [
     * {
     * "name":"common",
     * "type":"common",
     * "services": [
     * {
     * "name":"login",
     * "version":"1",
     * "id":34
     * },
     * {
     * "name":"logout",
     * "version":"1",
     * "id":35
     * },
     * ],
     * "id":77
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketfeatures(String bucket) throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", false);

            MongoBucket targetBucket = null;
            if (bucketId.isEmpty())
            {
                targetBucket = MongoBucket.getByName(bucket);
            }
            else
            {
                targetBucket = MongoBucket.getById(bucketId);
            }

            if (targetBucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket);

            // for compability
            List<Feature> sqlFeatures = new ArrayList<>();
            for (String featureName : targetBucket.getFeatureNames())
            {
                MongoFeature feature = MongoFeature.getByName(featureName);
                sqlFeatures.add(new Feature(feature));
            }
            Collections.sort(sqlFeatures, Feature.sortByPosition);

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("features", sqlFeatures);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Retrieves list of all features supported by the system
     * @httpmethod GET
     * @uri /api/{bucket}/getallfeatures
     * @responsejson {
     * "result": "ok",
     * "features": [
     * {
     * "name":"common",
     * "type":"common",
     * "services": [
     * {
     * "name":"login",
     * "version":"1",
     * "id":34
     * },
     * {
     * "name":"logout",
     * "version":"1",
     * "id":35
     * },
     * ],
     * "id":77
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getallfeatures(String bucket) throws ApiException
    {
        try
        {
            // for compability
            List<Feature> sqlFeatures = new ArrayList<>();
            List<MongoFeature> allFeatures = MongoFeature.q().fetchAll();
            for (MongoFeature feature : allFeatures)
            {
                sqlFeatures.add(new Feature(feature));
            }
            Collections.sort(sqlFeatures, Feature.sortByPosition);

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("features", sqlFeatures);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id           The id of bucket. Mandatory
     * @param feature-assignments Comma separated values. e.g ["live-view", "monitoring"]
     *
     * @servtitle Updates the list of features that should be available in a bucket
     * @httpmethod GET
     * @uri /api/{bucket}/updatebucketfeatures
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "features-update-failed"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatebucketfeatures() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            String featureAssignments = readApiParameter("feature-assignments", true);

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket);

            //parse feature list
            List<String> incomingFeatureNames = new ArrayList<>();
            incomingFeatureNames = new Gson().fromJson(featureAssignments, incomingFeatureNames.getClass());

            //verify feature names
            List<String> updateFeatureNames = new ArrayList<>();
            for (String featureName : incomingFeatureNames)
            {
                MongoFeature f = MongoFeature.getByName(featureName);
                if (f == null)
                {
                    throw new ApiException("invalid-feature-name");
                }
                updateFeatureNames.add(f.getName());
            }

            //find diff
            List<String> removedFeatureNames = ListUtil.getExtraItems(updateFeatureNames, targetBucket.getFeatureNames());
            List<String> addedFeatureNames = ListUtil.getExtraItems(targetBucket.getFeatureNames(), updateFeatureNames);

            //apply changes. Note: must remove first
            BucketManager bktMgr = BucketManager.getInstance();
            bktMgr.removeFeatures(bucketId, removedFeatureNames);
            bktMgr.addFeatures(bucketId, addedFeatureNames);

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();

                for (String featureName : removedFeatureNames)
                {
                    bLog.changes.add("Removed Feature : " + Messages.get(featureName));
                }
                for (String featureName : addedFeatureNames)
                {
                    bLog.changes.add("Added Feature : " + Messages.get(featureName));
                }

                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Return list of all users of current bucket
     * @httpmethod GET
     * @uri /api/{bucket}/getbucketusers
     * @responsejson {
     * "result": "ok",
     * "users":	[
     * {
     * "name":"Admin",
     * "login":"admin",
     * "email":"",
     * "userId":2,
     * "roles":"Administrator ",
     * "joinedLabels":["admin","kaisquare"],
     * "phone":"",
     * "activated":true
     * },
     * {
     * "name":"Supervisor",
     * "login":"demo1",
     * "email":"",
     * "userId":3,
     * "roles":"Supervisor ",
     * "joinedLabels":"",
     * "phone":"",
     * "activated":true
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketusers(String bucket) throws ApiException
    {
        try
        {
            String currentBucketId = getCallerBucketId();

            //verify bucket-id validity
            if (currentBucketId == null || currentBucketId.isEmpty())
            {
                throw new InternalException("Bucket ID is invalid, this should not happen!");
            }

            //verify bucket-id validity
            MongoBucket targetBucket = MongoBucket.getById(currentBucketId);
            if (targetBucket == null)
            {
                throw new InternalException("Bucket object is null, this should not happen!");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket);

            List<models.transients.UserInfo> resultset = new ArrayList<>();
            List<MongoUser> bucketUsers = MongoUser.q().filter("bucketId", currentBucketId).fetchAll();
            for (MongoUser bucketUser : bucketUsers)
            {
                resultset.add(bucketUser.getAsUserInfo());
            }

            // sort by name
            Collections.sort(resultset, new Comparator<UserInfo>()
            {
                @Override
                public int compare(UserInfo o1, UserInfo o2)
                {
                    return o1.name.compareToIgnoreCase(o2.name);
                }
            });

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("users", resultset);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id                   Id of a bucket. Mandatory
     * @param max-vca-count               Maximum number of vca that is assigned to this bucket. e.g 5. Mandatory
     * @param bucket-userlimit            Maximum number of users to this bucket. e.g 10. Mandatory
     * @param email-verification-of-users wheather to verify user by email or not i.e true/false. Mandatory
     *
     * @servtitle Updates settings of a bucket
     * @httpmethod POST
     * @uri /api/{bucket}/updatebucketsettings
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatebucketsettings() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            String userLimit = readApiParameter("bucket-userlimit", true);
            String emailVerificationOfUsersEnabled = readApiParameter("email-verification-of-users", true);
            String customlogo = readApiParameter("custom-logo", false);
            String binarydata = readApiParameter("binary-data", false);
            String mapSource = readApiParameter("map-source", false);


            //check if the caller have rights for this action
            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            checkAccess(targetBucket);

            BucketManager.getInstance().updateBucketSettings(bucketId,
                                                             Integer.parseInt(userLimit),
                                                             Boolean.parseBoolean(emailVerificationOfUsersEnabled),
                                                             mapSource,
                                                             Boolean.parseBoolean(customlogo),
                                                             binarydata);

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add("Updated bucket settings");
                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id Id of a bucket to activate. Mandatory
     *
     * @servtitle Activates (un-suspends) an inactive (suspended) bucket
     * @httpmethod POST
     * @uri /api/{bucket}/activatebucket
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void activatebucket() throws ApiException
    {
        Map responseMap = new ResultMap();
        try
        {
            String bucketId = readApiParameter("bucket-id", true);

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("bucket-not-found");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket);

            // check if parent is suspended
            MongoBucket parent = MongoBucket.getById(targetBucket.getParentId());
            if (parent != null && parent.isSuspended())
            {
                throw new ApiException("msg-activate-parent-first");
            }

            BucketManager.getInstance().activateBucket(bucketId);
            responseMap.put("result", "ok");

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add("Activated the bucket");
                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

        }
        catch (Exception e)
        {
            respondError(e);
        }

        renderJSON(responseMap);
    }

    /**
     * @param bucket-id Id of a bucket to deactivate. Mandatory
     *
     * @servtitle Deactivates (suspends) a bucket
     * @httpmethod POST
     * @uri /api/{bucket}/deactivatebucket
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void deactivatebucket() throws ApiException
    {
        Map responseMap = new ResultMap();
        try
        {
            String bucketId = readApiParameter("bucket-id", true);

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("bucket-not-found");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket);

            BucketManager.getInstance().deactivateBucket(bucketId);
            responseMap.put("result", "ok");

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add("Deactivated the bucket");
                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

        }
        catch (Exception e)
        {
            respondError(e);
        }

        renderJSON(responseMap);
    }

    /**
     * @param bucketLogo The image file with png format.
     *
     * @servtitle convert the image file to base 64 binary data
     * @httpmethod POST
     * @uri /api/{bucket}/uploadlogobinarydata
     * @responsejson {
     * image base 64 string
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void uploadlogobinarydata(File bucketLogo) throws ApiException
    {
        try
        {
            if (null == bucketLogo)
            {
                throw new ApiException("attached file not found");
            }
            InputStream is = new FileInputStream(bucketLogo);

            Map map = new ResultMap();
            byte[] bytes = IOUtils.toByteArray(is);
            map.put("result", "ok");
            map.put("binaryData", Base64.encodeBase64String(bytes));
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id The id of the bucket.
     *
     * @servtitle servers bucket settings for the bucket id provided
     * @httpmethod POST
     * @uri /api/{bucket}/getbucketsetting
     * @responsejson {
     * result: "ok",
     * bucketId: XX,
     * totalVCACount: XX,
     * userLimit: XX,
     * emailVerificationOfUsersEnabled: true/false,
     * logoBlobId: "xxxx",
     * mapSource: "google/baidu"
     * };
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketsetting() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("bucket-not-found");
            }
            models.BucketSetting bucketSetting = BucketManager.getInstance().getBucketSetting(bucketId);
            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("bucketId", bucketSetting.bucketId);
            map.put("userLimit", bucketSetting.userLimit);
            map.put("emailVerificationOfUsersEnabled", bucketSetting.emailVerificationOfUsersEnabled);
            map.put("logoBlobId", bucketSetting.logoBlobId);
            map.put("mapSource", bucketSetting.mapSource);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id The id of the bucket.
     *
     * @servtitle retrieve bucket logs
     * @httpmethod POST
     * @uri /api/{bucket}/getbucketlogs
     * @responsejson {
     * result: "ok",
     * logs: [
     * {@link models.BucketLog},
     * {@link models.BucketLog}
     * ],
     * };
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketlogs() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("bucket-not-found");
            }

            List<BucketLog> logs = BucketLog.q().filter("bucketId", Long.parseLong(bucketId)).order("-time").fetchAll();
            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("logs", logs);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id   Bucket id
     * @param file-format "pdf" or "xls".
     *
     * @servtitle Export users list as a file by bucket id.
     * @httpmethod GET
     * @uri /api/{bucket}/exportusersfilebybucketid
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportusersfilebybucketid()
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            FileFormat fileFormat = FileFormat.parse(readApiParameter("file-format", true));

            MongoBucket bucket = MongoBucket.getById(bucketId);
            if (null == bucket)
            {
                throw new ApiException("Bucket ID is invalid, this should not happen!");
            }

            ReportBuilder reportBuilder = BucketManager.getInstance().exportBucketUsers(bucketId, fileFormat, Lang.get());
            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id   Bucket id
     * @param file-format "pdf" or "xls".
     *
     * @servtitle Export nodes list as a file by bucket id.
     * @httpmethod POST
     * @uri /api/{bucket}/exportnodesbybucketid
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportnodesbybucketid()
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            FileFormat fileFormat = FileFormat.parse(readApiParameter("file-format", true));

            MongoBucket bucket = MongoBucket.getById(bucketId);
            if (null == bucket)
            {
                throw new ApiException("Bucket ID is invalid, this should not happen!");
            }

            ReportBuilder reportBuilder = BucketManager.getInstance().exportBucketNodes(bucketId, fileFormat, Lang.get());
            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Returns platform server information
     * @httpmethod POST
     * @uri /api/{bucket}/getplatforminformation
     * @responsejson {
     * "result":"ok",
     * "info" : {@link models.transportobjects.PlatformInfoTransport}
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getplatforminformation() throws ApiException
    {
        try
        {
            Environment env = Environment.getInstance();
            VersionManager verMgr = VersionManager.getInstance();

            PlatformInfoTransport platformInfo = new PlatformInfoTransport(env.getServerStartedTime(),
                                                                           env.getApplicationType(),
                                                                           verMgr.getPlatformVersion(),
                                                                           verMgr.getCodeCommitHash(),
                                                                           verMgr.getReleaseTag());

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("info", platformInfo);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id The id of the bucket.
     *
     * @servtitle servers bucket password policy for the bucket id provided
     * @httpmethod POST
     * @uri /api/{bucket}/getbucketpasswordpolicy
     * @responsejson {
     * result: "ok",
     * passwordPolicy: {
     * "requiredUppercase":false,
     * "requiredLowercase":false,
     * "requiredNumeric":false,
     * "requiredSpecialChar":false,
     * "enabledPasswordExpiration":false,
     * "preventedPasswordReuse":false,
     * "emailWhenPasswordExpired":false,
     * "minimumPasswordLength":8,
     * "passwordExpirationDays":15,
     * "numberOfReusePasswordPrevention":1
     * }
     * };
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketpasswordpolicy() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);

            MongoBucket bucket = MongoBucket.getById(bucketId);
            if (bucket == null)
            {
                throw new ApiException("bucket-not-found");
            }
            models.BucketPasswordPolicy passwordPolicy = new models.BucketPasswordPolicy(Long.parseLong(bucketId)).findOrCreate();
            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("passwordPolicy", passwordPolicy);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id                           The id of bucket. Mandatory
     * @param required-uppercase                  Boolean, Require at least one uppercase letter
     * @param required-lowercase                  Boolean, Require at least one lowercase letter
     * @param required-numeric                    Boolean, Require at least one number
     * @param required-special-character          Boolean, Require at least one special character
     * @param enabled-password-expiration         Boolean, Enable password expiration
     * @param prevented-password-reuse            Boolean, Prevent password reuse
     * @param email-when-password-expired         Boolean, Email when password expired
     * @param minimum-password-length             Number, Minimum password length
     * @param password-expiration-days            Number, Password expiration period (in days)
     * @param number-of-reuse-password-prevention Number, Number of passwords to remember
     *
     * @servtitle Updates servers bucket password policy
     * @httpmethod GET
     * @uri /api/{bucket}/updatebucketpasswordpolicy
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatebucketpasswordpolicy() throws ApiException
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            boolean requiredUppercase = asBoolean(readApiParameter("required-uppercase", true));
            boolean requiredLowercase = asBoolean(readApiParameter("required-lowercase", true));
            boolean requiredNumeric = asBoolean(readApiParameter("required-numeric", true));
            boolean requiredSpecialChar = asBoolean(readApiParameter("required-special-character", true));
            boolean requiredFirstLoginPassCheck = asBoolean(readApiParameter("required-first-login-password-change",
                                                                             true));
            boolean enabledPasswordExpiration = asBoolean(readApiParameter("enabled-password-expiration", true));
            boolean preventedPasswordReuse = asBoolean(readApiParameter("prevented-password-reuse", true));
            // boolean emailWhenPasswordExpired = asBoolean(readApiParameter("email-when-password-expired", true));
            int minimumPasswordLength = asInt(readApiParameter("minimum-password-length", true));
            int passwordExpirationDays = asInt(readApiParameter("password-expiration-days", true));
            int numberOfReusePasswordPrevention = asInt(readApiParameter("number-of-reuse-password-prevention", true));

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            if (minimumPasswordLength < 8)
            {
                throw new ApiException("minimum-8-digits-password-length");
            }

            //check if the caller have rights for this action
            checkAccess(targetBucket);

            //parse settings
            models.BucketPasswordPolicy passwordPolicy = new models.BucketPasswordPolicy(Long.parseLong(bucketId)).findOrCreate();
            passwordPolicy.setRequiredUppercase(requiredUppercase);
            passwordPolicy.setRequiredLowercase(requiredLowercase);
            passwordPolicy.setRequiredNumeric(requiredNumeric);
            passwordPolicy.setRequiredSpecialChar(requiredSpecialChar);
            passwordPolicy.setEnabledPasswordExpiration(enabledPasswordExpiration);
            passwordPolicy.setPreventedPasswordReuse(preventedPasswordReuse);
            // passwordPolicy.setEmailWhenPasswordExpired(emailWhenPasswordExpired);
            passwordPolicy.setMinimumPasswordLength(minimumPasswordLength);
            passwordPolicy.setPasswordExpirationDays(passwordExpirationDays);
            passwordPolicy.setNumberOfReusePasswordPrevention(numberOfReusePasswordPrevention);
            passwordPolicy.setRequiredFirstLoginPasswordCheck(requiredFirstLoginPassCheck);
            passwordPolicy.save();

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add("Update password policy: " + targetBucket.getName());
                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

            Map map = new ResultMap();
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
