package models;

import com.google.code.morphia.annotations.Entity;
import platform.coreengine.RecordingManager;
import platform.coreengine.RecordingUploadStatus;
import platform.coreengine.UploadedRecordingFile;
import platform.db.QueryHelper;
import platform.devices.DeviceChannelPair;
import platform.time.UtcPeriod;
import play.modules.morphia.Model;

import java.util.List;
import java.util.UUID;

/**
 * Upload requests must be saved because original session key is required for cancellation.
 * <p/>
 * Requests are kept in db for 7 days
 *
 * @author Aye Maung
 * @since v4.4
 */
@Entity
public class RecordingUploadRequest extends Model
{
    private final String sessionKey;
    private final DeviceChannelPair camera;
    private final UtcPeriod period;
    private final long requesterUserId;

    public static MorphiaQuery find(DeviceChannelPair camera, UtcPeriod period)
    {
        MorphiaQuery query = q();

        //camera
        query.filter("camera", camera);

        //period
        QueryHelper.mustOverlap(query, "period.from", "period.to", period);

        return query;
    }

    public RecordingUploadRequest(DeviceChannelPair camera,
                                  UtcPeriod period,
                                  long requesterUserId)
    {
        this.sessionKey = UUID.randomUUID().toString();
        this.camera = camera;
        this.period = period;
        this.requesterUserId = requesterUserId;
    }

    @Override
    public String toString()
    {
        return String.format("sessionKey:%s, camera - %s, period:%s", sessionKey, camera, period);
    }

    public String getSessionKey()
    {
        return sessionKey;
    }

    public DeviceChannelPair getCamera()
    {
        return camera;
    }

    public UtcPeriod getPeriod()
    {
        return period;
    }

    public long getRequesterUserId()
    {
        return requesterUserId;
    }

    public boolean isEffectivelyEmpty()
    {
        List<UploadedRecordingFile> files = RecordingManager.getInstance().searchRecordingsOnCloud(camera, period);
        for (UploadedRecordingFile file : files)
        {
            //keep the request if any of the file is active
            if (!RecordingUploadStatus.requestableList().contains(file.getStatus()))
            {
                return false;
            }
        }

        return true;
    }
}
