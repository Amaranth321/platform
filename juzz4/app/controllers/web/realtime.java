package controllers.web;

import play.mvc.Controller;
import play.mvc.With;
import controllers.interceptors.WebInterceptor;

@With(WebInterceptor.class)
public class realtime extends Controller
{
	public static void index()
    {
        renderArgs.put("realTimePage", true);
        renderTemplate(renderArgs.get("HtmlPath") + "/realtime/default.html");
    }
	
	public static void currentoccupancy()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/realtime/currentoccupancy.html");
    }
}
