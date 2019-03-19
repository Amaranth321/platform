package controllers.web;

import java.util.HashSet;
import java.util.Set;

import lib.util.Util;
import models.Holiday;
import models.RemoteShellState;
import play.mvc.Controller;
import play.mvc.With;
import controllers.interceptors.WebInterceptor;

@With(WebInterceptor.class)
public class support extends Controller
{

    public static void remoteshell()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/support/remote_shell_list.html");
    }

    public static void startshell(String id)
    {
        String nodePlatformDeviceId = id;
        String host = "", port = "", user = "";
        RemoteShellState state = RemoteShellState.find("cloudPlatformDeviceId", nodePlatformDeviceId).first();
        if (state != null)
        {
            host = state.host;
            port = Integer.toString(state.port);
            user = state.username;
        }
        renderTemplate(renderArgs.get("HtmlPath") +
                       "/support/remote_shell_start.html", nodePlatformDeviceId, host, port, user);
    }

    public static void nodeloglist()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/support/node_log_list.html");
    }

    public static void accountstatements()
    {
        boolean readonly = false;
        renderTemplate(renderArgs.get("HtmlPath") + "/support/account_statements.html", readonly);
    }

    public static void nodeinformation()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/support/node_information.html");
    }

    public static void holidaycalendar()
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/support/holiday_calendar.html");
    }

    public static void addholiday(String holidayId)
    {
        Holiday holiday = new Holiday();
        if (!Util.isNullOrEmpty(holidayId)) {
            holiday = Holiday.findById(holidayId);
            
            Set<String> countries = new HashSet<>();
            for (String country : holiday.getCountries()) {
            	countries.add(String.format("\"%s\"", country));
            }
            holiday.setCountries(countries);
        }

        renderTemplate(renderArgs.get("HtmlPath") + "/support/add_holiday.html", holiday);
    }

}
