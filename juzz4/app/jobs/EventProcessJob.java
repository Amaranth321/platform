package jobs;

import com.kaisquare.util.StringCollectionUtil;
import models.SystemPreference;
import models.UnprocessedVcaEvent;
import models.backwardcompatibility.UnprocessedEvent;
import platform.Environment;
import platform.config.readers.ConfigsShared;
import platform.events.EventType;
import platform.reports.EventReport;
import platform.reports.ReportParallelProcess;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.modules.morphia.Model.MorphiaQuery;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("deprecation")
@Every("5s")
public class EventProcessJob extends Job
{
    private static final ReentrantLock Lock = new ReentrantLock();
    private static final List<EventType> DefaultEventType = Arrays.asList(EventReport.getSupportedEventTypes());

    private ReportParallelProcess process;

    public EventProcessJob()
    {
        SystemPreference.setPreference(SystemPreference.SUPPORTED_VCA_TYPES,
                                       StringCollectionUtil.join(DefaultEventType, ","));

        process = new ReportParallelProcess();
    }

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
        if (Lock.tryLock())
        {
            try
            {
                if (UnprocessedEvent.col().count() > 0)
                {
                    Logger.warn("start migrating older unprocessed event migration (records: %d)",
                                UnprocessedEvent.col().count());
                    migrateOldUnprocessedEvents();
                }
                int threads = ConfigsShared.getInstance().reader().getAsInt("vca-event-processor.threads", 0);
                process.setMaximumThreads(threads);
                Logger.trace("EventProcessJob start");
                process.run();
                Logger.trace("EventProcessJob done");
            }
            catch (Exception e)
            {
                Logger.error(e, "error during event preprocess");
            }
            finally
            {
                Lock.unlock();
            }
        }
    }

    private void migrateOldUnprocessedEvents()
    {
        MorphiaQuery query = UnprocessedEvent.q();
        Iterable<UnprocessedEvent> iterator = query.fetch();

        for (UnprocessedEvent e : iterator)
        {
            UnprocessedVcaEvent.copyFrom(e).save();
            e.delete();
        }
    }
}
