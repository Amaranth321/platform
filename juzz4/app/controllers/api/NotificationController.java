package controllers.api;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.kaisquare.sync.CommandType;
import com.mongodb.gridfs.GridFSDBFile;
import controllers.interceptors.APIInterceptor;
import jobs.queries.QuerySentLabelNotifications;
import jobs.queries.QuerySentNotifications;
import lib.util.ResultMap;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.*;
import models.abstracts.ServerPagedResult;
import models.labels.DeviceLabel;
import models.labels.LabelStore;
import models.notification.*;
import models.transportobjects.CameraNotificationTransport;
import models.transportobjects.LabelNotificationTransport;
import platform.BucketManager;
import platform.DeviceManager;
import platform.NotificationManager;
import platform.NotificationManager.NotificationToken;
import platform.analytics.VcaFeature;
import platform.analytics.VcaType;
import platform.analytics.occupancy.OccupancyLimit;
import platform.content.FileFormat;
import platform.content.export.ReportBuilder;
import platform.content.export.manual.AlertsCsv;
import platform.content.export.manual.AlertsPdf;
import platform.db.cache.CacheClient;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsHelper;
import platform.devices.DeviceChannelPair;
import platform.events.EventManager;
import platform.events.EventType;
import platform.label.LabelManager;
import platform.label.LabelType;
import platform.notification.NotifyMethod;
import platform.time.UtcPeriod;
import play.Logger;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.F;
import play.mvc.With;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author KAI Square
 * @sectiontitle Notifications (Web and Mobile)
 * @sectiondesc APIs to receive comet style event notifications as well as push notifications via APNS and GCM.
 * @publicapi
 */

@With(APIInterceptor.class)
public class NotificationController extends APIController
{
    private static final Object NOTI_CHANGE_LOCK = new Object();

    private static void checkBucketAccess(MongoBucket targetBucket, boolean mustBeChild)
    {
        try
        {
            String callerBucketId = getCallerBucketId();
            MongoBucket callerBucket = MongoBucket.getById(callerBucketId);

            List<MongoBucket> meAndChildren = BucketManager.getInstance().getThisAndDescendants(callerBucketId);
            if (mustBeChild)
            {
                meAndChildren.remove(callerBucket);
            }

            if (!meAndChildren.contains(targetBucket))
            {
                throw new ApiException("msg-no-rights-to-buckets");
            }
        }
        catch (ApiException apiE)
        {
            Logger.error(apiE.getMessage());
            Map map = new ResultMap();
            map.put("result", "error");
            map.put("reason", apiE.getMessage());
            renderJSON(map);
        }
    }

