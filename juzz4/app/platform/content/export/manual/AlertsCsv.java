package platform.content.export.manual;

import models.MongoDevice;
import models.notification.SentNotification;
import org.joda.time.DateTime;
import platform.Environment;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import platform.content.export.ReportCsvWriter;
import platform.content.export.VcaExportHelper;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.notification.NotificationInfo;
import play.Logger;
import play.i18n.Messages;
import play.modules.morphia.Model;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static lib.util.Util.getClientTimeString;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class AlertsCsv implements ReportBuilder
{
    private final Model.MorphiaQuery query;
    private final int timeZoneOffsetMins;
    private final String locale;

    public AlertsCsv(Model.MorphiaQuery query, int timeZoneOffsetMins, String locale)
    {
        this.query = query;
        this.timeZoneOffsetMins = timeZoneOffsetMins;
        this.locale = locale;
    }

    @Override
    public String getFilename()
    {
        return String.format("Alerts_%s.%s",
                             getClientTimeString(DateTime.now(), timeZoneOffsetMins),
                             getFileFormat().getExtension());
    }

    @Override
    public FileFormat getFileFormat()
    {
        return FileFormat.CSV;
    }

    @Override
    public InputStream generate()
    {
        String deletedEntryTag = Messages.getMessage(locale, "deleted-db-entry");
        try (ReportCsvWriter csvWriter = new ReportCsvWriter())
        {
            //header row
            csvWriter.writeRow(Arrays.asList(
                    Messages.getMessage(locale, "datetime"),
                    Messages.getMessage(locale, "device-name"),
                    Messages.getMessage(locale, "channel"),
                    Messages.getMessage(locale, "type"),
                    Messages.getMessage(locale, "happen-at")
            ));

            //data rows
            Iterable<SentNotification> notifications = query.fetch();
            for (SentNotification notification : notifications)
            {
                NotificationInfo notificationInfo = notification.getNotificationInfo();
                List<String> dataRow = new ArrayList<>();
                dataRow.add(VcaExportHelper.getLocalTimeStamp(notificationInfo.getEventTime(), timeZoneOffsetMins));

                //find camera info
                String deviceName = deletedEntryTag;
                String channelName = deletedEntryTag;
                String address = deletedEntryTag;
                if (Environment.getInstance().onCloud())
                {
                    CacheClient cacheClient = CacheClient.getInstance();
                    CachedNodeCamera nodeCamera = cacheClient.getNodeCamera(notificationInfo.getCamera());
                    if (nodeCamera != null)
                    {
                        deviceName = nodeCamera.getNodeName();
                        channelName = nodeCamera.getCameraName();
                    }

                    CachedDevice nodeDevice = cacheClient.getDeviceByCoreId(notificationInfo.getCamera().getCoreDeviceId());
                    if (nodeDevice != null)
                    {
                        deviceName = nodeDevice.getName();
                        address = nodeDevice.getAddress();
                    }
                }
                else if (Environment.getInstance().onKaiNode())
                {
                    MongoDevice camera = MongoDevice.getByCoreId(notificationInfo.getCamera().getCoreDeviceId());
                    if (camera != null)
                    {
                        deviceName = camera.getName();
                        address = camera.getAddress();
                    }
                    channelName = "1";
                }

                dataRow.add(deviceName);
                dataRow.add(channelName);
                dataRow.add(Messages.getMessage(locale, notificationInfo.getEventType().toString()));
                dataRow.add(address);

                csvWriter.writeRow(dataRow);
            }

            return new FileInputStream(csvWriter.getCsvFile());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }
}
