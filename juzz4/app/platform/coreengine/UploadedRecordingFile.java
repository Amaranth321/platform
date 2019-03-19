package platform.coreengine;

import com.google.gson.Gson;
import com.kaisquare.core.thrift.StreamFileDetails;
import lib.util.Util;
import play.Logger;

import java.util.Map;

/**
 * Platform-side object of {@link com.kaisquare.core.thrift.StreamFileDetails}
 *
 * @author Aye Maung
 * @since v4.4
 */
public class UploadedRecordingFile
{
    private final long startTime;
    private final long endTime;
    private final long fileSize;      //in bytes
    private final Url url;
    private final int progress;      //percent
    private final RecordingUploadStatus status;

    public UploadedRecordingFile(StreamFileDetails streamDetails)
    {
        this.startTime = CoreUtils.getMillis(streamDetails.getFrom());
        this.endTime = CoreUtils.getMillis(streamDetails.getTo());
        this.fileSize = Integer.parseInt(streamDetails.getFileSize());
        this.status = RecordingUploadStatus.valueOf(streamDetails.getStatus().toUpperCase());

        //progress
        int percent = Util.isInteger(streamDetails.getProgress()) ? Integer.parseInt(streamDetails.getProgress()) : 0;
        if (percent < 0 || percent > 100)
        {
            Logger.error("percent value is out of bounds (%s)", streamDetails);
            percent = 0;
        }
        this.progress = percent;

        //parse Url
        url = Url.parse(streamDetails.getUrl());
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public long getFileSize()
    {
        return fileSize;
    }

    public Url getUrl()
    {
        return url;
    }

    public int getProgress()
    {
        return progress;
    }

    public RecordingUploadStatus getStatus()
    {
        return status;
    }

    public static class Url
    {
        public final String play;
        public final String download;

        public static Url parse(String jsonUrl)
        {
            if (Util.isNullOrEmpty(jsonUrl))
            {
                return null;
            }

            Map urlMap = new Gson().fromJson(jsonUrl, Map.class);
            if (urlMap == null || urlMap.isEmpty())
            {
                return null;
            }

            String play = String.valueOf(urlMap.get("play"));
            String download = String.valueOf(urlMap.get("download"));
            return new Url(play, download);
        }

        private Url(String play, String download)
        {
            this.play = play;
            this.download = download;
        }
    }
}
