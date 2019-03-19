package controllers.web;

import com.google.gson.Gson;
import controllers.interceptors.WebInterceptor;
import lib.util.Util;
import models.licensing.NodeLicenseInfo;
import platform.CloudLicenseManager;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class license extends Controller
{
    public static void localnodelicenses()
    {
        boolean readonly = true;
        renderTemplate(renderArgs.get("HtmlPath") + "/license/node_licenses.html", readonly);
    }

    public static void globalnodelicenses()
    {
        boolean readonly = false;
        renderTemplate(renderArgs.get("HtmlPath") + "/license/node_licenses.html", readonly);
    }

    public static void addlicense()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/license/add_license.html");
    }

    public static void editlicense(String id)
    {
        try
        {
            NodeLicenseInfo nodeLicenseInfo = CloudLicenseManager.getInstance().getNodeLicenseInfo(id);
            String jsonLicenseDetails = new Gson().toJson(nodeLicenseInfo);
            renderTemplate(renderArgs.get("HtmlPath") + "/license/edit_license.html", jsonLicenseDetails);
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            notFound();
        }
    }

    public static void licensedetails(String id)
    {
        try
        {
            NodeLicenseInfo nodeLicenseInfo = CloudLicenseManager.getInstance().getNodeLicenseInfo(id);
            String jsonLicenseDetails = new Gson().toJson(nodeLicenseInfo);
            renderTemplate(renderArgs.get("HtmlPath") + "/license/license_details.html", jsonLicenseDetails);
        }
        catch (Exception e)
        {
            Logger.error(Util.getStackTraceString(e));
            notFound();
        }
    }
}
