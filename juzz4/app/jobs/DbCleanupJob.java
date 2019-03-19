package jobs;

import com.kaisquare.sync.TaskManager;
import lib.util.Util;
import models.Analytics.NodeTmpVcaInstance;
import models.Analytics.VcaError;
import models.*;
import models.archived.ArchivedEvent;
import models.content.DeliveryItem;
import models.events.EventVideo;
import models.events.EventWithBinary;
import models.events.RejectedEvent;
import models.events.UniqueEventRecord;
import models.notification.AcknowledgementLog;
import models.notification.SentLabelNotification;
import models.notification.SentNotification;
import models.reports.ExportedFile;
import models.stats.VcaHourlyStats;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import platform.*;
import platform.access.UserSessionManager;
import platform.config.RetentionDays;
import platform.config.readers.ConfigsShared;
import platform.coreengine.RecordingManager;
import platform.db.QueryHelper;
import platform.debug.CommandLog;
import platform.debug.PlatformDebugger;
import platform.devices.DeviceLog;
import platform.events.EventType;
import platform.reports.AnalyticsReport;
import platform.reports.EventReport;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;
import play.modules.morphia.Model;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Aye Maung
 * @since v4.4
 */
@On("cron.Cleanup.db")
public class DbCleanupJob extends Job
{
    @Override
    public boolean init()
    {
        if (!Environment.getInstance().isStartupTasksCompleted())
        {
            return false;
        }

        return super.init();
    }

    @Override
    public void doJob()
    {
        Logger.info("%s started @ %s",
                    getClass().getSimpleName(),
                    PlatformDebugger.timestamp(DateTime.now(DateTimeZone.UTC).getMillis()));
        long startTime = System.nanoTime();
        process();
        long endTime = System.nanoTime();
        Logger.info("%s took %s millis",
                    getClass().getSimpleName(),
                    TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
    }

    private void process()
    {
        RetentionDays retentionDays = ConfigsShared.getInstance().getRetentionDays();

        /**
         *
         *
         *  User-configured expiry
         *
         *
         */

        //notifications
        SentNotification.removeEntriesOlderThan(retentionDays.notifications);
        SentLabelNotification.removeEntriesOlderThan(retentionDays.notifications);
        QueryHelper.removeOlderThan(retentionDays.notifications, AcknowledgementLog.q());

        //event videos
        EventVideo.removeEntriesOlderThan(retentionDays.eventVideos);

        //archived events
        ArchivedEvent.removeEntriesOlderThan(retentionDays.archivedEvents);

        //archived buckets
        BucketManager.getInstance().removeExpiredDeletedBuckets(retentionDays.deletedBuckets);

        //audit logs
        QueryHelper.removeOlderThan(retentionDays.auditLogs, AuditLog.q());

        //vca reports
        removeVcaReports(retentionDays.vcaReports);


        /**
         *
         *
         *  Auto-expired entries and temporary entries
         *
         *
         */

        //login sessions
        UserSessionManager.getInstance().removeExpiredSessions();

        //access keys
        AccessKeyManager.getInstance().removeExpiredKeys();

        //password reset keys
        PasswordResetManager.getInstance().removeExpiredKeys();

        //commands
        TaskManager.getInstance().removeRespondedCommands();

        //unused queues
        TaskManager.getInstance().removeUnusedQueues();

        //rejected events
        QueryHelper.removeOlderThan(7, RejectedEvent.q());

        //undelivered items
        QueryHelper.removeOlderThan(3, DeliveryItem.q());

        //tmp report files
        ExportedFile.removeEntriesOlderThan(1);

        //POS data
        removePOSData(7);

        //pending add vca requests
        QueryHelper.removeOlderThan(1, NodeTmpVcaInstance.q());

        //cloud recordings
        cleanupCoreRecordings();

        //stats collections
        QueryHelper.removeOlderThan(14, VcaHourlyStats.q());

        //Unique events
        QueryHelper.removeOlderThan(1, UniqueEventRecord.q());

        //logs
        QueryHelper.removeOlderThan(14, MigrationLog.q());
        QueryHelper.removeOlderThan(3, CommandLog.q());
        QueryHelper.removeOlderThan(30, VcaError.q());
        QueryHelper.removeOlderThan(30, DeviceLog.q());
        QueryHelper.removeOlderThan(2, UpTimeLog.q());
    }

    private void removeVcaReports(int retentionDays)
    {
        long now = Environment.getInstance().getCurrentUTCTimeMillis();
        Date from = new DateTime(now - (retentionDays * 24 * 60 * 60 * 1000L), DateTimeZone.UTC).toDate();

        //hourly data
        for (EventType eventType : EventReport.getSupportedEventTypes())
        {
            try
            {
                AnalyticsReport report = EventReport.getReport(eventType);
                report.retention(from);
            }
            catch (Exception e)
            {
                Logger.error(e, "");
            }
        }

        //binary data
        EventWithBinary.removeEntriesOlderThan(retentionDays);

        //todo: this part is not safe because event videos and software update files are stored together
//        //remove any dangling uploads on KaiSync fileserver
//        GridFS recordingGFS = new GridFS(Event.db(), "fileserver");
//        BasicDBObject query = new BasicDBObject();
//        query.put("uploadDate", new BasicDBObject("$lt", from));
//        recordingGFS.remove(query);
    }

    private void removePOSData(int retentionDays)
    {
        long now = Environment.getInstance().getCurrentUTCTimeMillis();
        Date oldestAllowed = new DateTime(now - (retentionDays * 24 * 60 * 60 * 1000L), DateTimeZone.UTC).toDate();
        PosDataReport.q().filter("sales.time < ", oldestAllowed).delete();
    }

    private void cleanupCoreRecordings()
    {
        if (!Environment.getInstance().onCloud())
        {
            //only cloud recordings need cleanup
            return;
        }

        //remove expired requests
        int keepDays = StorageManager.getCloudStorageKeepDays();
        Model.MorphiaQuery q1 = QueryHelper.getEntriesOlderThan(keepDays, RecordingUploadRequest.q());
        Iterable<RecordingUploadRequest> iterable = q1.fetch();
        for (RecordingUploadRequest request : iterable)
        {
            //delete core files
            RecordingManager.getInstance().deleteCloudRecordings(request.getCamera(), request.getPeriod());

            //delete request
            request.delete();
            Logger.info(Util.whichClass() + "removed expired cloud recordings (%s)", request);
        }

        //remove empty requests
        Model.MorphiaQuery q2 = RecordingUploadRequest.q();
        QueryHelper.mustOverlap(q2, "period.from", "period.to", StorageManager.maxQueryableCloudPeriod());
        Iterable<RecordingUploadRequest> iterable2 = q2.fetch();
        for (RecordingUploadRequest request : iterable2)
        {
            if (request.isEffectivelyEmpty())
            {
                request.delete();
                Logger.info(Util.whichClass() + "removed empty recording request (%s)", request);
            }
        }
    }
}
