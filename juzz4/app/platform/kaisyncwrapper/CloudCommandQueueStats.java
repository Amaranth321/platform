package platform.kaisyncwrapper;

import com.google.gson.Gson;
import platform.db.cache.CrossServerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aye Maung
 * @since v4.4
 */
public class CloudCommandQueueStats extends CrossServerEntity
{
    private List<QueueStats> queueStatsList;
    private int unknownResetCount;

    public static void set(List<QueueStats> queueStatsList, int unknownResetCount)
    {
        CloudCommandQueueStats record = get();
        record.queueStatsList = queueStatsList;
        record.unknownResetCount = unknownResetCount;
        record.save();
    }

    public static List<QueueStats> getQueueStats()
    {
        List<QueueStats> list = get().queueStatsList;
        if (list == null)
        {
            return new ArrayList<>();
        }
        return list;
    }

    public static int getUnknownResetCount()
    {
        return get().unknownResetCount;
    }

    private static CloudCommandQueueStats get()
    {
        Object o = find(getCachedKey());
        if (o != null)
        {
            return new Gson().fromJson((String) o, CloudCommandQueueStats.class);
        }
        CloudCommandQueueStats stats = new CloudCommandQueueStats();
        stats.save();
        return stats;
    }

    private static String getCachedKey()
    {
        return CloudCommandQueueStats.class.getName();
    }

    private CloudCommandQueueStats()
    {
        super(getCachedKey());
    }

    public static class QueueStats
    {
        public final String name;
        public final boolean processing;
        public final long queueCount;
        public final long waitingTime;

        public QueueStats(String name, boolean processing, long queueCount, long waitingTime)
        {
            this.name = name;
            this.processing = processing;
            this.queueCount = queueCount;
            this.waitingTime = waitingTime;
        }
    }
}