    /**
     * @servtitle Comet style API call to receive notifications; returns when an event occurs,
     * or after timeout (currently 20 seconds), whichever happens first
     * @httpmethod GET
     * @uri /api/{bucket}/recvcometnotification
     * @responsejson {
     * "result":"ok",
     * "event":"{\"id\":\"53abcdb460ed4407c955a2ba\",\"data\":\"{\\\"time\\\":1403768244205}\",
     * \"type\":\"event-passive-infrared\",\"time\":\"26/06/2014 07:37:24\",\"deviceId\":\"4\",
     * \"channelId\":\"1\",\"binaryData\":\"[B@18319eb7\",\"deviceName\":\"Super Node Device 2\"}"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "retry"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void recvcometnotification() throws ApiException
    {
        //load the User object
        String currentUserId = getCallerUserId();
        NotificationToken token = null;
        try
        {
            String event = null;
            //start "wait for event" job for this user
            token = NotificationManager.getManager().registerUser(currentUserId);//new jobs.WaitForEvent(currentUserId).now();
            F.Promise<String> promise = token.getPromise();

            //Put this request to sleep until this user has an event.
            //The request processing continues as soon as an event is received,
            //which is then sent as response to the request.
            event = await(promise);

            Map map = new ResultMap();
            if (event.isEmpty() || event.equals(""))
            {
                map.put("result", "error");
                map.put("reason", "retry");
            }
            else
            {
                map.put("result", "ok");
                map.put("event", event);
            }
            renderJSON(map);

        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id bucket id. Mandatory
     *
     * @servtitle returns the current alert settings for each event type
     * @httpmethod POST
     * @uri /api/{bucket}/getbucketnotificationsettings
     * @responsejson {
     * "result" : "ok",
     * "settings" : [
     * {@link models.notification.BucketNotificationSettings}
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getbucketnotificationsettings()
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            //access
            checkBucketAccess(targetBucket, false);

            BucketNotificationSettings settings = targetBucket.getNotificationSettings();
            if (settings == null)
            {
                throw new Exception("bucket does not exist");
            }

            //convert to map for transport
            Map<EventType, BucketNotificationSettings.EventTypeSettings> settingsMap = new LinkedHashMap<>();
            for (EventType eventType : settings.getSupportedEventTypes())
            {
                //hide types with no vca report feature assigned to bucket
                if (eventType.isVcaEvent())
                {
                    VcaFeature vcaFeature = VcaType.of(eventType).getReportFeature();
                    if (!targetBucket.hasAccessTo(vcaFeature.getName()))
                    {
                        continue;
                    }
                }

                settingsMap.put(eventType, settings.getSettingsByType(eventType));
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("settings", settingsMap);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param bucket-id            bucket id. Mandatory
     * @param event-type           {@link platform.events.EventType}. Mandatory
     * @param notification-enabled true/false. Mandatory
     * @param video-required       true/false. Mandatory
     *
     * @servtitle updates the alert settings for the given event type
     * @httpmethod POST
     * @uri /api/{bucket}/updatebucketnotificationsettings
     * @responsejson {
     * "result" : "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatebucketnotificationsettings()
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            EventType eventType = EventType.parse(readApiParameter("event-type", true));
            boolean notificationEnabled = Boolean.parseBoolean(readApiParameter("notification-enabled", true));
            boolean videoRequired = Boolean.parseBoolean(readApiParameter("video-required", true));

            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            //access
            checkBucketAccess(targetBucket, true);

            synchronized (NOTI_CHANGE_LOCK)
            {
                BucketNotificationSettings settings = targetBucket.getNotificationSettings();
                List<EventType> supportedTypes = settings.getSupportedEventTypes();
                if (!supportedTypes.contains(eventType))
                {
                    throw new ApiException("unsupported-event-type");
                }

                settings.updateEventTypeSettings(eventType, notificationEnabled, videoRequired);
                settings.save();

                //inform nodes
                BroadcastItem.queue(bucketId, CommandType.CLOUD_UPDATE_ALERT_SETTINGS, new Gson().toJson(settings));
            }

            //update cache
            CacheClient cacheClient = CacheClient.getInstance();
            cacheClient.remove(cacheClient.getBucket(bucketId));

            //log
            try
            {
                BucketLog bLog = new BucketLog();
                bLog.bucketId = Long.parseLong(targetBucket.getBucketId());
                bLog.bucketName = targetBucket.getName();
                bLog.remoteIp = request.remoteAddress;
                bLog.username = renderArgs.get("username").toString();
                bLog.changes.add(String.format("Updated notification settings (%s)", Messages.get(eventType.toString())));
                bLog.save();
            }
            catch (Exception e)
            {
                Logger.error("BucketLog: " + e.getMessage());
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param eventId    Event ID. Optional. If this is specified, other params are ignored
     * @param event-type The type of event, Optional.
     * @param device-id  If want to filter only for a specific device, specify the device ID. Optional.
     * @param channel-id If want to filter only for a specific channel, specify the channel number. Optional.
     * @param skip       Pagination control, skip how many events from beginning of result? Mandatory.
     * @param take       Pagination control, return maximum how many events? Mandatory.
     * @param from       Timestamp with format ddMMyyyyHHmmss. 18122014000000
     *                   Filter to return only the events which occurred at or after this time. Optional.
     * @param to         Timestamp with format ddMMyyyyHHmmss. 20122014030356
     *                   Filter to return only the events which occurred at or before this time. Optional.
     *
     * @servtitle Returns list of alerts filtered according to specified parameters,
     * sorted chronologically with most recent first
     * @httpmethod POST
     * @uri /api/{bucket}/getalerts
     * @responsejson {
     * "result": "ok"/"error",
     * "totalcount": 11139,
     * "alerts": [ {@link CameraNotificationTransport} ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getalerts() throws ApiException
    {
        try
        {
            String callerUserId = getCallerUserId();

            //raw inputs
            String eventId = readApiParameter("event-id", false);
            String eventTypes = readApiParameter("event-type", false);
            String skip = readApiParameter("skip", false);
            String take = readApiParameter("take", false);
            String platformDeviceId = readApiParameter("device-id", false);
            String channelId = readApiParameter("channel-id", false);
            String from = readApiParameter("from", false);
            String to = readApiParameter("to", false);

            String hideResolved = readApiParameter("hide-resolved", false);
            
            boolean bHideResolved = false;
            if (!hideResolved.isEmpty())
            {
                bHideResolved = asBoolean(hideResolved);
            }

            QuerySentNotifications query = null;
            if (!Util.isNullOrEmpty(eventId))
            {
                query = new QuerySentNotifications(eventId);
            }
            else
            {
                //may contain more than one type
                String[] splitEventTypes = null;
                if (!eventTypes.isEmpty())
                {
                    splitEventTypes = eventTypes.split(",");
                }

                //verify device
                List<DeviceChannelPair> cameraList = new ArrayList<>();
                if (!Util.isNullOrEmpty(platformDeviceId))
                {
                    if (!Util.isLong(platformDeviceId))
                    {
                        throw new ApiException("invalid-device-id");
                    }
                    MongoDevice device = MongoDevice.getByPlatformId(platformDeviceId);
                    cameraList.add(new DeviceChannelPair(device.getCoreDeviceId(), channelId));
                }
                else
                {
                    for (MongoDevice userDevice : DeviceManager.getInstance().getDevicesOfUser(callerUserId))
                    {
                        cameraList.add(new DeviceChannelPair(userDevice.getCoreDeviceId(), null));
                    }
                }

                query = new QuerySentNotifications(
                        Long.parseLong(callerUserId),
                        splitEventTypes,
                        cameraList,
                        from,
                        to,
                        skip,
                        take,
                        bHideResolved
                );
            }

            //query and wait
            ServerPagedResult<SentNotification> pagedResult = await(query.now());

            //convert db object to transport
            List<CameraNotificationTransport> transports = new ArrayList<>();
            for (SentNotification dbItem : pagedResult.getResultsForOnePage())
            {
                transports.add(new CameraNotificationTransport(Long.parseLong(callerUserId), dbItem));
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("totalcount", pagedResult.getTotalCount());
            response.put("alerts", transports);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param eventId Event ID. Mandatory
     *
     * @servtitle Returns the alert details for the given event id
     * @httpmethod POST
     * @uri /api/{bucket}/getalertdetails
     * @responsejson {
     * "result": "ok"/"error",
     * "alert": {@link CameraNotificationTransport}
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getalertdetails() throws ApiException
    {
        try
        {
            long callerUserId = Long.parseLong(renderArgs.get("caller-user-id").toString());

            String eventId = readApiParameter("event-id", true);

            //query and wait
            QuerySentNotifications query = new QuerySentNotifications(eventId);
            ServerPagedResult<SentNotification> pagedResult = await(query.now());

            CameraNotificationTransport transport = null;
            if (pagedResult.getTotalCount() > 0)
            {
                SentNotification notification = pagedResult.getResultsForOnePage().get(0);
                transport = new CameraNotificationTransport(callerUserId, notification);
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("alert", transport);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param file-format      "xls" or "pdf".
     * @param time-zone-offset The timezone to which event timestamps should be
     *                         converted to, expressed as number of minutes offset from UTC.
     * @param event-type       The type of events, Comma separated values or can be single value
     *                         eg. "event-vca-intrusion,event-vca-perimeter,event-vca-loitering" / event-vca-intrusion
     * @param device-id        ID of device
     * @param channel-id       ID of Channel
     * @param from             Timestamp with format ddMMyyyyHHmmss. 10061014000000
     *                         Filter to return only the events which occurred at or after this time. Optional.
     * @param to               Timestamp with format ddMMyyyyHHmmss. 13061014235959
     *                         Filter to return only the events which occurred at or before this time. Optional.
     *
     * @servtitle Exports alerts list as a file in the specified format i.e PDF or XLS
     * @httpmethod GET
     * @uri /api/{bucket}/exportalerts
     * @responsejson {
     * "result": "ok",
     * "download-url": "/public/files/tmp/13Jun2014175343094.pdf" / "/public/files/tmp/13Jun2014175343094.xls"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void exportalerts() throws ApiException
    {
        try
        {
            //validate input parameters
            String callerUserId = getCallerUserId();

            //raw inputs
            String eventTypes = readApiParameter("event-type", false);
            String platformDeviceId = readApiParameter("device-id", false);
            String channelId = readApiParameter("channel-id", false);
            String from = readApiParameter("from", false);
            String to = readApiParameter("to", false);
            String hideResolved = readApiParameter("hide-resolved", false);

            FileFormat fileFormat = FileFormat.parse(readApiParameter("file-format", true));
            String timeZoneOffset = readApiParameter("time-zone-offset", true);

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

            boolean bHideResolved = false;
            if (!hideResolved.isEmpty())
            {
                bHideResolved = asBoolean(hideResolved);
            }

            //may contain more than one type
            String[] splitEventTypes = eventTypes.split(",");

            //verify device
            List<DeviceChannelPair> cameraList = new ArrayList<>();
            if (!Util.isNullOrEmpty(platformDeviceId))
            {
                if (!Util.isLong(platformDeviceId))
                {
                    throw new ApiException("invalid-device-id");
                }
                MongoDevice device = MongoDevice.getByPlatformId(platformDeviceId);
                cameraList.add(new DeviceChannelPair(device.getCoreDeviceId(), channelId));
            }
            else
            {
                for (MongoDevice userDevice : DeviceManager.getInstance().getDevicesOfUser(callerUserId))
                {
                    cameraList.add(new DeviceChannelPair(userDevice.getCoreDeviceId(), null));
                }
            }

            //query and wait
            QuerySentNotifications queryJob = new QuerySentNotifications(
                    Long.parseLong(callerUserId),
                    splitEventTypes,
                    cameraList,
                    from,
                    to,
                    null,
                    null,
                    bHideResolved
            );

            ReportBuilder reportBuilder = null;
            String locale = Lang.get();
            switch (fileFormat)
            {
                case PDF:
                    reportBuilder = new AlertsPdf(queryJob.getQuery(), offsetMinutes, locale);
                    break;

                case CSV:
                    reportBuilder = new AlertsCsv(queryJob.getQuery(), offsetMinutes, locale);
                    break;

                default:
                    throw new ApiException("file-format-not-supported");
            }

            respondExportedFileUrl(reportBuilder);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param filename   event video file name
     * @param action     play or download
     * @param customName custom name for the downloaded file
     *
     * @servtitle Downloads event video file
     * @httpmethod GET
     * @uri /api/{bucket}/geteventvideo
     * @responsejson event video file
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void geteventvideo(String filename, String action, String customName) throws ApiException
    {
        try
        {
            boolean inline = false;
            if (!Util.isNullOrEmpty(action) && action.toLowerCase().equals("play"))
            {
                inline = true;
            }

            EventManager eventMgr = EventManager.getInstance();
            String eventId = eventMgr.extractEventIdFrom(filename);
            GridFsDetails videoDetails = eventMgr.getEventVideo(eventId);
            if (videoDetails == null)
            {
                throw new ApiException("event-video-unavailable");
            }

            GridFSDBFile gridFile = GridFsHelper.getGridFSDBFile(videoDetails);
            if (gridFile == null)
            {
                Logger.error("EventVideo exists, but gridFs file missing (%s)", eventId);
                throw new ApiException("event-video-unavailable");
            }

            //filename in the response
            String downloadedFilename = videoDetails.getFilename();
            if (!Util.isNullOrEmpty(customName))
            {
                downloadedFilename = customName;
            }

            renderBinary(
                    gridFile.getInputStream(),
                    downloadedFilename,
                    gridFile.getLength(),
                    downloadedFilename.endsWith("mp4") ? "video/mp4" : null,
                    inline
            );
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle restores the alert settings to defaults
     * @httpmethod POST
     * @uri /api/{bucket}/restorebucketnotificationsettings
     * @responsejson {
     * "result" : "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void restorebucketnotificationsettings()
    {
        try
        {
            String bucketId = readApiParameter("bucket-id", true);
            MongoBucket targetBucket = MongoBucket.getById(bucketId);
            if (targetBucket == null)
            {
                throw new ApiException("invalid-bucket-id");
            }

            //access
            checkBucketAccess(targetBucket, true);

            BucketNotificationSettings settings = targetBucket.getNotificationSettings();
            settings.restoreDefaults();
            settings.save();

            //inform nodes
            BroadcastItem.queue(bucketId, CommandType.CLOUD_UPDATE_ALERT_SETTINGS, new Gson().toJson(settings));

            //update cache
            CacheClient cacheClient = CacheClient.getInstance();
            cacheClient.remove(cacheClient.getBucket(bucketId));

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @servtitle get a list of notify methods that the server is configured to support
     * @httpmethod POST
     * @uri /api/{bucket}/getallowednotifymethods
     * @responsejson {
     * "result": "ok",
     * "methods": [
     * "ON_SCREEN",
     * "EMAIL",
     * "SMS",
     * "MOBILE_PUSH"
     * ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getallowednotifymethods()
    {
        try
        {
            Set<NotifyMethod> allowedList = NotifyMethod.getServerEnabledMethods();

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("methods", allowedList);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param label-id label id. Mandatory
     *
     * @servtitle occupancy settings of the label
     * @httpmethod POST
     * @uri /api/{bucket}/getlabeloccupancysettings
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getlabeloccupancysettings() throws ApiException
    {
        try
        {
            String labelId = readApiParameter("label-id", true);
            LabelOccupancySettings settings = LabelOccupancySettings.findByLabelId(labelId);
            if (settings == null)
            {
                throw new ApiException("invalid-label-id");
            }

            LabelStore label = LabelType.STORE.getQuery().filter("labelId", labelId).first();
            if (label == null)
            {
                throw new ApiException("invalid-label-id");
            }

            //check access
            boolean atLeastOneAssigned = false;
            String callerUserId = getCallerUserId();
            for (MongoDevice device : DeviceManager.getInstance().getDevicesOfUser(callerUserId))
            {
                for (DeviceChannelPair camera : label.getCameraList())
                {
                    if (device.getCoreDeviceId().equals(camera.getCoreDeviceId()))
                    {
                        atLeastOneAssigned = true;
                        break;
                    }
                }
            }
            if (!atLeastOneAssigned)
            {
                throw new ApiException("access-denied");
            }

            respondOK("settings", settings);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param label-id            label id. Mandatory
     * @param enabled             notification enabled. Mandatory
     * @param occupancy-limits    occupancy limits. Mandatory if enabled is true
     * @param min-notify-interval minimum interval between notifications. Mandatory if enabled is true
     *
     * @servtitle updates occupancy settings of the label
     * @httpmethod POST
     * @uri /api/{bucket}/updatelabeloccupancysettings
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void updatelabeloccupancysettings() throws ApiException
    {
        try
        {
            String labelId = readApiParameter("label-id", true);
            boolean isEnabled = asBoolean(readApiParameter("enabled", true));
            String jsonLimits = readApiParameter("occupancy-limits", false);
            String minNotifyInterval = readApiParameter("min-notify-interval", false);

            //verify id
            LabelOccupancySettings settings = LabelOccupancySettings.findByLabelId(labelId);
            if (settings == null)
            {
                throw new ApiException("invalid-label-id");
            }

            LabelStore label = LabelType.STORE.getQuery().filter("labelId", labelId).first();
            if (label == null)
            {
                throw new ApiException("invalid-label-id");
            }

            //check access
            boolean atLeastOneAssigned = false;
            String callerUserId = getCallerUserId();
            for (MongoDevice device : DeviceManager.getInstance().getDevicesOfUser(callerUserId))
            {
                for (DeviceChannelPair camera : label.getCameraList())
                {
                    if (device.getCoreDeviceId().equals(camera.getCoreDeviceId()))
                    {
                        atLeastOneAssigned = true;
                        break;
                    }
                }
            }
            if (!atLeastOneAssigned)
            {
                throw new ApiException("access-denied");
            }

            //validate inputs
            List<OccupancyLimit> occupancyLimits = new ArrayList<>();
            int minIntervalSeconds = 0;
            if (isEnabled)
            {
                if (Util.isNullOrEmpty(jsonLimits))
                {
                    throw new ApiException("missing-occupancy-limits");
                }
                try
                {
                    occupancyLimits = new Gson().fromJson(jsonLimits, new TypeToken<List<OccupancyLimit>>()
                    {
                    }.getType());

                    if (occupancyLimits.isEmpty())
                    {
                        throw new ApiException("error-empty-occupancy-limits");
                    }
                }
                catch (Exception e)
                {
                    Logger.error(e, "");
                    throw new ApiException("invalid-occupancy-limits");
                }

                if (Util.isNullOrEmpty(minNotifyInterval))
                {
                    throw new ApiException("missing-min-notify-interval");
                }
                minIntervalSeconds = asInt(minNotifyInterval);
            }

            settings.setEnabled(isEnabled);
            if (isEnabled)
            {
                //update the rest only if enable is true so that the old settings will remain the same
                settings.setLimits(new TreeSet<>(occupancyLimits));
                settings.setMinNotifyIntervalSeconds(minIntervalSeconds);
            }
            settings.save();

            //update cache
            LabelManager.getInstance().removeCachedSettings(labelId);

            respondOK();
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }


    /**
     * @param from       Timestamp with format ddMMyyyyHHmmss. 18122014000000
     *                   Filter to return only the events which occurred at or after this time. Mandatory.
     * @param to         Timestamp with format ddMMyyyyHHmmss. 20122014030356
     *                   Filter to return only the events which occurred at or before this time. Mandatory.
     * @param skip       Pagination control, skip how many events from beginning of result? Mandatory.
     * @param take       Pagination control, return maximum how many events? Mandatory.
     * @param event-type The type of event, Optional.
     * @param label-id   label id, Optional.
     *
     * @servtitle Returns list of label notifications
     * @httpmethod POST
     * @uri /api/{bucket}/getlabelnotifications
     * @responsejson {
     * "result" = "ok"/"error",
     * "totalcount": 11139,
     * "alerts": [ {@link models.transportobjects.LabelNotificationTransport} ]
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void getlabelnotifications() throws ApiException
    {
        try
        {
            String callerUserId = getCallerUserId();

            //inputs
            String eventId = readApiParameter("event-id", false);
            String from = readApiParameter("from", false);
            String to = readApiParameter("to", false);
            String skip = readApiParameter("skip", false);
            String take = readApiParameter("take", false);
            String eventTypes = readApiParameter("event-type", false);
            String labelId = readApiParameter("label-id", false);
            String hideResolved = readApiParameter("hide-resolved", false);

            AckStatus ackStatus = null;
            if (!hideResolved.isEmpty())
            {
                ackStatus = asBoolean(hideResolved) ? AckStatus.NO_ACTION : null;
            }

            QuerySentLabelNotifications query = null;
            if (!Util.isNullOrEmpty(eventId))
            {
                query = new QuerySentLabelNotifications(eventId);
            }
            else
            {
                if (from.isEmpty() || to.isEmpty())
                {
                    throw new ApiException("Missing period");
                }

                //verify range
                long fromMillis = toMilliseconds(from);
                long toMillis = toMilliseconds(to);
                int periodLimitDays = 30;
                if (toMillis - fromMillis > TimeUnit.DAYS.toMillis(periodLimitDays))
                {
                    throw new ApiException(Messages.get("error-select-shorter-period", periodLimitDays));
                }

                //may contain more than one type
                String[] splitEventTypes = eventTypes.isEmpty() ? null : eventTypes.split(",");

                //compile user accessible labels
                List<String> userLabelIdList = new ArrayList<>();
                for (DeviceLabel labelObj : LabelManager.getInstance().getUserAccessibleLabels(Long.parseLong(callerUserId)))
                {
                    userLabelIdList.add(labelObj.getLabelId());
                }

                //set label filter
                List<String> filteredLabelIdList = labelId.isEmpty() ? userLabelIdList : Arrays.asList(labelId);

                query = new QuerySentLabelNotifications(
                        Long.parseLong(callerUserId),
                        splitEventTypes,
                        filteredLabelIdList,
                        new UtcPeriod(fromMillis, toMillis),
                        skip,
                        take,
                        ackStatus
                );
            }

            //query and wait
            ServerPagedResult<SentLabelNotification> pagedResult = await(query.now());

            //convert db object to transport
            List<LabelNotificationTransport> transports = new ArrayList<>();
            for (SentLabelNotification dbItem : pagedResult.getResultsForOnePage())
            {
                transports.add(new LabelNotificationTransport(Long.parseLong(callerUserId), dbItem));
            }

            Map response = new LinkedHashMap();
            response.put("result", "ok");
            response.put("total-count", pagedResult.getTotalCount());
            response.put("notifications", transports);
            renderJSON(response);
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }

    /**
     * @param event-id event id. Mandatory
     *
     * @servtitle acknowledge the notification and set it to resolved
     * @httpmethod POST
     * @uri /api/{bucket}/acknowledgenotification
     * @responsejson {
     * "result": "ok"
     * }
     * @responsejson {
     * "result": "error",
     * "reason": "unknown"
     * }
     */
    public static void acknowledgenotification() throws ApiException
    {
        try
        {
            String eventId = readApiParameter("event-id", true);
            String message = readApiParameter("message", false);

            String callerUserId = getCallerUserId();
            MongoUser callerUser = MongoUser.getById(callerUserId);

            //check security alerts first
            QuerySentNotifications securityQuery = new QuerySentNotifications(eventId);
            ServerPagedResult<SentNotification> securityResult = await(securityQuery.now());
            if (!securityResult.getResultsForOnePage().isEmpty())
            {
                SentNotification target = securityResult.getResultsForOnePage().get(0);
                target.setAckStatus(AckStatus.ACKNOWLEDGED);
                target.save();
                AcknowledgementLog.save(eventId, AckStatus.ACKNOWLEDGED, message, callerUser);
                respondOK();
            }

            //check label notifications
            QuerySentLabelNotifications query = new QuerySentLabelNotifications(eventId);
            ServerPagedResult<SentLabelNotification> pagedResult = await(query.now());
            if (!pagedResult.getResultsForOnePage().isEmpty())
            {
                //update
                SentLabelNotification target = pagedResult.getResultsForOnePage().get(0);
                target.setAckStatus(AckStatus.ACKNOWLEDGED);
                target.save();
                AcknowledgementLog.save(eventId, AckStatus.ACKNOWLEDGED, message, callerUser);
                respondOK();
            }

            throw new ApiException("invalid-event-id");
        }
        catch (Exception e)
        {
            respondError(e);
        }
    }
    
}
