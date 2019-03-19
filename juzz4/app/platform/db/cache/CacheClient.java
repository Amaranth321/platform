package platform.db.cache;

import lib.util.Util;
import models.MongoBucket;
import models.MongoDevice;
import models.MongoUser;
import models.access.UserSession;
import models.cloud.UIConfigurableCloudSettings;
import models.events.EventVideo;
import models.labels.LabelStore;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.notification.LabelOccupancySettings;
import platform.db.cache.proxies.*;
import platform.devices.DeviceChannelPair;
import play.Logger;
import play.Play;

/**
 * Notes:
 * <p/>
 * {@link platform.db.cache.PlayCacheClient} uses Gson to serialize/deserialize objects.
 * Remember to implement {@link com.google.gson.JsonDeserializer} if Enum Maps are used in the object.
 * <p/>
 * Call {@link #remove(CachedObject)} if the db object has been updated or deleted
 * <p/>
 * Caching is auto-disabled on nodes, which means it will query the db every time.
 *
 * @author Aye Maung
 * @since v4.4
 */
public class CacheClient
{
    public static final String SINGLETON_CACHE_KEY = "singleton";

    private static final CacheClient INSTANCE = new CacheClient();
    private static boolean memcachedEnabled = false;

    private CacheClient()
    {
        memcachedEnabled = "enabled".equalsIgnoreCase(Play.configuration.getProperty("memcached"));
        Logger.info("CacheClient.memcachedEnabled = %s", memcachedEnabled);
    }

    public static CacheClient getInstance()
    {
        return INSTANCE;
    }

    public void clearAll()
    {
        PlayCacheClient.clearAll();
    }

    public void update(CachedObject updatedCachedObject)
    {
        if (updatedCachedObject == null)
        {
            return;
        }
        PlayCacheClient playClient = new PlayCacheClient<>(updatedCachedObject.getClass());
        playClient.update(updatedCachedObject);
    }

    public void remove(CachedObject cachedObject)
    {
        if (cachedObject == null)
        {
            return;
        }
        PlayCacheClient playClient = new PlayCacheClient<>(cachedObject.getClass());
        playClient.remove(cachedObject);
        Logger.debug(Util.getCallerFn() + "Removed (%s:%s)", cachedObject.getClass(), cachedObject.getCacheKey());
    }

