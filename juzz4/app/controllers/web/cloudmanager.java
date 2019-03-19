package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class cloudmanager extends Controller
{
    public static void nodebrowser()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/cloudmanager/node_browser.html");
    }

    public static void settings()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/cloudmanager/settings.html");
    }

}
