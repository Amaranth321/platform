package platform;

import lib.util.ListUtil;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.*;
import models.access.AccessKey;
import models.archived.ArchivedBucket;
import models.backwardcompatibility.DeviceLabel;
import models.licensing.NodeLicense;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.notification.UserNotificationSettings;
import platform.access.DefaultBucket;
import platform.access.DefaultRole;
import platform.access.DefaultUser;
import platform.access.UserSessionManager;
import platform.config.readers.AccountDefaultSettings;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import platform.content.export.manual.BucketNodesPdf;
import platform.content.export.manual.BucketNodesXls;
import platform.content.export.manual.BucketUsersPdf;
import platform.content.export.manual.BucketUsersXls;
import platform.db.cache.CacheClient;
import platform.devices.DeviceChannelPair;
import platform.label.LabelManager;
import platform.pubsub.PlatformEventMonitor;
import platform.pubsub.PlatformEventType;
import play.Logger;
import play.jobs.Job;

import java.util.*;

import static lib.util.Util.isNullOrEmpty;

public class BucketManager
{
    private static final BucketManager instance = new BucketManager();

    private BucketManager()
    {
    }

    public static BucketManager getInstance()
    {
        return instance;
    }

    public MongoBucket createNewBucket(String name, String parentId, String path, String description) throws ApiException
    {
        MongoBucket existingBucket = MongoBucket.getByName(name);
        if (existingBucket != null)
        {
            throw new ApiException("company-id-already-in-use");
        }

        if (name.length() > 20)
        {
            throw new ApiException("bucket-name-length-exceeded");
        }

        if (!isNullOrEmpty(description) && description.length() > 255)
        {
            throw new ApiException("description-limit-exceeded");
        }

        MongoBucket parentBucket = MongoBucket.getById(parentId);
        if (parentBucket == null)
        {
            throw new ApiException("invalid-parent-bucket-id");
        }

        // get default features
        List<String> defaultBucketFeatureNames = AccountDefaultSettings.getInstance().getBucketFeatureNames();
        List<String> filteredFeatureNames = new ArrayList<>();
        for (String defaultBucketFeatureName : defaultBucketFeatureNames)
        {
            if (parentBucket.getFeatureNames().contains(defaultBucketFeatureName))
            {
                filteredFeatureNames.add(defaultBucketFeatureName);
            }
        }

        // create bucket
        MongoBucket bucket = new MongoBucket(name, path, description, true);
        bucket.setBucketId(MongoBucket.generateNewId());
        bucket.setParentId(parentId);
        bucket.save();

        // add default features
        this.addFeatures(bucket.getBucketId(), filteredFeatureNames);

        // create default roles
        List<DefaultRole> defaultRoles = Arrays.asList(DefaultRole.ADMINISTRATOR);
        for (DefaultRole defaultRole : defaultRoles)
        {
            // create role
            MongoRole role = new MongoRole(bucket.getBucketId(), defaultRole.getRoleName(), defaultRole.getDescription());
            role.setRoleId(MongoRole.generateNewId());
            role.save();

            // create default users
            for (DefaultUser defaultUser : defaultRole.getUsers())
            {
                // create user
                MongoUser user = new MongoUser(bucket.getBucketId(), defaultUser.getFullName(), defaultUser.getUsername(), defaultUser.getPassword(), "", "", "en");
                user.setUserId(MongoUser.generateNewId());
                user.setBucketId(bucket.getBucketId());
                user.addRoleId(role.getRoleId());
                user.setActivated(true);
                user.getServiceNames().addAll(FeatureManager.getInstance().getCommonServiceNames());
                user.save();

                // create user password history
                UserPasswordHistory adminUserPassHistory = new UserPasswordHistory(Long.parseLong(user.getUserId()), user.getPassword(), true);
                adminUserPassHistory.save();
            }

            RoleManager.getInstance().addFeatures(role.getRoleId(), defaultRole.getFeatureNames());
        }

        return bucket;
    }

