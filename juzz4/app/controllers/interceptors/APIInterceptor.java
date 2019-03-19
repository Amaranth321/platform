package controllers.interceptors;


import controllers.api.APIController;
import jobs.cloud.LogApiJob;
import lib.util.ResultMap;
import lib.util.minifymod.Compression;
import models.MongoUser;
import platform.Environment;
import platform.access.UserSessionManager;
import platform.config.readers.ConfigsShared;
import platform.db.cache.proxies.CachedLoginSession;
import play.mvc.Before;
import play.mvc.Finally;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;


public class APIInterceptor extends APIController
{
    /**
     * Denied api requests if the server is not ready yet
     */
    @Before(priority = 3)
    public static void readyForRequests()
    {
        if (!Environment.getInstance().isHttpReady())
        {
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "system-under-maintenance");
            renderJSON(map);
        }
    }

    /**
     * This is triggered before rendering and has priority level 1<br/>
     * Validates session-key by checking if session-key is present<br/>
     * then use it otherwise check sessionKey in HTTP Session object<br/>
     * if not present at either place, forbid access.
     * This is not triggered for getannouncementlist api.
     *
     * @param <b>session-key</b> current request session key
     *
     * @see controllers.api.AnnouncementController#getannouncementlist(java.lang.String)
     */
    @Before(priority = 1,
            unless = {
                    "api.AnnouncementController.getannouncementlist",
                    "api.test.pushevent"
            })
    static void validateSession(String bucket)
    {
        try
        {
            String sessionKey = getSessionKey();
            UserSessionManager sessionMgr = UserSessionManager.getInstance();
            if (!sessionMgr.isSessionValid(sessionKey))
            {
                throw new Exception("session-expired");
            }

            CachedLoginSession cachedSession = sessionMgr.findSession(sessionKey);

            //automatically refresh the TTL of a session on every API call
            //except all keep running api: live view, dash board, analytics
            if (!request.actionMethod.equals("recvcometnotification")
                && !request.actionMethod.equals("getuserdevices")
                && !request.actionMethod.equals("listrunninganalytics")
                && !request.actionMethod.equals("getlivevideourl")
                && !request.actionMethod.equals("expirelivevideourl")
                && !request.actionMethod.equals("keepalivelivevideourl"))
            {
                sessionMgr.keepSessionAlive(cachedSession);
            }

            //populate the renderArgs with user information
            renderArgs.put("caller-user-id", cachedSession.getUserId());
            renderArgs.put("username", cachedSession.getUserFullName());
            renderArgs.put("caller-bucket-id", cachedSession.getBucketId());
            renderArgs.put("bucket", cachedSession.getBucketName());

            //set language
            MongoUser user = MongoUser.getById(cachedSession.getUserId());
            Environment.getInstance().setPlayLanguage(user, readBrowserLocale());
        }
        catch (Exception e)
        {
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", e.getMessage());
            renderJSON(map);
        }
    }

    /**
     * This is triggered before rendering and has priority level 2<br/>
     * Checks Access for api for current user<br/>
     * This check is excluded for keepalive api and getannouncementlist api
     * <p/>
     * <b>Note : No input parameters</b>
     *
     * @see controllers.api.Login#keepalive(java.lang.String)
     * @see controllers.api.AnnouncementController#getannouncementlist(java.lang.String)
     */
    @Before(priority = 2,
            unless = {
                    "api.Session.keepalive",
                    "api.AnnouncementController.getannouncementlist",
                    "api.test.pushevent"
            })
    static void isServiceProvisioned(String bucket)
    {
        try
        {
            //TODO: look into this to use url resource name instead of mapped method name
            String serviceName = request.actionMethod;
            String userId = getCallerUserId();
            boolean result = platform.ServiceProvisioning.isServiceProvisioned(userId, serviceName);
            if (result == false)
            {
                throw new Exception("api access denied: " + serviceName);
            }
        }
        catch (Exception e)
        {
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", e.getMessage());
            renderJSON(map);
        }
    }

    @Finally(priority = 1)
    static void audit()
    {
        LogApiJob.queue(new LogApiJob.ApiCall(request, response, renderArgs, params.allSimple()));
    }

    /**
     * This interception method is triggered after rendering is done.<br/> It takes the rendered template from response.out,
     * creates a gzipped stream for response.out  (if supported by the client),<br/> minifies the content
     * and writes the template-string back to response.out
     * <p/>
     * The response of following APIs is not compressed:
     * <ol>
     * <li>geteventvideo</li>
     * <li>getdata</li>
     * </ol>
     * <p/>
     * <b>Note : No input parameters</b>
     *
     * @see controllers.api.VideoProvisioning#geteventvideo(java.lang.String, java.lang.String)
     */
    @Finally(priority = 2, unless = {"api.VideoProvisioning.geteventvideo", "api.Reports.getdata"})
    static void compress() throws IOException
    {
        if (response != null)
        {

            String content = response.out.toString();
            if ("".equals(content))
            {
                return; // fix strange chars on suspended requests
            }

            // minify
            if (response.contentType != null)
            {
                // select compression method by contentType
                if (response.contentType.contains("text/html"))
                {    // could be "text/html; charset=utf-8"
                    content = Compression.compressHTML(content);
                }
                else if (response.contentType.contains("text/xml"))
                {
                    content = Compression.compressXML(content);
                }
                else if (response.contentType.contains("text/css"))
                {
                    content = Compression.compressCSS(content);
                }
                else if (response.contentType.contains("text/javascript"))
                {
                    content = Compression.compressJS(content);
                }
            }

            // gzip only if supported and not excluded
            if (Compression.isGzipSupported(request) && !Compression.isExcludedAction(request))
            {
                final ByteArrayOutputStream gzip = Compression.getGzipStream(content);
                // set response header
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Content-Length", gzip.size() + "");
                response.out = gzip;
            }
            else
            {
                response.out = new ByteArrayOutputStream(content.length());
                response.out.write(content.getBytes());
            }
        }
    }

    @Before
    static void setCORS()
    {

        //allow all to access our API
        response.accessControl("*", null, false);

        //just return headers for the OPTIONS request
        if (request.method.equalsIgnoreCase("OPTIONS"))
        {
            response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            response.setHeader("Access-Control-Expose-Headers", "Set-Cookie");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
            response.setHeader("Access-Control-Max-Age", "1800");
            ok();
        }
    }

    @Before
    public static void log()
    {
        if (ConfigsShared.getInstance().printApiCallLog())
        {
            logApiAndParams();
        }
    }

}
