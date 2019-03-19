package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class SoftwareUpdate extends Controller
{
    public static void list()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/softwareupdate/list_updates.html");
    }
}