    public void checkDefaultBuckets()
    {
        for (DefaultBucket defaultBucket : DefaultBucket.values())
        {
            try
            {
                defaultBucket.createIfNotExists();
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
    }

    public void activateDefaultAdmins(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        // verify admin role
        MongoRole adminRole = MongoRole.q()
                .filter("bucketId", bucketId)
                .filter("name", DefaultRole.ADMINISTRATOR.getRoleName())
                .get();

        if (adminRole == null)
        {
            throw new IllegalStateException(String.format("No admin role found for %s bucket", bucket.getName()));
        }

        // verify and activate admin users
        List<MongoUser> bucketUsers = MongoUser.q().filter("bucketId", bucketId).fetchAll();
        for (MongoUser bucketUser : bucketUsers)
        {
            if (bucketUser.getRoleIds().contains(adminRole.getRoleId()) && !bucketUser.isActivated())
            {
                bucketUser.setActivated(true);
                bucketUser.save();
                Logger.info("Admin user (%s:%s) has been activated", bucket, bucketUser.getLogin());
            }
        }
    }

    public boolean updateBucket(String bucketId, String newParentBucketId, String newName, String newPath, String newDescription) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        // verify fields
        if (newName.length() > 20)
        {
            throw new ApiException("bucket-name-length-exceeded");
        }

        if (Util.isNullOrEmpty(newDescription) && newDescription.length() > 255)
        {
            throw new ApiException("description-limit-exceeded");
        }

        MongoBucket newParentBucket = MongoBucket.getById(newParentBucketId);
        if (bucket.hasControlOver(newParentBucket))
        {
            throw new ApiException("msg-parent-cannot-be-subtree");
        }

        String oldParentBucketId = bucket.getParentId();
        bucket.setParentId(newParentBucket.getBucketId());
        bucket.setName(newName);
        bucket.setPath(newPath);
        bucket.setDescription(newDescription);

        // save before removing extra features
        bucket.save();

        // check if the parent has changed
        if (!oldParentBucketId.equals(newParentBucketId))
        {
            if (newParentBucket.isSuspended())
            {
                throw new ApiException("msg-new-parent-suspended");
            }

            bucketParentChanged(bucketId, newParentBucketId);
        }

        return true;
    }

    /**
     * All child buckets will also be set to deleted recursively
     */
    public void markAsDeleted(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        // recursively mark all children as deleted
        List<MongoBucket> childBuckets = MongoBucket.q().filter("parentId", bucketId).fetchAll();
        for (MongoBucket childBucket : childBuckets)
        {
            markAsDeleted(childBucket.getBucketId());
        }

        boolean result = bucket.setDeleted(true);
        if (result)
        {
            bucket.save();
        }

        // remove user sessions
        UserSessionManager.getInstance().removeSessionsOfBucket(bucketId);

        //remove cache
        CacheClient cacheClient = CacheClient.getInstance();
        cacheClient.remove(cacheClient.getBucket(bucket.getBucketId()));

        Logger.info("%s has been marked as deleted", bucket);
    }

    /**
     * All child buckets will also be set to un-deleted recursively
     */
    public void restoreDeletedBucket(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        // recursively restore all children
        List<MongoBucket> childBuckets = MongoBucket.q().filter("parentId", bucketId).fetchAll();
        for (MongoBucket childBucket : childBuckets)
        {
            restoreDeletedBucket(childBucket.getBucketId());
        }

        boolean result = bucket.setDeleted(false);
        if (result)
        {
            bucket.save();
        }

        Logger.info("%s has been restored", bucket);
    }

    public void permanentDeleteBucket(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        final String bucketName = bucket.getName();
        final String _bucketId = bucketId;
        Long bucketIdLong = Long.parseLong(bucketId);

        // remove and disable node licenses
        List<NodeLicense> bLicenses = NodeLicense.q().filter("cloudBucketId", bucketIdLong).fetchAll();
        for (NodeLicense license : bLicenses)
        {
            CloudLicenseManager.getInstance().deleteNodeLicense(license.licenseNumber);
        }
        Logger.info(bucketName + ": node licenses deleted");

        // remove devices
        List<MongoDevice> devices = MongoDevice.q().filter("bucketId", bucketId).fetchAll();
        for (MongoDevice device : devices)
        {
            DeviceManager.getInstance().removeDeviceFromBucket(bucketId, device.getDeviceId());
        }
        Logger.info(bucketName + ": devices deleted");

        // remove users
        List<MongoUser> users = MongoUser.q().filter("bucketId", bucketId).fetchAll();
        for (MongoUser user : users)
        {
            // remove user preference
            MongoUserPreference userPreference = MongoUserPreference.getByUserId(user.getUserId());
            if (userPreference != null)
            {
                userPreference.delete();
            }
            user.delete();
        }
        Logger.info(bucketName + ": users deleted");

        // remove roles
        MongoRole.q().filter("bucketId", bucketId).delete();
        Logger.info(bucketName + ": roles deleted");

        // remove pois
        Poi.q().filter("bucketId", bucketIdLong).delete();
        Logger.info(bucketName + ": poi deleted");

        // remove bucket settings
        BucketSetting.q().filter("bucketId", bucketId).delete();
        Logger.info(bucketName + ": bucket setting deleted");

        DeviceLabel.q().filter("bucketId", bucketIdLong).delete();
        Logger.info(bucketName + ": device label deleted");

        UserNotificationSettings.q().filter("bucketId", bucketIdLong).delete();
        Logger.info(bucketName + ": UserNotificationSettings deleted");

        SchedulePreset.q().filter("bucketId", bucketIdLong).delete();
        Logger.info(bucketName + ": schedule preset deleted");

        UserLabel.q().filter("bucketId", bucketIdLong).delete();
        Logger.info(bucketName + ": user label deleted");

        AccessKey.q().filter("bucketID", bucketIdLong).delete();
        Logger.info(bucketName + ": access keys deleted");

        POSImportSettings.q().filter("bucketId", bucketIdLong).delete();
        Logger.info(bucketName + ": POS import settings deleted");

        // remove huge list of entries in another thread
        new Job()
        {
            @Override
            public void doJob() throws Exception
            {
//                Logger.info("(%s) Deleting AuditLogs", bucketName);
//                AuditLog.q().filter("bucketId", _bucketId).delete();

                Logger.info("(%s) Deleting PosDataReports", bucketName);
                PosDataReport.q().filter("bucket", _bucketId).delete();

                Logger.info("(%s) Deleting SyncLogs", bucketName);
                SyncLog.q().filter("bucket", _bucketId).delete();
            }
        }.in(1);

        // remove bucket
        bucket.delete();
        Logger.info(bucketName + ": bucket successfully removed");
    }

    public void updateBucketSettings(String bucketId, int userLimit, boolean emailVerificationOfUsers, String mapSource, boolean customLogo, String logoBase64String) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        BucketSetting bucketSetting = getBucketSetting(bucketId);
        String prevLogoId = bucketSetting.logoBlobId;

        if (customLogo && logoBase64String.length() > 0)
        {
            bucketSetting.setBucketLogo(logoBase64String);
        }
        else
        {
            bucketSetting.setBucketLogo("");
        }

        bucketSetting.userLimit = userLimit;
        bucketSetting.emailVerificationOfUsersEnabled = emailVerificationOfUsers;
        bucketSetting.mapSource = mapSource;
        bucketSetting.save();

        //update nodes if needed
        if (Environment.getInstance().onCloud())
        {
            Map<String, Object> infoMap = new LinkedHashMap<>();

            //check if logo changes
            boolean logoChanged = false;
            if (Util.isNullOrEmpty(prevLogoId))
            {
                if (!Util.isNullOrEmpty(bucketSetting.logoBlobId))
                {
                    logoChanged = true;
                }
            }
            else if (!prevLogoId.equals(bucketSetting.logoBlobId))
            {
                logoChanged = true;
            }

            if (logoChanged)
            {
                infoMap.put("logoBinary", bucketSetting.getBase64EncodedLogoString());
            }

            if (!infoMap.isEmpty())
            {
                CloudActionMonitor.getInstance().cloudUpdateBucketSettings(bucketId, infoMap);
            }
        }
    }

