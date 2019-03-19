package platform.content.export.manual;

import com.kaisquare.util.XLSWriter;
import models.AuditLog;
import org.joda.time.DateTime;
import platform.content.FileFormat;
import platform.content.export.ExportUtils;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static lib.util.Util.getClientTimeString;

public class AuditLogXls implements ReportBuilder
{
    private final List<AuditLog> auditLogList;
    private final int timeZoneOffsetMins;
    private final String locale;

    public AuditLogXls(List<AuditLog> auditLogList, int timeZoneOffsetMins, String locale)
    {
        this.auditLogList = auditLogList;
        this.timeZoneOffsetMins = timeZoneOffsetMins;
        this.locale = locale;
    }

    @Override
    public String getFilename()
    {
        return String.format("AuditLogs_%s.%s",
                             getClientTimeString(DateTime.now(), timeZoneOffsetMins),
                             getFileFormat().getExtension());
    }

    @Override
    public FileFormat getFileFormat()
    {
        return FileFormat.XLS;
    }

    @Override
    public InputStream generate()
    {
        try
        {
            XLSWriter writer = new XLSWriter("Event");
            int maxRowsPerSheet = writer.getMaxRowsPerSheet();

            //column names row
            List<Object> headerList = new ArrayList<Object>();
            headerList.add(Messages.getMessage(locale, "datetime"));
            headerList.add(Messages.getMessage(locale, "company-name"));
            headerList.add(Messages.getMessage(locale, "username"));
            headerList.add(Messages.getMessage(locale, "activity-type"));
            headerList.add(Messages.getMessage(locale, "ip"));
            writer.addRow(headerList, true, false);

            //entries
            int rowIndex = 0;
            for (AuditLog audit : auditLogList)
            {
                List<Object> list = new ArrayList<Object>();
                list.add(ExportUtils.getLocalTimeString(new Date(audit.timeobject), timeZoneOffsetMins));
                list.add(audit.bucketName);
                list.add(audit.userName);
                list.add(Messages.getMessage(locale, audit.serviceName));
                list.add(Messages.getMessage(locale, audit.remoteIp));
                writer.addRow(list);
                rowIndex++;

                if (rowIndex >= maxRowsPerSheet)
                {
                    rowIndex = 0;
                }
            }

            //Generate outputStream
            byte[] fileBytes = null;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
            {
                writer.writeFile(outputStream);
                fileBytes = outputStream.toByteArray();
            }

            return new ByteArrayInputStream(fileBytes);

        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
    }
}
