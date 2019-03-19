package jobs.node;

import com.google.gson.Gson;
import com.kaisquare.events.thrift.EventDetails;
import core.ArbiterManagementClient;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import models.events.EventVideo;
import models.events.EventVideoRequest;
import models.events.UniqueEventRecord;
import models.events.UnsyncedEventVideo;
import models.notification.EventToNotify;
import platform.DeviceManager;
import platform.analytics.VcaStatus;
import platform.common.ACResource;
import platform.content.FileFormat;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedBucket;
import platform.db.cache.proxies.CachedDevice;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsFileGroup;
import platform.db.gridfs.GridFsHelper;
import platform.devices.DeviceLog;
import platform.devices.DeviceStatus;
import platform.events.EventInfo;
import platform.events.EventManager;
import platform.events.EventType;
import platform.events.VideoReadyEventData;
import platform.node.KaiSyncCommandClient;
import platform.node.NodeManager;
import platform.rt.RTFeedManager;
import play.Logger;
import play.jobs.Job;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is the event handling part of the old PushEvent task on cloud platform
 * <p/>
 * DO NOT add static variables to this class. There could be many instances of this job running
 * and synchronization for such variables will affect performance.
 * <p/>
 * Each event handling codes must return immediately after execution to reduce irrelevant checkings.
 * i.e. No event should ever reach the end of process() call.
 * If it did, the event would be assumed to have no handling codes
 *
 * @author Aye Maung
 * @since v4.4
 */
public class HandleEventOnNode extends Job<Boolean>
{
    private final EventManager eventMgr = EventManager.getInstance();
    private final EventInfo eventInfo;
    private final String jsonData;
    private final byte[] binaryData;

    private CachedDevice cachedDevice;

    public HandleEventOnNode(EventDetails eventDetails)
    {
        eventInfo = EventInfo.fromThriftEvent(eventDetails);
        jsonData = eventDetails.getData();
        binaryData = eventDetails.getBinaryData();
    }

