package controllers.interceptors;

import com.google.gson.Gson;
import controllers.api.APIController;
import jobs.cloud.LogApiJob;
import jobs.node.NetworkCheck;
import lib.util.Util;
import lib.util.exceptions.InternalException;
import models.AuditLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import platform.Environment;
import platform.config.readers.ConfigsCloud;
import play.Logger;
import play.mvc.Before;
import play.mvc.Finally;

import java.util.HashMap;
import java.util.Map;

public class LoginInterceptor extends APIController
{

    @Before(priority = 2)
    static void setCORS()
    {

        //allow all to access our API
        response.accessControl("*", null, false);

        //just return headers for the OPTIONS request
        if (request.method.equalsIgnoreCase("OPTIONS"))
        {
            response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            response.setHeader("Access-Control-Expose-Headers", "Set-Cookie");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
            response.setHeader("Access-Control-Max-Age", "1800");
            ok();
        }
    }

    @Before(priority = 2)
    static void setConfigParams()
    {
        // set localization
        Environment.getInstance().setPlayLanguage(null, readBrowserLocale());

        // for nodes
        if (Environment.getInstance().onKaiNode())
        {
            String internetStatus = "Connected";
            internetStatus = (NetworkCheck.getNetworkStatus() & NetworkCheck.STATUS_INTERNET_CONNECTED) != 0 ? "Connected" : "Disconnected";
            renderArgs.put("internetStatus", internetStatus);
        }
    }

    /**
     * config promo logo picture on login page.
     */
    @Before(priority = 2)
    public static void configPromoPic()
    {
        String promoPic = "promo-default.png";
        if (Environment.getInstance().onCloud())
        {
            promoPic = ConfigsCloud.getInstance().loginBrandingLogo(request.domain);
        }

        renderArgs.put("promoPic", promoPic);
    }

    /**
     * This interception is triggered after all action calls except for keepalive api
     * This Interception method saves recent request's <br/>actionMethod, user-id, username, bucket-name,<br/>
     * bucket, remote IP address, request headers , pamasdata and <br/> exception into audit log DB.
     *
     * @param exception if null saves blank
     *
     * @see controllers.api.Login#keepalive(java.lang.String)
     */
    @Finally(priority = 1, unless = {"api.Login.keepalive"})
    static void audit(Throwable exception)
    {
        LogApiJob.queue(new LogApiJob.ApiCall(request, response, renderArgs, params.allSimple()));
    }

}

