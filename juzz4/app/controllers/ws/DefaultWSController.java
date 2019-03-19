package controllers.ws;

import com.google.gson.Gson;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import platform.ServiceProvisioning;
import platform.access.UserSessionManager;
import platform.db.cache.proxies.CachedLoginSession;
import play.Logger;
import play.mvc.WebSocketController;

import java.util.Map;

/**
 * DO NOT add any of @Before, @After, @Finally, etc ...
 *
 * @author Aye Maung
 * @since v4.4
 */
class DefaultWSController extends WebSocketController
{
    private static final Gson gson = new Gson();

    protected static void validateSession()
    {
        try
        {
            //read session key
            String sessionKey = params.get("session-key");
            if (Util.isNullOrEmpty(sessionKey))
            {
                if (!session.contains("sessionKey"))
                {
                    throw new Exception("missing-session-key");
                }

                sessionKey = session.get("sessionKey");
            }

            //validate
            UserSessionManager sessionMgr = UserSessionManager.getInstance();
            if (!sessionMgr.isSessionValid(sessionKey))
            {
                throw new Exception("session-expired");
            }

            CachedLoginSession cachedSession = sessionMgr.findSession(sessionKey);

            //check API access
            String serviceName = request.actionMethod;
            String bucketId = cachedSession.getBucketId();
            String userId = cachedSession.getUserId();
            if (!ServiceProvisioning.isServiceProvisioned(userId, serviceName))
            {
                throw new Exception("api access denied: " + serviceName);
            }

            //populate user details
            params.put("caller-bucket-id", bucketId);
            params.put("caller-user-id", userId);
        }
        catch (Exception e)
        {
            Logger.error("[%s] %s", request.actionMethod, e.getMessage());
            disconnect();
        }
    }

    protected static long getCallerBucketId()
    {
        return Long.parseLong(params.get("caller-bucket-id"));
    }

    protected static long getCallerUserId()
    {
        return Long.parseLong(params.get("caller-user-id"));
    }

    protected static void writeToOutbound(Object o)
    {
        if (outbound.isOpen())
        {
            String json = gson.toJson(o);
            if (!Util.isNullOrEmpty(json))
            {
                outbound.send(json);
            }
        }
    }

    /**
     * reads API input parameters
     *
     * @param key      The parameter name
     * @param required true if parameter is mandatory, false if optional (if mandatory parameter
     *                 is missing, ApiException is thrown)
     *
     * @return Value of the input parameter as string
     */
    protected static String readApiParameter(String key, boolean required) throws ApiException
    {
        Map<String, String[]> inputMap = params.all();

        String value = inputMap.get(key) == null ? "" : inputMap.get(key)[0];
        value = value == null ? "" : value.trim();
        if (value.isEmpty() && required)
        {
            throw new ApiException("Missing " + key);
        }

        return value;
    }
}
