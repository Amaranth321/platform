package controllers.interceptors;

import controllers.DefaultController;
import jobs.node.NetworkCheck;
import lib.util.Util;
import lib.util.minifymod.Compression;
import models.BucketPasswordPolicy;
import models.MongoBucket;
import models.MongoUser;
import platform.Environment;
import platform.LocationManager;
import platform.VersionManager;
import platform.access.UserSessionManager;
import platform.config.readers.ConfigsShared;
import platform.db.cache.proxies.CachedLoginSession;
import play.Logger;
import play.Play;
import play.mvc.Before;
import play.mvc.Finally;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WebInterceptor extends DefaultController
{
    /**
     * Redirect Http to Https
     */
    @Before(priority = 1)
    public static void redirectToHttps()
    {
        if (!ConfigsShared.getInstance().forceHttps())
        {
            return;
        }

        if (!request.secure)
        {
            String httpsUrl = String.format("https://%s:%s",
                                            request.host.split(":")[0],
                                            Play.configuration.get("https.port"));

            redirect(httpsUrl + request.url);
        }
    }

    /**
     * Validates the {bucket} part of the URL. If not a valid bucket, redirects
     * to a default page.
     *
     * @param bucket
     */
    @Before(priority = 2)
    static void validateRequest(String bucket)
    {
        try
        {
            bucket = bucket.trim();
            if (Util.isNullOrEmpty(bucket))
            {
                redirectToLogin();
            }

            // Check if the session is still valid
            String sessionKey = session.get("sessionKey");
            if (Util.isNullOrEmpty(sessionKey))
            {
                redirectToLogin();
            }

            UserSessionManager sessionMgr = UserSessionManager.getInstance();
            if (!sessionMgr.isSessionValid(sessionKey))
            {
                redirectToLogin();
            }

            CachedLoginSession cachedSession = sessionMgr.findSession(sessionKey);

            //valid session, but trying to access the link for a different bucket
            if (!bucket.equalsIgnoreCase(cachedSession.getBucketName()))
            {
                forbidden();
            }
            
            //Check User password expiration & first time login 
            Long userId = Long.parseLong(cachedSession.getUserId());
            Long bucketId = Long.parseLong(cachedSession.getBucketId());
            BucketPasswordPolicy passwordPolicy = new BucketPasswordPolicy(bucketId).findOrCreate();
            if(!passwordPolicy.validatePasswordExpiration(userId) ||
                 !passwordPolicy.validateFirstLoginPasswordCheck(userId))
			{
            	//Destroy the current user session
            	sessionMgr.destroy(sessionKey);
            	//Redirect to change/reset password page
            	platform.PasswordResetManager resetManager = platform.PasswordResetManager.getInstance();
                String key = resetManager.createPasswordResetKey(userId, bucketId);
                String HtmlPath = "kaisquare";
                setDefaultContentPaths();
                renderTemplate("kaisquare/common/reset_pass_with_key.html", key, HtmlPath);
            }

            // populate the renderArgs with user information
            renderArgs.put("userId", cachedSession.getUserId());
            renderArgs.put("bucketId", cachedSession.getBucketId());
            renderArgs.put("username", cachedSession.getUserFullName());
            renderArgs.put("userSessionKey", cachedSession.getSessionKey());

            setConfigurations(bucket, true);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            redirectToLogin();
        }
    }

    /**
     * Denied http requests if the server is not ready yet
     */
    @Before(priority = 3)
    public static void readyForRequests()
    {
        if (!Environment.getInstance().isHttpReady())
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/common/under_maintenance.html");
        }
    }

    /**
     * this is triggered after rendering is done. It takes the rendered template
     * from response.out, creates a gzipped stream for response.out (if
     * supported by the client), minifies the content and writes the
     * template-string back to response.out
     */
    @Finally(priority = 1)
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
                { // could be "text/html; charset=utf-8"
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

    private static void setConfigurations(String bucket, boolean validSession)
    {
        if (validSession)
        {
            renderArgs.put("bucket", bucket);

            //get current user data for language preference
            String userId = renderArgs.get("userId").toString();
            MongoUser user = MongoUser.getById(userId);
            Environment.getInstance().setPlayLanguage(user, readBrowserLocale());

            //get map source
            MongoBucket targetBucket = MongoBucket.getByName(bucket);
            renderArgs.put("mapSource", LocationManager.getInstance().getMapSource(targetBucket.getBucketId()));
        }

        setDefaultContentPaths();
        configPiwik();

        // application type specific UI differences
        String applicationType = Environment.getInstance().getApplicationType();
        renderArgs.put("applicationType", applicationType);

        // check internet status for node
        String internetStatus = "Connected";
        if (applicationType.equalsIgnoreCase("node"))
        {
            internetStatus = (NetworkCheck.getNetworkStatus() & NetworkCheck.STATUS_INTERNET_CONNECTED) != 0 ?
                             "Connected" :
                             "Disconnected";
        }
        renderArgs.put("internetStatus", internetStatus);

        //set release number
        VersionManager versionManager = VersionManager.getInstance();
        renderArgs.put("platformVersion", versionManager.getPlatformVersion());

        int notificationSlpSecond = ConfigsShared.getInstance().notificationSoundMinGapSeconds();
        renderArgs.put("notificationSlpSecond", notificationSlpSecond);
    }

}
