package jobs.cloud;

import com.kaisquare.events.thrift.EventDetails;
import models.events.OldEventVideo;
import models.events.UniqueEventRecord;
import models.events.UnprocessedUploadedVideo;
import models.notification.EventToNotify;
import org.joda.time.DateTime;
import platform.DeviceManager;
import platform.config.readers.ConfigsCloud;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.devices.DeviceLog;
import platform.devices.DeviceStatus;
import platform.events.EventInfo;
import platform.events.EventManager;
import platform.rt.RTFeedManager;
import play.Logger;
import play.jobs.Job;

/**
 * This is the event handling part of the old PushEvent task on cloud platform
 * <p/>
 * DO NOT add static variables to this class. There could be many instances of this job running
 * and synchronization for such variables will affect performance.
 * <p/>
 * Each event handling codes must return immediately after execution to reduce irrelevant checking.
 * i.e. No event should ever reach the end of process() call.
 * If it did, the event would be assumed to have no handling codes
 *
 * @author Aye Maung
 * @author Keith
 * @since v4.4
 */
public class HandleEventOnCloud extends Job<Boolean>
{
    private final EventManager eventMgr = EventManager.getInstance();
    private final EventInfo eventInfo;
    private final String jsonData;
    private final byte[] binaryData;

    public HandleEventOnCloud(EventDetails eventDetails)
    {
        eventInfo = EventInfo.fromThriftEvent(eventDetails);
        jsonData = eventDetails.getData();
        binaryData = eventDetails.getBinaryData();
    }

    @Override
    public Boolean doJobWithResult()
    {
        try
        {
            //legacy event from older nodes
            if (eventInfo.getType().toString().equals("event-recording"))
            {
                OldEventVideo oldVid = new OldEventVideo(eventInfo);
                oldVid.save();
                return true;
            }

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
        	Logger.info("duplicateExists.............");
            eventMgr.reject(eventInfo, jsonData, "duplicate event");
            return;
        }

        //check device
        CachedDevice eventDevice = CacheClient.getInstance().getDeviceByCoreId(eventInfo.getCamera().getCoreDeviceId());
        if (eventDevice == null)
        {
        	Logger.info("check device error.............");
            eventMgr.reject(eventInfo, jsonData, "event's device not found in db");
            return;
        }

        //real time feeds
        RTFeedManager.getInstance().newEventReceived(eventInfo, jsonData);

        //notification
        EventToNotify.queue(eventInfo);

        //special processing for some types
        switch (eventInfo.getType())
        {
            case ERROR_VCA:
                eventMgr.processVcaError(eventInfo, jsonData);
                return;

            //node connection
            case CORE_DEVICE_CONNECTED:
            case CORE_DEVICE_CONNECTION_LOST:
                DeviceStatus nodeStatus = DeviceStatus.fromEvent(eventInfo.getType());
                DeviceManager.getInstance().updateDeviceStatus(eventInfo.getCamera(), nodeStatus);
                return;

            //log device events
            case CORE_DEVICE_CONNECTION_POOR:
            case CORE_NODE_UPSTREAM_FAILED:
            case CORE_RECORDING_STARTED:
            case CORE_RECORDING_STOPPED:
            case CORE_RECORDING_DISK_FULL:
                DeviceLog.createLog(Long.parseLong(eventDevice.getPlatformDeviceId()), eventInfo.getType().toString());
                break;

            case NODE_EVENT_VIDEO_UPLOADED:
                UnprocessedUploadedVideo.queue(jsonData);
                return;

            //don't process realtime-only events
            case VCA_FEED_PROFILING:
                return;
        }

        //vca report data
        if (eventInfo.getType().isVcaEvent())
        {
        	Logger.info("isVcaEvent......................queueForReportProcessing");
            //report processors
            eventMgr.queueForReportProcessing(eventInfo, jsonData, binaryData);

            return;
        }

        eventMgr.reject(eventInfo, jsonData, "no handling codes");
    }
}
