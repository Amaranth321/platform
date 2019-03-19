package controllers.web;

import controllers.interceptors.WebInterceptor;
import models.Poi;
import platform.LocationManager;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class poi extends Controller
{

    /**
     * Web controller to redirect page for POI list
     */
    public static void list()
    {

        renderTemplate(renderArgs.get("HtmlPath") + "/poi/list.html");
    }

    /**
     * Web controller to redirect page for add POI
     */
    public static void add()
    {
        if (LocationManager.getInstance().getMapSource(
                (String) renderArgs.get("bucketId")).equalsIgnoreCase("baidu"))
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/poi/bmap_add.html");
        }
        else
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/poi/gmap_add.html");
        }
    }

    /**
     * Web controller to redirect page for edit POI
     *
     * @param id POI ID
     */
    public static void edit(String id)
    {
        Poi targetPoi = Poi.findById(id);
        if (LocationManager.getInstance().getMapSource(
                (String) renderArgs.get("bucketId")).equalsIgnoreCase("baidu"))
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/poi/bmap_edit.html", targetPoi);
        }
        else
        {
            renderTemplate(renderArgs.get("HtmlPath") + "/poi/gmap_edit.html", targetPoi);
        }
    }
}
