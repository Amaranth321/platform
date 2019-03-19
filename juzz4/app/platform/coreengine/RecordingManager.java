package platform.coreengine;

import core.StreamControlClient;
import lib.util.Util;
import lib.util.exceptions.ApiException;
import lib.util.exceptions.InvalidEnvironmentException;
import models.RecordingUploadRequest;
import models.node.NodeObject;
import net.lingala.zip4j.exception.ZipException;
import platform.Environment;
import platform.StorageManager;
import platform.content.ZipHelper;
import platform.db.cache.CacheClient;
import platform.db.cache.proxies.CachedNodeCamera;
import platform.devices.DeviceChannelPair;
import platform.time.UtcPeriod;
import play.Logger;
import play.Play;
import play.libs.F;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum RecordingManager
{
    INSTANCE;

    private static final String TMP_DIR = Play.applicationPath + "/public/files/tmp/recordingZips";

    static
    {
        File gridDir = new File(TMP_DIR);
        if (!gridDir.exists())
        {
            gridDir.mkdirs();
        }
    }

    private static StreamControlClient streamClient = StreamControlClient.getInstance();

    public static RecordingManager getInstance()
    {
        return INSTANCE;
    }

    public static String getTempDirectory()
    {
        return TMP_DIR;
    }

    public List<UploadedRecordingFile> searchRecordingsOnCloud(DeviceChannelPair camera, UtcPeriod period)
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        return streamClient.getCloudStreamFileList(camera, period);
    }

    public void sendVideoUploadRequest(DeviceChannelPair camera, UtcPeriod period, String requesterId) throws ApiException
    {
        if (!Environment.getInstance().onCloud())
        {
            throw new InvalidEnvironmentException();
        }

        //get available files
        List<UploadedRecordingFile> files = searchRecordingsOnCloud(camera, period);
        if (files.isEmpty())
        {
            throw new ApiException("error-no-file-to-request-upload");
        }

        //cut the main period into missing sub periods
        List<UtcPeriod> subPeriods = new ArrayList<>();
        List<UploadedRecordingFile> currentGroup = new ArrayList<>();   //temporary holder
        List<UploadedRecordingFile> eligibleFiles = new ArrayList<>();   //for size estimation
        for (UploadedRecordingFile file : files)
        {
            boolean requestable = RecordingUploadStatus.requestableList().contains(file.getStatus());
            boolean lastFile = (files.indexOf(file) == files.size() - 1);

            if (!requestable)
            {
                if (!currentGroup.isEmpty())
                {
                    long from = currentGroup.get(0).getStartTime();
                    long to = currentGroup.get(currentGroup.size() - 1).getEndTime();
                    subPeriods.add(new UtcPeriod(from, to));
                    eligibleFiles.addAll(currentGroup);
                    currentGroup.clear();
                }
            }
            else
            {
                currentGroup.add(file);
                if (lastFile)
                {
                    long from = currentGroup.get(0).getStartTime();
                    long to = currentGroup.get(currentGroup.size() - 1).getEndTime();
                    subPeriods.add(new UtcPeriod(from, to));
                    eligibleFiles.addAll(currentGroup);
                }
            }
        }

        if (subPeriods.isEmpty())
        {
            throw new ApiException("error-no-eligible-request-period");
        }

        //calculate total size
        long totalSize = 0L;
        for (UploadedRecordingFile eligibleFile : eligibleFiles)
        {
            totalSize += eligibleFile.getFileSize();
        }
        int requestSizeMB = Math.round(totalSize / (1024 * 1024));

        //calculate remaining
        NodeObject nodeObject = NodeObject.findByCoreId(camera.getCoreDeviceId());
        int storageLimit = nodeObject.getCameraStorageLimit(camera.getChannelId());
        int storageUsed = StorageManager.getInstance().getUploadedRecordingSize(camera);
        Logger.info("[Upload request] (%s) storageLimit: %s MB. storageUsed: %s MB. requestSizeMB: %s MB",
                    camera, storageLimit, storageUsed, requestSizeMB);

        if (requestSizeMB >= (storageLimit - storageUsed))
        {
            throw new ApiException("recording-request-not-enough-space");
        }

        //send requests for each missing period
        for (UtcPeriod subPeriod : subPeriods)
        {
            if (streamClient.requestRecordingUpload(camera, subPeriod))
            {
                Logger.info("[%s] upload request sent (%s)", camera, subPeriod);
                new RecordingUploadRequest(camera, period, Long.parseLong(requesterId)).save();
            }
            else
            {
                Logger.error("[%s] upload request failed (%s)", camera, subPeriod);
            }
        }

        //remove cache
        CachedNodeCamera cachedNodeCamera = CacheClient.getInstance().getNodeCamera(camera);
        CacheClient.getInstance().remove(cachedNodeCamera);
    }

    public void deleteCloudRecordings(DeviceChannelPair camera) throws ApiException
    {
        //delete all of this camera
        deleteCloudRecordings(camera, StorageManager.maxQueryableCloudPeriod());
    }

    public void deleteCloudRecordings(DeviceChannelPair camera, UtcPeriod period)
    {
        //get available files
        List<UploadedRecordingFile> files = searchRecordingsOnCloud(camera, period);
        if (files.isEmpty())
        {
            Logger.info(Util.whichFn() + "no files to delete (%s, %s)", camera, period);
            return;
        }

        List<String> toCancelIdList = new ArrayList<>();
        for (UploadedRecordingFile file : files)
        {
            toCancelIdList.add(CoreUtils.convertToTimestamp(file.getStartTime()));
        }

        boolean result = streamClient.deleteCloudRecordings(camera, toCancelIdList);
        if (result)
        {
            Logger.error("[%s] recordings deleted (%s)", camera, toCancelIdList);
        }
        else
        {
            Logger.error("[%s] recording deletion failed (%s)", camera, toCancelIdList);
        }

        //remove cache
        CachedNodeCamera cachedNodeCamera = CacheClient.getInstance().getNodeCamera(camera);
        CacheClient.getInstance().remove(cachedNodeCamera);
    }

    /**
     * This not only deletes files, but it will also clear all records from db.
     */
    public void resetAllRecordings(DeviceChannelPair camera)
    {
        Logger.info("[%s] Removing recordings and clearing all records", camera);
        streamClient.resetRecordingFiles(camera);

        //remove cache
        CachedNodeCamera cachedNodeCamera = CacheClient.getInstance().getNodeCamera(camera);
        CacheClient.getInstance().remove(cachedNodeCamera);
    }

    /**
     * @param recordings local recording files from core
     */
    public F.Promise<File> zipRecordings(List<RecordedLocalFile> recordings) throws ZipException
    {
        if (!Environment.getInstance().onKaiNode())
        {
            throw new InvalidEnvironmentException();
        }

        String zipFile = Util.combine(TMP_DIR, UUID.randomUUID().toString());

        //verify files
        List<File> verifiedList = new ArrayList<>();
        for (RecordedLocalFile recording : recordings)
        {
            if (recording.available())
            {
                verifiedList.add(new File(recording.getPath()));
                continue;
            }
        }

        return ZipHelper.getInstance().zipAsync(zipFile, verifiedList);
    }

}
