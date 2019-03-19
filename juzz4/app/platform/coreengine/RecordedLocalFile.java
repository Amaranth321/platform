package platform.coreengine;

import com.kaisquare.core.thrift.RecordedMediaInfo;
import lib.util.Util;
import platform.devices.DeviceChannelPair;
import play.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class RecordedLocalFile
{
    private final DeviceChannelPair camera;
    private final long startTime;
    private final long endTime;
    private final String path;
    private final long fileSize;    //bytes
    private final RecordedFileStatus status;

    public RecordedLocalFile(RecordedMediaInfo coreFileInfo) throws FileNotFoundException
    {
        camera = new DeviceChannelPair(coreFileInfo.getDeviceId(), coreFileInfo.getChannelId());
        startTime = CoreUtils.getMillis(coreFileInfo.getFrom());
        endTime = CoreUtils.getMillis(coreFileInfo.getTo());

        //verify file path
        if (Util.isNullOrEmpty(coreFileInfo.getPath()))
        {
            throw new FileNotFoundException();
        }

        //Missing periods will be returned with "not-recorded" value in the path
        if (coreFileInfo.getPath().toLowerCase().equals("not-recorded"))
        {
            status = RecordedFileStatus.MISSING;
        }
        else
        {
            status = RecordedFileStatus.COMPLETED;
        }

        //check local file
        if (status.equals(RecordedFileStatus.COMPLETED))
        {
            File localFile = new File(coreFileInfo.getPath());
            if (!localFile.exists())
            {
                throw new FileNotFoundException(coreFileInfo.getPath());
            }

            path = coreFileInfo.getPath();
            fileSize = localFile.length();
        }
        else
        {
            path = "";
            fileSize = 0;
        }

        //log unusual file sizes (15 mins is the default)
        if (status == RecordedFileStatus.COMPLETED &&
            endTime - startTime > (TimeUnit.MINUTES.toMillis(15 + 1)))
        {
            Logger.error(Util.whichFn() + "recorded file duration is longer than 15mins (%s)", coreFileInfo);
        }
    }

    public DeviceChannelPair getCamera()
    {
        return camera;
    }

    public long startTime()
    {
        return startTime;
    }

    public long endTime()
    {
        return endTime;
    }

    public String getPath()
    {
        return path;
    }

    public long fileSize()
    {
        return fileSize;
    }

    public RecordedFileStatus status()
    {
        return status;
    }

    public boolean available()
    {
        return status.equals(RecordedFileStatus.COMPLETED);
    }

    public Map toAPIObject()
    {
        Map infoMap = new LinkedHashMap();
        infoMap.put("startTime", startTime);
        infoMap.put("endTime", endTime);
        infoMap.put("fileSize", fileSize);
        infoMap.put("status", status);
        return infoMap;
    }
}
