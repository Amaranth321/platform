package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author Aye Maung
 * @since v4.4
 */
@With(WebInterceptor.class)
public class Monitoring extends Controller
{
    public static void runningtasks()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/playback/running_tasks.html");
    }
}