    public void activateBucket(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        if (bucket.isActivated())
        {
            Logger.info(Util.whichFn() + "%s is already activated", bucket);
            return;
        }

        //default admins should always be active if the bucket is active
        activateDefaultAdmins(bucketId);

        bucket.setActivated(true);
        bucket.save();
        Logger.info("%s has been activated", bucket);

        //Notify others
        PlatformEventMonitor.getInstance().broadcast(PlatformEventType.BUCKET_ACTIVATED, bucketId);
    }

    public void deactivateBucket(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        if (!bucket.isActivated())
        {
            Logger.info(Util.whichFn() + "%s is already deactivated", bucket);
            return;
        }

        UserSessionManager.getInstance().removeSessionsOfBucket(bucketId);
        bucket.setActivated(false);
        bucket.save();
        Logger.info("%s has been deactivated", bucket);

        // notify others
        PlatformEventMonitor.getInstance().broadcast(PlatformEventType.BUCKET_SUSPENDED, bucketId);
    }

    public boolean checkBucketUserLimit(String bucketId) throws ApiException
    {
        //Check user limit from bucket settings
        BucketSetting bucketSetting = BucketSetting.q().filter("bucketId", bucketId).get();
        if (bucketSetting != null)
        {
            long bucketUserCount = MongoUser.q().filter("bucketId", bucketId).count();
            return bucketUserCount <= bucketSetting.userLimit;
        }

        return false;
    }

