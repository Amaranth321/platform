package platform.coreengine;

import java.util.Arrays;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public enum RecordingUploadStatus
{
    /**
     * recording available on node, but not requested for uploading yet
     */
    UNREQUESTED,

    /**
     * requested to start uploading
     */
    REQUESTED,

    /**
     * uploading in progress
     */
    UPLOADING,

    /**
     * completed
     */
    COMPLETED,

    /**
     * retrying failed upload
     */
    RETRYING,

    /**
     * upload aborted due to unrecoverable error
     */
    ABORTED,

    @Deprecated
    STOPPED;

    public static List<RecordingUploadStatus> requestableList()
    {
        return Arrays.asList(UNREQUESTED, STOPPED, ABORTED);
    }
}
