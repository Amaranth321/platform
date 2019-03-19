package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class Debugging extends Controller
{

    public static void index()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/debugging/status_logs.html");
    }

    public static void pagedtables()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/debugging/paged_tables.html");
    }

}
