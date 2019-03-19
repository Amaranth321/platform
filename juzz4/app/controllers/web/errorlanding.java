package controllers.web;

import controllers.api.APIController;
import controllers.interceptors.LoginInterceptor;
import play.mvc.Before;
import play.mvc.With;

/**
 * @author Aye Maung
 * @since v4.4
 */
@With(LoginInterceptor.class)
public class errorlanding extends APIController
{
    @Before(priority = 1)
    private static void configure()
    {
        setDefaultContentPaths();
    }

    public static void corebandwidthlimit()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/error/core_bandwidth_limit.html");
    }
}
