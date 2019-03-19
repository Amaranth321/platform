package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

@With(WebInterceptor.class)
public class role extends Controller
{

    public static void list()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/role/list.html");
    }

    public static void assignfeature(long id)
    {
        renderArgs.put("roleId", id);
        renderTemplate(renderArgs.get("HtmlPath") + "/role/assign_feature.html");
    }

    public static void add()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/role/add.html");
    }

    public static void edit(Long id)
    {
        Long roleId = id;
        renderTemplate(renderArgs.get("HtmlPath") + "/role/edit.html", roleId);
    }
}
