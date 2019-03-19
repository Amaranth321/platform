package controllers.api;

import controllers.interceptors.LoginInterceptor;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoUser;
import models.access.UserSession;
import models.transportobjects.UserSessionTransport;
import platform.AccessKeyManager;
import platform.Environment;
import platform.UserProvisioning;
import platform.access.UserSessionManager;
import platform.db.cache.proxies.CachedLoginSession;
import play.Logger;
import play.mvc.Http;
import play.mvc.With;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KAI Square
 * @sectiontitle User Authentication
 * @sectiondesc APIs for user authentication operations like login, reset password etc.
 * @publicapi
 */
@With(LoginInterceptor.class)
public class Login extends APIController
{
    /**
     * @param user-name   The user name e.g. demo
     * @param password    The password e.g. demo
     * @param remember-me Remember login user e.g. true/false
     *
     * @servtitle Login with username and password
     * @httpmethod POST
     * @uri /api/{bucket}/login
     * @responsejson {
     * "result": "ok",
     * "session-key": "the session key",
     * "user-id": "123",
     * "expiry": "session expires at this time - number of milliseconds since January 1, 1970, 00:00:00 GMT"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "msg-incorrect-login",
     * "locked-time-remaining": "number of login attempts remaining before account gets locked"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "msg-account-locked",
     * "login-attempt-remaining": "number of milliseconds remaining for account to be unlocked"
     * }
     * @responsejson {
     * "result": "error"
     * }
     */
    public static void login(String bucket) throws ApiException
    {
        try
        {
            String username = readApiParameter("user-name", true);
            String password = readApiParameter("password", true);
            String rememberMe = readApiParameter("remember-me", false);
            String clientIp = request.remoteAddress;

            //validate
            boolean bRememberMe = false;
            if (!Util.isNullOrEmpty(rememberMe))
            {
                if (!Util.isBoolean(rememberMe))
                {
                    throw new ApiException("invalid-remember-me");
                }
                bRememberMe = Boolean.parseBoolean(rememberMe);
            }

            //check if ip is locked
            Map lockedInfo = UserProvisioning.checkLockedHistory(clientIp);
            if (!lockedInfo.isEmpty())
            {
                Map responseMap = new LinkedHashMap();
                responseMap.put("result", "error");
                responseMap.putAll(lockedInfo);
                renderJSON(responseMap);
            }

            //try login
            String remoteIP = request.remoteAddress;
            String userAgent = getUserAgent();
            UserSession userSession = platform.UserProvisioning.login(
                    bucket,
                    username,
                    password,
                    remoteIP,
                    userAgent,
                    bRememberMe
            );

            //Failed
            if (userSession == null)
            {
                Map responseMap = new LinkedHashMap();

                //Never lock node
                if (Environment.getInstance().onKaiNode())
                {
                    responseMap.put("result", "error");
                    responseMap.put("reason", "msg-node-incorrect-login");
                }
                else
                {
                    Map attemptInfo = UserProvisioning.incrementLoginAttempt(clientIp);
                    responseMap.put("result", "error");
                    responseMap.putAll(attemptInfo);
                }

                renderJSON(responseMap);
            }

            //success
            UserProvisioning.clearLoginAttempts(clientIp);
            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("session-key", userSession.getSessionKey());
            responseMap.put("user-id", userSession.getUserId());
            responseMap.put("expiry", userSession.getExpiry());

            //cookie
            session.put("sessionKey", userSession.getSessionKey());

            renderJSON(responseMap);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param otp The one time pass
     *
     * @servtitle Login with a One Time Pass (OTP)
     * @httpmethod POST
     * @uri /api/otplogin
     * @responsejson {
     * "result": "ok",
     * "session-key": "the session key",
     * "payload": "4jus4r6ki5",
     * "user-id": "123",
     * "expiry": "14879657570"
     * }
     * @responsejson {
     * "result": "error"
     * }
     */
    public static void otplogin() throws ApiException
    {
        try
        {
            String otp = readApiParameter("otp", true);
            String remoteIP = request.remoteAddress;
            String userAgent = getUserAgent();
            AccessKeyManager.KeyAuthResult authResult = AccessKeyManager.getInstance()
                    .authenticateKey(otp, remoteIP, userAgent);

            if (!authResult.OK)
            {
                throw new ApiException("otp-auth-failed");
            }

            CachedLoginSession cachedSession = UserSessionManager.getInstance().findSession(authResult.sessionKey);

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("session-key", authResult.sessionKey);
            map.put("payload", authResult.payload);
            map.put("user-id", cachedSession.getUserId());
            map.put("expiry", cachedSession.getExpiry());

            //insert the session key in Session Cookie
            session.put("sessionKey", authResult.sessionKey);
            response.setHeader("Access-Control-Allow-Headers", "*");

            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param session-key The active session key
     *
     * @servtitle Call this API periodically to keep the session alive (e.g. once every minute)
     * @httpmethod POST
     * @uri /api/{bucket}/keepalive
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error"
     * }
     */
    public static void keepalive() throws ApiException
    {
        try
        {
            String sessionKey = getSessionKey();
            CachedLoginSession cachedSession = UserSessionManager.getInstance().findSession(sessionKey);
            cachedSession = UserSessionManager.getInstance().keepSessionAlive(cachedSession);
            respondOK("expiry", cachedSession.getExpiry());
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param session-key The active session key
     *
     * @servtitle Get the active session information
     * @httpmethod POST
     * @uri /api/{bucket}/getsessioninfo
     * @responsejson {
     * "result": "ok",
     * "session": {
           "sessionKey": "047b930f-ff13-4764-bb0d-17d9dc68e303",
           "expiry": 1488968973950
     * }
     * }
     * @responsejson {
     * "result": "error"
     * }
     */
    public static void getsessioninfo()
    {
        try
        {
            String sessionKey = readApiParameter("session-key", true);
            CachedLoginSession session = UserSessionManager.getInstance().findSession(sessionKey);
            respondOK("session", new UserSessionTransport(session));
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param session-key The active session key
     *
     * @servtitle Invalidate the active session and log out
     * @httpmethod POST
     * @uri /api/{bucket}/logout
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error"
     * }
     */
    public static void logout() throws ApiException
    {
        try
        {
            String sessionKey = getSessionKey();
            UserSessionManager.getInstance().destroy(sessionKey);
            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param email     Email address of the user
     * @param user-name The user name (optional if email address is provided)
     * @param bucket    The bucket name (optional if email address is provided)
     *
     * @servtitle Request password reset link via email.
     * @httpmethod POST
     * @uri /api/forgotpassword
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "invalid-email"
     * }
     */
    public static void forgotpassword() throws ApiException
    {
        try
        {
            String username = readApiParameter("user-name", false);
            String email = readApiParameter("email", false);
            String bucket = readApiParameter("bucket", false);
            if (Util.isNullOrEmpty(username) && Util.isNullOrEmpty(email) && Util.isNullOrEmpty(bucket))
            {
                throw new ApiException("CompanyID,Username OR Email  required.");
            }

            List<MongoUser> users = UserProvisioning.verifyForgotPasswordInfo(bucket, username, email);

            if (users.size() == 0)
            {
                throw new ApiException("Information could not be verified.");
            }
            else
            {

                Template emailTmpl = TemplateLoader.load(template("kaisquare/common/templates/reset_password_email.html"));
                String baseUrl = null;
                if (!Util.isNullOrEmpty(request.host))
                {
                    if (request.secure)
                    {
                        baseUrl = "https://" + request.host;
                    }
                    else
                    {
                        baseUrl = "http://" + request.host;
                    }
                }
                for (MongoUser user : users)
                {
                    UserProvisioning.sendPasswordResetEmail(emailTmpl, user.getUserId(), baseUrl);
                }
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
     * @param key The reset key
     *
     * @servtitle Verify password reset key and load the password reset page in browser.
     * @httpmethod GET
     * @uri /api/verifyresetkey/{key}
     * @responsehtml <html>The password reset page</html>
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void verifyresetkey(String key) throws ApiException
    {
        /*******
         * Note: This function is to be called with a GET request with bucketid, userid and resetkey as URL parameters.
         */
        try
        {
            setDefaultContentPaths();

            String HtmlPath = "kaisquare";
            if (platform.UserProvisioning.verifyResetKey(key))
            {
                // key verified, redirect to the new password input page
                renderTemplate("kaisquare/common/reset_pass_with_key.html", key, HtmlPath);
            }
            else
            {
                // key not verified, render login page
                Logger.warn("Password reset key cannot be verified: %s", key);
                renderTemplate("kaisquare/common/reset_pass_with_key.html", HtmlPath);
            }

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param key      The password reset key
     * @param password The new password
     *
     * @servtitle Change password with a password reset key
     * @httpmethod POST
     * @uri /api/resetpasswordwithkey
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void resetpasswordwithkey() throws ApiException
    {
        try
        {
            String resetKey = readApiParameter("key", true);
            String newPassword = readApiParameter("password", true);

            Map map = new ResultMap();
            if (platform.UserProvisioning.verifyResetKey(resetKey))
            {
                if (platform.UserProvisioning.resetPasswordWithKey(resetKey, newPassword))
                {
                    map.put("result", "ok");
                    renderJSON(map);
                }
            }
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
