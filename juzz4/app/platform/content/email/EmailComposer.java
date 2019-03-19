package platform.content.email;

import org.joda.time.DateTime;
import platform.common.Frequency;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedBucket;
import platform.db.cache.proxies.CachedUser;
import platform.db.gridfs.GridFsDetails;
import platform.notification.LabelNotificationInfo;
import platform.notification.NotificationInfo;
import play.Play;
import play.i18n.Messages;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum EmailComposer
{
    INSTANCE;

    private static final String EMAIL_TEMPLATE_DIR = "kaisquare/common/templates/";
    private static final String SERVER_BASE_URL = Play.configuration.getProperty("application.baseUrl");
    private static final String DEFAULT_SENDER = "System Admin";

    public static EmailComposer getInstance()
    {
        return INSTANCE;
    }

    public EmailItem createPeriodicReportEmail(String recipientEmail,
                                               List<GridFsDetails> attachments,
                                               Frequency frequency)
    {
        //construct html body from template
        Template template = TemplateLoader.load(EMAIL_TEMPLATE_DIR + "periodic_report_email_tmpl.html");
        Map emailArgs = new LinkedHashMap();
        emailArgs.put("recipient", recipientEmail);
        emailArgs.put("sender", DEFAULT_SENDER);
        emailArgs.put("baseUrl", SERVER_BASE_URL);
        emailArgs.put("type", Messages.get(frequency.name().toLowerCase()));

        //create EmailItem
        String emailHtmlBody = template.render(emailArgs);
        String subject = String.format("[%s] %s", frequency, Messages.get("periodic-report-generated"));
        EmailItem emailItem = new EmailItem(recipientEmail, subject, emailHtmlBody, attachments);

        return emailItem;
    }

    public EmailItem createEventAlertEmail(CachedUser user, NotificationInfo notificationInfo)
    {
        //construct details link
        CachedBucket bucket = CacheClient.getInstance().getBucket(user.getBucketId() + "");
        String detailsLink = String.format("%s/%s/notification/landing?alertType=camera&eventId=%s",
                                           SERVER_BASE_URL,
                                           bucket.getName(),
                                           notificationInfo.getEventId());
        //get device time
        DateTime dtDevice = new DateTime(notificationInfo.getEventTime(), notificationInfo.getLocation().getTimeZone());
        String timestamp = dtDevice.toString("dd/MM/yyyy HH:mm:ss z");

        //construct html body from template
        Map emailArgs = new LinkedHashMap();
        emailArgs.put("recipient", user.getName());
        emailArgs.put("sender", DEFAULT_SENDER);
        emailArgs.put("baseUrl", SERVER_BASE_URL);
        emailArgs.put("type", notificationInfo.getLocalizedEventName());
        emailArgs.put("deviceName", notificationInfo.getDeviceDisplayText());
        emailArgs.put("time", timestamp);
        emailArgs.put("address", notificationInfo.getLocation().getAddress());
        emailArgs.put("detailsLink", detailsLink);

        //create EmailItem
        Template template = TemplateLoader.load(EMAIL_TEMPLATE_DIR + "alert_notification_email.html");
        String emailHtmlBody = template.render(emailArgs);
        String subject = String.format("[Notification] %s", notificationInfo.getLocalizedEventName());
        EmailItem emailItem = new EmailItem(user.getEmail(), subject, emailHtmlBody, null);

        return emailItem;
    }

    public EmailItem createLabelNotification(CachedUser user, LabelNotificationInfo info)
    {
        //construct details link
        CachedBucket bucket = CacheClient.getInstance().getBucket(user.getBucketId() + "");

        //get device time
        DateTime dtDevice = new DateTime(info.getEventTime(), info.getLocation().getTimeZone());
        String timestamp = dtDevice.toString("dd/MM/yyyy HH:mm:ss z");

        //construct html body from template
        Map emailArgs = new LinkedHashMap();
        emailArgs.put("recipient", user.getName());
        emailArgs.put("sender", DEFAULT_SENDER);
        emailArgs.put("baseUrl", SERVER_BASE_URL);
        emailArgs.put("type", info.getLocalizedEventName());
        emailArgs.put("labelName", info.getLabelName());
        emailArgs.put("time", timestamp);
        emailArgs.put("occupancyLimit", info.getNotificationData().get("occupancyLimit"));
        emailArgs.put("message", info.getNotificationData().get("message"));
        emailArgs.put("address", info.getLocation().getAddress());

        //create EmailItem
        Template template = TemplateLoader.load(EMAIL_TEMPLATE_DIR + "label_notification_email.html");
        String emailHtmlBody = template.render(emailArgs);
        String subject = String.format("[Notification] %s", info.getLocalizedEventName());
        EmailItem emailItem = new EmailItem(user.getEmail(), subject, emailHtmlBody, null);

        return emailItem;
    }

}
