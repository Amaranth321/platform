package jobs.migration;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.kaisquare.kaisync.platform.DeviceType;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.Analytics.NodeVcaInstance;
import models.*;
import models.RemoteShellState.ConnectionState;
import models.access.AccessKey;
import models.backwardcompatibility.*;
import models.labels.LabelOthers;
import models.licensing.LicenseStatus;
import models.licensing.NodeLicense;
import models.node.NodeCamera;
import models.node.NodeObject;
import platform.*;
import platform.access.DefaultBucket;
import platform.access.DefaultRole;
import platform.access.DefaultUser;
import platform.analytics.IVcaInstance;
import platform.analytics.VcaManager;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.devices.DeviceChannelPair;
import platform.label.LabelManager;
import platform.label.LabelType;
import platform.node.NodeManager;
import platform.nodesoftware.SoftwareManager;
import play.Logger;
import play.jobs.Job;
import play.libs.F;
import play.modules.morphia.MorphiaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Migrations based on versions
 * <p/>
 * Always check major and minor release numbers under VersionManager
 * The last 'if' statement must match these two numbers. Add a new one, otherwise
 *
 * @author Aye Maung
 */
public class MigrationPreDbJob extends Job
{
    @Override
    public void doJob()
    {
        try
        {
            VersionManager verMgr = VersionManager.getInstance();
            double currentVersion = verMgr.getPlatformVersion();
            double releaseVersion = verMgr.getLatestPlatformVersion();

            //Downgrade is not supported
            if (currentVersion > releaseVersion)
            {
                verMgr.setPlatformVersion(releaseVersion);
            }

            //version independent
            migrateV44VcaInstances();
            migrateSoftwareUpdateFiles();
            migrateOldDeviceLabels();
            migrateV44RemoteShellState();

            // initialize latest features and services - must put before mongo migration
            FeatureManager.getInstance().checkAndUpdateFeatures();

            // only migrate for 4.0 < version < 4.6
            if (currentVersion < 4.6 && currentVersion != VersionManager.BEFORE_VERSIONING)
            {
                migrateMySqlToMongoDb();
            }

            migrateCurrentAnalytics();
            migrateCameraLimitToNodeLicense();
            dropUnusedCollections();
            normalizeNameCase();

            if (currentVersion < 4.5)
            {
                resetNodeOneMaxVcaCount();
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            Environment.getInstance().stopServer("MigrationPreDbJob failed. Stopping");
        }
        finally
        {
            Logger.info("MigrationPreDbJob completed");
        }
    }

    private void migrateCurrentAnalytics()
    {
        VcaManager vcaMgr = VcaManager.getInstance();
        List<IVcaInstance> vcaList = vcaMgr.listVcaInstances(null);

        double referenceVersion = 0.0;
        for (IVcaInstance inst : vcaList)
        {
            try
            {
                //node instances on cloud
                if (inst instanceof NodeVcaInstance)
                {
                    NodeVcaInstance nodeInst = (NodeVcaInstance) inst;
                    NodeObject nodeObject = nodeInst.getNodeObject();
                    if (nodeObject == null)
                    {
                        nodeInst.delete();
                        continue;
                    }

                    referenceVersion = nodeObject.getReleaseNumber();
                    inst = vcaMgr.checkAndMigrate(referenceVersion, nodeInst);
                    if (inst.migrationRequired())
                    {
                        nodeInst.save();
                    }
                }
            }
            catch (Exception e)
            {
                String errMsg = String.format("(v%s:%s) - %s", referenceVersion, inst.getVcaInfo(), e.getMessage());
                Logger.error(e, "Error migrating vca " + errMsg);
            }
        }
    }

    private void migrateSoftwareUpdateFiles()
    {
        List<SoftwareUpdate> oldFiles = SoftwareUpdate.q().asList();
        for (SoftwareUpdate oldFile : oldFiles)
        {
            try
            {
                long modelId = SoftwareManager.NODE_UBUNTU_MODEL_ID;
                if (oldFile.name.equalsIgnoreCase(DeviceType.NodeOne.toString()))
                {
                    modelId = SoftwareManager.NODE_ONE_AMEGIA_MODEL_ID;
                }

                SoftwareUpdateFile newFile = new SoftwareUpdateFile(oldFile.fileName,
                                                                    modelId,
                                                                    oldFile.version,
                                                                    oldFile.host,
                                                                    oldFile.port,
                                                                    oldFile.size,
                                                                    oldFile.uploadedDate);
                newFile.save();
                oldFile.delete();

                Logger.info("Migrated software update file (%s:%s:%s => %s)",
                            oldFile.version, oldFile.fileName, oldFile.name, modelId);
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
    }

    private void migrateV44VcaInstances()
    {
        if (Environment.getInstance().onCloud())
        {
            List<NodeObject> nodeObjectList = NodeObject.q().asList();
            for (NodeObject nodeObject : nodeObjectList)
            {
                if (Util.isNullOrEmpty(nodeObject.analytics))
                {
                    continue;
                }

                Logger.info("[%s] Migrating NodeObject.analytics (%s)",
                            nodeObject.getName(),
                            nodeObject.analytics.size());
                List<VcaInstance> successList = new ArrayList<>();
                for (VcaInstance oldInst : nodeObject.analytics)
                {
                    if (NodeVcaInstance.migrate(oldInst))
                    {
                        successList.add(oldInst);
                    }
                }

                nodeObject.analytics.removeAll(successList);
                nodeObject.save();
            }
        }
        else if (Environment.getInstance().onKaiNode())
        {
            boolean vcaMigrationDone = false;
            long retryDelayMillis = 10000;
            while (!vcaMigrationDone)
            {
                try
                {
                    F.Promise<Boolean> promise = new Migrate44NodeVcaJob().now();
                    vcaMigrationDone = promise.get();
                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                }
                finally
                {
                    if (!vcaMigrationDone)
                    {
                        try
                        {
                            Logger.error("v4.4 vca instance migration to retry after %s milliseconds",
                                         retryDelayMillis);
                            Thread.sleep(retryDelayMillis);
                        }
                        catch (InterruptedException e)
                        {
                        }
                    }
                }
            }
        }
    }

    private void migrateOldDeviceLabels()
    {
        if (!Environment.getInstance().onCloud())
        {
            return;
        }

        CacheClient cacheClient = CacheClient.getInstance();
        List<NodeObject> allNodeObjects = NodeObject.q().fetchAll();
        for (NodeObject nodeObject : allNodeObjects)
        {
            CachedDevice nodeDevice = cacheClient.getDeviceByCoreId(nodeObject.getNodeCoreDeviceId());
            for (NodeCamera nodeCamera : nodeObject.getCameras())
            {
                for (String oldLabel : nodeCamera.labels)
                {
                    try
                    {
                        models.labels.DeviceLabel existingLabel = LabelManager.getInstance()
                                .findLabel(Long.parseLong(nodeDevice.getBucketId()), oldLabel);
                        if (existingLabel != null && existingLabel.getType() != LabelType.OTHERS)
                        {
                            Logger.info("[Migration] same label name (%s:%s) exists. Ignored",
                                        existingLabel.getType(),
                                        oldLabel);
                            continue;
                        }

                        //save old labels as labels of OTHER type
                        LabelOthers migratedLabel = LabelOthers.createNew(Long.parseLong(nodeDevice.getBucketId()),
                                                                          oldLabel);
                        migratedLabel.save();
                        Logger.info("[Migration] created '%s' label (bucketId=%s)", oldLabel, nodeDevice.getBucketId());

                        //restore assignments
                        migratedLabel.assignCamera(new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(),
                                                                         nodeCamera.nodeCoreDeviceId));
                        migratedLabel.save();
                        Logger.info("[Migration] re-assigned '%s' to (%s - %s)",
                                    oldLabel,
                                    nodeObject.getName(),
                                    nodeCamera.name);
                    }
                    catch (Exception e)
                    {
                        Logger.error(e,
                                     "Failed to migrate %s > %s > %s",
                                     nodeObject.getName(),
                                     nodeCamera.name,
                                     oldLabel);
                    }
                }

                //clear all
                nodeCamera.labels = null;
                nodeObject.save();
            }
        }

        //clear unassigned
        DeviceLabel.q().delete();
    }

    private void migrateCameraLimitToNodeLicense()
    {
        if (Environment.getInstance().onCloud())
        {
            CloudLicenseManager licenseMgr = CloudLicenseManager.getInstance();
            Iterable<NodeObject> allNodeObjects = NodeObject.q().fetch();

            for (NodeObject nodeObject : allNodeObjects)
            {
                MongoDevice dbDevice = nodeObject.getDbDevice();
                NodeLicense nodeLicense = licenseMgr.getLicenseByNode(dbDevice.getDeviceId());

                //previous licenses' maxCameraLimit are 0
                if (nodeLicense.maxCameraLimit == 0)
                {
                    MongoDeviceModel mongoDeviceModel = MongoDeviceModel.getByModelId(dbDevice.getModelId());
                    nodeLicense.maxCameraLimit = mongoDeviceModel.getChannels();
                    nodeLicense.save();
                    Logger.info("[Migration] camera limit into node license: 'Node %s (%s), Camera limit = %s'",
                                dbDevice.getName(),
                                dbDevice.getDeviceId(),
                                nodeLicense.maxCameraLimit);
                }
            }
        }
        else
        {
            try
            {
                if (!NodeManager.getInstance().isRegisteredOnCloud())
                {
                    return;
                }

                NodeLicense nodeLicense = NodeManager.getInstance().getLicense();
                DeviceModel deviceModel = NodeManager.getInstance().getNodeInfo().getDeviceModel();

                //previous licenses' maxCameraLimit are 0
                if (nodeLicense.maxCameraLimit == 0)
                {
                    nodeLicense.maxCameraLimit = deviceModel.channels;
                    NodeManager.getInstance().setLicense(nodeLicense);
                }
            }
            catch (Exception e)
            {
                Logger.error(e, "Failed to migrate camera limit into node license!");
            }
        }
    }

    private void migrateV44RemoteShellState()
    {
        //Update Running Remote Shell
        Datastore ds = RemoteShellState.ds();
        Query<RemoteShellState> query = ds.createQuery(RemoteShellState.class);
        query.and(query.criteria("connectionState").doesNotExist())
                .and(query.criteria("open").equal(true));
        UpdateOperations<RemoteShellState> ops = ds.createUpdateOperations(RemoteShellState.class);
        ops.set("connectionState", ConnectionState.NODE_CONNECTED);
        ds.findAndModify(query, ops, false, false);

        //Update Stopped Remote Shell
        ds = RemoteShellState.ds();
        query = ds.createQuery(RemoteShellState.class);
        query.and(query.criteria("connectionState").doesNotExist())
                .and(query.criteria("open").equal(false));
        UpdateOperations<RemoteShellState> ops2 = ds.createUpdateOperations(RemoteShellState.class);
        ops2.set("connectionState", ConnectionState.NODE_DISCONNECTED);
        ds.findAndModify(query, ops2, false, false);
    }

    private void migrateMySqlToMongoDb() throws ApiException
    {
        Long startTime = System.currentTimeMillis();

        // migrate buckets
        List<Bucket> sqlBuckets = Bucket.findAll();
        for (Bucket sqlBucket : sqlBuckets)
        {
            String bucketId = sqlBucket.getId().toString();
            if (MongoBucket.getByName(sqlBucket.name.toLowerCase()) != null)
            {
                continue;
            }

            MongoBucket mongoBucket = MongoBucket.migrateFromSqlModel(sqlBucket);
            mongoBucket.save();
        }

        // migrate inventory items
        List<InventoryItem> sqlInventoryItems = InventoryItem.findAll();

        for (InventoryItem sqlInventoryItem : sqlInventoryItems)
        {
            String inventoryItemId = sqlInventoryItem.getId().toString();
            if (MongoInventoryItem.getById(inventoryItemId) != null)
            {
                continue;
            }

            MongoInventoryItem mongoInventoryItem = new MongoInventoryItem();
            mongoInventoryItem.setInventoryItemId(inventoryItemId);
            mongoInventoryItem.setActivated(sqlInventoryItem.activated);
            mongoInventoryItem.setDeviceModelId(sqlInventoryItem.modelNumber);
            mongoInventoryItem.setMacAddress(sqlInventoryItem.macAddress.trim().toLowerCase());
            mongoInventoryItem.setRegistrationNumber(sqlInventoryItem.registrationNumber);

            mongoInventoryItem.save();
        }

        // migrate roles
        MongoBucket superadminBucket = MongoBucket.getByName(DefaultBucket.SUPERADMIN.getBucketName());
        List<Role> sqlRoles = Role.findAll();

        for (Role sqlRole : sqlRoles)
        {
            if (Environment.getInstance().onCloud() &&
                sqlRole.name.equals(DefaultRole.NODE_USER.getRoleName()))
            {
                Logger.info("[Migration] node user role ignored on cloud");
                continue;
            }

            String roleId = sqlRole.getId().toString();
            if (MongoRole.getById(roleId) != null)
            {
                continue;
            }

            String bucketId = sqlRole.bucketId > 0L
                              ? sqlRole.bucketId + ""
                              : superadminBucket.getBucketId(); // for versions < 4.4 role could have empty bucketId

            MongoRole mongoRole = new MongoRole();
            mongoRole.setRoleId(roleId);
            mongoRole.setBucketId(bucketId);
            mongoRole.setDescription(sqlRole.description);
            mongoRole.setName(sqlRole.name);

            for (Feature sqlFeature : sqlRole.features)
            {
                MongoFeature mongoFeature = MongoFeature.getByName(sqlFeature.name);
                if (mongoFeature != null)
                {
                    mongoRole.addFeatureName(sqlFeature.name);
                }
            }

            mongoRole.save();
        }

        // migrate users
        List<User> sqlUsers = User.findAll();

        for (User sqlUser : sqlUsers)
        {
            if (Environment.getInstance().onCloud() &&
                sqlUser.login.equals(DefaultUser.NODE_USER.getUsername()))
            {
                Logger.info("[Migration] node user ignored on cloud");
                continue;
            }

            String userId = sqlUser.getId().toString();
            if (MongoUser.getById(userId) != null)
            {
                continue;
            }

            MongoUser mongoUser = new MongoUser();
            mongoUser.setUserId(userId);
            mongoUser.setBucketId(sqlUser.bucketId.toString());
            mongoUser.setActivated(sqlUser.activated);
            mongoUser.setCreationTimestamp(sqlUser.creationTimestamp);
            mongoUser.setEmail(sqlUser.email);
            mongoUser.setLanguage(sqlUser.language);
            mongoUser.setLogin(sqlUser.login.toLowerCase());
            mongoUser.setName(sqlUser.name);
            mongoUser.setPassword(sqlUser.password);
            mongoUser.setPhone(sqlUser.phone);

            for (Role role : sqlUser.roles)
            {
                MongoRole mRole = MongoRole.getById(role.getId().toString());
                if (mRole != null)
                {
                    mongoUser.addRoleId(role.id.toString());
                }
            }
            mongoUser.save();

            // NOTE: instead of copying user's services manually, call RoleManager.UpdateUserRoles() to get latest list of services
            // in case older features do not have latest apis
            RoleManager.getInstance().updateUserRoles(mongoUser.getUserId(), mongoUser.getRoleIds());
        }

        // migrate userpref
        List<UserPref> sqlUserPrefs = UserPref.findAll();

        for (UserPref sqlUserPref : sqlUserPrefs)
        {
            String userId = sqlUserPref.user.id.toString();
            if (MongoUserPreference.getByUserId(userId) != null)
            {
                continue;
            }

            MongoUserPreference mongoUserPreference = new MongoUserPreference();
            mongoUserPreference.setUserId(userId);
            mongoUserPreference.setApnsDeviceToken(sqlUserPref.APNSDeviceToken);
            mongoUserPreference.setAutoRotation(sqlUserPref.autoRotation != null && sqlUserPref.autoRotation);
            mongoUserPreference.setAutoRotationTime(sqlUserPref.autoRotationTime);
            mongoUserPreference.setDuration(sqlUserPref.duration);
            mongoUserPreference.setEmailNotificationEnabled(sqlUserPref.emailNotificationEnabled);
            mongoUserPreference.setGcmDeviceToken(sqlUserPref.GCMDeviceToken);
            mongoUserPreference.setNumberOfViews(sqlUserPref.numberOfViews);
            mongoUserPreference.setPosFakeDataEnabled(sqlUserPref.POSFakeDataEnabled != null && sqlUserPref.POSFakeDataEnabled);
            mongoUserPreference.setPushNotificationEnabled(sqlUserPref.pushNotificationEnabled);
            mongoUserPreference.setSlotSettingAssignments(sqlUserPref.slotSettingAssignments);
            mongoUserPreference.setSmsNotificationEnabled(sqlUserPref.smsNotificationEnabled);
            mongoUserPreference.setTheme(sqlUserPref.theme);

            mongoUserPreference.save();
        }

        // migrate device models
        List<DeviceModel> sqlDeviceModels = DeviceModel.findAll();

        for (DeviceModel sqlDeviceModel : sqlDeviceModels)
        {
            String modelId = sqlDeviceModel.getId().toString();
            if (MongoDeviceModel.getByModelId(modelId) != null)
            {
                continue;
            }

            MongoDeviceModel mongoDeviceModel = new MongoDeviceModel();
            mongoDeviceModel.setModelId(modelId);
            mongoDeviceModel.setName(sqlDeviceModel.name);
            mongoDeviceModel.setCapabilities(sqlDeviceModel.capabilities);
            mongoDeviceModel.setChannels(sqlDeviceModel.channels);
            mongoDeviceModel.setLiveview(sqlDeviceModel.liveview);
            mongoDeviceModel.setMisc(sqlDeviceModel.misc);

            mongoDeviceModel.save();
        }

        // migrate devices
        List<Device> sqlDevices = Device.findAll();

        for (Device sqlDevice : sqlDevices)
        {
            String platformDeviceId = sqlDevice.getId().toString();
            if (MongoDevice.getByPlatformId(platformDeviceId) != null)
            {
                continue;
            }

            MongoDevice mongoDevice = new MongoDevice();
            mongoDevice.setDeviceId(platformDeviceId);
            mongoDevice.setCoreDeviceId(sqlDevice.deviceId);
            mongoDevice.setAddress(sqlDevice.address);
            mongoDevice.setBucketId(sqlDevice.bucket.getId().toString());
            mongoDevice.setCloudRecordingEnabled(sqlDevice.cloudRecordingEnabled);
            mongoDevice.setDeviceKey(sqlDevice.deviceKey.toLowerCase());
            mongoDevice.setHost(sqlDevice.host);
            mongoDevice.setLastCheckedTime(sqlDevice.lastCheckedTime);
            mongoDevice.setLatitude(Double.parseDouble(sqlDevice.latitude));
            mongoDevice.setLogin(sqlDevice.login);
            mongoDevice.setLongitude(Double.parseDouble(sqlDevice.longitude));
            mongoDevice.setModelId(sqlDevice.model.getId().toString());
            mongoDevice.setName(sqlDevice.name);
            mongoDevice.setPassword(sqlDevice.password);
            mongoDevice.setPort(sqlDevice.port);
            mongoDevice.setStatus(sqlDevice.getStatus());

            for (User sqlUser : sqlDevice.users)
            {
                mongoDevice.addUserId(sqlUser.getId().toString());
            }

            mongoDevice.save();
        }

        Long endTime = System.currentTimeMillis();
        Long totalTime = endTime - startTime;
        Logger.info("MigrationPreDbJob.migrateMySqlToMongoDb() finished in " + totalTime + " ms");
    }

    private void dropUnusedCollections()
    {
        Logger.info("Dropping deprecated mongo collections");
        List<String> collectionsToDrop = new ArrayList<>(Arrays.asList(
                "NodeStatistics",         // deprecated since 4.5
                "PeriodicReport",         // deprecated since 4.6
                "PeriodicReportFiles",    // deprecated since 4.4
                "PeriodicReportPrefs",    // deprecated since 4.4
                "PeriodicReportSettings", // deprecated since 4.6
                "PERIODIC_REPORTS.files", // deprecated since 4.6
                "PERIODIC_REPORTS.chunks" // deprecated since 4.6
        ));

        for (String collectionName : collectionsToDrop)
        {
            MorphiaPlugin.ds().getDB().getCollection(collectionName).drop();
        }
    }

    private void normalizeNameCase()
    {
        List<AccessKey> accessKeys = AccessKey.q().fetchAll();
        for (AccessKey accessKey : accessKeys)
        {
            accessKey.bucket = accessKey.bucket.toLowerCase();
            accessKey.save();
        }
    }

    private void resetNodeOneMaxVcaCount()
    {
        if (!Environment.getInstance().onCloud())
        {
            return;
        }

        List<NodeLicense> licensesInUse = NodeLicense.q().filter("status !=", LicenseStatus.UNUSED).fetchAll();
        for (NodeLicense license : licensesInUse)
        {
            try
            {
                MongoInventoryItem invItem = MongoInventoryItem.getByRegistrationNumber(license.registrationNumber);
                if (invItem == null)
                {
                    Logger.error("[Migration] inventory not found (%s:%s)",
                                 license.licenseNumber,
                                 license.registrationNumber);
                    continue;
                }

                if (!invItem.isNodeOne())
                {
                    continue;
                }

                if (license.maxVcaCount > 1)
                {
                    license.maxVcaCount = 1;
                    license.save();
                    Logger.info("[Migration] Node one license (%s) reset to '1'", license.licenseNumber);
                }
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }
    }
}
