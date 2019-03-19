package controllers.web;

import controllers.interceptors.WebInterceptor;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.MongoUserPreference;
import platform.Environment;
import platform.UserProvisioning;
import platform.node.NodeManager;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author Nischal
 */
@With(WebInterceptor.class)
public class live extends Controller
{

    public static void fullview(int id)
    {
        try
        {
            String userId = renderArgs.get("userId").toString();
            MongoUserPreference userPreference = UserProvisioning.getUserPreference(userId);
            userPreference.setNumberOfViews(userPreference.getNumberOfViews() == null ? 1 : userPreference.getNumberOfViews());

            id = (id == 1 || id == 4 || id == 9 || id == 16) ? id : userPreference.getNumberOfViews();
            userPreference.setNumberOfViews(id);
            userPreference.save();

            //application type specific UI differences
            String applicationType = Environment.getInstance().getApplicationType();
            renderArgs.put("liveViewPage", true);
            int maxCameraLimit = 0;
            if (applicationType.equalsIgnoreCase("node"))
            {
            	maxCameraLimit = NodeManager.getInstance().getCameraLimit();
            }
            renderTemplate(renderArgs.get("HtmlPath") + "/live/" + id + ".html", maxCameraLimit);

        }
        catch (ApiException e)
        {
            notFound();
        }
    }

    public static void viewdevice(String id)
    {
        MongoDevice device = MongoDevice.getByCoreId(id);

        if (device != null)
        {
            renderArgs.put("deviceId", id);
            renderTemplate(renderArgs.get("HtmlPath") + "/live/view_device.html");
        }
        else
        {
            notFound();
        }
    }

}
