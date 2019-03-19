package controllers.api;

import com.google.gson.Gson;
import com.mongodb.gridfs.GridFSDBFile;
import controllers.interceptors.APIInterceptor;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.MongoDevice;
import models.MongoUser;
import models.events.EventWithBinary;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import platform.DeviceManager;
import platform.analytics.VcaDataRequest;
import platform.analytics.VcaFeature;
import platform.analytics.VcaType;
import platform.analytics.aggregation.AggregateType;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import platform.content.export.manual.vca.csv.DeviceGroupReportCsv;
import platform.content.export.manual.vca.csv.VcaDataLogCsv;
import platform.content.export.manual.vca.pdf.*;
import platform.coreengine.CoreSession;
import platform.coreengine.StreamingManager;
import platform.db.QueryHelper;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsHelper;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceGroup;
import platform.events.EventType;
import platform.time.UtcPeriod;
import play.Logger;
import play.i18n.Lang;
import play.mvc.Http;
import play.mvc.With;

import java.util.*;

/**
 * @author KAI Square
 * @sectiontitle Reports
 * @sectiondesc APIs for generating various type of reports in various formats (e.g. JSON, PDF, XLS etc.)
 * @publicapi
 */
@With(APIInterceptor.class)
public class ReportGeneration extends APIController
{
    /**
     * @param file-format      "xls" or "pdf".
     * @param time-zone-offset The timezone to which event timestamps should be
     *                         converted to, expressed as number of minutes offset from UTC.
     * @param event-type       Event type
     * @param device-id-list   JSON string of platform device ID list
     * @param channel-id       ID of Channel
     * @param from             Timestamp with format ddMMyyyyHHmmss. 10062014000000
     *                         Filter to return only the events which occurred at or after this time. Optional.
     * @param to               Timestamp with format ddMMyyyyHHmmss. 13062014235959
     *                         Filter to return only the events which occurred at or before this time. Optional.
     *
     * @servtitle Exports events list as a file in the specified format i.e PDF or XLS
     * @httpmethod GET
     * @uri /api/{bucket}/exportdatalogs
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportdatalogs() throws ApiException
    {
        try
        {
            //required params
            EventType eventType = EventType.parse(readApiParameter("event-type", true));
            FileFormat fileFormat = FileFormat.parse(readApiParameter("file-format", true));
            long fromMillis = toMilliseconds(readApiParameter("from", true));
            long toMillis = toMilliseconds(readApiParameter("to", true));
            String timeZoneOffset = readApiParameter("time-zone-offset", true);

            //optional params
            String deviceId = readApiParameter("device-id", false);
            String channelId = readApiParameter("channel-id", false);

            if (eventType.equals(EventType.UNKNOWN))
            {
                throw new ApiException("invalid-event-type");
            }

            //timezone
            if (!Util.isInteger(timeZoneOffset))
            {
                throw new ApiException("invalid-time-zone-offset");
            }
            int offsetMinutes = Integer.parseInt(timeZoneOffset);

            //parse camera list
            List<DeviceChannelPair> cameraList = QueryHelper.asCameraList(deviceId, channelId);
            String callerUserId = getCallerUserId();
            MongoUser callerUser = MongoUser.getById(getCallerUserId());

            //if no camera specified, filter by user's devices
            if (cameraList.isEmpty())
            {
                List<MongoDevice> userDevices = DeviceManager.getInstance().getDevicesOfUser(callerUserId);
                for (MongoDevice userDevice : userDevices)
                {
                    cameraList.add(new DeviceChannelPair(userDevice.getCoreDeviceId(), null));
                }
            }
            else
            {
                //check permissions
                for (DeviceChannelPair deviceChannelPair : cameraList)
                {
                    MongoDevice device = MongoDevice.getByCoreId(deviceChannelPair.getCoreDeviceId());
                    if (!callerUser.hasAccessToDevice(device))
                    {
                        throw new ApiException("device-access-denied");
                    }
                }
            }

            //report info
            UtcPeriod period = new UtcPeriod(fromMillis, toMillis);
            VcaDataRequest dataRequest = new VcaDataRequest(eventType, period, cameraList);

            //generate
            ReportBuilder reportBuilder;
            if (fileFormat.equals(FileFormat.CSV))
            {
                reportBuilder = new VcaDataLogCsv(dataRequest, offsetMinutes);
            }
            else if (fileFormat.equals(FileFormat.PDF))
            {
                reportBuilder = new VcaDataLogPdf(dataRequest, offsetMinutes, Lang.get());
            }
            else
            {
                throw new ApiException("invalid-file-format");
            }

            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param time-zone-offset The timezone to which event timestamps should be. Mandatory
     *                         converted to, expressed as number of minutes offset from UTC. Mandatory
     * @param base64-image     The SVG data representing of chart to be exported. Mandatory
     * @param report-info      Json string containing search query info. Mandatory
     *                         {
     *                         event-type: 	Type of event
     *                         site-name: 		Comma seperated device-camera/ group name,
     *                         from: 			Events which occurred at or after this time,
     *                         to:				Events which occurred at or before this time,
     *                         total-results:	Total events on specified time frame
     *                         }
     *
     * @servtitle Generates a traffic flow report in PDF format and returns its downloadable URL
     * @httpmethod POST
     * @uri /api/{bucket}/exporttrafficflowpdf
     * @responsejson {
     * "result": "ok",
     * "download-url": "url-link"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exporttrafficflowpdf() throws ApiException
    {
        try
        {
            String timeZoneOffset = readApiParameter("time-zone-offset", false);
            String base64String = readApiParameter("base64-image", true);
            String reportInfo = readApiParameter("report-info", true);

            int offsetMinutes = 0;
            if (!timeZoneOffset.isEmpty())
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

            Map<String, String> infoMap = new LinkedHashMap<>();
            infoMap = new Gson().fromJson(reportInfo, infoMap.getClass());

            //generate
            VcaPdfData pdfData = new VcaPdfData(infoMap, offsetMinutes, Lang.get());
            ReportBuilder reportBuilder = new TrafficFlowPdf(pdfData, base64String);
            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param time-zone-offset The timezone to which event timestamps should be. Mandatory
     *                         converted to, expressed as number of minutes offset from UTC. Mandatory
     * @param base64-image     The SVG data representing of chart to be exported. Mandatory
     * @param svg-image        The SVG data representing of chart to be exported. optional
     * @param report-info      Json string containing search query info. Mandatory
     *                         {
     *                         event-type: 	Type of event here its event-vca-audienceprofiling
     *                         site-name: 		Comma seperated device-camera/ group name,
     *                         from: 			Events which occurred at or after this time,
     *                         to:				Events which occurred at or before this time,
     *                         total-results:	Total events on specified time frame
     *                         }
     *
     * @servtitle Generates a crowd detection report in PDF format and returns its downloadable URL
     * @httpmethod POST
     * @uri /api/{bucket}/exportcrowddensitypdf
     * @responsejson {
     * "result": "ok",
     * "download-url": "/public/files/tmp/13Jun2014175343094.pdf"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportcrowddensitypdf() throws ApiException
    {
        try
        {
            String sessionKey = readApiParameter("session-key", true);
            String deviceId = readApiParameter("device-id", true);
            String channelId = readApiParameter("channel-id", true);
            String timeZoneOffset = readApiParameter("time-zone-offset", false);
            String base64String = readApiParameter("base64-image", true);
            String base64RegionString = readApiParameter("base64-region-image", false);
            String svgImage = readApiParameter("svg-image", false);
            String reportInfo = readApiParameter("report-info", true);

            MongoDevice device = MongoDevice.getByPlatformId(deviceId);
            if (device == null)
            {
                throw new ApiException("no-device");
            }

            int offsetMinutes = 0;
            if (!timeZoneOffset.isEmpty())
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
            Map<String, String> infoMap = new LinkedHashMap<>();
            infoMap = new Gson().fromJson(reportInfo, infoMap.getClass());

            DeviceChannelPair camera = new DeviceChannelPair(device.getCoreDeviceId(), channelId);
            CoreSession coreSession = StreamingManager.getInstance().getCameraSnapshot(camera, 300, request.remoteAddress);
            byte[] liveSnapshotBytes = Util.imageUrlToBytes(coreSession.getUrlList().get(0));
            VcaPdfData pdfData = new VcaPdfData(infoMap, offsetMinutes, Lang.get());
            ReportBuilder reportBuilder = new CrowdDensityPdf(pdfData, liveSnapshotBytes, base64String, base64RegionString, svgImage);

            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param time-zone-offset The timezone to which event timestamps should be
     *                         converted to, expressed as number of minutes offset from UTC. Mandatory
     * @param svg-string       The SVG data representing the chart to be exported. Mandatory
     * @param report-info      Json string containing search query info. All the fields inside json are Mandatory
     *                         {
     *                         event-type: 	Type of event,
     *                         site-name: 		Comma seperated device-camera/ group name,
     *                         from: 			Events which occurred at or after this time,
     *                         to:				Events which occurred at or before this time,
     *                         total-results:	Total events on specified time frame
     *                         }
     *
     * @servtitle Generates a people counting chart report (PDF file)
     * @httpmethod GET
     * @uri /api/{bucket}/exportpeoplecountingpdf
     * @responsejson {
     * "result": "ok",
     * "download-url": "/public/files/tmp/13Jun2014175343094.pdf"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportpeoplecountingpdf() throws ApiException
    {
        try
        {
            String timeZoneOffset = readApiParameter("time-zone-offset", false);
            String svgString = readApiParameter("svg-string", true);
            String reportInfo = readApiParameter("report-info", true);

            int offsetMinutes = 0;
            if (!timeZoneOffset.isEmpty())
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

            if (svgString.isEmpty())
            {
                throw new ApiException("missing-svg-strings");
            }
            if (reportInfo.isEmpty())
            {
                throw new ApiException("missing-chart-info");
            }

            Map<String, String> infoMap = new LinkedHashMap<>();
            infoMap = new Gson().fromJson(reportInfo, infoMap.getClass());
            VcaPdfData pdfData = new VcaPdfData(infoMap, offsetMinutes, Lang.get());

            //generate
            ReportBuilder reportBuilder = new PeopleCountingPdf(pdfData, svgString);
            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param time-zone-offset The timezone to which event timestamps should be
     *                         converted to, expressed as number of minutes offset from UTC. Mandatory
     * @param svg-string1      The SVG data representing of donut chart to be exported. Mandatory
     * @param svg-string2      The SVG data representing of area chart to be exported. Mandatory
     * @param report-info      Json string containing search query info. All the fields inside json are Mandatory
     *                         {
     *                         event-type: 	Type of event here its event-vca-audienceprofiling,
     *                         site-name: 		Comma seperated device-camera/ group name,
     *                         from: 			Events which occurred at or after this time,
     *                         to:				Events which occurred at or before this time,
     *                         total-results:	Total events on specified time frame
     *                         }
     *
     * @servtitle Generates a single chart report (PDF file) for audience profiling and returns its downloadable URL
     * @httpmethod POST
     * @uri /api/{bucket}/exportaudienceprofilingpdf
     * @responsejson {
     * "result": "ok",
     * "download-url": "/public/files/tmp/13Jun2014175343094.pdf"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportaudienceprofilingpdf() throws ApiException
    {
        try
        {
            String timeZoneOffset = readApiParameter("time-zone-offset", false);
            String svgString1 = readApiParameter("svg-string1", true);
            String svgString2 = readApiParameter("svg-string2", true);
            String reportInfo = readApiParameter("report-info", true);

            int offsetMinutes = 0;
            if (!timeZoneOffset.isEmpty())
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

            List<String> donutSvgList = new ArrayList<>();
            if (!Util.isNullOrEmpty(svgString1))
            {
                String[] donutArr = svgString1.split("</svg>,");
                donutSvgList = Arrays.asList(donutArr);
            }

            List<String> areaSvgList = new ArrayList<>();
            if (!Util.isNullOrEmpty(svgString2))
            {
                String[] areaArr = svgString2.split("</svg>,");
                areaSvgList = Arrays.asList(areaArr);
            }

            Map<String, String> infoMap = new LinkedHashMap<>();
            infoMap = new Gson().fromJson(reportInfo, infoMap.getClass());
            VcaPdfData pdfData = new VcaPdfData(infoMap, offsetMinutes, Lang.get());

            ReportBuilder reportBuilder = new AudienceProfilingPdf(pdfData, donutSvgList, areaSvgList);
            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param time-zone-offset The timezone to which event timestamps should be
     *                         converted to, expressed as number of minutes offset from UTC. Mandatory
     * @param svg-string       The SVG data representing the chart to be exported. Mandatory
     * @param event-ids        Comma seperated event ids. Mandatory
     * @param report-info      Json string containing search query info. All the fields inside json are Mandatory
     *                         {
     *                         event-type: 	Type of event here should be face indexing,
     *                         site-name: 		Comma seperated device-camera/ group name,
     *                         from: 			Events which occurred at or after this time,
     *                         to:				Events which occurred at or before this time,
     *                         total-results:	Total events on specified time frame
     *                         }
     *
     * @servtitle Generates a face indexing report in PDF format and returns its downloadable URL
     * @httpmethod POST
     * @uri /api/{bucket}/exportfaceindexingpdf
     * @responsejson {
     * "result": "ok",
     * "download-url": "/public/files/tmp/13Jun2014175343094.pdf"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportfaceindexingpdf() throws ApiException
    {
        try
        {
            String timeZoneOffset = readApiParameter("time-zone-offset", false);
            String svgString = readApiParameter("svg-string", false);
            String reportInfo = readApiParameter("report-info", true);
            String eventIds = readApiParameter("event-ids", true);

            int offsetMinutes = 0;
            if (!timeZoneOffset.isEmpty())
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

            if (Util.isNullOrEmpty(svgString))
            {
                svgString = null;
            }
            if (Util.isNullOrEmpty(reportInfo))
            {
                throw new ApiException("missing-chart-info");
            }
            if (Util.isNullOrEmpty(eventIds))
            {
                throw new ApiException("missing-eventIds");
            }

            Map<String, String> infoMap = new LinkedHashMap<>();
            infoMap = new Gson().fromJson(reportInfo, infoMap.getClass());

            List<String> eventIdList = new ArrayList<>();
            eventIdList = new Gson().fromJson(eventIds, eventIdList.getClass());

            VcaPdfData pdfData = new VcaPdfData(infoMap, offsetMinutes, Lang.get());
            ReportBuilder reportBuilder = new FaceIndexingPdf(pdfData, svgString, eventIdList, Lang.get());
            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param time-zone-offset The timezone to which event timestamps should be
     *                         converted to, expressed as number of minutes offset from UTC. Mandatory
     * @param svg-string       The SVG data representing the chart to be exported. Mandatory
     * @param report-info      Json string containing search query info. All the fields inside json are Mandatory
     *                         {
     *                         event-type: 	Type of event,
     *                         site-name: 		Comma seperated device-camera/ group name,
     *                         from: 			Events which occurred at or after this time,
     *                         to:				Events which occurred at or before this time,
     *                         total-results:	Total events on specified time frame
     *                         }
     *
     * @servtitle Generates a single chart report (PDF file) and returns its downloadable URL
     * @httpmethod POST
     * @uri /api/{bucket}/exportvcasecuritypdf
     * @responsejson {
     * "result": "ok",
     * "download-url": "/public/files/tmp/13Jun2014175343094.pdf"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportvcasecuritypdf() throws ApiException
    {
        try
        {
            String timeZoneOffset = readApiParameter("time-zone-offset", false);
            String svgString = readApiParameter("svg-string", true);
            String reportInfo = readApiParameter("report-info", true);

            int offsetMinutes = 0;
            if (!timeZoneOffset.isEmpty())
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

            Map<String, String> infoMap = new LinkedHashMap<>();
            infoMap = new Gson().fromJson(reportInfo, infoMap.getClass());

            VcaPdfData pdfData = new VcaPdfData(infoMap, offsetMinutes, Lang.get());
            ReportBuilder reportBuilder = new VcaSecurityPdf(pdfData, svgString);
            respondExportedFileUrl(reportBuilder);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param event-id The id of event. Mandatory
     *
     * @servtitle Returns event blob data
     * @httpmethod POST
     * @uri /api/{bucket}/geteventbinarydata
     * @responsejson {
     * "result": "ok",
     * "image-base64": <i>MIME type image file</i>
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unavailable"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void geteventbinarydata() throws ApiException
    {
        try
        {
            //validate input parameters
            String eventId = readApiParameter("event-id", true);

            EventWithBinary binaryData = EventWithBinary.find(eventId);
            if (binaryData == null)
            {
                throw new ApiException("no-binary-data");
            }

            //grid fs file
            GridFsDetails fsDetails = binaryData.getFileDetails();
            GridFSDBFile file = GridFsHelper.getGridFSDBFile(fsDetails);
            Map map = new ResultMap();
            Http.Header userAgent = request.current().headers.get("user-agent");
            if (userAgent != null &&
                (userAgent.toString().toLowerCase().contains("android") ||
                 userAgent.toString().toLowerCase().contains("iphone") ||
                 userAgent.toString().toLowerCase().contains("ipad") ||
                 userAgent.toString().toLowerCase().contains("ipod")))
            {
                response.setContentTypeIfNotSet(file.getContentType());
                response.setHeader("Content-Length", String.valueOf(file.getLength())); //must set
                response.cacheFor("24h"); //let this be cached, it's not going to change
                renderBinary(file.getInputStream(), file.getFilename());
            }
            else
            {
                byte[] bytes = IOUtils.toByteArray(file.getInputStream());
                map.put("result", "ok");
                map.put("image-base64", Base64.encodeBase64String(bytes));
                response.cacheFor("1h"); //let this be cached, it's not going to change
                renderJSON(map);
            }
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param event-type       The event type of that report eg: event-vca-audienceprofiling
     * @param time-zone-offset The timezone to which event timestamps should be. Mandatory
     *                         converted to, expressed as number of minutes offset from UTC. Mandatory
     * @param base-unit        Time representation on report. e.g hours/days/weeks/months
     * @param from             Timestamp with format ddMMyyyyHHmmss. 10061014000000
     *                         Filter to return only the events which occurred at or after this time. Mandatory.
     * @param to               Timestamp with format ddMMyyyyHHmmss. 13061014235959
     *                         Filter to return only the events which occurred at or before this time. Mandatory.
     * @param selected-groups  Json string containing search query info. Mandatory
     *                         [{
     *                         "groupName":"All",
     *                         "type":"labels",
     *                         "devicePairs":[
     *                         {"coreDeviceId":"227","channelId":"2"},
     *                         {"coreDeviceId":"227","channelId":"3"},
     *                         {"coreDeviceId":"227","channelId":"4"}]
     *                         },{
     *                         "groupName":"Node Demo 1",
     *                         "type":"devices",
     *                         "devicePairs":[
     *                         {"coreDeviceId":"227","channelId":"2"}]
     *                         }]
     *
     * @servtitle Generates aggregated vca report (CSV file) and returns its download URL
     * @httpmethod POST
     * @uri /api/{bucket}/exportaggregatedcsvreport
     * @responsejson {
     * "result": "ok",
     * "download-url": "/public/files/tmp/13Jun2014175343094.xls"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportaggregatedcsvreport()
    {
        try
        {
            String eventTypeName = readApiParameter("event-type", true);
            List<DeviceGroup> deviceGroups = DeviceGroup.parseAsList(readApiParameter("selected-groups", true));
            long from = toMilliseconds(readApiParameter("from", true));
            long to = toMilliseconds(readApiParameter("to", true));
            String timeZoneOffset = readApiParameter("time-zone-offset", true);
            AggregateType aggregateType = AggregateType.parse(readApiParameter("base-unit", true));

            /**
             *
             * Validations
             *
             */
            EventType eventType = EventType.parse(eventTypeName);
            if (eventType.in(EventType.UNKNOWN,
                             EventType.VCA_TRAFFIC_FLOW,
                             EventType.VCA_CROWD_DETECTION,
                             EventType.VCA_FACE_INDEXING))
            {
            	Logger.info("NOT INCLUDED!!!!!");
                throw new ApiException("invalid-event-type");
            }

            if (!Util.isInteger(timeZoneOffset))
            {
                throw new ApiException("invalid-time-zone-offset");
            }

            /**
             *
             * permissions
             *
             */
            String callerUserId = getCallerUserId();
            MongoUser callerUser = MongoUser.getById(callerUserId);
            VcaFeature feature = VcaType.of(eventType).getReportFeature();
            if (!callerUser.hasAccessToVcaFeature(feature))
            {
                throw new ApiException("feature-access-denied");
            }

            for (DeviceGroup deviceGroup : deviceGroups)
            {
                for (DeviceChannelPair camera : deviceGroup.getCameraList())
                {
                    MongoDevice dbDevice = MongoDevice.getByCoreId(camera.getCoreDeviceId());
                    if (!callerUser.hasAccessToDevice(dbDevice))
                    {
                        throw new ApiException("device-access-denied");
                    }
                }
            }

            //start generation
            UtcPeriod period = new UtcPeriod(from, to);
            int tzOffsetMins = Integer.parseInt(timeZoneOffset);

            DeviceGroupReportCsv report = new DeviceGroupReportCsv(eventType, period, tzOffsetMins, aggregateType, deviceGroups);
            respondExportedFileUrl(report);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
}
