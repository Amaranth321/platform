package platform;

import com.kaisquare.core.thrift.StorageInfo;
import core.ConfigControlClient;
import core.StreamControlClient;
import models.MongoDevice;
import models.cloud.UIConfigurableCloudSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.config.readers.ConfigsNode;
import platform.coreengine.RecordingUploadStatus;
import platform.coreengine.UploadedRecordingFile;
import platform.devices.DeviceChannelPair;
import platform.time.UtcPeriod;
import play.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * All storage values are in GB
 *
 * @author Aye Maung
 * @since v4.4
 */
public enum StorageManager
{
    INSTANCE;

    private static final ConfigControlClient coreClient = ConfigControlClient.getInstance();

    public static int getCloudStorageKeepDays()
    {
        return UIConfigurableCloudSettings.server().retentionDays().cloudRecordings;
    }

    public static UtcPeriod maxQueryableCloudPeriod()
    {
        DateTime dtTo = DateTime.now(DateTimeZone.UTC);
        DateTime dtFrom = dtTo.minusDays(getCloudStorageKeepDays());
        return new UtcPeriod(dtFrom.getMillis(), dtTo.getMillis());
    }

    public static StorageManager getInstance()
    {
        return INSTANCE;
    }

    public List<StorageInfo> coreStorageStatus()
    {
        return StreamControlClient.getInstance().getCoreStorageStatus();
    }

    public void setRecordingRetention(MongoDevice device)
    {
        if (Environment.getInstance().onCloud())
        {
            return;
        }

        if (device == null)
        {
            return;
        }

        int retentionDays = ConfigsNode.getInstance().getRecordingRetentionDays(Long.parseLong(device.getModelId()));
        Logger.info("[%s] setting recording retention to %s days", device.getName(), retentionDays);
        coreClient.setStorageKeepDays(device.getCoreDeviceId(), "0", retentionDays);
    }

    /**
     * @return total recordings uploaded in mega bytes
     */
    public int getUploadedRecordingSize(DeviceChannelPair camera)
    {
        List<RecordingUploadStatus> ignoreStatusList = Arrays.asList(RecordingUploadStatus.UNREQUESTED,
                                                                     RecordingUploadStatus.ABORTED);
        //period based on keep days
        UtcPeriod period = maxQueryableCloudPeriod();

        //search
        List<UploadedRecordingFile> fileList = StreamControlClient.getInstance().getCloudStreamFileList(camera, period);

        //loop and calculate total size
        long totalBytes = 0L;
        for (UploadedRecordingFile fileInfo : fileList)
        {
            if (ignoreStatusList.contains(fileInfo.getStatus()))
            {
                continue;
            }

            totalBytes += fileInfo.getFileSize();
        }

        return Math.round(totalBytes / (1024 * 1024));
    }
}
