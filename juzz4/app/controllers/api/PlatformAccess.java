package controllers.api;

import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.access.AccessKey;
import platform.AccessKeyManager;
import play.mvc.With;

import java.util.Map;

/**
 * @author KAI Square
 * @sectiontitle One Time Pass (OTP)
 * @sectiondesc APIs to manage OTPs and Access Keys for installers
 * @publicapi
 */

@With(APIInterceptor.class)
public class PlatformAccess extends APIController
{

    /**
     * @servtitle Returns generated key list
     * @httpmethod GET
     * @uri /api/{bucket}/getaccesskeylist
     * @responsejson {
     * "result":"ok",
     * "key-list": [
     * {
     * "_created" : 1382695795299,
     * "_id" : "526a43733918d728a6393f3e",
     * "_modified" : 1382695795299,
     * "bucket" : "kaisquare",
     * "bucketID" : 2,
     * "isValid" : false,
     * "key" : "INEG2R",
     * "payload" : "",
     * "ttl" : 1382706595297,
     * "userID" : 2,
     * "userName" : "Admin"
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void getaccesskeylist() throws ApiException
    {
        try
        {
            String callerBucketId = getCallerBucketId();

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("key-list", AccessKeyManager.getInstance().findAccessKeys(callerBucketId, true));
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param user-id       User ID. Mandatory
     * @param ttl           key's valid period in minutes. Mandatory
     * @param max-use-count Number of usages allowed for this OTP (-1 for unlimited). Mandatory
     *
     * @servtitle Generate and returns one time pass key
     * @httpmethod POST
     * @uri /api/{bucket}/generateaccesskey
     * @responsejson {
     * "result":"ok",
     * "key": "INEG2R"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void generateaccesskey() throws ApiException
    {
        try
        {
            String userId = readApiParameter("user-id", true);
            String ttl = readApiParameter("ttl", true);
            String maxUseCount = readApiParameter("max-use-count", true);

            //validate params
            if (!Util.isInteger(maxUseCount))
            {
                throw new ApiException("invalid-max-use-count");
            }
            if (!Util.isLong(ttl))
            {
                throw new ApiException("invalid-ttl");
            }

            AccessKey accessKey = AccessKeyManager.getInstance().generateKey(
                    userId,
                    Long.parseLong(ttl),
                    Integer.parseInt(maxUseCount),
                    AccessKeyManager.DEFAULT_KEY_LENGTH,
                    "");

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("key", accessKey.key);
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param key The key to be deleted. Mandatory
     *
     * @servtitle Removes one time pass
     * @httpmethod POST
     * @uri /api/{bucket}/removeaccesskey
     * @responsejson {
     * "result":"ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */

    public static void removeaccesskey() throws ApiException
    {
        try
        {
            String key = readApiParameter("key", true);

            AccessKey target = AccessKey.find("key", key).first();
            if (target == null)
            {
                throw new ApiException("access-key-not-found");
            }

            target.delete();

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
