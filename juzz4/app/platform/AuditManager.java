package platform;

import lib.util.exceptions.ApiException;
import models.AuditLog;
import models.MongoBucket;
import models.MongoUser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import platform.content.export.manual.AuditLogPdf;
import platform.content.export.manual.AuditLogXls;
import play.Logger;

import java.net.InetAddress;
import java.util.List;

public class AuditManager
{

    private static AuditManager instance = new AuditManager();

    private AuditManager()
    {
    }

    public static AuditManager getInstance()
    {
        return instance;
    }

    public void generateAuditlog(MongoUser user, String serviceName, Exception exp)
    {
        try
        {
            AuditLog audit = new AuditLog();

            MongoBucket bucket = MongoBucket.getById(user.getBucketId());
            audit.bucketId = bucket.getBucketId();
            audit.bucketName = bucket.getName();
            audit.userId = user.getUserId();
            audit.userName = user.getName();
            audit.serviceName = serviceName;

            InetAddress netAdd = InetAddress.getLocalHost();
            audit.remoteIp = netAdd.getHostAddress();
            if (null != exp)
            {
                audit.exception = exp.getMessage();
                audit.result = "failed";
            }
            else
            {
                audit.result = "ok";
            }
            audit.timeobject = DateTime.now().withZone(DateTimeZone.UTC).toDate().getTime();
            audit.save();
        }
        catch (Exception e)
        {
            Logger.error(lib.util.Util.getStackTraceString(e));
        }
    }


    /**
     * @param fileFormat         export format
     * @param timeZoneOffsetMins in minutes
     * @param auditList          audit log list
     */
    public ReportBuilder generateAuditReport(FileFormat fileFormat,
                                             int timeZoneOffsetMins,
                                             List<AuditLog> auditList,
                                             String locale) throws ApiException
    {
        switch (fileFormat)
        {
            case PDF:
                return new AuditLogPdf(auditList, timeZoneOffsetMins, locale);

            case XLS:
                return new AuditLogXls(auditList, timeZoneOffsetMins, locale);

            default:
                throw new ApiException("file-format-not-supported");
        }
    }
}