    public BucketSetting getBucketSetting(String bucketId) throws ApiException
    {
        BucketSetting bucketSetting = BucketSetting.find("bucketId", bucketId).first();
        if (bucketSetting == null)
        {
            Logger.info("(bucketId=%s) Bucket settings not found. A new one is created", bucketId);
            bucketSetting = new BucketSetting(bucketId);
            bucketSetting.save();
        }

        //some old buckets do not have mapSource
        if (Util.isNullOrEmpty(bucketSetting.mapSource))
        {
            bucketSetting.mapSource = LocationManager.DEFAULT_MAP_SOURCE;
            bucketSetting.save();
        }

        return bucketSetting;
    }

    public void addFeatures(String bucketId, List<String> addFeatureNames) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        List<String> actualAddFeatureNames = ListUtil.getExtraItems(bucket.getFeatureNames(), addFeatureNames);
        if (actualAddFeatureNames.isEmpty())
        {
            return;
        }

        //update bucket
        bucket.getFeatureNames().addAll(actualAddFeatureNames);
        bucket.save();

        //auto add this feature to root or admin role
        List<String> roleNames = Arrays.asList(DefaultRole.ROOT.getRoleName(), DefaultRole.ADMINISTRATOR.getRoleName());
        List<MongoRole> roles = MongoRole.q()
                .filter("bucketId", bucket.getBucketId())
                .filter("name in", roleNames)
                .fetchAll();

        for (MongoRole role : roles)
        {
            RoleManager.getInstance().addFeatures(role.getRoleId(), actualAddFeatureNames);
        }

