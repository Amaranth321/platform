package controllers.web;

import controllers.DefaultController;
import lib.util.Util;
import models.MongoBucket;
import platform.Environment;
import platform.access.UserSessionManager;
import platform.config.readers.ConfigsCloud;
import platform.config.readers.ConfigsShared;
import platform.db.cache.proxies.CachedLoginSession;
import platform.node.CloudConnector;
import platform.node.NodeManager;
import play.Logger;
import play.Play;
import play.mvc.Before;
import play.mvc.Finally;

import java.io.IOException;

public class login extends DefaultController
{
    /**
     * Redirect Http to Https
     */
    @Before(priority = 1)
    public static void redirectToHttps()
    {
        //set browser default language by request
        Environment.getInstance().setPlayLanguage(null, readBrowserLocale());

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

    @Before(priority = 2)
    public static void setConfigurations()
    {
        configPiwik();

        //branding
        String promoPic = "promo-default.png";
        if (Environment.getInstance().onCloud())
        {
            promoPic = ConfigsCloud.getInstance().loginBrandingLogo(request.domain);
        }

        renderArgs.put("promoPic", promoPic);
    }

    /**
     * Adds the cache control header
     */
    @Finally
    static void cacheControl() throws IOException
    {
        response.cacheFor("5mn");
    }

    public static void index(String redirectId)
    {
        try
        {
            setDefaultContentPaths();

            //search target page from redirectId
            String absoluteUrlPath = UserSessionManager.getInstance().getCachedUrlPath(redirectId);

            //redirect if already logged in.
            String sessionKey = session.get("sessionKey");
            if (UserSessionManager.getInstance().isSessionValid(sessionKey))
            {
                sso(session.get("sessionKey"), absoluteUrlPath);
            }

            if (Environment.getInstance().onCloud())
            {
                renderTemplate(renderArgs.get("HtmlPath") + "/common/login.html", absoluteUrlPath);
            }

            //nodes
            if (NodeManager.getInstance().isRegisteredOnCloud())
            {
                MongoBucket nodeBucket = NodeManager.getInstance().getBucket();
                renderArgs.put("bucket", nodeBucket.getName());
                renderTemplate(renderArgs.get("HtmlPath") + "/common/login_node.html");
            }
            else
            {
                String cloudServerHost = CloudConnector.getInstance().getServerHost();
                String nodeVersion = NodeManager.getInstance().getVersion();
                renderTemplate(renderArgs.get("HtmlPath") + "/common/register_node.html",
                               cloudServerHost,
                               nodeVersion);
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            error();
        }
    }

    public static void forgotpass()
    {
        setDefaultContentPaths();
        renderTemplate(renderArgs.get("HtmlPath") + "/common/forgot_pass.html");
    }

    public static void resetsubmitted()
    {
        setDefaultContentPaths();
        renderTemplate(renderArgs.get("HtmlPath") + "/common/reset_submitted.html");
    }

    public static void changepassword()
    {
        renderTemplate("/public/condo/changepw.html");
    }

    public static void sso(String sessionKey, String absoluteUrlPath)
    {
        UserSessionManager sessionMgr = UserSessionManager.getInstance();
        if (!sessionMgr.isSessionValid(sessionKey))
        {
            redirectToLogin();
        }

        CachedLoginSession cachedSession = sessionMgr.findSession(sessionKey);
        String bucketName = cachedSession.getBucketName();
        renderArgs.put("bucket", bucketName);
        renderArgs.put("caller-user-id", cachedSession.getUserId());
        renderArgs.put("HtmlPath", bucketName);
        session.put("sessionKey", sessionKey);

        if (Util.isNullOrEmpty(absoluteUrlPath))
        {
            redirect("/" + bucketName + "/dashboard");
        }
        else
        {
            redirect(absoluteUrlPath);
        }
    }
}