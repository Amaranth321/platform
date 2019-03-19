package controllers.web;


import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class node extends Controller
{
    public static void list()
    {
        boolean addDeviceAllowed = false;
        boolean limitedAccess = true;
        renderTemplate(renderArgs.get("HtmlPath") + "/device/device_listing.html", addDeviceAllowed, limitedAccess);
    }

    public static void localinfo()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/node/local_info.html");
    }
}
