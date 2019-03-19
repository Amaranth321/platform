package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import platform.analytics.VcaStatus;
import platform.devices.DeviceChannelPair;
import platform.devices.DeviceStatus;
import platform.time.UtcPeriod;
import play.modules.morphia.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Monitors on/off histories
 *
 * @author Aye Maung
 */
@Entity
@Indexes({
        @Index("id"),
        @Index("time")
})
public class UpTimeLog extends Model
{
    /**
     * current unused due to unreliable status events
     */
    public static void saveForNode(String coreDeviceId, DeviceStatus deviceStatus)
    {
        String id = String.format("node_%s", coreDeviceId);
        switch (deviceStatus)
        {
            case CONNECTED:
                saveRecord(id, Condition.UP);
                break;

            case DISCONNECTED:
                saveRecord(id, Condition.DOWN);
                break;
        }
    }

    /**
     * current unused due to unreliable status events
     */
    public static List<UtcPeriod> findNodeUpPeriods(String coreDeviceId,
                                                    UtcPeriod queryPeriod,
                                                    int minDurationMinutes)
    {
        String id = String.format("node_%s", coreDeviceId);
        return getUpPeriods(id, queryPeriod, minDurationMinutes);
    }

    /**
     * current unused due to unreliable status events
     */
    public static void saveForCamera(DeviceChannelPair camera, DeviceStatus deviceStatus)
    {
        String id = String.format("camera_%s", camera.toString());
        switch (deviceStatus)
        {
            case CONNECTED:
                saveRecord(id, Condition.UP);
                break;

            case DISCONNECTED:
                saveRecord(id, Condition.DOWN);
                break;
        }
    }

    /**
     * current unused due to unreliable status events
     */
    public static List<UtcPeriod> findCameraUpPeriods(DeviceChannelPair camera,
                                                      UtcPeriod queryPeriod,
                                                      int minDurationMinutes)
    {
        String id = String.format("camera_%s", camera.toString());
        return getUpPeriods(id, queryPeriod, minDurationMinutes);
    }

    public static void saveForVca(String instanceId, VcaStatus status)
    {
        String id = String.format("vca_%s", instanceId);
        switch (status)
        {
            case RUNNING:
                saveRecord(id, Condition.UP);
                break;

            case DISABLED:
            case NOT_SCHEDULED:
            case ERROR:
                saveRecord(id, Condition.DOWN);
                break;

            case WAITING:
                //ignore
        }
    }

    public static List<UtcPeriod> findVcaUpPeriods(String instanceId,
                                                   UtcPeriod queryPeriod,
                                                   int minDurationMinutes)
    {
        String id = String.format("vca_%s", instanceId);
        return getUpPeriods(id, queryPeriod, minDurationMinutes);
    }

    private static void saveRecord(String id, Condition condition)
    {
        new UpTimeLog(id, condition).save();
    }

    private static List<UtcPeriod> getUpPeriods(String id, UtcPeriod queryPeriod, int minDurationMinutes)
    {
        Iterable<UpTimeLog> logs = q().filter("id", id).order("time").fetch();
        List<UtcPeriod> upPeriods = new ArrayList<>();

        //remove repeating logs
        List<UpTimeLog> squeezedList = new ArrayList<>();
        UpTimeLog prevLog = null;
        for (UpTimeLog currentLog : logs)
        {
            if (prevLog != null && prevLog.condition == currentLog.condition)
            {
                continue;
            }

            squeezedList.add(currentLog);
            prevLog = currentLog;
        }

        //construct UP periods
        Long periodStart = queryPeriod.getFromMillis();
        for (UpTimeLog log : squeezedList)
        {
            if (log.condition == Condition.UP)
            {
                periodStart = log.time;
                continue;
            }

            UtcPeriod period = new UtcPeriod(periodStart, log.time);
            if (period.length() > TimeUnit.MINUTES.toMillis(minDurationMinutes))
            {
                upPeriods.add(period);
                periodStart = null;
            }
        }

        //ended with an UP
        if (periodStart != null)
        {
            UtcPeriod period = new UtcPeriod(periodStart, queryPeriod.getToMillis());
            if (period.length() > TimeUnit.MINUTES.toMillis(minDurationMinutes))
            {
                upPeriods.add(period);
            }
        }

        return upPeriods;
    }

    private enum Condition
    {
        UP,
        DOWN
    }

    private final String id;
    private final long time;
    private final Condition condition;

    private UpTimeLog(String id, Condition condition)
    {
        this.id = id;
        this.time = System.currentTimeMillis();
        this.condition = condition;
    }

}
