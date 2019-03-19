package controllers.api;

import com.google.code.morphia.query.Query;
import com.google.gson.*;
import com.mongodb.BasicDBObject;
import controllers.interceptors.APIInterceptor;
import jobs.queries.QueryAnalyticsReports;
import jobs.queries.QueryEventsWithBinary;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.Analytics.TickerReport;
import models.*;
import models.abstracts.ServerPagedResult;
import models.events.EventWithBinary;
import models.labels.DeviceLabel;
import models.node.NodeCamera;
import models.node.NodeObject;
import models.transportobjects.EventBinaryTransport;
import models.transportobjects.LabelDataTransport;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import platform.DeviceManager;
import platform.ReportManager;
import platform.content.ftp.FTPDetails;
import platform.content.ftp.FTPHandler;
import platform.content.ftp.FTPResult;
import platform.dashboard.DashboardManager;
import platform.dashboard.DashboardSummary;
import platform.dashboard.DashboardTransport;
import platform.db.QueryHelper;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceGroup;
import platform.events.EventType;
import platform.label.LabelManager;
import platform.reports.AnalyticsReport;
import platform.reports.AnalyticsTracksData.Track;
import platform.reports.AudienceProfilingAnalyticsReport.AudienceProfilingReport;
import platform.reports.CameraTamperingAnalyticsReport.CameraTamperingReport;
import platform.reports.CrowdDensityAnalyticsReport.CrowdDensityReport;
import platform.reports.EventReport;
import platform.reports.FaceIndexingAnalyticsReport.FaceIndexingReport;
import platform.reports.IntrusionAnalyticsReport.IntrusionReport;
import platform.reports.LoiteringAnalyticsReport.LoiteringReport;
import platform.reports.PeopleCountingAnalyticsReport.PeopleCountingReport;
import platform.reports.PerimeterAnalyticsReport.PerimeterReport;
import platform.reports.ReportQuery;
import platform.reports.TickerAnalyticsReport;
import platform.reports.TrafficFlowAnalyticsReport.TrafficFlowReport;
import platform.reports.TripWireCountingAnalyticsReport.TripWireReport;
import platform.time.UtcPeriod;
import play.Logger;
import play.modules.morphia.Model.MorphiaQuery;
import play.mvc.With;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author KAI Square
 * @sectiontitle Data Access
 * @sectiondesc APIs for managing/retrieving report data
 * @publicapi
 */

@With(APIInterceptor.class)
public class Reports extends APIController
{
    private static class POSSerializer implements JsonSerializer<models.PosDataReport>
    {
        public JsonElement serialize(models.PosDataReport pos, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject e = new JsonObject();
//            e.addProperty("id", event.eventId);
            e.addProperty("bucketId", pos.bucket);
            e.addProperty("name", pos.name);
            e.addProperty("parsetType", pos.parserType);
            JsonObject sales = new JsonObject();
            sales.addProperty("amount", pos.sales.amount);
            sales.addProperty("count", pos.sales.count);
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            sales.addProperty("time", df.format(pos.sales.time));
            e.add("sales", sales);

            return e;
        }
    }

