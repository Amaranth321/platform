package platform.devices;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import models.abstracts.ServerPagedResult;
import models.node.NodeCamera;
import platform.Environment;
import platform.db.QueryHelper;
import play.i18n.Messages;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Entity
@Indexes({
        @Index("time")
})
public class DeviceLog extends Model
{
    private final long time;
    private final long platformDeviceId;
    private final String message;

    /**
     * Use this function to log changes related to Node itself (on Cloud)
     *
     * @param platformDeviceId
     * @param message
     */
    public static void createLog(long platformDeviceId, String message)
    {
        String msg;
        if (Environment.getInstance().onCloud())
        {
            msg = String.format("[Node] %s", Messages.get(message));
        }
        else
        {
            msg = Messages.get(message);
        }

        DeviceLog log = new DeviceLog(platformDeviceId, msg);
        log.save();
    }

    /**
     * User this function to log changes related to Node's camera.
     *
     * @param platformDeviceId platform device Id of the node
     * @param nodeCamera       node camera
     * @param message
     */
    public static void createLog(long platformDeviceId, NodeCamera nodeCamera, String message)
    {
        String msg = String.format("[Camera] %s : %s", nodeCamera.name, Messages.get(message));
        DeviceLog log = new DeviceLog(platformDeviceId, msg);
        log.save();
    }

    public static ServerPagedResult<DeviceLog> query(long platformDeviceId,
                                                     int offset,
                                                     int take)
    {
        Model.MorphiaQuery query = DeviceLog.q()
                .filter("platformDeviceId", platformDeviceId)
                .order("-time");

        return QueryHelper.preparePagedResult(query, offset, take);
    }

    private DeviceLog(long platformDeviceId, String message)
    {
        this.message = message;
        this.time = Environment.getInstance().getCurrentUTCTimeMillis();
        this.platformDeviceId = platformDeviceId;
    }
}
