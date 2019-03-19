package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class access extends Controller
{

    public static void list()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/accesskeys/list.html");
    }

    public static void generate()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/accesskeys/generate.html");
    }

}
