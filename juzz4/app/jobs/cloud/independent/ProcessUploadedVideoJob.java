package jobs.cloud.independent;

import jobs.cloud.CloudCronJob;
import lib.util.Util;
import models.events.EventVideo;
import models.events.UnprocessedUploadedVideo;
import platform.common.ACResource;
import platform.db.cache.CacheClient;
import platform.db.gridfs.GridFsDetails;
import platform.db.gridfs.GridFsFileGroup;
import platform.db.gridfs.GridFsHelper;
import platform.events.EventInfo;
import platform.events.EventManager;
import platform.kaisyncwrapper.KaiSyncHelper;
import play.Logger;
import play.jobs.Every;

import java.io.InputStream;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Every("5s")
public class ProcessUploadedVideoJob extends CloudCronJob
{
    private static final CacheClient cacheClient = CacheClient.getInstance();

    //to prevent accessing uploaded files too soon
    private static final long NEWEST_EXCLUSION_BUFFER = 5 * 1000;
    private static final int ATTEMPT_LIMIT = 3;

    @Override
    public void doJob()
    {
        UnprocessedUploadedVideo uploadedVideo = getNext();
        while (uploadedVideo != null && !uploadedVideo.isNew())
        {
            if (process(uploadedVideo))
            {
                uploadedVideo.delete();
            }
            else
            {
                uploadedVideo.incrementAttempt();
            }

            uploadedVideo = getNext();
        }
    }

    private UnprocessedUploadedVideo getNext()
    {
        return UnprocessedUploadedVideo.q()
                .filter("_created <", System.currentTimeMillis() - NEWEST_EXCLUSION_BUFFER)
                .filter("attempts <", ATTEMPT_LIMIT + 1)
                .order("_created")
                .first();
    }

    private boolean process(UnprocessedUploadedVideo uploadedVideo)
    {
        try
        {
            String fileName = uploadedVideo.getFileName();
            EventInfo ownerEventInfo = uploadedVideo.getEventInfo();

            //save uploaded video in grid fs
            GridFsDetails videoDetails = null;
            try (ACResource<InputStream> acIn = KaiSyncHelper.retrieveUploadedFile(fileName))
            {
                if (acIn == null)
                {
                    return false;
                }

                videoDetails = GridFsHelper.saveFileInputStream(
                        fileName,
                        acIn.get(),
                        EventManager.EVENT_VIDEO_FORMAT,
                        GridFsFileGroup.EVENT_VIDEOS
                );
            }

            //save video on cloud
            EventVideo.createNew(ownerEventInfo, videoDetails);

            //delete uploaded file
            if (!KaiSyncHelper.deleteUploadedFile(fileName))
            {
                Logger.error(Util.whichFn() + "failed to delete uploaded file (%s)", fileName);
            }

            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }
}
