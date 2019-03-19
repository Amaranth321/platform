package controllers.web;

import controllers.interceptors.WebInterceptor;
import models.RemoteShellState;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author Keith
 * @since v4.6
 */

@With(WebInterceptor.class)
public class scheduletask extends Controller
{
    public static void list(String id)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/scheduletask/list.html");
    }

    public static void add(String id)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/scheduletask/add.html");
    }

    public static void detail(String id)
    {
        String scheduleId = id;
        renderTemplate(renderArgs.get("HtmlPath") + "/scheduletask/detail.html", scheduleId);
    }
}
