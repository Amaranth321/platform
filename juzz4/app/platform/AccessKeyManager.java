package platform;

import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoBucket;
import models.MongoUser;
import models.access.AccessKey;
import models.access.UserSession;
import org.apache.commons.lang.RandomStringUtils;
import platform.access.UserSessionManager;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

import static lib.util.Util.isNullOrEmpty;

public class AccessKeyManager
{
    public static final int DEFAULT_KEY_LENGTH = 6;

    public class KeyAuthResult
    {
        public boolean OK = false;
        public String sessionKey;
        public String payload;

        public KeyAuthResult(boolean ok, String s, String p)
        {
            OK = ok;
            sessionKey = s;
            payload = p;
        }

        public KeyAuthResult()
        {
            OK = false;
            sessionKey = "";
            payload = "";
        }
    }

    public static final Long PERIOD_BEFORE_DELETION = (30 * 24 * 60 * 60 * 1000L);    //30 days
    private static AccessKeyManager instance = null;

    private AccessKeyManager()
    {
    }

    public static AccessKeyManager getInstance()
    {
        if (instance == null)
        {
            instance = new AccessKeyManager();
        }
        return instance;
    }

    /**
     * @param userId      User ID
     * @param ttlMinutes  Key Expiry duration in minutes
     * @param maxUseCount Number of uses allowed (-1 for unlimited)
     * @param keyLength   Length of the generated key
     * @param payload     payload string
     */
    public AccessKey generateKey(String userId, long ttlMinutes, int maxUseCount, int keyLength, String payload) throws ApiException
    {
        MongoUser user = MongoUser.getById(userId);
        if (user == null)
        {
            throw new ApiException("Invalid User ID");
        }

        MongoBucket bucket = MongoBucket.getById(user.getBucketId());
        if (bucket == null)
        {
            throw new ApiException("Invalid Bucket ID");
        }

        AccessKey newKey = new AccessKey();
        newKey.bucketID = Long.parseLong(bucket.getBucketId());
        newKey.bucket = bucket.getName();
        newKey.userID = Long.parseLong(userId);
        newKey.userName = user.getName();
        newKey.ttl = Environment.getInstance().getCurrentUTCTimeMillis() + (ttlMinutes * 60 * 1000);
        newKey.payload = isNullOrEmpty(payload) ? "" : payload;
        newKey.key = RandomStringUtils.randomAlphanumeric(keyLength).toUpperCase();
        newKey.maxUseCount = maxUseCount;
        newKey.save();

        return newKey;
    }

    /**
     * This function authenticates keys
     *
     * @param accessKey Key to authenticate
     */
    public KeyAuthResult authenticateKey(String accessKey, String remoteIP, String userAgent) throws ApiException
    {
        try
        {
            AccessKey keyDb = AccessKey.find("key", accessKey.toUpperCase()).first();
            if (keyDb == null || !keyDb.ok())
            {
                return new KeyAuthResult();
            }

            MongoUser user = MongoUser.getById(keyDb.userID.toString());
            if (user == null)
            {
                Logger.error(Util.whichFn() + "user no longer exists");
                return new KeyAuthResult();
            }

            UserSession session = UserSessionManager.getInstance().createSession(user.getUserId(), remoteIP, userAgent, false);

            keyDb.isValid = false;
            keyDb.currentUseCount++;
            keyDb.save();

            return new KeyAuthResult(true, session.getSessionKey(), keyDb.payload);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return new KeyAuthResult();
        }
    }

    public void updateAccessKeyStatuses() throws ApiException
    {
        try
        {
            Iterable<AccessKey> keyList = AccessKey.all().fetch();
            for (AccessKey key : keyList)
            {
                key.isValid = key.ok();
                key.save();
            }
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
    }

    /**
     * Remove all access keys that have expired for 30 days
     */
    public void removeExpiredKeys()
    {
        try
        {
            updateAccessKeyStatuses();
            Long currentTime = Environment.getInstance().getCurrentUTCTimeMillis();
            Iterable<AccessKey> keyList = AccessKey.find("isValid", false).fetch();
            for (AccessKey key : keyList)
            {
                if ((key.ttl + PERIOD_BEFORE_DELETION) < currentTime)
                {
                    key.delete();
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }
    }

    /**
     * Returns all access keys of the specified bucket
     *
     * @param bucketId
     * @param includeChildBucketKeys
     */
    public List<AccessKey> findAccessKeys(String bucketId, boolean includeChildBucketKeys) throws ApiException
    {
        updateAccessKeyStatuses();

        //compile bucket id list
        List<Long> bucketIds = new ArrayList<>();
        bucketIds.add(Long.parseLong(bucketId));

        if (includeChildBucketKeys)
        {
            List<MongoBucket> childBuckets = BucketManager.getInstance().getDescendants(bucketId);
            for (MongoBucket bkt : childBuckets)
            {
                // ignore deleted buckets
                if (bkt.isDeleted())
                {
                    continue;
                }

                bucketIds.add(Long.parseLong(bkt.getBucketId()));
            }
        }
        return AccessKey.q().filter("bucketID in", bucketIds).fetchAll();
    }
}