    public CachedDevice getDeviceByPlatformId(String platformDeviceId)
    {
        PlayCacheClient<CachedDevice> playClient = new PlayCacheClient<>(CachedDevice.class);
        String cacheKey = "platform_" + platformDeviceId;
        CachedDevice cache = playClient.get(cacheKey);

        if (cache == null)
        {
            MongoDevice dbDevice = MongoDevice.getByPlatformId(platformDeviceId);
            if (dbDevice == null)
            {
                return null;
            }

            cache = new CachedDevice(cacheKey, dbDevice);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedDevice getDeviceByCoreId(String coreDeviceId)
    {
        PlayCacheClient<CachedDevice> playClient = new PlayCacheClient<>(CachedDevice.class);
        String cacheKey = "core_" + coreDeviceId;
        CachedDevice cache = playClient.get(cacheKey);

        if (cache == null)
        {
            MongoDevice dbDevice = MongoDevice.getByCoreId(coreDeviceId);
            if (dbDevice == null)
            {
                return null;
            }
            cache = new CachedDevice(cacheKey, dbDevice);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedNodeObjectInfo getNodeObject(CachedDevice cachedDevice)
    {
        if (cachedDevice == null || !cachedDevice.isKaiNode())
        {
            return null;
        }

        PlayCacheClient<CachedNodeObjectInfo> playClient = new PlayCacheClient<>(CachedNodeObjectInfo.class);
        String cacheKey = String.valueOf(cachedDevice.getPlatformDeviceId());
        CachedNodeObjectInfo cache = playClient.get(cacheKey);

        if (cache == null)
        {
            NodeObject dbObject = NodeObject.q().filter("cloudPlatformDeviceId", cacheKey).first();
            if (dbObject == null)
            {
                return null;
            }
            cache = new CachedNodeObjectInfo(cacheKey, dbObject);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedUser getUser(String userId)
    {
        PlayCacheClient<CachedUser> playClient = new PlayCacheClient<>(CachedUser.class);
        String cacheKey = String.valueOf(userId);
        CachedUser cache = playClient.get(cacheKey);

        if (cache == null)
        {
            MongoUser dbUser = MongoUser.getById(userId);
            if (dbUser == null)
            {
                return null;
            }
            cache = new CachedUser(cacheKey, dbUser);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedBucket getBucket(String bucketId)
    {
        PlayCacheClient<CachedBucket> playClient = new PlayCacheClient<>(CachedBucket.class);
        String cacheKey = bucketId;
        CachedBucket cache = playClient.get(cacheKey);

        if (cache == null)
        {
            MongoBucket dbBucket = MongoBucket.getById(bucketId);
            if (dbBucket == null)
            {
                return null;
            }
            cache = new CachedBucket(cacheKey, dbBucket);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedNodeCamera getNodeCamera(DeviceChannelPair idPair)
    {
        PlayCacheClient<CachedNodeCamera> playClient = new PlayCacheClient<>(CachedNodeCamera.class);
        String cacheKey = String.format("%s_%s", idPair.getCoreDeviceId(), idPair.getChannelId());
        CachedNodeCamera cache = playClient.get(cacheKey);

        if (cache == null)
        {
            NodeObject node = NodeObject.q().filter("cloudCoreDeviceId", idPair.getCoreDeviceId()).first();
            if (node == null)
            {
                return null;
            }

            NodeCamera nodeCamera = null;
            for (NodeCamera camera : node.getCameras())
            {
                if (camera.nodeCoreDeviceId.equals(idPair.getChannelId()))
                {
                    nodeCamera = camera;
                }
            }
            if (nodeCamera == null)
            {
                return null;
            }

            cache = new CachedNodeCamera(cacheKey, node, nodeCamera);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedEventVideo getEventVideo(String eventId)
    {
        PlayCacheClient<CachedEventVideo> playClient = new PlayCacheClient<>(CachedEventVideo.class);
        String cacheKey = eventId;
        CachedEventVideo cache = playClient.get(cacheKey);

        if (cache == null)
        {
            EventVideo dbVideo = EventVideo.find(eventId);
            if (dbVideo == null)
            {
                return null;
            }
            cache = new CachedEventVideo(cacheKey, dbVideo);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedOccupancySettings getOccupancySettings(String labelId)
    {
        PlayCacheClient<CachedOccupancySettings> playClient = new PlayCacheClient<>(CachedOccupancySettings.class);
        String cacheKey = String.format("%s", labelId);
        CachedOccupancySettings cache = playClient.get(cacheKey);

        if (cache == null)
        {
            LabelOccupancySettings settings = LabelOccupancySettings.findByLabelId(labelId);
            if (settings == null)
            {
                return null;
            }
            cache = new CachedOccupancySettings(cacheKey, settings);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedLoginSession getLoginSession(String sessionKey)
    {
        PlayCacheClient<CachedLoginSession> playClient = new PlayCacheClient<>(CachedLoginSession.class);
        String cacheKey = sessionKey;
        CachedLoginSession cache = playClient.get(cacheKey);

        if (cache == null)
        {
            UserSession dbSession = UserSession.find(sessionKey);
            if (dbSession == null)
            {
                return null;
            }

            MongoBucket bucket = MongoBucket.getById(String.valueOf(dbSession.getBucketId()));
            MongoUser user = MongoUser.getById(String.valueOf(dbSession.getUserId()));
            if (bucket == null || user == null)
            {
                return null;
            }

            cache = new CachedLoginSession(cacheKey, dbSession, bucket, user);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedCloudConfigs getCloudConfigs()
    {
        PlayCacheClient<CachedCloudConfigs> playClient = new PlayCacheClient<>(CachedCloudConfigs.class);
        String cacheKey = SINGLETON_CACHE_KEY;
        CachedCloudConfigs cache = playClient.get(cacheKey);

        if (cache == null)
        {
            cache = UIConfigurableCloudSettings.createCachedObject();
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedOccupancyData getOccupancyData(String labelId)
    {
        PlayCacheClient<CachedOccupancyData> playClient = new PlayCacheClient<>(CachedOccupancyData.class);
        String cacheKey = String.format("%s", labelId);
        CachedOccupancyData cache = playClient.get(cacheKey);
        if (cache == null)
        {
            cache = new CachedOccupancyData(cacheKey, labelId);
            playClient.add(cache);
        }

        return cache.getObject();
    }

    public CachedStoreLabel getStoreLabel(String labelId)
    {
        PlayCacheClient<CachedStoreLabel> playClient = new PlayCacheClient<>(CachedStoreLabel.class);
        String cacheKey = labelId;
        CachedStoreLabel cache = playClient.get(cacheKey);
        if (cache == null)
        {
            LabelStore storeLabel = LabelStore.q().filter("labelId", labelId).first();
            if (storeLabel == null)
            {
                return null;
            }

            cache = new CachedStoreLabel(cacheKey, storeLabel);
            playClient.add(cache);
        }

        return cache.getObject();
    }
}