    @Override
    public Boolean doJobWithResult()
    {
        if (!NodeManager.getInstance().isRegisteredOnCloud())
        {
            //not registered yet. ignore the event
            return true;
        }

        try
        {
            process();
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    private void process()
    {
        if (UniqueEventRecord.duplicateExists(eventInfo))
        {
            Logger.warn("[Duplicate] ignored event : %s", eventInfo);
            return;
        }

        //check type
        if (eventInfo.getType().equals(EventType.UNKNOWN))
        {
            eventMgr.reject(eventInfo, jsonData, "unrecognized event type");
            return;
        }

        //check device
        cachedDevice = CacheClient.getInstance().getDeviceByCoreId(eventInfo.getCamera().getCoreDeviceId());
        if (cachedDevice == null)
        {
            eventMgr.reject(eventInfo, jsonData, "event's device not found in db");
            return;
        }

        //real time feeds
        RTFeedManager.getInstance().newEventReceived(eventInfo, jsonData);

        //notification
        EventToNotify.queue(eventInfo);

        //event video
        sendEventVideoRequest();

        //special processing for some types
        switch (eventInfo.getType())
        {
            case ERROR_VCA:
                eventMgr.processVcaError(eventInfo, jsonData);
                eventMgr.queueForCloud(eventInfo, jsonData, binaryData);
                return;

            //node connection
            case CORE_DEVICE_CONNECTION_FAILED:
                String messages = String.format("%s (%s)", eventInfo.getType(), jsonData);
                DeviceLog.createLog(Long.parseLong(cachedDevice.getPlatformDeviceId()), messages);
                return;

            case CORE_DEVICE_CONNECTED:
            case CORE_DEVICE_CONNECTION_LOST:
                deviceStatusChanged();
                return;

            //log device events
            case CORE_DEVICE_CONNECTION_POOR:
            case CORE_NODE_UPSTREAM_FAILED:
            case CORE_RECORDING_STARTED:
            case CORE_RECORDING_STOPPED:
            case CORE_RECORDING_DISK_FULL:
                DeviceLog.createLog(Long.parseLong(cachedDevice.getPlatformDeviceId()), eventInfo.getType().toString());
                break;

            case CORE_EVENT_VIDEO_READY:
                processVideoReadyEvent();
                return;

            //ignore realtime-only events
            case VCA_FEED_PROFILING:
                return;

            case VCA_STATUS_CHANGED:
                updateVcaStateToCloud();
                return;
        }

        //vca report data
        if (eventInfo.getType().isVcaEvent())
        {

            //report processors
            eventMgr.queueForReportProcessing(eventInfo, jsonData, binaryData);

            //queue
            eventMgr.queueForCloud(eventInfo, jsonData, binaryData);

            return;
        }

        eventMgr.reject(eventInfo, jsonData, "no handling codes");
    }

    private void sendEventVideoRequest()
    {
        //check if video is required
        CachedBucket bucket = CacheClient.getInstance().getBucket(cachedDevice.getBucketId());
        if (!bucket.isVideoEnabled(eventInfo.getType()))
        {
            return;
        }

        Map<String, Object> videoRequestData = new LinkedHashMap();
        videoRequestData.put("eventId", eventInfo.getEventId());
        videoRequestData.put("deviceId", eventInfo.getCamera().getCoreDeviceId());
        videoRequestData.put("channelId", eventInfo.getCamera().getChannelId());
        videoRequestData.put("duration", EventManager.EVENT_VIDEO_SECONDS);
        videoRequestData.put("eventTime", eventInfo.getTime());
        String requestData = new Gson().toJson(videoRequestData);

        EventVideoRequest request = EventVideoRequest.createNew(eventInfo, requestData);
        boolean result = ArbiterManagementClient.getInstance().requestEventVideo(eventInfo, requestData);
        if (!result)
        {
            request.callFailed();
        }
    }

    private void processVideoReadyEvent()
    {
        /**
         * use json data only which holds the info of the event that the video belongs to
         *
         */
        //parse data
        VideoReadyEventData videoData = VideoReadyEventData.parse(jsonData);
        if (videoData == null)
        {
            eventMgr.reject(eventInfo, jsonData, "invalid json event data");
            return;
        }

        //eventId that the video belongs to
        String ownerEventId = videoData.getOwnerEventId();
        String filePath = videoData.getPath();
        FileFormat format = EventManager.EVENT_VIDEO_FORMAT;
        String filename = String.format("%s.%s", ownerEventId, format.getExtension());

        //save in grid fs
        try (ACResource<FileInputStream> acIn = new ACResource<>(new FileInputStream(filePath)))
        {
            GridFsDetails videoDetails = GridFsHelper.saveFileInputStream(
                    filename,
                    acIn.get(),
                    format,
                    GridFsFileGroup.EVENT_VIDEOS
            );

            if (videoDetails == null)
            {
                Logger.error(Util.whichFn() + "Failed to save event video (%s)", ownerEventId);
                return;
            }

            //find original request
            EventVideoRequest request = EventVideoRequest.find(ownerEventId);
            EventVideo.createNew(request.getOwnerEventInfo(), videoDetails);

            //queue for upload
            UnsyncedEventVideo.createNew(request.getOwnerEventInfo());

            //remove request
            request.delete();
        }
        catch (Exception e)
        {
            Logger.error(e, "Failed to save event video (%s)", ownerEventId);
            return;
        }

        //remove core file
        new File(filePath).delete();
    }

    private void deviceStatusChanged()
    {
        DeviceStatus cameraStatus = DeviceStatus.fromEvent(eventInfo.getType());
        DeviceManager.getInstance().updateDeviceStatus(eventInfo.getCamera(), cameraStatus);

        try
        {
            KaiSyncCommandClient.getInstance().nodeCameraStatusChanged(eventInfo.getCamera(), cameraStatus);
        }
        catch (ApiException e)
        {
            Logger.error(e, "");
        }
    }

    private void updateVcaStateToCloud()
    {
        try
        {
            Map statusMap = new Gson().fromJson(jsonData, Map.class);
            String instanceId = String.valueOf(statusMap.get("instanceId"));
            VcaStatus vcaStatus = VcaStatus.parse(String.valueOf(statusMap.get("status")));
            KaiSyncCommandClient.getInstance().nodeVcaStateChanged(instanceId, vcaStatus);
        }
        catch (ApiException e)
        {
            Logger.error(e, "");
        }
    }
}
