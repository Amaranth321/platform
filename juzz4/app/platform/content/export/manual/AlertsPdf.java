package platform.content.export.manual;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import models.MongoDevice;
import models.notification.SentNotification;
import org.joda.time.DateTime;
import platform.Environment;
import platform.content.FileFormat;
import platform.content.export.PdfSettings;
import platform.content.export.ReportBuilder;
import platform.content.export.VcaExportHelper;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.notification.NotificationInfo;
import play.Logger;
import play.i18n.Messages;
import play.modules.morphia.Model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static lib.util.Util.getClientTimeString;

public class AlertsPdf implements ReportBuilder
{
    private final Model.MorphiaQuery query;
    private final int timeZoneOffsetMins;
    private final String locale;

    public AlertsPdf(Model.MorphiaQuery query, int timeZoneOffsetMins, String locale)
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
        return FileFormat.PDF;
    }

    @Override
    public InputStream generate()
    {
        String deletedEntryTag = Messages.getMessage(locale, "deleted-db-entry");
        try
        {
            Document doc = new Document();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, outputStream);
            doc.open();

            //time generated
            Paragraph preface = new Paragraph();
            preface.add(new Paragraph(Messages.getMessage(locale, "report-generated-on") + ": " +
                                      getClientTimeString(DateTime.now(), timeZoneOffsetMins), PdfSettings.titleFont));
            preface.add(new Paragraph(" "));
            doc.add(preface);

            //data table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);

            //headers
            BaseColor headerBgColor = new BaseColor(196, 223, 155);
            int headerHeight = 18;
            PdfPCell cell = new PdfPCell();
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setBackgroundColor(headerBgColor);
            cell.setFixedHeight(headerHeight);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "datetime"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "device-name"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "channel"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "type"), PdfSettings.headerFont));
            table.addCell(cell);
            cell.setPhrase(new Paragraph(Messages.getMessage(locale, "happen-at"), PdfSettings.headerFont));
            table.addCell(cell);

            //Populate Rows
            int rowHeight = 15;
            PdfPCell newCell = new PdfPCell();
            newCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            newCell.setFixedHeight(rowHeight);

            Iterable<SentNotification> iterable = query.fetch();
            for (SentNotification notification : iterable)
            {
                NotificationInfo notificationInfo = notification.getNotificationInfo();
                String timestamp = VcaExportHelper.getLocalTimeStamp(notificationInfo.getEventTime(),
                                                                     timeZoneOffsetMins);
                String eventName = Messages.getMessage(locale, notificationInfo.getEventType().toString());

                //camera info
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

                //write to table
                newCell.setPhrase(new Paragraph(timestamp, PdfSettings.entryFont));
                table.addCell(newCell);
                newCell.setPhrase(new Paragraph(deviceName, PdfSettings.entryFont));
                table.addCell(newCell);
                newCell.setPhrase(new Paragraph(channelName, PdfSettings.entryFont));
                table.addCell(newCell);
                newCell.setPhrase(new Paragraph(eventName, PdfSettings.entryFont));
                table.addCell(newCell);
                newCell.setPhrase(new Paragraph(address, PdfSettings.entryFont));
                table.addCell(newCell);
            }

            doc.add(table);
            doc.add(new Paragraph(" "));

            doc.close();
            outputStream.close();

            return new ByteArrayInputStream(outputStream.toByteArray());
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }
}
