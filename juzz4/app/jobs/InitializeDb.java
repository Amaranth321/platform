package jobs;

import com.kaisquare.core.thrift.DeviceDetails;
import core.CoreClient;
import jobs.migration.MigrationPostDbJob;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import models.MongoDevice;
import models.MongoUser;
import models.labels.DeviceLabel;
import models.node.NodeObject;
import models.notification.BucketNotificationSettings;
import models.notification.UserNotificationSettings;
import platform.BucketManager;
import platform.DeviceManager;
import platform.Environment;
import platform.FeatureManager;
import platform.access.DefaultBucket;
import platform.config.readers.AccountDefaultSettings;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.devices.DeviceChannelPair;
import platform.events.EventType;
import platform.kaisyncwrapper.KaiSyncHelper;
import platform.kaisyncwrapper.node.PrioritizedCommandQueue;
import platform.kaisyncwrapper.node.SequencedCommandQueue;
import platform.label.LabelType;
import platform.node.NodeManager;
import platform.notification.NotifyMethod;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InitializeDb extends Job
{
    @Override
    public void doJob()
    {
        try
        {
            initMongoDb();

            //Run post db migrations
            new MigrationPostDbJob().now();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void initMongoDb() throws ApiException
    {
        if (Environment.getInstance().onCloud())
        {
            BucketManager.getInstance().checkDefaultBuckets();
            BucketManager.getInstance().checkOrphanedBuckets();

            KaiSyncHelper.cleanupOrphanedCommands();
            cleanupNodeObjects();
            cleanupLabels();
        }
        else if (Environment.getInstance().onKaiNode())
        {
            //on nodes, make sure there is at least one bucket
            List<MongoBucket> nodeBuckets = MongoBucket.q()
                    .filter("name <>", DefaultBucket.SUPERADMIN.getBucketName())
                    .fetchAll();
            if (nodeBuckets.isEmpty())
            {
                DefaultBucket.KAISQUARE.createIfNotExists();
            }

            NodeManager.getInstance().addDefaultNodeUser();

            SequencedCommandQueue.getInstance().clearAll();
            PrioritizedCommandQueue.getInstance().clearAll();
            KaiSyncHelper.clearAllCommands();
        }

        FeatureManager featureManager = FeatureManager.getInstance();
        //featureManager.checkAndUpdateFeatures(); // already called in MigrationPreDbJob.java
        featureManager.refreshModifiedBucketFeatures();
        featureManager.checkNodeLicenseFeatures();
        featureManager.verifyCommonServicesOnAllUsers();
        removeDanglingCoreDevices();
        cleanupDanglingDeviceUsers();

        syncDefaultNotificationSettings();

        Logger.info("Mongo db initialized");
    }

    private void cleanupLabels()
    {
        for (LabelType labelType : LabelType.values())
        {
            Iterable<DeviceLabel> labels = labelType.getQuery().fetch();
            for (DeviceLabel label : labels)
            {
                for (DeviceChannelPair camera : label.getCameraList())
                {
                    CachedNodeCamera cachedCam = null;
                    try
                    {
                        cachedCam = CacheClient.getInstance().getNodeCamera(camera);
                    }
                    catch (Exception e)
                    {
                        Logger.error(e, "");
                    }
                    if (cachedCam == null)
                    {
                        Logger.error("Camera (%s) no longer exists. Unassigning from (%s)", camera, label);
                        label.unassignCamera(camera);
                        label.save();
                    }
                }
            }
        }
    }

    private void cleanupNodeObjects()
    {
        try
        {
            List<MongoDevice> devices = MongoDevice.q().fetchAll();
            for (MongoDevice device : devices)
            {
                if (!device.isKaiNode())
                {
                    continue;
                }

                Model.MorphiaQuery query = NodeObject.q()
                        .filter("cloudPlatformDeviceId", device.getDeviceId())
                        .order("_created");

                //remove duplicates
                if (query.count() > 1)
                {
                    List<NodeObject> duplicates = query.asList();
                    for (int i = 0; i < duplicates.size(); i++)
                    {
                        if (i == 0)
                        {   //leave the oldest one
                            continue;
                        }

                        NodeObject dup = duplicates.get(i);
                        Logger.info("Removing duplicate NodeObject(%s)", dup.getName());
                        dup.delete();
                    }
                }
            }

            //Remove NodeObjects without the corresponding device
            List<NodeObject> nodeObjects = NodeObject.q().fetchAll();
            for (NodeObject nodeObject : nodeObjects)
            {
                MongoDevice nodeDevice = MongoDevice.getByPlatformId(nodeObject.getNodeId());
                if (nodeDevice == null)
                {
                    Logger.info("Removing orphaned NodeObject (%s)", nodeObject.getName());
                    nodeObject.delete();
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void removeDanglingCoreDevices()
    {
        try
        {
            // anything can dangle in the dev mode
            if (Play.mode.isDev())
            {
                return;
            }

            List<DeviceDetails> coreDeviceList = CoreClient.getInstance().deviceManagementClient.listDevices("all");
            if (coreDeviceList == null)
            {
                Logger.error(Util.whichFn() + "returns null list");
                return;
            }

            Logger.info(Util.whichFn() + "%s devices listed in Core", coreDeviceList.size());
            for (DeviceDetails coreDvc : coreDeviceList)
            {
                MongoDevice platformDevice = MongoDevice.getByCoreId(coreDvc.getId());
                if (platformDevice != null)
                {
                    continue;
                }
                try
                {
                    Logger.info("Deleting a dangling core device (id=%s, name=%s, modelId=%s)",
                                coreDvc.getId(),
                                coreDvc.getName(),
                                coreDvc.getModelId());

                    DeviceDetails deviceDetails = new DeviceDetails();
                    deviceDetails.setId(coreDvc.getId());
                    CoreClient.getInstance().deviceManagementClient.deleteDevice(deviceDetails);
                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                }
            }

            List<MongoDevice> platformList = MongoDevice.findAll();
            Logger.info(Util.whichFn() + "%s devices listed on platform", platformList.size());
            for (MongoDevice platformDvc : platformList)
            {
                boolean exists = false;
                for (DeviceDetails coreDvc : coreDeviceList)
                {
                    if (platformDvc.getCoreDeviceId().equals(coreDvc.getId()))
                    {
                        exists = true;
                        break;
                    }
                }
                if (!exists)
                {
                    Logger.error("Platform device doesn't exist in Core (%s:coreDeviceId=%s)",
                                 platformDvc.getName(),
                                 platformDvc.getCoreDeviceId());

                    //auto-remove it on nodes
                    if (Environment.getInstance().onKaiNode())
                    {
                        Logger.info("Deleting dangling platform device (%s)", platformDvc.getName());
                        DeviceManager.getInstance().removeDeviceFromBucket(platformDvc.getBucketId(),
                                                                           platformDvc.getDeviceId());
                    }
                }
            }

        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    private void syncDefaultNotificationSettings()
    {
        //buckets
        List<BucketNotificationSettings> bktList = BucketNotificationSettings.all().asList();
        Map<EventType, BucketNotificationSettings.EventTypeSettings> bktDefaults =
                AccountDefaultSettings.getInstance().getNotificationSettings();
        for (BucketNotificationSettings bktSetts : bktList)
        {
            bktSetts.syncWithDefaultTypes(bktDefaults);
        }

        //users
        List<UserNotificationSettings> userList = UserNotificationSettings.all().asList();
        Map<EventType, Set<NotifyMethod>> userDefaults = AccountDefaultSettings.getInstance().getNotifyMethods();
        for (UserNotificationSettings userSetts : userList)
        {
            userSetts.syncWithDefaultTypes(userDefaults);
        }
    }

    private void cleanupDanglingDeviceUsers()
    {
        Iterable<MongoDevice> devices = MongoDevice.q().fetch();
        for(MongoDevice device : devices)
        {
            List<String> userIds = device.getUserIds() != null ? device.getUserIds() : new ArrayList<String>();
            List<String> cleanedUserIds = new ArrayList<String>();
            boolean danglingFound = false;

            for (String userId : userIds)
            {
                if (MongoUser.getById(userId) == null)
                {
                    Logger.info("Cleanup dangling device user id (%s)", userId);
                    danglingFound = true;
                }
                else
                {
                    cleanedUserIds.add(userId);
                }
            }

            if (danglingFound)
            {
                device.setUserIds(cleanedUserIds);
                device.save();
            }
        }
    }
}