        Logger.info("(%s) Added features: %s", bucket.getName(), actualAddFeatureNames);
    }

    // recursive
    public void removeFeatures(String bucketId, List<String> removeFeatureNames) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        List<String> actualRemoveFeatureNames = ListUtil.hasInCommon(bucket.getFeatureNames(), removeFeatureNames);
        if (actualRemoveFeatureNames.isEmpty())
        {
            return;
        }
        bucket.removeFeatureNames(actualRemoveFeatureNames);
        bucket.save();

        Logger.info("(%s) Removed features: %s", bucket.getName(), actualRemoveFeatureNames);

        //if on cloud, remove these from licenses
        if (Environment.getInstance().onCloud())
        {
            CloudLicenseManager.getInstance().bucketFeaturesRemoved(bucketId, actualRemoveFeatureNames);
        }

        //remove these features from roles
        List<MongoRole> roles = MongoRole.q().filter("bucketId", bucketId).fetchAll();
        for (MongoRole role : roles)
        {
            RoleManager.getInstance().removeFeatures(role.getRoleId(), actualRemoveFeatureNames);
        }

        //remove features from children
        List<MongoBucket> childBuckets = MongoBucket.q().filter("parentId", bucketId).fetchAll();
        for (MongoBucket childBucket : childBuckets)
        {
            removeFeatures(childBucket.getBucketId(), actualRemoveFeatureNames);
        }
    }

    /**
     * returns the target bucket and all sub buckets under it
     * as a flat structure
     */
    public List<MongoBucket> getThisAndDescendants(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        // superadmin should see everything
        if (bucket.getName().equals(DefaultBucket.SUPERADMIN.getBucketName()))
        {
            return MongoBucket.q().order("name").fetchAll();
        }

        // the rest will only see herself and children
        List<MongoBucket> meAndChildren = new ArrayList<>();
        meAndChildren.add(bucket);
        meAndChildren.addAll(getDescendants(bucketId));

        return meAndChildren;
    }

    public void checkOrphanedBuckets()
    {
        MongoBucket superAdminBucket = MongoBucket.getByName(DefaultBucket.SUPERADMIN.getBucketName());
        if (superAdminBucket == null)
        {
            return;
        }

        List<MongoBucket> buckets = MongoBucket.q().filter("name <>", superAdminBucket.getName()).fetchAll();
        for (MongoBucket bucket : buckets)
        {
            if (Util.isNullOrEmpty(bucket.getParentId()))
            {
                bucket.setParentId(superAdminBucket.getBucketId());
                bucket.save();
                Logger.info("Set parent of orphaned bucket (%s) to superadmin", bucket.getName());
            }
        }
    }

    /**
     * This returns ALL sub buckets, not just immediate children
     * as a list
     */
    public List<MongoBucket> getDescendants(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        List<MongoBucket> descendants = new ArrayList<>();

        try
        {
            List<MongoBucket> childBuckets = MongoBucket.q().filter("parentId", bucketId).fetchAll();
            descendants.addAll(childBuckets);

            // recursively add child buckets
            for (MongoBucket childBucket : childBuckets)
            {
                descendants.addAll(getDescendants(childBucket.getBucketId()));
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "Failed to get child buckets of '%s'", bucket);
        }

        return descendants;
    }

    /**
     * This returns all buckets in the ancestry as a list
     * <p/>
     * Recursive
     */
    public List<MongoBucket> getAncestors(String bucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        List<MongoBucket> ancestors = new ArrayList<>();

        try
        {
            MongoBucket parentBucket = MongoBucket.getById(bucket.getParentId());
            if (parentBucket == null)
            {
                return ancestors;
            }

            ancestors.add(parentBucket);
            ancestors.addAll(getAncestors(parentBucket.getBucketId()));
        }
        catch (Exception e)
        {
            Logger.error(e, "Failed to get parent of '%s'", bucket.getName());
        }

        return ancestors;
    }

    public ReportBuilder exportBucketUsers(String bucketId, FileFormat fileFormat, String locale) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        switch (fileFormat)
        {
            case PDF:
                return new BucketUsersPdf(bucket, locale);
            case XLS:
                return new BucketUsersXls(bucket, locale);
            default:
                throw new ApiException("file-format-not-supported");
        }
    }

    public ReportBuilder exportBucketNodes(String bucketId, FileFormat fileFormat, String locale) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        List<MongoDevice> resultset = DeviceManager.getInstance().getDevicesOfBucket(bucketId);
        List<models.transients.DeviceInfo> nodeInfoList = new ArrayList<>();

        if (resultset != null && !resultset.isEmpty())
        {
            for (MongoDevice dbDevice : resultset)
            {
                MongoDeviceModel deviceModel = MongoDeviceModel.getByModelId(dbDevice.getModelId());

                if (deviceModel == null || !deviceModel.isKaiNode())
                {
                    continue;
                }

                models.transients.DeviceInfo deviceInfo = new models.transients.DeviceInfo();
                deviceInfo.address = dbDevice.getAddress();
                deviceInfo.bucket = Long.parseLong(dbDevice.getBucketId());
                deviceInfo.cloudRecordingEnabled = dbDevice.isCloudRecordingEnabled();
                deviceInfo.deviceId = dbDevice.getCoreDeviceId();
                deviceInfo.deviceKey = dbDevice.getDeviceKey();
                deviceInfo.host = dbDevice.getHost();
                deviceInfo.id = Long.parseLong(dbDevice.getDeviceId());
                deviceInfo.login = dbDevice.getLogin();
                deviceInfo.latitude = Util.isNullOrEmpty(dbDevice.getLatitude().toString()) ? "0.0" : dbDevice.getLatitude().toString();
                deviceInfo.longitude = Util.isNullOrEmpty(dbDevice.getLongitude().toString()) ? "0.0" : dbDevice.getLongitude().toString();
                deviceInfo.model.capabilities = deviceModel.getCapabilities();
                deviceInfo.model.channels = deviceModel.getChannels();
                deviceInfo.model.modelId = Long.parseLong(dbDevice.getModelId());
                deviceInfo.model.name = deviceModel.getName();
                deviceInfo.name = dbDevice.getName();
                deviceInfo.password = dbDevice.getPassword();
                deviceInfo.port = dbDevice.getPort();
                deviceInfo.status = dbDevice.getStatus().toString();

                // get list of users
                for (String userId : dbDevice.getUserIds())
                {
                    models.transients.DeviceInfo.UserId u = new models.transients.DeviceInfo.UserId();
                    u.id = Long.parseLong(userId);
                    deviceInfo.users.add(u);
                }

                deviceInfo.node = NodeObject.findByPlatformId(dbDevice.getDeviceId());

                Set<String> assignedLabels = new LinkedHashSet<>();
                if (dbDevice.isKaiNode())
                {
                    for (NodeCamera nodeCam : deviceInfo.node.getCameras())
                    {
                        DeviceChannelPair camera = new DeviceChannelPair(dbDevice.getCoreDeviceId(), nodeCam.nodeCoreDeviceId);
                        List<models.labels.DeviceLabel> labels = LabelManager.getInstance().getLabelsOf(camera);
                        for (models.labels.DeviceLabel label : labels)
                        {
                            assignedLabels.add(label.getLabelName());
                        }
                    }
                }
                deviceInfo.channelLabels = new ArrayList<>(assignedLabels);

                nodeInfoList.add(deviceInfo);
            }
        }

        switch (fileFormat)
        {
            case PDF:
                return new BucketNodesPdf(bucket, nodeInfoList, locale);
            case XLS:
                return new BucketNodesXls(bucket, nodeInfoList, locale);
            default:
                throw new ApiException("file-format-not-supported");
        }
    }

    public void removeExpiredDeletedBuckets(int days)
    {
        Long utcNow = Environment.getInstance().getCurrentUTCTimeMillis();
        Long deadline = utcNow - (days * 24 * 60 * 60 * 1000L);

        List<ArchivedBucket> expiredArchivedBuckets = ArchivedBucket.q().filter("time <", deadline).fetchAll();
        for (ArchivedBucket expiredArchivedBucket : expiredArchivedBuckets)
        {
            try
            {
                MongoBucket bucket = MongoBucket.getById(expiredArchivedBucket.bucketId.toString());
                if (bucket != null)
                {
                    // if bucket is also marked for deletion, proceed to delete Bucket and ArchivedBucket
                    if (bucket.isDeleted())
                    {
                        // delete bucket
                        this.permanentDeleteBucket(bucket.getBucketId());

                        // delete archivedbucket
                        expiredArchivedBucket.delete();
                    }
                    // if bucket is not marked for deletion, we have an inconsistent state here that needs investigation
                    else
                    {
                        Logger.error(Util.whichFn() + " ArchivedBucket entry exists but Bucket is not marked for deletion: " + bucket.getBucketId());
                    }
                }
            }
            catch (Exception e)
            {
                Logger.error(Util.whichFn() + e.getMessage());
            }
        }
    }

    private void bucketParentChanged(String bucketId, String parentBucketId) throws ApiException
    {
        // verify bucket
        MongoBucket bucket = MongoBucket.getById(bucketId);
        if (bucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        MongoBucket parentBucket = MongoBucket.getById(parentBucketId);
        if (parentBucket == null)
        {
            throw new ApiException("bucket-not-found");
        }

        Logger.info("Bucket Parent changed. verifying features");

        // remove features that parent doesn't have
        List<String> extraFeatureNames = ListUtil.getExtraItems(parentBucket.getFeatureNames(), bucket.getFeatureNames());

        if (extraFeatureNames.isEmpty())
        {
            return;
        }

        removeFeatures(bucketId, extraFeatureNames);
    }
}
