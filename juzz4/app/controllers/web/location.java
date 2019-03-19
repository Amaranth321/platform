package controllers.web;

import controllers.interceptors.WebInterceptor;
import platform.LocationManager;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class location extends Controller
{

    /**
     * Web controller to redirect page for live track
     */
    public static void track()
    {
        String mapSource = LocationManager.getInstance().getMapSource(String.valueOf(renderArgs.get("bucketId")));
        if ("baidu".equals(mapSource))
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/track/bmap_index.html");
        }
        else
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/track/gmap_index.html");
        }
    }
}
