package jobs.node;

import com.google.gson.Gson;
import com.kaisquare.transports.event.VideoOwnerEventData;
import lib.util.Util;
import models.events.UnsyncedEventVideo;
import platform.Environment;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedDevice;
import platform.db.gridfs.GridFsDetails;
import platform.events.EventInfo;
import platform.events.EventManager;
import platform.events.EventType;
import platform.kaisyncwrapper.KaiSyncHelper;
import play.Logger;

import java.util.UUID;

/**
 * Singleton job to sync event videos to cloud.
 * <p>
 * This job will be started by {@link NodeJobsManager}
 *
 * @author Aye Maung
 * @since v4.4
 */
class SyncVideoToCloudJob extends SingletonCloudDependentQueueJob<UnsyncedEventVideo>
{
    private static final SyncVideoToCloudJob instance = new SyncVideoToCloudJob();

    public static SyncVideoToCloudJob getInstance()
    {
        return instance;
    }

    @Override
    protected int getPauseDuration()
    {
        return 500;
    }

    @Override
    protected UnsyncedEventVideo getNext()
    {
        UnsyncedEventVideo dbItem = UnsyncedEventVideo.getOldest();
        if (dbItem != null && dbItem.isNew())
        {
            return null;
        }

        return dbItem;
    }

    @Override
    protected long countRemainingItems()
    {
        return UnsyncedEventVideo.q().count();
    }

    @Override
    protected void process(UnsyncedEventVideo unsyncedVid)
    {
        //event that this video belongs to
        EventInfo ownerEvent = unsyncedVid.getOwnerEventInfo();

        //no need to sync if camera is already deleted
        CachedDevice cachedDevice = CacheClient.getInstance()
                .getDeviceByCoreId(ownerEvent.getCamera().getCoreDeviceId());
        if (cachedDevice == null)
        {
            Logger.info(Util.whichFn() + "target camera no longer exists (%s)", unsyncedVid);
            unsyncedVid.delete();
            return;
        }

        //find video in db
        GridFsDetails videoDetails = unsyncedVid.getVideoDetails();
        if (videoDetails == null)
        {
            Logger.error("unsynced event video not found (%s)", ownerEvent);
            unsyncedVid.delete();
            return;
        }

        //upload
        boolean result = KaiSyncHelper.transferEventVideoToCloud(
                getNodeInfo().getCloudCoreDeviceId(),
                ownerEvent,
                videoDetails
        );

        //failed
        if (!result)
        {
            setStatus(NodeJobStatus.ERROR);
            unsyncedVid.incrementFailCount();
            return;
        }

        Logger.debug("Event video uploaded (%s)", ownerEvent);

        //inform cloud
        VideoOwnerEventData videoData = new VideoOwnerEventData(
                ownerEvent.getEventId(),
                videoDetails.getFilename(),
                ownerEvent.getType().toString(),
                getNodeInfo().getCloudCoreDeviceId(),
                ownerEvent.getCamera().getCoreDeviceId() //camera becomes channel on cloud
        );
        String jsonVidData = new Gson().toJson(videoData);

        EventInfo eventInfo = new EventInfo(
                UUID.randomUUID().toString(),
                EventType.NODE_EVENT_VIDEO_UPLOADED,
                ownerEvent.getCamera(),
                Environment.getInstance().getCurrentUTCTimeMillis()
        );

        //queue event
        EventManager.getInstance().queueForCloud(eventInfo, jsonVidData, null);
        unsyncedVid.delete();
    }
}
