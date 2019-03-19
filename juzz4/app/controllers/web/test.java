package controllers.web;

import controllers.interceptors.WebInterceptor;
import play.Play;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import java.util.Arrays;
import java.util.List;

@With(WebInterceptor.class)
public class test extends Controller
{
    @Before(priority = 2)
    private static void check()
    {
        List<String> allowedAddresses = Arrays.asList(
                "hk.uat.kaisquare.com.cn",
                "ci.developer.kaisquare.com"
        );

        if (Play.id.equals("node"))
        {
            return;
        }

        if (Play.mode.isProd() && !allowedAddresses.contains(request.host))
        {
            notFound();
        }
    }

    public static void timecard()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/timecard.html");
    }

    public static void vcatree()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/vca_tree.html");
    }

    public static void kaiflow()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/kai_flow.html");
    }

    public static void checkapi()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/checkapi.html");
    }

    public static void distribution()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/distribution.html");
    }

    public static void drawing()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/drawing.html");
    }

    public static void vcafeeds()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/vca_feeds.html");
    }

    public static void jw()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/jw.html");
    }

    public static void nodebrowser()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/debugging/node_browser.html");
    }

    public static void leaflet()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/leaftlet.html");
    }

    public static void dummy()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/test/dummy.html");
    }
}
