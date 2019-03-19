package jobs.cloud.independent;

import jobs.cloud.CloudCronJob;
import lib.util.Util;
import models.archived.ArchivedEvent;
import models.events.EventVideo;
import models.events.OldEventVideo;
import platform.common.ACResource;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsFileGroup;
import platform.db.gridfs.GridFsHelper;
import platform.events.EventManager;
import platform.kaisyncwrapper.KaiSyncHelper;
import play.Logger;
import play.jobs.Every;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Every("10s")
public class MonitorOldEventVideoJob extends CloudCronJob
{
    private static final int RETRY_MAX_PERIOD_MINS = 60;

    private OldEventVideo prevFailed;

    @Override
    public void doJob()
    {
        OldEventVideo oldVid = getNext();
        while (oldVid != null && !oldVid.isNew())
        {
            if (!move(oldVid))
            {
                break;
            }
            oldVid = getNext();
        }
    }

    private OldEventVideo getNext()
    {
        return OldEventVideo.getQuery().first();
    }

    private boolean move(OldEventVideo oldVid)
    {
        ArchivedEvent vidOwner = ArchivedEvent.find(oldVid.getEventId());
        if (vidOwner == null)
        {
            oldVid.incrementAttempts();

            //don't retry immediately
            if (prevFailed != null && prevFailed.getEventId().equals(oldVid.getEventId()))
            {
                return false;
            }

            prevFailed = oldVid;
            return true;
        }

        String filename = oldVid.getVideoFilename();
        GridFsDetails videoDetails = null;
        try (ACResource<InputStream> acIn = KaiSyncHelper.retrieveUploadedFile(filename))
        {
            if (acIn == null)
            {
                return false;
            }

            videoDetails = GridFsHelper.saveFileInputStream(
                    filename,
                    acIn.get(),
                    EventManager.EVENT_VIDEO_FORMAT,
                    GridFsFileGroup.EVENT_VIDEOS
            );
        }
        catch (Exception e)
        {
            Logger.error(e, "");
        }

        if (videoDetails == null)
        {
            if (System.currentTimeMillis() - oldVid._getCreated() > TimeUnit.MINUTES.toMillis(RETRY_MAX_PERIOD_MINS))
            {
                Logger.error("Stopped retrying to get event video (%s)", oldVid.getEventId());
                oldVid.delete();
                return true;
            }
            else
            {
                prevFailed = oldVid;
                return false;
            }
        }

        EventVideo.createNew(vidOwner.getEventInfo(), videoDetails);

        //delete uploaded file
        if (!KaiSyncHelper.deleteUploadedFile(filename))
        {
            Logger.error(Util.whichFn() + "failed to delete uploaded file (%s)", filename);
        }

        oldVid.delete();
        return true;
    }
}
