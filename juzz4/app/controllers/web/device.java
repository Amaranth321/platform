package controllers.web;

import controllers.interceptors.WebInterceptor;
import models.backwardcompatibility.Device;
import models.MongoDevice;
import platform.CryptoManager;
import platform.Environment;
import platform.config.readers.ConfigsCloud;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import static lib.util.Util.getStackTraceString;

@With(WebInterceptor.class)
public class device extends Controller
{
    public static void list()
    {
        boolean addDeviceAllowed = true;
        boolean limitedAccess = false;

        if (Environment.getInstance().onCloud())
        {
            addDeviceAllowed = ConfigsCloud.getInstance().allowAddDeviceOnCloud();
        }

        renderTemplate(renderArgs.get("HtmlPath") + "/device/device_listing.html", addDeviceAllowed, limitedAccess);
    }

    public static void add()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/device/add_device.html");
    }

    public static void edit(long id)
    {
        try
        {
            MongoDevice mongoDevice = MongoDevice.getByPlatformId(id + "");
            Device targetDevice = new Device(mongoDevice);
            renderTemplate(renderArgs.get("HtmlPath") + "/device/edit.html", targetDevice);
        }
        catch (Exception e)
        {
            Logger.error(getStackTraceString(e));
            error();
        }
    }

    public static void editlimited(long id)
    {
        try
        {
            MongoDevice mongoDevice = MongoDevice.getByPlatformId(id + "");
            mongoDevice.setPassword(CryptoManager.aesDecrypt(mongoDevice.getPassword()));
            Device targetDevice = new Device(mongoDevice);
            String labelString = "";
            renderTemplate(renderArgs.get("HtmlPath") + "/device/edit_limited.html", targetDevice, labelString);
        }
        catch (Exception e)
        {
            Logger.error(getStackTraceString(e));
            error();
        }
    }

    public static void nodeinfo(String id, boolean readonly)
    {
        String platformDeviceId = id;
        renderTemplate(renderArgs.get("HtmlPath") + "/device/node_info_popup.html", platformDeviceId, readonly);
    }

    public static void viewsnapshot(String id)
    {
        String[] idList = id.split("-");
        String coreDeviceId = idList[0];
        String channelId = idList[1];
        renderTemplate(renderArgs.get("HtmlPath") + "/device/view_snapshot.html", coreDeviceId, channelId);
    }

    public static void editnodedevice(String nodeId, String nodeCameraId)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/device/edit_node_device.html", nodeId, nodeCameraId);
    }

    public static void camselector(String preselectedId)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/device/camera_selector.html", preselectedId);
    }

    public static void devicelogs(long id)
    {
        long platformDeviceId = id;
        renderTemplate(renderArgs.get("HtmlPath") + "/device/device_logs.html", platformDeviceId);
    }
}
