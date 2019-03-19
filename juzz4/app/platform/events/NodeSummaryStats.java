package platform.events;

/**
 * Useful statistics for each node (v4.4 onwards). Only available on nodes.
 *
 * @author Aye Maung
 * @since v4.4
 */
public class NodeSummaryStats
{
    private int eventArrivalRate;
    private int eventSyncRate;
    private int eventVideoSyncRate;
    private long lastEventSyncTime;
    private long lastEventVideoSyncTime;
    private int avgVideoUploadDuration;

    public NodeSummaryStats()
    {
    }

    public NodeSummaryStats(int eventArrivalRate,
                            int eventSyncRate,
                            int eventVideoSyncRate,
                            long lastEventSyncTime,
                            long lastEventVideoSyncTime,
                            int avgVideoUploadDuration)
    {
        this.eventArrivalRate = eventArrivalRate;
        this.eventSyncRate = eventSyncRate;
        this.eventVideoSyncRate = eventVideoSyncRate;
        this.lastEventSyncTime = lastEventSyncTime;
        this.lastEventVideoSyncTime = lastEventVideoSyncTime;
        this.avgVideoUploadDuration = avgVideoUploadDuration;
    }

    public int getEventArrivalRate()
    {
        return eventArrivalRate;
    }

    public int getEventSyncRate()
    {
        return eventSyncRate;
    }

    public int getEventVideoSyncRate()
    {
        return eventVideoSyncRate;
    }

    public long getLastEventSyncTime()
    {
        return lastEventSyncTime;
    }

    public long getLastEventVideoSyncTime()
    {
        return lastEventVideoSyncTime;
    }

    public int getAvgVideoUploadDuration()
    {
        return avgVideoUploadDuration;
    }
}
