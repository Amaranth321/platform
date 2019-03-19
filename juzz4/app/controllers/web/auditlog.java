/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author user
 */
@With(WebInterceptor.class)
public class auditlog extends Controller
{

    public static void list(String bucket)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/auditlog/list.html");
    }

    public static void auditdetails(String id)
    {
        String auditId = id;
        renderTemplate(renderArgs.get("HtmlPath") + "/auditlog/details.html", auditId);
    }
}