    /**
     * @param event-type Type of events, Comma separated values or can be single value. Mandatory.
     * @param device-id  core device id. Comma separated values or can be a single value. Optional
     * @param channel-id channel id. Comma separated values or can be a single value. Optional
     * @param skip       Pagination control, skip how many events from beginning of result? Mandatory.
     * @param take       Pagination control, return maximum how many events? Mandatory.
     * @param from       Timestamp with format ddMMyyyyHHmmss. 18122014000000
     *                   Filter to return only the events which occurred at or after this time. Optional.
     * @param to         Timestamp with format ddMMyyyyHHmmss. e.g 20122014030356
     *                   Filter to return only the events which occurred at or before this time. Optional.
     *
     * @servtitle [DEPRECATED] Returns list of events filtered according to specified parameters,
     * sorted chronologically with most recent first. Use of this API is discouraged.
     * 'getanalyticsreport' and 'getalerts' should be used where possible.
     * @httpmethod POST
     * @uri /api/{bucket}/getevents
     * @responsejson {
     * "result" = "ok",
     * "totalcount": 11139,
     * "events": [
     * {@link models.archived.ArchivedEvent}
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getevents() throws ApiException
    {
        Map response = new LinkedHashMap();
        response.put("result", "error");
        response.put("reason", "deprecated");
        renderJSON(response);
    }

    /**
     * @param parser-type Parser name. Optional
     *
     * @servtitle Get imported POS sales data (hidden from API documentation)
     * @httpmethod POST
     * @uri /api/{bucket}/listposnames
     * @responsejson {
     * "result": "ok",
     * "names":	[
     * {
     * "bucket":"2",
     * "name":POS1,
     * "parserType":type1,
     * "sales":{
     * "time": "12/12/1014 23:12:69",
     * "count": 2,
     * "amount": $30000
     * }
     * },
     * {
     * "bucket":"3",
     * "name":POS2,
     * "parserType":type2,
     * "sales":{
     * "time": "11/12/1014 23:12:69",
     * "count": 1,
     * "amount": $50000
     * }
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void listposnames(String bucket)
    {
        ResultMap map = new ResultMap();
        try
        {
            String currentBucketId = getCallerBucketId();
            String parserType = readApiParameter("parser-type", false);

            MorphiaQuery query = PosDataReport.q();
            query.field("bucket").equal(currentBucketId);

            if (!"".equals(parserType))
            {
                query.field("parserType").equal(parserType);
            }

            List<BasicDBObject> dbobj = query.group("bucket name", query.getQueryObject(), "function(curr, result){}", "");

            map.put("result", "ok");
            map.put("names", dbobj);

        }
        catch (Exception e)
        {
            respondError(e);
        }

        renderJSON(map);
    }

    /**
     * @param from        Timestamp with format ddMMyyyyHHmmss.
     *                    Filter to return only the events which occurred at or after this time.
     * @param to          Timestamp with format ddMMyyyyHHmmss.
     *                    Filter to return only the events which occurred at or before this time.
     * @param name        Specific name (store name) of POS data
     * @param parser-type Parser name (OPTIONAL)
     *
     * @servtitle Get imported POS sales data (hidden from API documentation)
     * @httpmethod POST
     * @uri /api/{bucket}/getpossalesreport
     * @responsejson {
     * "result": "ok",
     * "sales":	[
     * {
     * "bucket":"2",
     * "name":POS1,
     * "parserType":type1,
     * "sales":{
     * "time": "12/12/1014 23:12:69",
     * "count": 2,
     * "amount": $30000
     * }
     * },
     * {
     * "bucket":"3",
     * "name":POS2,
     * "parserType":type2,
     * "sales":{
     * "time": "11/12/1014 23:12:69",
     * "count": 1,
     * "amount": $50000
     * }
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getpossalesreport(String bucket)
    {
        ResultMap map = new ResultMap();
        try
        {
            String currentBucketId = getCallerBucketId();
            String from = readApiParameter("from", false);
            String to = readApiParameter("to", false);
            String name = readApiParameter("name", false);
            String parserType = readApiParameter("parser-type", false);

            Date dtFrom = null;
            Date dtTo = null;
            try
            {
                if (!from.isEmpty())
                {
                    dtFrom = new SimpleDateFormat("ddMMyyyyHHmmss").parse(from);
                }
                if (!to.isEmpty())
                {
                    dtTo = new SimpleDateFormat("ddMMyyyyHHmmss").parse(to);
                }
            }
            catch (Exception e)
            {
                throw new ApiException("invalid-time-range");
            }

            ReportManager RM = ReportManager.getInstance();
            List<PosDataReport> POSData;
            POSData = RM.getPOSSalesData(currentBucketId, dtFrom, dtTo, name, parserType);

            map.put("result", "ok");
            map.put("sales", POSData);
            renderJSON(map, new POSSerializer());
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param from    Timestamp with format ddMMyyyyHHmmss.
     * @param to      Timestamp with format ddMMyyyyHHmmss.
     * @param POSName Specific name (store name) of POS data
     * @param POSData JSON string of POS sales Data
     *                {
     *                time: 03122014181500,
     *                amount: 3000,
     *                count: 12
     *                },
     *                {
     *                time: 03122014191500,
     *                amount: 6000,
     *                count: 15
     *                }
     *
     * @servtitle Get imported POS sales data (hidden from API documentation)
     * @httpmethod POST
     * @uri /api/{bucket}/getpossalesreport
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatepossalesdata(String bucket) throws ApiException
    {
        ResultMap map = new ResultMap();
        try
        {
            String currentBucketId = getCallerBucketId();
            String from = readApiParameter("from", true);
            String to = readApiParameter("to", true);
            String name = readApiParameter("POSName", true);
            String POSData = readApiParameter("POSData", true);

            Date dtFrom = null;
            Date dtTo = null;
            try
            {
                if (!from.isEmpty())
                {
                    dtFrom = new SimpleDateFormat("ddMMyyyyHHmmss").parse(from);
                }
                if (!to.isEmpty())
                {
                    dtTo = new SimpleDateFormat("ddMMyyyyHHmmss").parse(to);
                }
            }
            catch (Exception e)
            {
                throw new ApiException("invalid-time-range");
            }

            ReportManager RM = ReportManager.getInstance();
            RM.updatePOSSalesData(currentBucketId, dtFrom, dtTo, name, POSData);

            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param event-type      report type e.g event-vca-people-counting. Mandatory
     * @param date-from       start date of report query. e.g 25112014160000. Mandatory
     * @param date-to         start date of report query. e.g 25112014160000. Mandatory
     * @param device-selected JSON string of device selected
     *                        device-selected: [
     *                        {
     *                        label: "testLabel",
     *                        deviceId: 1,
     *                        channelId: 1
     *                        },
     *                        {
     *                        label: "testLabel",
     *                        deviceId: 2,
     *                        channelId: null
     *                        }
     *                        ]
     *
     * @servtitle Save generated report history of user (hidden from API documentation)
     * @httpmethod POST
     * @uri /api/{bucket}/savereportqueryhistory
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void savereportqueryhistory(String bucket) throws ApiException
    {
        Map map = new ResultMap();
        try
        {
            String currentUserId = getCallerUserId();
            String eventType = readApiParameter("event-type", true);
            String fromStr = readApiParameter("date-from", true);
            String toStr = readApiParameter("date-to", true);
            String deviceSelected = readApiParameter("device-selected", true);

            ReportManager.getInstance().saveReportQueryHistory(currentUserId, eventType, deviceSelected, fromStr, toStr);
            map.put("result", "ok");
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param event-type report type e.g event-vca-people-counting. Mandatory
     *
     * @servtitle Get generated report history of user (hidden from API documentation)
     * @httpmethod POST
     * @uri /api/{bucket}/getreportqueryhistory
     * @responsejson {
     * "result": "ok",
     * "query" : {
     * "event-type": "event-vca-people-counting",
     * "device-selected": [
     * {
     * "label": "testLabel1",
     * "deviceId": 1,
     * "channelId": 1
     * },
     * {
     * "label": "testLabel1",
     * "deviceId": 1,
     * "channelId": 1
     * }
     * ]
     * "dateFrom": 25112014160000
     * "dateTo": 03122014155959
     * }
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getreportqueryhistory(String bucket) throws ApiException
    {
        Map map = new ResultMap();
        try
        {
            String currentUserId = getCallerUserId();
            String eventType = readApiParameter("event-type", true);

            ReportQueryHistory query = ReportManager.getInstance().getReportQueryHistory(currentUserId, eventType);
            map.put("result", "ok");
            map.put("query", query);
            renderJSON(map);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param days             [Mandatory] last total days to calculate summary (Doesn't accept <=0 value)                      .
     * @param time-zone-offset [Mandatory] Time zone offset in minutes
     *
     * @servtitle Returns details required to generate dashboard
     * @httpmethod GET
     * @uri /api/{bucket}/getdashboard
     * @responsejson {
     * "result": "ok",
     * "eventscount": [JSON array of no. of events per day. Length of this array will be equal to 'days' input parameter],
     * "genderprofilingcount": {
     * "female": No. of female visitors in the last 'N' days,
     * "male": No. of male visitors in the last 'N' days
     * },
     * "peopleCounting": {
     * "total": Total no. of visitors in the last 'N' days,
     * "today": No. of visitors today,
     * "counts": [No. of visitors per day for the last 'N' days. Oldest first, today's count the last element.]
     * },
     * }
     * @responsejson {
     * "result": "ok",
     * "eventscount": [
     * 7241,
     * 6499,
     * 5878,
     * 7907,
     * 7147,
     * 7207,
     * 7601
     * ],
     * "genderprofilingcount": {
     * "female": 3574,
     * "male": 3559
     * },
     * "peopleCounting": {
     * "total": 32610,
     * "today": 5047,
     * "counts": [
     * 4979,
     * 4211,
     * 3759,
     * 5052,
     * 4920,
     * 4642,
     * 5047
     * ]
     * }
     * }
     * @responsejson {
     * "result": "error",
     * "eventscount": "unknown"
     * }
     */
    public static void getdashboard()
    {
        try
        {
            String callerUserId = getCallerUserId();

            String days = readApiParameter("days", true);
            String timeZoneOffset = readApiParameter("time-zone-offset", true);

            //check
            if (!Util.isInteger(days))
            {
                throw new ApiException("invalid-days");
            }
            if (!Util.isInteger(timeZoneOffset))
            {
                throw new ApiException("invalid-time-zone-offset");
            }

            int summaryDays = Integer.parseInt(days);
            int timeZoneMinute = Integer.parseInt(timeZoneOffset);
            DashboardSummary summary = DashboardManager.getInstance().compileSummary(callerUserId, timeZoneMinute, summaryDays);

            renderJSON(new DashboardTransport(summary));
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param event-type event type. Mandatory.
     * @param device-id  core device id. Comma separated values or can be a single value. Optional
     * @param channel-id channel id. Comma separated values or can be a single value. Optional
     * @param skip       Pagination control, skip how many events from beginning of result? Mandatory.
     * @param take       Pagination control, return maximum how many events? Mandatory.
     * @param from       Timestamp with format ddMMyyyyHHmmss. 18122014000000
     *                   Filter to return only the events which occurred at or after this time. Optional.
     * @param to         Timestamp with format ddMMyyyyHHmmss. e.g 20122014030356
     *                   Filter to return only the events which occurred at or before this time. Optional.
     *
     * @servtitle Returns list of events filtered according to specified parameters,
     * sorted chronologically with most recent first (hidden from API documentation)
     * @httpmethod POST
     * @uri /api/{bucket}/geteventswithbinary
     * @responsejson {
     * "result" = "ok",
     * "totalcount": 11139,
     * "events": [
     * {@link models.events.EventWithBinary}
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void geteventswithbinary() throws ApiException
    {
        try
        {
            String callerUserId = getCallerUserId();

            String eventType = readApiParameter("event-type", true);
            String deviceIds = readApiParameter("device-id", false);
            String channelIds = readApiParameter("channel-id", false);
            String skip = readApiParameter("skip", false);
            String take = readApiParameter("take", false);
            String from = readApiParameter("from", false);
            String to = readApiParameter("to", false);

            //check event types
            String[] eventTypeArr = eventType.isEmpty() ? new String[0] : eventType.split(",");
            List<EventType> eventTypeList = new ArrayList<>();
            if (eventTypeArr.length > 0)
            {
                for (String typeString : eventTypeArr)
                {
                    EventType type = EventType.parse(typeString.trim());
                    if (type.equals(EventType.UNKNOWN))
                    {
                    	Logger.info("geteventswithbinary.........");
                        throw new ApiException("invalid-event-type");
                    }
                    eventTypeList.add(type);
                }
            }

            //check skip and take
            if (!skip.isEmpty() && !Util.isInteger(skip))
            {
                throw new ApiException("invalid-skip");
            }
            if (!take.isEmpty() && !Util.isInteger(take))
            {
                throw new ApiException("invalid-take");
            }

            List<DeviceChannelPair> cameraList = QueryHelper.asCameraList(deviceIds, channelIds);

            //use user's cameras if IDs are not provided
            if (cameraList.isEmpty())
            {
                List<MongoDevice> userDevices = DeviceManager.getInstance().getDevicesOfUser(callerUserId);
                for (MongoDevice userDevice : userDevices)
                {
                    if (userDevice.isKaiNode())
                    {
                        NodeObject nodeObject = NodeObject.findByPlatformId(userDevice.getDeviceId());
                        for (NodeCamera camera : nodeObject.getCameras())
                        {
                            cameraList.add(new DeviceChannelPair(nodeObject.getNodeCoreDeviceId(), camera.nodeCoreDeviceId));
                        }
                    }
                    else
                    {
                        cameraList.add(new DeviceChannelPair(userDevice.getCoreDeviceId(), "0"));
                    }
                }
            }

            //query
            QueryEventsWithBinary queryJob = new QueryEventsWithBinary(eventTypeList, cameraList, from, to, skip, take);

            ServerPagedResult<EventWithBinary> pagedResult = await(queryJob.now());

            //convert db objects to transport objects
            List<EventBinaryTransport> transports = new ArrayList<>();
            for (EventWithBinary event : pagedResult.getResultsForOnePage())
            {
                transports.add(new EventBinaryTransport(event));
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("totalcount", pagedResult.getTotalCount());
            response.put("events", transports);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle Returns POS import settings of the caller's bucket (hidden from API documentation)
     * @httpmethod POST
     * @uri /api/{bucket}/getpossettings
     * @responsejson {
     * "result" : "ok",
     * "settings": {@link models.POSImportSettings}
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getpossettings()
    {
        try
        {
            String callerBucketId = getCallerBucketId();
            POSImportSettings settings = POSImportSettings.of(Long.parseLong(callerBucketId));

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("settings", settings);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param import-enabled true/false
     * @param ftp-details    json string of ftp settings. {@link platform.content.ftp.FTPDetails}
     *
     * @servtitle updates POS import settings of the caller's bucket (hidden from API documentation)
     * @httpmethod POST
     * @uri /api/{bucket}/updatepossettings
     * @responsejson {
     * "result" : "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatepossettings()
    {
        try
        {
            String callerBucketId = getCallerBucketId();

            //inputs
            boolean importEnabled = asBoolean(readApiParameter("import-enabled", true));
            String ftpJson = readApiParameter("ftp-details", false);

            //find settings
            POSImportSettings settings = POSImportSettings.of(Long.parseLong(callerBucketId));
            settings.setEnabled(importEnabled);

            if (importEnabled)
            {
                //ftp
                if (Util.isNullOrEmpty(ftpJson))
                {
                    throw new ApiException("missing-ftp-details");
                }
                FTPDetails ftpDetails = new Gson().fromJson(ftpJson, FTPDetails.class);
                FTPResult ftpResult = (new FTPHandler(ftpDetails)).validate();
                if (!ftpResult.equals(FTPResult.OK))
                {
                    throw new ApiException(ftpResult.getMessage());
                }

                settings.setFtpDetails(ftpDetails);
                if (settings.isDirectoryInUseByOthers())
                {
                    throw new ApiException("error-ftp-directory-conflict");
                }
            }

            settings.save();

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param from            start date string (ddMMyyyyHHmmss). Optional, last-n-days can be used instead of this.
     * @param to              end date string (ddMMyyyyHHmmss). Optional, last-n-days can be used instead of this.
     *                        'from' & 'to' are to be used as a pair - both must be specified for proper results. The
     *                        alternative to using them is to use 'last-n-days', in which case both 'from' & 'to'
     *                        can be omitted. The time range represented by 'from' and 'to' cannot be more than
     *                        366 days.
     * @param last-n-days     the number of days counting from NOW for which data should be returned, instead of from
     *                        and to. Optional, ignored if from & to are specified. If specified, it cannot be larger
     *                        than 366 days.
     * @param respond-as-file "true" if response is required as a file (Content-Disposition header is added to the
     *                        response). Optional.
     * @param from            start date string (ddMMyyyyHHmmss). Optional, last-n-days can be used instead of this.
     * @param to              end date string (ddMMyyyyHHmmss). Optional, last-n-days can be used instead of this.
     *                        'from' & 'to' are to be used as a pair - both must be specified for proper results. The
     *                        alternative to using them is to use 'last-n-days', in which case both 'from' & 'to'
     *                        can be omitted. The time range represented by 'from' and 'to' cannot be more than
     *                        366 days.
     * @param last-n-days     the number of days counting from NOW for which data should be returned, instead of from
     *                        and to. Optional, ignored if from & to are specified. If specified, it cannot be larger
     *                        than 366 days.
     * @param respond-as-file "true" if response is required as a file (Content-Disposition header is added to the
     *                        response). Optional.
     *
     * @servtitle Get data from an account, for purposes of importing the data into other applications e.g. 3rd party reporting application.
     * @httpmethod GET
     * @uri /api/{bucket}/getdata
     * @httpmethod GET
     * @uri /api/{bucket}/getdata
     * @responsejson common fields from {@link TickerReport}, the others are from each reports' instance extended {@link TickerReport}.
     * <p/>
     * <ul>
     * <li>Face Indexing and Crowd Density data is not included at the moment.</li>
     * <li>PeopleCounting: {@link PeopleCountingReport}</li>
     * <li>AudienceProfiling: {@link AudienceProfilingReport}</li>
     * <li>Intrusion: {@link IntrusionReport}</li>
     * <li>Perimeter: {@link PerimeterReport}</li>
     * <li>Loitering: {@link LoiteringReport}</li>
     * <li>CameraTempering: {@link CameraTamperingReport}</li>
     * <li>TripWire: {@link TripWireReport}</li>
     * <li>TrafficFlow: {@link TrafficFlowReport}</li>
     * </ul>
     * <p/>
     * ex:
     * {
     * "result": "ok",
     * "data":      [
     * {
     * "_id":"538f06f7da885c1433e6f4b4",
     * "_created":0,
     * "_modified":0,
     * "deviceId":"2",
     * "channelId":"1",
     * "count":1,
     * "date":"2014/06/04 11:00:00",
     * "time":1401879600000,
     * "hour":0
     * },
     * {
     * "_id":"538f0ff2da885c1433e6f597",
     * "_created":0,
     * "_modified":0,
     * "deviceId":"2",
     * "channelId":"1",
     * "count":4,
     * "date":"2014/06/04 12:00:00",
     * "time":1401883200000,
     * "hour":0
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getdata(String bucket)
    {
        ResultMap map = new ResultMap();

        try
        {
            //validate input parameters
            String callerUserId = getCallerUserId();
            String callerBucketId = getCallerBucketId();

            String from = readApiParameter("from", false);
            String to = readApiParameter("to", false);
            String lastNDays = readApiParameter("last-n-days", false);
            String respondAsFile = readApiParameter("respond-as-file", false);

            //filter device and permissions
            List<DeviceChannelPair> cameraList = new ArrayList<>();
            List<MongoDevice> userDevices = DeviceManager.getInstance().getDevicesOfUser(callerUserId);
            for (MongoDevice userDevice : userDevices)
            {
                cameraList.add(new DeviceChannelPair(userDevice.getCoreDeviceId(), null));
            }

            //no devices assigned, so no data
            if (cameraList.isEmpty())
            {
                map.put("result", "ok");
                map.put("data", new ArrayList<>());
                map.put("totalcount", 0);
                renderJSON(map);
            }

            DateTime dtFrom = null;
            DateTime dtTo = null;
            int MAX_DAYS = 366;
            //determine whether from/to pair is to be used or last-n-days. If last-n-days is to be used, then
            //calculate the dtFrom &dtTo values based on the last-n-days and current time.
            if (!Util.isNullOrEmpty(from) && !Util.isNullOrEmpty(to))
            {
                //parse the from and to timestamps
                DateTimeFormatter ddMMyyyyHHmmss = DateTimeFormat.forPattern("ddMMyyyyHHmmss");
                ddMMyyyyHHmmss.withZoneUTC();
                dtFrom = DateTime.parse(from, ddMMyyyyHHmmss);
                dtTo = DateTime.parse(to, ddMMyyyyHHmmss);
                if (dtFrom == null || dtTo == null)
                {
                    throw new ApiException("Invalid format of from or to parameter(s), must be ddMMyyyyHHmmss");
                }
                if (dtTo.minusDays(MAX_DAYS).isAfter(dtFrom))
                {
                    throw new ApiException("Data of maximum " + MAX_DAYS + " can be requested at a time");
                }
            }
            else if (!Util.isNullOrEmpty(lastNDays))
            {
                try
                {
                    int days = Integer.parseInt(lastNDays);
                    if (days <= 0)
                    {
                        throw new ApiException("Invalid value of last-n-days parameter, must be a positive integer");
                    }
                    else if (days > MAX_DAYS)
                    {
                        throw new ApiException("Data of maximum " + MAX_DAYS + " can be requested at a time");
                    }
                    dtTo = DateTime.now(DateTimeZone.UTC);
                    dtFrom = dtTo.minusDays(days);
                }
                catch (NumberFormatException exp)
                {
                    throw new ApiException("Invalid format of last-n-days parameter, must be a positive integer");
                }
            }
            else
            {
                throw new ApiException("Neither from/to nor last-n-days specified.");
            }

            //All tickers will be queried one by one, except crowd density and face indexing.
            //Those 2 are not included because the data is too vast and complex for external
            //systems to use anyway. No point exporting it as is - perhaps later we can include it
            //in a different simplified format.
            List<AnalyticsReport> reports = new ArrayList<AnalyticsReport>();
            reports.add(EventReport.getReport(EventType.VCA_PEOPLE_COUNTING));
            reports.add(EventReport.getReport(EventType.VCA_PROFILING));
            reports.add(EventReport.getReport(EventType.VCA_TRAFFIC_FLOW));
            reports.add(EventReport.getReport(EventType.VCA_INTRUSION));
            reports.add(EventReport.getReport(EventType.VCA_PERIMETER_DEFENSE));
            reports.add(EventReport.getReport(EventType.VCA_LOITERING));
            reports.add(EventReport.getReport(EventType.VCA_OBJECT_COUNTING));
            reports.add(EventReport.getReport(EventType.VCA_VIDEO_BLUR));

            //We'll hold all the data in this master list (all type of tickers combined in one list)
            List<TickerReport> tickers = new ArrayList<>();

            //Custom serializers for each of the report types, if available
            List<JsonSerializer> jsonSerializers = new ArrayList<>();

            for (AnalyticsReport report : reports)
            {
                ReportQuery reportQuery = report.query(dtFrom.toDate(), dtTo.toDate()).addDevice(cameraList);
                final Query query = reportQuery.getQuery().order("-time");
                Iterable<TickerReport> iterable = query.fetch();
                for (TickerReport tr : iterable)
                {
                    //localize the 'date' of the ticker, then add to the ticker list
                    tr.localizeTimestamp();
                    tickers.add(tr);
                }

                //report might provide customized JsonSerializers for result
                jsonSerializers.addAll(Arrays.asList(reportQuery.getJsonSerializers()));
            }

            List<DeviceLabel> labels = LabelManager.getInstance().getBucketLabels(Long.parseLong(callerBucketId));

            //if no labels set up for the account, return all the data, associating it with pseudo label "All"
            if (labels.isEmpty())
            {
                //iterate through all the tickers to serialize them as "TickerWithType" objects,
                //and massage them all into "LabelDataTransport" with pseudo-label "All"
                List<LabelDataTransport> dataByLabels = new ArrayList<>();
                LabelDataTransport data = new LabelDataTransport("All");
                for (TickerReport ticker : tickers)
                {
                    data.records.add(data.new TickerWithType(ticker.getType(), ticker));
                }
                dataByLabels.add(data);

                //finally pack the massaged data into the result map
                map.put("result", "ok");
                map.put("data", dataByLabels);
                map.put("totalcount", data.records.size());
            }
            else
            {
                // ***** Massage Data by Labels *****
                //If labels have been configured for the account, massage the data into a structure which indicates
                //the association of each data record with a label. The JSON equivalent is something like this:
                // data = [{
                //   labelName: <label name>,
                //   labelType: <label type>,
                //   records: [{
                //     type: <record type e.g. event-vca-people-counting for People Counting data row>
                //     record: {
                //       date: <date & time>,
                //       key1: value1,
                //       key2: value2
                //     }
                //   },{ more records }]
                // },{ more labels }]
                List<LabelDataTransport> dataByLabels = new ArrayList<>();
                int numRecords = 0; // to count the total number of records in the massaged data
                for (DeviceLabel label : labels)
                {
                    List<DeviceChannelPair> labelDevices = label.getCameraList();

                    //iterate through all the tickers to collect those belonging to devices of the current label
                    LabelDataTransport data = new LabelDataTransport(label);
                    for (TickerReport ticker : tickers)
                    {
                        if (labelDevices.contains(new DeviceChannelPair(ticker.deviceId, ticker.channelId)))
                        {
                            EventType tickerType = ticker.getType();
                            data.records.add(data.new TickerWithType(tickerType, ticker));
                            numRecords++;
                        }
                    }
                    dataByLabels.add(data);
                }

                //finally pack the massaged data into the result map
                map.put("result", "ok");
                map.put("data", dataByLabels);
                map.put("totalcount", numRecords);
            }

            //Return response as a JSON file if requested so. Some 3rd party systems require this.
            if (respondAsFile.equalsIgnoreCase("true"))
            {
                response.setHeader("Content-Disposition", "attachment; filename=\"data.json\"");
            }

            if (!jsonSerializers.isEmpty())
            {
                renderJSON(map, jsonSerializers.toArray(new JsonSerializer[0]));
            }
            else
            {
                renderJSON(map);
            }
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param event-type      Type of event. Mandatory
     *                        e.g event-vca-intrusion
     * @param device-groups   a list of {@link platform.devices.DeviceGroup}
     * @param device-id       Id of device. Optional
     * @param channel-id      Id of channel. Optional
     *                        if channel id is provided, report is generated combining device and channel id
     *                        if channel id is not provided, all the events generated by every channel of the specified
     *                        device is returned (as device is mandatory).
     * @param from            start date string (ddMMyyyyHHmmss)
     * @param to              end date string (ddMMyyyyHHmmss)
     * @param parameters      key-value JSON format parameters for reports (Optional, it depends on report type, each report has its own parameters for generating query)
     *                        e.g {"aggregative": true}
     *                        <p/>
     *                        FaceIndexing: none
     *                        PeopleCounting: none
     *                        AudienceProfiling: none
     *                        Intrusion: none
     *                        Perimeter: none
     *                        Loitering: none
     *                        CameraTempering: none
     *                        TripWire: none
     *                        CrowdDensity:
     *                        aggregative (boolean): true if the return data should be aggregated into one record (Default: false)
     *                        TrafficFlow: none
     * @param respond-as-file "true" if response is required as a file (Content-Disposition header is added to the response). Optional.
     *
     * @servtitle Get processed report data for specific analytics
     * @httpmethod POST
     * @uri /api/{bucket}/getanalyticsreport
     * @responsejson common fields from {@link TickerReport}, the others are from each reports' instance extended {@link TickerReport}
     * <ul>
     * <li>FaceIndexing: {@link FaceIndexingReport}</li>
     * <li>PeopleCounting: {@link PeopleCountingReport}</li>
     * <li>AudienceProfiling: {@link AudienceProfilingReport}</li>
     * <li>Intrusion: {@link IntrusionReport}</li>
     * <li>Perimeter: {@link PerimeterReport}</li>
     * <li>Loitering: {@link LoiteringReport}</li>
     * <li>CameraTempering: {@link CameraTamperingReport}</li>
     * <li>TripWire: {@link TripWireReport}</li>
     * <li>CrowdDensity: {@link CrowdDensityReport} and {@link Track} for each tracks</li>
     * <li>TrafficFlow: {@link TrafficFlowReport}</li>
     * </ul>
     * <p/>
     * eg:
     * {
     * "result": "ok",
     * "data":		[
     * {
     * "_id":"538f06f7da885c1433e6f4b4",
     * "_created":0,
     * "_modified":0,
     * "deviceId":"2",
     * "channelId":"1",
     * "count":1,
     * "date":"2014/06/04 11:00:00",
     * "time":1401879600000,
     * "hour":0
     * },
     * {
     * "_id":"538f0ff2da885c1433e6f597",
     * "_created":0,
     * "_modified":0,
     * "deviceId":"2",
     * "channelId":"1",
     * "count":4,
     * "date":"2014/06/04 12:00:00",
     * "time":1401883200000,
     * "hour":0
     * }
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getanalyticsreport()
    {
        try
        {
        	Logger.info("event-type(BEFORE PARSE):"+readApiParameter("event-type", true));
            EventType eventType = EventType.parse(readApiParameter("event-type", true));
            Logger.info("eventType:"+eventType.name());
            long from = toMilliseconds(readApiParameter("from", true));
            long to = toMilliseconds(readApiParameter("to", true));

            String deviceGroups = readApiParameter("device-groups", false);
            String deviceId = readApiParameter("device-id", false);
            String channelId = readApiParameter("channel-id", false);
            String vcaParams = readApiParameter("parameters", false);
            String respondAsFile = readApiParameter("respond-as-file", false);
            

            /**
             *
             * parse and validate
             *
             */
            List<DeviceChannelPair> cameraList = new ArrayList<>();
            if (!deviceGroups.isEmpty())
            {
                List<DeviceGroup> deviceGroupList = DeviceGroup.parseAsList(deviceGroups);
                for (DeviceGroup deviceGroup : deviceGroupList)
                {
                    cameraList.addAll(deviceGroup.getCameraList());
                }
            }
            else if (!deviceId.isEmpty())
            {
                if (!Util.isInteger(deviceId))
                {
                    throw new ApiException("invalid-device-id");
                }

                if (!channelId.isEmpty() && !Util.isInteger(channelId))
                {
                    throw new ApiException("invalid-channel-id");
                }

                //convert platform to core device id
                MongoDevice dbDvc = MongoDevice.getByPlatformId(deviceId);
                cameraList.add(new DeviceChannelPair(dbDvc.getCoreDeviceId(), channelId));
            }

            //Additional params
            Map<String, Object> additionalParams = new LinkedHashMap<>();
            if (!vcaParams.isEmpty())
            {
                additionalParams = TickerAnalyticsReport.parseAdditionalParams(vcaParams);
            }

            boolean bRespondAsFile = false;
            if (!respondAsFile.isEmpty())
            {
                if (!Util.isBoolean(respondAsFile))
                {
                    throw new ApiException("invalid-respond-as-file");
                }
                bRespondAsFile = Boolean.parseBoolean(respondAsFile);
            }

            /**
             *
             * permissions
             *
             */
            String callerUserId = getCallerUserId();
            MongoUser callerUser = MongoUser.getById(callerUserId);

            if (!cameraList.isEmpty())
            {
                for (DeviceChannelPair camera : cameraList)
                {
                    MongoDevice dbDevice = MongoDevice.getByCoreId(camera.getCoreDeviceId());
                    if (!callerUser.hasAccessToDevice(dbDevice))
                    {
                        throw new ApiException("device-access-denied");
                    }
                }
            }
            else
            {
                //use user's devices if no devices are provided
                List<MongoDevice> userDevices = DeviceManager.getInstance().getDevicesOfUser(callerUserId);
                for (MongoDevice usrDvc : userDevices)
                {
                    cameraList.add(new DeviceChannelPair(usrDvc.getCoreDeviceId(), null));
                }
            }

            UtcPeriod period = new UtcPeriod(from, to);
            QueryAnalyticsReports queryJob = new QueryAnalyticsReports(eventType, period, cameraList, additionalParams, true, 0, 0);
            ServerPagedResult<TickerReport> pagedResult = await(queryJob.now());
            Map responseMap = new LinkedHashMap();
            responseMap.put("result", "ok");
            responseMap.put("data", pagedResult.getResultsForOnePage());
            responseMap.put("totalcount", pagedResult.getTotalCount());

            if (bRespondAsFile)
            {
                response.setHeader("Content-Disposition", "attachment; filename=\"getanalyticsreport.json\"");
            }

            renderJSON(responseMap);
        }
        catch (Exception e)
        {
        	Logger.error(e.toString(), e);
            respondError(e);
        }
    }
}
