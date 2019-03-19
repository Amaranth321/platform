package platform.config;

/**
 * @author Aye Maung
 * @since v4.5
 */
public class RetentionDays
{
    public final int deletedBuckets;
    public final int auditLogs;
    public final int notifications;
    public final int eventVideos;
    public final int archivedEvents;
    public final int vcaReports;
    public final int cloudRecordings;

    public static RetentionDays forNewCloudServer()
    {
        //initial values for a new cloud server.
        return new RetentionDays(30, 7, 365, 30, 30, 365, 30);
    }

    public static RetentionDays forNode()
    {
        //these values should not be made configurable.
        //All nodes will use the same one month
        return new RetentionDays(30, 30, 30, 30, 30, 30, 30);
    }

    private RetentionDays(int deletedBuckets,
                          int auditLogs,
                          int notifications,
                          int eventVideos,
                          int archivedEvents,
                          int vcaReports, int cloudRecordings)
    {
        this.deletedBuckets = deletedBuckets;
        this.auditLogs = auditLogs;
        this.notifications = notifications;
        this.eventVideos = eventVideos;
        this.archivedEvents = archivedEvents;
        this.vcaReports = vcaReports;
        this.cloudRecordings = cloudRecordings;
    }
}
