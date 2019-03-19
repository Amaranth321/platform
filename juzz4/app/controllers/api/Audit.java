package controllers.api;

import controllers.interceptors.APIInterceptor;
import jobs.queries.AuditLogJob;
import jobs.queries.AuditReportJob;
import lib.util.ResultMap;
import lib.util.exceptions.ApiException;
import models.AuditLog;
import platform.AuditManager;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import play.Logger;
import play.i18n.Lang;
import play.libs.F;
import play.mvc.With;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author KAI Square
 *         publicapi (hidden from API documentation)
 * @sectiontitle Audit Trail
 * @sectiondesc APIs to access the system audit log
 */

@With(APIInterceptor.class)
public class Audit extends APIController
{

    /**
     * @param skip Pagination control, skip how many events from beginning (offset) e.g 15
     * @param take Pagination control, take how many events to return e.g 30
     *
     * @servtitle Generate an audit report for the current session user.
     * @httpmethod POST
     * @uri /api/{bucket}/getsimpleauditreport
     * @responsejson {
     * "result": "ok",
     * "report": "list of audit report for predefined APIs like
     * login, logout, getlivevideourl, getplaybackvideourl,
     * getlivelocation ,gethistoricallocation, getevents"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "simple-audit-report-error"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getsimpleauditreport(String bucket) throws ApiException
    {
        try
        {
            String currentBucketIdStr = renderArgs.get("caller-bucket-id").toString();
            String skip = readApiParameter("skip", true);
            String take = readApiParameter("take", true);

            int iSkip = Integer.parseInt(skip);
            int iTake = Integer.parseInt(take);
            if (iSkip < 0)
            {
                throw new ApiException("skip-invalid");
            }
            if (iTake <= 0)
            {
                throw new ApiException("take-invalid");
            }

            //Simple audit report includes only the following activities
            //1) login
            //2) logout
            //3) getlivevideourl
            //4) getplaybackvideourl
            //5) getlivelocation
            //6) gethistoricallocation
            //7) getevents
            List<String> activities = new ArrayList<String>();
            activities.add("login");
            activities.add("logout");
            activities.add("getlivevideourl");
            activities.add("getplaybackvideourl");
            activities.add("getlivelocation");
            activities.add("gethistoricallocation");
            activities.add("getevents");

            //start the audit report generation job and put this HTTP request to sleep until the job is finished
            F.Promise<Map> jobResultPromise = new AuditReportJob(currentBucketIdStr, iSkip, iTake, activities).now();
            Map jobResult = await(jobResultPromise);

            //After wakeup, check job result. If non-null, send it as response, otherwise throw exception.
            if (jobResult != null)
            {
                renderJSON(jobResult);
            }
            else
            {
                throw new ApiException("simple-audit-report-error");
            }

        }
        catch (ApiException apie)
        {
            Logger.warn(apie.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception exp)
        {
            Logger.warn(exp, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(exp));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param skip         Pagination control, skip how many events from beginning (offset) e.g 15
     * @param take         Pagination control, take how many events to return e.g 30
     * @param bucket-name  Name of bucket whose audit log is to be fetch. Optional
     * @param user-name    Name of user whose audit log is to be fetch. Optional
     * @param service-name Name of service. Optional
     * @param remote-ip    Remote IP address. Optional
     * @param from         Start date
     * @param to           End date
     *
     * @servtitle Returns list of all auditlogs based on param provived.
     * @httpmethod POST
     * @uri /api/{bucket}/getauditlog
     * @responsejson {
     * "result": "ok",
     * "report": "list of audit report"
     * }
     * @responsejson {
     * "result": "error",
     * "report": "invalid-time-range"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "getauditlog-error"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getauditlog(String bucket) throws ApiException
    {
        try
        {

            String bucketName = readApiParameter("bucket-name", false);
            String userName = readApiParameter("user-name", false);
            String serviceName = readApiParameter("service-name", false);
            String remoteIp = readApiParameter("remote-ip", false);
            String skip = readApiParameter("skip", false);
            String take = readApiParameter("take", false);
            String from = readApiParameter("from", false);
            String to = readApiParameter("to", false);

            Long dtFrom = null;
            Long dtTo = null;
            
            if (!bucket.equals("superadmin")) {
            	bucketName = bucket;
            }
            
            try
            {
                if (from.isEmpty() == false)
                {
                    dtFrom = new SimpleDateFormat("ddMMyyyyHHmmss").parse(from).getTime();
                }
                if (to.isEmpty() == false)
                {
                    dtTo = new SimpleDateFormat("ddMMyyyyHHmmss").parse(to).getTime();
                }
            }
            catch (Exception e)
            {
                Logger.info("Exception: " + e.getMessage());
                Logger.error(lib.util.Util.getStackTraceString(e));
                throw new ApiException("invalid-time-range");
            }
            int offset = Integer.parseInt(skip);
            int max = Integer.parseInt(take);
//
//            //start the audit report generation job and put this HTTP request to sleep until the job is finished
            F.Promise<Map> jobResultPromise = new AuditLogJob(bucketName,
                                                              userName,
                                                              dtFrom,
                                                              dtTo,
                                                              remoteIp,
                                                              offset,
                                                              max,
                                                              serviceName).now();
            Map jobResult = await(jobResultPromise);

            //After wakeup, check job result. If non-null, send it as response, otherwise throw exception.
            if (jobResult != null)
            {
                renderJSON(jobResult);
            }
            else
            {
                throw new ApiException("getauditlog-error");
            }

        }
        catch (ApiException apie)
        {
            Logger.warn(apie.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception exp)
        {
            Logger.warn(exp, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(exp));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param audit-id Id of audit log
     *
     * @servtitle Returns details of a single auditlog.
     * @httpmethod POST
     * @uri /api/{bucket}/getauditlogdetails
     * @responsejson {
     * "result": "ok",
     * "auditlog": "audit log details"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getauditlogdetails(String bucket) throws ApiException
    {
        try
        {
            String auditId = readApiParameter("audit-id", true);

            AuditLog auditLog = AuditLog.findById(auditId);

            Map map = new ResultMap();
            map.put("result", "ok");
            map.put("auditlog", auditLog);
            renderJSON(map);

        }
        catch (ApiException apie)
        {
            Logger.warn(apie.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apie.getMessage());
            renderJSON(map);
        }
        catch (Exception exp)
        {
            Logger.warn(exp, "Exception");
            Logger.error(lib.util.Util.getStackTraceString(exp));
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", "unknown");
            renderJSON(map);
        }
    }

    /**
     * @param file-format      "xls" or "pdf".
     * @param time-zone-offset The timezone to which event timestamps should be
     *                         converted to, expressed as number of minutes offset from UTC.
     * @param bucket-name      If want to filter using bucketName specify. Optional.
     * @param user-name        If want to filter using userName specify. Optional.
     * @param service-name     If want to filter using serviceName specify. Optional.
     * @param remote-ip        If want to filter using bucketName specify. Optional.
     * @param from             Timestamp with format ddMMyyyyHHmmss. 10061014000000
     *                         Filter to return only the events which occurred at or after this time. Optional.
     * @param to               Timestamp with format ddMMyyyyHHmmss. 13061014235959
     *                         Filter to return only the events which occurred at or before this time. Optional.
     * @param skip             Pagination control, skip how many events from beginning of result? Mandatory.
     * @param take             Pagination control, return maximum how many events? Mandatory.
     *
     * @servtitle Exports audit log list as a file in the specified format
     * @httpmethod GET
     * @uri /api/{bucket}/exportauditlog
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportauditlog() throws ApiException
    {
        try
        {
            FileFormat fileFormat = FileFormat.parse(readApiParameter("file-format", true));
            String timeZoneOffset = readApiParameter("time-zone-offset", false);
            String bucketName = readApiParameter("bucket-name", false);
            String userName = readApiParameter("user-name", false);
            String serviceName = readApiParameter("service-name", false);
            String remoteIp = readApiParameter("remote-ip", false);
            String skip = readApiParameter("skip", false);
            String take = readApiParameter("take", false);
            String from = readApiParameter("from", false);
            String to = readApiParameter("to", false);

            //validate input parameters
            int iSkip = 0;
            int iTake = 0;
            if (skip.isEmpty() == false)
            {
                iSkip = Integer.parseInt(skip);
            }
            if (take.isEmpty() == false)
            {
                iTake = Integer.parseInt(take);
            }
            if (iSkip < 0)
            {
                throw new ApiException("invalid-skip");
            }
            if (iTake < 0)
            {
                throw new ApiException("invalid-take");
            }

            if (!fileFormat.equals(FileFormat.PDF) && !fileFormat.equals(FileFormat.XLS))
            {
                throw new ApiException("file-format-not-supported");
            }

            int offsetMinutes = 0;
            if (timeZoneOffset.isEmpty() == false)
            {
                try
                {
                    offsetMinutes = Integer.parseInt(timeZoneOffset);
                }
                catch (NumberFormatException e)
                {
                    Logger.error(lib.util.Util.getStackTraceString(e));
                    throw new ApiException("invalid-time-zone-offset");
                }
            }

            //Validate from and to dates
            //Validate from and to dates
            Long dtFrom = null;
            Long dtTo = null;
            try
            {
                if (from.isEmpty() == false)
                {
                    dtFrom = new SimpleDateFormat("ddMMyyyyHHmmss").parse(from).getTime();
                }
                if (to.isEmpty() == false)
                {
                    dtTo = new SimpleDateFormat("ddMMyyyyHHmmss").parse(to).getTime();
                }
            }
            catch (ParseException e)
            {
                Logger.info("Exception: " + e.getMessage());
                Logger.error(lib.util.Util.getStackTraceString(e));
                throw new ApiException("invalid-time-range");
            }

//            //start the audit report generation job and put this HTTP request to sleep until the job is finished
            F.Promise<Map> jobResultPromise = new AuditLogJob(bucketName,
                                                              userName,
                                                              dtFrom,
                                                              dtTo,
                                                              remoteIp,
                                                              iSkip,
                                                              iTake,
                                                              serviceName).now();
            Map jobResult = await(jobResultPromise);

            //After wakeup, check job result. If non-null, send it as response, otherwise throw exception.
            if (jobResult == null)
            {
                throw new ApiException("audit retrieval failed.");
            }

            List<AuditLog> auditList = (List<AuditLog>) jobResult.get("auditLogs");
            if (auditList.isEmpty())
            {
                throw new ApiException("No results.");
            }

            ReportBuilder reportBuilder = AuditManager.getInstance().generateAuditReport(fileFormat,
                                                                                         offsetMinutes,
                                                                                         auditList,
                                                                                         Lang.get());
            respondExportedFileUrl(reportBuilder);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
