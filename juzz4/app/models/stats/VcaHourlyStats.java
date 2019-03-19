package models.stats;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexes;
import lib.util.Util;
import models.UpTimeLog;
import org.joda.time.DateTime;
import platform.Environment;
import platform.analytics.VcaStatus;
import platform.time.TimeUtil;
import play.modules.morphia.Model;

/**
 * @author Aye Maung
 * @since v4.4
 */
@Entity
@Indexes({
        @Index("instanceId, time")
})
public class VcaHourlyStats extends Model
{
    private final String instanceId;
    private final long time;
    private String currentVcaStatus; // for checking stateChangeCount
    private int stateChangeCount;
    private int errorEventCount;

    public static void statusChanged(String instanceId, VcaStatus newStatus)
    {
        if (Util.isNullOrEmpty(instanceId))
        {
            return;
        }

        long hourMark = TimeUtil.getCurrentHourStart().getMillis();
        VcaHourlyStats stats = getRecordForHour(instanceId, hourMark);
        if (!newStatus.name().equalsIgnoreCase(stats.currentVcaStatus))
        {
            stats.currentVcaStatus = newStatus.name();
            stats.stateChangeCount++;
            stats.save();

            UpTimeLog.saveForVca(instanceId, newStatus);
        }
    }

    public static void errorEventReceived(String instanceId)
    {
        if (!Environment.getInstance().onCloud() ||
            Util.isNullOrEmpty(instanceId))
        {
            return;
        }

        long hourMark = TimeUtil.getCurrentHourStart().getMillis();
        VcaHourlyStats stats = getRecordForHour(instanceId, hourMark);
        stats.errorEventCount++;
        stats.save();
    }

    public static int getAvgStatusChangeCount(String instanceId, int numOfPastHours)
    {
        if (numOfPastHours == 0)
        {
            throw new IllegalArgumentException("numOfPastHours is zero");
        }

        //get average of the past numOfHours
        DateTime lastHour = TimeUtil.getCurrentHourStart().minusHours(1);

        int countTotal = 0;
        for (int i = 0; i < numOfPastHours; i++)
        {
            long currHour = lastHour.getMillis();
            VcaHourlyStats stats = q()
                    .filter("instanceId", instanceId)
                    .filter("time", currHour)
                    .first();
            if (stats != null)
            {
                countTotal += stats.stateChangeCount;
            }

            lastHour = lastHour.minusHours(1);
        }

        return countTotal / numOfPastHours;
    }

    public static void remove(String instanceId)
    {
        q().filter("instanceId", instanceId).delete();
    }

    private static VcaHourlyStats getRecordForHour(String instanceId, long hourMark)
    {
        VcaHourlyStats stats = q()
                .filter("instanceId", instanceId)
                .filter("time", hourMark)
                .first();

        if (stats == null)
        {
            stats = new VcaHourlyStats(instanceId, hourMark);
        }
        return stats;
    }

    private VcaHourlyStats(String instanceId, long time)
    {
        this.instanceId = instanceId;
        this.time = time;
    }

    public String getInstanceId()
    {
        return instanceId;
    }

    public long getTime()
    {
        return time;
    }

    public int getStateChangeCount()
    {
        return stateChangeCount;
    }

    public int getErrorEventCount()
    {
        return errorEventCount;
    }
}
