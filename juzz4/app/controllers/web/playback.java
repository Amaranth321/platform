package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class playback extends Controller
{
    public static void cloud()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/playback/cloud.html");
    }

    public static void node()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/playback/node.html");
    }

    public static void nodeusbexport()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/playback/node_usb_export.html");
    }

    public static void browseuploadrequests(String deviceId, String channelId)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/playback/browse_upload_requests.html", deviceId, channelId);
    }
}
