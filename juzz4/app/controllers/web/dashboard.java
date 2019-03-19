package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author Nischal
 */

@With(WebInterceptor.class)
public class dashboard extends Controller
{

    public static void index()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/dashboard/dashboard.html");
    }
}
