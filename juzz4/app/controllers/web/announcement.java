package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class announcement extends Controller
{

    public static void list()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/announcement/list.html");
    }

    public static void add()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/announcement/add.html");
    }

    public static void edit(String id)
    {
        models.Announcement targetAnnouncement = models.Announcement.findById(id);
        renderTemplate(renderArgs.get("HtmlPath") + "/announcement/edit.html", targetAnnouncement);
    }
}
