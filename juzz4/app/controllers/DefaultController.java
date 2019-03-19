package controllers;

import com.google.gson.JsonSerializer;
import com.kaisquare.playframework.CustomizedRenderJson;
import models.cloud.UIConfigurableCloudSettings;
import platform.Environment;
import platform.access.UserSessionManager;
import platform.config.readers.ConfigsCloud;
import play.i18n.Lang;
import play.mvc.Controller;

import java.lang.reflect.Type;

public class DefaultController extends Controller
{
    protected static void renderJSON(Object o)
    {
        throw new CustomizedRenderJson(o);
    }

    protected static void renderJSON(Object o, Type type)
    {
        throw new CustomizedRenderJson(o, type);
    }

    protected static void renderJSON(Object o, JsonSerializer<?>... adapters)
    {
        throw new CustomizedRenderJson(o, adapters);
    }

    protected static void setDefaultContentPaths()
    {
        renderArgs.put("CdnRootPath", null);
        renderArgs.put("CdnPath", "/public/css");

        if (renderArgs.get("HtmlPath") == null)
        {
            renderArgs.put("HtmlPath", "kaisquare");
        }
    }

    protected static void configPiwik()
    {
        String piwikServer = "";
        int piwikId = 0;
        if (Environment.getInstance().onCloud())
        {
            piwikServer = ConfigsCloud.getInstance().piwikServerUrl();
            piwikId = UIConfigurableCloudSettings.server().keyValues().piwikServerId;
        }

        renderArgs.put("piwikServer", piwikServer);
        renderArgs.put("piwikId", piwikId);
    }

    protected static void redirectToLogin()
    {
        //save the sub link so that if the user decides to re-login, he will be redirected to that page
        String absoluteUrlPath = request.url;
        String redirectId = UserSessionManager.getInstance().generateRedirectId(absoluteUrlPath);
        redirect("/" + (redirectId == null ? "" : "?redirectId=" + redirectId));
    }

    protected static String readBrowserLocale()
    {
        for (String acceptedLang : request.acceptLanguage())
        {
            String localeName = acceptedLang.replace('-', '_');
            if (Lang.getLocale(localeName) == null)
            {
                continue;
            }

            acceptedLang = acceptedLang.contains("en") ? "en" : acceptedLang.toLowerCase();
            return acceptedLang;
        }

        //return default
        return "en";
    }
}
