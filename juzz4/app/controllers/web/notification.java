package controllers.web;

import controllers.interceptors.WebInterceptor;
import platform.notification.NotificationSource;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import java.text.ParseException;

@With(WebInterceptor.class)
public class notification extends Controller
{
    public static void details(String eventId, String source) throws ParseException
    {
        try
        {
            renderArgs.put("eventId", eventId);
            NotificationSource enumSource = NotificationSource.parse(source);
            switch (enumSource)
            {
                case CAMERA:
                    renderTemplate(renderArgs.get("HtmlPath") + "/notification/view_details_camera.html");
                    return;

                case LABEL:
                    renderTemplate(renderArgs.get("HtmlPath") + "/notification/view_details_label.html");
                    return;
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        notFound();
    }

    public static void labelsettings() throws ParseException
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/notification/label_settings.html");
    }

    public static void securityalerts(String eventId) throws ParseException
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/notification/security_alerts.html", eventId);
    }

    public static void labelnotifications(String eventId) throws ParseException
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/notification/label_notifications.html", eventId);
    }

    public static void landing(String alertType, String eventId)
    {
        renderTemplate(renderArgs.get("HtmlPath") + "/notification/landing.html", alertType, eventId);
    }
}
